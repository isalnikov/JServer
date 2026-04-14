package org.jserver.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для JsonRpcError кодов.
 */
class JsonRpcErrorTest {

    @Test
    void createsErrorWithCodeAndMessage() {
        var error = new JsonRpcError(-32600, "Invalid Request", null);

        assertEquals(-32600, error.code());
        assertEquals("Invalid Request", error.message());
        assertNull(error.data());
    }

    @Test
    void supportsOptionalData() {
        var error = new JsonRpcError(-32602, "Invalid params",
            java.util.Map.of("field", "username"));

        assertEquals(java.util.Map.of("field", "username"), error.data());
    }
}
