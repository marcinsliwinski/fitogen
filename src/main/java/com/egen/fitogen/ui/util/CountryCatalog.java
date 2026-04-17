package com.egen.fitogen.ui.util;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CountryCatalog {

    private static final Map<String, String> COUNTRY_TO_CODE = new LinkedHashMap<>();
    private static final Map<String, String> CODE_TO_COUNTRY = new LinkedHashMap<>();
    private static final Map<String, String> COUNTRY_ALIASES = new LinkedHashMap<>();

    static {
        add("Albania", "AL");
        add("Andora", "AD");
        add("Austria", "AT");
        add("Belgia", "BE");
        add("Białoruś", "BY");
        add("Bośnia i Hercegowina", "BA");
        add("Bułgaria", "BG");
        add("Chorwacja", "HR");
        add("Cypr", "CY");
        add("Czarnogóra", "ME");
        add("Czechy", "CZ");
        add("Dania", "DK");
        add("Estonia", "EE");
        add("Finlandia", "FI");
        add("Francja", "FR");
        add("Grecja", "EL");
        add("Hiszpania", "ES");
        add("Holandia", "NL");
        add("Irlandia", "IE");
        add("Islandia", "IS");
        add("Kosowo", "XK");
        add("Liechtenstein", "LI");
        add("Litwa", "LT");
        add("Luksemburg", "LU");
        add("Łotwa", "LV");
        add("Macedonia Północna", "MK");
        add("Malta", "MT");
        add("Mołdawia", "MD");
        add("Monako", "MC");
        add("Niemcy", "DE");
        add("Norwegia", "NO");
        add("Polska", "PL");
        add("Portugalia", "PT");
        add("Rosja", "RU");
        add("Rumunia", "RO");
        add("San Marino", "SM");
        add("Serbia", "RS");
        add("Słowacja", "SK");
        add("Słowenia", "SI");
        add("Szwajcaria", "CH");
        add("Szwecja", "SE");
        add("Turcja", "TR");
        add("Ukraina", "UA");
        add("Watykan", "VA");
        add("Węgry", "HU");
        add("Wielka Brytania", "GB");
        add("Włochy", "IT");

        CODE_TO_COUNTRY.put("GR", "Grecja");

        alias("UK", "GB");
        alias("Great Britain", "GB");
        alias("United Kingdom", "GB");
        alias("England", "GB");
        alias("Scotland", "GB");
        alias("Netherlands", "NL");
        alias("The Netherlands", "NL");
        alias("Bosnia and Herzegovina", "BA");
        alias("North Macedonia", "MK");
        alias("Czech Republic", "CZ");
        alias("Türkiye", "TR");
        alias("Turkey", "TR");
    }

    private CountryCatalog() {
    }

    public static List<String> countries() {
        return List.copyOf(COUNTRY_TO_CODE.keySet());
    }

    public static List<String> codes() {
        return List.copyOf(CODE_TO_COUNTRY.keySet());
    }

    public static String findCodeByCountry(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }

        String normalized = normalize(country);

        for (Map.Entry<String, String> entry : COUNTRY_TO_CODE.entrySet()) {
            if (normalize(entry.getKey()).equals(normalized)) {
                return entry.getValue();
            }
        }

        String aliasCode = COUNTRY_ALIASES.get(normalized);
        if (aliasCode != null && !aliasCode.isBlank()) {
            return aliasCode;
        }

        return null;
    }

    public static String findCountryByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        return CODE_TO_COUNTRY.get(code.trim().toUpperCase(Locale.ROOT));
    }

    public static Map<String, String> countryToCodeMap() {
        return Map.copyOf(COUNTRY_TO_CODE);
    }

    private static void add(String country, String code) {
        COUNTRY_TO_CODE.put(country, code);
        CODE_TO_COUNTRY.put(code, country);
    }

    private static void alias(String alias, String code) {
        COUNTRY_ALIASES.put(normalize(alias), code);
    }

    private static String normalize(String value) {
        String trimmed = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }
}