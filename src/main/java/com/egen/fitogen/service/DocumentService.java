package com.egen.fitogen.service;

import com.egen.fitogen.domain.NumberingType;
import com.egen.fitogen.dto.DocumentDTO;
import com.egen.fitogen.dto.DocumentItemDTO;
import com.egen.fitogen.mapper.DocumentMapper;
import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.DocumentItem;
import com.egen.fitogen.model.DocumentStatus;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.model.PlantBatchStatus;
import com.egen.fitogen.repository.DocumentItemRepository;
import com.egen.fitogen.repository.DocumentRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentItemRepository itemRepository;
    private final NumberingService numberingService;
    private final PlantBatchService plantBatchService;
    private final AuditLogService auditLogService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentItemRepository itemRepository,
            NumberingService numberingService,
            PlantBatchService plantBatchService,
            AuditLogService auditLogService) {

        this.documentRepository = documentRepository;
        this.itemRepository = itemRepository;
        this.numberingService = numberingService;
        this.plantBatchService = plantBatchService;
        this.auditLogService = auditLogService;
    }

    public void createDocument(DocumentDTO dto) {
        validateDocument(dto, null);

        PlantBatch batchContext = resolveBatchContext(dto.getItems());

        String number = numberingService.generateNextNumber(NumberingType.DOCUMENT, batchContext);
        dto.setDocumentNumber(number);

        if (dto.getStatus() == null) {
            dto.setStatus(DocumentStatus.ACTIVE);
        }

        Document document = DocumentMapper.toEntity(dto);
        documentRepository.save(document);

        saveItems(document.getId(), dto.getItems());
        logAudit("DOCUMENT", document.getId(), "CREATE",
                "Utworzono dokument " + buildDocumentSummary(document, dto.getItems()));
    }

    public DocumentDTO getDocumentDetails(int documentId) {

        Document document = documentRepository.findById(documentId);

        if (document == null) {
            return null;
        }

        List<DocumentItem> items = itemRepository.findByDocumentId(documentId);
        return mapToDto(document, items);
    }

    public void updateDocument(DocumentDTO dto) {

        if (dto.getId() <= 0) {
            throw new IllegalArgumentException("Nieprawidłowe ID dokumentu.");
        }

        Document existing = documentRepository.findById(dto.getId());
        if (existing == null) {
            throw new IllegalStateException("Dokument nie istnieje.");
        }

        if (existing.getStatus() == DocumentStatus.CANCELLED) {
            throw new IllegalStateException("Anulowany dokument nie może być edytowany.");
        }

        validateDocument(dto, dto.getId());

        dto.setDocumentNumber(existing.getDocumentNumber());
        dto.setStatus(existing.getStatus());

        Document updated = DocumentMapper.toEntity(dto);
        documentRepository.update(updated);

        itemRepository.deleteByDocumentId(dto.getId());
        saveItems(dto.getId(), dto.getItems());

        logAudit("DOCUMENT", updated.getId(), "UPDATE",
                "Zaktualizowano dokument " + buildDocumentSummary(updated, dto.getItems())
                        + ". Wcześniej: " + buildDocumentSummary(existing, itemRepository.findByDocumentId(existing.getId())));
    }

    public void deleteDocument(int documentId) {
        Document existing = documentRepository.findById(documentId);

        if (existing == null) {
            throw new IllegalStateException("Dokument nie istnieje.");
        }

        existing.setStatus(DocumentStatus.CANCELLED);
        documentRepository.update(existing);
        logAudit("DOCUMENT", existing.getId(), "DELETE",
                "Anulowano dokument " + buildDocumentSummary(existing, itemRepository.findByDocumentId(existing.getId())));
    }

    public int getAvailableQtyForBatch(int batchId) {
        PlantBatch batch = requireBatch(batchId);
        int usedQty = itemRepository.sumQtyInActiveDocumentsByPlantBatchId(batchId);
        return Math.max(0, batch.getQty() - usedQty);
    }

    public int getAvailableQtyForBatch(int batchId, Integer currentDocumentId) {
        PlantBatch batch = requireBatch(batchId);

        int usedQty = currentDocumentId == null
                ? itemRepository.sumQtyInActiveDocumentsByPlantBatchId(batchId)
                : itemRepository.sumQtyInActiveDocumentsByPlantBatchIdExcludingDocument(batchId, currentDocumentId);

        return Math.max(0, batch.getQty() - usedQty);
    }

    public void validateImportedDocument(DocumentDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dokument nie może być pusty.");
        }

        if (dto.getDocumentNumber() == null || dto.getDocumentNumber().isBlank()) {
            throw new IllegalArgumentException("Numer dokumentu jest wymagany przy imporcie CSV.");
        }

        if (dto.getStatus() == null) {
            dto.setStatus(DocumentStatus.ACTIVE);
        }

        validateDocument(dto, null);
    }

    private void validateDocument(DocumentDTO dto, Integer currentDocumentId) {
        if (dto == null) {
            throw new IllegalArgumentException("Dokument nie może być pusty.");
        }

        if (dto.getDocumentType() == null || dto.getDocumentType().isBlank()) {
            throw new IllegalArgumentException("Typ dokumentu jest wymagany.");
        }

        if (dto.getCreatedBy() == null || dto.getCreatedBy().isBlank()) {
            throw new IllegalArgumentException("Pole „Utworzył” jest wymagane.");
        }

        if (dto.getIssueDate() == null) {
            throw new IllegalArgumentException("Data wystawienia jest wymagana.");
        }

        if (dto.getContrahentId() <= 0) {
            throw new IllegalArgumentException("Wybierz kontrahenta.");
        }

        validateItems(dto.getItems(), currentDocumentId);
    }

    private void validateItems(List<DocumentItemDTO> items, Integer currentDocumentId) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Dokument musi zawierać co najmniej jedną pozycję.");
        }

        Set<Integer> usedBatchIdsInDocument = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {
            DocumentItemDTO item = items.get(i);
            int lineNo = i + 1;

            if (item == null) {
                throw new IllegalArgumentException("Pozycja " + lineNo + " jest pusta.");
            }

            if (item.getPlantBatchId() <= 0) {
                throw new IllegalArgumentException("Pozycja " + lineNo + ": wybierz partię roślin.");
            }

            if (item.getQty() <= 0) {
                throw new IllegalArgumentException("Pozycja " + lineNo + ": ilość musi być większa od zera.");
            }

            if (!usedBatchIdsInDocument.add(item.getPlantBatchId())) {
                throw new IllegalArgumentException(
                        "Partia została dodana więcej niż raz w jednym dokumencie. Połącz ilości w jedną pozycję."
                );
            }

            PlantBatch batch = requireBatch(item.getPlantBatchId());

            if (batch.getStatus() == PlantBatchStatus.CANCELLED) {
                throw new IllegalArgumentException(
                        "Pozycja " + lineNo + ": nie można użyć anulowanej partii " + formatBatchLabel(batch) + "."
                );
            }

            int availableQty = getAvailableQtyForBatch(item.getPlantBatchId(), currentDocumentId);
            if (item.getQty() > availableQty) {
                throw new IllegalArgumentException(
                        "Pozycja " + lineNo
                                + ": ilość przekracza stan dostępny dla partii "
                                + formatBatchLabel(batch)
                                + ". Dostępne: " + availableQty + ", wpisane: " + item.getQty() + "."
                );
            }
        }
    }

    private PlantBatch requireBatch(int batchId) {
        PlantBatch batch = plantBatchService.getBatchById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("Wybrana partia roślin nie istnieje.");
        }
        return batch;
    }

    private void saveItems(int documentId, List<DocumentItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (DocumentItemDTO itemDTO : items) {
            DocumentItem item = new DocumentItem();
            item.setDocumentId(documentId);
            item.setPlantBatchId(itemDTO.getPlantBatchId());
            item.setQty(itemDTO.getQty());
            item.setPassportRequired(itemDTO.isPassportRequired());

            itemRepository.save(item);
        }
    }

    private DocumentDTO mapToDto(Document document, List<DocumentItem> items) {

        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setDocumentNumber(document.getDocumentNumber());
        dto.setDocumentType(document.getDocumentType());
        dto.setIssueDate(document.getIssueDate());
        dto.setContrahentId(document.getContrahentId());
        dto.setCreatedBy(document.getCreatedBy());
        dto.setComments(document.getComments());
        dto.setStatus(document.getStatus());

        List<DocumentItemDTO> itemDtos = new ArrayList<>();

        if (items != null) {
            for (DocumentItem item : items) {
                DocumentItemDTO itemDto = new DocumentItemDTO();
                itemDto.setPlantBatchId(item.getPlantBatchId());
                itemDto.setQty(item.getQty());
                itemDto.setPassportRequired(item.isPassportRequired());
                itemDtos.add(itemDto);
            }
        }

        dto.setItems(itemDtos);
        return dto;
    }

    private PlantBatch resolveBatchContext(List<DocumentItemDTO> items) {

        if (items == null || items.isEmpty()) {
            return null;
        }

        DocumentItemDTO firstItem = items.get(0);

        if (firstItem == null || firstItem.getPlantBatchId() <= 0) {
            return null;
        }

        return plantBatchService.getBatchById(firstItem.getPlantBatchId());
    }

    private String formatBatchLabel(PlantBatch batch) {
        if (batch.getInteriorBatchNo() != null && !batch.getInteriorBatchNo().isBlank()) {
            return batch.getInteriorBatchNo();
        }
        if (batch.getExteriorBatchNo() != null && !batch.getExteriorBatchNo().isBlank()) {
            return batch.getExteriorBatchNo();
        }
        return "ID " + batch.getId();
    }

    private String buildDocumentSummary(Document document, List<?> items) {
        if (document == null) {
            return "[brak danych]";
        }

        String number = safe(document.getDocumentNumber());
        String type = safe(document.getDocumentType());
        String date = document.getIssueDate() == null ? "brak daty" : document.getIssueDate().toString();
        String status = document.getStatus() == null ? "UNKNOWN" : document.getStatus().name();
        int itemCount = items == null ? 0 : items.size();

        return "nr " + fallback(number, "[bez numeru]")
                + ", typ " + fallback(type, "[bez typu]")
                + ", data " + date
                + ", status " + status
                + ", pozycji: " + itemCount;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String fallback(String value, String fallbackValue) {
        return value == null || value.isBlank() ? fallbackValue : value;
    }

    private void logAudit(String entityType, Integer entityId, String actionType, String description) {
        if (auditLogService != null) {
            auditLogService.log(entityType, entityId, actionType, description);
        }
    }

}
