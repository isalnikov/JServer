package org.jserver.server;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Запуск JDK HttpServer на виртуальных потоках.
 * Конфигурирует все компоненты и регистрирует handlers.
 */
public class HttpServerBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(HttpServerBootstrap.class);

    private final ServerConfig config;

    /**
     * Создаёт bootstrap.
     *
     * @param config конфигурация сервера
     */
    public HttpServerBootstrap(ServerConfig config) {
        this.config = config;
    }

    /**
     * Запускает сервер.
     *
     * @throws IOException ошибка запуска
     */
    public void start() throws IOException {
        logger.info("Starting JServer on {}:{}", config.host(), config.port());

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

        // Запуск HttpServer на virtual threads
        HttpServer server = HttpServer.create(
            new InetSocketAddress(config.host(), config.port()), 0);
        server.createContext("/rpc", rpcHandler);
        server.createContext("/health", rpcHandler);
        server.createContext("/version", rpcHandler);
        server.createContext("/help", rpcHandler);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        logger.info("JServer started successfully on http://{}:{}", config.host(), config.port());
    }
}
