package com.egen.fitogen.model;

import lombok.Data;

@Data
public class Plant {

    public static final String DEFAULT_DOCUMENT_NURSERY_SUPPLIER = "NURSERY_SUPPLIER_DOCUMENT";
    public static final String DEFAULT_DOCUMENT_SUPPLIER = "SUPPLIER_DOCUMENT";

    private int id;
    private String species;
    private String variety;
    private String rootstock;
    private String latinSpeciesName;
    private String eppoCode;
    private boolean passportRequired;
    private String visibilityStatus; // Używany / Nieużywany
    private String defaultDocumentType;

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
        this(
                id,
                species,
                variety,
                rootstock,
                latinSpeciesName,
                eppoCode,
                passportRequired,
                visibilityStatus,
                null
        );
    }

    public Plant(
            int id,
            String species,
            String variety,
            String rootstock,
            String latinSpeciesName,
            String eppoCode,
            boolean passportRequired,
            String visibilityStatus,
            String defaultDocumentType
    ) {
        this.id = id;
        this.species = species;
        this.variety = variety;
        this.rootstock = rootstock;
        this.latinSpeciesName = latinSpeciesName;
        this.eppoCode = eppoCode;
        this.passportRequired = passportRequired;
        this.visibilityStatus = visibilityStatus;
        this.defaultDocumentType = defaultDocumentType;
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