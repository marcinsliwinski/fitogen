package com.egen.fitogen.service;

import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.DocumentItem;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.repository.DocumentItemRepository;
import com.egen.fitogen.repository.DocumentRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DocumentCsvExportService {
    private final DocumentRepository documentRepository;
    private final DocumentItemRepository documentItemRepository;
    private final ContrahentService contrahentService;
    private final PlantBatchService plantBatchService;

    public DocumentCsvExportService(
            DocumentRepository documentRepository,
            DocumentItemRepository documentItemRepository,
            ContrahentService contrahentService,
            PlantBatchService plantBatchService) {
        this.documentRepository = documentRepository;
        this.documentItemRepository = documentItemRepository;
        this.contrahentService = contrahentService;
        this.plantBatchService = plantBatchService;
    }

    public Path export(Path outputPath) {
        List<Document> documents = documentRepository.findAll();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("documentNumber;documentType;issueDate;status;contrahentName;contrahentCountryCode;createdBy;comments;lineNo;plantBatchNumber;plantBatchId;qty;passportRequired");
            writer.newLine();

            for (Document document : documents) {
                List<DocumentItem> items = documentItemRepository.findByDocumentId(document.getId());
                Contrahent contrahent = resolveContrahent(document.getContrahentId());

                if (items.isEmpty()) {
                    writer.write(String.join(";",
                            escape(document.getDocumentNumber()),
                            escape(document.getDocumentType()),
                            escape(document.getIssueDate() == null ? "" : document.getIssueDate().toString()),
                            escape(document.getStatus() == null ? "ACTIVE" : document.getStatus().name()),
                            escape(contrahent == null ? "" : contrahent.getName()),
                            escape(contrahent == null ? "" : contrahent.getCountryCode()),
                            escape(document.getCreatedBy()),
                            escape(document.getComments()),
                            "0",
                            "",
                            "",
                            "0",
                            "false"
                    ));
                    writer.newLine();
                    continue;
                }

                for (int i = 0; i < items.size(); i++) {
                    DocumentItem item = items.get(i);
                    PlantBatch batch = plantBatchService.getBatchById(item.getPlantBatchId());

                    writer.write(String.join(";",
                            escape(document.getDocumentNumber()),
                            escape(document.getDocumentType()),
                            escape(document.getIssueDate() == null ? "" : document.getIssueDate().toString()),
                            escape(document.getStatus() == null ? "ACTIVE" : document.getStatus().name()),
                            escape(contrahent == null ? "" : contrahent.getName()),
                            escape(contrahent == null ? "" : contrahent.getCountryCode()),
                            escape(document.getCreatedBy()),
                            escape(document.getComments()),
                            String.valueOf(i + 1),
                            escape(resolveBatchNumber(batch)),
                            batch == null ? "" : String.valueOf(batch.getId()),
                            String.valueOf(item.getQty()),
                            String.valueOf(item.isPassportRequired())
                    ));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować dokumentów do CSV: " + outputPath, e);
        }

        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Kolumny eksportu Documents CSV: documentNumber, documentType, issueDate, status, contrahentName, contrahentCountryCode, createdBy, comments, lineNo, plantBatchNumber, plantBatchId, qty, passportRequired.";
    }

    private Contrahent resolveContrahent(int contrahentId) {
        if (contrahentId <= 0) {
            return null;
        }
        for (Contrahent contrahent : contrahentService.getAllContrahents()) {
            if (contrahent.getId() == contrahentId) {
                return contrahent;
            }
        }
        return null;
    }

    private String resolveBatchNumber(PlantBatch batch) {
        if (batch == null) {
            return "";
        }
        if (batch.getInteriorBatchNo() != null && !batch.getInteriorBatchNo().isBlank()) {
            return batch.getInteriorBatchNo();
        }
        if (batch.getExteriorBatchNo() != null && !batch.getExteriorBatchNo().isBlank()) {
            return batch.getExteriorBatchNo();
        }
        return "ID " + batch.getId();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
