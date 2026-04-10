package com.egen.fitogen.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.prefs.Preferences;

public final class DatabaseConfig {

    private static final String LEGACY_DB_FILE_NAME = "fitogen.db";
    private static final String PREF_NODE = "com/egen/fitogen/database";
    private static final String PREF_DB_PATH = "db.path";
    private static final String PREF_DB_LABEL = "db.label";
    private static final String PREF_DB_TEST = "db.test";
    private static final String APP_DIRECTORY_NAME = ".fitogen-essentials";

    private static final Preferences PREFERENCES = Preferences.userRoot().node(PREF_NODE);
    private static volatile Path currentDatabaseFilePath = resolveInitialDatabaseFilePath();
    private static volatile String currentProfileLabel = resolveInitialProfileLabel(currentDatabaseFilePath);
    private static volatile boolean currentTestProfile = PREFERENCES.getBoolean(PREF_DB_TEST, false);

    private DatabaseConfig() {
    }

    public static synchronized Connection getConnection() throws SQLException {
        ensureParentDirectoryExists(currentDatabaseFilePath);
        return DriverManager.getConnection(buildUrl(currentDatabaseFilePath));
    }

    public static synchronized Connection getConnection(Path databaseFilePath) throws SQLException {
        ensureParentDirectoryExists(databaseFilePath);
        return DriverManager.getConnection(buildUrl(databaseFilePath));
    }

    public static synchronized void activateDatabase(Path databaseFilePath, String profileLabel, boolean testProfile) {
        Path normalized = normalize(databaseFilePath);
        ensureParentDirectoryExists(normalized);
        currentDatabaseFilePath = normalized;
        currentProfileLabel = normalizeLabel(profileLabel, normalized);
        currentTestProfile = testProfile;
        PREFERENCES.put(PREF_DB_PATH, normalized.toString());
        PREFERENCES.put(PREF_DB_LABEL, currentProfileLabel);
        PREFERENCES.putBoolean(PREF_DB_TEST, currentTestProfile);
    }

    public static String getDatabaseUrl() {
        return buildUrl(currentDatabaseFilePath);
    }

    public static String getDatabaseFileName() {
        return currentDatabaseFilePath.getFileName().toString();
    }

    public static Path getDatabaseFilePath() {
        return currentDatabaseFilePath;
    }

    public static String getCurrentProfileLabel() {
        return currentProfileLabel;
    }

    public static boolean isUsingTestDatabase() {
        return currentTestProfile;
    }

    public static Path getApplicationDataDirectory() {
        Path root = Paths.get(System.getProperty("user.home"), APP_DIRECTORY_NAME).toAbsolutePath().normalize();
        ensureParentDirectoryExists(root.resolve("placeholder.tmp"));
        return root;
    }

    public static Path getProfilesDirectory() {
        Path profilesDirectory = getApplicationDataDirectory().resolve("profiles");
        try {
            Files.createDirectories(profilesDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się utworzyć katalogu profili baz danych: " + profilesDirectory, e);
        }
        return profilesDirectory;
    }

    public static Path buildNamedProfilePath(String profileName) {
        String sanitized = sanitizeProfileName(profileName);
        return getProfilesDirectory().resolve(sanitized + ".db");
    }

    public static Path getTestProfilePath() {
        return getProfilesDirectory().resolve("baza_testowa.db");
    }

    public static String sanitizeProfileName(String profileName) {
        String raw = profileName == null ? "" : profileName.trim();
        if (raw.isBlank()) {
            return "nowa_baza";
        }

        String sanitized = raw
                .replaceAll("[\\\\/:*?\"<>|]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .replace(' ', '_');

        return sanitized.isBlank() ? "nowa_baza" : sanitized;
    }

    private static Path resolveInitialDatabaseFilePath() {
        String preferredPath = PREFERENCES.get(PREF_DB_PATH, "").trim();
        if (!preferredPath.isBlank()) {
            return normalize(Path.of(preferredPath));
        }

        Path legacyLocalPath = Paths.get(LEGACY_DB_FILE_NAME).toAbsolutePath().normalize();
        if (Files.exists(legacyLocalPath)) {
            return legacyLocalPath;
        }

        return getProfilesDirectory().resolve(LEGACY_DB_FILE_NAME).toAbsolutePath().normalize();
    }

    private static String resolveInitialProfileLabel(Path databasePath) {
        String storedLabel = PREFERENCES.get(PREF_DB_LABEL, "").trim();
        if (!storedLabel.isBlank()) {
            return storedLabel;
        }

        if (databasePath == null) {
            return "Baza główna";
        }

        String fileName = databasePath.getFileName() == null ? LEGACY_DB_FILE_NAME : databasePath.getFileName().toString();
        if (LEGACY_DB_FILE_NAME.equalsIgnoreCase(fileName)) {
            return "Baza główna";
        }
        return fileName;
    }

    private static Path normalize(Path databaseFilePath) {
        if (databaseFilePath == null) {
            throw new IllegalArgumentException("Ścieżka bazy danych nie może być pusta.");
        }
        return databaseFilePath.toAbsolutePath().normalize();
    }

    private static String normalizeLabel(String label, Path databaseFilePath) {
        String normalized = label == null ? "" : label.trim();
        if (!normalized.isBlank()) {
            return normalized;
        }
        return databaseFilePath == null || databaseFilePath.getFileName() == null
                ? "Baza danych"
                : databaseFilePath.getFileName().toString();
    }

    private static String buildUrl(Path databaseFilePath) {
        return "jdbc:sqlite:" + normalize(databaseFilePath);
    }

    private static void ensureParentDirectoryExists(Path databaseFilePath) {
        if (databaseFilePath == null) {
            return;
        }

        Path parent = databaseFilePath.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się przygotować katalogu dla bazy danych: " + parent, e);
        }
    }
}
