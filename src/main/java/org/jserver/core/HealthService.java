package org.jserver.core;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис проверки здоровья сервера.
 */
public class HealthService {

    private static final Logger logger = LoggerFactory.getLogger(HealthService.class);
    private final Instant startTime;

    /**
     * Создаёт сервис.
     */
    public HealthService() {
        this.startTime = Instant.now();
        logger.info("HealthService created");
    }

    /**
     * Возвращает статус здоровья сервера.
     *
     * @return map со статусом, uptime и timestamp
     */
    public Map<String, Object> getHealth() {
        long uptime = Instant.now().getEpochSecond() - startTime.getEpochSecond();
        return Map.of(
            "status", "healthy",
            "uptime", uptime + "s",
            "timestamp", Instant.now().toString()
        );
    }

    /**
     * Возвращает информацию о версии сервера.
     *
     * @return map с версией, временем сборки и номером билда
     */
    public Map<String, String> getVersion() {
        return Map.of(
            "version", "1.0.0-SNAPSHOT",
            "buildTime", "2026-04-14T00:00:00Z",
            "buildNumber", "1"
        );
    }
}
