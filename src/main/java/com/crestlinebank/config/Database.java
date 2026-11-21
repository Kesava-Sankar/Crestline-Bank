package com.crestlinebank.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {
    private static final Properties CONFIG = loadConfig();

    private static final String URL = setting(
            "CRESTLINE_DB_URL", "db.url",
            "jdbc:mysql://localhost:3306/crestline_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String USER = setting("CRESTLINE_DB_USER", "db.user", "root");
    private static final String PASSWORD = setting("CRESTLINE_DB_PASSWORD", "db.password", "");

    private Database() {}

    private static Properties loadConfig() {
        Properties properties = new Properties();
        Path configPath = Path.of("config.properties");
        if (Files.isRegularFile(configPath)) {
            try (InputStream input = Files.newInputStream(configPath)) {
                properties.load(input);
            } catch (IOException ex) {
                throw new ExceptionInInitializerError(
                        "Could not read project config.properties: " + ex.getMessage());
            }
        }
        return properties;
    }

    private static String setting(String environmentKey, String propertyKey, String fallback) {
        String environmentValue = System.getenv(environmentKey);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        return CONFIG.getProperty(propertyKey, fallback).trim();
    }

    public static Connection getConnection() throws SQLException {
        if (PASSWORD.isBlank()) {
            throw new SQLException(
                    "MySQL password is missing. Add db.password to the project's config.properties file.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
