package com.egen.fitogen.repository;

import com.egen.fitogen.domain.DocumentItem;

import java.util.List;

public interface DocumentItemRepository {

    List<DocumentItem> findByDocumentId(int documentId);

    void save(DocumentItem item);
}