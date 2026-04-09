package com.egen.fitogen.service;

import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.dto.CsvImportExecutionResult;
import com.egen.fitogen.dto.DocumentDTO;
import com.egen.fitogen.dto.DocumentImportPreviewResult;
import com.egen.fitogen.dto.DocumentImportPreviewRow;
import com.egen.fitogen.dto.DocumentItemDTO;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.DocumentStatus;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.model.PlantBatchStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DocumentCsvImportService {
    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_MATCHING_EXISTING = "MATCHING_EXISTING";
    private static final String STATUS_DUPLICATE_IN_FILE = "DUPLICATE_IN_FILE";
    private static final String STATUS_INVALID = "INVALID";

    private final DocumentService documentService;
    private final ContrahentService contrahentService;
    private final PlantBatchService plantBatchService;
    private final AuditLogService auditLogService;

    public DocumentCsvImportService(
            DocumentService documentService,
            ContrahentService contrahentService,
            PlantBatchService plantBatchService,
            AuditLogService auditLogService) {
        this.documentService = documentService;
        this.contrahentService = contrahentService;
        this.plantBatchService = plantBatchService;
        this.auditLogService = auditLogService;
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
        int commentsIndex = indexOf(headers, "comments", "uwagi");
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
            String normalizedNumber = normalizeDocumentNumber(document.getDocumentNumber());
            if (!normalizedNumber.isBlank()) {
                existingDocumentNumbers.add(normalizedNumber);
            }
        }

        Set<String> seenLines = new HashSet<>();
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
            String comments = valueAt(cells, commentsIndex);
            int lineNo = parsePositiveInt(valueAt(cells, lineNoIndex));
            String batchNumber = valueAt(cells, batchNumberIndex);
            String batchId = valueAt(cells, batchIdIndex);
            int qty = parsePositiveInt(valueAt(cells, qtyIndex));
            boolean passportRequired = parseBool(valueAt(cells, passportRequiredIndex));

            String rowStatus;
            StringBuilder message = new StringBuilder();
            String normalizedDocumentNumber = normalizeDocumentNumber(documentNumber);

            boolean missingRequiredValues = false;
            missingRequiredValues |= appendMessage(message, documentNumber.isBlank(), "Brak numeru dokumentu.");
            missingRequiredValues |= appendMessage(message, documentType.isBlank(), "Brak typu dokumentu.");
            missingRequiredValues |= appendMessage(message, issueDate.isBlank(), "Brak daty wystawienia.");
            missingRequiredValues |= appendMessage(message, createdBy.isBlank(), "Brak pola „Utworzył”.");
            missingRequiredValues |= appendMessage(message, contrahentName.isBlank() && contrahentCode.isBlank(), "Brak kontrahenta dokumentu.");
            missingRequiredValues |= appendMessage(message, batchId.isBlank() && batchNumber.isBlank(), "Brak powiązania z partią roślin.");
            missingRequiredValues |= appendMessage(message, lineNo <= 0, "Nieprawidłowy numer pozycji.");
            missingRequiredValues |= appendMessage(message, qty <= 0, "Ilość musi być większa od zera.");

            if (missingRequiredValues) {
                rowStatus = STATUS_INVALID;
                invalidCount++;
            } else if (!isValidDate(issueDate) || !isValidStatus(status)) {
                rowStatus = STATUS_INVALID;
                invalidCount++;
                appendMessage(message, !isValidDate(issueDate), "Nieprawidłowa data wystawienia.");
                appendMessage(message, !isValidStatus(status), "Nieprawidłowy status dokumentu.");
            } else if (existingDocumentNumbers.contains(normalizedDocumentNumber)) {
                rowStatus = STATUS_MATCHING_EXISTING;
                existingCount++;
                appendMessage(message, true, "Numer dokumentu już istnieje w bazie.");
                appendMessage(message, resolveContrahent(contrahentName, contrahentCode) == null,
                        "Kontrahent nie został rozpoznany w aktualnej bazie.");
                PlantBatch resolvedBatch = resolveBatch(batchId, batchNumber);
                appendMessage(message, resolvedBatch == null,
                        "Partia roślin nie została rozpoznana w aktualnej bazie.");
                appendMessage(message, resolvedBatch != null && resolvedBatch.getStatus() == PlantBatchStatus.CANCELLED,
                        "Partia roślin jest anulowana.");
            } else {
                String lineKey = normalizedDocumentNumber + "|" + lineNo;
                if (!seenLines.add(lineKey)) {
                    rowStatus = STATUS_DUPLICATE_IN_FILE;
                    duplicateCount++;
                    appendMessage(message, true, "Duplikat tej samej pozycji dokumentu w pliku.");
                } else {
                    Contrahent resolvedContrahent = resolveContrahent(contrahentName, contrahentCode);
                    PlantBatch resolvedBatch = resolveBatch(batchId, batchNumber);
                    int availableQty = resolvedBatch == null ? 0 : getAvailableQtySafe(resolvedBatch.getId());

                    if (resolvedContrahent == null
                            || resolvedBatch == null
                            || resolvedBatch.getStatus() == PlantBatchStatus.CANCELLED
                            || qty > availableQty) {
                        rowStatus = STATUS_INVALID;
                        invalidCount++;
                        appendMessage(message, resolvedContrahent == null,
                                "Kontrahent nie został rozpoznany w aktualnej bazie.");
                        appendMessage(message, resolvedBatch == null,
                                "Partia roślin nie została rozpoznana w aktualnej bazie.");
                        appendMessage(message, resolvedBatch != null && resolvedBatch.getStatus() == PlantBatchStatus.CANCELLED,
                                "Partia roślin jest anulowana.");
                        appendMessage(message, resolvedBatch != null && qty > availableQty,
                                "Ilość przekracza aktualnie dostępny stan partii: " + availableQty + ".");
                    } else {
                        rowStatus = STATUS_NEW;
                        newCount++;
                    }
                }
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
                    comments,
                    lineNo,
                    batchNumber,
                    batchId,
                    qty,
                    passportRequired,
                    rowStatus,
                    message.toString().trim()
            ));
        }

        return new DocumentImportPreviewResult(
                sourceName,
                delimiter,
                headers,
                rows,
                rows.size(),
                countReadyDocuments(rows),
                newCount,
                existingCount,
                duplicateCount,
                invalidCount
        );
    }

    public CsvImportExecutionResult applyPreview(DocumentImportPreviewResult previewResult) {
        if (previewResult == null) {
            throw new IllegalArgumentException("Brak podglądu importu dokumentów do wykonania.");
        }

        Set<String> existingDocumentNumbers = new HashSet<>();
        for (Document document : getExistingDocuments()) {
            String normalizedNumber = normalizeDocumentNumber(document.getDocumentNumber());
            if (!normalizedNumber.isBlank()) {
                existingDocumentNumbers.add(normalizedNumber);
            }
        }

        Map<String, List<DocumentImportPreviewRow>> documentsByKey = groupRowsByDocument(previewResult.getRows());
        List<String> problems = new ArrayList<>();
        int addedCount = 0;
        int skippedCount = 0;
        int rejectedCount = 0;

        for (List<DocumentImportPreviewRow> documentRows : documentsByKey.values()) {
            String normalizedDocumentNumber = normalizeDocumentNumber(documentRows.getFirst().getDocumentNumber());
            String displayDocumentNumber = displayDocumentNumber(documentRows);

            if (!normalizedDocumentNumber.isBlank() && existingDocumentNumbers.contains(normalizedDocumentNumber)) {
                skippedCount++;
                continue;
            }

            if (containsRowStatus(documentRows, STATUS_MATCHING_EXISTING)) {
                skippedCount++;
                continue;
            }

            if (containsRowStatus(documentRows, STATUS_DUPLICATE_IN_FILE) || containsRowStatus(documentRows, STATUS_INVALID)) {
                rejectedCount++;
                appendDocumentProblem(problems, displayDocumentNumber, buildRejectedDocumentReason(documentRows));
                continue;
            }

            if (!allRowsHaveStatus(documentRows, STATUS_NEW)) {
                rejectedCount++;
                appendDocumentProblem(problems, displayDocumentNumber,
                        "Dokument zawiera pozycje o nieobsługiwanym statusie podglądu importu.");
                continue;
            }

            try {
                DocumentDTO dto = mapRowsToDocument(documentRows);
                documentService.validateImportedDocument(dto);
                int documentId = saveDocumentAtomically(dto);
                existingDocumentNumbers.add(normalizeDocumentNumber(dto.getDocumentNumber()));
                logImportedDocument(documentId, dto, previewResult.getSourceName());
                addedCount++;
            } catch (Exception e) {
                rejectedCount++;
                appendDocumentProblem(problems, displayDocumentNumber,
                        fallbackMessage(e.getMessage(), "Nie udało się zapisać dokumentu do bazy."));
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
        return "Obsługiwane kolumny importu: numer dokumentu (documentNumber), typ dokumentu (documentType), data wystawienia (issueDate), status (status), nazwa kontrahenta (contrahentName), kod kraju kontrahenta (contrahentCountryCode), utworzył (createdBy), uwagi (comments), numer pozycji (lineNo), numer partii (plantBatchNumber), identyfikator partii (plantBatchId), ilość (qty), wymagany paszport (passportRequired).";
    }

    private int countReadyDocuments(List<DocumentImportPreviewRow> rows) {
        int count = 0;
        for (Map.Entry<String, List<DocumentImportPreviewRow>> entry : groupRowsByDocument(rows).entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("__row__")) {
                continue;
            }
            if (allRowsHaveStatus(entry.getValue(), STATUS_NEW)) {
                count++;
            }
        }
        return count;
    }

    private Map<String, List<DocumentImportPreviewRow>> groupRowsByDocument(List<DocumentImportPreviewRow> rows) {
        Map<String, List<DocumentImportPreviewRow>> grouped = new LinkedHashMap<>();
        if (rows == null) {
            return grouped;
        }

        for (DocumentImportPreviewRow row : rows) {
            String key = normalizeDocumentNumber(row.getDocumentNumber());
            if (key.isBlank()) {
                key = "__row__" + row.getRowNumber();
            }
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    private DocumentDTO mapRowsToDocument(List<DocumentImportPreviewRow> rows) {
        List<DocumentImportPreviewRow> sortedRows = new ArrayList<>(rows);
        sortedRows.sort(Comparator
                .comparingInt(DocumentImportPreviewRow::getLineNo)
                .thenComparingInt(DocumentImportPreviewRow::getRowNumber));

        DocumentImportPreviewRow firstRow = sortedRows.getFirst();
        Contrahent contrahent = resolveContrahent(firstRow.getContrahentName(), firstRow.getContrahentCountryCode());
        if (contrahent == null) {
            throw new IllegalArgumentException("Nie udało się jednoznacznie rozpoznać kontrahenta dokumentu.");
        }

        DocumentDTO dto = new DocumentDTO();
        dto.setDocumentNumber(valueOrEmpty(firstRow.getDocumentNumber()));
        dto.setDocumentType(valueOrEmpty(firstRow.getDocumentType()));
        dto.setIssueDate(LocalDate.parse(valueOrEmpty(firstRow.getIssueDate())));
        dto.setContrahentId(contrahent.getId());
        dto.setCreatedBy(valueOrEmpty(firstRow.getCreatedBy()));
        dto.setComments(valueOrEmpty(firstRow.getComments()));
        dto.setStatus(resolveDocumentStatus(firstRow.getStatus()));

        List<DocumentItemDTO> items = new ArrayList<>();
        for (DocumentImportPreviewRow row : sortedRows) {
            assertConsistentDocumentFields(firstRow, row, contrahent);

            PlantBatch batch = resolveBatch(row.getPlantBatchId(), row.getPlantBatchNumber());
            if (batch == null) {
                throw new IllegalArgumentException("Pozycja #" + row.getRowNumber() + ": nie udało się rozpoznać partii roślin.");
            }
            if (batch.getStatus() == PlantBatchStatus.CANCELLED) {
                throw new IllegalArgumentException("Pozycja #" + row.getRowNumber() + ": partia roślin jest anulowana.");
            }

            DocumentItemDTO itemDTO = new DocumentItemDTO();
            itemDTO.setPlantBatchId(batch.getId());
            itemDTO.setQty(row.getQty());
            itemDTO.setPassportRequired(row.isPassportRequired());
            items.add(itemDTO);
        }

        dto.setItems(items);
        return dto;
    }

    private void assertConsistentDocumentFields(DocumentImportPreviewRow firstRow,
                                                DocumentImportPreviewRow nextRow,
                                                Contrahent expectedContrahent) {
        if (!normalizeDocumentNumber(firstRow.getDocumentNumber()).equals(normalizeDocumentNumber(nextRow.getDocumentNumber()))) {
            throw new IllegalArgumentException("Pozycje nie należą do tego samego numeru dokumentu.");
        }
        if (!valueOrEmpty(firstRow.getDocumentType()).equalsIgnoreCase(valueOrEmpty(nextRow.getDocumentType()))) {
            throw new IllegalArgumentException("Ten sam numer dokumentu ma różne typy dokumentu w pliku importu.");
        }
        if (!valueOrEmpty(firstRow.getIssueDate()).equals(valueOrEmpty(nextRow.getIssueDate()))) {
            throw new IllegalArgumentException("Ten sam numer dokumentu ma różne daty wystawienia w pliku importu.");
        }
        if (!normalizeStatus(firstRow.getStatus()).equals(normalizeStatus(nextRow.getStatus()))) {
            throw new IllegalArgumentException("Ten sam numer dokumentu ma różne statusy w pliku importu.");
        }
        if (!valueOrEmpty(firstRow.getCreatedBy()).equalsIgnoreCase(valueOrEmpty(nextRow.getCreatedBy()))) {
            throw new IllegalArgumentException("Ten sam numer dokumentu ma różne wartości pola „Utworzył” w pliku importu.");
        }
        if (!valueOrEmpty(firstRow.getComments()).equals(valueOrEmpty(nextRow.getComments()))) {
            throw new IllegalArgumentException("Ten sam numer dokumentu ma różne uwagi w pliku importu.");
        }

        Contrahent nextContrahent = resolveContrahent(nextRow.getContrahentName(), nextRow.getContrahentCountryCode());
        if (nextContrahent == null || nextContrahent.getId() != expectedContrahent.getId()) {
            throw new IllegalArgumentException("Ten sam numer dokumentu wskazuje różne dane kontrahenta w pliku importu.");
        }
    }

    private int saveDocumentAtomically(DocumentDTO dto) {
        String documentSql = """
                INSERT INTO documents
                (document_number, document_type, issue_date, contrahent_id, created_by, comments, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        String itemSql = """
                INSERT INTO document_items
                (document_id, plant_batch_id, qty, passport_required)
                VALUES (?, ?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement documentStmt = null;
        PreparedStatement itemStmt = null;
        ResultSet generatedKeys = null;

        try {
            conn = DatabaseConfig.getConnection();
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                documentStmt = conn.prepareStatement(documentSql, Statement.RETURN_GENERATED_KEYS);
                documentStmt.setString(1, dto.getDocumentNumber());
                documentStmt.setString(2, dto.getDocumentType());
                documentStmt.setString(3, dto.getIssueDate() == null ? null : dto.getIssueDate().toString());
                documentStmt.setInt(4, dto.getContrahentId());
                documentStmt.setString(5, dto.getCreatedBy());
                documentStmt.setString(6, dto.getComments());
                documentStmt.setString(7, dto.getStatus() == null ? DocumentStatus.ACTIVE.name() : dto.getStatus().name());
                documentStmt.executeUpdate();

                generatedKeys = documentStmt.getGeneratedKeys();
                if (!generatedKeys.next()) {
                    throw new IllegalStateException("Nie udało się pobrać identyfikatora zapisanego dokumentu.");
                }
                int documentId = generatedKeys.getInt(1);

                itemStmt = conn.prepareStatement(itemSql);
                for (DocumentItemDTO item : dto.getItems()) {
                    itemStmt.setInt(1, documentId);
                    itemStmt.setInt(2, item.getPlantBatchId());
                    itemStmt.setInt(3, item.getQty());
                    itemStmt.setBoolean(4, item.isPassportRequired());
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();

                conn.commit();
                return documentId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Nie udało się zapisać dokumentu „" + fallback(dto.getDocumentNumber(), "[bez numeru]") + "”.",
                    e
            );
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(itemStmt);
            closeQuietly(documentStmt);
            closeQuietly(conn);
        }
    }

    private void logImportedDocument(int documentId, DocumentDTO dto, String sourceName) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.log(
                "DOCUMENT",
                documentId,
                "IMPORT",
                "Zaimportowano dokument nr " + fallback(dto.getDocumentNumber(), "[bez numeru]")
                        + " z pliku " + fallback(sourceName, "[nieznane źródło]")
                        + ", pozycji: " + (dto.getItems() == null ? 0 : dto.getItems().size()) + "."
        );
    }

    private void appendDocumentProblem(List<String> problems, String documentNumber, String message) {
        if (problems == null || message == null || message.isBlank() || problems.size() >= 12) {
            return;
        }
        problems.add(documentNumber + " — " + message.trim());
    }

    private String buildRejectedDocumentReason(List<DocumentImportPreviewRow> rows) {
        List<String> issues = new ArrayList<>();
        for (DocumentImportPreviewRow row : rows) {
            if (row.getMessage() != null && !row.getMessage().isBlank()) {
                issues.add("#" + row.getRowNumber() + " " + row.getMessage().trim());
            }
            if (issues.size() >= 3) {
                break;
            }
        }
        if (issues.isEmpty()) {
            return "Dokument zawiera pozycje, które nie przeszły walidacji albo duplikują się w pliku.";
        }
        return String.join(" ", issues);
    }

    private boolean containsRowStatus(List<DocumentImportPreviewRow> rows, String expectedStatus) {
        for (DocumentImportPreviewRow row : rows) {
            if (expectedStatus.equals(row.getRowStatus())) {
                return true;
            }
        }
        return false;
    }

    private boolean allRowsHaveStatus(List<DocumentImportPreviewRow> rows, String expectedStatus) {
        for (DocumentImportPreviewRow row : rows) {
            if (!expectedStatus.equals(row.getRowStatus())) {
                return false;
            }
        }
        return true;
    }

    private String displayDocumentNumber(List<DocumentImportPreviewRow> rows) {
        String number = rows == null || rows.isEmpty() ? "" : valueOrEmpty(rows.getFirst().getDocumentNumber());
        return number.isBlank() ? "Dokument [bez numeru]" : "Dokument " + number;
    }

    private List<Document> getExistingDocuments() {
        List<Document> documents = new ArrayList<>();
        for (Document document : new com.egen.fitogen.database.SqliteDocumentRepository().findAll()) {
            documents.add(document);
        }
        return documents;
    }

    private Contrahent resolveContrahent(String name, String countryCode) {
        String normalizedName = norm(name);
        String normalizedCode = norm(countryCode);

        List<Contrahent> matches = new ArrayList<>();
        for (Contrahent contrahent : contrahentService.getAllContrahents()) {
            boolean nameMatches = normalizedName.isBlank() || norm(contrahent.getName()).equals(normalizedName);
            boolean codeMatches = normalizedCode.isBlank() || norm(contrahent.getCountryCode()).equals(normalizedCode);
            if (nameMatches && codeMatches) {
                matches.add(contrahent);
            }
        }

        if (matches.size() == 1) {
            return matches.getFirst();
        }
        return null;
    }

    private PlantBatch resolveBatch(String batchIdRaw, String batchNumber) {
        PlantBatch batchById = null;
        if (batchIdRaw != null && !batchIdRaw.isBlank()) {
            int batchId = parsePositiveInt(batchIdRaw);
            if (batchId > 0) {
                batchById = plantBatchService.getBatchById(batchId);
                if (batchById == null) {
                    return null;
                }
            }
        }

        PlantBatch batchByNumber = null;
        String normalizedBatchNumber = norm(batchNumber);
        if (!normalizedBatchNumber.isBlank()) {
            for (PlantBatch batch : plantBatchService.getAllBatches()) {
                boolean matches = norm(batch.getInteriorBatchNo()).equals(normalizedBatchNumber)
                        || norm(batch.getExteriorBatchNo()).equals(normalizedBatchNumber);
                if (!matches) {
                    continue;
                }
                if (batchByNumber != null && batchByNumber.getId() != batch.getId()) {
                    return null;
                }
                batchByNumber = batch;
            }
            if (batchByNumber == null) {
                return null;
            }
        }

        if (batchById != null && batchByNumber != null && batchById.getId() != batchByNumber.getId()) {
            return null;
        }
        if (batchById != null) {
            return batchById;
        }
        return batchByNumber;
    }

    private int getAvailableQtySafe(int batchId) {
        try {
            return documentService.getAvailableQtyForBatch(batchId);
        } catch (Exception e) {
            return 0;
        }
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

    private DocumentStatus resolveDocumentStatus(String raw) {
        return DocumentStatus.valueOf(normalizeStatus(raw));
    }

    private String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return DocumentStatus.ACTIVE.name();
        }
        return raw.trim().toUpperCase();
    }

    private String normalizeDocumentNumber(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }

    private boolean appendMessage(StringBuilder builder, boolean condition, String text) {
        if (!condition) {
            return false;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(text);
        return true;
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

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
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

    private String fallback(String value, String fallbackValue) {
        return value == null || value.isBlank() ? fallbackValue : value;
    }

    private String fallbackMessage(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
