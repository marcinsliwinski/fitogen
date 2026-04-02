package com.egen.fitogen.dto;

public class ContrahentImportPreviewRow {

    private final int rowNumber;
    private final String name;
    private final String country;
    private final String countryCode;
    private final String city;
    private final String postalCode;
    private final String phytosanitaryNumber;
    private final boolean supplier;
    private final boolean client;
    private final String status;
    private final String message;

    public ContrahentImportPreviewRow(
            int rowNumber,
            String name,
            String country,
            String countryCode,
            String city,
            String postalCode,
            String phytosanitaryNumber,
            boolean supplier,
            boolean client,
            String status,
            String message
    ) {
        this.rowNumber = rowNumber;
        this.name = name;
        this.country = country;
        this.countryCode = countryCode;
        this.city = city;
        this.postalCode = postalCode;
        this.phytosanitaryNumber = phytosanitaryNumber;
        this.supplier = supplier;
        this.client = client;
        this.status = status;
        this.message = message;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getPhytosanitaryNumber() {
        return phytosanitaryNumber;
    }

    public boolean isSupplier() {
        return supplier;
    }

    public boolean isClient() {
        return client;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
