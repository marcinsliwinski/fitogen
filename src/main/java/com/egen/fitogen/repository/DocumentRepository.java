package com.egen.fitogen.repository;

import com.egen.fitogen.domain.Document;
import java.util.List;

public interface DocumentRepository {

    List<Document> findAll();

    Document findById(int id);

    void save(Document document);

    void update(Document document);

    void delete(int id);

    // Opcjonalnie: wyszukiwanie po kontrahencie
    List<Document> findByContrahentId(int contrahentId);

    // Opcjonalnie: wyszukiwanie po partii roślin
    List<Document> findByPlantBatchId(int plantBatchId);
}