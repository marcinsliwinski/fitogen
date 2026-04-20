package com.egen.fitogen.service;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.dto.CountryImportPreviewResult;
import com.egen.fitogen.dto.CsvImportExecutionResult;
import com.egen.fitogen.dto.EppoDictionaryImportPreviewResult;
import com.egen.fitogen.dto.EppoDictionaryImportPreviewRow;
import com.egen.fitogen.dto.PlantImportPreviewResult;
import com.egen.fitogen.dto.PlantImportPreviewRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.egen.fitogen.database.SqlitePlantRepository;

public class BootstrapStarterPackService {

    @FunctionalInterface
    public interface ProgressListener {
        void update(String message, double progress);
    }

    public static final String FG1_PACKAGE_CODE = "FG1";
    public static final String COUNTRIES_RESOURCE = "/bootstrap/fg1/kraje-import-fg1.csv";
    public static final String PLANTS_RESOURCE = "/bootstrap/fg1/rosliny-import-fg1.csv";
    public static final String EPPO_RESOURCE = "/bootstrap/fg1/slowniki-eppo-import-fg1.csv";

    public boolean isFg1PackageAvailable() {
        return resourceExists(COUNTRIES_RESOURCE)
                && resourceExists(PLANTS_RESOURCE)
                && resourceExists(EPPO_RESOURCE);
    }

    public String importFg1StarterPack() {
        return importFg1StarterPack(null);
    }

    public String importFg1StarterPack(ProgressListener progressListener) {
        notifyProgress(progressListener, "Przygotowanie pakietu startowego FG1...", 0.02);
        if (!isFg1PackageAvailable()) {
            throw new IllegalStateException("Nie znaleziono pełnego pakietu startowego FG1 w zasobach aplikacji.");
        }

        CountryCsvImportService countryImportService = new CountryCsvImportService(
                AppContext.getCountryDirectoryService(),
                AppContext.getAppSettingsService()
        );
        PlantCsvImportService plantImportService = new PlantCsvImportService(
                AppContext.getPlantService(),
                AppContext.getAppSettingsService()
        );
        EppoDictionaryCsvImportService eppoImportService = new EppoDictionaryCsvImportService(
                AppContext.getEppoCodeService(),
                AppContext.getEppoZoneService(),
                AppContext.getEppoCodeSpeciesLinkService(),
                AppContext.getEppoCodeZoneLinkService()
        );

        CountryImportPreviewResult countriesPreview;
        PlantImportPreviewResult plantsPreview;
        EppoDictionaryImportPreviewResult eppoPreview;

        try (Reader countriesReader = openResourceReader(COUNTRIES_RESOURCE)) {
            notifyProgress(progressListener, "Analiza pliku krajów FG1...", 0.12);
            countriesPreview = countryImportService.preview(countriesReader, resourceFileName(COUNTRIES_RESOURCE));
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać pakietu startowego FG1.", e);
        }

        try (Reader plantsReader = openResourceReader(PLANTS_RESOURCE)) {
            notifyProgress(progressListener, "Analiza pliku roślin FG1...", 0.26);
            plantsPreview = plantImportService.preview(plantsReader, resourceFileName(PLANTS_RESOURCE));
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać pakietu startowego FG1.", e);
        }

        try (Reader eppoReader = openResourceReader(EPPO_RESOURCE)) {
            notifyProgress(progressListener, "Analiza słowników EPPO FG1...", 0.40);
            eppoPreview = eppoImportService.preview(eppoReader, resourceFileName(EPPO_RESOURCE));
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać pakietu startowego FG1.", e);
        }

        validatePreview(countriesPreview.getTotalRowsCount() > 0, "Pakiet krajów FG1 jest pusty.");
        validatePreview(plantsPreview.getTotalRowsCount() > 0, "Pakiet roślin FG1 jest pusty.");
        validatePreview(eppoPreview.getTotalRowsCount() > 0, "Pakiet słowników EPPO FG1 jest pusty.");

        validatePreview(countriesPreview.getInvalidRowsCount() == 0 && countriesPreview.getDuplicateInFileCount() == 0,
                "Pakiet krajów FG1 zawiera błędne lub zduplikowane rekordy.");
        validatePreview(plantsPreview.getInvalidRowsCount() == 0 && plantsPreview.getDuplicateInFileCount() == 0,
                "Pakiet roślin FG1 zawiera błędne lub zduplikowane rekordy.");
        validatePreview(eppoPreview.getInvalidRowsCount() == 0 && eppoPreview.getDuplicateInFileCount() == 0,
                "Pakiet słowników EPPO FG1 zawiera błędne lub zduplikowane rekordy.");

        notifyProgress(progressListener, "Sprawdzanie spójności pakietu FG1...", 0.52);
        validateCrossPackageConsistency(countriesPreview, plantsPreview, eppoPreview);

        notifyProgress(progressListener, "Import krajów FG1...", 0.64);
        CsvImportExecutionResult countriesResult = countryImportService.applyPreview(countriesPreview);
        notifyProgress(progressListener, "Import roślin FG1...", 0.76);
        CsvImportExecutionResult plantsResult = plantImportService.applyPreview(plantsPreview);
        notifyProgress(progressListener, "Import słowników EPPO FG1...", 0.88);
        CsvImportExecutionResult eppoResult = eppoImportService.applyPreview(eppoPreview);

        validateExecution(countriesResult, "Pakiet krajów FG1");
        validateExecution(plantsResult, "Pakiet roślin FG1");
        validateExecution(eppoResult, "Pakiet słowników EPPO FG1");

        String summary = "Zasilono bazę pakietem startowym FG1: kraje " + countriesResult.getAddedCount()
                + ", rośliny " + plantsResult.getAddedCount()
                + ", słowniki EPPO " + eppoResult.getAddedCount() + ".";
        if (AppContext.getAuditLogService() != null) {
            AppContext.getAuditLogService().log("SYSTEM", null, "IMPORT", summary);
        }
        notifyProgress(progressListener, "Pakiet startowy FG1 został zaimportowany.", 1.0);
        return summary;
    }

