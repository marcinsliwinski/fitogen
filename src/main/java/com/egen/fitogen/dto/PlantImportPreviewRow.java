package com.egen.fitogen.dto;

public class PlantImportPreviewRow {

    private final int rowNumber;
    private final String species;
    private final String variety;
    private final String rootstock;
    private final String latinSpeciesName;
    private final String eppoCode;
    private final boolean passportRequired;
    private final String visibilityStatus;
    private final String status;
    private final String message;

    public PlantImportPreviewRow(
            int rowNumber,
            String species,
            String variety,
            String rootstock,
            String latinSpeciesName,
            String eppoCode,
            boolean passportRequired,
            String visibilityStatus,
            String status,
            String message
    ) {
        this.rowNumber = rowNumber;
        this.species = species;
        this.variety = variety;
        this.rootstock = rootstock;
        this.latinSpeciesName = latinSpeciesName;
        this.eppoCode = eppoCode;
        this.passportRequired = passportRequired;
        this.visibilityStatus = visibilityStatus;
        this.status = status;
        this.message = message;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getSpecies() {
        return species;
    }

    public String getVariety() {
        return variety;
    }

    public String getRootstock() {
        return rootstock;
    }

    public String getLatinSpeciesName() {
        return latinSpeciesName;
    }

    public String getEppoCode() {
        return eppoCode;
    }

    public boolean isPassportRequired() {
        return passportRequired;
    }

    public String getVisibilityStatus() {
        return visibilityStatus;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}