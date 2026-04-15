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
        logger.info("Pobieranie wszystkich partii roślin");
        return repository.findAll();
    }

    public PlantBatch getBatchById(int id) {
        logger.info("Pobieranie partii roślin o identyfikatorze {}", id);
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
        logger.info("Dodawanie nowej partii: {}", batch != null ? batch.getInteriorBatchNo() : null);

        if (batch == null) {
            throw new IllegalArgumentException("Partia roślin nie może być pusta.");
        }

        if (batch.isInternalSource() && numberingService != null) {
            batch.setInteriorBatchNo(generateUniqueInternalBatchNumber(batch));
        }

        validateInteriorBatchNumberUniqueness(batch);

        if (batch.getStatus() == null) {
            batch.setStatus(PlantBatchStatus.ACTIVE);
        }

        repository.save(batch);
        logChange("PlantBatch", batch.getId(), "CREATE", "Dodano partię roślin: " + describeBatch(batch));
    }

    public void updateBatch(PlantBatch batch) {
        PlantBatch beforeUpdate = batch == null ? null : repository.findById(batch.getId());

        if (batch == null) {
            throw new IllegalArgumentException("Partia roślin nie może być pusta.");
        }

        if (batch.getStatus() == null) {
            batch.setStatus(PlantBatchStatus.ACTIVE);
        }

        validateInteriorBatchNumberUniqueness(batch);

        logger.info("Aktualizacja partii o identyfikatorze {}", batch.getId());
        repository.update(batch);
        logChange("PlantBatch", batch.getId(), "UPDATE",
                "Zaktualizowano partię roślin: " + describeBatch(batch)
                        + buildBeforeAfterSuffix(describeBatch(beforeUpdate), describeBatch(batch)));
    }

    public void deleteBatch(int id) {
        logger.info("Anulowanie partii o identyfikatorze {}", id);

        List<String> documentNumbers = getDocumentNumbersUsingBatch(id);
        if (!documentNumbers.isEmpty()) {
            throw new IllegalStateException(buildBatchUsedMessage(documentNumbers));
        }

        PlantBatch batch = repository.findById(id);
        repository.deleteById(id);
        logChange("PlantBatch", id, "DELETE", "Anulowano partię roślin: " + describeBatch(batch));
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

    private String generateUniqueInternalBatchNumber(PlantBatch batch) {
        if (numberingService == null) {
            return safe(batch == null ? null : batch.getInteriorBatchNo());
        }

        for (int attempt = 0; attempt < 100; attempt++) {
            String candidate = safe(numberingService.generateNextNumber(NumberingType.BATCH, batch));
            if (!candidate.isBlank() && !interiorBatchNumberExists(candidate, null)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Nie udało się nadać unikalnego numeru partii wewnętrznej.");
    }

    private void validateInteriorBatchNumberUniqueness(PlantBatch batch) {
        if (batch == null) {
            return;
        }

        String number = safe(batch.getInteriorBatchNo());
        if (number.isBlank()) {
            return;
        }

        Integer excludedId = batch.getId() > 0 ? batch.getId() : null;
        if (interiorBatchNumberExists(number, excludedId)) {
            throw new IllegalStateException("Numer partii wewnętrznej już istnieje: " + number);
        }
    }

    private boolean interiorBatchNumberExists(String candidate, Integer excludedId) {
        String normalizedCandidate = safe(candidate);
        if (normalizedCandidate.isBlank()) {
            return false;
        }

        for (PlantBatch existing : repository.findAll()) {
            if (existing == null) {
                continue;
            }
            if (excludedId != null && existing.getId() == excludedId) {
                continue;
            }
            if (normalizedCandidate.equalsIgnoreCase(safe(existing.getInteriorBatchNo()))) {
                return true;
            }
        }
        return false;
    }

    private void logChange(String entityType, Integer entityId, String actionType, String description) {
        if (auditLogService != null) {
            auditLogService.log(entityType, entityId, actionType, description);
        }
    }

    private String describeBatch(PlantBatch batch) {
        if (batch == null) {
            return "[brak danych]";
        }

        String number = safe(batch.getInteriorBatchNo());
        if (number.isBlank()) {
            number = safe(batch.getExteriorBatchNo());
        }
        if (number.isBlank()) {
            number = "ID=" + batch.getId();
        }

        StringBuilder sb = new StringBuilder(number);
        if (batch.getCreationDate() != null) {
            sb.append(" [data: ").append(batch.getCreationDate()).append("]");
        }
        if (batch.getStatus() != null) {
            sb.append(" [status: ").append(batch.getStatus().name()).append("]");
        }
        return sb.toString();
    }

    private String buildBeforeAfterSuffix(String before, String after) {
        if (before == null || before.isBlank() || before.equals(after)) {
            return "";
        }
        return " (wcześniej: " + before + ")";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}