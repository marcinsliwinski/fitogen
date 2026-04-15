package com.egen.fitogen.service;

import com.egen.fitogen.dto.CsvImportExecutionResult;
import com.egen.fitogen.dto.EppoDictionaryImportPreviewResult;
import com.egen.fitogen.dto.EppoDictionaryImportPreviewRow;
import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoCodeSpeciesLink;
import com.egen.fitogen.model.EppoZone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EppoDictionaryCsvImportService {
    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_UPDATE_EXISTING = "UPDATE_EXISTING";
    private static final String STATUS_MATCHING_EXISTING = "MATCHING_EXISTING";
    private static final String STATUS_DUPLICATE_IN_FILE = "DUPLICATE_IN_FILE";
    private static final String STATUS_INVALID = "INVALID";

    private static final String TYPE_SPECIES = "SPECIES";
    private static final String TYPE_ZONE = "ZONE";

    private final EppoCodeService eppoCodeService;
    private final EppoZoneService eppoZoneService;
    private final EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService;
    private final EppoCodeZoneLinkService eppoCodeZoneLinkService;

    public EppoDictionaryCsvImportService(
            EppoCodeService eppoCodeService,
            EppoZoneService eppoZoneService,
            EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService,
            EppoCodeZoneLinkService eppoCodeZoneLinkService
    ) {
        this.eppoCodeService = eppoCodeService;
        this.eppoZoneService = eppoZoneService;
        this.eppoCodeSpeciesLinkService = eppoCodeSpeciesLinkService;
        this.eppoCodeZoneLinkService = eppoCodeZoneLinkService;
    }

    public EppoDictionaryImportPreviewResult preview(Path csvPath) {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            return preview(reader, csvPath.getFileName().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać pliku importu słowników EPPO: " + csvPath, e);
        }
    }

    public EppoDictionaryImportPreviewResult preview(Reader reader, String sourceName) {
        List<String> lines = new BufferedReader(reader).lines().toList();
        if (lines.isEmpty()) {
            return new EppoDictionaryImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0, 0);
        }

        String headerLine = firstNonBlank(lines);
        if (headerLine == null) {
            return new EppoDictionaryImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0, 0);
        }

        char delimiter = resolveDelimiter(headerLine);
        List<String> headers = parseCsvLine(headerLine, delimiter);

        int relationTypeIndex = indexOf(headers, "relationtype", "typrelacji", "typ");
        int eppoCodeIndex = indexOf(headers, "eppocode", "code", "kodeppo");
        int speciesNameIndex = indexOf(headers, "speciesname", "gatunek");
        int latinSpeciesNameIndex = indexOf(headers, "latinspeciesname", "nazwalacinska");
        int zoneCodeIndex = indexOf(headers, "zonecode", "kodstrefy");
        int zoneNameIndex = indexOf(headers, "zonename", "nazwastrefy");
        int countryCodeIndex = indexOf(headers, "countrycode", "kodkraju", "kraj");
        int passportRequiredIndex = indexOf(headers, "passportrequired", "paszport");
        int codeStatusIndex = indexOf(headers, "codestatus", "statuskodu");
        int zoneStatusIndex = indexOf(headers, "zonestatus", "statusstrefy");

        if (relationTypeIndex < 0 || eppoCodeIndex < 0) {
            throw new IllegalArgumentException("Plik CSV słowników EPPO musi zawierać kolumny relationType / typRelacji oraz eppoCode / kodEPPO.");
        }

        Set<String> inFile = new HashSet<>();
        List<EppoDictionaryImportPreviewRow> rows = new ArrayList<>();
        int newCount = 0;
        int updateCount = 0;
        int existingCount = 0;
        int duplicateCount = 0;
        int invalidCount = 0;
        boolean skippedHeader = false;
        int rowNo = 0;

        for (String line : lines) {
            rowNo++;
            if (!skippedHeader && line.equals(headerLine)) {
                skippedHeader = true;
                continue;
            }
            if (line == null || line.isBlank()) {
                continue;
            }

            List<String> cells = parseCsvLine(line, delimiter);
            String relationType = normalizeRelationType(valueAt(cells, relationTypeIndex));
            String eppoCode = normalizeUpper(valueAt(cells, eppoCodeIndex));
            String speciesName = normalizeText(valueAt(cells, speciesNameIndex));
            String latinSpeciesName = normalizeText(valueAt(cells, latinSpeciesNameIndex));
            String zoneCode = normalizeUpper(valueAt(cells, zoneCodeIndex));
            String zoneName = normalizeText(valueAt(cells, zoneNameIndex));
            String countryCode = normalizeUpper(valueAt(cells, countryCodeIndex));
            boolean passportRequired = parseBool(valueAt(cells, passportRequiredIndex));
            String codeStatus = normalizeStatus(valueAt(cells, codeStatusIndex));
            String zoneStatus = normalizeStatus(valueAt(cells, zoneStatusIndex));

            String message = "";
            String status;
            if (relationType.isBlank() || eppoCode.isBlank()) {
                status = STATUS_INVALID;
                invalidCount++;
                if (relationType.isBlank()) {
                    message += "Brak typu relacji SPECIES/ZONE. ";
                }
                if (eppoCode.isBlank()) {
                    message += "Brak kodu EPPO. ";
                }
            } else if (TYPE_SPECIES.equals(relationType) && speciesName.isBlank() && latinSpeciesName.isBlank()) {
                status = STATUS_INVALID;
                invalidCount++;
                message = "Relacja SPECIES wymaga gatunku lub nazwy łacińskiej.";
            } else if (TYPE_ZONE.equals(relationType) && (zoneCode.isBlank() || zoneName.isBlank() || countryCode.isBlank())) {
                status = STATUS_INVALID;
                invalidCount++;
                message = "Relacja ZONE wymaga kodu strefy, nazwy strefy i kodu kraju.";
            } else {
                String fileKey = buildFileKey(relationType, eppoCode, speciesName, latinSpeciesName, zoneCode, countryCode);
                if (!inFile.add(fileKey)) {
                    status = STATUS_DUPLICATE_IN_FILE;
                    duplicateCount++;
                    message = "Duplikat relacji w pliku importu.";
                } else {
                    status = resolveStatus(relationType, eppoCode, speciesName, latinSpeciesName, zoneCode, zoneName, countryCode, passportRequired, codeStatus, zoneStatus);
                    switch (status) {
                        case STATUS_NEW -> newCount++;
                        case STATUS_UPDATE_EXISTING -> updateCount++;
                        case STATUS_MATCHING_EXISTING -> existingCount++;
                        default -> {
                            invalidCount++;
                            if (message.isBlank()) {
                                message = "Nieobsługiwany status analizy.";
                            }
                        }
                    }
                    if (STATUS_MATCHING_EXISTING.equals(status)) {
                        message = "Relacja już istnieje w słownikach EPPO.";
                    } else if (STATUS_UPDATE_EXISTING.equals(status)) {
                        message = buildUpdateMessage(relationType, eppoCode, zoneCode);
                    }
                }
            }

            rows.add(new EppoDictionaryImportPreviewRow(
                    rowNo,
                    relationType,
                    eppoCode,
                    speciesName,
                    latinSpeciesName,
                    zoneCode,
                    zoneName,
                    countryCode,
                    passportRequired,
                    codeStatus,
                    zoneStatus,
                    status,
                    message.trim()
            ));
        }

        return new EppoDictionaryImportPreviewResult(sourceName, delimiter, headers, rows, newCount, updateCount, existingCount, duplicateCount, invalidCount);
    }

    public CsvImportExecutionResult applyPreview(EppoDictionaryImportPreviewResult previewResult) {
        if (previewResult == null) {
            throw new IllegalArgumentException("Brak podglądu importu słowników EPPO do wykonania.");
        }

        List<String> problems = new ArrayList<>();
        int appliedCount = 0;
        int skippedCount = 0;
        int rejectedCount = 0;

        for (EppoDictionaryImportPreviewRow row : previewResult.getRows()) {
            if (STATUS_MATCHING_EXISTING.equals(row.getStatus())) {
                skippedCount++;
                continue;
            }
            if (STATUS_DUPLICATE_IN_FILE.equals(row.getStatus()) || STATUS_INVALID.equals(row.getStatus())) {
                rejectedCount++;
                appendProblem(problems, row.getRowNumber(), fallbackMessage(row.getMessage(), "Wiersz nie kwalifikuje się do importu."));
                continue;
            }
            if (!STATUS_NEW.equals(row.getStatus()) && !STATUS_UPDATE_EXISTING.equals(row.getStatus())) {
                rejectedCount++;
                appendProblem(problems, row.getRowNumber(), "Nieobsługiwany status wiersza importu: " + fallbackMessage(row.getStatus(), "[brak statusu]"));
                continue;
            }

            try {
                EppoCode code = upsertCode(row);
                if (TYPE_SPECIES.equals(row.getRelationType())) {
                    upsertSpeciesLink(code, row);
                } else if (TYPE_ZONE.equals(row.getRelationType())) {
                    EppoZone zone = upsertZone(row);
                    upsertZoneLink(code, zone);
                }
                appliedCount++;
            } catch (Exception e) {
                rejectedCount++;
                appendProblem(problems, row.getRowNumber(), fallbackMessage(e.getMessage(), "Nie udało się zapisać wiersza słownika EPPO do bazy."));
            }
        }

        return new CsvImportExecutionResult(
                previewResult.getSourceName(),
                previewResult.getTotalRowsCount(),
                appliedCount,
                skippedCount,
                rejectedCount,
                problems
        );
    }

    public String getSupportedColumnsSummary() {
        return "Obsługiwany jest jeden plik CSV słowników EPPO. Wymagane kolumny: relationType (SPECIES lub ZONE), eppoCode. Dla SPECIES użyj speciesName i opcjonalnie latinSpeciesName. Dla ZONE użyj zoneCode, zoneName i countryCode. Opcjonalnie: passportRequired, codeStatus, zoneStatus.";
    }

    private String resolveStatus(
            String relationType,
            String eppoCodeValue,
            String speciesName,
            String latinSpeciesName,
            String zoneCode,
            String zoneName,
            String countryCode,
            boolean passportRequired,
            String codeStatus,
            String zoneStatus
    ) {
        EppoCode existingCode = eppoCodeService.getByCode(eppoCodeValue);
        boolean codeRequiresUpdate = requiresCodeUpdate(existingCode, speciesName, latinSpeciesName, passportRequired, codeStatus);

        if (TYPE_SPECIES.equals(relationType)) {
            if (existingCode == null) {
                return STATUS_NEW;
            }
            boolean linkExists = eppoCodeSpeciesLinkService.getEffectiveSpeciesLinks(existingCode.getId()).stream()
                    .anyMatch(link -> sameSpecies(link, speciesName, latinSpeciesName));
            if (!linkExists) {
                return STATUS_UPDATE_EXISTING;
            }
            return codeRequiresUpdate ? STATUS_UPDATE_EXISTING : STATUS_MATCHING_EXISTING;
        }

        EppoZone existingZone = zoneCode.isBlank() ? null : eppoZoneService.getByCode(zoneCode);
        boolean zoneRequiresUpdate = requiresZoneUpdate(existingZone, zoneName, countryCode, zoneStatus);
        if (existingCode == null || existingZone == null) {
            return STATUS_NEW;
        }

        boolean linkExists = eppoCodeZoneLinkService.getByEppoCodeId(existingCode.getId()).stream()
                .anyMatch(link -> link != null && link.getEppoZoneId() == existingZone.getId());
        if (!linkExists) {
            return STATUS_UPDATE_EXISTING;
        }
        return (codeRequiresUpdate || zoneRequiresUpdate) ? STATUS_UPDATE_EXISTING : STATUS_MATCHING_EXISTING;
    }

    private EppoCode upsertCode(EppoDictionaryImportPreviewRow row) {
        EppoCode existing = eppoCodeService.getByCode(row.getEppoCode());
        EppoCode code = existing != null ? existing : new EppoCode(0, row.getEppoCode(), row.getSpeciesName(), row.getLatinSpeciesName(), row.getLatinSpeciesName(), row.getSpeciesName(), row.isPassportRequired(), row.getCodeStatus());

        if (existing == null) {
            code.setCode(row.getEppoCode());
        }
        if (!row.getSpeciesName().isBlank()) {
            code.setSpeciesName(row.getSpeciesName());
            if (isBlank(code.getCommonName())) {
                code.setCommonName(row.getSpeciesName());
            }
        }
        if (!row.getLatinSpeciesName().isBlank()) {
            code.setLatinSpeciesName(row.getLatinSpeciesName());
            if (isBlank(code.getScientificName())) {
                code.setScientificName(row.getLatinSpeciesName());
            }
        }
        code.setPassportRequired(row.isPassportRequired());
        if (!row.getCodeStatus().isBlank()) {
            code.setStatus(row.getCodeStatus());
        }
        eppoCodeService.save(code);
        return eppoCodeService.getByCode(row.getEppoCode());
    }

    private EppoZone upsertZone(EppoDictionaryImportPreviewRow row) {
        EppoZone existing = eppoZoneService.getByCode(row.getZoneCode());
        EppoZone zone = existing != null ? existing : new EppoZone(0, row.getZoneCode(), row.getZoneName(), row.getCountryCode(), row.getZoneStatus());
        zone.setCode(row.getZoneCode());
        zone.setName(row.getZoneName());
        zone.setCountryCode(row.getCountryCode());
        if (!row.getZoneStatus().isBlank()) {
            zone.setStatus(row.getZoneStatus());
        }
        eppoZoneService.save(zone);
        return eppoZoneService.getByCode(row.getZoneCode());
    }

    private void upsertSpeciesLink(EppoCode code, EppoDictionaryImportPreviewRow row) {
        if (code == null) {
            throw new IllegalStateException("Nie udało się przygotować kodu EPPO dla relacji gatunkowej.");
        }
        boolean exists = eppoCodeSpeciesLinkService.getEffectiveSpeciesLinks(code.getId()).stream()
                .anyMatch(link -> sameSpecies(link, row.getSpeciesName(), row.getLatinSpeciesName()));
        if (!exists) {
            eppoCodeSpeciesLinkService.addLink(code.getId(), row.getSpeciesName(), row.getLatinSpeciesName());
        }
    }

    private void upsertZoneLink(EppoCode code, EppoZone zone) {
        if (code == null || zone == null) {
            throw new IllegalStateException("Nie udało się przygotować relacji kod EPPO → strefa.");
        }
        boolean exists = eppoCodeZoneLinkService.getByEppoCodeId(code.getId()).stream()
                .anyMatch(link -> link != null && link.getEppoZoneId() == zone.getId());
        if (!exists) {
            eppoCodeZoneLinkService.addLink(code.getId(), zone.getId());
        }
    }

    private boolean requiresCodeUpdate(EppoCode existing, String speciesName, String latinSpeciesName, boolean passportRequired, String codeStatus) {
        if (existing == null) {
            return false;
        }
        if (!speciesName.isBlank() && !speciesName.equalsIgnoreCase(normalizeText(existing.getSpeciesName()))) {
            return true;
        }
        if (!latinSpeciesName.isBlank() && !latinSpeciesName.equalsIgnoreCase(normalizeText(existing.getLatinSpeciesName()))) {
            return true;
        }
        if (existing.isPassportRequired() != passportRequired) {
            return true;
        }
        return !codeStatus.isBlank() && !codeStatus.equalsIgnoreCase(normalizeStatus(existing.getStatus()));
    }

    private boolean requiresZoneUpdate(EppoZone existing, String zoneName, String countryCode, String zoneStatus) {
        if (existing == null) {
            return false;
        }
        if (!zoneName.isBlank() && !zoneName.equalsIgnoreCase(normalizeText(existing.getName()))) {
            return true;
        }
        if (!countryCode.isBlank() && !countryCode.equalsIgnoreCase(normalizeUpper(existing.getCountryCode()))) {
            return true;
        }
        return !zoneStatus.isBlank() && !zoneStatus.equalsIgnoreCase(normalizeStatus(existing.getStatus()));
    }

    private boolean sameSpecies(EppoCodeSpeciesLink link, String speciesName, String latinSpeciesName) {
        if (link == null) {
            return false;
        }
        return normalizeText(link.getSpeciesName()).equalsIgnoreCase(normalizeText(speciesName))
                && normalizeText(link.getLatinSpeciesName()).equalsIgnoreCase(normalizeText(latinSpeciesName));
    }

    private String buildUpdateMessage(String relationType, String eppoCode, String zoneCode) {
        if (TYPE_ZONE.equals(relationType)) {
            return "Kod EPPO lub strefa istnieje już w bazie i zostaną zaktualizowane / spięte relacją (" + eppoCode + " ↔ " + zoneCode + ").";
        }
        return "Kod EPPO istnieje już w bazie i zostanie zaktualizowany lub uzupełniony relacją gatunkową.";
    }

    private void appendProblem(List<String> problems, int rowNumber, String message) {
        if (problems == null || message == null || message.isBlank() || problems.size() >= 12) {
            return;
        }
        problems.add("#" + rowNumber + " " + message.trim());
    }

    private String fallbackMessage(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String buildFileKey(String relationType, String eppoCode, String speciesName, String latinSpeciesName, String zoneCode, String countryCode) {
        return relationType + "|" + eppoCode + "|" + normalizeText(speciesName) + "|" + normalizeText(latinSpeciesName) + "|" + zoneCode + "|" + countryCode;
    }

    private int indexOf(List<String> headers, String... aliases) {
        for (int i = 0; i < headers.size(); i++) {
            String normalizedHeader = norm(headers.get(i));
            for (String alias : aliases) {
                if (norm(alias).equals(normalizedHeader)) {
                    return i;
                }
            }
        }
        return -1;
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
        return index < 0 || index >= cells.size() || cells.get(index) == null ? "" : cells.get(index).trim();
    }

    private boolean parseBool(String raw) {
        String value = norm(raw);
        return value.equals("true") || value.equals("tak") || value.equals("yes") || value.equals("1");
    }

    private String normalizeRelationType(String value) {
        String normalized = normalizeUpper(value);
        if (normalized.equals("SPECIES") || normalized.equals("GATUNEK")) {
            return TYPE_SPECIES;
        }
        if (normalized.equals("ZONE") || normalized.equals("STREFA")) {
            return TYPE_ZONE;
        }
        return normalized;
    }

    private String normalizeStatus(String value) {
        String normalized = normalizeUpper(value);
        return normalized.isBlank() ? "ACTIVE" : normalized;
    }

    private String normalizeUpper(String value) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? "" : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private String norm(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private char resolveDelimiter(String line) {
        int semicolons = count(line, ';');
        int commas = count(line, ',');
        int tabs = count(line, '	');
        if (tabs >= semicolons && tabs >= commas) {
            return '	';
        }
        if (commas > semicolons) {
            return ',';
        }
        return ';';
    }

    private int count(String value, char c) {
        int total = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == c) {
                total++;
            }
        }
        return total;
    }

    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == delimiter && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }
}
