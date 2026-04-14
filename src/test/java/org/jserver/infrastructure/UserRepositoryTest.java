package org.jserver.infrastructure;

import org.jserver.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты UserRepository с H2.
 */
class UserRepositoryTest {

    private UserRepository repository;
    private H2DataSource dataSource;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        String dbName = "jserver_user_" + testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9]", "_");
        var config = new ServerConfig(8080, "0.0.0.0",
            "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "",
            "test-secret",
            Duration.ofMinutes(15), Duration.ofDays(7),
            true, 100, 100);
        dataSource = new H2DataSource(config);
        dataSource.initialize();
        repository = new UserRepository(dataSource);
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
    void savesAndFindsUserByUsername() {
        var user = new User(UUID.randomUUID(), "testuser", "hash123",
            Set.of("user"), Instant.now(), Instant.now());
        repository.save(user);

        var found = repository.findByUsername("testuser");
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().username());
    }

    @Test
    void returnsEmptyForUnknownUser() {
        var found = repository.findByUsername("nonexistent");
        assertTrue(found.isEmpty());
    }

    @Test
    void savesAndFindsUserById() {
        var user = new User(UUID.randomUUID(), "user_by_id", "hash123",
            Set.of("user"), Instant.now(), Instant.now());
        repository.save(user);

        var found = repository.findById(user.id());
        assertTrue(found.isPresent());
        assertEquals(user.id(), found.get().id());
    }

    @Test
    void returnsEmptyForUnknownId() {
        var found = repository.findById(UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    void savesWithMultipleRoles() {
        var user = new User(UUID.randomUUID(), "adminuser", "hash456",
            Set.of("admin", "user"), Instant.now(), Instant.now());
        repository.save(user);

        var found = repository.findByUsername("adminuser");
        assertTrue(found.isPresent());
        assertEquals(2, found.get().roles().size());
        assertTrue(found.get().roles().contains("admin"));
        assertTrue(found.get().roles().contains("user"));
    }

    @Test
    void updatesExistingUserOnSave() {
        var id = UUID.randomUUID();
        var now = Instant.now();
        var user = new User(id, "updateuser", "hash1", Set.of("user"), now, now);
        repository.save(user);

        var updated = new User(id, "updateuser", "hash2", Set.of("user", "admin"), now, now.plusSeconds(60));
        repository.save(updated);

        var found = repository.findById(id);
        assertTrue(found.isPresent());
        assertEquals("hash2", found.get().passwordHash());
        assertTrue(found.get().roles().contains("admin"));
    }
}
