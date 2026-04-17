package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoCodeSpeciesLink;
import com.egen.fitogen.model.EppoZone;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EppoDictionaryCsvExportService {

    private static final String TYPE_SPECIES = "SPECIES";
    private static final String TYPE_ZONE = "ZONE";

    private final EppoCodeService eppoCodeService;
    private final EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService;
    private final EppoCodeZoneLinkService eppoCodeZoneLinkService;

    public EppoDictionaryCsvExportService(
            EppoCodeService eppoCodeService,
            EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService,
            EppoCodeZoneLinkService eppoCodeZoneLinkService
    ) {
        this.eppoCodeService = eppoCodeService;
        this.eppoCodeSpeciesLinkService = eppoCodeSpeciesLinkService;
        this.eppoCodeZoneLinkService = eppoCodeZoneLinkService;
    }

    public Path export(Path outputPath) {
        Map<Integer, EppoCode> codeById = new HashMap<>();
        for (EppoCode code : eppoCodeService.getAll()) {
            codeById.put(code.getId(), code);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("relationType;eppoCode;codeCommonName;codeScientificName;speciesName;latinSpeciesName;zoneCode;zoneName;countryCode;passportRequired;codeStatus;zoneStatus");
            writer.newLine();

            for (EppoCode code : codeById.values()) {
                List<EppoCodeSpeciesLink> speciesLinks = eppoCodeSpeciesLinkService.getEffectiveSpeciesLinks(code.getId());
                for (EppoCodeSpeciesLink link : speciesLinks) {
                    writer.write(String.join(";",
                            TYPE_SPECIES,
                            escape(code.getCode()),
                            escape(code.getCommonName()),
                            escape(code.getScientificName()),
                            escape(link.getSpeciesName()),
                            escape(link.getLatinSpeciesName()),
                            "",
                            "",
                            "",
                            String.valueOf(code.isPassportRequired()),
                            escape(code.getStatus()),
                            ""
                    ));
                    writer.newLine();
                }

                List<EppoZone> zones = eppoCodeZoneLinkService.getZonesForCode(code.getId());
                for (EppoZone zone : zones) {
                    writer.write(String.join(";",
                            TYPE_ZONE,
                            escape(code.getCode()),
                            escape(code.getCommonName()),
                            escape(code.getScientificName()),
                            "",
                            "",
                            escape(zone.getCode()),
                            escape(zone.getName()),
                            escape(zone.getCountryCode()),
                            String.valueOf(code.isPassportRequired()),
                            escape(code.getStatus()),
                            escape(zone.getStatus())
                    ));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować słowników EPPO do CSV: " + outputPath, e);
        }

        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Jeden plik CSV słowników EPPO. Kolumny: typ relacji (relationType: SPECIES lub ZONE), kod EPPO (eppoCode), nazwa polska kodu/agrofaga (codeCommonName), nazwa łacińska kodu/agrofaga (codeScientificName), gatunek żywicielski (speciesName), nazwa łacińska gatunku (latinSpeciesName), kod strefy (zoneCode), nazwa strefy (zoneName), kod kraju (countryCode), wymagany paszport (passportRequired), status kodu (codeStatus), status strefy (zoneStatus).";
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
