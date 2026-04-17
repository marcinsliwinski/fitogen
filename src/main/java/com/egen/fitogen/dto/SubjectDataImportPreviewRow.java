package com.egen.fitogen.dto;

public class SubjectDataImportPreviewRow {
    private final int rowNumber;
    private final String nurseryName;
    private final String country;
    private final String countryCode;
    private final String postalCode;
    private final String city;
    private final String street;
    private final boolean noStreet;
    private final String phytosanitaryNumber;
    private final String status;
    private final String message;

    public SubjectDataImportPreviewRow(int rowNumber,
                                       String nurseryName,
                                       String country,
                                       String countryCode,
                                       String postalCode,
                                       String city,
                                       String street,
                                       boolean noStreet,
                                       String phytosanitaryNumber,
                                       String status,
                                       String message) {
        this.rowNumber = rowNumber;
        this.nurseryName = nurseryName;
        this.country = country;
        this.countryCode = countryCode;
        this.postalCode = postalCode;
        this.city = city;
        this.street = street;
        this.noStreet = noStreet;
        this.phytosanitaryNumber = phytosanitaryNumber;
        this.status = status;
        this.message = message;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getNurseryName() {
        return nurseryName;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public boolean isNoStreet() {
        return noStreet;
    }

    public String getPhytosanitaryNumber() {
        return phytosanitaryNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
