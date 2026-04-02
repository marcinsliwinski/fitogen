package com.egen.fitogen.service;

import com.egen.fitogen.dto.ContrahentImportPreviewResult;
import com.egen.fitogen.dto.ContrahentImportPreviewRow;
import com.egen.fitogen.model.Contrahent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContrahentCsvImportService {

    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_MATCHING_EXISTING = "MATCHING_EXISTING";
    private static final String STATUS_DUPLICATE_IN_FILE = "DUPLICATE_IN_FILE";
    private static final String STATUS_INVALID = "INVALID";

    private final ContrahentService contrahentService;
    private final CountryDirectoryService countryDirectoryService;

    public ContrahentCsvImportService(ContrahentService contrahentService,
                                      CountryDirectoryService countryDirectoryService) {
        this.contrahentService = contrahentService;
        this.countryDirectoryService = countryDirectoryService;
    }

    public ContrahentImportPreviewResult preview(Path csvPath) {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            return preview(reader, csvPath.getFileName().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać pliku importu kontrahentów: " + csvPath, e);
        }
    }

    public ContrahentImportPreviewResult preview(Reader reader, String sourceName) {
        List<String> lines = new BufferedReader(reader).lines().toList();
        if (lines.isEmpty()) {
            return new ContrahentImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0);
        }

        String headerLine = firstNonBlank(lines);
        if (headerLine == null) {
            return new ContrahentImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0);
        }

        char delimiter = resolveDelimiter(headerLine);
        List<String> headerCells = parseCsvLine(headerLine, delimiter);
        HeaderMapping mapping = buildHeaderMapping(headerCells);

        if (mapping.nameIndex < 0) {
            throw new IllegalArgumentException("Plik CSV dla kontrahentów musi zawierać kolumnę name / nazwa.");
        }

        Set<String> existingKeys = loadExistingKeys();
        Set<String> fileKeys = new HashSet<>();
        List<ContrahentImportPreviewRow> rows = new ArrayList<>();
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
            Candidate candidate = mapCandidate(cells, mapping);
            if (candidate.isCompletelyEmpty()) {
                continue;
            }

            List<String> issues = new ArrayList<>();
            if (candidate.name == null || candidate.name.isBlank()) {
                issues.add("Brak wymaganej nazwy kontrahenta.");
            }

            if ((candidate.country == null || candidate.country.isBlank())
                    && (candidate.countryCode == null || candidate.countryCode.isBlank())) {
                issues.add("Brak kraju lub kodu kraju.");
            } else {
                if ((candidate.country == null || candidate.country.isBlank()) && candidate.countryCode != null && !candidate.countryCode.isBlank()) {
                    String resolvedCountry = countryDirectoryService.findCountryByCode(candidate.countryCode);
                    if (resolvedCountry != null) {
                        candidate.country = resolvedCountry;
                    }
                }
                if ((candidate.countryCode == null || candidate.countryCode.isBlank()) && candidate.country != null && !candidate.country.isBlank()) {
                    String resolvedCode = countryDirectoryService.findCodeByCountry(candidate.country);
                    if (resolvedCode != null) {
                        candidate.countryCode = resolvedCode;
                    }
                }

                if (candidate.countryCode != null && !candidate.countryCode.isBlank() && candidate.countryCode.trim().length() != 2) {
                    issues.add("Kod kraju musi mieć 2 litery.");
                }

                if (candidate.country != null && !candidate.country.isBlank()
                        && candidate.countryCode != null && !candidate.countryCode.isBlank()) {
                    String expectedCode = countryDirectoryService.findCodeByCountry(candidate.country);
                    String expectedCountry = countryDirectoryService.findCountryByCode(candidate.countryCode);
                    if (expectedCode != null && !expectedCode.equalsIgnoreCase(candidate.countryCode.trim())) {
                        issues.add("Kod kraju nie zgadza się ze wspólnym słownikiem krajów.");
                    }
                    if (expectedCountry != null && !expectedCountry.equalsIgnoreCase(candidate.country.trim())) {
                        issues.add("Kraj nie zgadza się ze wspólnym słownikiem krajów.");
                    }
                }
            }

            String key = buildKey(candidate.name, candidate.countryCode, candidate.city);
            String status;
            if (!issues.isEmpty()) {
                status = STATUS_INVALID;
                invalidRowsCount++;
            } else if (fileKeys.contains(key)) {
                status = STATUS_DUPLICATE_IN_FILE;
                duplicateInFileCount++;
                issues.add("Duplikat w pliku importu.");
            } else if (existingKeys.contains(key)) {
                status = STATUS_MATCHING_EXISTING;
                matchingExistingCount++;
                issues.add("Rekord już istnieje w bazie.");
                fileKeys.add(key);
            } else {
                status = STATUS_NEW;
                newRowsCount++;
                fileKeys.add(key);
            }

            rows.add(new ContrahentImportPreviewRow(
                    physicalRowNumber,
                    candidate.name,
                    candidate.country,
                    upperOrEmpty(candidate.countryCode),
                    candidate.city,
                    candidate.postalCode,
                    candidate.phytosanitaryNumber,
                    candidate.supplier,
                    candidate.client,
                    status,
                    String.join(" ", issues)
            ));
        }

        return new ContrahentImportPreviewResult(
                sourceName,
                delimiter,
                mapping.resolvedHeaders,
                rows,
                newRowsCount,
                matchingExistingCount,
                duplicateInFileCount,
                invalidRowsCount
        );
    }

    public String getSupportedColumnsSummary() {
        return "Obsługiwane kolumny CSV: name/nazwa, country/kraj, countryCode/kodKraju, city/miasto, postalCode, street, "
                + "phytosanitaryNumber, supplier/dostawca, client/odbiorca.";
    }

    private Candidate mapCandidate(List<String> cells, HeaderMapping mapping) {
        Candidate candidate = new Candidate();
        candidate.name = valueAt(cells, mapping.nameIndex);
        candidate.country = valueAt(cells, mapping.countryIndex);
        candidate.countryCode = upperOrEmpty(valueAt(cells, mapping.countryCodeIndex));
        candidate.city = valueAt(cells, mapping.cityIndex);
        candidate.postalCode = valueAt(cells, mapping.postalCodeIndex);
        candidate.street = valueAt(cells, mapping.streetIndex);
        candidate.phytosanitaryNumber = valueAt(cells, mapping.phytosanitaryNumberIndex);
        candidate.supplier = parseBoolean(valueAt(cells, mapping.supplierIndex));
        candidate.client = parseBoolean(valueAt(cells, mapping.clientIndex));
        return candidate;
    }

    private HeaderMapping buildHeaderMapping(List<String> headerCells) {
        HeaderMapping mapping = new HeaderMapping();
        for (int i = 0; i < headerCells.size(); i++) {
            String normalized = normalizeHeader(headerCells.get(i));
            if (matches(normalized, "name", "nazwa", "contrahentname")) {
                mapping.nameIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "country", "kraj")) {
                mapping.countryIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "countrycode", "kodkraju", "code")) {
                mapping.countryCodeIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "city", "miasto")) {
                mapping.cityIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "postalcode", "kodpocztowy")) {
                mapping.postalCodeIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "street", "ulica", "adres")) {
                mapping.streetIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "phytosanitarynumber", "fitosanitarny", "numerfitosanitarny")) {
                mapping.phytosanitaryNumberIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "supplier", "dostawca")) {
                mapping.supplierIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            } else if (matches(normalized, "client", "odbiorca", "klient")) {
                mapping.clientIndex = i;
                mapping.resolvedHeaders.add(headerCells.get(i));
            }
        }
        return mapping;
    }

    private Set<String> loadExistingKeys() {
        Set<String> keys = new HashSet<>();
        for (Contrahent contrahent : contrahentService.getAllContrahents()) {
            keys.add(buildKey(contrahent.getName(), contrahent.getCountryCode(), contrahent.getCity()));
        }
        return keys;
    }

    private String buildKey(String name, String countryCode, String city) {
        return normalizeValue(name) + "|" + normalizeValue(countryCode) + "|" + normalizeValue(city);
    }

    private boolean parseBoolean(String value) {
        String normalized = normalizeHeader(value);
        return normalized.equals("true") || normalized.equals("tak") || normalized.equals("yes") || normalized.equals("1");
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

    private String upperOrEmpty(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim().toLowerCase();
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
        private int nameIndex = -1;
        private int countryIndex = -1;
        private int countryCodeIndex = -1;
        private int cityIndex = -1;
        private int postalCodeIndex = -1;
        private int streetIndex = -1;
        private int phytosanitaryNumberIndex = -1;
        private int supplierIndex = -1;
        private int clientIndex = -1;
        private final List<String> resolvedHeaders = new ArrayList<>();
    }

    private static class Candidate {
        private String name;
        private String country;
        private String countryCode;
        private String city;
        private String postalCode;
        private String street;
        private String phytosanitaryNumber;
        private boolean supplier;
        private boolean client;

        private boolean isCompletelyEmpty() {
            return blank(name) && blank(country) && blank(countryCode) && blank(city)
                    && blank(postalCode) && blank(street) && blank(phytosanitaryNumber);
        }

        private boolean blank(String value) {
            return value == null || value.isBlank();
        }
    }
}
