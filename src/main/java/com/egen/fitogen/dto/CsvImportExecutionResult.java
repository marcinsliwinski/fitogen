package com.egen.fitogen.dto;

import java.util.List;

public class CsvImportExecutionResult {
    private final String sourceName;
    private final int totalRowsCount;
    private final int addedCount;
    private final int skippedCount;
    private final int rejectedCount;
    private final List<String> problems;

    public CsvImportExecutionResult(String sourceName,
                                    int totalRowsCount,
                                    int addedCount,
                                    int skippedCount,
                                    int rejectedCount,
                                    List<String> problems) {
        this.sourceName = sourceName;
        this.totalRowsCount = totalRowsCount;
        this.addedCount = addedCount;
        this.skippedCount = skippedCount;
        this.rejectedCount = rejectedCount;
        this.problems = problems == null ? List.of() : List.copyOf(problems);
    }

    public String getSourceName() {
        return sourceName;
    }

    public int getTotalRowsCount() {
        return totalRowsCount;
    }

    public int getAddedCount() {
        return addedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public List<String> getProblems() {
        return problems;
    }
}
