package com.egen.fitogen.repository;

import com.egen.fitogen.domain.Plant;
import java.util.List;

public interface PlantRepository {

    List<Plant> findAll();

    Plant findById(int id);

    void save(Plant plant);

    void update(Plant plant);

    void delete(int id);
}