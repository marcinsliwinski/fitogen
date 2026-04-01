package com.egen.fitogen.service;

import com.egen.fitogen.model.Plant;
import com.egen.fitogen.repository.PlantRepository;

import java.util.List;

public class PlantService {

    private final PlantRepository repository;
    private final AppSettingsService appSettingsService;
    private final AuditLogService auditLogService;

    public PlantService(PlantRepository repository, AppSettingsService appSettingsService) {
        this(repository, appSettingsService, null);
    }

    public PlantService(PlantRepository repository, AppSettingsService appSettingsService, AuditLogService auditLogService) {
        this.repository = repository;
        this.appSettingsService = appSettingsService;
        this.auditLogService = auditLogService;
    }

    public List<Plant> getAllPlants() {
        return repository.findAll();
    }

    public void addPlant(Plant plant) {
        validate(plant);
        repository.save(plant);
        log("CREATE", null, "Dodano roślinę: " + describe(plant));
    }

    public void updatePlant(Plant plant) {
        validate(plant);
        Plant existing = repository.findById(plant.getId());
        repository.update(plant);
        log(
                "UPDATE",
                plant.getId(),
                "Zaktualizowano roślinę z " + describe(existing) + " na " + describe(plant)
        );
    }

    public void deletePlant(int id) {
        Plant existing = repository.findById(id);
        repository.delete(id);
        log("DELETE", id, "Usunięto roślinę: " + describe(existing));
    }

    public List<String> getSpeciesSuggestions() {
        return repository.findAll().stream()
                .map(Plant::getSpecies)
                .filter(this::notBlank)
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> getVarietySuggestions() {
        return repository.findAll().stream()
                .map(Plant::getVariety)
                .filter(this::notBlank)
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> getRootstockSuggestions() {
        return repository.findAll().stream()
                .map(Plant::getRootstock)
                .filter(this::notBlank)
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> getLatinSpeciesNameSuggestions() {
        return repository.findAll().stream()
                .map(Plant::getLatinSpeciesName)
                .filter(this::notBlank)
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private void validate(Plant plant) {
        if (plant == null) {
            throw new IllegalArgumentException("Roślina nie może być pusta.");
        }

        if (plant.getSpecies() == null || plant.getSpecies().isBlank()) {
            throw new IllegalArgumentException("Plant species required");
        }

        normalizePlant(plant);

        if (appSettingsService.isPlantFullCatalogEnabled() && hasDuplicateIdentity(plant)) {
            throw new IllegalArgumentException(
                    "Taka roślina już istnieje w pełnej bazie. Duplikat jest blokowany dla kombinacji: Gatunek + Odmiana + Podkładka."
            );
        }
    }

    private void normalizePlant(Plant plant) {
        plant.setSpecies(normalizeText(plant.getSpecies()));
        plant.setVariety(normalizeText(plant.getVariety()));
        plant.setRootstock(normalizeText(plant.getRootstock()));
        plant.setLatinSpeciesName(normalizeText(plant.getLatinSpeciesName()));
        plant.setVisibilityStatus(normalizeText(plant.getVisibilityStatus()));

        if (appSettingsService.isPlantPassportRequiredForAll()) {
            plant.setPassportRequired(true);
        }

        if (plant.getEppoCode() != null) {
            plant.setEppoCode(normalizeText(plant.getEppoCode()).toUpperCase());
        }
    }

    private boolean hasDuplicateIdentity(Plant candidate) {
        return repository.existsByIdentityExcludingId(
                candidate.getSpecies(),
                candidate.getVariety(),
                candidate.getRootstock(),
                candidate.getId()
        );
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? "" : normalized;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isBlank();
    }

    private void log(String actionType, Integer entityId, String description) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.log("PLANT", entityId, actionType, description);
    }

    private String describe(Plant plant) {
        if (plant == null) {
            return "[brak rośliny]";
        }

        StringBuilder sb = new StringBuilder();
        appendPart(sb, plant.getSpecies());
        appendPart(sb, plant.getRootstock());
        appendPart(sb, plant.getVariety());

        String eppoCode = normalizeText(plant.getEppoCode());
        if (eppoCode != null && !eppoCode.isBlank()) {
            sb.append(" [EPPO: ").append(eppoCode).append("]");
        }

        String visibilityStatus = normalizeText(plant.getVisibilityStatus());
        if (visibilityStatus != null && !visibilityStatus.isBlank()) {
            sb.append(" [widoczność: ").append(visibilityStatus).append("]");
        }

        sb.append(plant.isPassportRequired() ? " [paszport: wymagany]" : " [paszport: opcjonalny]");

        String description = sb.toString().trim();
        return description.isBlank() ? "[brak rośliny]" : description;
    }

    private void appendPart(StringBuilder sb, String value) {
        String normalized = normalizeText(value);
        if (normalized == null || normalized.isBlank()) {
            return;
        }

        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(normalized);
    }
}
