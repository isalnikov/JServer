package org.jserver.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для RateLimitService.
 * Проверяют token bucket алгоритм.
 */
class RateLimitServiceTest {

    private RateLimitService service;

    @BeforeEach
    void setUp() {
        service = new RateLimitService(10, 10); // 10 tokens, 10/min
    }

    @Test
    void allowsRequestsUnderLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(service.tryConsume("test-ip"));
        }
    }

    @Test
    void blocksRequestsOverLimit() {
        for (int i = 0; i < 10; i++) {
            service.tryConsume("test-ip");
        }
        assertFalse(service.tryConsume("test-ip"));
    }

    @Test
    void differentIpsHaveIndependentLimits() {
        for (int i = 0; i < 10; i++) {
            service.tryConsume("ip1");
        }
        assertTrue(service.tryConsume("ip2"));
    }

    @Test
    void consumesExactlyAvailableTokens() {
        for (int i = 0; i < 10; i++) {
            assertTrue(service.tryConsume("test-ip"));
        }
        assertFalse(service.tryConsume("test-ip"));
    }
}
