package com.egen.fitogen.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    private int id;
    private int plantBatchId;   // powiązanie z PlantBatch
    private int contrahentId;   // powiązanie z Contrahent
    private String documentType; // np. "Dokument dostawcy" lub "Szkółkarski"
    private String createdBy;     // kto wystawił dokument
    private boolean passport;    // czy dokument zawiera paszport
    private String comments;     // uwagi
    private LocalDate issueDate; // data wystawienia dokumentu
    private String documentNumber;

    // construuctor without id & documentNumber
    public Document(int plantBatchId, int contrahentId, String documentType, String createdBy, boolean passport, String comments, LocalDate issueDate) {
        this.plantBatchId = plantBatchId;
        this.contrahentId = contrahentId;
        this.documentType = documentType;
        this.createdBy = createdBy;
        this.passport = passport;
        this.comments = comments;
        this.issueDate = issueDate;
    }
}