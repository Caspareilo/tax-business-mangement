package com.example.tax.support;

import com.example.tax.util.Db;
import org.h2.tools.RunScript;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates a fresh in-memory H2 database (MySQL compatibility mode) per test.
 * Production code still targets MySQL 8; Db reads these values as system properties.
 */
public final class TestDatabase {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private TestDatabase() {
    }

    public static void reset() {
        String databaseName = "tax_test_" + COUNTER.incrementAndGet();
        String url = "jdbc:h2:mem:" + databaseName
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty("tax.db.url", url);
        System.setProperty("tax.db.username", "sa");
        System.setProperty("tax.db.password", "");
        System.setProperty("tax.db.driver", "org.h2.Driver");
        System.setProperty("tax.db.pool.maximum", "4");
        System.setProperty("tax.db.pool.minimum", "1");
        Db.resetForTests();
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            run(connection, "schema-h2.sql");
            run(connection, "seed-h2.sql");
        } catch (SQLException e) {
            throw new IllegalStateException("无法初始化测试数据库", e);
        }
    }

    public static void execute(String sql, Object... params) {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("测试数据准备失败", e);
        }
    }

    public static long count(String sql, Object... params) {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (java.sql.ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("测试查询失败", e);
        }
    }

    private static void run(Connection connection, String resource) throws SQLException {
        try (Reader reader = new InputStreamReader(
                TestDatabase.class.getClassLoader().getResourceAsStream(resource),
                StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("无法读取测试脚本 " + resource, e);
        }
    }
}
