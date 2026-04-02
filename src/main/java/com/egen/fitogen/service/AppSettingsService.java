package com.egen.fitogen.service;

import com.egen.fitogen.model.IssuerProfile;
import com.egen.fitogen.repository.AppSettingsRepository;
import com.egen.fitogen.ui.util.CountryDirectory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppSettingsService {

    public static final String ISSUER_NURSERY_NAME = "issuer.nursery_name";
    public static final String ISSUER_COUNTRY = "issuer.country";
    public static final String ISSUER_COUNTRY_CODE = "issuer.country_code";
    public static final String ISSUER_POSTAL_CODE = "issuer.postal_code";
    public static final String ISSUER_CITY = "issuer.city";
    public static final String ISSUER_STREET = "issuer.street";
    public static final String ISSUER_PHYTOSANITARY_NUMBER = "issuer.phytosanitary_number";
    public static final String LAST_BACKUP_PATH = "backup.last.path";
    public static final String LAST_BACKUP_AT = "backup.last.at";
    public static final String PLANT_FULL_CATALOG_ENABLED = "plant.full_catalog_enabled";
    public static final String PLANT_PASSPORT_REQUIRED_FOR_ALL = "plant.passport_required_for_all";
    public static final String CUSTOM_COUNTRY_DIRECTORY_ENTRIES = "dictionary.country.custom_entries";

    private final AppSettingsRepository appSettingsRepository;

    public AppSettingsService(AppSettingsRepository appSettingsRepository) {
        this.appSettingsRepository = appSettingsRepository;
    }

    public String getSetting(String key) {
        return appSettingsRepository.findValueByKey(key).orElse("");
    }

    public void saveSetting(String key, String value) {
        appSettingsRepository.upsert(key, value == null ? "" : value.trim());
    }

    public String getValue(String key) {
        return getSetting(key);
    }

    public void setValue(String key, String value) {
        saveSetting(key, value);
    }

    public Map<String, String> getAllSettings() {
        return appSettingsRepository.findAll();
    }

    public IssuerProfile getIssuerProfile() {
        IssuerProfile profile = new IssuerProfile();
        profile.setNurseryName(getSetting(ISSUER_NURSERY_NAME));
        profile.setCountry(getSetting(ISSUER_COUNTRY));
        profile.setCountryCode(getSetting(ISSUER_COUNTRY_CODE));
        profile.setPostalCode(getSetting(ISSUER_POSTAL_CODE));
        profile.setCity(getSetting(ISSUER_CITY));
        profile.setStreet(getSetting(ISSUER_STREET));
        profile.setPhytosanitaryNumber(getSetting(ISSUER_PHYTOSANITARY_NUMBER));
        return profile;
    }

    public IssuerProfile loadIssuerProfile() {
        return getIssuerProfile();
    }

    public void saveIssuerProfile(IssuerProfile profile) {
        if (profile == null) {
            profile = new IssuerProfile();
        }

        saveSetting(ISSUER_NURSERY_NAME, profile.getNurseryName());
        saveSetting(ISSUER_COUNTRY, profile.getCountry());
        saveSetting(ISSUER_COUNTRY_CODE, profile.getCountryCode());
        saveSetting(ISSUER_POSTAL_CODE, profile.getPostalCode());
        saveSetting(ISSUER_CITY, profile.getCity());
        saveSetting(ISSUER_STREET, profile.getStreet());
        saveSetting(ISSUER_PHYTOSANITARY_NUMBER, profile.getPhytosanitaryNumber());
    }

    public void saveLastBackup(String path, String at) {
        saveSetting(LAST_BACKUP_PATH, path);
        saveSetting(LAST_BACKUP_AT, at);
    }

    public String getLastBackupPath() {
        return getSetting(LAST_BACKUP_PATH);
    }

    public String getLastBackupAt() {
        return getSetting(LAST_BACKUP_AT);
    }

    public List<CountryDirectory.CountryEntry> getCustomCountryEntries() {
        String raw = getSetting(CUSTOM_COUNTRY_DIRECTORY_ENTRIES);
        List<CountryDirectory.CountryEntry> entries = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return entries;
        }

        for (String line : raw.split("\n")) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String[] parts = line.split("\t", 2);
            if (parts.length < 2) {
                continue;
            }

            String country = unescape(parts[0]);
            String code = unescape(parts[1]);
            if (notBlank(country) && notBlank(code)) {
                entries.add(new CountryDirectory.CountryEntry(country.trim(), code.trim().toUpperCase()));
            }
        }

        return entries;
    }

    public void saveCustomCountryEntries(List<CountryDirectory.CountryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            saveSetting(CUSTOM_COUNTRY_DIRECTORY_ENTRIES, "");
            return;
        }

        StringBuilder serialized = new StringBuilder();
        for (CountryDirectory.CountryEntry entry : entries) {
            if (entry == null || !notBlank(entry.country()) || !notBlank(entry.countryCode())) {
                continue;
            }

            if (serialized.length() > 0) {
                serialized.append("\n");
            }

            serialized.append(escape(entry.country().trim()))
                    .append("\t")
                    .append(escape(entry.countryCode().trim().toUpperCase()));
        }

        saveSetting(CUSTOM_COUNTRY_DIRECTORY_ENTRIES, serialized.toString());
    }

    public boolean isPlantFullCatalogEnabled() {
        String raw = getSetting(PLANT_FULL_CATALOG_ENABLED);
        return "true".equalsIgnoreCase(raw) || "1".equals(raw);
    }

    public void setPlantFullCatalogEnabled(boolean enabled) {
        saveSetting(PLANT_FULL_CATALOG_ENABLED, String.valueOf(enabled));
    }

    public boolean isPlantPassportRequiredForAll() {
        String raw = getSetting(PLANT_PASSPORT_REQUIRED_FOR_ALL);
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        return "true".equalsIgnoreCase(raw) || "1".equals(raw);
    }

    public void setPlantPassportRequiredForAll(boolean enabled) {
        saveSetting(PLANT_PASSPORT_REQUIRED_FOR_ALL, String.valueOf(enabled));
    }

    public boolean isIssuerProfileComplete() {
        IssuerProfile profile = getIssuerProfile();
        return notBlank(profile.getNurseryName())
                && notBlank(profile.getCountry())
                && notBlank(profile.getPostalCode())
                && notBlank(profile.getCity())
                && notBlank(profile.getStreet())
                && notBlank(profile.getPhytosanitaryNumber());
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\n", "\\n");
    }

    private String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (escaping) {
                if (current == 't') {
                    result.append('\t');
                } else if (current == 'n') {
                    result.append('\n');
                } else {
                    result.append(current);
                }
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else {
                result.append(current);
            }
        }
        if (escaping) {
            result.append('\\');
        }
        return result.toString();
    }
}
