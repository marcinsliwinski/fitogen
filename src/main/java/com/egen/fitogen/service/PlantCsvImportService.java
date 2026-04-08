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
import java.util.HashSet;
import java.util.List;
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
        if (lines.isEmpty()) return new PlantImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0);

        String headerLine = firstNonBlank(lines);
        if (headerLine == null) return new PlantImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0);

        char delimiter = resolveDelimiter(headerLine);
        List<String> headers = parseCsvLine(headerLine, delimiter);

        int speciesIndex = indexOf(headers, "species", "gatunek");
        int varietyIndex = indexOf(headers, "variety", "odmiana");
        int rootstockIndex = indexOf(headers, "rootstock", "podkladka", "podkładka");
        int latinIndex = indexOf(headers, "latinspeciesname", "nazwalacinska");
        int eppoIndex = indexOf(headers, "eppocode", "kodeppo", "eppo");
        int passportIndex = indexOf(headers, "passportrequired", "paszport");
        int visibilityIndex = indexOf(headers, "visibilitystatus", "statuswidocznosci");
        if (speciesIndex < 0) throw new IllegalArgumentException("Plik CSV dla roślin musi zawierać kolumnę species / gatunek.");

        Set<String> existing = new HashSet<>();
        for (Plant p : plantService.getAllPlants()) existing.add(key(p.getSpecies(), p.getVariety(), p.getRootstock()));
        Set<String> inFile = new HashSet<>();
        List<PlantImportPreviewRow> rows = new ArrayList<>();
        int newCount=0, existCount=0, dupCount=0, invalidCount=0;
        boolean skipped=false; int rowNo=0;
        for (String line : lines) {
            rowNo++;
            if (!skipped && line.equals(headerLine)) { skipped = true; continue; }
            if (line == null || line.isBlank()) continue;
            List<String> cells = parseCsvLine(line, delimiter);
            String species = valueAt(cells, speciesIndex);
            String variety = valueAt(cells, varietyIndex);
            String rootstock = valueAt(cells, rootstockIndex);
            String latin = valueAt(cells, latinIndex);
            String eppo = valueAt(cells, eppoIndex);
            boolean passport = parseBool(valueAt(cells, passportIndex), appSettingsService.isPlantPassportRequiredForAll());
            String visibility = normalizeVisibility(valueAt(cells, visibilityIndex));

            String message = "";
            String status;
            if (species.isBlank() || !isValidVisibility(visibility)) {
                status = STATUS_INVALID; invalidCount++;
                if (species.isBlank()) message += "Brak wymaganego gatunku. ";
                if (!isValidVisibility(visibility)) message += "Nieprawidłowy status widoczności.";
            } else {
                String k = key(species, variety, rootstock);
                if (inFile.contains(k)) {
                    status = STATUS_DUPLICATE_IN_FILE; dupCount++; message = "Duplikat w pliku importu.";
                } else if (existing.contains(k)) {
                    status = STATUS_MATCHING_EXISTING; existCount++; inFile.add(k); message = "Rekord już istnieje w bazie.";
                } else {
                    status = STATUS_NEW; newCount++; inFile.add(k);
                }
            }
            rows.add(new PlantImportPreviewRow(rowNo, species, variety, rootstock, latin, eppo, passport, visibility, status, message.trim()));
        }
        return new PlantImportPreviewResult(sourceName, delimiter, headers, rows, newCount, existCount, dupCount, invalidCount);
    }

    public String getSupportedColumnsSummary() {
        return "Obsługiwane kolumny importu: gatunek (species), odmiana (variety), podkładka (rootstock), nazwa łacińska (latinSpeciesName), kod EPPO (eppoCode), wymagany paszport (passportRequired), status widoczności (visibilityStatus).";
    }

    private int indexOf(List<String> headers, String... aliases) {
        for (int i=0;i<headers.size();i++) {
            String norm = norm(headers.get(i));
            for (String alias : aliases) if (norm(alias).equals(norm)) return i;
        }
        return -1;
    }

    private String firstNonBlank(List<String> lines) { for (String l: lines) if (l != null && !l.isBlank()) return l; return null; }
    private String valueAt(List<String> cells, int index) { return index < 0 || index >= cells.size() || cells.get(index) == null ? "" : cells.get(index).trim(); }
    private String key(String species, String variety, String rootstock) { return low(species)+"|"+low(variety)+"|"+low(rootstock); }
    private String low(String value) { return value == null ? "" : value.trim().toLowerCase(); }
    private boolean parseBool(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String n = norm(raw);
        return n.equals("true") || n.equals("tak") || n.equals("yes") || n.equals("1");
    }
    private String normalizeVisibility(String raw) {
        if (raw == null || raw.isBlank()) return "Używany";
        String n = norm(raw);
        if (n.equals("uzywany") || n.equals("used") || n.equals("active")) return "Używany";
        if (n.equals("nieuzywany") || n.equals("unused") || n.equals("inactive")) return "Nieużywany";
        return raw.trim();
    }
    private boolean isValidVisibility(String value) { return "Używany".equals(value) || "Nieużywany".equals(value); }
    private String norm(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }
    private char resolveDelimiter(String line) {
        int s = count(line, ';'), c = count(line, ','), t = count(line, '\t');
        if (t >= s && t >= c) return '\t';
        if (c > s) return ',';
        return ';';
    }
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
            } else {
                current.append(ch);
            }
        }
        vals.add(current.toString());
        return vals;
    }
}
