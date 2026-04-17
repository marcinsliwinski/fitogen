package com.egen.fitogen.service;

import com.egen.fitogen.model.IssuerProfile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SubjectDataCsvExportService {

    private final AppSettingsService appSettingsService;

    public SubjectDataCsvExportService(AppSettingsService appSettingsService) {
        this.appSettingsService = appSettingsService;
    }

    public Path export(Path outputPath) {
        IssuerProfile profile = appSettingsService.getIssuerProfile();
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("nurseryName;country;countryCode;postalCode;city;street;noStreet;phytosanitaryNumber");
            writer.newLine();
            writer.write(escape(profile.getNurseryName()));
            writer.write(';');
            writer.write(escape(profile.getCountry()));
            writer.write(';');
            writer.write(escape(profile.getCountryCode()));
            writer.write(';');
            writer.write(escape(profile.getPostalCode()));
            writer.write(';');
            writer.write(escape(profile.getCity()));
            writer.write(';');
            writer.write(escape(profile.getStreet()));
            writer.write(';');
            writer.write(profile.isNoStreet() ? "true" : "false");
            writer.write(';');
            writer.write(escape(profile.getPhytosanitaryNumber()));
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować danych podmiotu do CSV: " + outputPath, e);
        }
        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Jeden plik CSV danych podmiotu. Kolumny: nurseryName, country, countryCode, postalCode, city, street, noStreet, phytosanitaryNumber. Eksport obejmuje profil szkółki / wystawcy używany w dokumentach.";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
