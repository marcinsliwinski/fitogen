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
        log("CREATE", null, "Dodano kontrahenta: " + describe(contrahent));
    }

    public void updateContrahent(Contrahent contrahent) {
        validate(contrahent);
        Contrahent existing = repository.findById(contrahent.getId());
        repository.update(contrahent);
        log(
                "UPDATE",
                contrahent.getId(),
                "Zaktualizowano kontrahenta z " + describe(existing) + " na " + describe(contrahent)
        );
    }

    public void deleteContrahent(int id) {
        Contrahent existing = repository.findById(id);
        repository.deleteById(id);
        log("DELETE", id, "Usunięto kontrahenta: " + describe(existing));
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

    private void log(String actionType, Integer entityId, String description) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.log("CONTRAHENT", entityId, actionType, description);
    }

    private String describe(Contrahent contrahent) {
        if (contrahent == null) {
            return "[brak kontrahenta]";
        }

        StringBuilder sb = new StringBuilder();
        appendPart(sb, contrahent.getName());

        String city = normalizeText(contrahent.getCity());
        if (city != null && !city.isBlank()) {
            sb.append(" [miasto: ").append(city).append("]");
        }

        String country = normalizeText(contrahent.getCountry());
        String countryCode = normalizeText(contrahent.getCountryCode());
        if ((country != null && !country.isBlank()) || (countryCode != null && !countryCode.isBlank())) {
            sb.append(" [kraj: ");
            if (country != null && !country.isBlank()) {
                sb.append(country);
            }
            if (countryCode != null && !countryCode.isBlank()) {
                if (country != null && !country.isBlank()) {
                    sb.append(' ');
                }
                sb.append('(').append(countryCode).append(')');
            }
            sb.append("]");
        }

        String phytoNo = normalizeText(contrahent.getPhytosanitaryNumber());
        if (phytoNo != null && !phytoNo.isBlank()) {
            sb.append(" [nr fito: ").append(phytoNo).append("]");
        }

        sb.append(contrahent.isSupplier() ? " [dostawca]" : "");
        sb.append(contrahent.isClient() ? " [odbiorca]" : "");

        String description = sb.toString().trim();
        return description.isBlank() ? "[brak kontrahenta]" : description;
    }

    private void appendPart(StringBuilder sb, String value) {
        String normalized = normalizeText(value);
        if (normalized == null || normalized.isBlank()) {
            return;
        }

        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(normalized);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
