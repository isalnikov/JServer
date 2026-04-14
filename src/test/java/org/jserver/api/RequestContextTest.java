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
        var ctx = RequestContext.anonymous("127.0.0.1", null, Set.of());
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
        var ctx = RequestContext.anonymous("127.0.0.1", null, Set.of("anonymous"));
        assertNull(ctx.userId());
    }

    @Test
    void authenticatedUserHasUserId() {
        var userId = UUID.randomUUID();
        var ctx = RequestContext.anonymous("127.0.0.1", userId, Set.of("user"));
        assertEquals(userId, ctx.userId());
    }

    @Test
    void hasRoleChecksRoleMembership() {
        var ctx = RequestContext.anonymous("127.0.0.1", null, Set.of("user", "admin"));
        assertTrue(ctx.hasRole("admin"));
        assertFalse(ctx.hasRole("anonymous"));
    }
}
