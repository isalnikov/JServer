package org.jserver.core;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jserver.infrastructure.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис аутентификации.
 * Управляет login, logout, refresh токенов.
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final JwtProvider jwtProvider;
    private final AuditService auditService;

    /**
     * Создаёт сервис.
     *
     * @param jwtProvider провайдер JWT
     * @param auditService сервис аудита
     * @param userRepository репозиторий пользователей (для будущей реализации)
     */
    public AuthService(JwtProvider jwtProvider, AuditService auditService,
                       Object userRepository) {
        this.jwtProvider = jwtProvider;
        this.auditService = auditService;
        logger.info("AuthService created");
    }

    /**
     * Выполняет вход пользователя.
     *
     * @param username имя пользователя
     * @param password пароль
     * @return результат с токенами
     */
    public LoginResult login(String username, String password) {
        logger.info("Login attempt for user: {}", username);

        // TODO: реализовать проверку пароля через UserRepository
        // Для заглушки всегда возвращаем токены для admin
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of("admin");

        String accessToken = jwtProvider.generateAccessToken(userId, roles);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        auditService.logAction("auth.login", userId,
            "User " + username + " logged in", null);

        logger.info("User {} logged in successfully", username);
        return new LoginResult(accessToken, refreshToken,
            Instant.now().plus(jwtProvider.accessTokenTtl()));
    }

    /**
     * Выполняет выход пользователя.
     *
     * @param accessToken access токен
     */
    public void logout(String accessToken) {
        var claims = jwtProvider.validateToken(accessToken);
        if (claims != null) {
            auditService.logAction("auth.logout", claims.userId(),
                "User logged out", null);
            logger.info("User {} logged out", claims.userId());
        }
    }

    /**
     * Обновляет access токен по refresh.
     *
     * @param refreshToken refresh токен
     * @return новый набор токенов или null
     */
    public LoginResult refresh(String refreshToken) {
        var claims = jwtProvider.validateToken(refreshToken);
        if (claims == null || !"refresh".equals(claims.type())) {
            logger.warn("Invalid refresh token");
            return null;
        }

        Set<String> roles = claims.roles().isEmpty() ? Set.of("user") : claims.roles();
        String newAccessToken = jwtProvider.generateAccessToken(claims.userId(), roles);
        String newRefreshToken = jwtProvider.generateRefreshToken(claims.userId());

        auditService.logAction("auth.refresh", claims.userId(),
            "Token refreshed", null);

        return new LoginResult(newAccessToken, newRefreshToken,
            Instant.now().plus(jwtProvider.accessTokenTtl()));
    }

    /**
     * Результат входа.
     */
    public record LoginResult(
        String accessToken,
        String refreshToken,
        Instant expiresAt
    ) {
        /**
         * Преобразует результат в map.
         *
         * @return map с токенами и временем истечения
         */
        public Map<String, Object> toMap() {
            return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "expiresAt", expiresAt.toString()
            );
        }
    }
}
