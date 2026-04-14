package org.jserver.handlers;

import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.RequestContext;
import org.jserver.core.AuditService;
import org.jserver.core.AuthService;
import org.jserver.infrastructure.AuditRepository;
import org.jserver.infrastructure.H2DataSource;
import org.jserver.infrastructure.JwtProvider;
import org.jserver.infrastructure.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для AuthHandler.
 */
class AuthHandlerTest {

    private AuthHandler handler;
    private H2DataSource dataSource;

    @BeforeEach
    void setUp() {
        var jwtProvider = new JwtProvider("test-secret-key-at-least-256-bits-long!!!",
            Duration.ofMinutes(15), Duration.ofDays(7));
        var config = new ServerConfig(8080, "0.0.0.0",
            "jdbc:h2:mem:auth_handler_test;DB_CLOSE_DELAY=-1", "sa", "",
            "test-secret",
            Duration.ofMinutes(15), Duration.ofDays(7),
            true, 100, 100);
        dataSource = new H2DataSource(config);
        dataSource.initialize();
        var auditRepository = new AuditRepository(dataSource);
        var auditService = new AuditService(auditRepository);
        var authService = new AuthService(jwtProvider, auditService, null);
        handler = new AuthHandler(authService);
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
    void handlesAuthLogin() {
        var request = new JsonRpcRequest("2.0", "auth.login",
            Map.of("username", "admin", "password", "password"), 1);
        var ctx = new RequestContext("req-1", "127.0.0.1", null, Set.of());
        var response = handler.handle(request, ctx).join();

        assertNotNull(response.result());
        assertNull(response.error());
        @SuppressWarnings("unchecked")
        var result = (Map<String, Object>) response.result();
        assertTrue(result.containsKey("accessToken"));
        assertTrue(result.containsKey("refreshToken"));
    }

    @Test
    void handlesAuthLoginWithMissingParams() {
        var request = new JsonRpcRequest("2.0", "auth.login",
            Map.of("username", "admin"), 1);
        var ctx = new RequestContext("req-1", "127.0.0.1", null, Set.of());
        var response = handler.handle(request, ctx).join();

        assertNotNull(response.error());
        assertEquals(JsonRpcError.INVALID_PARAMS, response.error().code());
    }

    @Test
    void handlesAuthRefresh() {
        // Сначала логинимся
        var loginRequest = new JsonRpcRequest("2.0", "auth.login",
            Map.of("username", "admin", "password", "password"), 1);
        var ctx = new RequestContext("req-1", "127.0.0.1", null, Set.of());
        var loginResponse = handler.handle(loginRequest, ctx).join();

        @SuppressWarnings("unchecked")
        var loginResult = (Map<String, Object>) loginResponse.result();
        String refreshToken = (String) loginResult.get("refreshToken");

        // Теперь refresh
        var refreshRequest = new JsonRpcRequest("2.0", "auth.refresh",
            Map.of("refreshToken", refreshToken), 2);
        var refreshResponse = handler.handle(refreshRequest, ctx).join();

        assertNotNull(refreshResponse.result());
        assertNull(refreshResponse.error());
    }

    @Test
    void handlesAuthLogout() {
        var request = new JsonRpcRequest("2.0", "auth.logout", null, 1);
        var ctx = new RequestContext("req-1", "127.0.0.1", null, Set.of());
        var response = handler.handle(request, ctx).join();

        assertNotNull(response.result());
        assertNull(response.error());
    }
}
