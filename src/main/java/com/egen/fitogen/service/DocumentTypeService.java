package com.egen.fitogen.service;

import com.egen.fitogen.model.DocumentType;
import com.egen.fitogen.repository.DocumentTypeRepository;

import java.util.List;

public class DocumentTypeService {

    private final DocumentTypeRepository repository;
    private final AuditLogService auditLogService;

    public DocumentTypeService(DocumentTypeRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    public List<DocumentType> getAll() {
        return repository.findAll();
    }

    public void save(DocumentType documentType) {
        validate(documentType);

        if (documentType.getId() > 0) {
            repository.update(documentType);
            log("UPDATE", documentType.getId(), "Zaktualizowano typ dokumentu: " + describe(documentType));
            return;
        }

        repository.save(documentType);
        log("CREATE", null, "Dodano typ dokumentu: " + describe(documentType));
    }

    public void delete(int id) {
        DocumentType existing = findById(id);
        repository.deleteById(id);
        log("DELETE", id, "Usunięto typ dokumentu: " + describe(existing));
    }

    private void validate(DocumentType documentType) {
        if (documentType == null) {
            throw new IllegalArgumentException("Typ dokumentu nie może być pusty.");
        }
        if (documentType.getName() == null || documentType.getName().isBlank()) {
            throw new IllegalArgumentException("Nazwa typu dokumentu jest wymagana.");
        }
    }

    private DocumentType findById(int id) {
        return repository.findAll().stream()
                .filter(type -> type.getId() == id)
                .findFirst()
                .orElse(null);
    }

    private void log(String actionType, Integer entityId, String description) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.log("DOCUMENT_TYPE", entityId, actionType, description);
    }

    private String describe(DocumentType documentType) {
        if (documentType == null) {
            return "[brak danych]";
        }

        String name = documentType.getName() == null ? "" : documentType.getName().trim();
        String code = documentType.getCode() == null ? "" : documentType.getCode().trim();

        if (name.isBlank() && code.isBlank()) {
            return "[brak danych]";
        }
        if (code.isBlank()) {
            return name;
        }
        if (name.isBlank()) {
            return code;
        }
        return name + " (" + code + ")";
    }
}
