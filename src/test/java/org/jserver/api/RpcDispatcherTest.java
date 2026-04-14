package org.jserver.api;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для RpcDispatcher.
 */
class RpcDispatcherTest {

    @Test
    void routesToRegisteredHandler() {
        var dispatcher = new RpcDispatcher();
        dispatcher.register("test.method", (req, ctx) ->
            CompletableFuture.completedFuture(JsonRpcResponse.success("2.0", "ok", req.id())));

        var ctx = new RequestContext("req-1", "127.0.0.1", null, Set.of());
        var request = new JsonRpcRequest("2.0", "test.method", null, 1);
        var response = dispatcher.dispatch(request, ctx).join();

        assertEquals("ok", response.result());
    }

    @Test
    void returnsMethodNotFoundForUnregistered() {
        var dispatcher = new RpcDispatcher();
        var ctx = new RequestContext("req-1", "127.0.0.1", null, Set.of());
        var request = new JsonRpcRequest("2.0", "unknown.method", null, 1);
        var response = dispatcher.dispatch(request, ctx).join();

        assertNotNull(response.error());
        assertEquals(JsonRpcError.METHOD_NOT_FOUND, response.error().code());
    }

    @Test
    void returnsListOfMethods() {
        var dispatcher = new RpcDispatcher();
        dispatcher.register("method.a", (req, ctx) -> null);
        dispatcher.register("method.b", (req, ctx) -> null);

        var methods = dispatcher.getRegisteredMethods();
        assertTrue(methods.contains("method.a"));
        assertTrue(methods.contains("method.b"));
    }

    @Test
    void asChainHandlerReturnsDispatcherFunction() {
        var dispatcher = new RpcDispatcher();
        dispatcher.register("test", (req, ctx) ->
            CompletableFuture.completedFuture(JsonRpcResponse.success("2.0", "result", req.id())));

        var handler = dispatcher.asChainHandler();
        var ctx = new RequestContext("req-1", "127.0.0.1", null, Set.of());
        var request = new JsonRpcRequest("2.0", "test", null, 1);
        var response = handler.handle(request, ctx);

        assertEquals("result", response.join().result());
    }
}
