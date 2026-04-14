package org.jserver.infrastructure;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для H2DataSource.
 */
class H2DataSourceTest {

    private static ServerConfig testConfig(String dbSuffix) {
        return new ServerConfig(8080, "0.0.0.0",
            "jdbc:h2:mem:jserver_test_" + dbSuffix + ";DB_CLOSE_DELAY=-1", "sa", "",
            "test-secret",
            Duration.ofMinutes(15), Duration.ofDays(7),
            true, 100, 100);
    }

    @Test
    void createsDataSource() {
        var config = testConfig("create");
        var ds = new H2DataSource(config);
        assertNotNull(ds);
    }

    @Test
    void opensConnection() throws SQLException {
        var config = testConfig("open");
        var ds = new H2DataSource(config);
        
        try (Connection conn = ds.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        }
    }

    @Test
    void initializesSchema() throws SQLException {
        var config = testConfig("init");
        var ds = new H2DataSource(config);
        ds.initialize();
        
        try (Connection conn = ds.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                 "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'PUBLIC' AND table_type = 'BASE TABLE'")) {
            assertTrue(rs.next());
            int tableCount = rs.getInt(1);
            assertTrue(tableCount >= 4, "Expected at least 4 tables, got: " + tableCount);
        }
    }
}
