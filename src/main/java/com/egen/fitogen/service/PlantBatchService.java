package com.egen.fitogen.service;

import com.egen.fitogen.domain.NumberingType;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.model.PlantBatchStatus;
import com.egen.fitogen.repository.DocumentItemRepository;
import com.egen.fitogen.repository.PlantBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PlantBatchService {

    private static final Logger logger = LoggerFactory.getLogger(PlantBatchService.class);

    private final PlantBatchRepository repository;
    private final DocumentItemRepository documentItemRepository;
    private final NumberingService numberingService;
    private final AuditLogService auditLogService;

    public PlantBatchService(PlantBatchRepository repository) {
        this(repository, null, null, null);
    }

    public PlantBatchService(
            PlantBatchRepository repository,
            NumberingService numberingService) {
        this(repository, numberingService, null, null);
    }

    public PlantBatchService(
            PlantBatchRepository repository,
            NumberingService numberingService,
            DocumentItemRepository documentItemRepository) {
        this(repository, numberingService, documentItemRepository, null);
    }

    public PlantBatchService(
            PlantBatchRepository repository,
            NumberingService numberingService,
            DocumentItemRepository documentItemRepository,
            AuditLogService auditLogService) {
        this.repository = repository;
        this.numberingService = numberingService;
        this.documentItemRepository = documentItemRepository;
        this.auditLogService = auditLogService;
    }

    public List<PlantBatch> getAllBatches() {
        logger.info("Fetching all plant batches");
        return repository.findAll();
    }

    public PlantBatch getBatchById(int id) {
        logger.info("Fetching plant batch by id {}", id);
        return repository.findById(id);
    }

    public String previewNextInternalBatchNumber(PlantBatch batchContext) {
        if (numberingService == null) {
            return "";
        }
        if (batchContext == null) {
            batchContext = new PlantBatch();
        }
        return numberingService.previewNumber(NumberingType.BATCH, batchContext);
    }

    public void addBatch(PlantBatch batch) {
        logger.info("Adding new batch: {}", batch != null ? batch.getInteriorBatchNo() : null);

        if (batch == null) {
            throw new IllegalArgumentException("Plant batch cannot be null.");
        }

        if (numberingService != null
                && (batch.getInteriorBatchNo() == null || batch.getInteriorBatchNo().isBlank())) {
            String generatedNumber = numberingService.generateNextNumber(NumberingType.BATCH, batch);
            batch.setInteriorBatchNo(generatedNumber);
        }

        if (batch.getStatus() == null) {
            batch.setStatus(PlantBatchStatus.ACTIVE);
        }

        repository.save(batch);
        if (auditLogService != null) {
            auditLogService.log("PLANT_BATCH", null, "CREATE", "Dodano partię: " + buildBatchSummary(batch));
        }
    }

    public void updateBatch(PlantBatch batch) {
        if (batch == null) {
            throw new IllegalArgumentException("Plant batch cannot be null.");
        }

        if (batch.getStatus() == null) {
            batch.setStatus(PlantBatchStatus.ACTIVE);
        }

        logger.info("Updating batch id {}", batch.getId());
        repository.update(batch);
        if (auditLogService != null) {
            auditLogService.log("PLANT_BATCH", batch.getId(), "UPDATE", "Zaktualizowano partię: " + buildBatchSummary(batch));
        }
    }

    public void deleteBatch(int id) {
        logger.info("Cancelling batch id {}", id);

        List<String> documentNumbers = getDocumentNumbersUsingBatch(id);
        if (!documentNumbers.isEmpty()) {
            throw new IllegalStateException(buildBatchUsedMessage(documentNumbers));
        }

        repository.deleteById(id);
        if (auditLogService != null) {
            auditLogService.log("PLANT_BATCH", id, "CANCEL", "Anulowano partię o ID=" + id);
        }
    }

    public boolean isBatchUsedInDocuments(int batchId) {
        return !getDocumentNumbersUsingBatch(batchId).isEmpty();
    }

    public List<String> getDocumentNumbersUsingBatch(int batchId) {
        if (documentItemRepository == null) {
            return List.of();
        }
        return documentItemRepository.findActiveDocumentNumbersByPlantBatchId(batchId);
    }


    private String buildBatchSummary(PlantBatch batch) {
        String number = batch.getInteriorBatchNo() == null || batch.getInteriorBatchNo().isBlank()
                ? "bez numeru"
                : batch.getInteriorBatchNo().trim();
        String externalNumber = batch.getExteriorBatchNo() == null || batch.getExteriorBatchNo().isBlank()
                ? ""
                : " | nr zewnętrzny: " + batch.getExteriorBatchNo().trim();
        String qty = " | ilość: " + batch.getQty();
        String status = " | status: " + (batch.getStatus() == null ? "BRAK" : batch.getStatus().name());
        return number + externalNumber + qty + status;
    }

    private String buildBatchUsedMessage(List<String> documentNumbers) {
        return "Partia została użyta w aktywnych dokumentach: " + String.join(", ", documentNumbers);
    }
}