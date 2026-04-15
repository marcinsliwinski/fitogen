package com.egen.fitogen.dto;

public class EppoDictionaryImportPreviewRow {
    private final int rowNumber;
    private final String relationType;
    private final String eppoCode;
    private final String speciesName;
    private final String latinSpeciesName;
    private final String zoneCode;
    private final String zoneName;
    private final String countryCode;
    private final boolean passportRequired;
    private final String codeStatus;
    private final String zoneStatus;
    private final String status;
    private final String message;

    public EppoDictionaryImportPreviewRow(
            int rowNumber,
            String relationType,
            String eppoCode,
            String speciesName,
            String latinSpeciesName,
            String zoneCode,
            String zoneName,
            String countryCode,
            boolean passportRequired,
            String codeStatus,
            String zoneStatus,
            String status,
            String message
    ) {
        this.rowNumber = rowNumber;
        this.relationType = relationType;
        this.eppoCode = eppoCode;
        this.speciesName = speciesName;
        this.latinSpeciesName = latinSpeciesName;
        this.zoneCode = zoneCode;
        this.zoneName = zoneName;
        this.countryCode = countryCode;
        this.passportRequired = passportRequired;
        this.codeStatus = codeStatus;
        this.zoneStatus = zoneStatus;
        this.status = status;
        this.message = message;
    }

    public int getRowNumber() { return rowNumber; }
    public String getRelationType() { return relationType; }
    public String getEppoCode() { return eppoCode; }
    public String getSpeciesName() { return speciesName; }
    public String getLatinSpeciesName() { return latinSpeciesName; }
    public String getZoneCode() { return zoneCode; }
    public String getZoneName() { return zoneName; }
    public String getCountryCode() { return countryCode; }
    public boolean isPassportRequired() { return passportRequired; }
    public String getCodeStatus() { return codeStatus; }
    public String getZoneStatus() { return zoneStatus; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
