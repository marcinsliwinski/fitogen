package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoCodeSpeciesLink;
import com.egen.fitogen.repository.EppoCodeRepository;
import com.egen.fitogen.repository.EppoCodeSpeciesLinkRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EppoCodeSpeciesLinkService {

    private final EppoCodeSpeciesLinkRepository repository;
    private final EppoCodeRepository eppoCodeRepository;
    private final AuditLogService auditLogService;

    public EppoCodeSpeciesLinkService(
            EppoCodeSpeciesLinkRepository repository,
            EppoCodeRepository eppoCodeRepository
    ) {
        this(repository, eppoCodeRepository, null);
    }

    public EppoCodeSpeciesLinkService(
            EppoCodeSpeciesLinkRepository repository,
            EppoCodeRepository eppoCodeRepository,
            AuditLogService auditLogService
    ) {
        this.repository = repository;
        this.eppoCodeRepository = eppoCodeRepository;
        this.auditLogService = auditLogService;
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
        log("UPDATE", eppoCodeId, "Dodano przypisanie gatunku do kodu EPPO " + describeCode(eppoCodeId)
                + ": " + describeLink(link));
    }

    public void replaceSpeciesForCode(int eppoCodeId, List<EppoCodeSpeciesLink> speciesLinks) {
        validateEppoCodeExists(eppoCodeId);

        String before = describeLinkSet(repository.findByEppoCodeId(eppoCodeId));

        repository.deleteByEppoCodeId(eppoCodeId);

        if (speciesLinks == null || speciesLinks.isEmpty()) {
            if (!before.equals("[brak przypisań gatunków]")) {
                log("UPDATE", eppoCodeId, "Zaktualizowano przypisania gatunków dla kodu EPPO "
                        + describeCode(eppoCodeId) + " z " + before + " na [brak przypisań gatunków]");
            }
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

        String after = describeLinkSet(repository.findByEppoCodeId(eppoCodeId));
        if (!before.equals(after)) {
            log("UPDATE", eppoCodeId, "Zaktualizowano przypisania gatunków dla kodu EPPO "
                    + describeCode(eppoCodeId) + " z " + before + " na " + after);
        }
    }

    public void deleteById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Nieprawidłowe przypisanie gatunku EPPO.");
        }

        EppoCodeSpeciesLink existing = repository.findAll().stream()
                .filter(link -> link.getId() == id)
                .findFirst()
                .orElse(null);
        repository.deleteById(id);

        Integer entityId = existing == null ? null : existing.getEppoCodeId();
        log("DELETE", entityId, "Usunięto przypisanie gatunku EPPO dla kodu " + describeCode(entityId)
                + ": " + describeLink(existing));
    }

    public void deleteAllForCode(int eppoCodeId) {
        validateEppoCodeExists(eppoCodeId);
        String before = describeLinkSet(repository.findByEppoCodeId(eppoCodeId));
        repository.deleteByEppoCodeId(eppoCodeId);
        if (!before.equals("[brak przypisań gatunków]")) {
            log("DELETE", eppoCodeId, "Usunięto wszystkie przypisania gatunków dla kodu EPPO "
                    + describeCode(eppoCodeId) + ": " + before);
        }
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

    private void log(String actionType, Integer entityId, String description) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.log("EPPO_SPECIES_LINK", entityId, actionType, description);
    }

    private String describeCode(Integer eppoCodeId) {
        if (eppoCodeId == null || eppoCodeId <= 0) {
            return "[brak kodu EPPO]";
        }

        EppoCode code = eppoCodeRepository.findById(eppoCodeId);
        if (code == null) {
            return "[brak kodu EPPO]";
        }

        String codeValue = normalizeText(code.getCode());
        return codeValue == null || codeValue.isBlank() ? "[brak kodu EPPO]" : codeValue;
    }

    private String describeLink(EppoCodeSpeciesLink link) {
        if (link == null) {
            return "[brak przypisania gatunku]";
        }

        String species = normalizeText(link.getSpeciesName());
        String latin = normalizeText(link.getLatinSpeciesName());
        if (species.isBlank() && latin.isBlank()) {
            return "[brak przypisania gatunku]";
        }
        if (species.isBlank()) {
            return latin;
        }
        if (latin.isBlank()) {
            return species;
        }
        return species + " / " + latin;
    }

    private String describeLinkSet(List<EppoCodeSpeciesLink> links) {
        if (links == null || links.isEmpty()) {
            return "[brak przypisań gatunków]";
        }

        String joined = links.stream()
                .map(this::normalizeLink)
                .map(this::describeLink)
                .distinct()
                .collect(Collectors.joining(", "));

        return joined.isBlank() ? "[brak przypisań gatunków]" : joined;
    }

    private String signature(String speciesName, String latinSpeciesName) {
        return normalizeText(speciesName) + "||" + normalizeText(latinSpeciesName);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().replaceAll("\s+", " ");
        return normalized;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
