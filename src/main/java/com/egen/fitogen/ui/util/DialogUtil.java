package com.egen.fitogen.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public final class DialogUtil {

    private DialogUtil() {
    }

    public static boolean confirmDelete(String entityName) {
        return confirmAction("Usuwanie rekordu", "usunąć", entityName);
    }

    public static boolean confirmCancellation(String entityLabel, String entityName) {
        return confirmAction("Anulowanie rekordu", "anulować", buildEntityCaption(entityLabel, entityName));
    }

    public static boolean confirmAction(String header, String actionVerb, String entityName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Potwierdzenie");
        alert.setHeaderText(header);
        alert.setContentText("Czy na pewno chcesz " + actionVerb + ": " + normalizeEntityName(entityName) + "?");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static String buildEntityCaption(String entityLabel, String entityName) {
        String normalizedLabel = normalizeEntityName(entityLabel);
        String normalizedName = normalizeEntityName(entityName);

        if (normalizedName.equals("wybrany rekord")) {
            return normalizedLabel;
        }
        return normalizedLabel + " „" + normalizedName + "”";
    }

    private static String normalizeEntityName(String value) {
        if (value == null) {
            return "wybrany rekord";
        }

        String normalized = value.trim();
        return normalized.isBlank() ? "wybrany rekord" : normalized;
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showSuccess(String message) {
        showInfo("Sukces", message);
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Wystąpił błąd");
        alert.setContentText(message);
        alert.showAndWait();
    }
}