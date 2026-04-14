package org.jserver.server;

import org.jserver.api.JsonRpcRequest;
import org.jserver.api.RpcDispatcher;
import org.jserver.core.HealthService;
import org.jserver.middleware.MiddlewareChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для RpcHttpHandler.
 * Проверяют парсинг JSON-RPC запросов.
 */
class RpcHttpHandlerTest {

    private RpcHttpHandler handler;

    @BeforeEach
    void setUp() {
        var dispatcher = new RpcDispatcher();
        dispatcher.register("system.health", (req, ctx) ->
            java.util.concurrent.CompletableFuture.completedFuture(
                org.jserver.api.JsonRpcResponse.success("2.0",
                    java.util.Map.of("status", "healthy"), req.id())));

        var healthService = new HealthService();
        var chain = new MiddlewareChain(
            java.util.List.of(),
            dispatcher.asChainHandler());

        handler = new RpcHttpHandler(chain, healthService);
    }

    @Test
    void parsesValidJsonRpcRequest() {
        String body = "{\"jsonrpc\":\"2.0\",\"method\":\"system.health\",\"params\":null,\"id\":1}";
        var request = handler.parseJsonRpc(body);

        assertNotNull(request);
        assertEquals("2.0", request.jsonrpc());
        assertEquals("system.health", request.method());
        assertEquals(1, request.id());
    }

    @Test
    void returnsNullForInvalidJson() {
        var request = handler.parseJsonRpc("not json");
        assertNull(request);
    }

    @Test
    void returnsRequestWithNullMethodForMissingMethod() {
        var request = handler.parseJsonRpc("{\"jsonrpc\":\"2.0\",\"id\":1}");
        // Парсер создаёт запрос, но method будет null — это обрабатывается на уровне dispatcher
        assertNotNull(request);
        assertNull(request.method());
    }
}
