package com.egen.fitogen.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class DatabaseConfig {

    private static final String APP_FOLDER_NAME = ".fitogen-essentials";
    private static final String PROFILES_FOLDER_NAME = "profiles";
    private static final String STATE_FILE_NAME = "state.properties";
    private static final String DEFAULT_DB_FILE_NAME = "fitogen.db";
    private static final String DEFAULT_TEST_DB_FILE_NAME = "baza_testowa.db";
    private static final String ACTIVE_DB_PATH_KEY = "database.active.path";
    private static final String MISSING_DB_PATH_KEY = "database.missing.path";
    private static final String LAST_BACKUP_PATH_KEY = "backup.last.path";

    private static final Path APP_HOME = Paths.get(System.getProperty("user.home"), APP_FOLDER_NAME)
            .toAbsolutePath()
            .normalize();
    private static final Path PROFILES_HOME = APP_HOME.resolve(PROFILES_FOLDER_NAME)
            .toAbsolutePath()
            .normalize();
    private static final Path STATE_FILE = APP_HOME.resolve(STATE_FILE_NAME)
            .toAbsolutePath()
            .normalize();

    private static Path activeDatabaseFilePath = resolveStartupDatabasePath();

    private DatabaseConfig() {
    }

    public static synchronized Connection getConnection() throws SQLException {
        ensureDirectories();
        return DriverManager.getConnection(getDatabaseUrl());
    }

    public static synchronized Connection getConnection(Path databaseFilePath) throws SQLException {
        if (databaseFilePath == null) {
            throw new IllegalArgumentException("Ścieżka bazy danych jest wymagana.");
        }
        ensureDirectories();
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFilePath.toAbsolutePath().normalize());
    }

    public static synchronized String getDatabaseUrl() {
        return "jdbc:sqlite:" + getDatabaseFilePath();
    }

    public static synchronized String getDatabaseFileName() {
        return getDatabaseFilePath().getFileName().toString();
    }

    public static synchronized Path getDatabaseFilePath() {
        if (activeDatabaseFilePath == null) {
            activeDatabaseFilePath = resolveStartupDatabasePath();
        }
        return activeDatabaseFilePath;
    }

    public static synchronized Path getAppHomeDirectory() {
        ensureDirectories();
        return APP_HOME;
    }

    public static synchronized Path getProfilesDirectory() {
        ensureDirectories();
        return PROFILES_HOME;
    }

    public static synchronized Path getTestProfilePath() {
        ensureDirectories();
        return PROFILES_HOME.resolve(DEFAULT_TEST_DB_FILE_NAME).toAbsolutePath().normalize();
    }

    public static synchronized String sanitizeProfileName(String rawProfileName) {
        return normalizeProfileName(rawProfileName);
    }

    public static synchronized Path buildNamedProfilePath(String rawProfileName) {
        return PROFILES_HOME.resolve(ensureDbExtension(normalizeProfileName(rawProfileName))).toAbsolutePath().normalize();
    }

    public static synchronized void activateDatabase(Path databaseFilePath, String displayName, boolean testProfile) {
        if (databaseFilePath == null) {
            throw new IllegalArgumentException("Ścieżka bazy danych jest wymagana.");
        }

        Path normalized = databaseFilePath.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się przygotować katalogu dla bazy danych.", e);
        }

        activeDatabaseFilePath = normalized;
        Properties state = loadState();
        state.setProperty(ACTIVE_DB_PATH_KEY, normalized.toString());
        state.remove(MISSING_DB_PATH_KEY);
        storeState(state);
    }

    public static synchronized List<Path> listAvailableDatabaseFiles() {
        ensureDirectories();
        List<Path> result = new ArrayList<>();
        try (var stream = Files.list(PROFILES_HOME)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".db"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .forEach(result::add);
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać listy baz danych z katalogu użytkownika.", e);
        }
        return result;
    }

    public static synchronized Path createDatabaseProfile(String rawProfileName) {
        String normalizedProfileName = normalizeProfileName(rawProfileName);
        Path target = PROFILES_HOME.resolve(ensureDbExtension(normalizedProfileName)).normalize();
        if (Files.exists(target)) {
            throw new IllegalArgumentException("Baza danych o tej nazwie już istnieje: " + target.getFileName());
        }

        try {
            Files.createDirectories(target.getParent());
            Files.createFile(target);
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się utworzyć nowej bazy danych: " + target.getFileName(), e);
        }

        switchDatabase(target);
        return target;
    }

    public static synchronized Path restoreDatabaseFromBackup(Path sourceBackup, String rawProfileName) {
        if (sourceBackup == null) {
            throw new IllegalArgumentException("Ścieżka do backupu jest wymagana.");
        }
        if (!Files.exists(sourceBackup) || !Files.isRegularFile(sourceBackup)) {
            throw new IllegalArgumentException("Nie znaleziono pliku backupu: " + sourceBackup);
        }

        String requestedName = rawProfileName == null || rawProfileName.isBlank()
                ? stripExtension(sourceBackup.getFileName().toString())
                : rawProfileName;

        Path target = PROFILES_HOME.resolve(ensureDbExtension(normalizeProfileName(requestedName))).normalize();
        if (Files.exists(target)) {
            throw new IllegalArgumentException("Baza danych o tej nazwie już istnieje: " + target.getFileName());
        }

        try {
            Files.createDirectories(target.getParent());
            Files.copy(sourceBackup, target);
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odzyskać bazy danych z backupu.", e);
        }

        switchDatabase(target);
        return target;
    }

    public static synchronized void switchDatabase(Path databaseFilePath) {
        if (databaseFilePath == null) {
            throw new IllegalArgumentException("Ścieżka bazy danych jest wymagana.");
        }

        Path normalized = databaseFilePath.toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            throw new IllegalArgumentException("Nie znaleziono bazy danych: " + normalized);
        }

        activeDatabaseFilePath = normalized;
        Properties state = loadState();
        state.setProperty(ACTIVE_DB_PATH_KEY, normalized.toString());

        String missingPath = state.getProperty(MISSING_DB_PATH_KEY, "");
        if (missingPath.equalsIgnoreCase(normalized.toString())) {
            state.remove(MISSING_DB_PATH_KEY);
        }

        storeState(state);
    }

    public static synchronized Optional<Path> getMissingRememberedDatabasePath() {
        Properties state = loadState();
        String raw = trimToEmpty(state.getProperty(MISSING_DB_PATH_KEY));
        if (raw.isBlank()) {
            return Optional.empty();
        }

        Path missingPath = Path.of(raw).toAbsolutePath().normalize();
        if (Files.exists(missingPath)) {
            state.remove(MISSING_DB_PATH_KEY);
            storeState(state);
            return Optional.empty();
        }
        return Optional.of(missingPath);
    }

    public static synchronized void clearMissingRememberedDatabase() {
        Properties state = loadState();
        state.remove(MISSING_DB_PATH_KEY);
        storeState(state);
    }

    public static synchronized void rememberBackup(Path backupPath) {
        if (backupPath == null) {
            return;
        }

        Properties state = loadState();
        state.setProperty(LAST_BACKUP_PATH_KEY, backupPath.toAbsolutePath().normalize().toString());
        storeState(state);
    }

    public static synchronized Optional<Path> getRememberedBackupPath() {
        Properties state = loadState();
        String raw = trimToEmpty(state.getProperty(LAST_BACKUP_PATH_KEY));
        if (raw.isBlank()) {
            return Optional.empty();
        }

        Path path = Path.of(raw).toAbsolutePath().normalize();
        return Files.exists(path) ? Optional.of(path) : Optional.empty();
    }

    public static synchronized boolean isTestDatabase(Path databaseFilePath) {
        if (databaseFilePath == null) {
            return false;
        }

        Path normalized = databaseFilePath.toAbsolutePath().normalize();
        Path testPath = getTestProfilePath();
        if (testPath != null && normalized.equals(testPath)) {
            return true;
        }

        if (normalized.getFileName() == null || testPath == null || testPath.getFileName() == null) {
            return false;
        }

        return normalized.getFileName().toString().equalsIgnoreCase(testPath.getFileName().toString());
    }

    public static synchronized String getActiveDatabaseDisplayName() {
        Path path = getDatabaseFilePath();
        return path.getFileName() == null ? path.toString() : path.getFileName().toString();
    }

    private static synchronized Path resolveStartupDatabasePath() {
        ensureDirectories();

        Properties state = loadState();
        String rawActivePath = trimToEmpty(state.getProperty(ACTIVE_DB_PATH_KEY));
        if (!rawActivePath.isBlank()) {
            Path savedPath = Path.of(rawActivePath).toAbsolutePath().normalize();
            if (Files.exists(savedPath)) {
                state.remove(MISSING_DB_PATH_KEY);
                storeState(state);
                return savedPath;
            }
            state.setProperty(MISSING_DB_PATH_KEY, savedPath.toString());
        }

        List<Path> availableProfiles = listAvailableDatabaseFiles();
        Path fallbackPath = availableProfiles.isEmpty()
                ? PROFILES_HOME.resolve(DEFAULT_DB_FILE_NAME).toAbsolutePath().normalize()
                : availableProfiles.get(0).toAbsolutePath().normalize();

        state.setProperty(ACTIVE_DB_PATH_KEY, fallbackPath.toString());
        storeState(state);
        return fallbackPath;
    }

    private static void ensureDirectories() {
        try {
            Files.createDirectories(APP_HOME);
            Files.createDirectories(PROFILES_HOME);
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się utworzyć katalogów roboczych aplikacji.", e);
        }
    }

    private static Properties loadState() {
        ensureDirectories();
        Properties properties = new Properties();
        if (!Files.exists(STATE_FILE)) {
            return properties;
        }

        try (InputStream input = Files.newInputStream(STATE_FILE)) {
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać pliku stanu aplikacji.", e);
        }
    }

    private static void storeState(Properties properties) {
        ensureDirectories();
        try (OutputStream output = Files.newOutputStream(STATE_FILE)) {
            properties.store(output, "Fito Gen Essentials - stan aplikacji");
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się zapisać pliku stanu aplikacji.", e);
        }
    }

    private static String normalizeProfileName(String rawProfileName) {
        String normalized = trimToEmpty(rawProfileName)
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Podaj nazwę nowej bazy danych.");
        }

        return normalized;
    }

    private static String ensureDbExtension(String fileName) {
        return fileName.toLowerCase().endsWith(".db") ? fileName : fileName + ".db";
    }

    private static String stripExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return DEFAULT_DB_FILE_NAME;
        }

        int extensionIndex = fileName.toLowerCase().lastIndexOf(".db");
        if (extensionIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, extensionIndex);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
