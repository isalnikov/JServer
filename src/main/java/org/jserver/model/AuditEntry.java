package org.jserver.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Запись в audit-логе.
 * Фиксирует действия пользователей в системе.
 *
 * @param id уникальный идентификатор записи
 * @param timestamp время действия
 * @param action выполненное действие (например "auth.login")
 * @param userId идентификатор пользователя (null для анонимных действий)
 * @param details дополнительные детали действия
 * @param ipAddress IP-адрес источника запроса
 */
public record AuditEntry(
    UUID id,
    Instant timestamp,
    String action,
    UUID userId,
    String details,
    String ipAddress
) {}
