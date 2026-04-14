package org.jserver.api;

/**
 * JSON-RPC 2.0 запрос.
 * Соответствует спецификации JSON-RPC 2.0, раздел 4.1.
 *
 * @param jsonrpc версия протокола (всегда "2.0")
 * @param method имя вызываемого метода
 * @param params параметры метода (может быть null)
 * @param id идентификатор запроса (null для notification)
 */
public record JsonRpcRequest(
    String jsonrpc,
    String method,
    Object params,
    Object id
) {}
