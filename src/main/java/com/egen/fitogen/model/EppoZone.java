package com.egen.fitogen.model;

import lombok.Data;

@Data
public class EppoZone {

    private int id;
    private String code;
    private String name;
    private String countryCode;
    private String status;

    public EppoZone(
            int id,
            String code,
            String name,
            String countryCode,
            String status
    ) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.countryCode = countryCode;
        this.status = status;
    }
}