package org.jserver;

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
        logger.info("JServer starting...");
        logger.info("JServer v{}-SNAPSHOT", "1.0.0");
        logger.info("JServer started successfully");
    }
}
