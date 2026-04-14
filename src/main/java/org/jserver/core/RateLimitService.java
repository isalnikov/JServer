package org.jserver.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис rate limiting на основе token bucket.
 * Потокобезопасный, хранит buckets per IP.
 */
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    
    private final int defaultCapacity;
    private final int defaultRefillRate;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Создаёт сервис с настройками по умолчанию.
     *
     * @param defaultCapacity ёмкость bucket
     * @param defaultRefillRate скорость пополнения (токенов/минуту)
     */
    public RateLimitService(int defaultCapacity, int defaultRefillRate) {
        this.defaultCapacity = defaultCapacity;
        this.defaultRefillRate = defaultRefillRate;
        logger.info("RateLimitService created: capacity={}, refillRate={}/min", 
            defaultCapacity, defaultRefillRate);
    }

    /**
     * Пытается потребить токен для IP.
     *
     * @param clientIp IP-адрес клиента
     * @return true если токен доступен, false если лимит исчерпан
     */
    public boolean tryConsume(String clientIp) {
        var bucket = buckets.computeIfAbsent(clientIp, 
            ip -> new TokenBucket(defaultCapacity, defaultRefillRate));
        
        boolean consumed = bucket.tryConsume();
        if (!consumed) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
        }
        return consumed;
    }

    /**
     * Token bucket с refill.
     */
    private static class TokenBucket {
        private final int capacity;
        private final long refillIntervalNanos;
        private int tokens;
        private volatile long lastRefillTime;

        TokenBucket(int capacity, int refillRatePerMinute) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillIntervalNanos = 60_000_000_000L / refillRatePerMinute;
            this.lastRefillTime = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillTime;
            if (elapsed >= refillIntervalNanos) {
                int tokensToAdd = (int) (elapsed / refillIntervalNanos);
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }
}
