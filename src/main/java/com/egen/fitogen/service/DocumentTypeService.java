package com.egen.fitogen.service;

import com.egen.fitogen.model.DocumentType;
import com.egen.fitogen.repository.DocumentTypeRepository;

import java.util.List;

public class DocumentTypeService {

    private final DocumentTypeRepository repository;

    public DocumentTypeService(DocumentTypeRepository repository) {
        this.repository = repository;
    }

    public List<DocumentType> getAll() {
        return repository.findAll();
    }

    public void save(DocumentType documentType) {
        validate(documentType);
        if (documentType.getId() > 0) {
            repository.update(documentType);
        } else {
            repository.save(documentType);
        }
    }

    public void delete(int id) {
        repository.deleteById(id);
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