package org.jserver.api;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.jserver.middleware.MiddlewareChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Диспетчер JSON-RPC методов.
 * Маршрутизирует запрос к зарегистрированным обработчикам.
 */
public class RpcDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(RpcDispatcher.class);
    private final Map<String, RpcMethodHandler> handlers = new ConcurrentHashMap<>();

    /**
     * Регистрирует обработчик для метода.
     *
     * @param method имя метода
     * @param handler обработчик
     */
    public void register(String method, RpcMethodHandler handler) {
        handlers.put(method, handler);
        logger.info("Registered handler for method: {}", method);
    }

    /**
     * Диспетчеризует запрос к обработчику.
     *
     * @param request JSON-RPC запрос
     * @param ctx контекст
     * @return CompletableFuture с ответом
     */
    public CompletableFuture<JsonRpcResponse> dispatch(JsonRpcRequest request, RequestContext ctx) {
        RpcMethodHandler handler = handlers.get(request.method());
        if (handler == null) {
            logger.warn("Method not found: {}", request.method());
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.methodNotFound(request.method()), request.id()));
        }

        try {
            return handler.handle(request, ctx);
        } catch (Exception e) {
            logger.error("Handler failed for method: {}", request.method(), e);
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error("2.0", JsonRpcError.internalError(e.getMessage()), request.id()));
        }
    }

    /**
     * Возвращает список зарегистрированных методов.
     *
     * @return неизменяемый сет методов
     */
    public Set<String> getRegisteredMethods() {
        return Set.copyOf(handlers.keySet());
    }

    /**
     * Создаёт обработчик для диспетчеризации (адаптер для MiddlewareChain.RpcHandler).
     *
     * @return адаптер для цепочки middleware
     */
    public MiddlewareChain.RpcHandler asChainHandler() {
        return (request, ctx) -> dispatch(request, ctx);
    }
}
