package org.jserver.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Провайдер JWT-токенов.
 * Генерирует и валидирует access/refresh токены.
 * Использует HS256 алгоритм.
 */
public class JwtProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtProvider.class);

    private final String secret;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    /**
     * Создаёт провайдер.
     *
     * @param secret секретный ключ (минимум 256 бит)
     * @param accessTokenTtl время жизни access токена
     * @param refreshTokenTtl время жизни refresh токена
     */
    public JwtProvider(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
        this.secret = secret;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        logger.info("JwtProvider created with accessTtl={}, refreshTtl={}",
            accessTokenTtl, refreshTokenTtl);
    }

    /**
     * Генерирует access токен.
     *
     * @param userId идентификатор пользователя
     * @param roles роли пользователя
     * @return JWT токен
     */
    public String generateAccessToken(UUID userId, Set<String> roles) {
        return createToken(userId, "access", roles, accessTokenTtl);
    }

    /**
     * Генерирует refresh токен.
     *
     * @param userId идентификатор пользователя
     * @return JWT токен
     */
    public String generateRefreshToken(UUID userId) {
        return createToken(userId, "refresh", Set.of(), refreshTokenTtl);
    }

    /**
     * Валидирует токен и возвращает claims.
     *
     * @param token JWT токен
     * @return claims или null при неверном токене
     */
    public TokenClaims validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!parts[2].equals(expectedSignature)) {
                return null;
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String[] payloadParts = payload.split(",");
            if (payloadParts.length < 3) {
                return null;
            }

            UUID userId = UUID.fromString(payloadParts[0]);
            String type = payloadParts[1];
            long exp = Long.parseLong(payloadParts[2]);

            if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                return null;
            }

            Set<String> roles = payloadParts.length > 3
                ? Set.of(payloadParts[3].split("\\|"))
                : Set.of();

            return new TokenClaims(userId, type, roles, Instant.ofEpochSecond(exp));

        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Создаёт хеш refresh токена для хранения в БД.
     *
     * @param token raw токен
     * @return SHA-256 хеш
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    /**
     * Возвращает время жизни access токена.
     *
     * @return TTL access токена
     */
    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    private String createToken(UUID userId, String type, Set<String> roles, Duration ttl) {
        String header = Base64.getUrlEncoder().encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

        Instant exp = Instant.now().plus(ttl);
        String rolesStr = String.join("|", roles);
        String payload = userId.toString() + "," + type + "," + exp.getEpochSecond()
            + (rolesStr.isEmpty() ? "" : "," + rolesStr);
        String encodedPayload = Base64.getUrlEncoder()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        String signature = sign(header + "." + encodedPayload);
        return header + "." + encodedPayload + "." + signature;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign token", e);
        }
    }

    /**
     * Claims из валидированного токена.
     */
    public record TokenClaims(
        UUID userId,
        String type,
        Set<String> roles,
        Instant expiresAt
    ) {}
}
