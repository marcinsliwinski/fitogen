package com.egen.fitogen.ui.util;

import com.egen.fitogen.model.Contrahent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CountryDirectory {

    private CountryDirectory() {
    }

    public static List<String> countries(List<Contrahent> contrahents) {
        return entries(contrahents).stream()
                .map(CountryEntry::country)
                .toList();
    }

    public static List<String> codes(List<Contrahent> contrahents) {
        return entries(contrahents).stream()
                .map(CountryEntry::countryCode)
                .distinct()
                .toList();
    }

    public static String findCodeByCountry(String country, List<Contrahent> contrahents) {
        String normalizedCountry = normalize(country);
        if (normalizedCountry == null) {
            return null;
        }

        for (CountryEntry entry : entries(contrahents)) {
            if (normalize(entry.country()).equals(normalizedCountry)) {
                return entry.countryCode();
            }
        }

        return null;
    }

    public static String findCountryByCode(String code, List<Contrahent> contrahents) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return null;
        }

        for (CountryEntry entry : entries(contrahents)) {
            if (normalizedCode.equals(normalizeCode(entry.countryCode()))) {
                return entry.country();
            }
        }

        return null;
    }

    public static List<CountryEntry> entries(List<Contrahent> contrahents) {
        Map<String, CountryEntry> merged = new LinkedHashMap<>();

        CountryCatalog.countryToCodeMap().forEach((country, code) -> {
            CountryEntry entry = buildEntry(country, code);
            if (entry != null) {
                merged.put(normalize(entry.country()), entry);
            }
        });

        if (contrahents != null) {
            for (Contrahent contrahent : contrahents) {
                if (contrahent == null) {
                    continue;
                }

                CountryEntry entry = buildEntry(contrahent.getCountry(), contrahent.getCountryCode());
                if (entry == null) {
                    continue;
                }

                merged.putIfAbsent(normalize(entry.country()), entry);
            }
        }

        List<CountryEntry> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(CountryEntry::country, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static CountryEntry buildEntry(String country, String countryCode) {
        String normalizedCountry = normalizeDisplay(country);
        if (normalizedCountry == null) {
            return null;
        }

        String normalizedCode = normalizeCode(countryCode);
        if (normalizedCode == null) {
            normalizedCode = CountryCatalog.findCodeByCountry(normalizedCountry);
        }
        if (normalizedCode == null) {
            return null;
        }

        String canonicalCountry = CountryCatalog.findCountryByCode(normalizedCode);
        if (canonicalCountry != null && !canonicalCountry.isBlank()) {
            normalizedCountry = canonicalCountry;
        }

        return new CountryEntry(normalizedCountry, normalizedCode);
    }

    private static String normalize(String value) {
        String normalized = normalizeDisplay(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeDisplay(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private static String normalizeCode(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    public record CountryEntry(String country, String countryCode) {
    }
}
