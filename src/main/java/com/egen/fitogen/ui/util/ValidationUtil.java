package com.egen.fitogen.ui.util;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static String requireText(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " jest wymagane.");
        }
        return value.trim();
    }

    public static int requirePositiveInt(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " jest wymagane.");
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(fieldLabel + " musi być większe od zera.");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldLabel + " musi być liczbą całkowitą.");
        }
    }

    public static String optionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static void requireSelection(Object value, String fieldLabel) {
        if (value == null) {
            throw new IllegalArgumentException("Wybierz: " + fieldLabel + ".");
        }
    }

    public static void requireNotNull(Object value, String fieldLabel) {
        if (value == null) {
            throw new IllegalArgumentException(fieldLabel + " jest wymagane.");
        }
    }
}