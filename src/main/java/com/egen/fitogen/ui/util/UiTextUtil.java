package com.egen.fitogen.ui.util;

import java.util.List;

public final class UiTextUtil {

    public static final String NL = "\n";
    public static final String DOUBLE_NL = NL + NL;

    private UiTextUtil() {
    }

    public static String buildQuotedFilterSuffix(String label, String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return "";
        }

        return " " + label + ": \"" + normalized + "\".";
    }

    public static void appendSectionHeader(StringBuilder builder, String header) {
        builder.append(header).append(NL);
    }

    public static void appendSummaryLine(StringBuilder builder, String label, Object value) {
        builder.append("- ").append(label).append(": ").append(value).append(NL);
    }

    public static void appendLabelValue(StringBuilder builder, String label, Object value) {
        builder.append(label).append(": ").append(value).append(NL);
    }

    public static void appendEmptyLine(StringBuilder builder) {
        builder.append(NL);
    }

    public static void appendIssuesSection(StringBuilder builder, String header, List<String> issues) {
        if (issues == null || issues.isEmpty()) {
            return;
        }

        appendEmptyLine(builder);
        appendSectionHeader(builder, header);
        for (String issue : issues) {
            builder.append("- ").append(issue).append(NL);
        }
    }

    public static String buildEmptyPreviewText(String entityLabel, String details) {
        return "Brak preview importu " + entityLabel + "." + DOUBLE_NL + details;
    }
}
