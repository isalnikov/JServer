package org.jserver.middleware;

import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для MiddlewareChain.
 */
class MiddlewareTest {

    @Test
    void chainCallsMiddlewaresInOrder() {
        List<String> callOrder = new ArrayList<>();
        
        Middleware middleware1 = (request, ctx, chain) -> {
            callOrder.add("middleware1");
            return chain.proceed(ctx, request);
        };
        
        Middleware middleware2 = (request, ctx, chain) -> {
            callOrder.add("middleware2");
            return chain.proceed(ctx, request);
        };
        
        var request = new JsonRpcRequest("2.0", "test", null, 1);
        var ctx = new RequestContext(UUID.randomUUID().toString(), "127.0.0.1", null, Set.of());
        
        var chain = new MiddlewareChain(
            List.of(middleware1, middleware2),
            (req, context) -> CompletableFuture.completedFuture(
                JsonRpcResponse.success("2.0", "final", req.id())));
        
        var response = chain.proceed(ctx, request).join();
        
        assertEquals("final", response.result());
        assertEquals(List.of("middleware1", "middleware2"), callOrder);
    }
    
    @Test
    void middlewareCanShortCircuit() {
        Middleware blockingMiddleware = (request, ctx, chain) -> 
            CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", org.jserver.api.JsonRpcError.forbidden(), 1));
        
        Middleware neverCalled = (request, ctx, chain) -> {
            fail("This middleware should not be called");
            return null;
        };
        
        var ctx = new RequestContext(UUID.randomUUID().toString(), "127.0.0.1", null, Set.of());
        
        var chain = new MiddlewareChain(
            List.of(blockingMiddleware, neverCalled),
            (req, context) -> CompletableFuture.completedFuture(
                JsonRpcResponse.success("2.0", "final", req.id())));
        
        var response = chain.proceed(ctx, new JsonRpcRequest("2.0", "test", null, 1)).join();
        
        assertNotNull(response.error());
        assertEquals(org.jserver.api.JsonRpcError.FORBIDDEN, response.error().code());
    }
    
    @Test
    void chainCallsFinalHandlerWhenNoMiddlewares() {
        var ctx = new RequestContext(UUID.randomUUID().toString(), "127.0.0.1", null, Set.of());
        var request = new JsonRpcRequest("2.0", "test", null, 1);
        
        var chain = new MiddlewareChain(
            List.of(),
            (req, context) -> CompletableFuture.completedFuture(
                JsonRpcResponse.success("2.0", "direct", req.id())));
        
        var response = chain.proceed(ctx, request).join();
        
        assertEquals("direct", response.result());
    }
}
