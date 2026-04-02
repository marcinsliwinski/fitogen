package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.BackupService;
import com.egen.fitogen.ui.router.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UpdatesController {

    private static final String APP_VERSION = "1.0.0";
    private static final DateTimeFormatter FILE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private Label appVersionLabel;
    @FXML private Label systemUpdateStatusLabel;
    @FXML private Label plantDatabaseStatusLabel;
    @FXML private Label lastBackupInfoLabel;
    @FXML private Label moduleReadinessLabel;

    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final BackupService backupService = AppContext.getBackupService();

    @FXML
    public void initialize() {
        appVersionLabel.setText(APP_VERSION);
        loadBackupStatus();
        loadModuleReadiness();
        loadServerUpdateStatus();
    }

    @FXML
    private void openSettings() {
        ViewManager.show(ViewManager.SETTINGS);
    }

    @FXML
    private void openHelp() {
        ViewManager.show(ViewManager.HELP);
    }

    private void loadBackupStatus() {
        String lastBackupAt = trimmedOrNull(appSettingsService.getLastBackupAt());
        String lastBackupPath = trimmedOrNull(appSettingsService.getLastBackupPath());

        if (lastBackupAt == null) {
            lastBackupInfoLabel.setText(
                    "Backup: brak informacji o wykonanym backupie. Przed wdrożeniem Server Update wykonaj backup bazy danych w Ustawieniach."
            );
            return;
        }

        StringBuilder sb = new StringBuilder("Ostatni backup: ").append(lastBackupAt);

        if (lastBackupPath != null) {
            Path backupPath = Path.of(lastBackupPath);
            sb.append("\nŚcieżka: ").append(backupPath.toAbsolutePath().normalize());
            sb.append(Files.exists(backupPath) ? " (plik dostępny)" : " (plik nie został odnaleziony)");
        }

        lastBackupInfoLabel.setText(sb.toString());
    }

    private void loadModuleReadiness() {
        boolean hasBackup = trimmedOrNull(appSettingsService.getLastBackupAt()) != null;
        boolean issuerComplete = appSettingsService.isIssuerProfileComplete();

        if (hasBackup && issuerComplete) {
            moduleReadinessLabel.setText(
                    "Gotowość modułu: dobra. Importy i eksporty CSV są prowadzone w Ustawieniach, a ten ekran pozostaje przygotowany pod przyszły Server Update danych i aktualizację aplikacji."
            );
            return;
        }

        if (hasBackup) {
            moduleReadinessLabel.setText(
                    "Gotowość modułu: częściowa. Backup jest wykonany, ale warto jeszcze uzupełnić dane podmiotu przed wejściem w bezpieczny Server Update."
            );
            return;
        }

        moduleReadinessLabel.setText(
                "Gotowość modułu: niska. Najpierw wykonaj backup i uporządkuj podstawową konfigurację w Ustawieniach."
        );
    }

    private void loadServerUpdateStatus() {
        Path databasePath = backupService.getDatabaseFilePath();
        String databaseStatus;

        if (!Files.exists(databasePath)) {
            databaseStatus = "Plik bazy danych nie został odnaleziony.";
        } else {
            try {
                long sizeBytes = Files.size(databasePath);
                String modifiedAt = FILE_TIME_FORMATTER.format(
                        LocalDateTime.ofInstant(Files.getLastModifiedTime(databasePath).toInstant(), java.time.ZoneId.systemDefault())
                );
                databaseStatus = "Plik bazy danych jest dostępny (" + formatSize(sizeBytes) + "), ostatnia modyfikacja: " + modifiedAt + ".";
            } catch (Exception e) {
                databaseStatus = "Plik bazy danych jest dostępny, ale nie udało się odczytać jego pełnych metadanych.";
            }
        }

        systemUpdateStatusLabel.setText(
                "Server Update: ten moduł jest przeznaczony wyłącznie do przyszłej aktualizacji aplikacji oraz synchronizacji danych z serwera. Importy i eksporty CSV zostały przeniesione do Ustawień."
        );

        plantDatabaseStatusLabel.setText(
                "Zakres przyszłej synchronizacji z serwera: Plants, EPPO oraz wspólny słownik krajów. Kontrahenci i dokumenty pozostają wyłącznie zakresem import-only w Ustawieniach. "
                        + databaseStatus
        );
    }

    private String trimmedOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatSize(long sizeBytes) {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        if (sizeBytes < 1024 * 1024) {
            return (sizeBytes / 1024) + " KB";
        }
        return String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
    }
}
