package com.egen.fitogen.dto;

import lombok.Data;

@Data
public class DocumentPreviewItemDTO {
    private int lp;
    private String plantName;
    private String batchNumber;
    private String batchAgeLabel;
    private String batchCategoryLabel;
    private int qty;
    private String passportLabel;
    private boolean summaryRow;
}
