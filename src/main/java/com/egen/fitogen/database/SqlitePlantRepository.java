package com.egen.fitogen.database;

import com.egen.fitogen.domain.Plant;
import com.egen.fitogen.repository.PlantRepository;
import java.util.ArrayList;
import java.util.List;

public class SqlitePlantRepository implements PlantRepository {

    private final List<Plant> plants = new ArrayList<>();

    @Override
    public List<Plant> findAll() {
        return plants;
    }

    @Override
    public Plant findById(int id) {
        return plants.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    @Override
    public void save(Plant plant) {
        plants.add(plant);
    }

    @Override
    public void update(Plant plant) {
        // simple replace
        delete(plant.getId());
        save(plant);
    }

    @Override
    public void delete(int id) {
        plants.removeIf(p -> p.getId() == id);
    }
}