package com.egen.fitogen.dto;

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
    private List<DocumentItemDTO> items;
}