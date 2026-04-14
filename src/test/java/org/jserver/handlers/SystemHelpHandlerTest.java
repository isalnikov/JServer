package org.jserver.handlers;

import org.jserver.api.JsonRpcRequest;
import org.jserver.api.RpcDispatcher;
import org.jserver.api.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для SystemHelpHandler.
 */
class SystemHelpHandlerTest {

    private SystemHelpHandler handler;

    @BeforeEach
    void setUp() {
        var dispatcher = new RpcDispatcher();
        dispatcher.register("method.a", (req, ctx) -> null);
        dispatcher.register("method.b", (req, ctx) -> null);
        handler = new SystemHelpHandler(dispatcher);
    }

    @Test
    void returnsSortedMethodList() {
        var request = new JsonRpcRequest("2.0", "system.help", null, 1);
        var ctx = RequestContext.anonymous("127.0.0.1", null, Set.of());
        var response = handler.handle(request, ctx).join();

        assertNotNull(response.result());
        assertNull(response.error());
        @SuppressWarnings("unchecked")
        var result = (java.util.Map<String, java.util.List<String>>) response.result();
        var methods = result.get("methods");
        assertTrue(methods.contains("method.a"));
        assertTrue(methods.contains("method.b"));
        // Проверяем сортировку
        assertEquals("method.a", methods.get(0));
        assertEquals("method.b", methods.get(1));
    }
}
