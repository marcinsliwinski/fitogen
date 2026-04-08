package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.EppoZone;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.EppoZoneService;
import com.egen.fitogen.ui.util.ComboBoxAutoComplete;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ValidationUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;


public class EppoZoneFormController {

    @FXML private ComboBox<String> countryField;
    @FXML private TextField countryCodeField;
    @FXML private ComboBox<String> statusBox;
    @FXML private Label countryHintLabel;

    private final EppoZoneService eppoZoneService = AppContext.getEppoZoneService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();

    private EppoZone eppoZone;
    private String initialSnapshot = "";
    private boolean saveCompleted;
    private boolean closeConfirmed;
    private boolean closeGuardInstalled;

    @FXML
    public void initialize() {
        statusBox.getItems().addAll("ACTIVE", "INACTIVE");
        configureStatusBox();
        statusBox.setValue("ACTIVE");
        countryCodeField.setEditable(false);

        configureCountrySuggestions();
        configureCountryAutoFill();
        updateCountryHint();

        Platform.runLater(() -> {
            installWindowCloseGuard();
            markCurrentStateAsClean();
        });
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
        markCurrentStateAsClean();
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

            saveCompleted = true;
            closeConfirmed = true;
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

    private void configureStatusBox() {
        statusBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return formatStatusForDisplay(value);
            }

            @Override
            public String fromString(String value) {
                return parseStatusFromDisplay(value);
            }
        });
        statusBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : formatStatusForDisplay(item));
            }
        });
        statusBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : formatStatusForDisplay(item));
            }
        });
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

    private String formatStatusForDisplay(String status) {
        String normalized = nullSafe(status).trim().toUpperCase();
        if ("ACTIVE".equals(normalized)) {
            return "Aktywny";
        }
        if ("INACTIVE".equals(normalized)) {
            return "Nieaktywny";
        }
        return normalized.isBlank() ? "—" : nullSafe(status).trim();
    }

    private String parseStatusFromDisplay(String value) {
        String normalized = nullSafe(value).trim().toLowerCase();
        if (normalized.equals("aktywny")) {
            return "ACTIVE";
        }
        if (normalized.equals("nieaktywny")) {
            return "INACTIVE";
        }
        return nullSafe(value).trim().toUpperCase();
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
        return DialogUtil.confirmDiscardChanges(eppoZone == null ? "kraju EPPO" : "edycji kraju EPPO");
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
                nullSafe(ComboBoxAutoComplete.getCommittedValue(countryField)).trim(),
                nullSafe(countryCodeField == null ? null : countryCodeField.getText()).trim(),
                nullSafe(statusBox == null ? null : statusBox.getValue()).trim()
        );
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

            if (!DialogUtil.confirmDiscardChanges(eppoZone == null ? "kraju EPPO" : "edycji kraju EPPO")) {
                event.consume();
                return;
            }

            closeConfirmed = true;
        });
        closeGuardInstalled = true;
    }

    private Stage resolveStage() {
        if (countryField != null && countryField.getScene() != null && countryField.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }
}
