package com.egen.fitogen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    private int id;
    private int contrahentId;
    private String documentType;
    private String createdBy;
    private String comments;
    private LocalDate issueDate;
    private String documentNumber;
    private boolean printPassports;
    private DocumentStatus status = DocumentStatus.ACTIVE;
}