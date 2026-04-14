package org.jserver.handlers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.RequestContext;
import org.jserver.api.RpcDispatcher;
import org.jserver.api.RpcMethodHandler;

/**
 * Обработчик метода system.help.
 * Возвращает список зарегистрированных методов.
 */
public class SystemHelpHandler implements RpcMethodHandler {

    private final RpcDispatcher dispatcher;

    /**
     * Создаёт обработчик.
     *
     * @param dispatcher диспетчер методов
     */
    public SystemHelpHandler(RpcDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public CompletableFuture<JsonRpcResponse> handle(JsonRpcRequest request, RequestContext ctx) {
        var methods = dispatcher.getRegisteredMethods().stream().sorted().toList();
        return CompletableFuture.completedFuture(
            JsonRpcResponse.success("2.0", Map.of("methods", methods), request.id()));
    }
}
