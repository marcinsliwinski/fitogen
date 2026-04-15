package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoCodeSpeciesLink;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EppoCodeSpeciesLinkCsvExportService {

    private final EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService;
    private final EppoCodeService eppoCodeService;

    public EppoCodeSpeciesLinkCsvExportService(
            EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService,
            EppoCodeService eppoCodeService
    ) {
        this.eppoCodeSpeciesLinkService = eppoCodeSpeciesLinkService;
        this.eppoCodeService = eppoCodeService;
    }

    public Path export(Path outputPath) {
        List<EppoCodeSpeciesLink> links = eppoCodeSpeciesLinkService.getAll();
        Map<Integer, String> codeById = buildCodeMap();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("eppoCode;speciesName;latinSpeciesName");
            writer.newLine();

            for (EppoCodeSpeciesLink link : links) {
                writer.write(String.join(";",
                        escape(codeById.getOrDefault(link.getEppoCodeId(), "")),
                        escape(link.getSpeciesName()),
                        escape(link.getLatinSpeciesName())
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować powiązań kod EPPO -> gatunek do CSV: " + outputPath, e);
        }

        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Kolumny eksportu: kod EPPO (eppoCode), gatunek (speciesName), nazwa łacińska (latinSpeciesName).";
    }

    private Map<Integer, String> buildCodeMap() {
        Map<Integer, String> result = new HashMap<>();
        for (EppoCode code : eppoCodeService.getAll()) {
            result.put(code.getId(), code.getCode());
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
