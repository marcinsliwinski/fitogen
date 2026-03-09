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
    private String issuedBy;     // kto wystawił dokument
    private boolean passport;    // czy dokument zawiera paszport
    private String comments;     // uwagi
    private LocalDate creationDate; // data wystawienia dokumentu

    // dodatkowy konstruktor bez id, np. do tworzenia nowych dokumentów
    public Document(int plantBatchId, int contrahentId, String documentType, String issuedBy, boolean passport, String comments, LocalDate creationDate) {
        this.plantBatchId = plantBatchId;
        this.contrahentId = contrahentId;
        this.documentType = documentType;
        this.issuedBy = issuedBy;
        this.passport = passport;
        this.comments = comments;
        this.creationDate = creationDate;
    }
}