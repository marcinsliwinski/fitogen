package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.Plant;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PassportAdvisoryService {

    private final EppoCodePlantLinkService eppoCodePlantLinkService;

    public PassportAdvisoryService(EppoCodePlantLinkService eppoCodePlantLinkService) {
        this.eppoCodePlantLinkService = eppoCodePlantLinkService;
    }

    public AdvisoryResult analyzePlant(Plant plant) {
        if (plant == null) {
            return new AdvisoryResult("Brak wybranej rośliny.", false, false, false);
        }

        List<EppoCode> linkedCodes = eppoCodePlantLinkService.getCodesForPlant(plant.getId());
        List<EppoCode> passportCodes = linkedCodes.stream()
                .filter(EppoCode::isPassportRequired)
                .toList();

        boolean plantRequiresPassport = plant.isPassportRequired();
        boolean linkedCodeRequiresPassport = !passportCodes.isEmpty();
        boolean attention = plantRequiresPassport || linkedCodeRequiresPassport;

        StringBuilder message = new StringBuilder();
        message.append("Roślina: ").append(formatPlantSummary(plant)).append("\n");
        message.append("Wymaga paszportu w kartotece roślin: ")
                .append(plantRequiresPassport ? "tak" : "nie")
                .append("\n");
        message.append("Powiązany kod EPPO z flagą paszportową: ")
                .append(linkedCodeRequiresPassport ? "tak" : "nie");

        String inlineNote = buildInlinePassportNote(plant, linkedCodes);
        if (!inlineNote.isBlank()) {
            message.append("\n").append(inlineNote.trim());
        }

        if (!attention) {
            message.append("\nBrak dodatkowej uwagi paszportowej na podstawie aktualnych danych rośliny i EPPO.");
        }

        return new AdvisoryResult(
                message.toString(),
                attention,
                plantRequiresPassport,
                linkedCodeRequiresPassport
        );
    }

    public String buildInlinePassportNote(Plant plant) {
        if (plant == null) {
            return "";
        }
        return buildInlinePassportNote(plant, eppoCodePlantLinkService.getCodesForPlant(plant.getId()));
    }

    private String buildInlinePassportNote(Plant plant, List<EppoCode> linkedCodes) {
        if (plant == null) {
            return "";
        }

        List<EppoCode> safeCodes = linkedCodes == null ? List.of() : linkedCodes;
        List<EppoCode> passportCodes = safeCodes.stream()
                .filter(EppoCode::isPassportRequired)
                .toList();

        boolean plantRequiresPassport = plant.isPassportRequired();
        boolean linkedCodeRequiresPassport = !passportCodes.isEmpty();

        if (plantRequiresPassport && linkedCodeRequiresPassport) {
            return "Uwaga paszportowa: zarówno karta rośliny, jak i co najmniej jeden powiązany kod EPPO wskazują wymaganie paszportowe.";
        }

        if (plantRequiresPassport) {
            return "Uwaga paszportowa: karta rośliny ma zaznaczone 'Wymaga paszportu'.";
        }

        if (linkedCodeRequiresPassport) {
            String codes = passportCodes.stream()
                    .map(EppoCode::getCode)
                    .map(this::safe)
                    .filter(code -> !code.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));
            if (codes.isBlank()) {
                return "Uwaga paszportowa: powiązane kody EPPO sugerują wymaganie paszportowe.";
            }
            return "Uwaga paszportowa: powiązane kody EPPO sugerują wymaganie paszportowe (" + codes + ").";
        }

        return "";
    }

    private String formatPlantSummary(Plant plant) {
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
            return String.join(" ", parts);
        }

        String latin = safe(plant.getLatinSpeciesName());
        return latin.isBlank() ? ("Roślina ID " + plant.getId()) : latin;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record AdvisoryResult(
            String message,
            boolean attention,
            boolean plantRequiresPassport,
            boolean linkedCodeRequiresPassport
    ) {
    }
}