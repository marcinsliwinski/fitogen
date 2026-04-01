package com.egen.fitogen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantBatch {

    private int id;
    private int plantId;
    private String interiorBatchNo;
    private String exteriorBatchNo;
    private int qty;
    private LocalDate creationDate;
    private String manufacturerCountryCode;
    private String fitoQualificationCategory;
    private String eppoCode;
    private String zpZone;
    private int contrahentId;
    private boolean internalSource;
    private String comments;
    private PlantBatchStatus status = PlantBatchStatus.ACTIVE;

    public PlantBatch(int id, int plantId, String interiorBatchNo) {
        this.id = id;
        this.plantId = plantId;
        this.interiorBatchNo = interiorBatchNo;
        this.status = PlantBatchStatus.ACTIVE;
    }

    @Override
    public String toString() {
        if (interiorBatchNo != null && !interiorBatchNo.isBlank()) {
            return interiorBatchNo;
        }
        if (exteriorBatchNo != null && !exteriorBatchNo.isBlank()) {
            return exteriorBatchNo;
        }
        return "Partia ID: " + id;
    }
}