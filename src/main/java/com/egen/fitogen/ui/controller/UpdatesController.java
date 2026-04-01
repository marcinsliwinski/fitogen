package com.egen.fitogen.ui.controller;

import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.ui.router.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class UpdatesController {

    private static final String APP_VERSION = "1.0.0";

    @FXML private Label appVersionLabel;
    @FXML private Label systemUpdateStatusLabel;
    @FXML private Label plantDatabaseStatusLabel;
    @FXML private Label lastBackupInfoLabel;
    @FXML private Label moduleReadinessLabel;

    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();

    @FXML
    public void initialize() {
        appVersionLabel.setText(APP_VERSION);
        systemUpdateStatusLabel.setText("Moduł aktualizacji aplikacji jest przygotowany do dalszego rozwoju.");
        plantDatabaseStatusLabel.setText("Moduł aktualizacji bazy roślin jest przygotowany do dalszego rozwoju.");

        String lastBackupAt = appSettingsService.getLastBackupAt();
        if (lastBackupAt == null || lastBackupAt.isBlank()) {
            lastBackupInfoLabel.setText("Brak informacji o wykonanym backupie.");
        } else {
            lastBackupInfoLabel.setText("Ostatni zapisany backup: " + lastBackupAt);
        }

        moduleReadinessLabel.setText(
                "Na tym etapie uporządkowano routing i osobny widok modułu Aktualizacje. "
                        + "Silnik aktualizacji aplikacji i bazy roślin będzie można bezpiecznie dodać w kolejnych etapach."
        );
    }

    @FXML
    private void openSettings() {
        ViewManager.show(ViewManager.SETTINGS);
    }

    @FXML
    private void openHelp() {
        ViewManager.show(ViewManager.HELP);
    }
}