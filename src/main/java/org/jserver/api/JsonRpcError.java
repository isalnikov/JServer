package org.jserver.api;

/**
 * JSON-RPC 2.0 ошибка.
 * Содержит код, сообщение и опциональные данные.
 *
 * @param code код ошибки (отрицательное число)
 * @param message человекочитаемое сообщение
 * @param data дополнительные данные (может быть null)
 */
public record JsonRpcError(
    int code,
    String message,
    Object data
) {

    // Стандартные коды ошибок JSON-RPC 2.0
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    // Кастомные коды JServer (диапазон сервера JSON-RPC 2.0: -32000...-32099)
    public static final int RATE_LIMIT_EXCEEDED = -32001;
    public static final int UNAUTHORIZED = -32002;
    public static final int FORBIDDEN = -32003;

    /**
     * Создаёт ошибку parse error.
     *
     * @return ошибка парсинга JSON
     */
    public static JsonRpcError parseError() {
        return new JsonRpcError(PARSE_ERROR, "Parse error", null);
    }

    /**
     * Создаёт ошибку invalid request.
     *
     * @param details детали ошибки
     * @return ошибка неверного запроса
     */
    public static JsonRpcError invalidRequest(String details) {
        return new JsonRpcError(INVALID_REQUEST, "Invalid Request: " + details, null);
    }

    /**
     * Создаёт ошибку method not found.
     *
     * @param method имя ненайденного метода
     * @return ошибка неизвестного метода
     */
    public static JsonRpcError methodNotFound(String method) {
        return new JsonRpcError(METHOD_NOT_FOUND, "Method not found: " + method, null);
    }

    /**
     * Создаёт ошибку invalid params.
     *
     * @param details описание проблемы с параметрами
     * @return ошибка неверных параметров
     */
    public static JsonRpcError invalidParams(String details) {
        return new JsonRpcError(INVALID_PARAMS, "Invalid params: " + details, null);
    }

    /**
     * Создаёт ошибку internal error.
     *
     * @param details детали внутренней ошибки
     * @return внутренняя ошибка сервера
     */
    public static JsonRpcError internalError(String details) {
        return new JsonRpcError(INTERNAL_ERROR, "Internal error: " + details, null);
    }

    /**
     * Создаёт ошибку превышения rate limit.
     *
     * @return ошибка rate limit
     */
    public static JsonRpcError rateLimitExceeded() {
        return new JsonRpcError(RATE_LIMIT_EXCEEDED, "Rate limit exceeded", null);
    }

    /**
     * Создаёт ошибку авторизации.
     *
     * @return ошибка отсутствия или неверного токена
     */
    public static JsonRpcError unauthorized() {
        return new JsonRpcError(UNAUTHORIZED, "Unauthorized", null);
    }

    /**
     * Создаёт ошибку прав доступа.
     *
     * @return ошибка недостаточных прав
     */
    public static JsonRpcError forbidden() {
        return new JsonRpcError(FORBIDDEN, "Forbidden", null);
    }
}
