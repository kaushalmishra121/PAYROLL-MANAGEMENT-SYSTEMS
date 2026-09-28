package com.payroll.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        // Priority 1: External config file if provided via system property
        String externalConfig = System.getProperty("app.config.file");
        if (externalConfig != null && new File(externalConfig).exists()) {
            try (InputStream input = new FileInputStream(externalConfig)) {
                properties.load(input);
                logger.info("Loaded configuration from external file: {}", externalConfig);
                return;
            } catch (Exception e) {
                logger.warn("Failed to load external config file {}. Falling back to classpath.", externalConfig, e);
            }
        }

        // Priority 2: Classpath resource
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("config/db.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("Loaded configuration from classpath config/db.properties");
            } else {
                logger.warn("db.properties not found in classpath. Applying built-in defaults.");
                applyDefaults();
            }
        } catch (Exception e) {
            logger.error("Error reading db.properties. Using default settings.", e);
            applyDefaults();
        }
    }

    private static void applyDefaults() {
        properties.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        properties.setProperty("db.url", "jdbc:mysql://localhost:3306/payroll_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true");
        properties.setProperty("db.username", "root");
        properties.setProperty("db.password", "root");
        properties.setProperty("hikari.maximumPoolSize", "10");
        properties.setProperty("hikari.minimumIdle", "2");
        properties.setProperty("hikari.idleTimeout", "30000");
        properties.setProperty("hikari.connectionTimeout", "10000");
        properties.setProperty("hikari.maxLifetime", "1800000");
        properties.setProperty("hikari.poolName", "PayrollHikariPool");
    }

    public static String get(String key) {
        // Check system property first (e.g. -Ddb.password=...)
        String systemProp = System.getProperty(key);
        if (systemProp != null && !systemProp.isBlank()) {
            return systemProp;
        }
        // Check environment variable (e.g. DB_PASSWORD for db.password)
        String envKey = key.replace('.', '_').toUpperCase();
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        String val = get(key);
        return val != null ? val : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key);
        if (val != null) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer for key {}: {}. Using default: {}", key, val, defaultValue);
            }
        }
        return defaultValue;
    }

    public static long getLong(String key, long defaultValue) {
        String val = get(key);
        if (val != null) {
            try {
                return Long.parseLong(val.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid long for key {}: {}. Using default: {}", key, val, defaultValue);
            }
        }
        return defaultValue;
    }
}
