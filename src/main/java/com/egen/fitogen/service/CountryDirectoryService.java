package com.egen.fitogen.service;

import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.ui.util.CountryDirectory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CountryDirectoryService {

    private final ContrahentService contrahentService;
    private final AppSettingsService appSettingsService;
    private final AuditLogService auditLogService;

    public CountryDirectoryService(ContrahentService contrahentService,
                                   AppSettingsService appSettingsService,
                                   AuditLogService auditLogService) {
        this.contrahentService = contrahentService;
        this.appSettingsService = appSettingsService;
        this.auditLogService = auditLogService;
    }

    public List<CountryDirectory.CountryEntry> getEntries() {
        Map<String, CountryDirectory.CountryEntry> merged = new LinkedHashMap<>();

        for (CountryDirectory.CountryEntry entry : CountryDirectory.entries(existingContrahents())) {
            merged.put(normalizeKey(entry.country()), normalizeEntry(entry));
        }

        for (CountryDirectory.CountryEntry entry : getCustomEntries()) {
            CountryDirectory.CountryEntry normalized = normalizeEntry(entry);
            if (normalized == null) {
                continue;
            }
            merged.put(normalizeKey(normalized.country()), normalized);
        }

        List<CountryDirectory.CountryEntry> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(CountryDirectory.CountryEntry::country, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public List<String> getCountries() {
        return getEntries().stream()
                .map(CountryDirectory.CountryEntry::country)
                .toList();
    }

    public List<String> getCodes() {
        return getEntries().stream()
                .map(CountryDirectory.CountryEntry::countryCode)
                .distinct()
                .toList();
    }

    public String findCodeByCountry(String country) {
        String normalizedCountry = normalizeKey(country);
        if (normalizedCountry == null) {
            return null;
        }

        for (CountryDirectory.CountryEntry entry : getEntries()) {
            if (normalizedCountry.equals(normalizeKey(entry.country()))) {
                return entry.countryCode();
            }
        }

        return null;
    }

    public String findCountryByCode(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return null;
        }

        for (CountryDirectory.CountryEntry entry : getEntries()) {
            if (normalizedCode.equals(normalizeCode(entry.countryCode()))) {
                return entry.country();
            }
        }

        return null;
    }

    public List<CountryDirectory.CountryEntry> getCustomEntries() {
        return appSettingsService.getCustomCountryEntries();
    }

    public void saveCustomEntry(String country, String countryCode) {
        saveCustomEntry(null, null, country, countryCode);
    }

    public void saveCustomEntry(String originalCountry,
                                String originalCountryCode,
                                String country,
                                String countryCode) {
        CountryDirectory.CountryEntry normalized = normalizeEntry(new CountryDirectory.CountryEntry(country, countryCode));
        if (normalized == null) {
            throw new IllegalArgumentException("Kraj i kod kraju są wymagane.");
        }

        CountryDirectory.CountryEntry original = normalizeEntry(new CountryDirectory.CountryEntry(originalCountry, originalCountryCode));
        validateEntry(normalized, original);

        Map<String, CountryDirectory.CountryEntry> customMap = new LinkedHashMap<>();
        for (CountryDirectory.CountryEntry entry : getCustomEntries()) {
            CountryDirectory.CountryEntry current = normalizeEntry(entry);
            if (current != null) {
                customMap.put(normalizeKey(current.country()), current);
            }
        }

        CountryDirectory.CountryEntry existing = original != null ? findCustomEntryByCountry(original.country()) : findCustomEntryByCountry(normalized.country());
        if (sameEntry(existing, normalized)) {
            return;
        }

        if (original != null) {
            customMap.remove(normalizeKey(original.country()));
        }

        customMap.put(normalizeKey(normalized.country()), normalized);
        appSettingsService.saveCustomCountryEntries(new ArrayList<>(customMap.values()));

        if (existing == null) {
            log("CREATE", "Dodano własny wpis słownika krajów: " + describe(normalized));
        } else {
            log(
                    "UPDATE",
                    "Zaktualizowano własny wpis słownika krajów z " + describe(existing)
                            + " na " + describe(normalized)
            );
        }
    }

    public void deleteCustomEntry(String country, String countryCode) {
        String normalizedCountry = normalizeKey(country);
        String normalizedCode = normalizeCode(countryCode);
        if (normalizedCountry == null) {
            return;
        }

        CountryDirectory.CountryEntry existing = findCustomEntryByCountry(country);

        List<CountryDirectory.CountryEntry> retained = new ArrayList<>();
        for (CountryDirectory.CountryEntry entry : getCustomEntries()) {
            CountryDirectory.CountryEntry current = normalizeEntry(entry);
            if (current == null) {
                continue;
            }

            boolean sameCountry = normalizedCountry.equals(normalizeKey(current.country()));
            boolean sameCode = normalizedCode == null || normalizedCode.equals(normalizeCode(current.countryCode()));
            if (!(sameCountry && sameCode)) {
                retained.add(current);
            }
        }

        appSettingsService.saveCustomCountryEntries(retained);
        log("DELETE", "Usunięto własny wpis słownika krajów: " + describe(existing));
    }

    private List<Contrahent> existingContrahents() {
        return contrahentService == null ? List.of() : contrahentService.getAllContrahents();
    }

    private CountryDirectory.CountryEntry findCustomEntryByCountry(String country) {
        String key = normalizeKey(country);
        if (key == null) {
            return null;
        }

        return getCustomEntries().stream()
                .map(this::normalizeEntry)
                .filter(entry -> entry != null && key.equals(normalizeKey(entry.country())))
                .findFirst()
                .orElse(null);
    }

    private void validateEntry(CountryDirectory.CountryEntry candidate, CountryDirectory.CountryEntry ignoredEntry) {
        if (candidate == null) {
            throw new IllegalArgumentException("Kraj i kod kraju są wymagane.");
        }

        if (!candidate.countryCode().matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("Kod kraju musi składać się z dokładnie 2 liter, np. PL.");
        }

        String candidateCountryKey = normalizeKey(candidate.country());
        String candidateCode = normalizeCode(candidate.countryCode());
        String ignoredCountryKey = ignoredEntry == null ? null : normalizeKey(ignoredEntry.country());

        for (CountryDirectory.CountryEntry entry : getEntries()) {
            CountryDirectory.CountryEntry current = normalizeEntry(entry);
            if (current == null) {
                continue;
            }

            if (ignoredCountryKey != null && ignoredCountryKey.equals(normalizeKey(current.country()))) {
                continue;
            }

            boolean sameCountry = candidateCountryKey.equals(normalizeKey(current.country()));
            boolean sameCode = candidateCode.equals(normalizeCode(current.countryCode()));
            if (sameCode && !sameCountry) {
                throw new IllegalArgumentException(
                        "Kod kraju " + candidateCode + " jest już używany dla kraju: " + current.country() + "."
                );
            }
        }
    }

    private boolean sameEntry(CountryDirectory.CountryEntry first, CountryDirectory.CountryEntry second) {
        if (first == null || second == null) {
            return false;
        }

        return normalizeKey(first.country()).equals(normalizeKey(second.country()))
                && normalizeCode(first.countryCode()).equals(normalizeCode(second.countryCode()));
    }

    private CountryDirectory.CountryEntry normalizeEntry(CountryDirectory.CountryEntry entry) {
        if (entry == null) {
            return null;
        }

        String country = normalizeDisplay(entry.country());
        String code = normalizeCode(entry.countryCode());
        if (country == null || code == null) {
            return null;
        }

        return new CountryDirectory.CountryEntry(country, code);
    }

    private String normalizeKey(String value) {
        String normalized = normalizeDisplay(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplay(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private void log(String actionType, String description) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.log("COUNTRY_DIRECTORY", null, actionType, description);
    }

    private String describe(CountryDirectory.CountryEntry entry) {
        if (entry == null) {
            return "[brak wpisu]";
        }
        return entry.country() + " (" + entry.countryCode() + ")";
    }
}
