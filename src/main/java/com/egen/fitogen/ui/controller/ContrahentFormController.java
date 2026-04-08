package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.service.ContrahentService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ValidationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

public class ContrahentFormController {

    @FXML private javafx.scene.control.TextField nameField;
    @FXML private ComboBox<String> countryField;
    @FXML private ComboBox<String> countryCodeField;
    @FXML private javafx.scene.control.TextField postalCodeField;
    @FXML private javafx.scene.control.TextField cityField;
    @FXML private javafx.scene.control.TextField streetField;
    @FXML private javafx.scene.control.TextField phytosanitaryNumberField;
    @FXML private CheckBox supplierCheckBox;
    @FXML private CheckBox clientCheckBox;

    private final ContrahentService contrahentService = AppContext.getContrahentService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();

    private Contrahent contrahent;
    private boolean updatingCountryFields;
    private String initialSnapshot = "";
    private boolean saveCompleted;
    private boolean closeConfirmed;
    private boolean closeGuardInstalled;

    @FXML
    public void initialize() {
        configureEditableCombo(countryField);
        configureEditableCombo(countryCodeField);

        installTextTrimSupport(nameField);
        installTextTrimSupport(postalCodeField);
        installTextTrimSupport(cityField);
        installTextTrimSupport(streetField);
        installTextTrimSupport(phytosanitaryNumberField);
        installComboTrimSupport(countryField);
        installComboTrimSupport(countryCodeField);
        installUppercaseEditorSupport(countryCodeField);
        installFieldTooltips();

        attachAutocomplete(countryField, countryDirectoryService::getCountries);
        attachAutocomplete(countryCodeField, countryDirectoryService::getCodes);

        if (countryField != null && countryField.getEditor() != null) {
            countryField.getEditor().textProperty().addListener((obs, oldVal, newVal) -> syncCodeFromCountry());
        }

        if (countryCodeField != null && countryCodeField.getEditor() != null) {
            countryCodeField.getEditor().textProperty().addListener((obs, oldVal, newVal) -> syncCountryFromCode());
        }

        countryField.valueProperty().addListener((obs, oldVal, newVal) -> syncCodeFromCountry());
        countryCodeField.valueProperty().addListener((obs, oldVal, newVal) -> syncCountryFromCode());

        Platform.runLater(() -> {
            installWindowCloseGuard();
            markCurrentStateAsClean();
        });
    }

    public void setContrahent(Contrahent contrahent) {
        this.contrahent = contrahent;

        nameField.setText(contrahent.getName());
        setComboValue(countryField, contrahent.getCountry());
        setComboValue(countryCodeField, normalizeCountryCode(contrahent.getCountryCode()));
        postalCodeField.setText(contrahent.getPostalCode());
        cityField.setText(contrahent.getCity());
        streetField.setText(contrahent.getStreet());
        phytosanitaryNumberField.setText(contrahent.getPhytosanitaryNumber());
        supplierCheckBox.setSelected(contrahent.isSupplier());
        clientCheckBox.setSelected(contrahent.isClient());
        markCurrentStateAsClean();
    }

