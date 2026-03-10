package com.egen.fitogen.repository;

import com.egen.fitogen.domain.Document;
import java.util.List;

public interface DocumentRepository {

    List<Document> findAll();

    Document findById(int id);

    void save(Document document);

    void update(Document document);

    void delete(int id);

    // Optional: find by ContahentId
    List<Document> findByContrahentId(int contrahentId);

    // Optional: find by  plant batch
    List<Document> findByPlantBatchId(int plantBatchId);
}