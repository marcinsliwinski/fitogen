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
import java.util.function.Supplier;

public final class ComboBoxAutoComplete {

    private static final String INTERNAL_CHANGE_KEY = "comboBoxAutoCompleteInternalChange";

    private ComboBoxAutoComplete() {
    }

    public static void bindEditable(ComboBox<String> comboBox, List<String> sourceValues) {
        bindEditable(comboBox, () -> sourceValues);
    }

    public static void bindEditable(ComboBox<String> comboBox, Supplier<List<String>> sourceSupplier) {
        if (comboBox == null) {
            return;
        }

        Supplier<List<String>> safeSupplier = sourceSupplier == null ? List::of : sourceSupplier;
        comboBox.setEditable(true);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        restoreStringItems(comboBox, safeSupplier);

        if (comboBox.getEditor() == null) {
            return;
        }

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (isInternalChange(comboBox)) {
                return;
            }
            if (!comboBox.isFocused() && !comboBox.getEditor().isFocused()) {
                return;
            }

            runInternalChange(comboBox, () -> {
                String typedText = safe(newValue);
                int caretPosition = comboBox.getEditor().getCaretPosition();
                comboBox.setItems(FXCollections.observableArrayList(filterStrings(safeSupplier.get(), typedText)));

                if (!Objects.equals(typedText, comboBox.getEditor().getText())) {
                    comboBox.getEditor().setText(typedText);
                }
                comboBox.getEditor().positionCaret(Math.min(caretPosition, comboBox.getEditor().getText().length()));
            });
        });

        comboBox.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                restoreStringItems(comboBox, safeSupplier);
                return;
            }
            restoreStringItems(comboBox, safeSupplier);
            commitEditorValue(comboBox);
        });

        comboBox.setOnMouseClicked(event -> restoreStringItems(comboBox, safeSupplier));

        comboBox.setOnAction(event -> {
            if (isInternalChange(comboBox)) {
                return;
            }

            String selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                runInternalChange(comboBox, () -> {
                    comboBox.setValue(selected);
                    comboBox.getEditor().setText(selected);
                    comboBox.getEditor().positionCaret(selected.length());
                    restoreStringItems(comboBox, safeSupplier);
                });
                return;
            }

            restoreStringItems(comboBox, safeSupplier);
            commitEditorValue(comboBox);
        });

        comboBox.getEditor().setOnAction(event -> {
            restoreStringItems(comboBox, safeSupplier);
            commitEditorValue(comboBox);
        });
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
        comboBox.setEditable(true);
        comboBox.setItems(FXCollections.observableArrayList(masterItems));
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : safe(textProvider.apply(object));
            }

            @Override
            public T fromString(String string) {
                return findBestMatch(masterItems, string, textProvider);
            }
        });

        if (comboBox.getEditor() == null) {
            return;
        }

        comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (isInternalChange(comboBox)) {
                return;
            }

            runInternalChange(comboBox, () -> {
                if (newValue != null) {
                    String displayText = safe(textProvider.apply(newValue));
                    if (!Objects.equals(comboBox.getEditor().getText(), displayText)) {
                        comboBox.getEditor().setText(displayText);
                    }
                    comboBox.getEditor().positionCaret(comboBox.getEditor().getText().length());
                } else if (!comboBox.isFocused() && !comboBox.getEditor().isFocused() && !comboBox.getEditor().getText().isBlank()) {
                    comboBox.getEditor().clear();
                }
            });
        });

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (isInternalChange(comboBox)) {
                return;
            }
            if (!comboBox.isFocused() && !comboBox.getEditor().isFocused()) {
                return;
            }

            T selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null && Objects.equals(safe(textProvider.apply(selected)), safe(newValue))) {
                return;
            }

            String typedText = safe(newValue);
            int caretPosition = comboBox.getEditor().getCaretPosition();
            String normalized = normalizeKey(typedText);
            ObservableList<T> filteredItems = normalized.isBlank()
                    ? FXCollections.observableArrayList(masterItems)
                    : masterItems.filtered(item -> normalizeKey(textProvider.apply(item)).contains(normalized));

            runInternalChange(comboBox, () -> {
                comboBox.setItems(filteredItems);
                if (!Objects.equals(comboBox.getEditor().getText(), typedText)) {
                    comboBox.getEditor().setText(typedText);
                }
                comboBox.getEditor().positionCaret(Math.min(caretPosition, comboBox.getEditor().getText().length()));
            });

            if (!comboBox.isShowing() && !filteredItems.isEmpty()) {
                comboBox.show();
            }
        });

        comboBox.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused || isInternalChange(comboBox)) {
                return;
            }
            restoreItems(comboBox, masterItems);
            commitSelection(comboBox, masterItems, textProvider);
        });

        comboBox.setOnMouseClicked(event -> restoreItems(comboBox, masterItems));

        comboBox.setOnAction(event -> {
            if (isInternalChange(comboBox)) {
                return;
            }
            restoreItems(comboBox, masterItems);
            commitSelection(comboBox, masterItems, textProvider);
        });

        comboBox.getEditor().setOnAction(event -> {
            if (isInternalChange(comboBox)) {
                return;
            }
            restoreItems(comboBox, masterItems);
            commitSelection(comboBox, masterItems, textProvider);
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

    private static void restoreStringItems(ComboBox<String> comboBox, Supplier<List<String>> sourceSupplier) {
        runInternalChange(comboBox, () -> comboBox.setItems(FXCollections.observableArrayList(normalize(sourceSupplier.get()))));
    }

    private static <T> void commitSelection(
            ComboBox<T> comboBox,
            ObservableList<T> masterItems,
            Function<T, String> textProvider
    ) {
        if (comboBox.getEditor() == null) {
            return;
        }

        runInternalChange(comboBox, () -> {
            String editorText = comboBox.getEditor().getText();
            T exactMatch = findBestMatch(masterItems, editorText, textProvider);
            if (exactMatch != null) {
                if (!Objects.equals(comboBox.getValue(), exactMatch)) {
                    comboBox.setValue(exactMatch);
                }
                if (!Objects.equals(comboBox.getSelectionModel().getSelectedItem(), exactMatch)) {
                    comboBox.getSelectionModel().select(exactMatch);
                }
                String displayText = safe(textProvider.apply(exactMatch));
                if (!Objects.equals(comboBox.getEditor().getText(), displayText)) {
                    comboBox.getEditor().setText(displayText);
                }
                comboBox.getEditor().positionCaret(comboBox.getEditor().getText().length());
                return;
            }

            T selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String displayText = safe(textProvider.apply(selected));
                if (!Objects.equals(comboBox.getEditor().getText(), displayText)) {
                    comboBox.getEditor().setText(displayText);
                }
                comboBox.getEditor().positionCaret(comboBox.getEditor().getText().length());
                return;
            }

            if (!Objects.equals(comboBox.getValue(), null)) {
                comboBox.setValue(null);
            }
            if (!comboBox.getEditor().getText().isBlank()) {
                comboBox.getEditor().setText("");
            }
        });
    }

    private static void commitEditorValue(ComboBox<String> comboBox) {
        if (comboBox.getEditor() == null) {
            return;
        }

        runInternalChange(comboBox, () -> {
            String text = comboBox.getEditor().getText();
            if (text == null) {
                comboBox.setValue(null);
                return;
            }

            String trimmed = text.trim();
            comboBox.setValue(trimmed.isBlank() ? null : trimmed);
            comboBox.getEditor().setText(trimmed);
            comboBox.getEditor().positionCaret(trimmed.length());
        });
    }

    private static boolean isInternalChange(ComboBox<?> comboBox) {
        return Boolean.TRUE.equals(comboBox.getProperties().get(INTERNAL_CHANGE_KEY));
    }

    private static void runInternalChange(ComboBox<?> comboBox, Runnable action) {
        comboBox.getProperties().put(INTERNAL_CHANGE_KEY, Boolean.TRUE);
        try {
            action.run();
        } finally {
            comboBox.getProperties().put(INTERNAL_CHANGE_KEY, Boolean.FALSE);
        }
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

    private static List<String> filterStrings(List<String> values, String typedValue) {
        String keyword = normalizeKey(typedValue);
        List<String> result = new ArrayList<>();

        for (String value : normalize(values)) {
            if (keyword.isBlank() || normalizeKey(value).contains(keyword)) {
                result.add(value);
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
