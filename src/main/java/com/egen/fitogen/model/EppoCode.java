package com.egen.fitogen.model;

import lombok.Data;

@Data
public class EppoCode {

    private int id;
    private String code;

    /**
     * New reference model for EPPO:
     * - speciesName mirrors Plant.species
     * - latinSpeciesName mirrors Plant.latinSpeciesName
     */
    private String speciesName;
    private String latinSpeciesName;

    /**
     * Legacy compatibility fields kept temporarily during rebuild.
     * They should be phased out from UI and business logic in next steps.
     */
    private String scientificName;
    private String commonName;

    private boolean passportRequired;
    private String status;

    public EppoCode(
            int id,
            String code,
            String speciesName,
            String latinSpeciesName,
            String scientificName,
            String commonName,
            boolean passportRequired,
            String status
    ) {
        this.id = id;
        this.code = code;
        this.speciesName = speciesName;
        this.latinSpeciesName = latinSpeciesName;
        this.scientificName = scientificName;
        this.commonName = commonName;
        this.passportRequired = passportRequired;
        this.status = status;
    }

    public EppoCode(
            int id,
            String code,
            String scientificName,
            String commonName,
            boolean passportRequired,
            String status
    ) {
        this(
                id,
                code,
                commonName,
                scientificName,
                scientificName,
                commonName,
                passportRequired,
                status
        );
    }

    public String getDisplaySpeciesName() {
        if (speciesName != null && !speciesName.isBlank()) {
            return speciesName;
        }
        return commonName;
    }

    public String getDisplayLatinSpeciesName() {
        if (latinSpeciesName != null && !latinSpeciesName.isBlank()) {
            return latinSpeciesName;
        }
        return scientificName;
    }
}
