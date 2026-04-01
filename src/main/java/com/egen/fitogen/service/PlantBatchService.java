package com.egen.fitogen.service;

import com.egen.fitogen.domain.NumberingType;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.model.PlantBatchStatus;
import com.egen.fitogen.repository.DocumentItemRepository;
import com.egen.fitogen.repository.PlantBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
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
        log("CREATE", null, "Dodano partię roślin: " + describe(batch));
    }

    public void updateBatch(PlantBatch batch) {
        if (batch == null) {
            throw new IllegalArgumentException("Plant batch cannot be null.");
        }

        if (batch.getStatus() == null) {
            batch.setStatus(PlantBatchStatus.ACTIVE);
        }

        PlantBatch existing = repository.findById(batch.getId());
        logger.info("Updating batch id {}", batch.getId());
        repository.update(batch);
        log(
                "UPDATE",
                batch.getId(),
                "Zaktualizowano partię roślin z " + describe(existing) + " na " + describe(batch)
        );
    }

    public void deleteBatch(int id) {
        logger.info("Cancelling batch id {}", id);

        List<String> documentNumbers = getDocumentNumbersUsingBatch(id);
        if (!documentNumbers.isEmpty()) {
            throw new IllegalStateException(buildBatchUsedMessage(documentNumbers));
        }

        PlantBatch existing = repository.findById(id);
        repository.deleteById(id);
        log("DELETE", id, "Anulowano partię roślin: " + describe(existing));
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

    private String buildBatchUsedMessage(List<String> documentNumbers) {
        return "Partia została użyta w aktywnych dokumentach: " + String.join(", ", documentNumbers);
    }

    private void log(String actionType, Integer entityId, String description) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.log("PLANT_BATCH", entityId, actionType, description);
    }

    private String describe(PlantBatch batch) {
        if (batch == null) {
            return "[brak partii]";
        }

        String batchNo = firstNotBlank(batch.getInteriorBatchNo(), batch.getExteriorBatchNo());
        StringBuilder sb = new StringBuilder();

        if (batchNo != null) {
            sb.append(batchNo);
        } else {
            sb.append("ID ").append(batch.getId());
        }

        sb.append(" [plantId: ").append(batch.getPlantId()).append("]");
        sb.append(" [qty: ").append(batch.getQty()).append("]");

        LocalDate creationDate = batch.getCreationDate();
        if (creationDate != null) {
            sb.append(" [data: ").append(creationDate).append("]");
        }

        String countryCode = firstNotBlank(batch.getManufacturerCountryCode());
        if (countryCode != null) {
            sb.append(" [kraj: ").append(countryCode).append("]");
        }

        String eppoCode = firstNotBlank(batch.getEppoCode());
        if (eppoCode != null) {
            sb.append(" [EPPO: ").append(eppoCode).append("]");
        }

        String zpZone = firstNotBlank(batch.getZpZone());
        if (zpZone != null) {
            sb.append(" [ZP: ").append(zpZone).append("]");
        }

        sb.append(" [status: ").append(batch.getStatus() == null ? PlantBatchStatus.ACTIVE : batch.getStatus()).append("]");
        return sb.toString();
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null) {
                String normalized = value.trim().replaceAll("\\s+", " ");
                if (!normalized.isBlank()) {
                    return normalized;
                }
            }
        }

        return null;
    }
}
