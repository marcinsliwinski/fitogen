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
import java.util.List;
import java.util.Set;

public class ContrahentCsvImportService {
    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_MATCHING_EXISTING = "MATCHING_EXISTING";
    private static final String STATUS_DUPLICATE_IN_FILE = "DUPLICATE_IN_FILE";
    private static final String STATUS_INVALID = "INVALID";

    private final ContrahentService contrahentService;
    private final CountryDirectoryService countryDirectoryService;

    public ContrahentCsvImportService(ContrahentService contrahentService, CountryDirectoryService countryDirectoryService) {
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
        if (lines.isEmpty()) return new ContrahentImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0);

        String headerLine = firstNonBlank(lines);
        if (headerLine == null) return new ContrahentImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0);

        char delimiter = resolveDelimiter(headerLine);
        List<String> headers = parseCsvLine(headerLine, delimiter);

        int nameIndex = indexOf(headers, "name", "nazwa");
        int countryIndex = indexOf(headers, "country", "kraj");
        int codeIndex = indexOf(headers, "countrycode", "kodkraju");
        int cityIndex = indexOf(headers, "city", "miasto");
        int postalIndex = indexOf(headers, "postalcode", "kodpocztowy");
        int phytoIndex = indexOf(headers, "phytosanitarynumber", "nrfitosanitarny", "numerfitosanitarny");
        int supplierIndex = indexOf(headers, "supplier", "dostawca");
        int clientIndex = indexOf(headers, "client", "odbiorca");

        if (nameIndex < 0) throw new IllegalArgumentException("Plik CSV dla kontrahentów musi zawierać kolumnę name / nazwa.");

        Set<String> existing = new HashSet<>();
        for (Contrahent c : contrahentService.getAllContrahents()) existing.add(key(c.getName(), c.getCountryCode()));
        Set<String> inFile = new HashSet<>();
        List<ContrahentImportPreviewRow> rows = new ArrayList<>();
        int newCount=0, existCount=0, dupCount=0, invalidCount=0;
        boolean skipped=false; int rowNo=0;
        for (String line : lines) {
            rowNo++;
            if (!skipped && line.equals(headerLine)) { skipped = true; continue; }
            if (line == null || line.isBlank()) continue;
            List<String> cells = parseCsvLine(line, delimiter);
            String name = valueAt(cells, nameIndex);
            String country = valueAt(cells, countryIndex);
            String code = valueAt(cells, codeIndex);
            String city = valueAt(cells, cityIndex);
            String postal = valueAt(cells, postalIndex);
            String phyto = valueAt(cells, phytoIndex);
            boolean supplier = parseBool(valueAt(cells, supplierIndex));
            boolean client = parseBool(valueAt(cells, clientIndex));

            if (!country.isBlank() && code.isBlank()) {
                String found = countryDirectoryService.findCodeByCountry(country);
                if (found != null) code = found;
            }
            if (country.isBlank() && !code.isBlank()) {
                String found = countryDirectoryService.findCountryByCode(code);
                if (found != null) country = found;
            }

            String message = "";
            String status;
            if (name.isBlank()) {
                status = STATUS_INVALID; invalidCount++; message = "Brak nazwy.";
            } else {
                String resolvedCode = country.isBlank() ? null : countryDirectoryService.findCodeByCountry(country);
                if (resolvedCode != null && !code.isBlank() && !resolvedCode.equalsIgnoreCase(code)) {
                    status = STATUS_INVALID; invalidCount++; message = "Niespójny kraj i kod kraju.";
                } else {
                    String k = key(name, code);
                    if (inFile.contains(k)) {
                        status = STATUS_DUPLICATE_IN_FILE; dupCount++; message = "Duplikat w pliku importu.";
                    } else if (existing.contains(k)) {
                        status = STATUS_MATCHING_EXISTING; existCount++; inFile.add(k); message = "Rekord już istnieje w bazie.";
                    } else {
                        status = STATUS_NEW; newCount++; inFile.add(k);
                    }
                }
            }

            rows.add(new ContrahentImportPreviewRow(rowNo, name, country, code, city, postal, phyto, supplier, client, status, message));
        }
        return new ContrahentImportPreviewResult(sourceName, delimiter, headers, rows, newCount, existCount, dupCount, invalidCount);
    }

    public String getSupportedColumnsSummary() {
        return "Obsługiwane kolumny CSV: name/nazwa, country/kraj, countryCode/kodKraju, city/miasto, postalCode, phytosanitaryNumber, supplier/dostawca, client/odbiorca.";
    }

    private int indexOf(List<String> headers, String... aliases) {
        for (int i=0;i<headers.size();i++) {
            String norm = norm(headers.get(i));
            for (String a : aliases) if (norm(a).equals(norm)) return i;
        }
        return -1;
    }
    private String firstNonBlank(List<String> lines) { for (String l: lines) if (l != null && !l.isBlank()) return l; return null; }
    private String valueAt(List<String> cells, int index) { return index < 0 || index >= cells.size() || cells.get(index) == null ? "" : cells.get(index).trim(); }
    private boolean parseBool(String raw) { String n = norm(raw); return n.equals("true") || n.equals("tak") || n.equals("yes") || n.equals("1"); }
    private String key(String name, String code) { return low(name)+"|"+low(code); }
    private String low(String value) { return value == null ? "" : value.trim().toLowerCase(); }
    private String norm(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }
    private char resolveDelimiter(String line) { int s=count(line,';'), c=count(line,','), t=count(line,'\t'); if (t>=s && t>=c) return '\t'; if (c>s) return ','; return ';'; }
    private int count(String value, char c) { int n=0; for (int i=0;i<value.length();i++) if (value.charAt(i)==c) n++; return n; }
    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> vals = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i=0;i<line.length();i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i+1<line.length() && line.charAt(i+1)=='"') { current.append('"'); i++; }
                else inQuotes = !inQuotes;
            } else if (ch == delimiter && !inQuotes) {
                vals.add(current.toString()); current.setLength(0);
            } else current.append(ch);
        }
        vals.add(current.toString());
        return vals;
    }
}
