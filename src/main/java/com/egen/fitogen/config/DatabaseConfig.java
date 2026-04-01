package com.egen.fitogen.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String DB_FILE_NAME = "fitogen.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE_NAME;

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static String getDatabaseUrl() {
        return DB_URL;
    }

    public static String getDatabaseFileName() {
        return DB_FILE_NAME;
    }

    public static Path getDatabaseFilePath() {
        return Paths.get(DB_FILE_NAME).toAbsolutePath().normalize();
    }
}