package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.EppoCodeService;
import com.egen.fitogen.service.PlantService;
import com.egen.fitogen.ui.util.CountryDirectory;
import com.egen.fitogen.ui.util.UiTextUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        if (appVersionLabel != null) {
            appVersionLabel.setText(APP_VERSION);
        }

        if (appUpdateStatusLabel != null) {
            appUpdateStatusLabel.setText(
                "Moduł aktualizacji aplikacji nie jest jeszcze aktywny. Ekran jest przygotowany pod bezpieczne wdrożenie sprawdzania, pobierania i instalacji nowszej wersji programu."
        );
        }
        if (plantsUpdateStatusLabel != null) {
            plantsUpdateStatusLabel.setText(
                "Aktualizacja serwerowa dla bazy roślin nie jest jeszcze aktywna. Docelowo tutaj będą obsługiwane import i aktualizacja danych roślin z serwera."
        );
        }
        if (eppoUpdateStatusLabel != null) {
            eppoUpdateStatusLabel.setText(
                "Aktualizacja serwerowa dla bazy EPPO nie jest jeszcze aktywna. Docelowo tutaj będą obsługiwane import i aktualizacja danych referencyjnych EPPO z serwera."
        );
        }
        if (countriesUpdateStatusLabel != null) {
            countriesUpdateStatusLabel.setText(
                "Aktualizacja serwerowa dla wspólnego słownika krajów nie jest jeszcze aktywna. Docelowo tutaj będą obsługiwane import i aktualizacja słownika krajów z serwera."
        );
        }

        refreshTechnicalState();
        initializeDryRunPreviewState();
    }

    private void initializeDryRunPreviewState() {
        setDryRunStatus("Podgląd / test bez zapisu pokazuje tylko lokalne podsumowanie gotowości. Nie pobiera danych z serwera i nie zapisuje zmian.");
        resetPlantsDryRunPreview();
        resetEppoDryRunPreview();
        resetCountriesDryRunPreview();
    }

    @FXML
    private void previewAllDryRun() {
        refreshTechnicalState();
        plantsDryRunPreviewArea.setText(buildPlantsDryRunPreview());
        eppoDryRunPreviewArea.setText(buildEppoDryRunPreview());
        countriesDryRunPreviewArea.setText(buildCountriesDryRunPreview());
        setDryRunStatus("Wygenerowano pełny podgląd aktualizacji serwerowej dla roślin, EPPO i wspólnego słownika krajów bez pobierania danych z serwera.");
    }

    @FXML
    private void clearAllDryRunPreviews() {
        resetPlantsDryRunPreview();
        resetEppoDryRunPreview();
        resetCountriesDryRunPreview();
        refreshTechnicalState();
        setDryRunStatus("Wyczyszczono wszystkie sekcje podglądu / testu bez zapisu.");
    }

    @FXML
    private void previewPlantsDryRun() {
        refreshTechnicalState();
        plantsDryRunPreviewArea.setText(buildPlantsDryRunPreview());
        setDryRunStatus("Wygenerowano podgląd aktualizacji serwerowej roślin bez pobierania danych z serwera.");
    }

    @FXML
    private void previewEppoDryRun() {
        refreshTechnicalState();
        eppoDryRunPreviewArea.setText(buildEppoDryRunPreview());
        setDryRunStatus("Wygenerowano podgląd aktualizacji serwerowej EPPO bez pobierania danych z serwera.");
    }

    @FXML
    private void previewCountriesDryRun() {
        refreshTechnicalState();
        countriesDryRunPreviewArea.setText(buildCountriesDryRunPreview());
        setDryRunStatus("Wygenerowano podgląd aktualizacji wspólnego słownika krajów bez pobierania danych z serwera.");
    }

    @FXML
    private void clearPlantsDryRunPreview() {
        resetPlantsDryRunPreview();
        refreshTechnicalState();
        setDryRunStatus("Wyczyszczono podgląd aktualizacji serwerowej roślin.");
    }

    @FXML
    private void clearEppoDryRunPreview() {
        resetEppoDryRunPreview();
        refreshTechnicalState();
        setDryRunStatus("Wyczyszczono podgląd aktualizacji serwerowej EPPO.");
    }

    @FXML
    private void clearCountriesDryRunPreview() {
        resetCountriesDryRunPreview();
        refreshTechnicalState();
        setDryRunStatus("Wyczyszczono podgląd wspólnego słownika krajów.");
    }

    private String buildPlantsDryRunPreview() {
        List<com.egen.fitogen.model.Plant> plants = plantService.getAllPlants();
        long passportRequiredCount = plants.stream().filter(com.egen.fitogen.model.Plant::isPassportRequired).count();
        long withEppoCount = plants.stream().filter(plant -> !isBlank(plant.getEppoCode())).count();
        long missingSpeciesCount = plants.stream().filter(plant -> isBlank(plant.getSpecies())).count();
        long missingVisibilityCount = plants.stream().filter(plant -> isBlank(plant.getVisibilityStatus())).count();
        long unusedCount = plants.stream()
                .filter(plant -> "Nieużywany".equalsIgnoreCase(safe(plant.getVisibilityStatus())))
                .count();

        StringBuilder builder = new StringBuilder();
        builder.append("Lokalna baza roślin: ").append(plants.size()).append(" rekordów").append(UiTextUtil.NL);
        builder.append("Rośliny z wymaganym paszportem: ").append(passportRequiredCount).append(UiTextUtil.NL);
        builder.append("Rośliny z informacyjnym kodem EPPO: ").append(withEppoCount).append(UiTextUtil.NL);
        builder.append("Rośliny bez nazwy gatunku: ").append(missingSpeciesCount).append(UiTextUtil.NL);
        builder.append("Rośliny bez statusu widoczności: ").append(missingVisibilityCount).append(UiTextUtil.NL);
        builder.append("Rośliny oznaczone jako Nieużywany: ").append(unusedCount).append(UiTextUtil.DOUBLE_NL);
        builder.append("Ocena gotowości: ")
                .append(buildPlantsReadinessStatus(plants.size(), missingSpeciesCount, missingVisibilityCount))
                .append(UiTextUtil.DOUBLE_NL);
        appendPlantsPreviewSample(builder, plants);
        builder.append(UiTextUtil.NL).append("To jest tylko lokalny podgląd gotowości pod przyszłą aktualizację serwerową roślin.");
        return builder.toString();
    }

    private String buildEppoDryRunPreview() {
        List<com.egen.fitogen.model.EppoCode> codes = eppoCodeService.getAll();
        long activeCount = codes.stream().filter(this::isEppoActive).count();
        long missingCodeCount = codes.stream().filter(code -> isBlank(code.getCode())).count();
        long missingDisplayNameCount = codes.stream()
                .filter(code -> isBlank(code.getDisplaySpeciesName()) && isBlank(code.getDisplayLatinSpeciesName()))
                .count();
        long missingStatusCount = codes.stream().filter(code -> isBlank(code.getStatus())).count();

        StringBuilder builder = new StringBuilder();
        builder.append("Lokalna baza EPPO: ").append(codes.size()).append(" rekordów").append(UiTextUtil.NL);
        builder.append("Rekordy aktywne: ").append(activeCount).append(UiTextUtil.NL);
        builder.append("Rekordy bez kodu: ").append(missingCodeCount).append(UiTextUtil.NL);
        builder.append("Rekordy bez nazwy referencyjnej: ").append(missingDisplayNameCount).append(UiTextUtil.NL);
        builder.append("Rekordy bez statusu: ").append(missingStatusCount).append(UiTextUtil.DOUBLE_NL);
        builder.append("Ocena gotowości: ")
                .append(buildEppoReadinessStatus(codes.size(), missingCodeCount, missingDisplayNameCount))
                .append(UiTextUtil.DOUBLE_NL);
        appendEppoPreviewSample(builder, codes);
        builder.append(UiTextUtil.NL).append("To jest tylko lokalny podgląd gotowości pod przyszłą aktualizację serwerową EPPO.");
        return builder.toString();
    }

    private String buildCountriesDryRunPreview() {
        List<CountryDirectory.CountryEntry> entries = countryDirectoryService.getEntries();
        List<CountryDirectory.CountryEntry> customEntries = countryDirectoryService.getCustomEntries();
        long missingCountryCount = entries.stream().filter(entry -> isBlank(entry.country())).count();
        long missingCodeCount = entries.stream().filter(entry -> isBlank(entry.countryCode())).count();
        long duplicateCodeCount = countDuplicateCountryCodes(entries);

        StringBuilder builder = new StringBuilder();
        builder.append("Wspólny słownik krajów: ").append(entries.size()).append(" rekordów łącznie").append(UiTextUtil.NL);
        builder.append("Wpisy własne użytkownika: ").append(customEntries.size()).append(UiTextUtil.NL);
        builder.append("Rekordy bez nazwy kraju: ").append(missingCountryCount).append(UiTextUtil.NL);
        builder.append("Rekordy bez kodu kraju: ").append(missingCodeCount).append(UiTextUtil.NL);
        builder.append("Powtarzające się kody kraju: ").append(duplicateCodeCount).append(UiTextUtil.DOUBLE_NL);
        builder.append("Ocena gotowości: ")
                .append(buildCountriesReadinessStatus(entries.size(), missingCountryCount, missingCodeCount, duplicateCodeCount))
                .append(UiTextUtil.DOUBLE_NL);
        appendCountriesPreviewSample(builder, entries, customEntries);
        builder.append(UiTextUtil.NL).append("To jest tylko lokalny podgląd gotowości pod przyszłą aktualizację serwerową wspólnego słownika krajów.");
        return builder.toString();
    }

    private void appendPlantsPreviewSample(StringBuilder builder, List<com.egen.fitogen.model.Plant> plants) {
        builder.append("Próbka pierwszych rekordów:").append(UiTextUtil.NL);
        if (plants.isEmpty()) {
            builder.append("- Brak lokalnych rekordów roślin.").append(UiTextUtil.NL);
            return;
        }

        int limit = Math.min(plants.size(), 8);
        for (int i = 0; i < limit; i++) {
            com.egen.fitogen.model.Plant plant = plants.get(i);
            builder.append("- ")
                    .append(buildPlantDisplay(plant))
                    .append(" | status=").append(valueOrDash(plant.getVisibilityStatus()))
                    .append(" | paszport=").append(plant.isPassportRequired() ? "TAK" : "NIE")
                    .append(" | EPPO=").append(valueOrDash(plant.getEppoCode()))
                    .append(UiTextUtil.NL);
        }
    }

    private void appendEppoPreviewSample(StringBuilder builder, List<com.egen.fitogen.model.EppoCode> codes) {
        builder.append("Próbka pierwszych rekordów:").append(UiTextUtil.NL);
        if (codes.isEmpty()) {
            builder.append("- Brak lokalnych rekordów EPPO.").append(UiTextUtil.NL);
            return;
        }

        int limit = Math.min(codes.size(), 8);
        for (int i = 0; i < limit; i++) {
            com.egen.fitogen.model.EppoCode code = codes.get(i);
            builder.append("- ")
                    .append(valueOrDash(code.getCode()))
                    .append(" | nazwa=").append(valueOrDash(code.getDisplaySpeciesName()))
                    .append(" | łacina=").append(valueOrDash(code.getDisplayLatinSpeciesName()))
                    .append(" | status=").append(valueOrDash(code.getStatus()))
                    .append(UiTextUtil.NL);
        }
    }

    private void appendCountriesPreviewSample(StringBuilder builder,
                                              List<CountryDirectory.CountryEntry> entries,
                                              List<CountryDirectory.CountryEntry> customEntries) {
        builder.append("Próbka pierwszych rekordów:").append(UiTextUtil.NL);
        if (entries.isEmpty()) {
            builder.append("- Brak lokalnych rekordów słownika krajów.").append(UiTextUtil.NL);
            return;
        }

        int limit = Math.min(entries.size(), 10);
        for (int i = 0; i < limit; i++) {
            CountryDirectory.CountryEntry entry = entries.get(i);
            builder.append("- ")
                    .append(valueOrDash(entry.country()))
                    .append(" (").append(valueOrDash(entry.countryCode())).append(")");
            if (containsCustomEntry(customEntries, entry)) {
                builder.append(" | wpis własny");
            }
            builder.append(UiTextUtil.NL);
        }
    }

    private void resetPlantsDryRunPreview() {
        if (plantsDryRunPreviewArea != null) {
            plantsDryRunPreviewArea.setText(UiTextUtil.buildEmptyPreviewText("Aktualizacja serwerowa roślin", "Użyj przycisku podglądu, aby zobaczyć lokalne podsumowanie gotowości."));
        }
    }

    private void resetEppoDryRunPreview() {
        if (eppoDryRunPreviewArea != null) {
            eppoDryRunPreviewArea.setText(UiTextUtil.buildEmptyPreviewText("Aktualizacja serwerowa EPPO", "Użyj przycisku podglądu, aby zobaczyć lokalne podsumowanie gotowości."));
        }
    }

    private void resetCountriesDryRunPreview() {
        if (countriesDryRunPreviewArea != null) {
            countriesDryRunPreviewArea.setText(UiTextUtil.buildEmptyPreviewText("wspólnego słownika krajów", "Użyj przycisku podglądu, aby zobaczyć lokalne podsumowanie gotowości."));
        }
    }

    private void refreshTechnicalState() {
        refreshBackupInfo();
        refreshReadinessSummary();
    }

    private void refreshBackupInfo() {
        String lastBackupAt = appSettingsService.getLastBackupAt();
        if (lastBackupInfoLabel == null) {
            return;
        }

        if (lastBackupAt == null || lastBackupAt.isBlank()) {
            lastBackupInfoLabel.setText("Brak informacji o wykonanej kopii zapasowej.");
        } else {
            lastBackupInfoLabel.setText("Ostatnia zapisana kopia zapasowa: " + lastBackupAt);
        }
    }

    private void refreshReadinessSummary() {
        List<com.egen.fitogen.model.Plant> plants = plantService.getAllPlants();
        List<com.egen.fitogen.model.EppoCode> eppoCodes = eppoCodeService.getAll();
        List<CountryDirectory.CountryEntry> countryEntries = countryDirectoryService.getEntries();

        long plantsMissingSpecies = plants.stream().filter(plant -> isBlank(plant.getSpecies())).count();
        long plantsMissingVisibility = plants.stream().filter(plant -> isBlank(plant.getVisibilityStatus())).count();
        long eppoMissingCode = eppoCodes.stream().filter(code -> isBlank(code.getCode())).count();
        long eppoMissingDisplayName = eppoCodes.stream()
                .filter(code -> isBlank(code.getDisplaySpeciesName()) && isBlank(code.getDisplayLatinSpeciesName()))
                .count();
        long countriesMissingCountry = countryEntries.stream().filter(entry -> isBlank(entry.country())).count();
        long countriesMissingCode = countryEntries.stream().filter(entry -> isBlank(entry.countryCode())).count();
        long duplicateCountryCodes = countDuplicateCountryCodes(countryEntries);

        String plantsStatus = buildPlantsReadinessStatus(plants.size(), plantsMissingSpecies, plantsMissingVisibility);
        String eppoStatus = buildEppoReadinessStatus(eppoCodes.size(), eppoMissingCode, eppoMissingDisplayName);
        String countriesStatus = buildCountriesReadinessStatus(
                countryEntries.size(),
                countriesMissingCountry,
                countriesMissingCode,
                duplicateCountryCodes
        );

        StringBuilder builder = new StringBuilder();
        builder.append("Ten ekran jest zarezerwowany wyłącznie dla aktualizacji aplikacji i przyszłych synchronizacji serwerowych. Lokalne importy i eksporty CSV pozostają poza tym modułem.").append(UiTextUtil.DOUBLE_NL);
        builder.append("Rośliny: ").append(plantsStatus)
                .append(" (rekordy: ").append(plants.size())
                .append(", braki gatunku: ").append(plantsMissingSpecies)
                .append(", braki statusu widoczności: ").append(plantsMissingVisibility)
                .append(")").append(UiTextUtil.NL);
        builder.append("EPPO: ").append(eppoStatus)
                .append(" (rekordy: ").append(eppoCodes.size())
                .append(", braki kodu: ").append(eppoMissingCode)
                .append(", braki nazwy: ").append(eppoMissingDisplayName)
                .append(")").append(UiTextUtil.NL);
        builder.append("Kraje: ").append(countriesStatus)
                .append(" (rekordy: ").append(countryEntries.size())
                .append(", braki nazwy: ").append(countriesMissingCountry)
                .append(", braki kodu: ").append(countriesMissingCode)
                .append(", duplikaty kodów: ").append(duplicateCountryCodes)
                .append(")");

        if (readinessSummaryLabel != null) {
            readinessSummaryLabel.setText(builder.toString());
        }
    }

    private void setDryRunStatus(String message) {
        if (dryRunStatusLabel != null) {
            dryRunStatusLabel.setText(message);
        }
    }

    private String buildPlantsReadinessStatus(long totalCount, long missingSpeciesCount, long missingVisibilityCount) {
        if (totalCount == 0) {
            return "brak danych lokalnych do oceny";
        }
        if (missingSpeciesCount > 0) {
            return "wymaga porządkowania danych przed pełną aktualizacją serwerową";
        }
        if (missingVisibilityCount > 0) {
            return "częściowo gotowe — warto uzupełnić status widoczności";
        }
        return "gotowe do bezpiecznego testu bez zapisu po stronie lokalnej";
    }

    private String buildEppoReadinessStatus(long totalCount, long missingCodeCount, long missingDisplayNameCount) {
        if (totalCount == 0) {
            return "brak danych lokalnych do oceny";
        }
        if (missingCodeCount > 0 || missingDisplayNameCount > 0) {
            return "wymaga porządkowania rekordów referencyjnych";
        }
        return "gotowe do bezpiecznego testu bez zapisu po stronie lokalnej";
    }

    private String buildCountriesReadinessStatus(long totalCount,
                                                 long missingCountryCount,
                                                 long missingCodeCount,
                                                 long duplicateCodeCount) {
        if (totalCount == 0) {
            return "brak danych lokalnych do oceny";
        }
        if (missingCountryCount > 0 || missingCodeCount > 0) {
            return "wymaga uzupełnienia wspólnego słownika krajów";
        }
        if (duplicateCodeCount > 0) {
            return "częściowo gotowe — sprawdź duplikaty kodów kraju";
        }
        return "gotowe do bezpiecznego testu bez zapisu po stronie lokalnej";
    }

    private long countDuplicateCountryCodes(List<CountryDirectory.CountryEntry> entries) {
        Set<String> uniqueCodes = new LinkedHashSet<>();
        long duplicates = 0;
        for (CountryDirectory.CountryEntry entry : entries) {
            String normalizedCode = normalizeCountryCode(entry.countryCode());
            if (normalizedCode == null) {
                continue;
            }
            if (!uniqueCodes.add(normalizedCode)) {
                duplicates++;
            }
        }
        return duplicates;
    }

    private boolean containsCustomEntry(List<CountryDirectory.CountryEntry> customEntries,
                                        CountryDirectory.CountryEntry candidate) {
        String candidateCountry = normalizeCountry(candidate.country());
        String candidateCode = normalizeCountryCode(candidate.countryCode());
        for (CountryDirectory.CountryEntry entry : customEntries) {
            if (equalsNormalized(candidateCountry, normalizeCountry(entry.country()))
                    && equalsNormalized(candidateCode, normalizeCountryCode(entry.countryCode()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isEppoActive(com.egen.fitogen.model.EppoCode code) {
        return isBlank(code.getStatus()) || "ACTIVE".equalsIgnoreCase(safe(code.getStatus()));
    }

    private String buildPlantDisplay(com.egen.fitogen.model.Plant plant) {
        StringBuilder builder = new StringBuilder();
        appendDisplayPart(builder, plant.getSpecies());
        appendDisplayPart(builder, plant.getRootstock());
        appendDisplayPart(builder, plant.getVariety());
        return builder.isEmpty() ? "—" : builder.toString();
    }

    private void appendDisplayPart(StringBuilder builder, String value) {
        String normalized = safe(value);
        if (normalized.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(normalized);
    }

    private String valueOrDash(String value) {
        String normalized = safe(value);
        return normalized.isBlank() ? "—" : normalized;
    }

    private boolean isBlank(String value) {
        return safe(value).isBlank();
    }

    private String normalizeCountry(String value) {
        String normalized = safe(value);
        return normalized.isBlank() ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeCountryCode(String value) {
        String normalized = safe(value);
        return normalized.isBlank() ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private boolean equalsNormalized(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
