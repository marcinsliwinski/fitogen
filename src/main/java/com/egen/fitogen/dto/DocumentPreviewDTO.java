package com.egen.fitogen.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class DocumentPreviewDTO {
    private int documentId;
    private String documentNumber;
    private String documentType;
    private String statusLabel;
    private boolean cancelled;

    private LocalDate issueDate;
    private String issueDateLabel;
    private String issuePlaceLabel;
    private String createdBy;
    private String comments;

    private String contrahentName;
    private String contrahentAddress;

    private String issuerName;
    private String issuerAddressLine1;
    private String issuerAddressLine2;
    private String issuerPhytosanitaryNumber;

    private String customerName;
    private String customerAddressLine1;
    private String customerAddressLine2;
    private String customerPhytosanitaryNumber;

    private int totalQty;

    private List<DocumentPreviewItemDTO> items = new ArrayList<>();
}
