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
        logChange("Plant", plant.getId(), "CREATE", "Dodano roślinę: " + describePlant(plant));
    }

    public void updatePlant(Plant plant) {
        Plant beforeUpdate = repository.findById(plant.getId());
        validate(plant);
        repository.update(plant);
        logChange("Plant", plant.getId(), "UPDATE",
                "Zaktualizowano roślinę: " + describePlant(plant)
                        + buildBeforeAfterSuffix(describePlant(beforeUpdate), describePlant(plant)));
    }

    public void deletePlant(int id) {
        Plant plant = repository.findById(id);
        repository.delete(id);
        logChange("Plant", id, "DELETE", "Usunięto roślinę: " + describePlant(plant));
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

    private void logChange(String entityType, Integer entityId, String actionType, String description) {
        if (auditLogService != null) {
            auditLogService.log(entityType, entityId, actionType, description);
        }
    }

    private String describePlant(Plant plant) {
        if (plant == null) {
            return "[brak danych]";
        }

        String label = plant.toString();
        if (label == null || label.isBlank()) {
            label = "ID=" + plant.getId();
        }

        String visibility = normalizeText(plant.getVisibilityStatus());
        if (visibility == null || visibility.isBlank()) {
            return label;
        }
        return label + " [status widoczności: " + visibility + "]";
    }

    private String buildBeforeAfterSuffix(String before, String after) {
        if (before == null || before.isBlank() || before.equals(after)) {
            return "";
        }
        return " (wcześniej: " + before + ")";
    }
}