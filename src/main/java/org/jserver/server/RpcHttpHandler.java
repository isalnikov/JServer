package org.jserver.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.jserver.api.JsonRpcError;
import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.jserver.core.HealthService;
import org.jserver.middleware.MiddlewareChain;
import org.jserver.server.HelpPageGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * HTTP обработчик JSON-RPC.
 * Принимает HTTP запросы, преобразует в JSON-RPC и передаёт в dispatcher.
 */
public class RpcHttpHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(RpcHttpHandler.class);
    private static final String CONTENT_TYPE = "application/json";

    private final MiddlewareChain chain;
    private final HealthService healthService;
    private final ObjectMapper objectMapper;

    /**
     * Создаёт обработчик.
     *
     * @param chain цепочка middleware
     * @param healthService сервис health check
     */
    public RpcHttpHandler(MiddlewareChain chain, HealthService healthService) {
        this.chain = chain;
        this.healthService = healthService;
        this.objectMapper = new ObjectMapper();
        logger.info("RpcHttpHandler created");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        try {
            switch (path) {
                case "/rpc" -> handleRpc(exchange);
                case "/health" -> handleHealth(exchange);
                case "/version" -> handleVersion(exchange);
                case "/help" -> handleHelp(exchange);
                default -> sendNotFound(exchange);
            }
        } catch (Exception e) {
            logger.error("Unhandled exception", e);
            sendJsonResponse(exchange, 500,
                JsonRpcResponse.error("2.0", JsonRpcError.internalError(e.getMessage()), null));
        } finally {
            exchange.close();
        }
    }

    private void handleRpc(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405,
                JsonRpcResponse.error("2.0", JsonRpcError.invalidRequest("POST required"), null));
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonRpcRequest request = parseJsonRpc(body);

        if (request == null) {
            sendJsonResponse(exchange, 400,
                JsonRpcResponse.error("2.0", JsonRpcError.parseError(), null));
            return;
        }

        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        var ctx = RequestContext.anonymous(ip, null, java.util.Set.of("anonymous"));

        chain.proceed(ctx, request).thenAccept(response -> {
            try {
                sendJsonResponse(exchange, 200, response);
            } catch (IOException e) {
                logger.error("Failed to send response", e);
            }
        }).join();
    }

    /**
     * Парсит JSON-RPC запрос из тела HTTP.
     *
     * @param body тело запроса
     * @return JSON-RPC запрос или null при ошибке
     */
    public JsonRpcRequest parseJsonRpc(String body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(body, Map.class);
            return new JsonRpcRequest(
                (String) map.get("jsonrpc"),
                (String) map.get("method"),
                map.get("params"),
                map.get("id"));
        } catch (Exception e) {
            logger.error("Failed to parse JSON-RPC request: {}", e.getMessage());
            return null;
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        Map<String, Object> health = healthService.getHealth();
        byte[] response = objectMapper.writeValueAsBytes(health);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void handleVersion(HttpExchange exchange) throws IOException {
        Map<String, String> version = healthService.getVersion();
        byte[] response = objectMapper.writeValueAsBytes(version);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void handleHelp(HttpExchange exchange) throws IOException {
        String html = HelpPageGenerator.generate();
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void sendNotFound(HttpExchange exchange) throws IOException {
        sendJsonResponse(exchange, 404,
            JsonRpcResponse.error("2.0", JsonRpcError.methodNotFound("path not found"), null));
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonRpcResponse response)
            throws IOException {
        byte[] body = objectMapper.writeValueAsBytes(response);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.getResponseHeaders().set("Content-Length", String.valueOf(body.length));
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
