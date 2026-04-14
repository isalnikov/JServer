package org.jserver.core;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jserver.infrastructure.AuditRepository;
import org.jserver.model.AuditEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис аудита действий (stub — полная реализация в Task 6).
 */
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository repository;

    /**
     * Создаёт сервис аудита.
     *
     * @param repository репозиторий audit-записей
     */
    public AuditService(AuditRepository repository) {
        this.repository = repository;
        logger.info("AuditService created");
    }

    /**
     * Логирует действие пользователя.
     *
     * @param action выполненное действие
     * @param userId идентификатор пользователя
     * @param details дополнительные детали
     * @param ipAddress IP-адрес источника запроса
     */
    public void logAction(String action, UUID userId, String details, String ipAddress) {
        var entry = new AuditEntry(
            UUID.randomUUID(), Instant.now(), action, userId, details, ipAddress);
        repository.save(entry);
        logger.debug("Audit logged: action={} userId={}", action, userId);
    }

    /**
     * Возвращает последние audit-записи.
     *
     * @param limit максимальное количество записей
     * @return список записей
     */
    public List<AuditEntry> getRecentEntries(int limit) {
        return repository.findRecent(limit);
    }
}
