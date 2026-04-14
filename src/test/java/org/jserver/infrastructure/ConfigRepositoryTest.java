package org.jserver.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты ConfigRepository с H2.
 */
class ConfigRepositoryTest {

    private ConfigRepository repository;
    private H2DataSource dataSource;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        String dbName = "jserver_config_" + testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9]", "_");
        var config = new ServerConfig(8080, "0.0.0.0",
            "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "",
            "test-secret",
            Duration.ofMinutes(15), Duration.ofDays(7),
            true, 100, 100);
        dataSource = new H2DataSource(config);
        dataSource.initialize();
        repository = new ConfigRepository(dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dataSource != null) {
            try (var conn = dataSource.getConnection()) {
                conn.createStatement().execute("SHUTDOWN");
            }
        }
    }

    @Test
    void savesAndLoadsMethodConfig() {
        repository.saveMethodConfig("auth.login", 20, 20);

        var configs = repository.getAllMethodConfigs();
        assertEquals(1, configs.size());

        var config = configs.get("auth.login");
        assertNotNull(config);
        assertEquals(20, config.capacity());
        assertEquals(20, config.refillRate());
    }

    @Test
    void returnsEmptyMapWhenNoConfigs() {
        var configs = repository.getAllMethodConfigs();
        assertTrue(configs.isEmpty());
    }

    @Test
    void savesMultipleMethodConfigs() {
        repository.saveMethodConfig("auth.login", 10, 5);
        repository.saveMethodConfig("data.query", 50, 10);
        repository.saveMethodConfig("admin.reset", 5, 1);

        var configs = repository.getAllMethodConfigs();
        assertEquals(3, configs.size());
        assertEquals(10, configs.get("auth.login").capacity());
        assertEquals(50, configs.get("data.query").capacity());
        assertEquals(5, configs.get("admin.reset").capacity());
    }

    @Test
    void updatesExistingMethodConfig() {
        repository.saveMethodConfig("auth.login", 10, 5);
        repository.saveMethodConfig("auth.login", 20, 10);

        var configs = repository.getAllMethodConfigs();
        assertEquals(1, configs.size());
        assertEquals(20, configs.get("auth.login").capacity());
        assertEquals(10, configs.get("auth.login").refillRate());
    }
}
