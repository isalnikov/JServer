package org.jserver.middleware;

import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для LoggingMiddleware.
 */
class LoggingMiddlewareTest {

    @Test
    void logsRequestAndPassesThrough() {
        var middleware = new LoggingMiddleware();
        var ctx = new RequestContext("test-req-1", "127.0.0.1", null, Set.of("anonymous"));
        var request = new JsonRpcRequest("2.0", "system.health", null, 1);

        var chain = new TestChain();
        var response = middleware.process(request, ctx, chain);

        assertNotNull(response);
        assertTrue(chain.wasCalled());
    }

    @Test
    void logsResponseDuration() {
        var middleware = new LoggingMiddleware();
        var ctx = new RequestContext("test-req-2", "127.0.0.1", null, Set.of());
        var request = new JsonRpcRequest("2.0", "test", null, 1);

        var chain = new TestChainWithDelay(50); // 50ms delay
        var response = middleware.process(request, ctx, chain);

        assertNotNull(response);
        assertTrue(chain.wasCalled());
    }

    @Test
    void logsErrorResponse() {
        var middleware = new LoggingMiddleware();
        var ctx = new RequestContext("test-req-3", "10.0.0.1", UUID.randomUUID(), Set.of("user"));
        var request = new JsonRpcRequest("2.0", "auth.login", null, 2);

        var chain = new TestChainWithError();
        var future = middleware.process(request, ctx, chain);

        assertNotNull(future);
        var response = future.join();
        assertNotNull(response.error());
        assertTrue(chain.wasCalled());
    }

    // Test double
    static class TestChain extends MiddlewareChain {
        private boolean called = false;

        TestChain() {
            super(List.of(), (req, ctx) ->
                CompletableFuture.completedFuture(JsonRpcResponse.success("2.0", "ok", req.id())));
        }

        @Override
        public CompletableFuture<JsonRpcResponse> proceed(RequestContext ctx, JsonRpcRequest request) {
            this.called = true;
            return CompletableFuture.completedFuture(JsonRpcResponse.success("2.0", "ok", request.id()));
        }

        boolean wasCalled() { return called; }
    }

    static class TestChainWithDelay extends MiddlewareChain {
        private boolean called = false;
        private final long delay;

        TestChainWithDelay(long delay) {
            super(List.of(), (req, ctx) ->
                CompletableFuture.completedFuture(JsonRpcResponse.success("2.0", "ok", req.id())));
            this.delay = delay;
        }

        @Override
        public CompletableFuture<JsonRpcResponse> proceed(RequestContext ctx, JsonRpcRequest request) {
            this.called = true;
            try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return CompletableFuture.completedFuture(JsonRpcResponse.success("2.0", "ok", request.id()));
        }

        boolean wasCalled() { return called; }
    }

    static class TestChainWithError extends MiddlewareChain {
        private boolean called = false;

        TestChainWithError() {
            super(List.of(), (req, ctx) ->
                CompletableFuture.completedFuture(JsonRpcResponse.error("2.0",
                    org.jserver.api.JsonRpcError.internalError("test error"), req.id())));
        }

        @Override
        public CompletableFuture<JsonRpcResponse> proceed(RequestContext ctx, JsonRpcRequest request) {
            this.called = true;
            return CompletableFuture.completedFuture(JsonRpcResponse.error("2.0",
                org.jserver.api.JsonRpcError.internalError("test error"), request.id()));
        }

        boolean wasCalled() { return called; }
    }
}
