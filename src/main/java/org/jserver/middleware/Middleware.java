package org.jserver.middleware;

import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;

/**
 * Интерфейс middleware компонента.
 * Каждый middleware может обработать запрос и решить,
 * передать ли его дальше по цепочке.
 */
public interface Middleware {

    /**
     * Обрабатывает запрос в цепочке middleware.
     *
     * @param request JSON-RPC запрос
     * @param ctx контекст запроса
     * @param chain цепочка для передачи следующему middleware
     * @return CompletableFuture с ответом
     */
    CompletableFuture<JsonRpcResponse> process(
        JsonRpcRequest request, 
        RequestContext ctx, 
        MiddlewareChain chain);
}
