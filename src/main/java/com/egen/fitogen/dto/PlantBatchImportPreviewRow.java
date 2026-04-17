package com.egen.fitogen.dto;

public class PlantBatchImportPreviewRow {
    private final int rowNumber;
    private final Integer batchId;
    private final String interiorBatchNo;
    private final String exteriorBatchNo;
    private final String plantSpecies;
    private final String plantVariety;
    private final String plantRootstock;
    private final String sourceContrahentName;
    private final int qty;
    private final int ageYears;
    private final String creationDate;
    private final String manufacturerCountryCode;
    private final String fitoQualificationCategory;
    private final String eppoCode;
    private final String zpZone;
    private final boolean internalSource;
    private final String comments;
    private final String batchStatus;
    private final String status;
    private final String message;

    public PlantBatchImportPreviewRow(int rowNumber,
                                      Integer batchId,
                                      String interiorBatchNo,
                                      String exteriorBatchNo,
                                      String plantSpecies,
                                      String plantVariety,
                                      String plantRootstock,
                                      String sourceContrahentName,
                                      int qty,
                                      int ageYears,
                                      String creationDate,
                                      String manufacturerCountryCode,
                                      String fitoQualificationCategory,
                                      String eppoCode,
                                      String zpZone,
                                      boolean internalSource,
                                      String comments,
                                      String batchStatus,
                                      String status,
                                      String message) {
        this.rowNumber = rowNumber;
        this.batchId = batchId;
        this.interiorBatchNo = interiorBatchNo;
        this.exteriorBatchNo = exteriorBatchNo;
        this.plantSpecies = plantSpecies;
        this.plantVariety = plantVariety;
        this.plantRootstock = plantRootstock;
        this.sourceContrahentName = sourceContrahentName;
        this.qty = qty;
        this.ageYears = ageYears;
        this.creationDate = creationDate;
        this.manufacturerCountryCode = manufacturerCountryCode;
        this.fitoQualificationCategory = fitoQualificationCategory;
        this.eppoCode = eppoCode;
        this.zpZone = zpZone;
        this.internalSource = internalSource;
        this.comments = comments;
        this.batchStatus = batchStatus;
        this.status = status;
        this.message = message;
    }

    public int getRowNumber() { return rowNumber; }
    public Integer getBatchId() { return batchId; }
    public String getInteriorBatchNo() { return interiorBatchNo; }
    public String getExteriorBatchNo() { return exteriorBatchNo; }
    public String getPlantSpecies() { return plantSpecies; }
    public String getPlantVariety() { return plantVariety; }
    public String getPlantRootstock() { return plantRootstock; }
    public String getSourceContrahentName() { return sourceContrahentName; }
    public int getQty() { return qty; }
    public int getAgeYears() { return ageYears; }
    public String getCreationDate() { return creationDate; }
    public String getManufacturerCountryCode() { return manufacturerCountryCode; }
    public String getFitoQualificationCategory() { return fitoQualificationCategory; }
    public String getEppoCode() { return eppoCode; }
    public String getZpZone() { return zpZone; }
    public boolean isInternalSource() { return internalSource; }
    public String getComments() { return comments; }
    public String getBatchStatus() { return batchStatus; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
