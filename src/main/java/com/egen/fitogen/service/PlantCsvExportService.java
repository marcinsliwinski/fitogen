package com.egen.fitogen.service;

import com.egen.fitogen.model.Plant;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PlantCsvExportService {
    private final PlantService plantService;

    public PlantCsvExportService(PlantService plantService) {
        this.plantService = plantService;
    }

    public Path export(Path outputPath) {
        List<Plant> plants = plantService.getAllPlants();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("species;variety;rootstock;latinSpeciesName;eppoCode;passportRequired;visibilityStatus");
            writer.newLine();

            for (Plant plant : plants) {
                writer.write(String.join(";",
                        escape(plant.getSpecies()),
                        escape(plant.getVariety()),
                        escape(plant.getRootstock()),
                        escape(plant.getLatinSpeciesName()),
                        escape(plant.getEppoCode()),
                        String.valueOf(plant.isPassportRequired()),
                        escape(plant.getVisibilityStatus())
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować roślin do CSV: " + outputPath, e);
        }

        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Kolumny eksportu: gatunek (species), odmiana (variety), podkładka (rootstock), nazwa łacińska (latinSpeciesName), kod EPPO (eppoCode), wymagany paszport (passportRequired), status widoczności (visibilityStatus).";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
