package com.egen.fitogen.repository;

import com.egen.fitogen.model.DocumentItem;

import java.util.List;

public interface DocumentItemRepository {

    void save(DocumentItem item);

    List<DocumentItem> findByDocumentId(int documentId);

    void deleteByDocumentId(int documentId);

    List<String> findActiveDocumentNumbersByPlantBatchId(int plantBatchId);

    int sumQtyInActiveDocumentsByPlantBatchId(int plantBatchId);

    int sumQtyInActiveDocumentsByPlantBatchIdExcludingDocument(int plantBatchId, int documentId);
}