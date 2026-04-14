package org.jserver.infrastructure;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Репозиторий конфигурации rate limiting.
 */
public class ConfigRepository {

    private static final Logger logger = LoggerFactory.getLogger(ConfigRepository.class);
    private final H2DataSource dataSource;

    /**
     * Создаёт репозиторий.
     *
     * @param dataSource источник данных
     */
    public ConfigRepository(H2DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Возвращает все конфигурации rate limiting.
     *
     * @return map method_name -> config
     */
    public Map<String, RateLimitConfig> getAllMethodConfigs() {
        Map<String, RateLimitConfig> configs = new HashMap<>();
        String sql = "SELECT * FROM rate_limit_config";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                configs.put(rs.getString("method_name"),
                    new RateLimitConfig(rs.getInt("capacity"), rs.getInt("refill_rate")));
            }
        } catch (SQLException e) {
            logger.error("Failed to load rate limit configs", e);
        }
        return configs;
    }

    /**
     * Сохраняет конфигурацию rate limiting для метода.
     *
     * @param method имя метода
     * @param capacity ёмкость
     * @param refillRate скорость пополнения
     */
    public void saveMethodConfig(String method, int capacity, int refillRate) {
        String sql = """
            MERGE INTO rate_limit_config (method_name, capacity, refill_rate)
            KEY (method_name) VALUES (?, ?, ?)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, method);
            stmt.setInt(2, capacity);
            stmt.setInt(3, refillRate);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save rate limit config for method: {}", method, e);
        }
    }

    /**
     * Конфигурация rate limiting для метода.
     *
     * @param capacity ёмкость
     * @param refillRate скорость пополнения
     */
    public record RateLimitConfig(int capacity, int refillRate) {}
}
