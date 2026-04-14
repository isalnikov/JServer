package org.jserver.middleware;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.jserver.infrastructure.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware для проверки JWT-токенов.
 * Извлекает токен из заголовка Authorization и валидирует.
 */
public class AuthMiddleware implements Middleware {

    private static final Logger logger = LoggerFactory.getLogger(AuthMiddleware.class);

    private final JwtProvider jwtProvider;
    private static final Set<String> PUBLIC_METHODS = Set.of(
        "system.health", "system.version", "system.help", "auth.login", "auth.refresh");

    /**
     * Создаёт middleware.
     *
     * @param jwtProvider провайдер JWT
     */
    public AuthMiddleware(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
        logger.info("AuthMiddleware created");
    }

    @Override
    public CompletableFuture<JsonRpcResponse> process(
            JsonRpcRequest request, RequestContext ctx, MiddlewareChain chain) {

        if (request == null || PUBLIC_METHODS.contains(request.method())) {
            // Публичные методы не требуют авторизации
            return chain.proceed(ctx, request);
        }

        // TODO: извлечь токен из HTTP заголовка Authorization
        // Пока — заглушка, требующая токена
        String authHeader = getAuthHeader(ctx);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for request {}", ctx.requestId());
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.unauthorized(), request.id()));
        }

        String token = authHeader.substring(7);
        var claims = jwtProvider.validateToken(token);

        if (claims == null) {
            logger.warn("Invalid JWT token for request {}", ctx.requestId());
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.unauthorized(), request.id()));
        }

        // Создаём новый контекст с userId и ролями из токена
        var authCtx = new RequestContext(
            ctx.requestId(), ctx.clientIpAddress(),
            claims.userId(), claims.roles());

        return chain.proceed(authCtx, request);
    }

    private String getAuthHeader(RequestContext ctx) {
        // В реальной реализации извлекается из HTTP заголовка
        // Здесь — заглушка
        return null;
    }
}
