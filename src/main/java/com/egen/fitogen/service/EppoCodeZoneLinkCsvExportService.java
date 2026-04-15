package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoCodeZoneLink;
import com.egen.fitogen.model.EppoZone;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EppoCodeZoneLinkCsvExportService {

    private final EppoCodeZoneLinkService eppoCodeZoneLinkService;
    private final EppoCodeService eppoCodeService;
    private final EppoZoneService eppoZoneService;

    public EppoCodeZoneLinkCsvExportService(
            EppoCodeZoneLinkService eppoCodeZoneLinkService,
            EppoCodeService eppoCodeService,
            EppoZoneService eppoZoneService
    ) {
        this.eppoCodeZoneLinkService = eppoCodeZoneLinkService;
        this.eppoCodeService = eppoCodeService;
        this.eppoZoneService = eppoZoneService;
    }

    public Path export(Path outputPath) {
        List<EppoCodeZoneLink> links = eppoCodeZoneLinkService.getAll();
        Map<Integer, String> codeById = buildCodeMap();
        Map<Integer, EppoZone> zonesById = buildZoneMap();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("eppoCode;zoneCode;zoneName;countryCode");
            writer.newLine();

            for (EppoCodeZoneLink link : links) {
                EppoZone zone = zonesById.get(link.getEppoZoneId());
                writer.write(String.join(";",
                        escape(codeById.getOrDefault(link.getEppoCodeId(), "")),
                        escape(zone == null ? "" : zone.getCode()),
                        escape(zone == null ? "" : zone.getName()),
                        escape(zone == null ? "" : zone.getCountryCode())
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować powiązań kod EPPO -> strefa do CSV: " + outputPath, e);
        }

        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Kolumny eksportu: kod EPPO (eppoCode), kod strefy (zoneCode), nazwa strefy (zoneName), kod kraju strefy (countryCode).";
    }

    private Map<Integer, String> buildCodeMap() {
        Map<Integer, String> result = new HashMap<>();
        for (EppoCode code : eppoCodeService.getAll()) {
            result.put(code.getId(), code.getCode());
        }
        return result;
    }

    private Map<Integer, EppoZone> buildZoneMap() {
        Map<Integer, EppoZone> result = new HashMap<>();
        for (EppoZone zone : eppoZoneService.getAll()) {
            result.put(zone.getId(), zone);
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
