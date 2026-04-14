package org.jserver.handlers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.RequestContext;
import org.jserver.api.RpcMethodHandler;
import org.jserver.core.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Обработчик методов auth.login, auth.refresh, auth.logout.
 */
public class AuthHandler implements RpcMethodHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    private final AuthService authService;

    /**
     * Создаёт обработчик.
     *
     * @param authService сервис аутентификации
     */
    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<JsonRpcResponse> handle(JsonRpcRequest request, RequestContext ctx) {
        var params = request.params() != null ?
            (Map<String, Object>) request.params() : Map.<String, Object>of();

        return switch (request.method()) {
            case "auth.login" -> handleLogin(params, request);
            case "auth.refresh" -> handleRefresh(params, request);
            case "auth.logout" -> handleLogout(ctx, request);
            default -> CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.methodNotFound(request.method()), request.id()));
        };
    }

    private CompletableFuture<JsonRpcResponse> handleLogin(
            Map<String, Object> params, JsonRpcRequest request) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");

        if (username == null || password == null) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0",
                    JsonRpcError.invalidParams("username and password required"), request.id()));
        }

        var result = authService.login(username, password);
        return CompletableFuture.completedFuture(
            JsonRpcResponse.success("2.0", result.toMap(), request.id()));
    }

    private CompletableFuture<JsonRpcResponse> handleRefresh(
            Map<String, Object> params, JsonRpcRequest request) {
        String refreshToken = (String) params.get("refreshToken");
        if (refreshToken == null) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0",
                    JsonRpcError.invalidParams("refreshToken required"), request.id()));
        }

        var result = authService.refresh(refreshToken);
        if (result == null) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.unauthorized(), request.id()));
        }
        return CompletableFuture.completedFuture(
            JsonRpcResponse.success("2.0", result.toMap(), request.id()));
    }

    private CompletableFuture<JsonRpcResponse> handleLogout(
            RequestContext ctx, JsonRpcRequest request) {
        // В реальной реализации токен извлекается из заголовка
        authService.logout("token");
        return CompletableFuture.completedFuture(
            JsonRpcResponse.success("2.0", Map.of("status", "logged out"), request.id()));
    }
}
