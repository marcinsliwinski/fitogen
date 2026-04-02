package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoZone;
import com.egen.fitogen.repository.EppoZoneRepository;

import java.util.List;

public class EppoZoneService {

    private final EppoZoneRepository repository;

    public EppoZoneService(EppoZoneRepository repository) {
        this.repository = repository;
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
            repository.update(eppoZone);
        } else {
            repository.save(eppoZone);
        }
    }

    public void delete(int id) {
        repository.deleteById(id);
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

        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? "" : normalized;
    }
}