package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.EppoZone;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.EppoZoneService;
import com.egen.fitogen.ui.util.ComboBoxAutoComplete;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


public class EppoZoneFormController {

    @FXML private ComboBox<String> countryField;
    @FXML private TextField countryCodeField;
    @FXML private ComboBox<String> statusBox;
    @FXML private Label countryHintLabel;

    private final EppoZoneService eppoZoneService = AppContext.getEppoZoneService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();

    private EppoZone eppoZone;

    @FXML
    public void initialize() {
        statusBox.getItems().addAll("ACTIVE", "INACTIVE");
        statusBox.setValue("ACTIVE");
        countryCodeField.setEditable(false);

        configureCountrySuggestions();
        configureCountryAutoFill();
        updateCountryHint();
    }

    public void setEppoZone(EppoZone eppoZone) {
        this.eppoZone = eppoZone;

        if (eppoZone == null) {
            return;
        }

        countryField.setValue(eppoZone.getName());
        countryField.getEditor().setText(nullSafe(eppoZone.getName()));
        countryCodeField.setText(resolveCountryCode(eppoZone.getName(), eppoZone.getCountryCode(), eppoZone.getCode()));
        statusBox.setValue(
                eppoZone.getStatus() == null || eppoZone.getStatus().isBlank()
                        ? "ACTIVE"
                        : eppoZone.getStatus()
        );

        updateCountryHint();
    }

    @FXML
    private void save() {
        try {
            String country = ValidationUtil.requireText(ComboBoxAutoComplete.getCommittedValue(countryField), "Kraj");
            String countryCode = ValidationUtil.requireText(countryCodeField.getText(), "Kod kraju");
            String status = ValidationUtil.requireText(statusBox.getValue(), "Status");

            EppoZone entity = eppoZone != null
                    ? eppoZone
                    : new EppoZone(0, "", "", "", "ACTIVE");

            entity.setCode(countryCode);
            entity.setName(country);
            entity.setCountryCode(countryCode);
            entity.setStatus(status);

            eppoZoneService.save(entity);

            if (eppoZone == null) {
                DialogUtil.showSuccess("Kraj EPPO został dodany.");
            } else {
                DialogUtil.showSuccess("Kraj EPPO został zaktualizowany.");
            }

            close();

        } catch (IllegalArgumentException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu", "Nie udało się zapisać kraju EPPO.");
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private void configureCountrySuggestions() {
        ComboBoxAutoComplete.bindEditable(countryField, countryDirectoryService.getCountries());
    }

    private void configureCountryAutoFill() {
        countryField.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                applyCountrySelection();
            }
        });

        countryField.setOnAction(event -> applyCountrySelection());
    }

    private void applyCountrySelection() {
        String country = ComboBoxAutoComplete.getCommittedValue(countryField);
        String countryCode = resolveCountryCode(country, null, null);
        countryCodeField.setText(nullSafe(countryCode));
        updateCountryHint();
    }

    private void updateCountryHint() {
        if (countryHintLabel == null) {
            return;
        }

        if (countryCodeField.getText() == null || countryCodeField.getText().isBlank()) {
            countryHintLabel.setText("Wybierz kraj ze wspólnego słownika krajów i kontrahentów, aby automatycznie uzupełnić kod kraju.");
            return;
        }

        countryHintLabel.setText("Kod kraju został uzupełniony automatycznie ze wspólnego słownika krajów używanego także przez kontrahentów.");
    }

    private String resolveCountryCode(String countryName, String countryCode, String zoneCode) {
        if (countryCode != null && !countryCode.isBlank()) {
            return countryCode.trim().toUpperCase();
        }

        String fromSharedDirectory = countryDirectoryService.findCodeByCountry(countryName);
        if (fromSharedDirectory != null && !fromSharedDirectory.isBlank()) {
            return fromSharedDirectory;
        }

        if (zoneCode != null && zoneCode.trim().matches("[A-Za-z]{2}")) {
            return zoneCode.trim().toUpperCase();
        }

        return null;
    }


    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private void close() {
        Stage stage = (Stage) countryField.getScene().getWindow();
        stage.close();
    }
}
