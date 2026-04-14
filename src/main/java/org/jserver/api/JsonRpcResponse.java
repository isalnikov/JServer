package org.jserver.api;

/**
 * JSON-RPC 2.0 ответ.
 * Соответствует спецификации JSON-RPC 2.0, раздел 4.2.
 *
 * @param jsonrpc версия протокола (всегда "2.0")
 * @param result результат выполнения (null при ошибке)
 * @param error информация об ошибке (null при успехе)
 * @param id идентификатор запроса (из запроса)
 */
public record JsonRpcResponse(
    String jsonrpc,
    Object result,
    JsonRpcError error,
    Object id
) {

    /**
     * Создаёт успешный ответ.
     *
     * @param jsonrpc версия протокола
     * @param result результат
     * @param id идентификатор запроса
     * @return успешный ответ
     */
    public static JsonRpcResponse success(String jsonrpc, Object result, Object id) {
        return new JsonRpcResponse(jsonrpc, result, null, id);
    }

    /**
     * Создаёт ответ с ошибкой.
     *
     * @param jsonrpc версия протокола
     * @param error информация об ошибке
     * @param id идентификатор запроса
     * @return ответ с ошибкой
     */
    public static JsonRpcResponse error(String jsonrpc, JsonRpcError error, Object id) {
        return new JsonRpcResponse(jsonrpc, null, error, id);
    }
}
