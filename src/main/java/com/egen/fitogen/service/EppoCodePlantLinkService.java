package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoCodePlantLink;
import com.egen.fitogen.model.EppoCodeSpeciesLink;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.repository.EppoCodePlantLinkRepository;
import com.egen.fitogen.repository.EppoCodeRepository;
import com.egen.fitogen.repository.PlantRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EppoCodePlantLinkService {

    private final EppoCodePlantLinkRepository linkRepository;
    private final EppoCodeRepository eppoCodeRepository;
    private final PlantRepository plantRepository;
    private final EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService;

    public EppoCodePlantLinkService(
            EppoCodePlantLinkRepository linkRepository,
            EppoCodeRepository eppoCodeRepository,
            PlantRepository plantRepository,
            EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService
    ) {
        this.linkRepository = linkRepository;
        this.eppoCodeRepository = eppoCodeRepository;
        this.plantRepository = plantRepository;
        this.eppoCodeSpeciesLinkService = eppoCodeSpeciesLinkService;
    }

    public List<EppoCodePlantLink> getAll() {
        return linkRepository.findAll();
    }

    public List<EppoCodePlantLink> getByEppoCodeId(int eppoCodeId) {
        validateEppoCodeExists(eppoCodeId);
        return linkRepository.findByEppoCodeId(eppoCodeId);
    }

    public List<EppoCodePlantLink> getByPlantId(int plantId) {
        validatePlantExists(plantId);
        return linkRepository.findByPlantId(plantId);
    }

    public List<Plant> getPlantsForCode(int eppoCodeId) {
        validateEppoCodeExists(eppoCodeId);

        List<EppoCodeSpeciesLink> speciesLinks = eppoCodeSpeciesLinkService.getEffectiveSpeciesLinks(eppoCodeId);
        if (!speciesLinks.isEmpty()) {
            return plantRepository.findAll().stream()
                    .filter(plant -> matchesAnySpeciesLink(speciesLinks, plant))
                    .toList();
        }

        List<Plant> legacyPlants = new ArrayList<>();
        for (EppoCodePlantLink link : linkRepository.findByEppoCodeId(eppoCodeId)) {
            Plant plant = plantRepository.findById(link.getPlantId());
            if (plant != null) {
                legacyPlants.add(plant);
            }
        }
        return legacyPlants;
    }

    public List<EppoCode> getCodesForPlant(int plantId) {
        validatePlantExists(plantId);

        Plant plant = plantRepository.findById(plantId);
        List<EppoCode> automaticallyMatched = findCodesMatchedAutomatically(plant);
        if (!automaticallyMatched.isEmpty()) {
            return automaticallyMatched;
        }

        Set<Integer> resolvedIds = new LinkedHashSet<>();
        List<EppoCode> resolvedCodes = new ArrayList<>();

        for (EppoCodePlantLink link : linkRepository.findByPlantId(plantId)) {
            EppoCode code = eppoCodeRepository.findById(link.getEppoCodeId());
            if (code != null && resolvedIds.add(code.getId())) {
                resolvedCodes.add(code);
            }
        }

        String plantEppoCode = normalizeCode(plant.getEppoCode());
        if (!plantEppoCode.isBlank()) {
            EppoCode directCode = eppoCodeRepository.findByCode(plantEppoCode);
            if (directCode != null && resolvedIds.add(directCode.getId())) {
                resolvedCodes.add(directCode);
            }
        }

        return resolvedCodes;
    }

    public void addLink(int eppoCodeId, int plantId) {
        validateIds(eppoCodeId, plantId);

        if (linkRepository.exists(eppoCodeId, plantId)) {
            throw new IllegalArgumentException("Takie powiązanie kodu EPPO z rośliną już istnieje.");
        }

        linkRepository.save(new EppoCodePlantLink(0, eppoCodeId, plantId));
    }

    public void deleteLink(int eppoCodeId, int plantId) {
        validateIds(eppoCodeId, plantId);
        linkRepository.deleteByPair(eppoCodeId, plantId);
    }

    public void deleteAllForCode(int eppoCodeId) {
        validateEppoCodeExists(eppoCodeId);
        linkRepository.deleteByEppoCodeId(eppoCodeId);
    }

    public void deleteAllForPlant(int plantId) {
        validatePlantExists(plantId);
        linkRepository.deleteByPlantId(plantId);
    }

    public void replacePlantsForCode(int eppoCodeId, List<Integer> plantIds) {
        validateEppoCodeExists(eppoCodeId);

        Set<Integer> normalizedPlantIds = normalizePlantIds(plantIds);
        for (Integer plantId : normalizedPlantIds) {
            validatePlantExists(plantId);
        }

        linkRepository.deleteByEppoCodeId(eppoCodeId);

        for (Integer plantId : normalizedPlantIds) {
            linkRepository.save(new EppoCodePlantLink(0, eppoCodeId, plantId));
        }
    }

    private List<EppoCode> findCodesMatchedAutomatically(Plant plant) {
        if (plant == null) {
            return List.of();
        }

        String plantSpecies = normalizeText(plant.getSpecies());
        String plantLatinSpeciesName = normalizeText(plant.getLatinSpeciesName());

        if (plantSpecies.isBlank() && plantLatinSpeciesName.isBlank()) {
            return List.of();
        }

        return eppoCodeRepository.findAll().stream()
                .filter(code -> matchesAnySpeciesLink(eppoCodeSpeciesLinkService.getEffectiveSpeciesLinks(code.getId()), plant))
                .toList();
    }

    private boolean matchesAnySpeciesLink(List<EppoCodeSpeciesLink> speciesLinks, Plant plant) {
        if (speciesLinks == null || speciesLinks.isEmpty() || plant == null) {
            return false;
        }

        for (EppoCodeSpeciesLink speciesLink : speciesLinks) {
            if (matchesPlant(speciesLink, plant)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesPlant(EppoCodeSpeciesLink speciesLink, Plant plant) {
        if (speciesLink == null || plant == null) {
            return false;
        }

        String codeSpecies = normalizeText(speciesLink.getSpeciesName());
        String codeLatin = normalizeText(speciesLink.getLatinSpeciesName());
        String plantSpecies = normalizeText(plant.getSpecies());
        String plantLatin = normalizeText(plant.getLatinSpeciesName());

        boolean speciesMatch = !codeSpecies.isBlank() && !plantSpecies.isBlank() && codeSpecies.equalsIgnoreCase(plantSpecies);
        boolean latinMatch = !codeLatin.isBlank() && !plantLatin.isBlank() && codeLatin.equalsIgnoreCase(plantLatin);

        return speciesMatch || latinMatch;
    }

    private void validateIds(int eppoCodeId, int plantId) {
        validateEppoCodeExists(eppoCodeId);
        validatePlantExists(plantId);
    }

    private void validateEppoCodeExists(int eppoCodeId) {
        if (eppoCodeId <= 0) {
            throw new IllegalArgumentException("Nieprawidłowy kod EPPO.");
        }

        EppoCode code = eppoCodeRepository.findById(eppoCodeId);
        if (code == null) {
            throw new IllegalArgumentException("Wybrany kod EPPO nie istnieje.");
        }
    }

    private void validatePlantExists(int plantId) {
        if (plantId <= 0) {
            throw new IllegalArgumentException("Nieprawidłowa roślina.");
        }

        Plant plant = plantRepository.findById(plantId);
        if (plant == null) {
            throw new IllegalArgumentException("Wybrana roślina nie istnieje.");
        }
    }

    private Set<Integer> normalizePlantIds(List<Integer> plantIds) {
        Set<Integer> normalized = new LinkedHashSet<>();

        if (plantIds == null) {
            return normalized;
        }

        for (Integer plantId : plantIds) {
            if (plantId != null && plantId > 0) {
                normalized.add(plantId);
            }
        }

        return normalized;
    }

    private String normalizeText(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value == null) {
                continue;
            }

            String normalized = value.trim().replaceAll("\\s+", " ");
            if (!normalized.isBlank()) {
                return normalized;
            }
        }

        return "";
    }

    private String normalizeCode(String value) {
        String normalized = normalizeText(value);
        return normalized.toUpperCase();
    }
}
