package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EppoCodeCsvExportService {

    private final EppoCodeService eppoCodeService;

    public EppoCodeCsvExportService(EppoCodeService eppoCodeService) {
        this.eppoCodeService = eppoCodeService;
    }

    public Path export(Path outputPath) {
        List<EppoCode> codes = eppoCodeService.getAll();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("code;speciesName;latinSpeciesName;scientificName;commonName;passportRequired;status");
            writer.newLine();

            for (EppoCode code : codes) {
                writer.write(String.join(";",
                        escape(code.getCode()),
                        escape(code.getSpeciesName()),
                        escape(code.getLatinSpeciesName()),
                        escape(code.getScientificName()),
                        escape(code.getCommonName()),
                        String.valueOf(code.isPassportRequired()),
                        escape(code.getStatus())
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować kodów EPPO do CSV: " + outputPath, e);
        }

        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Kolumny eksportu: kod EPPO (code), gatunek (speciesName), nazwa łacińska (latinSpeciesName), naukowa zgodność legacy (scientificName), nazwa zwyczajowa legacy (commonName), wymagany paszport (passportRequired), status (status).";
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
