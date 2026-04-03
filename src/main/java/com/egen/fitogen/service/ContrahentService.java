package com.egen.fitogen.service;

import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.repository.ContrahentRepository;

import java.util.List;

public class ContrahentService {

    private final ContrahentRepository repository;
    private final AuditLogService auditLogService;

    public ContrahentService(ContrahentRepository repository) {
        this(repository, null);
    }

    public ContrahentService(ContrahentRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    public void addContrahent(Contrahent contrahent) {
        validate(contrahent);
        repository.save(contrahent);
        logChange("Contrahent", contrahent.getId(), "CREATE", "Dodano kontrahenta: " + describeContrahent(contrahent));
    }

    public void updateContrahent(Contrahent contrahent) {
        Contrahent beforeUpdate = repository.findById(contrahent.getId());
        validate(contrahent);
        repository.update(contrahent);
        logChange("Contrahent", contrahent.getId(), "UPDATE",
                "Zaktualizowano kontrahenta: " + describeContrahent(contrahent)
                        + buildBeforeAfterSuffix(describeContrahent(beforeUpdate), describeContrahent(contrahent)));
    }

    public void deleteContrahent(int id) {
        Contrahent contrahent = repository.findById(id);
        repository.deleteById(id);
        logChange("Contrahent", id, "DELETE", "Usunięto kontrahenta: " + describeContrahent(contrahent));
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

    private void validate(Contrahent contrahent) {
        if (contrahent == null) {
            throw new IllegalArgumentException("Contrahent cannot be null.");
        }

        if (contrahent.getName() == null || contrahent.getName().isBlank()) {
            throw new IllegalArgumentException("Contrahent name required.");
        }
    }

    private void logChange(String entityType, Integer entityId, String actionType, String description) {
        if (auditLogService != null) {
            auditLogService.log(entityType, entityId, actionType, description);
        }
    }

    private String describeContrahent(Contrahent contrahent) {
        if (contrahent == null) {
            return "[brak danych]";
        }

        String name = safe(contrahent.getName());
        String country = safe(contrahent.getCountry());
        String city = safe(contrahent.getCity());
        String role = buildRoleLabel(contrahent);

        StringBuilder sb = new StringBuilder(name.isBlank() ? "ID=" + contrahent.getId() : name);
        if (!city.isBlank()) {
            sb.append(", ").append(city);
        }
        if (!country.isBlank()) {
            sb.append(", ").append(country);
        }
        if (!role.isBlank()) {
            sb.append(" [").append(role).append("]");
        }
        return sb.toString();
    }

    private String buildRoleLabel(Contrahent contrahent) {
        if (contrahent.isSupplier() && contrahent.isClient()) {
            return "dostawca i klient";
        }
        if (contrahent.isSupplier()) {
            return "dostawca";
        }
        if (contrahent.isClient()) {
            return "klient";
        }
        return "";
    }

    private String buildBeforeAfterSuffix(String before, String after) {
        if (before == null || before.isBlank() || before.equals(after)) {
            return "";
        }
        return " (wcześniej: " + before + ")";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}