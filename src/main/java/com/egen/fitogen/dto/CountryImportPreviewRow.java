package com.egen.fitogen.dto;

public class CountryImportPreviewRow {
    private final int rowNumber;
    private final String country;
    private final String countryCode;
    private final String status;
    private final String message;

    public CountryImportPreviewRow(int rowNumber, String country, String countryCode, String status, String message) {
        this.rowNumber = rowNumber;
        this.country = country;
        this.countryCode = countryCode;
        this.status = status;
        this.message = message;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
