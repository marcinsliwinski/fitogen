package com.egen.fitogen.repository;

import com.egen.fitogen.model.Document;

import java.util.List;

public interface DocumentRepository {

    void save(Document document);

    void update(Document document);

    void deleteById(int id);

    Document findById(int id);

    List<Document> findAll();
}