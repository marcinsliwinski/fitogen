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
            DocumentType existing = findById(documentType.getId());
            repository.update(documentType);
            logAudit("UPDATE", documentType,
                    "Zaktualizowano typ dokumentu " + summarize(documentType)
                            + ". Wcześniej: " + summarize(existing));
        } else {
            repository.save(documentType);
            logAudit("CREATE", documentType, "Utworzono typ dokumentu " + summarize(documentType));
        }
    }

    public void delete(int id) {
        DocumentType existing = findById(id);
        repository.deleteById(id);
        logAudit("DELETE", existing, "Usunięto typ dokumentu " + summarize(existing));
    }

    private void validate(DocumentType documentType) {
        if (documentType == null) {
            throw new IllegalArgumentException("Typ dokumentu nie może być pusty.");
        }
        if (documentType.getName() == null || documentType.getName().isBlank()) {
            throw new IllegalArgumentException("Nazwa typu dokumentu jest wymagana.");
        }
    }

    private void logAudit(String actionType, DocumentType documentType, String description) {
        if (auditLogService != null) {
            auditLogService.log("DOCUMENT_TYPE", documentType == null ? null : documentType.getId(), actionType, description);
        }
    }

    private String summarize(DocumentType documentType) {
        if (documentType == null) {
            return "[brak danych]";
        }

        String name = documentType.getName() == null || documentType.getName().isBlank()
                ? "[bez nazwy]"
                : documentType.getName().trim();
        String code = documentType.getCode() == null || documentType.getCode().isBlank()
                ? "[bez kodu]"
                : documentType.getCode().trim();
        return "nazwa=" + name + ", kod=" + code;
    }


    private DocumentType findById(int id) {
        for (DocumentType item : repository.findAll()) {
            if (item != null && item.getId() == id) {
                return item;
            }
        }
        return null;
    }

}
