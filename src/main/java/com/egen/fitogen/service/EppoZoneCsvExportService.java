package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoZone;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EppoZoneCsvExportService {

    private final EppoZoneService eppoZoneService;

    public EppoZoneCsvExportService(EppoZoneService eppoZoneService) {
        this.eppoZoneService = eppoZoneService;
    }

    public Path export(Path outputPath) {
        List<EppoZone> zones = eppoZoneService.getAll();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("code;name;countryCode;status");
            writer.newLine();

            for (EppoZone zone : zones) {
                writer.write(String.join(";",
                        escape(zone.getCode()),
                        escape(zone.getName()),
                        escape(zone.getCountryCode()),
                        escape(zone.getStatus())
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować stref EPPO do CSV: " + outputPath, e);
        }

        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Kolumny eksportu: kod strefy (code), nazwa strefy (name), kod kraju (countryCode), status (status).";
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
