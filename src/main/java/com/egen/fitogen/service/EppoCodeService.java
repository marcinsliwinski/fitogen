package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.repository.EppoCodeRepository;

import java.util.List;

public class EppoCodeService {

    private final EppoCodeRepository repository;
    private final AuditLogService auditLogService;

    public EppoCodeService(EppoCodeRepository repository) {
        this(repository, null);
    }

    public EppoCodeService(EppoCodeRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    public List<EppoCode> getAll() {
        return repository.findAll();
    }

    public EppoCode getById(int id) {
        return repository.findById(id);
    }

    public EppoCode getByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return repository.findByCode(normalizeCode(code));
    }

    public void save(EppoCode eppoCode) {
        validate(eppoCode);

        if (eppoCode.getId() > 0) {
            EppoCode existing = repository.findById(eppoCode.getId());
            repository.update(eppoCode);
            log("UPDATE", eppoCode.getId(), "Zaktualizowano kod EPPO z " + describe(existing) + " na " + describe(eppoCode));
            return;
        }

        repository.save(eppoCode);
        EppoCode persisted = repository.findByCode(eppoCode.getCode());
        Integer entityId = persisted == null ? null : persisted.getId();
        log("CREATE", entityId, "Dodano kod EPPO: " + describe(persisted == null ? eppoCode : persisted));
    }

    public void delete(int id) {
        EppoCode existing = repository.findById(id);
        repository.deleteById(id);
        log("DELETE", id, "Usunięto kod EPPO: " + describe(existing));
    }

    private void validate(EppoCode eppoCode) {
        if (eppoCode == null) {
            throw new IllegalArgumentException("Kod EPPO nie może być pusty.");
        }

        normalize(eppoCode);

        if (eppoCode.getCode() == null || eppoCode.getCode().isBlank()) {
            throw new IllegalArgumentException("Kod EPPO jest wymagany.");
        }


        EppoCode existing = repository.findByCode(eppoCode.getCode());
        if (existing != null && existing.getId() != eppoCode.getId()) {
            throw new IllegalArgumentException("Taki kod EPPO już istnieje.");
        }
    }

    private void normalize(EppoCode eppoCode) {
        eppoCode.setCode(normalizeCode(eppoCode.getCode()));
        eppoCode.setSpeciesName(normalizeText(eppoCode.getSpeciesName()));
        eppoCode.setLatinSpeciesName(normalizeText(eppoCode.getLatinSpeciesName()));
        eppoCode.setScientificName(normalizeText(eppoCode.getScientificName()));
        eppoCode.setCommonName(normalizeText(eppoCode.getCommonName()));

        if ((eppoCode.getSpeciesName() == null || eppoCode.getSpeciesName().isBlank())
                && eppoCode.getCommonName() != null && !eppoCode.getCommonName().isBlank()) {
            eppoCode.setSpeciesName(eppoCode.getCommonName());
        }

        if ((eppoCode.getLatinSpeciesName() == null || eppoCode.getLatinSpeciesName().isBlank())
                && eppoCode.getScientificName() != null && !eppoCode.getScientificName().isBlank()) {
            eppoCode.setLatinSpeciesName(eppoCode.getScientificName());
        }

        if ((eppoCode.getCommonName() == null || eppoCode.getCommonName().isBlank())
                && eppoCode.getSpeciesName() != null && !eppoCode.getSpeciesName().isBlank()) {
            eppoCode.setCommonName(eppoCode.getSpeciesName());
        }

        if ((eppoCode.getScientificName() == null || eppoCode.getScientificName().isBlank())
                && eppoCode.getLatinSpeciesName() != null && !eppoCode.getLatinSpeciesName().isBlank()) {
            eppoCode.setScientificName(eppoCode.getLatinSpeciesName());
        }

        eppoCode.setStatus(normalizeStatus(eppoCode.getStatus()));
    }

    private void log(String actionType, Integer entityId, String description) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.log("EPPO_CODE", entityId, actionType, description);
    }

    private String describe(EppoCode eppoCode) {
        if (eppoCode == null) {
            return "[brak kodu EPPO]";
        }

        String code = normalizeText(eppoCode.getCode());
        String species = firstNonBlank(eppoCode.getSpeciesName(), eppoCode.getCommonName());
        String latin = firstNonBlank(eppoCode.getLatinSpeciesName(), eppoCode.getScientificName());
        String status = normalizeText(eppoCode.getStatus());

        StringBuilder sb = new StringBuilder();
        if (code != null && !code.isBlank()) {
            sb.append(code);
        }
        if (species != null && !species.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append(species);
        }
        if (latin != null && !latin.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append(latin);
        }
        if (status != null && !status.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append("[").append(status).append("]");
        }

        String description = sb.toString().trim();
        return description.isBlank() ? "[brak kodu EPPO]" : description;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String normalizeCode(String value) {
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
