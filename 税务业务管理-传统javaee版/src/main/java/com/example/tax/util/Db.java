package com.example.tax.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Lazily initialised HikariCP data source.
 *
 * Connection settings are resolved in this order:
 * 1. JVM system properties (tax.db.url, tax.db.username, tax.db.password, tax.db.driver)
 * 2. environment variables (TAX_DB_URL, TAX_DB_USERNAME, TAX_DB_PASSWORD, TAX_DB_DRIVER)
 * 3. classpath db.properties (copy db.properties.example, never commit real credentials)
 *
 * Initialisation is lazy so that pages which do not touch the database (login page,
 * error pages) still render when the database is temporarily unavailable.
 */
public final class Db {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/tax_demo"
            + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
            + "&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    private static volatile HikariDataSource dataSource;

    private Db() {
    }

    public static Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }

    public static boolean isConfigured() {
        return dataSource != null;
    }

    private static HikariDataSource dataSource() {
        HikariDataSource local = dataSource;
        if (local == null) {
            synchronized (Db.class) {
                local = dataSource;
                if (local == null) {
                    local = buildDataSource();
                    dataSource = local;
                }
            }
        }
        return local;
    }

    private static HikariDataSource buildDataSource() {
        Properties fileProps = loadProperties();
        String url = resolve("tax.db.url", "TAX_DB_URL", "db.url", fileProps, DEFAULT_URL);
        String username = resolve("tax.db.username", "TAX_DB_USERNAME", "db.username",
                fileProps, "root");
        String password = resolve("tax.db.password", "TAX_DB_PASSWORD", "db.password",
                fileProps, "");
        String driver = resolve("tax.db.driver", "TAX_DB_DRIVER", "db.driver",
                fileProps, DEFAULT_DRIVER);
        int maximumPoolSize = parseInt(resolve("tax.db.pool.maximum",
                "TAX_DB_POOL_MAXIMUM", "db.pool.maximum", fileProps, "10"), 10);
        int minimumIdle = parseInt(resolve("tax.db.pool.minimum",
                "TAX_DB_POOL_MINIMUM", "db.pool.minimum", fileProps, "2"), 2);

        HikariConfig config = new HikariConfig();
        config.setPoolName("tax-db-pool");
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if (!driver.isEmpty()) {
            config.setDriverClassName(driver);
        }
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(Math.min(minimumIdle, maximumPoolSize));
        config.setConnectionTimeout(10_000L);
        config.setValidationTimeout(5_000L);
        config.setIdleTimeout(300_000L);
        config.setMaxLifetime(1_800_000L);
        return new HikariDataSource(config);
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = Db.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("无法读取数据库配置 db.properties", e);
        }
        return props;
    }

    private static String resolve(String systemKey, String environmentKey, String fileKey,
                                  Properties props, String defaultValue) {
        String value = System.getProperty(systemKey);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(environmentKey);
        }
        if (value == null || value.trim().isEmpty()) {
            value = props.getProperty(fileKey);
        }
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Test hook: closes and forgets the pool so the next call re-reads configuration. */
    public static synchronized void resetForTests() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
