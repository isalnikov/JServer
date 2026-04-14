package org.jserver.infrastructure;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для ServerConfig.
 * Проверяют чтение конфигурационных значений.
 */
class ServerConfigTest {

    @Test
    void loadsDefaultConfig() {
        var config = ServerConfig.load();
        assertNotNull(config);
        assertTrue(config.port() > 0);
        assertNotNull(config.databaseUrl());
        assertNotNull(config.jwtSecret());
    }

    @Test
    void defaultPortIs8080() {
        var config = ServerConfig.load();
        assertEquals(8080, config.port());
    }

    @Test
    void jwtSecretHasDefaultValue() {
        var config = ServerConfig.load();
        assertEquals("change-me-in-production-min-256-bits", config.jwtSecret());
    }
}
