package org.jserver.infrastructure;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jserver.model.AuditEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Репозиторий audit-записей.
 */
public class AuditRepository {

    private static final Logger logger = LoggerFactory.getLogger(AuditRepository.class);
    private final H2DataSource dataSource;

    /**
     * Создаёт репозиторий.
     *
     * @param dataSource источник данных
     */
    public AuditRepository(H2DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Сохраняет audit-запись.
     *
     * @param entry запись для сохранения
     */
    public void save(AuditEntry entry) {
        String sql = """
            INSERT INTO audit_log (id, timestamp, action, user_id, details, ip_address)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, entry.id());
            stmt.setTimestamp(2, Timestamp.from(entry.timestamp()));
            stmt.setString(3, entry.action());
            stmt.setObject(4, entry.userId());
            stmt.setString(5, entry.details());
            stmt.setString(6, entry.ipAddress());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save audit entry", e);
        }
    }

    /**
     * Возвращает последние audit-записи.
     *
     * @param limit максимальное количество записей
     * @return список записей
     */
    public List<AuditEntry> findRecent(int limit) {
        String sql = "SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT ?";
        List<AuditEntry> entries = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to query audit entries", e);
        }
        return entries;
    }

    private AuditEntry mapRow(ResultSet rs) throws SQLException {
        return new AuditEntry(
            rs.getObject("id", UUID.class),
            rs.getTimestamp("timestamp").toInstant(),
            rs.getString("action"),
            rs.getObject("user_id", UUID.class),
            rs.getString("details"),
            rs.getString("ip_address"));
    }
}
