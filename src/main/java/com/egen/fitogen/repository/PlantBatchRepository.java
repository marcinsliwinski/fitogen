package com.egen.fitogen.repository;

import com.egen.fitogen.model.PlantBatch;

import java.util.List;

public interface PlantBatchRepository {

    void save(PlantBatch batch);

    void update(PlantBatch batch);

    void deleteById(int id);

    PlantBatch findById(int id);

    List<PlantBatch> findAll();
}