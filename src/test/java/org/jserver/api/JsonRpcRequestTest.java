package org.jserver.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для JsonRpcRequest.
 * Проверяют создание record и корректность полей.
 */
class JsonRpcRequestTest {

    @Test
    void createsRequestWithAllFields() {
        var request = new JsonRpcRequest("2.0", "auth.login",
            java.util.Map.of("username", "admin"), 1);

        assertEquals("2.0", request.jsonrpc());
        assertEquals("auth.login", request.method());
        assertEquals(java.util.Map.of("username", "admin"), request.params());
        assertEquals(1, request.id());
    }

    @Test
    void allowsNullParams() {
        var request = new JsonRpcRequest("2.0", "system.health", null, 1);
        assertNull(request.params());
    }

    @Test
    void allowsNullIdForNotifications() {
        var request = new JsonRpcRequest("2.0", "system.health", null, null);
        assertNull(request.id());
    }
}
