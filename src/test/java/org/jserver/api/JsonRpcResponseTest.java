package org.jserver.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для JsonRpcResponse.
 */
class JsonRpcResponseTest {

    @Test
    void createsSuccessResponse() {
        var result = java.util.Map.of("token", "abc123");
        var response = JsonRpcResponse.success("2.0", result, 1);

        assertEquals("2.0", response.jsonrpc());
        assertEquals(result, response.result());
        assertNull(response.error());
        assertEquals(1, response.id());
    }

    @Test
    void createsErrorResponse() {
        var error = new JsonRpcError(-32600, "Invalid Request", null);
        var response = JsonRpcResponse.error("2.0", error, null);

        assertEquals("2.0", response.jsonrpc());
        assertEquals(error, response.error());
        assertNull(response.result());
        assertNull(response.id());
    }
}
