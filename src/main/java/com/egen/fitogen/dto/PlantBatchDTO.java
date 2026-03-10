package com.egen.fitogen.dto;

import lombok.Data;

@Data
public class PlantBatchDTO {

    private int id;
    private String interiorBatchNo;
    private String species;
    private String variety;
    private String contrahentName;
    private int qty;
    private String creationDate;
}