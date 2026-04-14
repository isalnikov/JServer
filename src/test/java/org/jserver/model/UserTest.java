package org.jserver.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для User record.
 */
class UserTest {

    @Test
    void createsUserWithAllFields() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var user = new User(id, "admin", "hash123", Set.of("admin"), now, now);

        assertEquals(id, user.id());
        assertEquals("admin", user.username());
        assertEquals(Set.of("admin"), user.roles());
    }

    @Test
    void supportsMultipleRoles() {
        var user = new User(UUID.randomUUID(), "user", "hash",
            Set.of("user", "admin"), Instant.now(), Instant.now());

        assertTrue(user.roles().contains("user"));
        assertTrue(user.roles().contains("admin"));
    }
}
