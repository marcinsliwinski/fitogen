package com.egen.fitogen.repository;

import com.egen.fitogen.domain.PlantBatch;
import java.util.List;

public interface PlantBatchRepository {

    List<PlantBatch> findAll();

    PlantBatch findById(int id);

    void save(PlantBatch plantBatch);

    void update(PlantBatch plantBatch);

    void delete(int id);

    // Opcjonalnie: wyszukiwanie po kontrahencie
    List<PlantBatch> findByContrahentId(int contrahentId);
}