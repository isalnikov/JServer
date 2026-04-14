package org.jserver.handlers;

import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.RequestContext;
import org.jserver.api.RpcMethodHandler;
import org.jserver.core.HealthService;

/**
 * Обработчик методов system.health и system.version.
 */
public class HealthHandler implements RpcMethodHandler {

    private final HealthService healthService;

    /**
     * Создаёт обработчик.
     *
     * @param healthService сервис здоровья
     */
    public HealthHandler(HealthService healthService) {
        this.healthService = healthService;
    }

    @Override
    public CompletableFuture<JsonRpcResponse> handle(JsonRpcRequest request, RequestContext ctx) {
        return switch (request.method()) {
            case "system.health" -> CompletableFuture.completedFuture(
                JsonRpcResponse.success("2.0", healthService.getHealth(), request.id()));
            case "system.version" -> CompletableFuture.completedFuture(
                JsonRpcResponse.success("2.0", healthService.getVersion(), request.id()));
            default -> CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.methodNotFound(request.method()), request.id()));
        };
    }
}
