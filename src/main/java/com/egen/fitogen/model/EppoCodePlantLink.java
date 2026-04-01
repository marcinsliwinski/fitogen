package com.egen.fitogen.model;

import lombok.Data;

@Data
public class EppoCodePlantLink {

    private int id;
    private int eppoCodeId;
    private int plantId;

    public EppoCodePlantLink(int id, int eppoCodeId, int plantId) {
        this.id = id;
        this.eppoCodeId = eppoCodeId;
        this.plantId = plantId;
    }
}