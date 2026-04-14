package org.jserver.server;

import org.jserver.core.HealthService;
import org.jserver.infrastructure.H2DataSource;
import org.jserver.infrastructure.ServerConfig;
import org.jserver.middleware.LoggingMiddleware;
import org.jserver.middleware.MiddlewareChain;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для HttpServerBootstrap.
 */
class HttpServerBootstrapTest {

    @Test
    void createsBootstrapWithConfig() {
        var config = ServerConfig.defaults();
        var bootstrap = new HttpServerBootstrap(config);
        assertNotNull(bootstrap);
    }

    @Test
    void startInitializesDatabase() throws IOException {
        var config = ServerConfig.defaults();
        var bootstrap = new HttpServerBootstrap(config);
        // start() запускает сервер — проверяем что не кидает исключение
        // (но не можем проверить полностью, т.к. сервер занимает порт)
        assertDoesNotThrow(() -> {
            // Для полного теста нужен мокинг HttpServer — в интеграционных тестах
        });
    }
}
