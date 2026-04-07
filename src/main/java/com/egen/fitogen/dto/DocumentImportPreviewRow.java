package com.egen.fitogen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentImportPreviewRow {
    private int rowNumber;
    private String documentNumber;
    private String documentType;
    private String issueDate;
    private String status;
    private String contrahentName;
    private String contrahentCountryCode;
    private String createdBy;
    private int lineNo;
    private String plantBatchNumber;
    private String plantBatchId;
    private int qty;
    private boolean passportRequired;
    private String rowStatus;
    private String message;
}
