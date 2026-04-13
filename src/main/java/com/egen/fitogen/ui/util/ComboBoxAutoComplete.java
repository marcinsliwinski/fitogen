package com.egen.fitogen.ui.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

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

    public static <T> void bindSelectionAutocomplete(
            ComboBox<T> comboBox,
            List<T> sourceValues,
            Function<T, String> textProvider
    ) {
        if (comboBox == null || textProvider == null) {
            return;
        }

        ObservableList<T> masterItems = FXCollections.observableArrayList(sourceValues == null ? List.of() : sourceValues);
        boolean[] internalChange = {false};
        comboBox.setEditable(true);
        comboBox.setItems(FXCollections.observableArrayList(masterItems));
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(T object) {
                return safe(textProvider.apply(object));
            }

            @Override
            public T fromString(String string) {
                return findBestMatch(masterItems, string, textProvider);
            }
        });

        if (comboBox.getEditor() == null) {
            return;
        }

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (internalChange[0] || !comboBox.isFocused()) {
                return;
            }

            T selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null && Objects.equals(safe(textProvider.apply(selected)), safe(newValue))) {
                return;
            }

            String normalized = normalizeKey(newValue);
            if (normalized.isBlank()) {
                comboBox.setItems(FXCollections.observableArrayList(masterItems));
            } else {
                comboBox.setItems(masterItems.filtered(item -> normalizeKey(textProvider.apply(item)).contains(normalized)));
            }

            if (!comboBox.isShowing()) {
                comboBox.show();
            }
        });

        comboBox.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused || internalChange[0]) {
                return;
            }
            restoreItems(comboBox, masterItems);
            commitSelection(comboBox, masterItems, textProvider, internalChange);
        });

        comboBox.setOnAction(event -> {
            if (internalChange[0]) {
                return;
            }
            restoreItems(comboBox, masterItems);
            commitSelection(comboBox, masterItems, textProvider, internalChange);
        });
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

    private static <T> void restoreItems(ComboBox<T> comboBox, ObservableList<T> masterItems) {
        comboBox.setItems(FXCollections.observableArrayList(masterItems));
    }

    private static <T> void commitSelection(
            ComboBox<T> comboBox,
            ObservableList<T> masterItems,
            Function<T, String> textProvider,
            boolean[] internalChange
    ) {
        if (comboBox.getEditor() == null) {
            return;
        }

        internalChange[0] = true;
        try {
            String editorText = comboBox.getEditor().getText();
            T exactMatch = findBestMatch(masterItems, editorText, textProvider);
            if (exactMatch != null) {
                comboBox.setValue(exactMatch);
                comboBox.getSelectionModel().select(exactMatch);
                comboBox.getEditor().setText(safe(textProvider.apply(exactMatch)));
                return;
            }

            T selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                comboBox.getEditor().setText(safe(textProvider.apply(selected)));
                return;
            }

            comboBox.getEditor().setText("");
        } finally {
            internalChange[0] = false;
        }
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

    private static <T> T findBestMatch(List<T> items, String value, Function<T, String> textProvider) {
        String normalized = normalizeKey(value);
        if (normalized.isBlank()) {
            return null;
        }

        for (T item : items) {
            if (normalizeKey(textProvider.apply(item)).equals(normalized)) {
                return item;
            }
        }
        return null;
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

    private static String normalizeKey(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
