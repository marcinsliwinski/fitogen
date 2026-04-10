package com.egen.fitogen.service;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.database.DatabaseInitializer;
import com.egen.fitogen.dto.DatabaseProfileInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

public class DatabaseProfileService {

    public DatabaseProfileInfo getCurrentProfileInfo() {
        return new DatabaseProfileInfo(
                DatabaseConfig.getDatabaseFilePath(),
                DatabaseConfig.getCurrentProfileLabel(),
                DatabaseConfig.isUsingTestDatabase(),
                false
        );
    }

    public DatabaseProfileInfo createOrActivateNewProfile(String profileName) {
        String normalizedDisplayName = normalizeDisplayName(profileName);
        Path profilePath = DatabaseConfig.buildNamedProfilePath(normalizedDisplayName);
        boolean createdNow = Files.notExists(profilePath);

        DatabaseConfig.activateDatabase(profilePath, normalizedDisplayName, false);
        DatabaseInitializer.initDatabase(profilePath);
        seedNewProfile(profilePath, normalizedDisplayName, createdNow);

        return new DatabaseProfileInfo(profilePath, normalizedDisplayName, false, createdNow);
    }

    public DatabaseProfileInfo activateTestProfile() {
        Path testProfilePath = DatabaseConfig.getTestProfilePath();
        boolean createdNow = Files.notExists(testProfilePath);

        DatabaseConfig.activateDatabase(testProfilePath, "Baza testowa", true);
        DatabaseInitializer.initDatabase(testProfilePath);
        seedTestProfile(testProfilePath, createdNow);

        return new DatabaseProfileInfo(testProfilePath, "Baza testowa", true, createdNow);
    }

    public String buildCurrentProfileSummary() {
        DatabaseProfileInfo info = getCurrentProfileInfo();
        String mode = info.testProfile() ? "Wersja testowa" : "Baza robocza";
        return info.displayName() + " • " + mode + " • " + info.databasePath();
    }

    private void seedNewProfile(Path profilePath, String profileName, boolean createdNow) {
        if (!createdNow) {
            return;
        }

        try (Connection connection = DatabaseConfig.getConnection(profilePath)) {
            upsertSetting(connection, AppSettingsService.ISSUER_NURSERY_NAME, profileName);
            insertAuditEntry(connection, "SYSTEM", null, "CREATE", "System", "Założono nową bazę danych profilu: " + profileName + ".");
        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się przygotować nowej bazy danych profilu.", e);
        }
    }

    private void seedTestProfile(Path profilePath, boolean createdNow) {
        if (!createdNow) {
            return;
        }

        try (Connection connection = DatabaseConfig.getConnection(profilePath)) {
            upsertSetting(connection, AppSettingsService.ISSUER_NURSERY_NAME, "Szkółka testowa eGen Labs");
            upsertSetting(connection, AppSettingsService.ISSUER_COUNTRY, "Polska");
            upsertSetting(connection, AppSettingsService.ISSUER_COUNTRY_CODE, "PL");
            upsertSetting(connection, AppSettingsService.ISSUER_POSTAL_CODE, "41-800");
            upsertSetting(connection, AppSettingsService.ISSUER_CITY, "Zabrze");
            upsertSetting(connection, AppSettingsService.ISSUER_STREET, "ul. Testowa 1");
            upsertSetting(connection, AppSettingsService.ISSUER_PHYTOSANITARY_NUMBER, "PL-TEST-001");

            insertIfTableEmpty(connection,
                    "plants",
                    "INSERT INTO plants (species, variety, rootstock, latin_species_name, eppo_code, passport_required, visibility_status) VALUES " +
                            "('Jabłoń', 'Golden Delicious', 'M26', 'Malus domestica', 'MABSD', 1, 'Używany')," +
                            "('Wiśnia', 'Łutówka', 'Antypka', 'Prunus cerasus', 'PRNCE', 1, 'Używany')");

            insertIfTableEmpty(connection,
                    "contrahents",
                    "INSERT INTO contrahents (name, country, country_code, postal_code, city, street, phytosanitary_number, is_supplier, is_client) VALUES " +
                            "('Szkółka Klient Test', 'Polska', 'PL', '00-001', 'Warszawa', 'ul. Klonowa 7', 'PL-CLI-001', 0, 1)," +
                            "('Dostawca Test', 'Polska', 'PL', '60-001', 'Poznań', 'ul. Ogrodowa 3', 'PL-SUP-001', 1, 0)");

            insertIfTableEmpty(connection,
                    "plant_batches",
                    "INSERT INTO plant_batches (interior_batch_no, exterior_batch_no, plant_id, qty, creation_date, age, manufacturer_country_code, fito_qualification_category, eppo_code, zp_zone, contrahent_id, is_internal_source, comments, status) VALUES " +
                            "('BATCH-TEST-001', 'EXT-001', 1, 120, '" + LocalDate.now() + "', '12', 'PL', 'CAC', 'MABSD', 'PL', 2, 1, 'Partia testowa 1', 'ACTIVE')," +
                            "('BATCH-TEST-002', 'EXT-002', 2, 80, '" + LocalDate.now() + "', '8', 'PL', 'CAC', 'PRNCE', 'PL', 2, 1, 'Partia testowa 2', 'ACTIVE')");

            insertIfTableEmpty(connection,
                    "documents",
                    "INSERT INTO documents (document_number, document_type, issue_date, contrahent_id, created_by, comments, status) VALUES " +
                            "('TEST-DOC-001', 'Dokument dostawcy', '" + LocalDate.now() + "', 1, 'System', 'Dokument demonstracyjny', 'ACTIVE')");

            insertIfTableEmpty(connection,
                    "document_items",
                    "INSERT INTO document_items (document_id, plant_batch_id, qty, passport_required) VALUES (1, 1, 25, 1)");

            insertAuditEntry(connection, "SYSTEM", null, "CREATE", "System", "Przygotowano wbudowaną bazę testową z przykładowymi rekordami.");
        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się przygotować bazy testowej.", e);
        }
    }

    private void insertIfTableEmpty(Connection connection, String tableName, String insertSql) throws Exception {
        boolean empty = false;
        try (Statement countStatement = connection.createStatement();
             ResultSet resultSet = countStatement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            empty = resultSet.next() && resultSet.getInt(1) == 0;
        }

        if (!empty) {
            return;
        }

        try (Statement insertStatement = connection.createStatement()) {
            insertStatement.executeUpdate(insertSql);
        }
    }

    private void upsertSetting(Connection connection, String key, String value) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) " +
                        "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value"
        )) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }

    private void insertAuditEntry(Connection connection, String entityType, Integer entityId, String actionType, String actor, String description) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO audit_log (entity_type, entity_id, action_type, actor, description, changed_at) VALUES (?, ?, ?, ?, ?, datetime('now'))"
        )) {
            statement.setString(1, entityType);
            if (entityId == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, entityId);
            }
            statement.setString(3, actionType);
            statement.setString(4, actor);
            statement.setString(5, description);
            statement.executeUpdate();
        }
    }

    private String normalizeDisplayName(String profileName) {
        String normalized = profileName == null ? "" : profileName.trim();
        return normalized.isBlank() ? "Nowa baza" : normalized;
    }
}
