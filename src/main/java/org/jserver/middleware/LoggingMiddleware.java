package org.jserver.middleware;

import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware для логирования всех запросов.
 * Логирует method, IP, requestId и duration выполнения.
 */
public class LoggingMiddleware implements Middleware {

    private static final Logger logger = LoggerFactory.getLogger(LoggingMiddleware.class);

    @Override
    public CompletableFuture<JsonRpcResponse> process(
            JsonRpcRequest request, RequestContext ctx, MiddlewareChain chain) {

        String method = request != null ? request.method() : "unknown";
        logger.info("Request [{}] method={} ip={}", ctx.requestId(), method, ctx.clientIpAddress());

        long start = System.currentTimeMillis();
        return chain.proceed(ctx, request).thenApply(response -> {
            long duration = System.currentTimeMillis() - start;
            logger.info("Response [{}] method={} status={} duration={}ms",
                ctx.requestId(), method,
                response.error() != null ? "error" : "success", duration);
            return response;
        });
    }
}
