package com.egen.fitogen.ui.util;

public final class UiTextUtil {

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
}
