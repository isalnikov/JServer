package org.jserver.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Источник данных H2.
 * Создаёт подключение и инициализирует схему БД.
 */
public class H2DataSource {

    private static final Logger logger = LoggerFactory.getLogger(H2DataSource.class);

    private final JdbcDataSource dataSource;
    private final ServerConfig config;

    /**
     * Создаёт источник данных.
     *
     * @param config конфигурация сервера
     */
    public H2DataSource(ServerConfig config) {
        this.config = config;
        this.dataSource = createDataSource();
    }

    /**
     * Инициализирует схему БД.
     * Создаёт все необходимые таблицы.
     */
    public void initialize() {
        logger.info("Initializing database schema");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id UUID PRIMARY KEY,
                    username VARCHAR(255) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    roles VARCHAR(500) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id UUID PRIMARY KEY,
                    timestamp TIMESTAMP NOT NULL,
                    action VARCHAR(255) NOT NULL,
                    user_id UUID,
                    details TEXT,
                    ip_address VARCHAR(45)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rate_limit_config (
                    method_name VARCHAR(255) PRIMARY KEY,
                    capacity INT NOT NULL,
                    refill_rate INT NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS refresh_tokens (
                    token_hash VARCHAR(64) PRIMARY KEY,
                    user_id UUID NOT NULL,
                    expires_at TIMESTAMP NOT NULL
                )
                """);

            // Создаём пользователя admin по умолчанию
            stmt.execute("""
                MERGE INTO users (id, username, password_hash, roles, created_at, updated_at)
                KEY (username) VALUES (
                    RANDOM_UUID(), 'admin',
                    '$2a$10$dummyHashForDevelopmentOnly',
                    'admin', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
                )
                """);

            logger.info("Database schema initialized");

        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Получает подключение к БД.
     *
     * @return подключение
     * @throws SQLException ошибка подключения
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Возвращает DataSource для использования в репозиториях.
     *
     * @return DataSource
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    private JdbcDataSource createDataSource() {
        var ds = new JdbcDataSource();
        ds.setURL(config.databaseUrl());
        ds.setUser(config.databaseUser());
        ds.setPassword(config.databasePassword());
        logger.info("H2 DataSource created: {}", config.databaseUrl());
        return ds;
    }
}
