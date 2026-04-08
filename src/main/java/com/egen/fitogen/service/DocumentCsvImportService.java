package com.egen.fitogen.service;

import com.egen.fitogen.dto.DocumentImportPreviewResult;
import com.egen.fitogen.dto.DocumentImportPreviewRow;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.DocumentStatus;
import com.egen.fitogen.model.PlantBatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DocumentCsvImportService {
    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_MATCHING_EXISTING = "MATCHING_EXISTING";
    private static final String STATUS_DUPLICATE_IN_FILE = "DUPLICATE_IN_FILE";
    private static final String STATUS_INVALID = "INVALID";

    private final DocumentService documentService;
    private final ContrahentService contrahentService;
    private final PlantBatchService plantBatchService;

    public DocumentCsvImportService(
            DocumentService documentService,
            ContrahentService contrahentService,
            PlantBatchService plantBatchService) {
        this.documentService = documentService;
        this.contrahentService = contrahentService;
        this.plantBatchService = plantBatchService;
    }

    public DocumentImportPreviewResult preview(Path csvPath) {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            return preview(reader, csvPath.getFileName().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać pliku importu dokumentów: " + csvPath, e);
        }
    }

    public DocumentImportPreviewResult preview(Reader reader, String sourceName) {
        List<String> lines = new BufferedReader(reader).lines().toList();
        if (lines.isEmpty()) {
            return new DocumentImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0, 0, 0);
        }

        String headerLine = firstNonBlank(lines);
        if (headerLine == null) {
            return new DocumentImportPreviewResult(sourceName, ';', List.of(), List.of(), 0, 0, 0, 0, 0, 0);
        }

        char delimiter = resolveDelimiter(headerLine);
        List<String> headers = parseCsvLine(headerLine, delimiter);

        int numberIndex = indexOf(headers, "documentnumber", "numerdokumentu");
        int typeIndex = indexOf(headers, "documenttype", "typdokumentu");
        int issueDateIndex = indexOf(headers, "issuedate", "datawystawienia", "datadokumentu");
        int statusIndex = indexOf(headers, "status");
        int contrahentNameIndex = indexOf(headers, "contrahentname", "nazwaodbiorcy", "kontrahent");
        int contrahentCodeIndex = indexOf(headers, "contrahentcountrycode", "kodkrajukontrahenta");
        int createdByIndex = indexOf(headers, "createdby", "utworzyl");
        int lineNoIndex = indexOf(headers, "lineno", "pozycja", "lp");
        int batchNumberIndex = indexOf(headers, "plantbatchnumber", "numerpartii");
        int batchIdIndex = indexOf(headers, "plantbatchid", "idpartii");
        int qtyIndex = indexOf(headers, "qty", "ilosc");
        int passportRequiredIndex = indexOf(headers, "passportrequired", "paszport", "wymagapaszportu");

        if (numberIndex < 0) {
            throw new IllegalArgumentException("Plik CSV dla dokumentów musi zawierać kolumnę documentNumber / numerDokumentu.");
        }
        if (typeIndex < 0) {
            throw new IllegalArgumentException("Plik CSV dla dokumentów musi zawierać kolumnę documentType / typDokumentu.");
        }
        if (issueDateIndex < 0) {
            throw new IllegalArgumentException("Plik CSV dla dokumentów musi zawierać kolumnę issueDate / dataWystawienia.");
        }
        if (lineNoIndex < 0 || qtyIndex < 0) {
            throw new IllegalArgumentException("Plik CSV dla dokumentów musi zawierać kolumny lineNo i qty.");
        }

        Set<String> existingDocumentNumbers = new HashSet<>();
        for (Document document : getExistingDocuments()) {
            if (document.getDocumentNumber() != null && !document.getDocumentNumber().isBlank()) {
                existingDocumentNumbers.add(document.getDocumentNumber().trim().toLowerCase());
            }
        }

        Set<String> seenLines = new HashSet<>();
        Set<String> newDocumentNumbers = new LinkedHashSet<>();
        List<DocumentImportPreviewRow> rows = new ArrayList<>();
        int newCount = 0;
        int existingCount = 0;
        int duplicateCount = 0;
        int invalidCount = 0;
        boolean skipped = false;
        int rowNo = 0;

        for (String line : lines) {
            rowNo++;
            if (!skipped && line.equals(headerLine)) {
                skipped = true;
                continue;
            }
            if (line == null || line.isBlank()) {
                continue;
            }

            List<String> cells = parseCsvLine(line, delimiter);
            String documentNumber = valueAt(cells, numberIndex);
            String documentType = valueAt(cells, typeIndex);
            String issueDate = valueAt(cells, issueDateIndex);
            String status = normalizeStatus(valueAt(cells, statusIndex));
            String contrahentName = valueAt(cells, contrahentNameIndex);
            String contrahentCode = valueAt(cells, contrahentCodeIndex);
            String createdBy = valueAt(cells, createdByIndex);
            int lineNo = parsePositiveInt(valueAt(cells, lineNoIndex));
            String batchNumber = valueAt(cells, batchNumberIndex);
            String batchId = valueAt(cells, batchIdIndex);
            int qty = parsePositiveInt(valueAt(cells, qtyIndex));
            boolean passportRequired = parseBool(valueAt(cells, passportRequiredIndex));

            String statusText;
            StringBuilder message = new StringBuilder();

            if (documentNumber.isBlank() || documentType.isBlank() || issueDate.isBlank() || lineNo <= 0 || qty <= 0) {
                statusText = STATUS_INVALID;
                invalidCount++;
                appendMessage(message, documentNumber.isBlank(), "Brak numeru dokumentu.");
                appendMessage(message, documentType.isBlank(), "Brak typu dokumentu.");
                appendMessage(message, issueDate.isBlank(), "Brak daty wystawienia.");
                appendMessage(message, lineNo <= 0, "Nieprawidłowy numer pozycji.");
                appendMessage(message, qty <= 0, "Ilość musi być większa od zera.");
            } else if (!isValidDate(issueDate) || !isValidStatus(status) || (batchId.isBlank() && batchNumber.isBlank())) {
                statusText = STATUS_INVALID;
                invalidCount++;
                appendMessage(message, !isValidDate(issueDate), "Nieprawidłowa data wystawienia.");
                appendMessage(message, !isValidStatus(status), "Nieprawidłowy status dokumentu.");
                appendMessage(message, batchId.isBlank() && batchNumber.isBlank(), "Brak powiązania z partią roślin.");
            } else {
                String lineKey = documentNumber.trim().toLowerCase() + "|" + lineNo;
                if (!seenLines.add(lineKey)) {
                    statusText = STATUS_DUPLICATE_IN_FILE;
                    duplicateCount++;
                    appendMessage(message, true, "Duplikat tej samej pozycji dokumentu w pliku.");
                } else if (existingDocumentNumbers.contains(documentNumber.trim().toLowerCase())) {
                    statusText = STATUS_MATCHING_EXISTING;
                    existingCount++;
                    appendMessage(message, true, "Numer dokumentu już istnieje w bazie.");
                } else {
                    statusText = STATUS_NEW;
                    newCount++;
                    newDocumentNumbers.add(documentNumber);
                }

                appendMessage(message, !contrahentName.isBlank() && !contrahentExists(contrahentName, contrahentCode),
                        "Kontrahent nie został rozpoznany w aktualnej bazie.");
                appendMessage(message, !batchResolvable(batchId, batchNumber),
                        "Partia roślin nie została rozpoznana w aktualnej bazie.");
            }

            rows.add(new DocumentImportPreviewRow(
                    rowNo,
                    documentNumber,
                    documentType,
                    issueDate,
                    status,
                    contrahentName,
                    contrahentCode,
                    createdBy,
                    lineNo,
                    batchNumber,
                    batchId,
                    qty,
                    passportRequired,
                    statusText,
                    message.toString().trim()
            ));
        }

        return new DocumentImportPreviewResult(
                sourceName,
                delimiter,
                headers,
                rows,
                rows.size(),
                newDocumentNumbers.size(),
                newCount,
                existingCount,
                duplicateCount,
                invalidCount
        );
    }

    public String getSupportedColumnsSummary() {
        return "Obsługiwane kolumny importu: numer dokumentu (documentNumber), typ dokumentu (documentType), data wystawienia (issueDate), status (status), nazwa kontrahenta (contrahentName), kod kraju kontrahenta (contrahentCountryCode), utworzył (createdBy), uwagi (comments), numer pozycji (lineNo), numer partii (plantBatchNumber), identyfikator partii (plantBatchId), ilość (qty), wymagany paszport (passportRequired).";
    }

    private List<Document> getExistingDocuments() {
        List<Document> documents = new ArrayList<>();
        for (Document document : new com.egen.fitogen.database.SqliteDocumentRepository().findAll()) {
            documents.add(document);
        }
        return documents;
    }

    private boolean contrahentExists(String name, String countryCode) {
        for (Contrahent contrahent : contrahentService.getAllContrahents()) {
            boolean sameName = equalsNormalized(contrahent.getName(), name);
            boolean sameCode = countryCode == null || countryCode.isBlank() || equalsNormalized(contrahent.getCountryCode(), countryCode);
            if (sameName && sameCode) {
                return true;
            }
        }
        return false;
    }

    private boolean batchResolvable(String batchIdRaw, String batchNumber) {
        if (batchIdRaw != null && !batchIdRaw.isBlank()) {
            int batchId = parsePositiveInt(batchIdRaw);
            if (batchId > 0 && plantBatchService.getBatchById(batchId) != null) {
                return true;
            }
        }
        if (batchNumber == null || batchNumber.isBlank()) {
            return false;
        }
        for (PlantBatch batch : plantBatchService.getAllBatches()) {
            if (equalsNormalized(batch.getInteriorBatchNo(), batchNumber) || equalsNormalized(batch.getExteriorBatchNo(), batchNumber)) {
                return true;
            }
        }
        return false;
    }

    private boolean equalsNormalized(String left, String right) {
        return norm(left).equals(norm(right));
    }

    private boolean isValidDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidStatus(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            DocumentStatus.valueOf(value.trim().toUpperCase());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "ACTIVE";
        }
        return raw.trim().toUpperCase();
    }

    private void appendMessage(StringBuilder builder, boolean condition, String text) {
        if (!condition) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(text);
    }

    private int indexOf(List<String> headers, String... aliases) {
        for (int i = 0; i < headers.size(); i++) {
            String normalized = norm(headers.get(i));
            for (String alias : aliases) {
                if (norm(alias).equals(normalized)) {
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

    private int parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean parseBool(String raw) {
        String normalized = norm(raw);
        return normalized.equals("true") || normalized.equals("tak") || normalized.equals("yes") || normalized.equals("1");
    }

    private String norm(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private char resolveDelimiter(String line) {
        int semicolonCount = count(line, ';');
        int commaCount = count(line, ',');
        int tabCount = count(line, '\t');
        if (tabCount >= semicolonCount && tabCount >= commaCount) {
            return '\t';
        }
        if (commaCount > semicolonCount) {
            return ',';
        }
        return ';';
    }

    private int count(String value, char searched) {
        int total = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == searched) {
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
