package org.jserver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты полного цикла.
 * Требуют запущенного сервера на порту 8080.
 */
class IntegrationTest {

    private static HttpClient client;

    @BeforeAll
    static void setUp() {
        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Test
    @Disabled("Requires running server on port 8080")
    void systemHealthReturnsOk() throws Exception {
        String body = """
            {"jsonrpc":"2.0","method":"system.health","params":null,"id":1}
            """;
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/rpc"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("healthy"));
    }

    @Test
    @Disabled("Requires running server on port 8080")
    void healthEndpointReturnsOk() throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/health"))
            .GET()
            .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("healthy"));
    }

    @Test
    @Disabled("Requires running server on port 8080")
    void versionEndpointReturnsOk() throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/version"))
            .GET()
            .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("version"));
    }
}
