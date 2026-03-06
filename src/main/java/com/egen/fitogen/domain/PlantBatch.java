package com.egen.fitogen.domain;
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
    private String comments;

    public PlantBatch(int id, int plantId, String interiorBatchNo) {
        this.id = id;
        this.plantId = plantId;
        this.interiorBatchNo = interiorBatchNo;
    }

    // Getters and Setters
}