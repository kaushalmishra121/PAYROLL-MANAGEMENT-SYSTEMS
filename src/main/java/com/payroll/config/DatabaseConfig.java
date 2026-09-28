package com.payroll.config;

import com.payroll.exception.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    private DatabaseConfig() {
        // Private constructor for singleton utility class
    }

    public static synchronized DataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            initDataSource();
        }
        return dataSource;
    }

    private static void initDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            
            String driver = AppConfig.get("db.driver", "com.mysql.cj.jdbc.Driver");
            String url = AppConfig.get("db.url");
            String username = AppConfig.get("db.username");
            String password = AppConfig.get("db.password");

            config.setDriverClassName(driver);
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);

            // Pool configuration
            config.setMaximumPoolSize(AppConfig.getInt("hikari.maximumPoolSize", 10));
            config.setMinimumIdle(AppConfig.getInt("hikari.minimumIdle", 2));
            config.setIdleTimeout(AppConfig.getLong("hikari.idleTimeout", 30000));
            config.setConnectionTimeout(AppConfig.getLong("hikari.connectionTimeout", 10000));
            config.setMaxLifetime(AppConfig.getLong("hikari.maxLifetime", 1800000));
            config.setPoolName(AppConfig.get("hikari.poolName", "PayrollHikariPool"));

            // MySQL optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            dataSource = new HikariDataSource(config);
            logger.info("HikariCP DataSource initialized successfully for URL: {}", url);
        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new DatabaseException("Could not connect to database. Please check MySQL server and db.properties credentials.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static synchronized void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP DataSource closed.");
        }
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            logger.error("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }
}
