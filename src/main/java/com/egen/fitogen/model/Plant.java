package com.egen.fitogen.model;

import lombok.Data;

@Data
public class Plant {

    private int id;
    private String species;
    private String variety;
    private String rootstock;
    private String latinSpeciesName;
    private String eppoCode;
    private boolean passportRequired;
    private String visibilityStatus; // Używany / Nieużywany

    public Plant(
            int id,
            String species,
            String variety,
            String rootstock,
            String latinSpeciesName,
            String eppoCode,
            boolean passportRequired,
            String visibilityStatus
    ) {
        this.id = id;
        this.species = species;
        this.variety = variety;
        this.rootstock = rootstock;
        this.latinSpeciesName = latinSpeciesName;
        this.eppoCode = eppoCode;
        this.passportRequired = passportRequired;
        this.visibilityStatus = visibilityStatus;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (species != null && !species.isBlank()) {
            sb.append(species.trim());
        }

        if (rootstock != null && !rootstock.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(rootstock.trim());
        }

        if (variety != null && !variety.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(variety.trim());
        }

        return sb.toString();
    }
}