    @FXML
    private void saveContrahent() {
        try {
            String name = ValidationUtil.requireText(nameField.getText(), "Nazwa");

            Contrahent entity = contrahent != null ? contrahent : new Contrahent();
            entity.setName(name);
            entity.setCountry(ValidationUtil.optionalText(getComboValue(countryField)));
            entity.setCountryCode(ValidationUtil.optionalText(normalizeCountryCode(getComboValue(countryCodeField))));
            entity.setPostalCode(ValidationUtil.optionalText(postalCodeField.getText()));
            entity.setCity(ValidationUtil.optionalText(cityField.getText()));
            entity.setStreet(ValidationUtil.optionalText(streetField.getText()));
            entity.setPhytosanitaryNumber(ValidationUtil.optionalText(phytosanitaryNumberField.getText()));
            entity.setSupplier(supplierCheckBox.isSelected());
            entity.setClient(clientCheckBox.isSelected());

            if (contrahent == null) {
                contrahentService.addContrahent(entity);
                DialogUtil.showSuccess("Kontrahent został dodany.");
            } else {
                contrahentService.updateContrahent(entity);
                DialogUtil.showSuccess("Kontrahent został zaktualizowany.");
            }

            saveCompleted = true;
            closeConfirmed = true;
            close();
        } catch (IllegalArgumentException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu", "Nie udało się zapisać kontrahenta.");
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private void syncCodeFromCountry() {
        if (updatingCountryFields) {
            return;
        }

        String country = getComboValue(countryField);
        if (country == null || country.isBlank()) {
            return;
        }

        String detectedCode = countryDirectoryService.findCodeByCountry(country);
        if (detectedCode == null || detectedCode.isBlank()) {
            return;
        }

        updatingCountryFields = true;
        try {
            setComboValue(countryCodeField, detectedCode);
        } finally {
            updatingCountryFields = false;
        }
    }

    private void syncCountryFromCode() {
        if (updatingCountryFields) {
            return;
        }

        String code = normalizeCountryCode(getComboValue(countryCodeField));
        if (code == null || code.isBlank()) {
            return;
        }

        String detectedCountry = countryDirectoryService.findCountryByCode(code);
        if (detectedCountry == null || detectedCountry.isBlank()) {
            return;
        }

        updatingCountryFields = true;
        try {
            setComboValue(countryField, detectedCountry);
        } finally {
            updatingCountryFields = false;
        }
    }


    private void configureEditableCombo(ComboBox<String> comboBox) {
        comboBox.setEditable(true);
        comboBox.setMaxWidth(Double.MAX_VALUE);
    }

    private void installFieldTooltips() {
        setTooltip(nameField, "Pole wymagane. Najlepiej wpisywać pełną nazwę kontrahenta tak, jak ma pojawiać się na dokumentach.");
        setTooltip(countryField, "Możesz wpisać własną nazwę kraju albo skorzystać ze wspólnego słownika krajów.");
        setTooltip(countryCodeField, "Kod kraju jest synchronizowany ze słownikiem krajów. Wpis ręczny zostanie zapisany wielkimi literami.");
        setTooltip(postalCodeField, "Pole opcjonalne. Możesz wpisać kod pocztowy używany na dokumentach i etykietach.");
        setTooltip(cityField, "Pole opcjonalne. Ułatwia identyfikację adresu kontrahenta.");
        setTooltip(streetField, "Pole opcjonalne. Możesz wpisać ulicę i numer, jeśli mają pojawiać się w danych kontrahenta.");
        setTooltip(phytosanitaryNumberField, "Pole opcjonalne. Wpisz numer fitosanitarny, jeśli kontrahent go posiada i ma być widoczny na dokumentach.");
        setTooltip(supplierCheckBox, "Zaznacz, jeśli kontrahent występuje jako dostawca partii roślin.");
        setTooltip(clientCheckBox, "Zaznacz, jeśli kontrahent występuje jako odbiorca lub klient na dokumentach.");
    }

    private void installTextTrimSupport(TextField field) {
        if (field == null) {
            return;
        }

        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                field.setText(normalizeText(field.getText()));
            }
        });
    }

    private void installComboTrimSupport(ComboBox<String> comboBox) {
        if (comboBox == null || comboBox.getEditor() == null) {
            return;
        }

        comboBox.getEditor().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                comboBox.getEditor().setText(normalizeText(comboBox.getEditor().getText()));
            }
        });
    }

    private void installUppercaseEditorSupport(ComboBox<String> comboBox) {
        if (comboBox == null || comboBox.getEditor() == null) {
            return;
        }

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            String normalized = normalizeCountryCode(newValue);
            if (!Objects.equals(newValue, normalized)) {
                comboBox.getEditor().setText(normalized);
                comboBox.getEditor().positionCaret(normalized.length());
            }
        });
    }

    private void setTooltip(javafx.scene.control.Control control, String text) {
        if (control != null && text != null && !text.isBlank()) {
            control.setTooltip(new Tooltip(text));
        }
    }

    private void attachAutocomplete(ComboBox<String> comboBox, Supplier<List<String>> sourceSupplier) {
        comboBox.setItems(FXCollections.observableArrayList(sourceSupplier.get()));

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (isAutocompleteUpdating(comboBox)) {
                return;
            }
            if (!comboBox.isFocused() && !comboBox.getEditor().isFocused()) {
                return;
            }

            runAutocompleteUpdate(comboBox, () -> {
                String typedText = newValue == null ? "" : newValue;
                int caretPosition = comboBox.getEditor().getCaretPosition();
                List<String> filtered = filterValues(sourceSupplier.get(), typedText);
                comboBox.setItems(FXCollections.observableArrayList(filtered));

                if (!Objects.equals(typedText, comboBox.getEditor().getText())) {
                    comboBox.getEditor().setText(typedText);
                }
                comboBox.getEditor().positionCaret(Math.min(caretPosition, comboBox.getEditor().getText().length()));
            });
        });

        comboBox.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                runAutocompleteUpdate(comboBox, () ->
                        comboBox.setItems(FXCollections.observableArrayList(sourceSupplier.get()))
                );
            }
        });

        comboBox.setOnMouseClicked(event ->
                runAutocompleteUpdate(comboBox, () ->
                        comboBox.setItems(FXCollections.observableArrayList(sourceSupplier.get()))
                )
        );

        comboBox.setOnAction(event -> {
            String selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                runAutocompleteUpdate(comboBox, () -> comboBox.getEditor().setText(selected));
            }
        });
    }

    private boolean isAutocompleteUpdating(ComboBox<String> comboBox) {
        return Boolean.TRUE.equals(comboBox.getProperties().get("autocompleteUpdating"));
    }

    private void runAutocompleteUpdate(ComboBox<String> comboBox, Runnable action) {
        comboBox.getProperties().put("autocompleteUpdating", Boolean.TRUE);
        try {
            action.run();
        } finally {
            comboBox.getProperties().put("autocompleteUpdating", Boolean.FALSE);
        }
    }

    private List<String> filterValues(List<String> source, String typedValue) {
        String keyword = typedValue == null ? "" : typedValue.trim().toLowerCase();
        List<String> result = new ArrayList<>();

        for (String value : source) {
            if (value == null || value.isBlank()) {
                continue;
            }

            if (keyword.isBlank() || value.toLowerCase().contains(keyword)) {
                result.add(value);
            }
        }

        return result;
    }

    private String getComboValue(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return "";
        }

        String editorText = comboBox.isEditable() && comboBox.getEditor() != null
                ? comboBox.getEditor().getText()
                : null;

        if (editorText != null && !editorText.isBlank()) {
            return editorText.trim();
        }

        String value = comboBox.getValue();
        return value == null ? "" : value.trim();
    }

    private void setComboValue(ComboBox<String> comboBox, String value) {
        String normalized = value == null ? "" : value.trim();
        if (comboBox == countryCodeField) {
            normalized = normalizeCountryCode(normalized);
        }
        comboBox.setValue(normalized);
        if (comboBox.isEditable() && comboBox.getEditor() != null) {
            comboBox.getEditor().setText(normalized);
        }
    }

    private void close() {
        if (!canCloseWindow()) {
            return;
        }

        Stage stage = resolveStage();
        if (stage != null) {
            closeConfirmed = true;
            stage.close();
        }
    }

    private boolean canCloseWindow() {
        if (closeConfirmed || saveCompleted || !hasUnsavedChanges()) {
            return true;
        }
        return DialogUtil.confirmDiscardChanges(contrahent == null ? "kontrahenta" : "edycji kontrahenta");
    }

    private boolean hasUnsavedChanges() {
        return !java.util.Objects.equals(initialSnapshot, buildFormSnapshot());
    }

    private void markCurrentStateAsClean() {
        initialSnapshot = buildFormSnapshot();
        saveCompleted = false;
        closeConfirmed = false;
    }

    private String buildFormSnapshot() {
        return String.join("|",
                normalizeText(nameField == null ? null : nameField.getText()),
                normalizeText(getComboValue(countryField)),
                normalizeCountryCode(getComboValue(countryCodeField)),
                normalizeText(postalCodeField == null ? null : postalCodeField.getText()),
                normalizeText(cityField == null ? null : cityField.getText()),
                normalizeText(streetField == null ? null : streetField.getText()),
                normalizeText(phytosanitaryNumberField == null ? null : phytosanitaryNumberField.getText()),
                String.valueOf(supplierCheckBox != null && supplierCheckBox.isSelected()),
                String.valueOf(clientCheckBox != null && clientCheckBox.isSelected())
        );
    }

    private String normalizeCountryCode(String value) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? "" : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? "" : normalized;
    }

    private void installWindowCloseGuard() {
        Stage stage = resolveStage();
        if (stage == null || closeGuardInstalled) {
            return;
        }

        stage.setOnCloseRequest(event -> {
            if (closeConfirmed || saveCompleted || !hasUnsavedChanges()) {
                return;
            }

            if (!DialogUtil.confirmDiscardChanges(contrahent == null ? "kontrahenta" : "edycji kontrahenta")) {
                event.consume();
                return;
            }

            closeConfirmed = true;
        });
        closeGuardInstalled = true;
    }

    private Stage resolveStage() {
        Node[] candidates = {
                nameField,
                countryField,
                countryCodeField,
                postalCodeField,
                cityField,
                streetField,
                phytosanitaryNumberField
        };

        for (Node candidate : candidates) {
            if (candidate != null && candidate.getScene() != null && candidate.getScene().getWindow() instanceof Stage stage) {
                return stage;
            }
        }

        return null;
    }
}
