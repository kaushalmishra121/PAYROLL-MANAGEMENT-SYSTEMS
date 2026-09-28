package com.payroll.config;

import com.payroll.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    public static void initializeDatabaseIfRequired() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            if (!isTableExisting(conn, "users") || !isTableExisting(conn, "employees")) {
                logger.info("Core database tables not detected. Running schema and initial seed setup...");
                runSqlScript(conn, "database/schema.sql");
                runSqlScript(conn, "database/seed.sql");
                logger.info("Database initialized successfully with schema and seed data.");
            } else {
                logger.info("Database schema already verified and active.");
            }
        } catch (Exception e) {
            logger.warn("Auto-initialization check encountered an issue: {}. Proceeding with existing tables.", e.getMessage());
        }
    }

    private static boolean isTableExisting(Connection conn, String tableName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
            // Case-insensitive check
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("Table check failed for {}: {}", tableName, e.getMessage());
        }
        return false;
    }

    public static void runSqlScript(Connection conn, String scriptPath) {
        InputStream is = null;
        try {
            // Check filesystem first
            File file = new File(scriptPath);
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                // Check classpath
                is = DatabaseInitializer.class.getClassLoader().getResourceAsStream(scriptPath);
            }

            if (is == null) {
                logger.warn("SQL script not found at {}. Skipping script execution.", scriptPath);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                 Statement stmt = conn.createStatement()) {
                
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.startsWith("//") || trimmed.isEmpty() || trimmed.startsWith("/*")) {
                        continue;
                    }
                    sb.append(line).append("\n");
                    if (trimmed.endsWith(";")) {
                        String sql = sb.toString().trim();
                        // Strip trailing semicolon
                        sql = sql.substring(0, sql.length() - 1).trim();
                        if (!sql.isEmpty() && !sql.toLowerCase().startsWith("use ") && !sql.toLowerCase().startsWith("create database")) {
                            try {
                                stmt.execute(sql);
                            } catch (Exception sqlEx) {
                                logger.debug("Statement execution note: {} - {}", sqlEx.getMessage(), sql);
                            }
                        }
                        sb.setLength(0);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error executing SQL script {}: {}", scriptPath, e.getMessage(), e);
            throw new DatabaseException("Failed to run script: " + scriptPath, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
