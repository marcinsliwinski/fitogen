package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoCodePlantLink;
import com.egen.fitogen.model.Plant;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EppoCodePlantLinkCsvExportService {

    private final EppoCodePlantLinkService eppoCodePlantLinkService;
    private final EppoCodeService eppoCodeService;
    private final PlantService plantService;

    public EppoCodePlantLinkCsvExportService(
            EppoCodePlantLinkService eppoCodePlantLinkService,
            EppoCodeService eppoCodeService,
            PlantService plantService
    ) {
        this.eppoCodePlantLinkService = eppoCodePlantLinkService;
        this.eppoCodeService = eppoCodeService;
        this.plantService = plantService;
    }

    public Path export(Path outputPath) {
        List<EppoCodePlantLink> links = eppoCodePlantLinkService.getAll();
        Map<Integer, String> codeById = buildCodeMap();
        Map<Integer, Plant> plantsById = buildPlantMap();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("eppoCode;plantSpecies;plantVariety;plantRootstock;plantLatinSpeciesName");
            writer.newLine();

            for (EppoCodePlantLink link : links) {
                Plant plant = plantsById.get(link.getPlantId());
                writer.write(String.join(";",
                        escape(codeById.getOrDefault(link.getEppoCodeId(), "")),
                        escape(plant == null ? "" : plant.getSpecies()),
                        escape(plant == null ? "" : plant.getVariety()),
                        escape(plant == null ? "" : plant.getRootstock()),
                        escape(plant == null ? "" : plant.getLatinSpeciesName())
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować powiązań kod EPPO -> roślina do CSV: " + outputPath, e);
        }

        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Kolumny eksportu: kod EPPO (eppoCode), gatunek rośliny (plantSpecies), odmiana (plantVariety), podkładka (plantRootstock), nazwa łacińska rośliny (plantLatinSpeciesName).";
    }

    private Map<Integer, String> buildCodeMap() {
        Map<Integer, String> result = new HashMap<>();
        for (EppoCode code : eppoCodeService.getAll()) {
            result.put(code.getId(), code.getCode());
        }
        return result;
    }

    private Map<Integer, Plant> buildPlantMap() {
        Map<Integer, Plant> result = new HashMap<>();
        for (Plant plant : plantService.getAllPlants()) {
            result.put(plant.getId(), plant);
        }
        return result;
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
