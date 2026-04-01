package com.egen.fitogen.ui.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

import java.util.ArrayList;
import java.util.List;

public final class ComboBoxAutoComplete {

    private ComboBoxAutoComplete() {
    }

    public static void bindEditable(ComboBox<String> comboBox, List<String> sourceValues) {
        if (comboBox == null) {
            return;
        }

        ObservableList<String> masterItems = FXCollections.observableArrayList(normalize(sourceValues));
        comboBox.setItems(masterItems);
        comboBox.setEditable(true);

        comboBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitEditorValue(comboBox);
            }
        });

        comboBox.setOnAction(event -> commitEditorValue(comboBox));
    }

    public static void refreshSource(ComboBox<String> comboBox, List<String> sourceValues) {
        if (comboBox == null) {
            return;
        }

        comboBox.setItems(FXCollections.observableArrayList(normalize(sourceValues)));
    }

    public static String getCommittedValue(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return null;
        }

        String editorValue = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
        if (editorValue != null && !editorValue.isBlank()) {
            return editorValue.trim();
        }

        String selected = comboBox.getValue();
        return selected == null || selected.isBlank() ? null : selected.trim();
    }

    private static void commitEditorValue(ComboBox<String> comboBox) {
        if (comboBox.getEditor() == null) {
            return;
        }

        String text = comboBox.getEditor().getText();
        if (text == null) {
            comboBox.setValue(null);
            return;
        }

        String trimmed = text.trim();
        comboBox.setValue(trimmed.isBlank() ? null : trimmed);
    }

    private static List<String> normalize(List<String> values) {
        if (values == null) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (!result.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }
}