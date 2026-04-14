package org.jserver.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для AuditEntry record.
 */
class AuditEntryTest {

    @Test
    void createsAuditEntryWithAllFields() {
        var id = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var now = Instant.now();
        var entry = new AuditEntry(id, now, "auth.login", userId,
            "User admin logged in", "127.0.0.1");

        assertEquals(id, entry.id());
        assertEquals(now, entry.timestamp());
        assertEquals("auth.login", entry.action());
        assertEquals(userId, entry.userId());
        assertEquals("127.0.0.1", entry.ipAddress());
    }

    @Test
    void allowsNullUserIdForAnonymous() {
        var entry = new AuditEntry(UUID.randomUUID(), Instant.now(),
            "system.health", null, "Health check", "127.0.0.1");

        assertNull(entry.userId());
    }
}
