package com.egen.fitogen.repository;

import com.egen.fitogen.domain.Document;
import java.util.List;

public interface DocumentRepository {

    void save(Document document);

    Document findById(int id);

    List<Document> findAll();
}