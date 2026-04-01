package com.egen.fitogen.service;

import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.repository.EppoCodeRepository;

import java.util.List;

public class EppoCodeService {

    private final EppoCodeRepository repository;

    public EppoCodeService(EppoCodeRepository repository) {
        this.repository = repository;
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
            repository.update(eppoCode);
        } else {
            repository.save(eppoCode);
        }
    }

    public void delete(int id) {
        repository.deleteById(id);
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

        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? "" : normalized;
    }
}