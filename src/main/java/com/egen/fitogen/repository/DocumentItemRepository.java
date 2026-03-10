package com.egen.fitogen.repository;

import com.egen.fitogen.domain.DocumentItem;
import java.util.List;

public interface DocumentItemRepository {

    void save(DocumentItem item);

    List<DocumentItem> findByDocumentId(int documentId);
}