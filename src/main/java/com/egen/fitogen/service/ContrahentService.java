package com.egen.fitogen.service;

import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.repository.ContrahentRepository;

import java.util.List;

public class ContrahentService {

    private final ContrahentRepository repository;
    private final AuditLogService auditLogService;

    public ContrahentService(ContrahentRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    public void addContrahent(Contrahent contrahent) {
        validate(contrahent);
        repository.save(contrahent);
        if (auditLogService != null) {
            auditLogService.log("CONTRAHENT", null, "CREATE", "Dodano kontrahenta: " + buildContrahentSummary(contrahent));
        }
    }

    public void updateContrahent(Contrahent contrahent) {
        validate(contrahent);
        repository.update(contrahent);
        if (auditLogService != null) {
            auditLogService.log("CONTRAHENT", contrahent.getId(), "UPDATE", "Zaktualizowano kontrahenta: " + buildContrahentSummary(contrahent));
        }
    }

    public void deleteContrahent(int id) {
        repository.deleteById(id);
        if (auditLogService != null) {
            auditLogService.log("CONTRAHENT", id, "DELETE", "Usunięto kontrahenta o ID=" + id);
        }
    }

    public Contrahent getContrahentById(int id) {
        return repository.findById(id);
    }

    public List<Contrahent> getAllContrahents() {
        return repository.findAll();
    }

    public List<Contrahent> getAllClients() {
        return repository.findAllClients();
    }


    private String buildContrahentSummary(Contrahent contrahent) {
        String name = contrahent.getName() == null ? "" : contrahent.getName().trim();
        String country = contrahent.getCountry() == null ? "" : contrahent.getCountry().trim();

        if (country.isBlank()) {
            return name.isBlank() ? "brak nazwy" : name;
        }
        if (name.isBlank()) {
            return "brak nazwy | " + country;
        }

        return name + " | " + country;
    }

    private void validate(Contrahent contrahent) {
        if (contrahent == null) {
            throw new IllegalArgumentException("Contrahent cannot be null.");
        }

        if (contrahent.getName() == null || contrahent.getName().isBlank()) {
            throw new IllegalArgumentException("Contrahent name required.");
        }
    }
}