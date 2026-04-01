package com.egen.fitogen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class DocumentItem {

    private int id;
    private int documentId;
    private int plantBatchId;
    private int qty;
    private boolean passportRequired;
}