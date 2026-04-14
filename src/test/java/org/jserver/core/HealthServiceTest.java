package org.jserver.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для HealthService.
 */
class HealthServiceTest {

    @Test
    void getHealthReturnsStatusAndUptime() {
        var service = new HealthService();
        var health = service.getHealth();

        assertEquals("healthy", health.get("status"));
        assertTrue(health.containsKey("uptime"));
        assertTrue(health.containsKey("timestamp"));
    }

    @Test
    void getVersionReturnsVersionInfo() {
        var service = new HealthService();
        var version = service.getVersion();

        assertTrue(version.containsKey("version"));
        assertTrue(version.containsKey("buildTime"));
        assertTrue(version.containsKey("buildNumber"));
    }
}
