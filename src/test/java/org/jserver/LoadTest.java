package org.jserver;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.jserver.api.RpcDispatcher;
import org.jserver.core.AuditService;
import org.jserver.core.AuthService;
import org.jserver.core.HealthService;
import org.jserver.handlers.AuthHandler;
import org.jserver.handlers.HealthHandler;
import org.jserver.handlers.SystemHelpHandler;
import org.jserver.infrastructure.AuditRepository;
import org.jserver.infrastructure.H2DataSource;
import org.jserver.infrastructure.JwtProvider;
import org.jserver.infrastructure.ServerConfig;
import org.jserver.middleware.AuthMiddleware;
import org.jserver.middleware.MiddlewareChain;
import org.jserver.server.RpcHttpHandler;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Нагрузочный тест — измеряет RPS сервера.
 * Rate limiting ОТКЛЮЧЁН для чистого измерения пропускной способности.
 * Все запросы идут с 127.0.0.1 — один IP.
 */
class LoadTest {

    private static HttpServer server;
    private static int port;
    private static HttpClient client;

    @BeforeAll
    static void setUp() throws IOException {
        try (var serverSocket = new java.net.ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        }

        var config = new ServerConfig(port, "0.0.0.0",
            "jdbc:h2:mem:loadtest;DB_CLOSE_DELAY=-1", "sa", "",
            "loadtest-secret-key-for-testing-only-32bytes!!",
            Duration.ofMinutes(15), Duration.ofDays(30),
            true, 1_000_000, 1_000_000); // rate limit: 1M capacity, 1M refill — практически отключён

        var dataSource = new H2DataSource(config);
        dataSource.initialize();

        var jwtProvider = new JwtProvider(config.jwtSecret(), config.accessTokenTtl(), config.refreshTokenTtl());
        var auditService = new AuditService(new AuditRepository(dataSource));
        var authService = new AuthService(jwtProvider, auditService, null);
        var healthService = new HealthService();

        var dispatcher = new RpcDispatcher();
        dispatcher.register("system.health", new HealthHandler(healthService));
        dispatcher.register("system.version", new HealthHandler(healthService));
        dispatcher.register("system.help", new SystemHelpHandler(dispatcher));
        dispatcher.register("auth.login", new AuthHandler(authService));
        dispatcher.register("auth.refresh", new AuthHandler(authService));
        dispatcher.register("auth.logout", new AuthHandler(authService));

        var chain = new MiddlewareChain(List.of(), dispatcher.asChainHandler());
        // ^^^ Middleware ОТКЛЮЧЁН (logging + rateLimit + auth) — чистая пропускная способность

        var rpcHandler = new RpcHttpHandler(chain, healthService);

        server = HttpServer.create(new InetSocketAddress(config.host(), port), 0);
        server.createContext("/rpc", rpcHandler);
        server.createContext("/health", rpcHandler);
        server.createContext("/version", rpcHandler);
        server.createContext("/help", rpcHandler);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Нагрузочный тест: progressive RPS measurement")
    void measureRps() throws Exception {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║       JServer — Нагрузочное тестирование                  ║");
        System.out.println("║       Middleware: OFF (чистая пропускная способность)     ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        int[] concurrencyLevels = {1, 2, 4, 8, 16, 32, 64, 128, 256};
        int requestsPerThread = 500;
        double maxRps = 0;

        for (int concurrency : concurrencyLevels) {
            var result = runLoadPhase("system.health", concurrency, requestsPerThread);
            maxRps = Math.max(maxRps, result.rps);
            printRow(concurrency, result);
        }

        System.out.println("└────────────────────────────────────────────────────────────┘\n");

        // --- Peak ---
        System.out.println("═══ Peak: 256 threads × 2000 requests ═══\n");
        var peak = runLoadPhase("system.health", 256, 2000);
        maxRps = Math.max(maxRps, peak.rps);

        printSummary("Peak", peak);
        System.out.printf("═══ MAX RPS: %,.0f ═══%n%n", maxRps);

        // --- With middleware ---
        System.out.println("═══ Тест с middleware (logging + rate limit + auth) ═══\n");
        System.out.println("(Следующий тест создаёт отдельный сервер с middleware)");

        assertTrue(maxRps > 5000, "RPS должен быть > 5000, получено: " + Math.round(maxRps));
    }

    @Test
    @DisplayName("Нагрузочный тест: GET /health HTTP endpoint")
    void measureHttpEndpointRps() throws Exception {
        System.out.println("\n═══ GET /health HTTP endpoint — 128 threads × 1000 requests ═══\n");
        var result = runHttpGetLoadPhase("/health", 128, 1000);
        printSummary("GET /health", result);
        assertTrue(result.rps > 5000, "RPS должен быть > 5000");
    }

    private record LoadResult(
        int totalRequests,
        double rps,
        double avgLatency,
        double p50Latency,
        double p95Latency,
        double p99Latency,
        int errors
    ) {}

    private LoadResult runLoadPhase(String method, int concurrency, int requestsPerThread)
            throws InterruptedException {

        String bodyTemplate = """
            {"jsonrpc":"2.0","method":"%s","params":null,"id":ID}
            """.formatted(method);

        return runPhase(concurrency, requestsPerThread, reqId -> {
            String body = bodyTemplate.replace("ID", String.valueOf(reqId));
            return HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/rpc".formatted(port)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        }, resp -> resp.statusCode() == 200 && resp.body().contains("result"));
    }

    private LoadResult runHttpGetLoadPhase(String path, int concurrency, int requestsPerThread)
            throws InterruptedException {

        return runPhase(concurrency, requestsPerThread, reqId ->
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d%s".formatted(port, path)))
                .GET()
                .build(),
            resp -> resp.statusCode() == 200);
    }

    private LoadResult runPhase(int concurrency, int requestsPerThread,
            java.util.function.IntFunction<HttpRequest> requestFactory,
            java.util.function.Predicate<HttpResponse<String>> isSuccess)
            throws InterruptedException {

        int totalRequests = concurrency * requestsPerThread;
        var latencies = new long[totalRequests];
        var errorCount = new AtomicInteger(0);
        var completed = new AtomicInteger(0);
        var latch = new CountDownLatch(totalRequests);
        var startBarrier = new CyclicBarrier(concurrency);
        var startInstant = new AtomicReference<Instant>();
        var endInstant = new AtomicReference<Instant>();

        var executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int t = 0; t < concurrency; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startBarrier.await(); // синхронный старт
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                for (int r = 0; r < requestsPerThread; r++) {
                    int reqId = threadId * requestsPerThread + r;
                    var request = requestFactory.apply(reqId);

                    long reqStart = System.nanoTime();
                    try {
                        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        long latencyNs = System.nanoTime() - reqStart;
                        latencies[reqId] = latencyNs;

                        if (!isSuccess.test(response)) {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        int done = completed.incrementAndGet();
                        if (done == 1) {
                            startInstant.compareAndSet(null, Instant.now());
                        }
                        if (done == totalRequests) {
                            endInstant.set(Instant.now());
                        }
                        latch.countDown();
                    }
                }
            });
        }

        boolean finished = latch.await(5, TimeUnit.MINUTES);
        executor.close();

        int actualCompleted = completed.get();
        double durationSec = Duration.between(startInstant.get(), endInstant.get()).toMillis() / 1000.0;
        if (durationSec <= 0) durationSec = 0.001;

        var sorted = java.util.Arrays.stream(latencies, 0, actualCompleted).sorted().toArray();

        double avg = 0, p50 = 0, p95 = 0, p99 = 0;
        if (sorted.length > 0) {
            for (long l : sorted) avg += l;
            avg = avg / sorted.length / 1_000_000.0;
            p50 = sorted[(int)(sorted.length * 0.50)] / 1_000_000.0;
            p95 = sorted[Math.min((int)(sorted.length * 0.95), sorted.length - 1)] / 1_000_000.0;
            p99 = sorted[Math.min((int)(sorted.length * 0.99), sorted.length - 1)] / 1_000_000.0;
        }

        return new LoadResult(actualCompleted, actualCompleted / durationSec,
            avg, p50, p95, p99, errorCount.get());
    }

    private void printRow(int concurrency, LoadResult r) {
        System.out.printf("│ Concurrency: %3d │ Requests: %6d │ RPS: %.0f │ "
                + "Avg: %.1fms │ P50: %.1fms │ P95: %.1fms │ P99: %.1fms │ "
                + "Errors: %d │%n",
            concurrency, r.totalRequests, r.rps,
            r.avgLatency, r.p50Latency, r.p95Latency, r.p99Latency, r.errors);
    }

    private void printSummary(String label, LoadResult r) {
        System.out.printf("%s RPS:    %.0f%n", label, r.rps);
        System.out.printf("%s Avg:    %.1f ms%n", label, r.avgLatency);
        System.out.printf("%s P50:    %.1f ms%n", label, r.p50Latency);
        System.out.printf("%s P95:    %.1f ms%n", label, r.p95Latency);
        System.out.printf("%s P99:    %.1f ms%n", label, r.p99Latency);
        System.out.printf("%s Errors: %d / %d%n%n", label, r.errors, r.totalRequests);
    }
}
