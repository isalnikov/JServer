package org.jserver;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import org.jserver.api.RpcDispatcher;
import org.jserver.core.AuditService;
import org.jserver.core.AuthService;
import org.jserver.core.HealthService;
import org.jserver.core.RateLimitService;
import org.jserver.handlers.AuthHandler;
import org.jserver.handlers.HealthHandler;
import org.jserver.handlers.SystemHelpHandler;
import org.jserver.infrastructure.AuditRepository;
import org.jserver.infrastructure.H2DataSource;
import org.jserver.infrastructure.JwtProvider;
import org.jserver.infrastructure.ServerConfig;
import org.jserver.middleware.AuthMiddleware;
import org.jserver.middleware.LoggingMiddleware;
import org.jserver.middleware.MiddlewareChain;
import org.jserver.middleware.RateLimitMiddleware;
import org.jserver.server.RpcHttpHandler;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты полного цикла.
 * Запускают реальный сервер на случайном порту и проверяют HTTP endpoints.
 */
class IntegrationTest {

    private static HttpServer server;
    private static int port;
    private static HttpClient client;

    @BeforeAll
    static void setUp() throws IOException {
        // Находим свободный порт
        try (var serverSocket = new java.net.ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        }

        var config = new ServerConfig(port, "0.0.0.0",
            "jdbc:h2:mem:integration-test;DB_CLOSE_DELAY=-1", "sa", "",
            "integration-test-secret-key-for-testing-only-32bytes!",
            Duration.ofMinutes(15), Duration.ofDays(30),
            true, 100, 10);

        // Инициализация БД
        var dataSource = new H2DataSource(config);
        dataSource.initialize();

        // Создание сервисов
        var jwtProvider = new JwtProvider(
            config.jwtSecret(), config.accessTokenTtl(), config.refreshTokenTtl());
        var auditService = new AuditService(new AuditRepository(dataSource));
        var authService = new AuthService(jwtProvider, auditService, null);
        var rateLimitService = new RateLimitService(
            config.defaultRateLimitCapacity(), config.defaultRateLimitRefillRate());
        var healthService = new HealthService();

        // Создание dispatcher и регистрация handlers
        var dispatcher = new RpcDispatcher();
        dispatcher.register("system.health", new HealthHandler(healthService));
        dispatcher.register("system.version", new HealthHandler(healthService));
        dispatcher.register("system.help", new SystemHelpHandler(dispatcher));
        dispatcher.register("auth.login", new AuthHandler(authService));
        dispatcher.register("auth.refresh", new AuthHandler(authService));
        dispatcher.register("auth.logout", new AuthHandler(authService));

        // Создание middleware chain
        var middlewares = List.of(
            new LoggingMiddleware(),
            new RateLimitMiddleware(rateLimitService),
            new AuthMiddleware(jwtProvider));
        var chain = new MiddlewareChain(middlewares, dispatcher.asChainHandler());

        // Создание HTTP handler
        var rpcHandler = new RpcHttpHandler(chain, healthService);

        // Запуск сервера
        server = HttpServer.create(new InetSocketAddress(config.host(), port), 0);
        server.createContext("/rpc", rpcHandler);
        server.createContext("/health", rpcHandler);
        server.createContext("/version", rpcHandler);
        server.createContext("/help", rpcHandler);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void systemHealthReturnsOk() throws Exception {
        String body = """
            {"jsonrpc":"2.0","method":"system.health","params":null,"id":1}
            """;
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:%d/rpc".formatted(port)))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("healthy"));
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:%d/health".formatted(port)))
            .GET()
            .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("healthy"));
    }

    @Test
    void versionEndpointReturnsOk() throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:%d/version".formatted(port)))
            .GET()
            .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("version"));
    }
}
