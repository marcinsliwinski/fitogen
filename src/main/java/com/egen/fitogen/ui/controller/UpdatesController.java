package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.service.AppSettingsService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class UpdatesController {

    private static final String APP_VERSION = "1.0.0";

    @FXML private Label appVersionLabel;
    @FXML private Label appUpdateStatusLabel;
    @FXML private Label plantsUpdateStatusLabel;
    @FXML private Label eppoUpdateStatusLabel;
    @FXML private Label countriesUpdateStatusLabel;
    @FXML private Label lastBackupInfoLabel;
    @FXML private Label readinessSummaryLabel;

    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();

    @FXML
    public void initialize() {
        appVersionLabel.setText(APP_VERSION);

        appUpdateStatusLabel.setText(
                "Moduł aktualizacji aplikacji nie jest jeszcze aktywny. Ekran jest przygotowany pod bezpieczne wdrożenie sprawdzania, pobierania i instalacji nowszej wersji programu."
        );
        plantsUpdateStatusLabel.setText(
                "Server Update dla bazy roślin nie jest jeszcze aktywny. Docelowo tutaj będą obsługiwane import i aktualizacja danych roślin z serwera."
        );
        eppoUpdateStatusLabel.setText(
                "Server Update dla bazy EPPO nie jest jeszcze aktywny. Docelowo tutaj będą obsługiwane import i aktualizacja danych referencyjnych EPPO z serwera."
        );
        countriesUpdateStatusLabel.setText(
                "Server Update dla wspólnego słownika krajów nie jest jeszcze aktywny. Docelowo tutaj będą obsługiwane import i aktualizacja słownika krajów z serwera."
        );

        String lastBackupAt = appSettingsService.getLastBackupAt();
        if (lastBackupAt == null || lastBackupAt.isBlank()) {
            lastBackupInfoLabel.setText("Brak informacji o wykonanym backupie.");
        } else {
            lastBackupInfoLabel.setText("Ostatni zapisany backup: " + lastBackupAt);
        }

        readinessSummaryLabel.setText(
                "Ten ekran jest zarezerwowany wyłącznie dla aktualizacji aplikacji i przyszłych synchronizacji Server Update. Lokalne importy i eksporty CSV pozostają poza tym modułem."
        );
    }
}