    public void verifyFg1StarterPackImported() {
        int plantCount = new SqlitePlantRepository().findAll().size();
        int eppoSpeciesLinks = AppContext.getEppoCodeSpeciesLinkService() == null
                ? 0
                : AppContext.getEppoCodeSpeciesLinkService().getAll().size();
        int eppoZoneLinks = AppContext.getEppoCodeZoneLinkService() == null
                ? 0
                : AppContext.getEppoCodeZoneLinkService().getAll().size();

        if (plantCount <= 0) {
            throw new IllegalStateException("Pakiet startowy FG1 nie zapisał roślin w aktywnej bazie.");
        }
        if (eppoSpeciesLinks <= 0 || eppoZoneLinks <= 0) {
            throw new IllegalStateException("Pakiet startowy FG1 nie zapisał pełnych słowników EPPO w aktywnej bazie.");
        }
    }

    private void validateCrossPackageConsistency(CountryImportPreviewResult countriesPreview,
                                                 PlantImportPreviewResult plantsPreview,
                                                 EppoDictionaryImportPreviewResult eppoPreview) {
        Set<String> countryCodes = new HashSet<>();
        countriesPreview.getRows().forEach(row -> {
            if (row.getCountryCode() != null && !row.getCountryCode().isBlank()) {
                countryCodes.add(row.getCountryCode().trim().toUpperCase());
            }
        });

        Set<String> plantSpecies = new HashSet<>();
        Set<String> plantLatinSpecies = new HashSet<>();
        for (PlantImportPreviewRow row : plantsPreview.getRows()) {
            if (row.getSpecies() != null && !row.getSpecies().isBlank()) {
                plantSpecies.add(normalizeKey(row.getSpecies()));
            }
            if (row.getLatinSpeciesName() != null && !row.getLatinSpeciesName().isBlank()) {
                plantLatinSpecies.add(normalizeKey(row.getLatinSpeciesName()));
            }
        }

        List<String> problems = new ArrayList<>();
        for (EppoDictionaryImportPreviewRow row : eppoPreview.getRows()) {
            if ("SPECIES".equalsIgnoreCase(row.getRelationType())) {
                boolean speciesMatch = row.getSpeciesName() != null
                        && !row.getSpeciesName().isBlank()
                        && plantSpecies.contains(normalizeKey(row.getSpeciesName()));
                boolean latinMatch = row.getLatinSpeciesName() != null
                        && !row.getLatinSpeciesName().isBlank()
                        && plantLatinSpecies.contains(normalizeKey(row.getLatinSpeciesName()));
                if (!speciesMatch && !latinMatch) {
                    problems.add("#" + row.getRowNumber() + " brak rośliny dla relacji EPPO SPECIES: "
                            + fallback(row.getSpeciesName(), row.getLatinSpeciesName()));
                }
            } else if ("ZONE".equalsIgnoreCase(row.getRelationType())) {
                String countryCode = row.getCountryCode() == null ? "" : row.getCountryCode().trim().toUpperCase();
                if (!countryCode.isBlank() && !countryCodes.contains(countryCode)) {
                    problems.add("#" + row.getRowNumber() + " brak kraju dla relacji EPPO ZONE: " + countryCode);
                }
            }
            if (problems.size() >= 10) {
                break;
            }
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("Pakiet startowy FG1 nie jest spójny: " + String.join(" | ", problems));
        }
    }


    private void notifyProgress(ProgressListener progressListener, String message, double progress) {
        if (progressListener == null) {
            return;
        }
        progressListener.update(message, progress);
    }

    private void validateExecution(CsvImportExecutionResult result, String sectionLabel) {
        if (result.getRejectedCount() > 0) {
            throw new IllegalStateException(sectionLabel + " nie został zaimportowany poprawnie. Problemy: "
                    + String.join(" | ", result.getProblems()));
        }
    }

    private void validatePreview(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private Reader openResourceReader(String resourcePath) throws IOException {
        InputStream stream = getClass().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IOException("Nie znaleziono zasobu: " + resourcePath);
        }
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private boolean resourceExists(String resourcePath) {
        return getClass().getResource(resourcePath) != null;
    }

    private String resourceFileName(String resourcePath) {
        int separator = resourcePath.lastIndexOf('/');
        return separator >= 0 ? resourcePath.substring(separator + 1) : resourcePath;
    }

    private String fallback(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return "[brak wartości]";
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
