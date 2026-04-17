package com.egen.fitogen.service;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.database.DatabaseInitializer;
import com.egen.fitogen.dto.DatabaseProfileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DatabaseProfileService {

    public List<DatabaseProfileInfo> getAvailableProfiles() {
        List<DatabaseProfileInfo> result = new ArrayList<>();
        Path currentPath = DatabaseConfig.getDatabaseFilePath();

        try {
            if (Files.exists(DatabaseConfig.getProfilesDirectory())) {
                try (var profiles = Files.list(DatabaseConfig.getProfilesDirectory())) {
                    profiles.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".db"))
                            .sorted(Comparator.comparing(this::lastModifiedSafe).reversed()
                                    .thenComparing(path -> humanizeLabel(path), String.CASE_INSENSITIVE_ORDER))
                            .forEach(path -> result.add(buildProfileInfo(path)));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać listy dostępnych baz danych.", e);
        }

        boolean currentAlreadyVisible = result.stream()
                .anyMatch(info -> info.databasePath() != null && info.databasePath().equals(currentPath));

        if (!currentAlreadyVisible && currentPath != null) {
            result.add(0, buildProfileInfo(currentPath));
        }

        result.add(DatabaseProfileInfo.newProfileOption());
        return result;
    }

    public DatabaseProfileInfo getCurrentProfileInfo() {
        return buildProfileInfo(DatabaseConfig.getDatabaseFilePath());
    }

    public DatabaseProfileInfo activateProfile(DatabaseProfileInfo profileInfo) {
        if (profileInfo == null || profileInfo.createNewOption() || profileInfo.databasePath() == null) {
            throw new IllegalArgumentException("Nie wybrano poprawnego profilu bazy danych.");
        }

        DatabaseConfig.switchDatabase(profileInfo.databasePath());
        DatabaseInitializer.initDatabase();
        return getCurrentProfileInfo();
    }

    public DatabaseProfileInfo createAndActivateProfile(String profileName) {
        return createAndActivateProfile(profileName, false);
    }

    public DatabaseProfileInfo createAndActivateProfile(String profileName, boolean importStarterPackFg1) {
        String sanitized = DatabaseConfig.sanitizeProfileName(profileName);
        Path profilePath = DatabaseConfig.buildNamedProfilePath(sanitized);
        if (Files.exists(profilePath)) {
            throw new IllegalArgumentException("Baza danych o tej nazwie już istnieje: " + profilePath.getFileName());
        }

        Path createdPath = DatabaseConfig.createDatabaseProfile(sanitized);
        DatabaseInitializer.initDatabase();
        seedNewProfile(createdPath, profileName);

        if (importStarterPackFg1) {
            BootstrapStarterPackService starterPackService = new BootstrapStarterPackService();
            starterPackService.importFg1StarterPack();
            AppSettingsService settingsService = new AppSettingsService(
                    new com.egen.fitogen.database.SqliteAppSettingsRepository(),
                    com.egen.fitogen.config.AppContext.getAuditLogService()
            );
            settingsService.setPlantFullCatalogEnabled(true);
            starterPackService.verifyFg1StarterPackImported();
        }

        DatabaseConfig.switchDatabase(createdPath);
        DatabaseInitializer.initDatabase();
        return getCurrentProfileInfo();
    }

    public DatabaseProfileInfo restoreAndActivateProfile(Path backupFilePath, String profileName) {
        Path restoredPath = DatabaseConfig.restoreDatabaseFromBackup(backupFilePath, profileName);
        DatabaseInitializer.initDatabase();
        return buildProfileInfo(restoredPath);
    }

    public String buildCurrentProfileSummary() {
        DatabaseProfileInfo info = getCurrentProfileInfo();
        String mode = info.testProfile() ? "Baza testowa" : "Baza robocza";
        String existence = info.exists() ? "plik dostępny" : "brak pliku";
        return info.displayName() + " • " + mode + " • " + existence;
    }

    private DatabaseProfileInfo buildProfileInfo(Path databasePath) {
        Path normalizedPath = databasePath == null ? null : databasePath.toAbsolutePath().normalize();
        boolean exists = normalizedPath != null && Files.exists(normalizedPath);
        boolean current = normalizedPath != null && normalizedPath.equals(DatabaseConfig.getDatabaseFilePath());
        return new DatabaseProfileInfo(
                normalizedPath,
                humanizeLabel(normalizedPath),
                current,
                isTestProfile(normalizedPath),
                false,
                exists
        );
    }

    private void seedNewProfile(Path profilePath, String requestedName) {
        String displayName = requestedName == null || requestedName.trim().isBlank()
                ? humanizeLabel(profilePath)
                : requestedName.trim();

        try (Connection connection = DatabaseConfig.getConnection(profilePath);
             PreparedStatement settingStatement = connection.prepareStatement(
                     "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) " +
                             "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value");
             PreparedStatement auditStatement = connection.prepareStatement(
                     "INSERT INTO audit_log (entity_type, entity_id, action_type, actor, description, changed_at) VALUES (?, ?, ?, ?, ?, ?)")
        ) {
            settingStatement.setString(1, AppSettingsService.ISSUER_NURSERY_NAME);
            settingStatement.setString(2, displayName);
            settingStatement.executeUpdate();

            auditStatement.setString(1, "SYSTEM");
            auditStatement.setNull(2, java.sql.Types.INTEGER);
            auditStatement.setString(3, "CREATE");
            auditStatement.setString(4, "System");
            auditStatement.setString(5, "Założono nową bazę danych: " + displayName + ".");
            auditStatement.setString(6, LocalDateTime.now().toString());
            auditStatement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się przygotować nowej bazy danych.", e);
        }
    }

    private FileTime lastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    private boolean isTestProfile(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        return path.getFileName().toString().equalsIgnoreCase(DatabaseConfig.getTestProfilePath().getFileName().toString());
    }

    private String humanizeLabel(Path path) {
        if (path == null || path.getFileName() == null) {
            return "Baza danych";
        }

        String fileName = path.getFileName().toString();
        String stem = fileName.endsWith(".db") ? fileName.substring(0, fileName.length() - 3) : fileName;
        String normalized = stem.replace('_', ' ').trim();
        if (normalized.isBlank()) {
            return "Baza danych";
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
