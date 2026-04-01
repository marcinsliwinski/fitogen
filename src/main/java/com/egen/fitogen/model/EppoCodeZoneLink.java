package com.egen.fitogen.model;

import lombok.Data;

@Data
public class EppoCodeZoneLink {

    private int id;
    private int eppoCodeId;
    private int eppoZoneId;

    public EppoCodeZoneLink(int id, int eppoCodeId, int eppoZoneId) {
        this.id = id;
        this.eppoCodeId = eppoCodeId;
        this.eppoZoneId = eppoZoneId;
    }
}