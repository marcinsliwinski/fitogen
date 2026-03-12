package com.egen.fitogen.domain;
//import lombok.AllArgsConstructor;
import lombok.Data;
//import lombok.NoArgsConstructor;

@Data
//@NoArgsConstructor
//@AllArgsConstructor

public class Plant {

    private int id;
    private String species;
    private String variety;
    private String rootstock;
    private String latinSpeciesName;
    private String visibilityStatus; // IMPORT / ACTIVE

    public Plant(int id, String species, String variety, String rootstock, String latinSpeciesName, String visibilityStatus) {
        this.id = id;
        this.species = species;
        this.variety = variety;
        this.rootstock = rootstock;
        this.latinSpeciesName = latinSpeciesName;
        this.visibilityStatus = visibilityStatus;
    }
    @Override
    public String toString() {
        return species + " " + variety;
    }
}