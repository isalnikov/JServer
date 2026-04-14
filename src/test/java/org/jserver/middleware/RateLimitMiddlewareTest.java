package org.jserver.middleware;

import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.jserver.core.RateLimitService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для RateLimitMiddleware.
 */
class RateLimitMiddlewareTest {

    @Test
    void allowsRequestWhenTokensAvailable() {
        var rateLimitService = new RateLimitService(100, 100);
        var middleware = new RateLimitMiddleware(rateLimitService);
        var ctx = new RequestContext(UUID.randomUUID().toString(), "127.0.0.1", null, Set.of("anonymous"));
        
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        var chain = new MiddlewareChain(
            List.of(),
            (req, context) -> {
                chainCalled.set(true);
                return CompletableFuture.completedFuture(JsonRpcResponse.success("2.0", "ok", 1));
            });
        
        var response = middleware.process(null, ctx, chain).join();
        
        assertNotNull(response);
        assertNull(response.error());
        assertTrue(chainCalled.get());
    }

    @Test
    void blocksRequestWhenLimitExceeded() {
        var rateLimitService = new RateLimitService(1, 1);
        var middleware = new RateLimitMiddleware(rateLimitService);
        var ctx = new RequestContext(UUID.randomUUID().toString(), "127.0.0.1", null, Set.of("anonymous"));
        
        // Первый запрос потребляет токен
        rateLimitService.tryConsume("127.0.0.1");
        
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        var chain = new MiddlewareChain(
            List.of(),
            (req, context) -> {
                chainCalled.set(true);
                return CompletableFuture.completedFuture(JsonRpcResponse.success("2.0", "ok", 1));
            });
        
        var request = new JsonRpcRequest("2.0", "test", null, 1);
        var response = middleware.process(request, ctx, chain).join();
        
        assertNotNull(response.error());
        assertEquals(JsonRpcError.RATE_LIMIT_EXCEEDED, response.error().code());
        assertFalse(chainCalled.get());
    }
}
