package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoCodeZoneLink;
import com.egen.fitogen.model.EppoZone;
import com.egen.fitogen.repository.EppoCodeRepository;
import com.egen.fitogen.repository.EppoCodeZoneLinkRepository;
import com.egen.fitogen.repository.EppoZoneRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EppoCodeZoneLinkService {

    private final EppoCodeZoneLinkRepository linkRepository;
    private final EppoCodeRepository eppoCodeRepository;
    private final EppoZoneRepository eppoZoneRepository;
    private final AuditLogService auditLogService;

    public EppoCodeZoneLinkService(
            EppoCodeZoneLinkRepository linkRepository,
            EppoCodeRepository eppoCodeRepository,
            EppoZoneRepository eppoZoneRepository
    ) {
        this(linkRepository, eppoCodeRepository, eppoZoneRepository, null);
    }

    public EppoCodeZoneLinkService(
            EppoCodeZoneLinkRepository linkRepository,
            EppoCodeRepository eppoCodeRepository,
            EppoZoneRepository eppoZoneRepository,
            AuditLogService auditLogService
    ) {
        this.linkRepository = linkRepository;
        this.eppoCodeRepository = eppoCodeRepository;
        this.eppoZoneRepository = eppoZoneRepository;
        this.auditLogService = auditLogService;
    }

    public List<EppoCodeZoneLink> getAll() {
        return linkRepository.findAll();
    }

    public List<EppoCodeZoneLink> getByEppoCodeId(int eppoCodeId) {
        validateEppoCodeExists(eppoCodeId);
        return linkRepository.findByEppoCodeId(eppoCodeId);
    }

    public List<EppoCodeZoneLink> getByEppoZoneId(int eppoZoneId) {
        validateEppoZoneExists(eppoZoneId);
        return linkRepository.findByEppoZoneId(eppoZoneId);
    }

    public List<EppoZone> getZonesForCode(int eppoCodeId) {
        validateEppoCodeExists(eppoCodeId);

        return linkRepository.findByEppoCodeId(eppoCodeId).stream()
                .map(link -> eppoZoneRepository.findById(link.getEppoZoneId()))
                .filter(zone -> zone != null)
                .toList();
    }

    public List<EppoCode> getCodesForZone(int eppoZoneId) {
        validateEppoZoneExists(eppoZoneId);

        return linkRepository.findByEppoZoneId(eppoZoneId).stream()
                .map(link -> eppoCodeRepository.findById(link.getEppoCodeId()))
                .filter(code -> code != null)
                .toList();
    }

    public void addLink(int eppoCodeId, int eppoZoneId) {
        validateIds(eppoCodeId, eppoZoneId);

        if (linkRepository.exists(eppoCodeId, eppoZoneId)) {
            throw new IllegalArgumentException("Takie powiązanie kodu EPPO ze strefą już istnieje.");
        }

        linkRepository.save(new EppoCodeZoneLink(0, eppoCodeId, eppoZoneId));
        log("UPDATE", eppoCodeId, "Dodano przypisanie strefy do kodu EPPO " + describeCode(eppoCodeId)
                + ": " + describeZone(eppoZoneId));
    }

    public void deleteLink(int eppoCodeId, int eppoZoneId) {
        validateIds(eppoCodeId, eppoZoneId);
        linkRepository.deleteByPair(eppoCodeId, eppoZoneId);
        log("DELETE", eppoCodeId, "Usunięto przypisanie strefy z kodu EPPO " + describeCode(eppoCodeId)
                + ": " + describeZone(eppoZoneId));
    }

    public void deleteAllForZone(int eppoZoneId) {
        validateEppoZoneExists(eppoZoneId);
        List<EppoCodeZoneLink> before = linkRepository.findByEppoZoneId(eppoZoneId);
        linkRepository.deleteByEppoZoneId(eppoZoneId);

        if (!before.isEmpty()) {
            log("DELETE", null, "Usunięto wszystkie przypisania strefy EPPO " + describeZone(eppoZoneId)
                    + " z kodów: " + describeCodesForZoneLinks(before));
        }
    }

    public void replaceZonesForCode(int eppoCodeId, List<Integer> zoneIds) {
        validateEppoCodeExists(eppoCodeId);

        String before = describeZoneSet(linkRepository.findByEppoCodeId(eppoCodeId));

        Set<Integer> normalizedZoneIds = normalizeZoneIds(zoneIds);
        for (Integer zoneId : normalizedZoneIds) {
            validateEppoZoneExists(zoneId);
        }

        linkRepository.deleteByEppoCodeId(eppoCodeId);

        for (Integer zoneId : normalizedZoneIds) {
            linkRepository.save(new EppoCodeZoneLink(0, eppoCodeId, zoneId));
        }

        String after = describeZoneSet(linkRepository.findByEppoCodeId(eppoCodeId));
        if (!before.equals(after)) {
            log("UPDATE", eppoCodeId, "Zaktualizowano przypisania stref dla kodu EPPO "
                    + describeCode(eppoCodeId) + " z " + before + " na " + after);
        }
    }

    private void validateIds(int eppoCodeId, int eppoZoneId) {
        validateEppoCodeExists(eppoCodeId);
        validateEppoZoneExists(eppoZoneId);
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

    private void validateEppoZoneExists(int eppoZoneId) {
        if (eppoZoneId <= 0) {
            throw new IllegalArgumentException("Nieprawidłowa strefa EPPO.");
        }

        EppoZone zone = eppoZoneRepository.findById(eppoZoneId);
        if (zone == null) {
            throw new IllegalArgumentException("Wybrana strefa EPPO nie istnieje.");
        }
    }

    private void log(String actionType, Integer entityId, String description) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.log("EPPO_ZONE_LINK", entityId, actionType, description);
    }

    private String describeCode(int eppoCodeId) {
        EppoCode code = eppoCodeRepository.findById(eppoCodeId);
        if (code == null || code.getCode() == null || code.getCode().isBlank()) {
            return "[brak kodu EPPO]";
        }
        return code.getCode().trim();
    }

    private String describeZone(int eppoZoneId) {
        EppoZone zone = eppoZoneRepository.findById(eppoZoneId);
        if (zone == null) {
            return "[brak strefy EPPO]";
        }

        String code = zone.getCode() == null ? "" : zone.getCode().trim();
        String name = zone.getName() == null ? "" : zone.getName().trim();
        if (code.isBlank() && name.isBlank()) {
            return "[brak strefy EPPO]";
        }
        if (code.isBlank()) {
            return name;
        }
        if (name.isBlank()) {
            return code;
        }
        return code + " / " + name;
    }

    private String describeZoneSet(List<EppoCodeZoneLink> links) {
        if (links == null || links.isEmpty()) {
            return "[brak przypisań stref]";
        }

        String joined = links.stream()
                .map(EppoCodeZoneLink::getEppoZoneId)
                .map(this::describeZone)
                .distinct()
                .collect(Collectors.joining(", "));

        return joined.isBlank() ? "[brak przypisań stref]" : joined;
    }

    private String describeCodesForZoneLinks(List<EppoCodeZoneLink> links) {
        if (links == null || links.isEmpty()) {
            return "[brak kodów EPPO]";
        }

        String joined = links.stream()
                .map(EppoCodeZoneLink::getEppoCodeId)
                .map(this::describeCode)
                .distinct()
                .collect(Collectors.joining(", "));

        return joined.isBlank() ? "[brak kodów EPPO]" : joined;
    }

    private Set<Integer> normalizeZoneIds(List<Integer> zoneIds) {
        Set<Integer> normalized = new LinkedHashSet<>();

        if (zoneIds == null) {
            return normalized;
        }

        for (Integer zoneId : zoneIds) {
            if (zoneId != null && zoneId > 0) {
                normalized.add(zoneId);
            }
        }

        return normalized;
    }
}
