package org.jserver.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для RefreshToken.
 */
class RefreshTokenTest {

    @Test
    void createsRefreshTokenWithAllFields() {
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        String tokenHash = "abc123hash";

        var refreshToken = new RefreshToken(tokenHash, userId, expiresAt);

        assertEquals(tokenHash, refreshToken.tokenHash());
        assertEquals(userId, refreshToken.userId());
        assertEquals(expiresAt, refreshToken.expiresAt());
    }

    @Test
    void expiresAtIsCorrectlySet() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(7200);

        var refreshToken = new RefreshToken("hash", userId, expiresAt);

        assertEquals(expiresAt, refreshToken.expiresAt());
        assertTrue(refreshToken.expiresAt().isAfter(now));
        assertEquals(7200, refreshToken.expiresAt().getEpochSecond() - now.getEpochSecond(), 1);
    }
}
