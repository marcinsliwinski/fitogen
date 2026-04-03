package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.EppoCodeService;
import com.egen.fitogen.service.PlantService;
import com.egen.fitogen.ui.util.CountryDirectory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.util.List;

public class UpdatesController {

    private static final String APP_VERSION = "1.0.0";

    @FXML private Label appVersionLabel;
    @FXML private Label appUpdateStatusLabel;
    @FXML private Label plantsUpdateStatusLabel;
    @FXML private Label eppoUpdateStatusLabel;
    @FXML private Label countriesUpdateStatusLabel;
    @FXML private Label lastBackupInfoLabel;
    @FXML private Label readinessSummaryLabel;
    @FXML private Label dryRunStatusLabel;
    @FXML private TextArea plantsDryRunPreviewArea;
    @FXML private TextArea eppoDryRunPreviewArea;
    @FXML private TextArea countriesDryRunPreviewArea;

    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final PlantService plantService = AppContext.getPlantService();
    private final EppoCodeService eppoCodeService = AppContext.getEppoCodeService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();

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

        initializeDryRunPreviewState();
    }

    private void initializeDryRunPreviewState() {
        if (dryRunStatusLabel != null) {
            dryRunStatusLabel.setText("Preview / dry-run pokazuje tylko lokalne podsumowanie gotowości. Nie pobiera danych z serwera i nie zapisuje zmian.");
        }
        if (plantsDryRunPreviewArea != null) {
            plantsDryRunPreviewArea.setText("Brak preview Plants Server Update.");
        }
        if (eppoDryRunPreviewArea != null) {
            eppoDryRunPreviewArea.setText("Brak preview EPPO Server Update.");
        }
        if (countriesDryRunPreviewArea != null) {
            countriesDryRunPreviewArea.setText("Brak preview wspólnego słownika krajów.");
        }
    }

    @FXML
    private void previewPlantsDryRun() {
        List<com.egen.fitogen.model.Plant> plants = plantService.getAllPlants();
        long passportRequiredCount = plants.stream().filter(com.egen.fitogen.model.Plant::isPassportRequired).count();
        long withEppoCount = plants.stream().filter(plant -> plant.getEppoCode() != null && !plant.getEppoCode().isBlank()).count();

        StringBuilder builder = new StringBuilder();
        builder.append("Lokalna baza Plants: ").append(plants.size()).append(" rekordów\n");
        builder.append("Rośliny z wymaganym paszportem: ").append(passportRequiredCount).append("\n");
        builder.append("Rośliny z informacyjnym kodem EPPO: ").append(withEppoCount).append("\n\n");
        builder.append("Próbka pierwszych rekordów:\n");

        int limit = Math.min(plants.size(), 8);
        for (int i = 0; i < limit; i++) {
            com.egen.fitogen.model.Plant plant = plants.get(i);
            builder.append("- ")
                    .append(safe(plant.getSpecies()));
            if (plant.getVariety() != null && !plant.getVariety().isBlank()) {
                builder.append(" | odmiana=").append(plant.getVariety());
            }
            if (plant.getRootstock() != null && !plant.getRootstock().isBlank()) {
                builder.append(" | podkładka=").append(plant.getRootstock());
            }
            if (plant.getEppoCode() != null && !plant.getEppoCode().isBlank()) {
                builder.append(" | EPPO=").append(plant.getEppoCode());
            }
            builder.append("\n");
        }

        builder.append("\nTo jest tylko lokalny preview gotowości pod przyszły Server Update Plants.");
        plantsDryRunPreviewArea.setText(builder.toString());
        dryRunStatusLabel.setText("Wygenerowano preview Plants Server Update bez pobierania danych z serwera.");
    }

    @FXML
    private void previewEppoDryRun() {
        List<com.egen.fitogen.model.EppoCode> codes = eppoCodeService.getAll();
        long activeCount = codes.stream().filter(code -> code.getStatus() == null || code.getStatus().isBlank() || "ACTIVE".equalsIgnoreCase(code.getStatus())).count();

        StringBuilder builder = new StringBuilder();
        builder.append("Lokalna baza EPPO: ").append(codes.size()).append(" rekordów\n");
        builder.append("Rekordy aktywne: ").append(activeCount).append("\n\n");
        builder.append("Próbka pierwszych rekordów:\n");

        int limit = Math.min(codes.size(), 8);
        for (int i = 0; i < limit; i++) {
            com.egen.fitogen.model.EppoCode code = codes.get(i);
            builder.append("- ")
                    .append(safe(code.getCode()))
                    .append(" | nazwa=").append(safe(code.getSpeciesName()))
                    .append(" | status=").append(safe(code.getStatus()))
                    .append("\n");
        }

        builder.append("\nTo jest tylko lokalny preview gotowości pod przyszły Server Update EPPO.");
        eppoDryRunPreviewArea.setText(builder.toString());
        dryRunStatusLabel.setText("Wygenerowano preview EPPO Server Update bez pobierania danych z serwera.");
    }

    @FXML
    private void previewCountriesDryRun() {
        List<CountryDirectory.CountryEntry> entries = countryDirectoryService.getEntries();
        List<CountryDirectory.CountryEntry> customEntries = countryDirectoryService.getCustomEntries();

        StringBuilder builder = new StringBuilder();
        builder.append("Wspólny słownik krajów: ").append(entries.size()).append(" rekordów łącznie\n");
        builder.append("Wpisy własne użytkownika: ").append(customEntries.size()).append("\n\n");
        builder.append("Próbka pierwszych rekordów:\n");

        int limit = Math.min(entries.size(), 10);
        for (int i = 0; i < limit; i++) {
            CountryDirectory.CountryEntry entry = entries.get(i);
            builder.append("- ").append(safe(entry.country())).append(" (").append(safe(entry.countryCode())).append(")\n");
        }

        builder.append("\nTo jest tylko lokalny preview gotowości pod przyszły Server Update wspólnego słownika krajów.");
        countriesDryRunPreviewArea.setText(builder.toString());
        dryRunStatusLabel.setText("Wygenerowano preview wspólnego słownika krajów bez pobierania danych z serwera.");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
