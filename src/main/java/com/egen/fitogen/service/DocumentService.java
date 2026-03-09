package com.egen.fitogen.service;

import com.egen.fitogen.domain.Document;
import com.egen.fitogen.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DocumentService {

    private final DocumentRepository repository;
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    public List<Document> getAllDocuments() {
        logger.info("Fetching all documents");
        return repository.findAll();
    }

    public List<Document> getDocumentsByPlantBatchId(int plantBatchId) {
        logger.info("Fetching documents by plantBatchId {}", plantBatchId);
        return repository.findByPlantBatchId(plantBatchId);
    }

    public List<Document> getDocumentsByContrahentId(int contrahentId) {
        logger.info("Fetching documents by contrahentId {}", contrahentId);
        return repository.findByContrahentId(contrahentId);
    }

    public void addDocument(Document document) {
        logger.info("Adding document for plantBatchId {} and contrahentId {}", document.getPlantBatchId(), document.getContrahentId());
        repository.save(document);
    }

    public void updateDocument(Document document) {
        logger.info("Updating document id {}", document.getId());
        repository.update(document);
    }

    public void deleteDocument(int id) {
        logger.info("Deleting document id {}", id);
        repository.delete(id);
    }
}