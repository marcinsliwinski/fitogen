package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoZone;
import com.egen.fitogen.model.Plant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class EppoAdvisoryService {

    private final EppoCodePlantLinkService eppoCodePlantLinkService;
    private final EppoCodeZoneLinkService eppoCodeZoneLinkService;

    public EppoAdvisoryService(EppoCodePlantLinkService eppoCodePlantLinkService,
                               EppoCodeZoneLinkService eppoCodeZoneLinkService) {
        this.eppoCodePlantLinkService = eppoCodePlantLinkService;
        this.eppoCodeZoneLinkService = eppoCodeZoneLinkService;
    }

    public AdvisoryResult analyzePlantForCountry(Plant plant, String clientCountryCode, int rowNumber, String rowPrefix) {
        String normalizedPrefix = safe(rowPrefix).isBlank() ? "Pozycja" : safe(rowPrefix);
        String normalizedCountryCode = safe(clientCountryCode).toUpperCase();

        if (plant == null) {
            return new AdvisoryResult(
                    normalizedPrefix + " " + rowNumber + " — brak wybranej rośliny.",
                    false,
                    false,
                    false
            );
        }

        List<EppoCode> codes = eppoCodePlantLinkService.getCodesForPlant(plant.getId());
        if (codes == null || codes.isEmpty()) {
            String message = normalizedPrefix + " " + rowNumber + " — " + formatPlantName(plant)
                    + ": Status: brak dopasowanego kodu EPPO dla gatunku w słowniku." + buildPassportNote(plant);
            return new AdvisoryResult(message, false, true, plant.isPassportRequired());
        }

        List<String> codeLabels = codes.stream()
                .map(this::formatCodeLabel)
                .filter(label -> !label.isBlank())
                .distinct()
                .toList();

        List<EppoZone> matchedZones = codes.stream()
                .flatMap(code -> eppoCodeZoneLinkService.getZonesForCode(code.getId()).stream())
                .filter(zone -> zone != null)
                .filter(zone -> safe(zone.getCountryCode()).equalsIgnoreCase(normalizedCountryCode))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(EppoZone::getId, zone -> zone, (left, right) -> left, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));

        String base = normalizedPrefix + " " + rowNumber + " — " + formatPlantName(plant)
                + "Kody EPPO: " + String.join(", ", codeLabels) + ". ";

        if (matchedZones.isEmpty()) {
            return new AdvisoryResult(
                    base + "Status: brak dopasowanego kraju EPPO dla kraju klienta [" + normalizedCountryCode + "]." + buildPassportNote(plant),
                    false,
                    false,
                    plant.isPassportRequired()
            );
        }

        String zonesText = matchedZones.stream()
                .map(this::formatZoneLabel)
                .filter(label -> !label.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        return new AdvisoryResult(
                base + "Status: znaleziono dopasowane kraje EPPO dla kraju klienta [" + normalizedCountryCode + "]: " + zonesText + "." + buildPassportNote(plant),
                true,
                false,
                plant.isPassportRequired()
        );
    }

    public String formatPlantName(Plant plant) {
        if (plant == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if (!safe(plant.getSpecies()).isBlank()) {
            parts.add(safe(plant.getSpecies()));
        }
        if (!safe(plant.getVariety()).isBlank()) {
            parts.add(safe(plant.getVariety()));
        }
        if (!safe(plant.getRootstock()).isBlank()) {
            parts.add("na " + safe(plant.getRootstock()));
        }

        if (!parts.isEmpty()) {
            return String.join(" ", parts) + " ";
        }

        String latin = safe(plant.getLatinSpeciesName());
        return latin.isBlank() ? ("Roślina ID " + plant.getId() + " ") : (latin + " ");
    }

    public String formatCodeLabel(EppoCode code) {
        if (code == null) {
            return "";
        }
        String codeValue = safe(code.getCode());
        String nameValue = firstNonBlank(
                safe(code.getDisplaySpeciesName()),
                safe(code.getDisplayLatinSpeciesName()),
                safe(code.getCommonName()),
                safe(code.getScientificName())
        );
        return nameValue.isBlank() ? codeValue : codeValue + " (" + nameValue + ")";
    }

    public String formatZoneLabel(EppoZone zone) {
        if (zone == null) {
            return "";
        }
        String code = safe(zone.getCountryCode());
        String name = safe(zone.getName());
        return name.isBlank() ? code : code + " (" + name + ")";
    }

    public String buildPassportNote(Plant plant) {
        if (plant == null || !plant.isPassportRequired()) {
            return "";
        }
        return " Uwaga paszportowa: roślina ma ustawione 'Wymaga paszportu'.";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public record AdvisoryResult(String message, boolean elevatedRisk, boolean missingCode, boolean passportAttention) {
    }
}
