package com.egen.fitogen.service;

import com.egen.fitogen.dto.CountryImportPreviewResult;
import com.egen.fitogen.dto.CountryImportPreviewRow;
import com.egen.fitogen.dto.CsvImportExecutionResult;
import com.egen.fitogen.ui.util.CountryCatalog;
import com.egen.fitogen.ui.util.CountryDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CountryCsvImportService {

    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_UPDATE_EXISTING = "UPDATE_EXISTING";
    private static final String STATUS_MATCHING_EXISTING = "MATCHING_EXISTING";
    private static final String STATUS_DUPLICATE_IN_FILE = "DUPLICATE_IN_FILE";
    private static final String STATUS_INVALID = "INVALID";

    private final CountryDirectoryService countryDirectoryService;
    private final AppSettingsService appSettingsService;

    public CountryCsvImportService(CountryDirectoryService countryDirectoryService,
                                   AppSettingsService appSettingsService) {
        this.countryDirectoryService = countryDirectoryService;
        this.appSettingsService = appSettingsService;
    }

    public CountryImportPreviewResult preview(Path csvPath) {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            return preview(reader, csvPath.getFileName().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać pliku importu słownika krajów: " + csvPath, e);
        }
    }

    public CountryImportPreviewResult preview(Reader reader, String sourceName) {
        List<String> lines = new BufferedReader(reader).lines().toList();
        if (lines.isEmpty()) {
            return new CountryImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0, 0);
        }

        String headerLine = firstNonBlank(lines);
        if (headerLine == null) {
            return new CountryImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0, 0);
        }

        char delimiter = resolveDelimiter(headerLine);
        List<String> headers = parseCsvLine(headerLine, delimiter);
        int countryIndex = indexOf(headers, "country", "kraj", "countrynamedisplay");
        int codeIndex = indexOf(headers, "countrycode", "kodkraju", "code");
        if (countryIndex < 0 || codeIndex < 0) {
            throw new IllegalArgumentException("Plik CSV słownika krajów musi zawierać kolumny country / kraj oraz countryCode / kodKraju.");
        }

        Map<String, CountryDirectory.CountryEntry> existingByCountry = mapByCountry(countryDirectoryService.getEntries());
        Map<String, CountryDirectory.CountryEntry> existingByCode = mapByCode(countryDirectoryService.getEntries());
        Map<String, Integer> seenCountries = new LinkedHashMap<>();
        Map<String, Integer> seenCodes = new LinkedHashMap<>();
        List<CountryImportPreviewRow> rows = new ArrayList<>();
        int newCount = 0;
        int updateCount = 0;
        int matchingCount = 0;
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
            String country = normalizeDisplay(valueAt(cells, countryIndex));
            String code = normalizeCode(valueAt(cells, codeIndex));
            String status;
            String message = "";

            if (country == null || code == null) {
                status = STATUS_INVALID;
                invalidCount++;
                if (country == null) {
                    message += "Brak nazwy kraju. ";
                }
                if (code == null) {
                    message += "Brak kodu kraju. ";
                }
            } else if (!code.matches("[A-Z]{2,3}")) {
                status = STATUS_INVALID;
                invalidCount++;
                message = "Kod kraju powinien zawierać 2 lub 3 litery alfabetu łacińskiego.";
            } else {
                String countryKey = normalizeKey(country);
                String codeKey = normalizeCodeKey(code);
                Integer existingCountryRow = seenCountries.putIfAbsent(countryKey, rowNo);
                Integer existingCodeRow = seenCodes.putIfAbsent(codeKey, rowNo);
                if (existingCountryRow != null) {
                    status = STATUS_DUPLICATE_IN_FILE;
                    duplicateCount++;
                    message = "Duplikat kraju w pliku importu. Pierwszy wpis: wiersz " + existingCountryRow + ".";
                } else if (existingCodeRow != null) {
                    status = STATUS_DUPLICATE_IN_FILE;
                    duplicateCount++;
                    message = "Duplikat kodu kraju w pliku importu. Pierwszy wpis: wiersz " + existingCodeRow + ".";
                } else {
                    CountryDirectory.CountryEntry existingByCountryEntry = existingByCountry.get(countryKey);
                    CountryDirectory.CountryEntry existingByCodeEntry = existingByCode.get(codeKey);
                    if (existingByCountryEntry != null) {
                        if (code.equalsIgnoreCase(existingByCountryEntry.countryCode())) {
                            status = STATUS_MATCHING_EXISTING;
                            matchingCount++;
                            message = "Wpis już istnieje we wspólnym słowniku krajów.";
                        } else {
                            status = STATUS_UPDATE_EXISTING;
                            updateCount++;
                            message = "Kraj istnieje, ale ma inny kod kraju w słowniku. Import zaktualizuje wpis własny.";
                        }
                    } else if (existingByCodeEntry != null && !country.equalsIgnoreCase(existingByCodeEntry.country())) {
                        status = STATUS_INVALID;
                        invalidCount++;
                        message = "Kod kraju jest już używany przez inny kraj w słowniku: " + existingByCodeEntry.country() + ".";
                    } else {
                        status = STATUS_NEW;
                        newCount++;
                        message = "Nowy wpis własny wspólnego słownika krajów.";
                    }
                }
            }

            rows.add(new CountryImportPreviewRow(rowNo, fallback(country), fallback(code), status, message.trim()));
        }

        return new CountryImportPreviewResult(sourceName, delimiter, headers, rows, newCount, updateCount, matchingCount, duplicateCount, invalidCount);
    }

    public CsvImportExecutionResult applyPreview(CountryImportPreviewResult previewResult) {
        if (previewResult == null) {
            throw new IllegalArgumentException("Brak podglądu importu słownika krajów do wykonania.");
        }

        List<String> problems = new ArrayList<>();
        int appliedCount = 0;
        int skippedCount = 0;
        int rejectedCount = 0;
        Map<String, CountryDirectory.CountryEntry> customByCountry = mapByCountry(countryDirectoryService.getCustomEntries());

        for (CountryImportPreviewRow row : previewResult.getRows()) {
            if (STATUS_MATCHING_EXISTING.equals(row.getStatus())) {
                skippedCount++;
                continue;
            }
            if (STATUS_DUPLICATE_IN_FILE.equals(row.getStatus()) || STATUS_INVALID.equals(row.getStatus())) {
                rejectedCount++;
                problems.add("Wiersz " + row.getRowNumber() + ": " + fallback(row.getMessage()));
                continue;
            }
            if (!STATUS_NEW.equals(row.getStatus()) && !STATUS_UPDATE_EXISTING.equals(row.getStatus())) {
                rejectedCount++;
                problems.add("Wiersz " + row.getRowNumber() + ": nieobsługiwany status importu " + fallback(row.getStatus()));
                continue;
            }

            String country = normalizeDisplay(row.getCountry());
            String code = normalizeCode(row.getCountryCode());
            if (country == null || code == null) {
                rejectedCount++;
                problems.add("Wiersz " + row.getRowNumber() + ": brak poprawnego kraju lub kodu kraju po normalizacji.");
                continue;
            }

            String baseCode = CountryCatalog.findCodeByCountry(country);
            String countryKey = normalizeKey(country);
            if (baseCode != null && baseCode.equalsIgnoreCase(code)) {
                customByCountry.remove(countryKey);
            } else {
                customByCountry.put(countryKey, new CountryDirectory.CountryEntry(country, code));
            }
            appliedCount++;
        }

        try {
            appSettingsService.saveCustomCountryEntries(new ArrayList<>(customByCountry.values()));
        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się zapisać wspólnego słownika krajów.", e);
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
        return "Jeden plik CSV wspólnego słownika krajów. Wymagane kolumny: country (kraj) oraz countryCode (kod kraju). Import działa add/update przez wpisy własne współdzielone przez Kontrahentów, EPPO i dane podmiotu.";
    }

    private Map<String, CountryDirectory.CountryEntry> mapByCountry(List<CountryDirectory.CountryEntry> entries) {
        Map<String, CountryDirectory.CountryEntry> result = new LinkedHashMap<>();
        if (entries == null) {
            return result;
        }
        for (CountryDirectory.CountryEntry entry : entries) {
            String key = normalizeKey(entry == null ? null : entry.country());
            if (key != null && !result.containsKey(key)) {
                result.put(key, entry);
            }
        }
        return result;
    }

    private Map<String, CountryDirectory.CountryEntry> mapByCode(List<CountryDirectory.CountryEntry> entries) {
        Map<String, CountryDirectory.CountryEntry> result = new LinkedHashMap<>();
        if (entries == null) {
            return result;
        }
        for (CountryDirectory.CountryEntry entry : entries) {
            String key = normalizeCodeKey(entry == null ? null : entry.countryCode());
            if (key != null && !result.containsKey(key)) {
                result.put(key, entry);
            }
        }
        return result;
    }

    private String firstNonBlank(List<String> lines) {
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                return line;
            }
        }
        return null;
    }

    private char resolveDelimiter(String headerLine) {
        int semicolons = countOccurrences(headerLine, ';');
        int commas = countOccurrences(headerLine, ',');
        return semicolons >= commas ? ';' : ',';
    }

    private int countOccurrences(String value, char character) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == character) {
                count++;
            }
        }
        return count;
    }

    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> cells = new ArrayList<>();
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
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cells.add(current.toString());
        return cells;
    }

    private int indexOf(List<String> headers, String... candidates) {
        if (headers == null) {
            return -1;
        }
        for (int i = 0; i < headers.size(); i++) {
            String normalized = normalizeHeader(headers.get(i));
            for (String candidate : candidates) {
                if (normalized.equals(normalizeHeader(candidate))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String valueAt(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return "";
        }
        return cells.get(index);
    }

    private String normalizeHeader(String value) {
        return normalizeAscii(value == null ? "" : value).replaceAll("[^a-z0-9]", "");
    }

    private String normalizeKey(String value) {
        String normalized = normalizeDisplay(value);
        return normalized == null ? null : normalizeAscii(normalized);
    }

    private String normalizeCodeKey(String value) {
        String normalized = normalizeCode(value);
        return normalized == null ? null : normalized;
    }

    private String normalizeDisplay(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeAscii(String value) {
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }

    private String fallback(String value) {
        return value == null ? "" : value;
    }
}
