package com.egen.fitogen;

import com.egen.fitogen.domain.Plant;
import com.egen.fitogen.database.SqlitePlantRepository;

public class MainApp {
    public static void main(String[] args) {

        SqlitePlantRepository repo = new SqlitePlantRepository();

        Plant plant = new Plant(1, "Róża", "Chippendale", "PodkładkaA", "Rosa Chippendale", "ACTIVE");
        repo.save(plant);

        System.out.println("Plants in repo:");
        repo.findAll().forEach(p -> System.out.println(p.getSpecies()));
    }
}