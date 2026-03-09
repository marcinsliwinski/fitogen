package com.egen.fitogen.service;

import com.egen.fitogen.domain.PlantBatch;
import com.egen.fitogen.repository.PlantBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PlantBatchService {

    private final PlantBatchRepository repository;
    private static final Logger logger = LoggerFactory.getLogger(PlantBatchService.class);

    public PlantBatchService(PlantBatchRepository repository) {
        this.repository = repository;
    }

    public List<PlantBatch> getAllBatches() {
        logger.info("Fetching all plant batches");
        return repository.findAll();
    }

    public PlantBatch getBatchById(int id) {
        logger.info("Fetching plant batch by id {}", id);
        return repository.findById(id);
    }

    public void addBatch(PlantBatch batch) {
        logger.info("Adding new batch: {}", batch.getInteriorBatchNo());
        repository.save(batch);
    }

    public void updateBatch(PlantBatch batch) {
        logger.info("Updating batch id {}", batch.getId());
        repository.update(batch);
    }

    public void deleteBatch(int id) {
        logger.info("Deleting batch id {}", id);
        repository.delete(id);
    }
}