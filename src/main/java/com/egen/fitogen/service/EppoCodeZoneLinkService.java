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

public class EppoCodeZoneLinkService {

    private final EppoCodeZoneLinkRepository linkRepository;
    private final EppoCodeRepository eppoCodeRepository;
    private final EppoZoneRepository eppoZoneRepository;

    public EppoCodeZoneLinkService(
            EppoCodeZoneLinkRepository linkRepository,
            EppoCodeRepository eppoCodeRepository,
            EppoZoneRepository eppoZoneRepository
    ) {
        this.linkRepository = linkRepository;
        this.eppoCodeRepository = eppoCodeRepository;
        this.eppoZoneRepository = eppoZoneRepository;
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
    }

    public void deleteLink(int eppoCodeId, int eppoZoneId) {
        validateIds(eppoCodeId, eppoZoneId);
        linkRepository.deleteByPair(eppoCodeId, eppoZoneId);
    }

    public void deleteAllForZone(int eppoZoneId) {
        validateEppoZoneExists(eppoZoneId);
        linkRepository.deleteByEppoZoneId(eppoZoneId);
    }

    public void replaceZonesForCode(int eppoCodeId, List<Integer> zoneIds) {
        validateEppoCodeExists(eppoCodeId);

        Set<Integer> normalizedZoneIds = normalizeZoneIds(zoneIds);
        for (Integer zoneId : normalizedZoneIds) {
            validateEppoZoneExists(zoneId);
        }

        linkRepository.deleteByEppoCodeId(eppoCodeId);

        for (Integer zoneId : normalizedZoneIds) {
            linkRepository.save(new EppoCodeZoneLink(0, eppoCodeId, zoneId));
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