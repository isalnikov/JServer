package org.jserver.core;

import org.jserver.infrastructure.AuditRepository;
import org.jserver.infrastructure.H2DataSource;
import org.jserver.infrastructure.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты AuditService с H2.
 */
class AuditServiceTest {

    private AuditService service;
    private H2DataSource dataSource;
    private AuditRepository repository;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        String dbName = "jserver_audit_" + testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9]", "_");
        var config = new ServerConfig(8080, "0.0.0.0",
            "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "",
            "test-secret",
            Duration.ofMinutes(15), Duration.ofDays(7),
            true, 100, 100);
        dataSource = new H2DataSource(config);
        dataSource.initialize();
        repository = new AuditRepository(dataSource);
        service = new AuditService(repository);
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
    void logsActionAndRetrievesIt() {
        var userId = UUID.randomUUID();
        service.logAction("auth.login", userId, "success", "127.0.0.1");

        var entries = service.getRecentEntries(10);
        assertEquals(1, entries.size());
        assertEquals("auth.login", entries.get(0).action());
        assertEquals(userId, entries.get(0).userId());
        assertEquals("127.0.0.1", entries.get(0).ipAddress());
        assertEquals("success", entries.get(0).details());
    }

    @Test
    void logsMultipleActionsAndRetrievesRecent() {
        var userId = UUID.randomUUID();
        service.logAction("auth.login", userId, "success", "127.0.0.1");

        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        service.logAction("data.query", userId, "query executed", "127.0.0.1");

        var entries = service.getRecentEntries(10);
        assertEquals(2, entries.size());
        // Most recent first
        assertEquals("data.query", entries.get(0).action());
        assertEquals("auth.login", entries.get(1).action());
    }

    @Test
    void respectsLimitOnRecentEntries() {
        for (int i = 0; i < 5; i++) {
            service.logAction("action." + i, UUID.randomUUID(), "details", "127.0.0.1");
            try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        var entries = service.getRecentEntries(3);
        assertEquals(3, entries.size());
    }

    @Test
    void logsAnonymousAction() {
        service.logAction("system.health", null, "anonymous access", "10.0.0.1");

        var entries = service.getRecentEntries(10);
        assertEquals(1, entries.size());
        assertNull(entries.get(0).userId());
    }

    @Test
    void returnsEmptyListWhenNoEntries() {
        var entries = service.getRecentEntries(10);
        assertTrue(entries.isEmpty());
    }
}
