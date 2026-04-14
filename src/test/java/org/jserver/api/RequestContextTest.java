package org.jserver.api;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для RequestContext.
 */
class RequestContextTest {

    @Test
    void createsContextWithRequestId() {
        var ctx = new RequestContext("127.0.0.1", null, Set.of(), null);
        assertNotNull(ctx.requestId());
    }

    @Test
    void createsContextWithProvidedRequestId() {
        var id = UUID.randomUUID().toString();
        var ctx = new RequestContext(id, "127.0.0.1", null, Set.of());
        assertEquals(id, ctx.requestId());
    }

    @Test
    void anonymousUserHasNullUserId() {
        var ctx = new RequestContext("127.0.0.1", null, Set.of("anonymous"), null);
        assertNull(ctx.userId());
    }

    @Test
    void authenticatedUserHasUserId() {
        var userId = UUID.randomUUID();
        var ctx = new RequestContext("127.0.0.1", userId, Set.of("user"), null);
        assertEquals(userId, ctx.userId());
    }

    @Test
    void hasRoleChecksRoleMembership() {
        var ctx = new RequestContext("127.0.0.1", null, Set.of("user", "admin"), null);
        assertTrue(ctx.hasRole("admin"));
        assertFalse(ctx.hasRole("anonymous"));
    }
}
