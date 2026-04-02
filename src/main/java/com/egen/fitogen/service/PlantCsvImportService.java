package com.egen.fitogen.service;

import com.egen.fitogen.dto.PlantImportPreviewResult;
import com.egen.fitogen.dto.PlantImportPreviewRow;
import com.egen.fitogen.model.Plant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlantCsvImportService {

    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_MATCHING_EXISTING = "MATCHING_EXISTING";
    private static final String STATUS_DUPLICATE_IN_FILE = "DUPLICATE_IN_FILE";
    private static final String STATUS_INVALID = "INVALID";

    private final PlantService plantService;
    private final AppSettingsService appSettingsService;

    public PlantCsvImportService(PlantService plantService, AppSettingsService appSettingsService) {
        this.plantService = plantService;
        this.appSettingsService = appSettingsService;
    }

    public PlantImportPreviewResult preview(Path csvPath) {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            return preview(reader, csvPath.getFileName().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać pliku importu roślin: " + csvPath, e);
        }
    }

    public PlantImportPreviewResult preview(Reader reader, String sourceName) {
        List<String> lines = new BufferedReader(reader).lines().toList();
        if (lines.isEmpty()) {
            return new PlantImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0);
        }

        String headerLine = firstNonBlank(lines);
        if (headerLine == null) {
            return new PlantImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0);
        }

        char delimiter = resolveDelimiter(headerLine);
        List<String> headerCells = parseCsvLine(headerLine, delimiter);
        HeaderMapping mapping = buildHeaderMapping(headerCells);

        if (mapping.speciesIndex < 0) {
            throw new IllegalArgumentException(
                    "Plik CSV dla roślin musi zawierać kolumnę species / gatunek."
            );
        }

        Set<String> existingKeys = loadExistingKeys();
        Set<String> fileKeys = new HashSet<>();

        List<PlantImportPreviewRow> previewRows = new ArrayList<>();
        int newRowsCount = 0;
        int matchingExistingCount = 0;
        int duplicateInFileCount = 0;
        int invalidRowsCount = 0;

        boolean headerConsumed = false;
        int physicalRowNumber = 0;

        for (String line : lines) {
            physicalRowNumber++;

            if (!headerConsumed && line.equals(headerLine)) {
                headerConsumed = true;
                continue;
            }

            if (line == null || line.isBlank()) {
                continue;
            }

            List<String> cells = parseCsvLine(line, delimiter);
            PlantCandidate candidate = mapCandidate(cells, mapping);

            if (candidate.isCompletelyEmpty()) {
                continue;
            }

            List<String> issues = new ArrayList<>();
            if (candidate.species == null || candidate.species.isBlank()) {
                issues.add("Brak wymaganego gatunku.");
            }
            if (!isValidVisibility(candidate.visibilityStatus)) {
                issues.add("Nieprawidłowy status widoczności. Dozwolone: Używany / Nieużywany.");
            }

            String plantKey = buildPlantKey(candidate.species, candidate.variety, candidate.rootstock);
            String status;
            if (!issues.isEmpty()) {
                status = STATUS_INVALID;
                invalidRowsCount++;
            } else if (fileKeys.contains(plantKey)) {
                status = STATUS_DUPLICATE_IN_FILE;
                duplicateInFileCount++;
                issues.add("Duplikat w pliku importu.");
            } else if (existingKeys.contains(plantKey)) {
                status = STATUS_MATCHING_EXISTING;
                matchingExistingCount++;
                issues.add("Rekord już istnieje w bazie.");
                fileKeys.add(plantKey);
            } else {
                status = STATUS_NEW;
                newRowsCount++;
                fileKeys.add(plantKey);
            }

            previewRows.add(new PlantImportPreviewRow(
                    physicalRowNumber,
                    candidate.species,
                    candidate.variety,
                    candidate.rootstock,
                    candidate.latinSpeciesName,
                    candidate.eppoCode,
                    candidate.passportRequired,
                    candidate.visibilityStatus,
                    status,
                    String.join(" ", issues)
            ));
        }

        return new PlantImportPreviewResult(
                sourceName,
                delimiter,
                mapping.resolvedHeaders,
                previewRows,
                newRowsCount,
                matchingExistingCount,
                duplicateInFileCount,
                invalidRowsCount
        );
    }

    public String getSupportedColumnsSummary() {
        return "Obsługiwane kolumny CSV: species/gatunek, variety/odmiana, rootstock/podkladka, "
                + "latinSpeciesName/nazwaLacinska, eppoCode, passportRequired, visibilityStatus.";
    }

    private HeaderMapping buildHeaderMapping(List<String> headerCells) {
        HeaderMapping mapping = new HeaderMapping();
        for (int i = 0; i < headerCells.size(); i++) {
            String normalized = normalizeHeader(headerCells.get(i));
            if (matches(normalized, "species", "gatunek")) {
                mapping.speciesIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "variety", "odmiana")) {
                mapping.varietyIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "rootstock", "podkladka", "podkładka")) {
                mapping.rootstockIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "latinspeciesname", "latinspecies", "latin", "nazwalacinska")) {
                mapping.latinSpeciesNameIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "eppocode", "eppo", "kodeppo")) {
                mapping.eppoCodeIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "passportrequired", "passport", "paszport")) {
                mapping.passportRequiredIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "visibilitystatus", "visibility", "statuswidocznosci")) {
                mapping.visibilityStatusIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            }
        }
        return mapping;
    }

    private PlantCandidate mapCandidate(List<String> cells, HeaderMapping mapping) {
        PlantCandidate candidate = new PlantCandidate();
        candidate.species = valueAt(cells, mapping.speciesIndex);
        candidate.variety = valueAt(cells, mapping.varietyIndex);
        candidate.rootstock = valueAt(cells, mapping.rootstockIndex);
        candidate.latinSpeciesName = valueAt(cells, mapping.latinSpeciesNameIndex);
        candidate.eppoCode = valueAt(cells, mapping.eppoCodeIndex);

        String passportRaw = valueAt(cells, mapping.passportRequiredIndex);
        candidate.passportRequired = parsePassportRequired(passportRaw);

        String visibilityRaw = valueAt(cells, mapping.visibilityStatusIndex);
        candidate.visibilityStatus = normalizeVisibility(visibilityRaw);

        return candidate;
    }

    private boolean parsePassportRequired(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return appSettingsService.isPlantPassportRequiredForAll();
        }

        String normalized = normalizeHeader(rawValue);
        return normalized.equals("true")
                || normalized.equals("tak")
                || normalized.equals("yes")
                || normalized.equals("1");
    }

    private String normalizeVisibility(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "Używany";
        }

        String normalized = normalizeHeader(rawValue);
        if (normalized.equals("uzywany") || normalized.equals("used") || normalized.equals("active")) {
            return "Używany";
        }
        if (normalized.equals("nieuzywany") || normalized.equals("unused") || normalized.equals("inactive")) {
            return "Nieużywany";
        }
        return rawValue.trim();
    }

    private boolean isValidVisibility(String value) {
        return "Używany".equals(value) || "Nieużywany".equals(value);
    }

    private Set<String> loadExistingKeys() {
        Set<String> keys = new HashSet<>();
        for (Plant plant : plantService.getAllPlants()) {
            String key = buildPlantKey(plant.getSpecies(), plant.getVariety(), plant.getRootstock());
            if (!key.isBlank()) {
                keys.add(key);
            }
        }
        return keys;
    }

    private String buildPlantKey(String species, String variety, String rootstock) {
        String normalizedSpecies = normalizeValue(species);
        if (normalizedSpecies.isBlank()) {
            return "";
        }
        return normalizedSpecies + "|" + normalizeValue(variety) + "|" + normalizeValue(rootstock);
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    private String firstNonBlank(List<String> lines) {
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                return line;
            }
        }
        return null;
    }

    private String valueAt(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return "";
        }
        return cells.get(index) == null ? "" : cells.get(index).trim();
    }

    private boolean matches(String normalized, String... options) {
        for (String option : options) {
            if (normalizeHeader(option).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private char resolveDelimiter(String headerLine) {
        Map<Character, Integer> counts = new LinkedHashMap<>();
        counts.put(';', countChar(headerLine, ';'));
        counts.put(',', countChar(headerLine, ','));
        counts.put('\t', countChar(headerLine, '\t'));

        char best = ';';
        int bestCount = -1;
        for (Map.Entry<Character, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    private int countChar(String value, char c) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);

            if (currentChar == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (currentChar == delimiter && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(currentChar);
        }

        values.add(current.toString());
        return values;
    }

    private static class HeaderMapping {
        private int speciesIndex = -1;
        private int varietyIndex = -1;
        private int rootstockIndex = -1;
        private int latinSpeciesNameIndex = -1;
        private int eppoCodeIndex = -1;
        private int passportRequiredIndex = -1;
        private int visibilityStatusIndex = -1;
        private final List<String> resolvedHeaders = new ArrayList<>();
    }

    private static class PlantCandidate {
        private String species;
        private String variety;
        private String rootstock;
        private String latinSpeciesName;
        private String eppoCode;
        private boolean passportRequired;
        private String visibilityStatus;

        private boolean isCompletelyEmpty() {
            return blank(species)
                    && blank(variety)
                    && blank(rootstock)
                    && blank(latinSpeciesName)
                    && blank(eppoCode)
                    && blank(visibilityStatus);
        }

        private boolean blank(String value) {
            return value == null || value.isBlank();
        }
    }
}