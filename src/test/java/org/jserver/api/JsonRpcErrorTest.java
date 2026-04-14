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

    @Test
    void parseErrorReturnsCorrectCodeAndMessage() {
        var error = JsonRpcError.parseError();

        assertEquals(JsonRpcError.PARSE_ERROR, error.code());
        assertEquals(-32700, error.code());
        assertEquals("Parse error", error.message());
        assertNull(error.data());
    }

    @Test
    void invalidRequestReturnsCodeAndContainsDetails() {
        var error = JsonRpcError.invalidRequest("missing method");

        assertEquals(JsonRpcError.INVALID_REQUEST, error.code());
        assertEquals(-32600, error.code());
        assertTrue(error.message().contains("missing method"));
        assertNull(error.data());
    }

    @Test
    void methodNotFoundReturnsCodeAndMethodName() {
        var error = JsonRpcError.methodNotFound("auth.login");

        assertEquals(JsonRpcError.METHOD_NOT_FOUND, error.code());
        assertEquals(-32601, error.code());
        assertTrue(error.message().contains("auth.login"));
        assertNull(error.data());
    }

    @Test
    void invalidParamsReturnsCodeAndContainsDetails() {
        var error = JsonRpcError.invalidParams("username required");

        assertEquals(JsonRpcError.INVALID_PARAMS, error.code());
        assertEquals(-32602, error.code());
        assertTrue(error.message().contains("username required"));
        assertNull(error.data());
    }

    @Test
    void internalErrorReturnsCodeAndContainsDetails() {
        var error = JsonRpcError.internalError("database unavailable");

        assertEquals(JsonRpcError.INTERNAL_ERROR, error.code());
        assertEquals(-32603, error.code());
        assertTrue(error.message().contains("database unavailable"));
        assertNull(error.data());
    }

    @Test
    void rateLimitExceededReturnsServerRangeCode() {
        var error = JsonRpcError.rateLimitExceeded();

        assertEquals(JsonRpcError.RATE_LIMIT_EXCEEDED, error.code());
        assertEquals(-32001, error.code());
        assertEquals("Rate limit exceeded", error.message());
        assertNull(error.data());
    }

    @Test
    void unauthorizedReturnsServerRangeCode() {
        var error = JsonRpcError.unauthorized();

        assertEquals(JsonRpcError.UNAUTHORIZED, error.code());
        assertEquals(-32002, error.code());
        assertEquals("Unauthorized", error.message());
        assertNull(error.data());
    }

    @Test
    void forbiddenReturnsServerRangeCode() {
        var error = JsonRpcError.forbidden();

        assertEquals(JsonRpcError.FORBIDDEN, error.code());
        assertEquals(-32003, error.code());
        assertEquals("Forbidden", error.message());
        assertNull(error.data());
    }
}
