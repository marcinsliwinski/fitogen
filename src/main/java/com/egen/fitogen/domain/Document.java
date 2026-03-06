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
    private int contrahentId;
    private int plantBatchId;
    private LocalDate issueDate;
    private boolean passportRequired;
    private String comments;
    private String issuedBy;

    public Document(int id) {
        this.id = id;
    }

    // Getters and Setters
}