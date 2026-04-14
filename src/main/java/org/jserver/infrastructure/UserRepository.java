package org.jserver.infrastructure;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.jserver.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Репозиторий пользователей.
 */
public class UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private final H2DataSource dataSource;

    /**
     * Создаёт репозиторий.
     *
     * @param dataSource источник данных
     */
    public UserRepository(H2DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Находит пользователя по имени.
     *
     * @param username имя пользователя
     * @return Optional с пользователем
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find user by username: {}", username, e);
        }
        return Optional.empty();
    }

    /**
     * Находит пользователя по идентификатору.
     *
     * @param id идентификатор
     * @return Optional с пользователем
     */
    public Optional<User> findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find user by id: {}", id, e);
        }
        return Optional.empty();
    }

    /**
     * Сохраняет пользователя.
     *
     * @param user пользователь
     */
    public void save(User user) {
        String sql = """
            MERGE INTO users (id, username, password_hash, roles, created_at, updated_at)
            KEY (id) VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, user.id());
            stmt.setString(2, user.username());
            stmt.setString(3, user.passwordHash());
            stmt.setString(4, String.join(",", user.roles()));
            stmt.setTimestamp(5, Timestamp.from(user.createdAt()));
            stmt.setTimestamp(6, Timestamp.from(user.updatedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save user: {}", user.username(), e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String rolesStr = rs.getString("roles");
        Set<String> roles = rolesStr != null && !rolesStr.isEmpty()
            ? Set.of(rolesStr.split(","))
            : Set.of();
        return new User(
            rs.getObject("id", UUID.class),
            rs.getString("username"),
            rs.getString("password_hash"),
            roles,
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
    }
}
