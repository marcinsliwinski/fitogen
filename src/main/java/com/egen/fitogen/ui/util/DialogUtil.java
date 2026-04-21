package com.egen.fitogen.ui.util;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

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

    public static boolean confirmDiscardChanges(String formLabel) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Niezapisane zmiany");
        alert.setHeaderText("Zamknąć formularz bez zapisu?");
        alert.setContentText(buildDiscardChangesMessage(formLabel));

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void applyReadableDecisionDialog(Alert alert, double minWidth, double prefWidth, double minButtonWidth, double prefButtonWidth) {
        if (alert == null || alert.getDialogPane() == null) {
            return;
        }

        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(Math.max(640, minWidth));
        alert.getDialogPane().setPrefWidth(Math.max(alert.getDialogPane().getMinWidth(), prefWidth));
        alert.getDialogPane().setMinHeight(240);

        Node contentNode = alert.getDialogPane().lookup(".content.label");
        if (contentNode instanceof Label label) {
            label.setWrapText(true);
        }

        for (ButtonType buttonType : alert.getButtonTypes()) {
            Node node = alert.getDialogPane().lookupButton(buttonType);
            if (node instanceof Button button) {
                button.setMinWidth(Math.max(150, minButtonWidth));
                button.setPrefWidth(Math.max(button.getMinWidth(), prefButtonWidth));
                button.setWrapText(true);
            }
        }
    }

    private static String buildDiscardChangesMessage(String formLabel) {
        String normalized = normalizeEntityName(formLabel);
        if (normalized.equals("wybrany rekord")) {
            return "W formularzu są niezapisane zmiany. Czy chcesz zamknąć okno bez zapisywania?";
        }
        return "W formularzu „" + normalized + "” są niezapisane zmiany. Czy chcesz zamknąć okno bez zapisywania?";
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