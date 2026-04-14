package org.jserver.middleware;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;

/**
 * Цепочка выполнения middleware.
 * Последовательно вызывает middleware в порядке регистрации.
 */
public class MiddlewareChain {

    private final List<Middleware> middlewares;
    private int index = 0;
    private final RpcHandler finalHandler;

    /**
     * Функциональный интерфейс для финального обработчика.
     */
    @FunctionalInterface
    public interface RpcHandler {
        CompletableFuture<JsonRpcResponse> handle(JsonRpcRequest request, RequestContext ctx);
    }

    /**
     * Создаёт цепочку middleware.
     *
     * @param middlewares список middleware
     * @param finalHandler финальный обработчик (dispatcher)
     */
    public MiddlewareChain(List<Middleware> middlewares, RpcHandler finalHandler) {
        this.middlewares = middlewares;
        this.finalHandler = finalHandler;
    }

    /**
     * Передаёт управление следующему middleware или финальному обработчику.
     *
     * @param ctx контекст запроса
     * @param request JSON-RPC запрос
     * @return CompletableFuture с ответом
     */
    public CompletableFuture<JsonRpcResponse> proceed(RequestContext ctx, JsonRpcRequest request) {
        if (index < middlewares.size()) {
            Middleware current = middlewares.get(index);
            index++;
            return current.process(request, ctx, this);
        } else {
            // Reset index so the chain can be reused for the next request
            index = 0;
            return finalHandler.handle(request, ctx);
        }
    }
}
