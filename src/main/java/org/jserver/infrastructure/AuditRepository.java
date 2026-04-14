package org.jserver.infrastructure;

import java.util.ArrayList;
import java.util.List;
import org.jserver.model.AuditEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Репозиторий audit-записей (stub — полная реализация в Task 6).
 */
public class AuditRepository {

    private static final Logger logger = LoggerFactory.getLogger(AuditRepository.class);

    /**
     * Создаёт репозиторий.
     *
     * @param dataSource источник данных (используется в полной реализации)
     */
    public AuditRepository(H2DataSource dataSource) {
        logger.debug("AuditRepository created (stub)");
    }

    /**
     * Сохраняет audit-запись (stub — логирует только).
     *
     * @param entry запись для сохранения
     */
    public void save(AuditEntry entry) {
        logger.debug("Audit entry saved (stub): action={}", entry.action());
    }

    /**
     * Возвращает последние audit-записи (stub — возвращает пустой список).
     *
     * @param limit максимальное количество записей
     * @return пустой список (stub)
     */
    public List<AuditEntry> findRecent(int limit) {
        return new ArrayList<>();
    }
}
