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
        boolean update = documentType.getId() > 0;
        if (update) {
            repository.update(documentType);
        } else {
            repository.save(documentType);
        }

        if (auditLogService != null) {
            auditLogService.log(
                    "DOCUMENT_TYPE",
                    documentType.getId() > 0 ? documentType.getId() : null,
                    update ? "UPDATE" : "CREATE",
                    (update ? "Zaktualizowano typ dokumentu: " : "Dodano typ dokumentu: ") + buildDocumentTypeSummary(documentType)
            );
        }
    }

    public void delete(int id) {
        repository.deleteById(id);
        if (auditLogService != null) {
            auditLogService.log("DOCUMENT_TYPE", id, "DELETE", "Usunięto typ dokumentu o ID=" + id);
        }
    }

    private String buildDocumentTypeSummary(DocumentType documentType) {
        String name = documentType.getName() == null ? "" : documentType.getName().trim();
        String code = documentType.getCode() == null ? "" : documentType.getCode().trim();
        return code.isBlank() ? name : name + " / " + code;
    }

    private void validate(DocumentType documentType) {
        if (documentType == null) {
            throw new IllegalArgumentException("Typ dokumentu nie może być pusty.");
        }
        if (documentType.getName() == null || documentType.getName().isBlank()) {
            throw new IllegalArgumentException("Nazwa typu dokumentu jest wymagana.");
        }
    }
}