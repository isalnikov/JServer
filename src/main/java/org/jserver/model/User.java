package org.jserver.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Доменная модель пользователя.
 * Хранит идентификатор, имя, хеш пароля и роли.
 *
 * @param id уникальный идентификатор пользователя
 * @param username имя пользователя (уникальное)
 * @param passwordHash хеш пароля
 * @param roles набор ролей пользователя
 * @param createdAt время создания записи
 * @param updatedAt время последнего обновления
 */
public record User(
    UUID id,
    String username,
    String passwordHash,
    Set<String> roles,
    Instant createdAt,
    Instant updatedAt
) {}
