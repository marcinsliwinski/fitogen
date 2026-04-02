package com.egen.fitogen.service;

import com.egen.fitogen.model.Contrahent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ContrahentCsvExportService {
    private final ContrahentService contrahentService;

    public ContrahentCsvExportService(ContrahentService contrahentService) {
        this.contrahentService = contrahentService;
    }

    public Path export(Path outputPath) {
        List<Contrahent> contrahents = contrahentService.getAllContrahents();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("name;country;countryCode;city;postalCode;street;phytosanitaryNumber;supplier;client");
            writer.newLine();
            for (Contrahent contrahent : contrahents) {
                writer.write(String.join(";",
                        escape(contrahent.getName()),
                        escape(contrahent.getCountry()),
                        escape(contrahent.getCountryCode()),
                        escape(contrahent.getCity()),
                        escape(contrahent.getPostalCode()),
                        escape(contrahent.getStreet()),
                        escape(contrahent.getPhytosanitaryNumber()),
                        String.valueOf(contrahent.isSupplier()),
                        String.valueOf(contrahent.isClient())
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się wyeksportować kontrahentów do CSV: " + outputPath, e);
        }
        return outputPath;
    }

    public String getSupportedColumnsSummary() {
        return "Kolumny eksportu Contrahents CSV: name, country, countryCode, city, postalCode, street, phytosanitaryNumber, supplier, client.";
    }

    private String escape(String value) {
        if (value == null) return "";
        boolean needsQuotes = value.contains(";") || value.contains(""") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace(""", """");
        return needsQuotes ? """ + escaped + """ : escaped;
    }
}
