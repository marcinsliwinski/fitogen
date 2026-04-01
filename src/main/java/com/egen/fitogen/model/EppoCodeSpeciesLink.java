package com.egen.fitogen.model;

import lombok.Data;

@Data
public class EppoCodeSpeciesLink {

    private int id;
    private int eppoCodeId;
    private String speciesName;
    private String latinSpeciesName;

    public EppoCodeSpeciesLink(int id, int eppoCodeId, String speciesName, String latinSpeciesName) {
        this.id = id;
        this.eppoCodeId = eppoCodeId;
        this.speciesName = speciesName;
        this.latinSpeciesName = latinSpeciesName;
    }
}
