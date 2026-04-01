package com.egen.fitogen.dto;

import com.egen.fitogen.model.DocumentStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DocumentDTO {

    private int id;
    private String documentNumber;
    private String documentType;
    private LocalDate issueDate;
    private int contrahentId;
    private String createdBy;
    private String comments;
    private DocumentStatus status = DocumentStatus.ACTIVE;
    private List<DocumentItemDTO> items;
}