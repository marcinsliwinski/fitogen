package com.egen.fitogen.service;

import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.model.PlantBatch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantBatchCsvExportService {
    private final PlantBatchService plantBatchService;
    private final PlantService plantService;
    private final ContrahentService contrahentService;

    public PlantBatchCsvExportService(PlantBatchService plantBatchService,
                                      PlantService plantService,
                                      ContrahentService contrahentService) {
        this.plantBatchService = plantBatchService;
        this.plantService = plantService;
        this.contrahentService = contrahentService;
    }

    public Path export(Path outputPath) {
        List<PlantBatch> batches = plantBatchService.getAllBatches();
        Map<Integer, Plant> plantById = new HashMap<>();
        for (Plant plant : plantService.getAllPlants()) {
            plantById.put(plant.getId(), plant);
        }
        Map<Integer, Contrahent> contrahentById = new HashMap<>();
        for (Contrahent contrahent : contrahentService.getAllContrahents()) {
            contrahentById.put(contrahent.getId(), contrahent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("batchId;interiorBatchNo;exteriorBatchNo;species;variety;rootstock;sourceContrahentName;qty;ageYears;creationDate;manufacturerCountryCode;fitoQualificationCategory;eppoCode;zpZone;internalSource;comments;status");
            writer.newLine();

            for (PlantBatch batch : batches) {
                Plant plant = plantById.get(batch.getPlantId());
                Contrahent contrahent = contrahentById.get(batch.getContrahentId());
                writer.write(String.join(";",
                        String.valueOf(batch.getId()),
                        escape(batch.getInteriorBatchNo()),
                        escape(batch.getExteriorBatchNo()),
                        escape(plant == null ? "" : plant.getSpecies()),
                        escape(plant == null ? "" : plant.getVariety()),
                        escape(plant == null ? "" : plant.getRootstock()),
                        escape(contrahent == null ? "" : contrahent.getName()),
                        String.valueOf(batch.getQty()),
                        String.valueOf(batch.getAgeYears()),
                        escape(batch.getCreationDate() == null ? "" : batch.getCreationDate().toString()),
                        escape(batch.getManufacturerCountryCode()),
                        escape(batch.getFitoQualificationCategory()),
                        escape(batch.getEppoCode()),
                        escape(batch.getZpZone()),
                        String.valueOf(batch.isInternalSource()),
                        escape(batch.getComments()),
                        escape(batch.getStatus() == null ? "" : batch.getStatus().name())
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować partii roślin do CSV: " + outputPath, e);
        }

        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Kolumny eksportu: batchId, interiorBatchNo, exteriorBatchNo, species, variety, rootstock, sourceContrahentName, qty, ageYears, creationDate, manufacturerCountryCode, fitoQualificationCategory, eppoCode, zpZone, internalSource, comments, status.";
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
