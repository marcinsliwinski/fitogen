package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.service.ContrahentService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
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
    private Consumer<Contrahent> onSaved;

    @FXML
    public void initialize() {
        configureEditableCombo(countryField);
        configureEditableCombo(countryCodeField);

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
    }

    public void setContrahent(Contrahent contrahent) {
        this.contrahent = contrahent;

        nameField.setText(contrahent.getName());
        setComboValue(countryField, contrahent.getCountry());
        setComboValue(countryCodeField, contrahent.getCountryCode());
        postalCodeField.setText(contrahent.getPostalCode());
        cityField.setText(contrahent.getCity());
        streetField.setText(contrahent.getStreet());
        phytosanitaryNumberField.setText(contrahent.getPhytosanitaryNumber());
        supplierCheckBox.setSelected(contrahent.isSupplier());
        clientCheckBox.setSelected(contrahent.isClient());
    }


    public void setOnSaved(Consumer<Contrahent> onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    private void saveContrahent() {
        try {
            String name = ValidationUtil.requireText(nameField.getText(), "Nazwa");

            Contrahent entity = contrahent != null ? contrahent : new Contrahent();
            entity.setName(name);
            entity.setCountry(ValidationUtil.optionalText(getComboValue(countryField)));
            entity.setCountryCode(ValidationUtil.optionalText(getComboValue(countryCodeField)));
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

            if (onSaved != null) {
                onSaved.accept(copyForSelection(entity));
            }

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

        String code = getComboValue(countryCodeField);
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
        comboBox.setValue(normalized);
        if (comboBox.isEditable() && comboBox.getEditor() != null) {
            comboBox.getEditor().setText(normalized);
        }
    }

    private Contrahent copyForSelection(Contrahent source) {
        Contrahent copy = new Contrahent();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setCountry(source.getCountry());
        copy.setCountryCode(source.getCountryCode());
        copy.setCity(source.getCity());
        copy.setPostalCode(source.getPostalCode());
        copy.setStreet(source.getStreet());
        copy.setPhytosanitaryNumber(source.getPhytosanitaryNumber());
        copy.setSupplier(source.isSupplier());
        copy.setClient(source.isClient());
        return copy;
    }

    private void close() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
