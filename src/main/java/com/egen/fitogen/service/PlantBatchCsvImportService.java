package com.egen.fitogen.service;

import com.egen.fitogen.dto.CsvImportExecutionResult;
import com.egen.fitogen.dto.PlantBatchImportPreviewResult;
import com.egen.fitogen.dto.PlantBatchImportPreviewRow;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.model.PlantBatchStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PlantBatchCsvImportService {
    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_MATCHING_EXISTING = "MATCHING_EXISTING";
    private static final String STATUS_DUPLICATE_IN_FILE = "DUPLICATE_IN_FILE";
    private static final String STATUS_INVALID = "INVALID";

    private final PlantBatchService plantBatchService;
    private final PlantService plantService;
    private final ContrahentService contrahentService;

    public PlantBatchCsvImportService(PlantBatchService plantBatchService,
                                      PlantService plantService,
                                      ContrahentService contrahentService) {
        this.plantBatchService = plantBatchService;
        this.plantService = plantService;
        this.contrahentService = contrahentService;
    }

    public PlantBatchImportPreviewResult preview(Path csvPath) {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            return preview(reader, csvPath.getFileName().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać pliku importu partii roślin: " + csvPath, e);
        }
    }

    public PlantBatchImportPreviewResult preview(Reader reader, String sourceName) {
        List<String> lines = new BufferedReader(reader).lines().toList();
        if (lines.isEmpty()) {
            return new PlantBatchImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0);
        }

        String headerLine = firstNonBlank(lines);
        if (headerLine == null) {
            return new PlantBatchImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0);
        }

        char delimiter = resolveDelimiter(headerLine);
        List<String> headers = parseCsvLine(headerLine, delimiter);

        int batchIdIndex = indexOf(headers, "batchid", "id");
        int interiorIndex = indexOf(headers, "interiorbatchno", "numerpartiiwewnetrznej");
        int exteriorIndex = indexOf(headers, "exteriorbatchno", "numerpartiizewnetrznej");
        int speciesIndex = indexOf(headers, "species", "gatunek");
        int varietyIndex = indexOf(headers, "variety", "odmiana");
        int rootstockIndex = indexOf(headers, "rootstock", "podkladka", "podkładka");
        int sourceNameIndex = indexOf(headers, "sourcecontrahentname", "sourceoriginname", "zrodlopochodzenia", "źródłopochodzenia", "dostawca", "suppliername");
        int qtyIndex = indexOf(headers, "qty", "ilosc", "ilość");
        int ageIndex = indexOf(headers, "ageyears", "wiekpartii", "wiekpartiil");
        int creationDateIndex = indexOf(headers, "creationdate", "datautworzenia");
        int countryIndex = indexOf(headers, "manufacturercountrycode", "countrycode", "krajpochodzenia");
        int fitoIndex = indexOf(headers, "fitoqualificationcategory", "fitoqualificationcategory", "kategoriakwalifikacji");
        int eppoIndex = indexOf(headers, "eppocode", "kodeppo");
        int zpIndex = indexOf(headers, "zpzone", "zp", "strefazp");
        int internalIndex = indexOf(headers, "internalsource", "wewnetrzne", "wewnętrzne", "isinternalsource");
        int commentsIndex = indexOf(headers, "comments", "uwagi");
        int statusIndex = indexOf(headers, "status");

        if (speciesIndex < 0) {
            throw new IllegalArgumentException("Plik CSV partii roślin musi zawierać kolumnę species / gatunek.");
        }

        Map<String, Plant> plantByKey = buildPlantMap();
        Map<String, Contrahent> contrahentByName = buildContrahentMap();
        ExistingBatchIndex existingIndex = buildExistingBatchIndex();
        Set<String> inFile = new HashSet<>();
        List<PlantBatchImportPreviewRow> rows = new ArrayList<>();
        int newCount = 0;
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
            Integer batchId = parseInteger(valueAt(cells, batchIdIndex));
            String interiorBatchNo = normalizeUppercase(valueAt(cells, interiorIndex));
            String exteriorBatchNo = normalizeSpaces(valueAt(cells, exteriorIndex));
            String species = normalizeSpaces(valueAt(cells, speciesIndex));
            String variety = normalizeSpaces(valueAt(cells, varietyIndex));
            String rootstock = normalizeSpaces(valueAt(cells, rootstockIndex));
            String sourceContrahentName = normalizeSpaces(valueAt(cells, sourceNameIndex));
            Integer qty = parseInteger(valueAt(cells, qtyIndex));
            Integer ageYears = parseInteger(valueAt(cells, ageIndex));
            String creationDate = normalizeSpaces(valueAt(cells, creationDateIndex));
            String manufacturerCountryCode = normalizeUppercase(valueAt(cells, countryIndex));
            String fitoQualificationCategory = normalizeSpaces(valueAt(cells, fitoIndex));
            String eppoCode = normalizeUppercase(valueAt(cells, eppoIndex));
            String zpZone = normalizeUppercase(valueAt(cells, zpIndex));
            boolean internalSource = parseBool(valueAt(cells, internalIndex), false);
            String comments = normalizeSpaces(valueAt(cells, commentsIndex));
            String batchStatus = normalizeStatus(valueAt(cells, statusIndex));

            StringBuilder message = new StringBuilder();
            String status;

            Plant matchedPlant = plantByKey.get(buildPlantKey(species, variety, rootstock));
            if (species.isBlank()) {
                appendMessage(message, "Brak wymaganego gatunku.");
            }
            if (matchedPlant == null) {
                appendMessage(message, "Nie znaleziono rośliny o podanym zestawie gatunek/odmiana/podkładka.");
            }
            if (qty == null || qty <= 0) {
                appendMessage(message, "Ilość musi być dodatnią liczbą całkowitą.");
            }
            if (ageYears == null || ageYears <= 0) {
                appendMessage(message, "Wiek partii musi być dodatnią liczbą całkowitą.");
            }
            if (!creationDate.isBlank() && parseDate(creationDate) == null) {
                appendMessage(message, "Nieprawidłowa data utworzenia. Użyj formatu RRRR-MM-DD.");
            }
            if (!isValidStatus(batchStatus)) {
                appendMessage(message, "Nieprawidłowy status partii.");
            }
            Contrahent matchedContrahent = null;
            if (!internalSource) {
                if (sourceContrahentName.isBlank()) {
                    appendMessage(message, "Dla partii zewnętrznej wymagane jest źródło pochodzenia.");
                } else {
                    matchedContrahent = contrahentByName.get(normalizeKey(sourceContrahentName));
                    if (matchedContrahent == null) {
                        appendMessage(message, "Nie znaleziono kontrahenta o podanej nazwie źródła pochodzenia.");
                    }
                }
            }

            if (message.length() > 0) {
                status = STATUS_INVALID;
                invalidCount++;
            } else {
                String identityKey = buildIdentityKey(batchId, interiorBatchNo, exteriorBatchNo, species, variety, rootstock, sourceContrahentName, creationDate, internalSource);
                if (!inFile.add(identityKey)) {
                    status = STATUS_DUPLICATE_IN_FILE;
                    duplicateCount++;
                    appendMessage(message, "Duplikat partii w pliku importu.");
                } else if (existingIndex.matches(batchId, interiorBatchNo, exteriorBatchNo, species, variety, rootstock, sourceContrahentName, creationDate, internalSource)) {
                    status = STATUS_MATCHING_EXISTING;
                    existingCount++;
                    appendMessage(message, "Partia już istnieje w bazie.");
                } else {
                    status = STATUS_NEW;
                    newCount++;
                }
            }

            rows.add(new PlantBatchImportPreviewRow(
                    rowNo,
                    batchId,
                    interiorBatchNo,
                    exteriorBatchNo,
                    species,
                    variety,
                    rootstock,
                    sourceContrahentName,
                    qty == null ? 0 : qty,
                    ageYears == null ? 0 : ageYears,
                    creationDate,
                    manufacturerCountryCode,
                    fitoQualificationCategory,
                    eppoCode,
                    zpZone,
                    internalSource,
                    comments,
                    batchStatus,
                    status,
                    message.toString()
            ));
        }

        return new PlantBatchImportPreviewResult(sourceName, delimiter, headers, rows, newCount, existingCount, duplicateCount, invalidCount);
    }

    public CsvImportExecutionResult applyPreview(PlantBatchImportPreviewResult previewResult) {
        if (previewResult == null) {
            throw new IllegalArgumentException("Brak podglądu importu partii roślin do wykonania.");
        }

        Map<String, Plant> plantByKey = buildPlantMap();
        Map<String, Contrahent> contrahentByName = buildContrahentMap();
        ExistingBatchIndex existingIndex = buildExistingBatchIndex();
        Set<String> importedInRun = new HashSet<>();
        List<String> problems = new ArrayList<>();
        int addedCount = 0;
        int skippedCount = 0;
        int rejectedCount = 0;

        for (PlantBatchImportPreviewRow row : previewResult.getRows()) {
            if (STATUS_MATCHING_EXISTING.equals(row.getStatus())) {
                skippedCount++;
                continue;
            }
            if (STATUS_DUPLICATE_IN_FILE.equals(row.getStatus())) {
                rejectedCount++;
                appendProblem(problems, row.getRowNumber(), fallbackMessage(row.getMessage(), "Duplikat partii w pliku importu."));
                continue;
            }
            if (STATUS_INVALID.equals(row.getStatus())) {
                rejectedCount++;
                appendProblem(problems, row.getRowNumber(), fallbackMessage(row.getMessage(), "Wiersz nie przeszedł walidacji."));
                continue;
            }
            if (!STATUS_NEW.equals(row.getStatus())) {
                rejectedCount++;
                appendProblem(problems, row.getRowNumber(), "Nieobsługiwany status wiersza importu partii: " + fallbackMessage(row.getStatus(), "[brak statusu]"));
                continue;
            }

            String identityKey = buildIdentityKey(row.getBatchId(), row.getInteriorBatchNo(), row.getExteriorBatchNo(), row.getPlantSpecies(), row.getPlantVariety(), row.getPlantRootstock(), row.getSourceContrahentName(), row.getCreationDate(), row.isInternalSource());
            if (!importedInRun.add(identityKey)) {
                rejectedCount++;
                appendProblem(problems, row.getRowNumber(), "Wiersz powiela partię już zakwalifikowaną w tym samym imporcie.");
                continue;
            }
            if (existingIndex.matches(row.getBatchId(), row.getInteriorBatchNo(), row.getExteriorBatchNo(), row.getPlantSpecies(), row.getPlantVariety(), row.getPlantRootstock(), row.getSourceContrahentName(), row.getCreationDate(), row.isInternalSource())) {
                skippedCount++;
                appendProblem(problems, row.getRowNumber(), "Partia została pominięta, bo istnieje już w bazie w momencie wykonywania importu.");
                continue;
            }

            try {
                Plant plant = plantByKey.get(buildPlantKey(row.getPlantSpecies(), row.getPlantVariety(), row.getPlantRootstock()));
                Contrahent contrahent = row.isInternalSource() ? null : contrahentByName.get(normalizeKey(row.getSourceContrahentName()));
                plantBatchService.addBatch(mapRowToBatch(row, plant, contrahent));
                addedCount++;
            } catch (Exception e) {
                rejectedCount++;
                appendProblem(problems, row.getRowNumber(), fallbackMessage(e.getMessage(), "Nie udało się zapisać partii roślin do bazy."));
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
        return "Obsługiwane kolumny importu: batchId, interiorBatchNo, exteriorBatchNo, species, variety, rootstock, sourceContrahentName, qty, ageYears, creationDate, manufacturerCountryCode, fitoQualificationCategory, eppoCode, zpZone, internalSource, comments, status.";
    }

    private PlantBatch mapRowToBatch(PlantBatchImportPreviewRow row, Plant plant, Contrahent contrahent) {
        PlantBatch batch = new PlantBatch();
        batch.setPlantId(plant == null ? 0 : plant.getId());
        batch.setInteriorBatchNo(normalizeUppercase(row.getInteriorBatchNo()));
        batch.setExteriorBatchNo(normalizeSpaces(row.getExteriorBatchNo()));
        batch.setQty(Math.max(0, row.getQty()));
        batch.setAgeYears(Math.max(0, row.getAgeYears()));
        batch.setCreationDate(parseDate(row.getCreationDate()) == null ? LocalDate.now() : parseDate(row.getCreationDate()));
        batch.setManufacturerCountryCode(normalizeUppercase(row.getManufacturerCountryCode()));
        batch.setFitoQualificationCategory(normalizeSpaces(row.getFitoQualificationCategory()));
        batch.setEppoCode(normalizeUppercase(row.getEppoCode()));
        batch.setZpZone(normalizeUppercase(row.getZpZone()));
        batch.setContrahentId(row.isInternalSource() || contrahent == null ? 0 : contrahent.getId());
        batch.setInternalSource(row.isInternalSource());
        batch.setComments(normalizeSpaces(row.getComments()));
        batch.setStatus(parseStatus(row.getBatchStatus()));
        return batch;
    }

    private ExistingBatchIndex buildExistingBatchIndex() {
        ExistingBatchIndex index = new ExistingBatchIndex();
        for (PlantBatch batch : plantBatchService.getAllBatches()) {
            index.add(batch, resolvePlantSummary(batch.getPlantId()), resolveContrahentName(batch.getContrahentId()));
        }
        return index;
    }

    private Map<String, Plant> buildPlantMap() {
        Map<String, Plant> result = new HashMap<>();
        for (Plant plant : plantService.getAllPlants()) {
            result.put(buildPlantKey(plant.getSpecies(), plant.getVariety(), plant.getRootstock()), plant);
        }
        return result;
    }

    private Map<String, Contrahent> buildContrahentMap() {
        Map<String, Contrahent> result = new HashMap<>();
        for (Contrahent contrahent : contrahentService.getAllContrahents()) {
            result.put(normalizeKey(contrahent.getName()), contrahent);
        }
        return result;
    }

    private String resolvePlantSummary(int plantId) {
        for (Plant plant : plantService.getAllPlants()) {
            if (plant != null && plant.getId() == plantId) {
                return buildPlantKey(plant.getSpecies(), plant.getVariety(), plant.getRootstock());
            }
        }
        return "";
    }

    private String resolveContrahentName(int contrahentId) {
        if (contrahentId <= 0) {
            return "";
        }
        for (Contrahent contrahent : contrahentService.getAllContrahents()) {
            if (contrahent != null && contrahent.getId() == contrahentId) {
                return safe(contrahent.getName());
            }
        }
        return "";
    }

    private String buildPlantKey(String species, String variety, String rootstock) {
        return normalizeKey(species) + "|" + normalizeKey(variety) + "|" + normalizeKey(rootstock);
    }

    private String buildIdentityKey(Integer batchId,
                                    String interiorBatchNo,
                                    String exteriorBatchNo,
                                    String species,
                                    String variety,
                                    String rootstock,
                                    String sourceContrahentName,
                                    String creationDate,
                                    boolean internalSource) {
        if (batchId != null && batchId > 0) {
            return "ID:" + batchId;
        }
        if (!safe(interiorBatchNo).isBlank()) {
            return "INT:" + normalizeKey(interiorBatchNo);
        }
        return "EXT:" + buildPlantKey(species, variety, rootstock)
                + "|SRC:" + normalizeKey(sourceContrahentName)
                + "|NO:" + normalizeKey(exteriorBatchNo)
                + "|DATE:" + normalizeKey(creationDate)
                + "|INTERNAL:" + internalSource;
    }

    private String firstNonBlank(List<String> lines) {
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                return line;
            }
        }
        return null;
    }

    private int indexOf(List<String> headers, String... aliases) {
        for (int i = 0; i < headers.size(); i++) {
            String normalized = normalizeKey(headers.get(i));
            for (String alias : aliases) {
                if (normalizeKey(alias).equals(normalized)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String valueAt(List<String> cells, int index) {
        return index < 0 || index >= cells.size() || cells.get(index) == null ? "" : cells.get(index).trim();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean parseBool(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = normalizeKey(value);
        return normalized.equals("true") || normalized.equals("tak") || normalized.equals("yes") || normalized.equals("1");
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return PlantBatchStatus.ACTIVE.name();
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isValidStatus(String value) {
        return PlantBatchStatus.ACTIVE.name().equals(value) || PlantBatchStatus.CANCELLED.name().equals(value);
    }

    private PlantBatchStatus parseStatus(String value) {
        if (PlantBatchStatus.CANCELLED.name().equalsIgnoreCase(safe(value))) {
            return PlantBatchStatus.CANCELLED;
        }
        return PlantBatchStatus.ACTIVE;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('ł', 'l')
                .replace('Ł', 'L');
        normalized = normalized.replaceAll("[^A-Za-z0-9]", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeSpaces(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? "" : normalized;
    }

    private String normalizeUppercase(String value) {
        return normalizeSpaces(value).toUpperCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void appendMessage(StringBuilder message, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (message.length() > 0) {
            message.append(' ');
        }
        message.append(part.trim());
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

    private char resolveDelimiter(String line) {
        int semicolons = count(line, ';');
        int commas = count(line, ',');
        int tabs = count(line, '\t');
        if (tabs >= semicolons && tabs >= commas) {
            return '\t';
        }
        if (commas > semicolons) {
            return ',';
        }
        return ';';
    }

    private int count(String value, char expected) {
        int total = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == expected) {
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
            if (ch == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    current.append('\"');
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

    private static final class ExistingBatchIndex {
        private final Map<Integer, Boolean> byId = new HashMap<>();
        private final Set<String> byInteriorNumber = new HashSet<>();
        private final Set<String> byExternalComposite = new HashSet<>();

        void add(PlantBatch batch, String plantKey, String contrahentName) {
            if (batch == null) {
                return;
            }
            byId.put(batch.getId(), Boolean.TRUE);
            String interiorNumber = safeStatic(batch.getInteriorBatchNo());
            if (!interiorNumber.isBlank()) {
                byInteriorNumber.add(normalizeStatic(interiorNumber));
            }
            if (!safeStatic(batch.getExteriorBatchNo()).isBlank()) {
                String composite = buildComposite(batch.getExteriorBatchNo(), plantKey, contrahentName, batch.getCreationDate(), batch.isInternalSource());
                byExternalComposite.add(composite);
            }
        }

        boolean matches(Integer batchId,
                        String interiorBatchNo,
                        String exteriorBatchNo,
                        String species,
                        String variety,
                        String rootstock,
                        String sourceContrahentName,
                        String creationDate,
                        boolean internalSource) {
            if (batchId != null && batchId > 0 && byId.containsKey(batchId)) {
                return true;
            }
            if (!safeStatic(interiorBatchNo).isBlank() && byInteriorNumber.contains(normalizeStatic(interiorBatchNo))) {
                return true;
            }
            if (safeStatic(exteriorBatchNo).isBlank()) {
                return false;
            }
            return byExternalComposite.contains(buildComposite(exteriorBatchNo,
                    normalizeStatic(species) + "|" + normalizeStatic(variety) + "|" + normalizeStatic(rootstock),
                    sourceContrahentName,
                    parseStaticDate(creationDate),
                    internalSource));
        }

        private String buildComposite(String exteriorBatchNo,
                                      String plantKey,
                                      String contrahentName,
                                      LocalDate creationDate,
                                      boolean internalSource) {
            return normalizeStatic(exteriorBatchNo)
                    + "|PLANT:" + safeStatic(plantKey)
                    + "|SRC:" + normalizeStatic(contrahentName)
                    + "|DATE:" + (creationDate == null ? "" : creationDate)
                    + "|INTERNAL:" + internalSource;
        }

        private static String safeStatic(String value) {
            return value == null ? "" : value.trim();
        }

        private static String normalizeStatic(String value) {
            if (value == null) {
                return "";
            }
            String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .replace('ł', 'l')
                    .replace('Ł', 'L');
            normalized = normalized.replaceAll("[^A-Za-z0-9]", "");
            return normalized.toLowerCase(Locale.ROOT);
        }

        private static LocalDate parseStaticDate(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return LocalDate.parse(value.trim());
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
