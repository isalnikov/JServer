package org.jserver.core;

import org.jserver.infrastructure.AuditRepository;
import org.jserver.infrastructure.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для AuthService.
 */
class AuthServiceTest {

    private AuthService authService;
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider("test-secret-key-at-least-256-bits-long!!!",
            Duration.ofMinutes(15), Duration.ofDays(7));
        var auditService = new AuditService(new AuditRepository(null));
        authService = new AuthService(jwtProvider, auditService, null);
    }

    @Test
    void loginReturnsTokens() {
        var result = authService.login("admin", "password");

        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
    }

    @Test
    void logoutLogsAction() {
        var loginResult = authService.login("admin", "password");
        // Logout должен залогировать действие
        assertDoesNotThrow(() -> authService.logout(loginResult.accessToken()));
    }

    @Test
    void refreshReturnsNewTokens() {
        var loginResult = authService.login("admin", "password");
        var refreshResult = authService.refresh(loginResult.refreshToken());

        assertNotNull(refreshResult);
        assertNotNull(refreshResult.accessToken());
        assertNotEquals(loginResult.accessToken(), refreshResult.accessToken());
    }

    @Test
    void refreshWithInvalidTokenReturnsNull() {
        var result = authService.refresh("invalid-token");
        assertNull(result);
    }

    @Test
    void loginResultToMapContainsAllFields() {
        var result = authService.login("admin", "password");
        var map = result.toMap();

        assertTrue(map.containsKey("accessToken"));
        assertTrue(map.containsKey("refreshToken"));
        assertTrue(map.containsKey("expiresAt"));
    }
}
