package com.egen.fitogen.dto;

import java.util.List;

public class SubjectDataImportPreviewResult {
    private final String sourceName;
    private final char delimiter;
    private final List<String> resolvedHeaders;
    private final List<SubjectDataImportPreviewRow> rows;
    private final int newRowsCount;
    private final int updateRowsCount;
    private final int matchingExistingCount;
    private final int duplicateInFileCount;
    private final int invalidRowsCount;

    public SubjectDataImportPreviewResult(String sourceName,
                                          char delimiter,
                                          List<String> resolvedHeaders,
                                          List<SubjectDataImportPreviewRow> rows,
                                          int newRowsCount,
                                          int updateRowsCount,
                                          int matchingExistingCount,
                                          int duplicateInFileCount,
                                          int invalidRowsCount) {
        this.sourceName = sourceName;
        this.delimiter = delimiter;
        this.resolvedHeaders = resolvedHeaders == null ? List.of() : List.copyOf(resolvedHeaders);
        this.rows = rows == null ? List.of() : List.copyOf(rows);
        this.newRowsCount = newRowsCount;
        this.updateRowsCount = updateRowsCount;
        this.matchingExistingCount = matchingExistingCount;
        this.duplicateInFileCount = duplicateInFileCount;
        this.invalidRowsCount = invalidRowsCount;
    }

    public String getSourceName() {
        return sourceName;
    }

    public char getDelimiter() {
        return delimiter;
    }

    public List<String> getResolvedHeaders() {
        return resolvedHeaders;
    }

    public List<SubjectDataImportPreviewRow> getRows() {
        return rows;
    }

    public int getNewRowsCount() {
        return newRowsCount;
    }

    public int getUpdateRowsCount() {
        return updateRowsCount;
    }

    public int getMatchingExistingCount() {
        return matchingExistingCount;
    }

    public int getDuplicateInFileCount() {
        return duplicateInFileCount;
    }

    public int getInvalidRowsCount() {
        return invalidRowsCount;
    }

    public int getTotalRowsCount() {
        return rows.size();
    }

    public int getImportableRowsCount() {
        return newRowsCount + updateRowsCount;
    }
}
