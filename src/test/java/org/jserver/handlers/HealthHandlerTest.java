package org.jserver.handlers;

import org.jserver.api.JsonRpcRequest;
import org.jserver.api.JsonRpcResponse;
import org.jserver.api.RequestContext;
import org.jserver.core.HealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для HealthHandler.
 */
class HealthHandlerTest {

    private HealthHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HealthHandler(new HealthService());
    }

    @Test
    void handlesSystemHealth() {
        var request = new JsonRpcRequest("2.0", "system.health", null, 1);
        var ctx = new RequestContext("req-1", "127.0.0.1", null, Set.of());
        var response = handler.handle(request, ctx).join();

        assertNotNull(response.result());
        assertNull(response.error());
    }

    @Test
    void handlesSystemVersion() {
        var request = new JsonRpcRequest("2.0", "system.version", null, 1);
        var ctx = new RequestContext("req-1", "127.0.0.1", null, Set.of());
        var response = handler.handle(request, ctx).join();

        assertNotNull(response.result());
        assertNull(response.error());
    }

    @Test
    void returnsMethodNotFoundForUnknownMethod() {
        var request = new JsonRpcRequest("2.0", "system.unknown", null, 1);
        var ctx = new RequestContext("req-1", "127.0.0.1", null, Set.of());
        var response = handler.handle(request, ctx).join();

        assertNotNull(response.error());
        assertNull(response.result());
    }
}
