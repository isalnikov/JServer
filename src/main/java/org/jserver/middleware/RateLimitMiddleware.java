package org.jserver.middleware;

import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.jserver.core.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware для ограничения частоты запросов.
 * Проверяет лимиты по IP-адресу клиента.
 */
public class RateLimitMiddleware implements Middleware {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitMiddleware.class);
    
    private final RateLimitService rateLimitService;

    /**
     * Создаёт middleware.
     *
     * @param rateLimitService сервис rate limiting
     */
    public RateLimitMiddleware(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
        logger.info("RateLimitMiddleware created");
    }

    @Override
    public CompletableFuture<JsonRpcResponse> process(
            JsonRpcRequest request, RequestContext ctx, MiddlewareChain chain) {
        
        if (!rateLimitService.tryConsume(ctx.clientIpAddress())) {
            logger.warn("Rate limit exceeded for request {} from IP {}", 
                ctx.requestId(), ctx.clientIpAddress());
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.rateLimitExceeded(), 
                    request != null ? request.id() : null));
        }
        
        return chain.proceed(ctx, request);
    }
}
