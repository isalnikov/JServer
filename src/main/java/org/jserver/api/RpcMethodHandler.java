package org.jserver.api;

import java.util.concurrent.CompletableFuture;

/**
 * Интерфейс обработчика JSON-RPC метода.
 * Каждый обработчик отвечает за один метод.
 */
@FunctionalInterface
public interface RpcMethodHandler {

    /**
     * Обрабатывает JSON-RPC запрос.
     *
     * @param request запрос
     * @param ctx контекст
     * @return CompletableFuture с ответом
     */
    CompletableFuture<JsonRpcResponse> handle(JsonRpcRequest request, RequestContext ctx);
}
