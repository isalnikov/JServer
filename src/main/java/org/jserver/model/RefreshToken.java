package org.jserver.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Модель refresh-токена.
 * Хранит хеш токена, связь с пользователем и время истечения.
 *
 * @param tokenHash SHA-256 хеш refresh-токена
 * @param userId идентификатор пользователя
 * @param expiresAt время истечения токена
 */
public record RefreshToken(
    String tokenHash,
    UUID userId,
    Instant expiresAt
) {}
