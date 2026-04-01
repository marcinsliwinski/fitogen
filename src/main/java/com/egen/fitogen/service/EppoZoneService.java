package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoZone;
import com.egen.fitogen.repository.EppoZoneRepository;

import java.util.List;

public class EppoZoneService {

    private final EppoZoneRepository repository;
    private final AuditLogService auditLogService;

    public EppoZoneService(EppoZoneRepository repository) {
        this(repository, null);
    }

    public EppoZoneService(EppoZoneRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    public List<EppoZone> getAll() {
        return repository.findAll();
    }

    public EppoZone getById(int id) {
        return repository.findById(id);
    }

    public EppoZone getByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return repository.findByCode(normalizeCode(code));
    }

    public void save(EppoZone eppoZone) {
        validate(eppoZone);

        if (eppoZone.getId() > 0) {
            EppoZone existing = repository.findById(eppoZone.getId());
            repository.update(eppoZone);
            log("UPDATE", eppoZone.getId(), "Zaktualizowano strefę EPPO z " + describe(existing) + " na " + describe(eppoZone));
        } else {
            repository.save(eppoZone);
            EppoZone persisted = repository.findByCode(eppoZone.getCode());
            Integer entityId = persisted == null ? null : persisted.getId();
            log("CREATE", entityId, "Dodano strefę EPPO: " + describe(persisted == null ? eppoZone : persisted));
        }
    }

    public void delete(int id) {
        EppoZone existing = repository.findById(id);
        repository.deleteById(id);
        log("DELETE", id, "Usunięto strefę EPPO: " + describe(existing));
    }

    private void validate(EppoZone eppoZone) {
        if (eppoZone == null) {
            throw new IllegalArgumentException("Strefa EPPO nie może być pusta.");
        }

        normalize(eppoZone);

        if (eppoZone.getCode() == null || eppoZone.getCode().isBlank()) {
            throw new IllegalArgumentException("Kod strefy EPPO jest wymagany.");
        }

        if (eppoZone.getName() == null || eppoZone.getName().isBlank()) {
            throw new IllegalArgumentException("Nazwa strefy EPPO jest wymagana.");
        }

        EppoZone existing = repository.findByCode(eppoZone.getCode());
        if (existing != null && existing.getId() != eppoZone.getId()) {
            throw new IllegalArgumentException("Taki kod strefy EPPO już istnieje.");
        }
    }

    private void normalize(EppoZone eppoZone) {
        eppoZone.setCode(normalizeCode(eppoZone.getCode()));
        eppoZone.setName(normalizeText(eppoZone.getName()));
        eppoZone.setCountryCode(normalizeCountryCode(eppoZone.getCountryCode()));
        eppoZone.setStatus(normalizeStatus(eppoZone.getStatus()));
    }

    private void log(String actionType, Integer entityId, String description) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.log("EPPO_ZONE", entityId, actionType, description);
    }

    private String describe(EppoZone eppoZone) {
        if (eppoZone == null) {
            return "[brak strefy EPPO]";
        }

        String code = normalizeText(eppoZone.getCode());
        String name = normalizeText(eppoZone.getName());
        String countryCode = normalizeText(eppoZone.getCountryCode());
        String status = normalizeText(eppoZone.getStatus());

        StringBuilder sb = new StringBuilder();
        if (code != null && !code.isBlank()) {
            sb.append(code);
        }
        if (name != null && !name.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append(name);
        }
        if (countryCode != null && !countryCode.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append("[").append(countryCode).append("]");
        }
        if (status != null && !status.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append("[").append(status).append("]");
        }

        String description = sb.toString().trim();
        return description.isBlank() ? "[brak strefy EPPO]" : description;
    }

    private String normalizeCode(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String normalizeCountryCode(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String normalizeStatus(String value) {
        String normalized = normalizeText(value);
        return (normalized == null || normalized.isBlank()) ? "ACTIVE" : normalized.toUpperCase();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\s+", " ");
        return normalized.isEmpty() ? "" : normalized;
    }
}
