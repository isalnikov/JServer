package org.jserver;

import org.jserver.infrastructure.ServerConfig;
import org.jserver.server.HttpServerBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа приложения JServer.
 * Загружает конфигурацию и запускает HTTP сервер.
 */
public class JServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(JServerApplication.class);

    /**
     * Главный метод запуска сервера.
     *
     * @param args аргументы командной строки (пока не используются)
     */
    public static void main(String[] args) {
        try {
            logger.info("JServer v1.0.0-SNAPSHOT starting...");

            ServerConfig config = ServerConfig.load();
            HttpServerBootstrap bootstrap = new HttpServerBootstrap(config);
            bootstrap.start();

            logger.info("JServer is running. Press Ctrl+C to stop.");

            // Блокируем основной поток
            Thread.currentThread().join();

        } catch (Exception e) {
            logger.error("Failed to start JServer", e);
            System.err.println("Failed to start JServer: " + e.getMessage());
            System.exit(1);
        }
    }
}
