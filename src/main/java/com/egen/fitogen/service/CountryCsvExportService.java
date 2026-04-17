package com.egen.fitogen.service;

import com.egen.fitogen.ui.util.CountryDirectory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CountryCsvExportService {

    private final CountryDirectoryService countryDirectoryService;

    public CountryCsvExportService(CountryDirectoryService countryDirectoryService) {
        this.countryDirectoryService = countryDirectoryService;
    }

    public Path export(Path outputPath) {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("country;countryCode");
            writer.newLine();
            for (CountryDirectory.CountryEntry entry : countryDirectoryService.getEntries()) {
                writer.write(escape(entry.country()));
                writer.write(';');
                writer.write(escape(entry.countryCode()));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować słownika krajów do CSV: " + outputPath, e);
        }
        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Jeden plik CSV wspólnego słownika krajów. Kolumny: country (kraj), countryCode (kod kraju). Eksport obejmuje wspólny słownik używany przez Kontrahentów, EPPO i dane podmiotu.";
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
