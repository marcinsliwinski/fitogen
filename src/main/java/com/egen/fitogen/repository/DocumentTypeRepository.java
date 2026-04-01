package com.egen.fitogen.repository;

import com.egen.fitogen.model.DocumentType;

import java.util.List;

public interface DocumentTypeRepository {

    List<DocumentType> findAll();

    void save(DocumentType documentType);

    void update(DocumentType documentType);

    void deleteById(int id);
}