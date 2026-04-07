package com.egen.fitogen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentImportPreviewResult {
    private String sourceName;
    private char delimiter;
    private List<String> resolvedHeaders;
    private List<DocumentImportPreviewRow> rows;
    private int totalRowsCount;
    private int documentCount;
    private int newRowsCount;
    private int matchingExistingCount;
    private int duplicateInFileCount;
    private int invalidRowsCount;
}
