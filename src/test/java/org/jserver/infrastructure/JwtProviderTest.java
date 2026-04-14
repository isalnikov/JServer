package org.jserver.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для JwtProvider.
 */
class JwtProviderTest {

    private JwtProvider provider;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        provider = new JwtProvider("test-secret-key-at-least-256-bits-long!!!",
            Duration.ofMinutes(15), Duration.ofDays(7));
    }

    @Test
    void generatesAndValidatesAccessToken() {
        String token = provider.generateAccessToken(userId, Set.of("user", "admin"));
        var claims = provider.validateToken(token);

        assertNotNull(claims);
        assertEquals(userId, claims.userId());
        assertTrue(claims.roles().contains("user"));
        assertTrue(claims.roles().contains("admin"));
    }

    @Test
    void rejectsInvalidToken() {
        var claims = provider.validateToken("invalid.token.here");
        assertNull(claims);
    }

    @Test
    void generatesAndValidatesRefreshToken() {
        String token = provider.generateRefreshToken(userId);
        var claims = provider.validateToken(token);

        assertNotNull(claims);
        assertEquals(userId, claims.userId());
    }

    @Test
    void differentTokensForDifferentUsers() {
        String token1 = provider.generateAccessToken(userId, Set.of("user"));
        String token2 = provider.generateAccessToken(UUID.randomUUID(), Set.of("user"));

        assertNotEquals(token1, token2);
    }
}
