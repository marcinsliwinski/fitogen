package com.egen.fitogen.service;

import com.egen.fitogen.dto.CsvImportExecutionResult;
import com.egen.fitogen.dto.SubjectDataImportPreviewResult;
import com.egen.fitogen.dto.SubjectDataImportPreviewRow;
import com.egen.fitogen.model.IssuerProfile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubjectDataCsvImportService {

    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_UPDATE_EXISTING = "UPDATE_EXISTING";
    private static final String STATUS_MATCHING_EXISTING = "MATCHING_EXISTING";
    private static final String STATUS_DUPLICATE_IN_FILE = "DUPLICATE_IN_FILE";
    private static final String STATUS_INVALID = "INVALID";

    private final AppSettingsService appSettingsService;
    private final CountryDirectoryService countryDirectoryService;

    public SubjectDataCsvImportService(AppSettingsService appSettingsService,
                                       CountryDirectoryService countryDirectoryService) {
        this.appSettingsService = appSettingsService;
        this.countryDirectoryService = countryDirectoryService;
    }

    public SubjectDataImportPreviewResult preview(Path csvPath) {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            return preview(reader, csvPath.getFileName().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać pliku importu danych podmiotu: " + csvPath, e);
        }
    }

    public SubjectDataImportPreviewResult preview(Reader reader, String sourceName) {
        List<String> lines = new BufferedReader(reader).lines().toList();
        if (lines.isEmpty()) {
            return new SubjectDataImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0, 0);
        }

        String headerLine = firstNonBlank(lines);
        if (headerLine == null) {
            return new SubjectDataImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0, 0);
        }

        char delimiter = resolveDelimiter(headerLine);
        List<String> headers = parseCsvLine(headerLine, delimiter);
        int nurseryNameIndex = indexOf(headers, "nurseryname", "issuername", "nazwa", "nazwapodmiotu");
        int countryIndex = indexOf(headers, "country", "kraj");
        int countryCodeIndex = indexOf(headers, "countrycode", "kodkraju");
        int postalCodeIndex = indexOf(headers, "postalcode", "kodpocztowy");
        int cityIndex = indexOf(headers, "city", "miasto");
        int streetIndex = indexOf(headers, "street", "ulica", "ulicainr");
        int noStreetIndex = indexOf(headers, "nostreet", "brakulicy");
        int phytosanitaryNumberIndex = indexOf(headers, "phytosanitarynumber", "numerfitosanitarny", "nrfitosanitarny");

        if (nurseryNameIndex < 0 || countryIndex < 0 || countryCodeIndex < 0 || postalCodeIndex < 0
                || cityIndex < 0 || streetIndex < 0 || noStreetIndex < 0 || phytosanitaryNumberIndex < 0) {
            throw new IllegalArgumentException("Plik CSV danych podmiotu musi zawierać kolumny nurseryName, country, countryCode, postalCode, city, street, noStreet i phytosanitaryNumber.");
        }

        IssuerProfile current = appSettingsService.getIssuerProfile();
        List<SubjectDataImportPreviewRow> rows = new ArrayList<>();
        boolean headerSkipped = false;
        boolean validProfileAlreadySeen = false;
        int rowNo = 0;
        int newCount = 0;
        int updateCount = 0;
        int matchingCount = 0;
        int duplicateCount = 0;
        int invalidCount = 0;

        for (String line : lines) {
            rowNo++;
            if (!headerSkipped && line.equals(headerLine)) {
                headerSkipped = true;
                continue;
            }
            if (line == null || line.isBlank()) {
                continue;
            }

            List<String> cells = parseCsvLine(line, delimiter);
            String nurseryName = normalizeText(valueAt(cells, nurseryNameIndex));
            String country = normalizeText(valueAt(cells, countryIndex));
            String countryCode = normalizeCode(valueAt(cells, countryCodeIndex));
            String postalCode = normalizeText(valueAt(cells, postalCodeIndex));
            String city = normalizeText(valueAt(cells, cityIndex));
            String street = normalizeText(valueAt(cells, streetIndex));
            boolean noStreet = parseBool(valueAt(cells, noStreetIndex));
            String phytosanitaryNumber = normalizeText(valueAt(cells, phytosanitaryNumberIndex));

            if (country == null && countryCode != null) {
                String resolvedCountry = countryDirectoryService.findCountryByCode(countryCode);
                if (resolvedCountry != null) {
                    country = resolvedCountry;
                }
            }
            if (countryCode == null && country != null) {
                String resolvedCode = countryDirectoryService.findCodeByCountry(country);
                if (resolvedCode != null) {
                    countryCode = normalizeCode(resolvedCode);
                }
            }

            String status;
            String message = "";
            if (allBlank(nurseryName, country, countryCode, postalCode, city, street, phytosanitaryNumber) && !noStreet) {
                status = STATUS_INVALID;
                invalidCount++;
                message = "Wiersz nie zawiera żadnych danych podmiotu.";
            } else if (countryCode != null && !countryCode.matches("[A-Z]{2,3}")) {
                status = STATUS_INVALID;
                invalidCount++;
                message = "Kod kraju powinien zawierać 2 lub 3 litery alfabetu łacińskiego.";
            } else if (country != null && countryCode != null && !isCountryPairConsistent(country, countryCode)) {
                status = STATUS_INVALID;
                invalidCount++;
                message = "Para kraj / kod kraju nie jest spójna ze wspólnym słownikiem krajów.";
            } else if (validProfileAlreadySeen) {
                status = STATUS_DUPLICATE_IN_FILE;
                duplicateCount++;
                message = "Plik danych podmiotu powinien zawierać tylko jeden rekord profilu.";
            } else {
                validProfileAlreadySeen = true;
                IssuerProfile rowProfile = toProfile(nurseryName, country, countryCode, postalCode, city, street, noStreet, phytosanitaryNumber);
                if (isSameProfile(current, rowProfile)) {
                    status = STATUS_MATCHING_EXISTING;
                    matchingCount++;
                    message = "Profil danych podmiotu już odpowiada zawartości pliku.";
                } else if (isProfileEmpty(current)) {
                    status = STATUS_NEW;
                    newCount++;
                    message = "Nowy profil danych podmiotu do zapisania w ustawieniach.";
                } else {
                    status = STATUS_UPDATE_EXISTING;
                    updateCount++;
                    message = "Import zaktualizuje istniejący profil danych podmiotu.";
                }
            }

            rows.add(new SubjectDataImportPreviewRow(
                    rowNo,
                    fallback(nurseryName),
                    fallback(country),
                    fallback(countryCode),
                    fallback(postalCode),
                    fallback(city),
                    fallback(street),
                    noStreet,
                    fallback(phytosanitaryNumber),
                    status,
                    message
            ));
        }

        return new SubjectDataImportPreviewResult(sourceName, delimiter, headers, rows, newCount, updateCount, matchingCount, duplicateCount, invalidCount);
    }

    public CsvImportExecutionResult applyPreview(SubjectDataImportPreviewResult previewResult) {
        if (previewResult == null) {
            throw new IllegalArgumentException("Brak podglądu importu danych podmiotu do wykonania.");
        }

        List<String> problems = new ArrayList<>();
        int addedCount = 0;
        int skippedCount = 0;
        int rejectedCount = 0;

        for (SubjectDataImportPreviewRow row : previewResult.getRows()) {
            if (STATUS_MATCHING_EXISTING.equals(row.getStatus())) {
                skippedCount++;
                continue;
            }
            if (STATUS_DUPLICATE_IN_FILE.equals(row.getStatus()) || STATUS_INVALID.equals(row.getStatus())) {
                rejectedCount++;
                appendProblem(problems, row.getRowNumber(), fallback(row.getMessage()));
                continue;
            }
            if (!STATUS_NEW.equals(row.getStatus()) && !STATUS_UPDATE_EXISTING.equals(row.getStatus())) {
                rejectedCount++;
                appendProblem(problems, row.getRowNumber(), "Nieobsługiwany status importu: " + fallback(row.getStatus()));
                continue;
            }

            try {
                IssuerProfile profile = toProfile(
                        normalizeText(row.getNurseryName()),
                        normalizeText(row.getCountry()),
                        normalizeCode(row.getCountryCode()),
                        normalizeText(row.getPostalCode()),
                        normalizeText(row.getCity()),
                        normalizeText(row.getStreet()),
                        row.isNoStreet(),
                        normalizeText(row.getPhytosanitaryNumber())
                );
                appSettingsService.saveIssuerProfile(profile);
                addedCount++;
            } catch (Exception e) {
                rejectedCount++;
                appendProblem(problems, row.getRowNumber(), fallback(e.getMessage()));
            }
        }

        return new CsvImportExecutionResult(
                previewResult.getSourceName(),
                previewResult.getTotalRowsCount(),
                addedCount,
                skippedCount,
                rejectedCount,
                problems
        );
    }

    public String getSupportedColumnsSummary() {
        return "Jeden plik CSV danych podmiotu. Wymagane kolumny: nurseryName, country, countryCode, postalCode, city, street, noStreet, phytosanitaryNumber. Import zapisuje profil szkółki / wystawcy wykorzystywany w dokumentach.";
    }

    private boolean isCountryPairConsistent(String country, String countryCode) {
        String resolvedCode = countryDirectoryService.findCodeByCountry(country);
        String resolvedCountry = countryDirectoryService.findCountryByCode(countryCode);
        boolean codeMatches = resolvedCode == null || normalizeCode(resolvedCode).equals(normalizeCode(countryCode));
        boolean countryMatches = resolvedCountry == null || normalizeText(resolvedCountry).equalsIgnoreCase(normalizeText(country));
        return codeMatches && countryMatches;
    }

    private IssuerProfile toProfile(String nurseryName,
                                    String country,
                                    String countryCode,
                                    String postalCode,
                                    String city,
                                    String street,
                                    boolean noStreet,
                                    String phytosanitaryNumber) {
        IssuerProfile profile = new IssuerProfile();
        profile.setNurseryName(nurseryName);
        profile.setCountry(country);
        profile.setCountryCode(countryCode);
        profile.setPostalCode(postalCode);
        profile.setCity(city);
        profile.setStreet(street);
        profile.setNoStreet(noStreet);
        profile.setPhytosanitaryNumber(phytosanitaryNumber);
        return profile;
    }

    private boolean isSameProfile(IssuerProfile current, IssuerProfile candidate) {
        if (current == null && candidate == null) {
            return true;
        }
        if (current == null || candidate == null) {
            return false;
        }
        return safe(current.getNurseryName()).equalsIgnoreCase(safe(candidate.getNurseryName()))
                && safe(current.getCountry()).equalsIgnoreCase(safe(candidate.getCountry()))
                && safe(current.getCountryCode()).equalsIgnoreCase(safe(candidate.getCountryCode()))
                && safe(current.getPostalCode()).equalsIgnoreCase(safe(candidate.getPostalCode()))
                && safe(current.getCity()).equalsIgnoreCase(safe(candidate.getCity()))
                && safe(current.getStreet()).equalsIgnoreCase(safe(candidate.getStreet()))
                && current.isNoStreet() == candidate.isNoStreet()
                && safe(current.getPhytosanitaryNumber()).equalsIgnoreCase(safe(candidate.getPhytosanitaryNumber()));
    }

    private boolean isProfileEmpty(IssuerProfile profile) {
        return profile == null || allBlank(
                normalizeText(profile.getNurseryName()),
                normalizeText(profile.getCountry()),
                normalizeCode(profile.getCountryCode()),
                normalizeText(profile.getPostalCode()),
                normalizeText(profile.getCity()),
                normalizeText(profile.getStreet()),
                normalizeText(profile.getPhytosanitaryNumber())
        );
    }

    private boolean allBlank(String... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private void appendProblem(List<String> problems, int rowNumber, String message) {
        if (problems == null || message == null || message.isBlank() || problems.size() >= 10) {
            return;
        }
        problems.add("#" + rowNumber + " " + message.trim());
    }

    private int indexOf(List<String> headers, String... aliases) {
        for (int i = 0; i < headers.size(); i++) {
            String normalizedHeader = normalizeHeader(headers.get(i));
            for (String alias : aliases) {
                if (normalizeHeader(alias).equals(normalizedHeader)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        return normalized;
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
        if (line == null) {
            return cells;
        }

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

    private String valueAt(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return "";
        }
        String value = cells.get(index);
        return value == null ? "" : value.trim();
    }

    private String normalizeText(String value) {
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

    private boolean parseBool(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized)
                || "1".equals(normalized)
                || "tak".equals(normalized)
                || "yes".equals(normalized)
                || "y".equals(normalized);
    }

    private String fallback(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
