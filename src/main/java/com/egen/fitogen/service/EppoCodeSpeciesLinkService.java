package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoCodeSpeciesLink;
import com.egen.fitogen.repository.EppoCodeRepository;
import com.egen.fitogen.repository.EppoCodeSpeciesLinkRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EppoCodeSpeciesLinkService {

    private final EppoCodeSpeciesLinkRepository repository;
    private final EppoCodeRepository eppoCodeRepository;

    public EppoCodeSpeciesLinkService(
            EppoCodeSpeciesLinkRepository repository,
            EppoCodeRepository eppoCodeRepository
    ) {
        this.repository = repository;
        this.eppoCodeRepository = eppoCodeRepository;
    }

    public List<EppoCodeSpeciesLink> getAll() {
        return repository.findAll();
    }

    public List<EppoCodeSpeciesLink> getByEppoCodeId(int eppoCodeId) {
        validateEppoCodeExists(eppoCodeId);
        return repository.findByEppoCodeId(eppoCodeId);
    }

    public List<EppoCodeSpeciesLink> getEffectiveSpeciesLinks(int eppoCodeId) {
        validateEppoCodeExists(eppoCodeId);

        Set<String> signatures = new LinkedHashSet<>();
        List<EppoCodeSpeciesLink> repositoryLinks = repository.findByEppoCodeId(eppoCodeId);
        List<EppoCodeSpeciesLink> effectiveLinks = new java.util.ArrayList<>();

        for (EppoCodeSpeciesLink link : repositoryLinks) {
            String signature = signature(link.getSpeciesName(), link.getLatinSpeciesName());
            if (signatures.add(signature)) {
                effectiveLinks.add(normalizeLink(link));
            }
        }

        EppoCode code = eppoCodeRepository.findById(eppoCodeId);
        if (code != null) {
            EppoCodeSpeciesLink legacyLink = new EppoCodeSpeciesLink(
                    0,
                    eppoCodeId,
                    firstNonBlank(code.getSpeciesName(), code.getCommonName()),
                    firstNonBlank(code.getLatinSpeciesName(), code.getScientificName())
            );
            legacyLink = normalizeLink(legacyLink);
            if (hasAnySpeciesValue(legacyLink)) {
                String signature = signature(legacyLink.getSpeciesName(), legacyLink.getLatinSpeciesName());
                if (signatures.add(signature)) {
                    effectiveLinks.add(legacyLink);
                }
            }
        }

        return effectiveLinks;
    }

    public void addLink(int eppoCodeId, String speciesName, String latinSpeciesName) {
        validateEppoCodeExists(eppoCodeId);

        EppoCodeSpeciesLink link = normalizeLink(new EppoCodeSpeciesLink(0, eppoCodeId, speciesName, latinSpeciesName));
        validateSpeciesLink(link);

        if (repository.exists(eppoCodeId, link.getSpeciesName(), link.getLatinSpeciesName())) {
            throw new IllegalArgumentException("Takie powiązanie kodu EPPO z gatunkiem już istnieje.");
        }

        repository.save(link);
    }

    public void replaceSpeciesForCode(int eppoCodeId, List<EppoCodeSpeciesLink> speciesLinks) {
        validateEppoCodeExists(eppoCodeId);

        repository.deleteByEppoCodeId(eppoCodeId);

        if (speciesLinks == null || speciesLinks.isEmpty()) {
            return;
        }

        Set<String> signatures = new LinkedHashSet<>();
        for (EppoCodeSpeciesLink rawLink : speciesLinks) {
            if (rawLink == null) {
                continue;
            }

            EppoCodeSpeciesLink link = normalizeLink(new EppoCodeSpeciesLink(
                    0,
                    eppoCodeId,
                    rawLink.getSpeciesName(),
                    rawLink.getLatinSpeciesName()
            ));
            validateSpeciesLink(link);

            String signature = signature(link.getSpeciesName(), link.getLatinSpeciesName());
            if (signatures.add(signature)) {
                repository.save(link);
            }
        }
    }

    public void deleteById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Nieprawidłowe przypisanie gatunku EPPO.");
        }
        repository.deleteById(id);
    }

    public void deleteAllForCode(int eppoCodeId) {
        validateEppoCodeExists(eppoCodeId);
        repository.deleteByEppoCodeId(eppoCodeId);
    }

    private void validateSpeciesLink(EppoCodeSpeciesLink link) {
        if (!hasAnySpeciesValue(link)) {
            throw new IllegalArgumentException("Gatunek albo łacińska nazwa gatunku jest wymagana.");
        }
    }

    private boolean hasAnySpeciesValue(EppoCodeSpeciesLink link) {
        return link != null
                && ((link.getSpeciesName() != null && !link.getSpeciesName().isBlank())
                || (link.getLatinSpeciesName() != null && !link.getLatinSpeciesName().isBlank()));
    }

    private EppoCodeSpeciesLink normalizeLink(EppoCodeSpeciesLink link) {
        link.setSpeciesName(normalizeText(link.getSpeciesName()));
        link.setLatinSpeciesName(normalizeText(link.getLatinSpeciesName()));
        return link;
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

    private String signature(String speciesName, String latinSpeciesName) {
        return normalizeText(speciesName) + "||" + normalizeText(latinSpeciesName);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
