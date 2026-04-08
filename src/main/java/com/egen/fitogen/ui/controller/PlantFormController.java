package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.EppoCodeService;
import com.egen.fitogen.service.PlantService;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ValidationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class PlantFormController {

    @FXML
    private ComboBox<String> speciesField;

    @FXML
    private ComboBox<String> varietyField;

    @FXML
    private ComboBox<String> rootstockField;

    @FXML
    private ComboBox<String> latinSpeciesNameField;

    @FXML
    private Label eppoCodeInfoLabel;

    @FXML
    private CheckBox passportRequiredBox;

    @FXML
    private Label passportRequirementInfoLabel;

    @FXML
    private ComboBox<String> visibilityStatusBox;

    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final EppoCodeService eppoCodeService = AppContext.getEppoCodeService();

    private PlantService plantService;
    private Plant plant;
    private boolean syncingSpeciesFields;
    private String initialSnapshot = "";
    private boolean saveCompleted;
    private boolean closeConfirmed;
    private boolean closeGuardInstalled;

    @FXML
    public void initialize() {
        visibilityStatusBox.getItems().addAll("Używany", "Nieużywany");
        visibilityStatusBox.setValue("Używany");
        passportRequiredBox.setSelected(false);

        configureEditableCombo(speciesField);
        configureEditableCombo(varietyField);
        configureEditableCombo(rootstockField);
        configureEditableCombo(latinSpeciesNameField);

        installComboTrimSupport(speciesField);
        installComboTrimSupport(varietyField);
        installComboTrimSupport(rootstockField);
        installComboTrimSupport(latinSpeciesNameField);
        installFieldTooltips();

        configureSpeciesSynchronization();
        refreshPassportRequirementPresentation();
        refreshResolvedEppoCodePresentation();

        Platform.runLater(() -> {
            installWindowCloseGuard();
            markCurrentStateAsClean();
        });
    }

    public void setPlantService(PlantService plantService) {
        this.plantService = plantService;
        loadSuggestions();
        refreshResolvedEppoCodePresentation();
    }

    public void setPlant(Plant plant) {
        this.plant = plant;

        setComboValue(speciesField, plant.getSpecies());
        setComboValue(varietyField, plant.getVariety());
        setComboValue(rootstockField, plant.getRootstock());
        setComboValue(latinSpeciesNameField, plant.getLatinSpeciesName());
        passportRequiredBox.setSelected(plant.isPassportRequired());
        visibilityStatusBox.setValue(plant.getVisibilityStatus());

        refreshPassportRequirementPresentation();
        refreshResolvedEppoCodePresentation();
        markCurrentStateAsClean();
    }

    @FXML
    private void save() {
        try {
            String species = ValidationUtil.requireText(getComboValue(speciesField), "Gatunek");
            String variety = ValidationUtil.optionalText(getComboValue(varietyField));
            String rootstock = ValidationUtil.optionalText(getComboValue(rootstockField));
            String latinSpeciesName = ValidationUtil.optionalText(getComboValue(latinSpeciesNameField));
            String eppoCode = resolveCurrentEppoCode(species, latinSpeciesName);
            boolean passportRequired = resolveCurrentPassportRequirement();
            String visibilityStatus = ValidationUtil.requireText(
                    visibilityStatusBox.getValue(),
                    "Status widoczności"
            );

            if (plant == null) {
                Plant newPlant = new Plant(
                        0,
                        species,
                        variety,
                        rootstock,
                        latinSpeciesName,
                        eppoCode,
                        passportRequired,
                        visibilityStatus
                );

                plantService.addPlant(newPlant);
                DialogUtil.showSuccess("Roślina została dodana.");
            } else {
                plant.setSpecies(species);
                plant.setVariety(variety);
                plant.setRootstock(rootstock);
                plant.setLatinSpeciesName(latinSpeciesName);
                plant.setEppoCode(eppoCode);
                plant.setPassportRequired(passportRequired);
                plant.setVisibilityStatus(visibilityStatus);

                plantService.updatePlant(plant);
                DialogUtil.showSuccess("Roślina została zaktualizowana.");
            }

            saveCompleted = true;
            closeConfirmed = true;
            close();

        } catch (IllegalArgumentException e) {
            if (isDuplicatePlantViolation(e)) {
                DialogUtil.showWarning(
                        "Duplikat rośliny",
                        buildDuplicatePlantMessage(
                                getComboValue(speciesField),
                                getComboValue(varietyField),
                                getComboValue(rootstockField)
                        )
                );
            } else {
                DialogUtil.showWarning("Błędne dane", e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu", "Nie udało się zapisać rośliny.");
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private void loadSuggestions() {
        if (plantService == null) {
            return;
        }

        attachAutocomplete(speciesField, plantService::getSpeciesSuggestions);
        attachAutocomplete(varietyField, plantService::getVarietySuggestions);
        attachAutocomplete(rootstockField, plantService::getRootstockSuggestions);
        attachAutocomplete(latinSpeciesNameField, plantService::getLatinSpeciesNameSuggestions);
    }

    private void configureEditableCombo(ComboBox<String> comboBox) {
        comboBox.setEditable(true);
        comboBox.setMaxWidth(Double.MAX_VALUE);
    }

    private void installFieldTooltips() {
        setTooltip(speciesField, "Pole wymagane. Możesz wpisać własny gatunek albo wybrać jedną z podpowiedzi.");
        setTooltip(varietyField, "Pole opcjonalne. Uzupełnij odmianę, jeśli chcesz ją rozróżniać na liście roślin.");
        setTooltip(rootstockField, "Pole opcjonalne. Przydaje się przy roślinach szczepionych i przy numeracji partii.");
        setTooltip(latinSpeciesNameField, "Pole opcjonalne. Wpis łaciński pomaga w automatycznym dopasowaniu kodu EPPO.");
        setTooltip(eppoCodeInfoLabel, "Informacja jest wyliczana automatycznie na podstawie gatunku lub nazwy łacińskiej.");
        setTooltip(passportRequiredBox, "Wymaganie paszportu możesz ustawić ręcznie albo pozostawić zgodnie z ustawieniami systemu i słowników EPPO.");
        setTooltip(passportRequirementInfoLabel, "To pole wyjaśnia, czy dla tej rośliny paszport wynika z ustawień globalnych czy z decyzji w formularzu.");
        setTooltip(visibilityStatusBox, "Status wpływa na widoczność rośliny w codziennej pracy. „Nieużywany” ukrywa ją z głównych przepływów bez usuwania danych.");
    }

    private void installComboTrimSupport(ComboBox<String> comboBox) {
        if (comboBox == null || comboBox.getEditor() == null) {
            return;
        }

        comboBox.getEditor().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                return;
            }

            String normalized = normalizeText(comboBox.getEditor().getText());
            comboBox.getEditor().setText(normalized == null ? "" : normalized);
        });
    }

    private void setTooltip(javafx.scene.control.Control control, String text) {
        if (control != null && text != null && !text.isBlank()) {
            control.setTooltip(new Tooltip(text));
        }
    }

    private void configureSpeciesSynchronization() {
        installSpeciesFieldListeners(speciesField);
        installSpeciesFieldListeners(latinSpeciesNameField);
    }

    private void installSpeciesFieldListeners(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }

        comboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            synchronizeSpeciesFields(comboBox);
            refreshResolvedEppoCodePresentation();
        });

        if (comboBox.getEditor() != null) {
            comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
                synchronizeSpeciesFields(comboBox);
                refreshResolvedEppoCodePresentation();
            });
        }
    }

    private void synchronizeSpeciesFields(ComboBox<String> sourceField) {
        if (syncingSpeciesFields || plantService == null) {
            return;
        }

        String species = normalizeText(getComboValue(speciesField));
        String latinSpeciesName = normalizeText(getComboValue(latinSpeciesNameField));

        if ((species == null || species.isBlank()) && (latinSpeciesName == null || latinSpeciesName.isBlank())) {
            return;
        }

        syncingSpeciesFields = true;
        try {
            for (Plant existingPlant : plantService.getAllPlants()) {
                if (existingPlant == null) {
                    continue;
                }

                String existingSpecies = normalizeText(existingPlant.getSpecies());
                String existingLatin = normalizeText(existingPlant.getLatinSpeciesName());

                boolean sourceSpeciesMatches = sourceField == speciesField
                        && species != null
                        && !species.isBlank()
                        && equalsIgnoreCase(existingSpecies, species)
                        && notBlank(existingLatin);

                boolean sourceLatinMatches = sourceField == latinSpeciesNameField
                        && latinSpeciesName != null
                        && !latinSpeciesName.isBlank()
                        && equalsIgnoreCase(existingLatin, latinSpeciesName)
                        && notBlank(existingSpecies);

                if (sourceSpeciesMatches && !equalsIgnoreCase(latinSpeciesName, existingLatin)) {
                    setComboValue(latinSpeciesNameField, existingLatin);
                    return;
                }

                if (sourceLatinMatches && !equalsIgnoreCase(species, existingSpecies)) {
                    setComboValue(speciesField, existingSpecies);
                    return;
                }
            }
        } finally {
            syncingSpeciesFields = false;
        }
    }

    private void attachAutocomplete(ComboBox<String> comboBox, Supplier<List<String>> sourceSupplier) {
        ObservableList<String> baseItems = FXCollections.observableArrayList(sourceSupplier.get());
        comboBox.setItems(baseItems);

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

    private boolean isDuplicatePlantViolation(IllegalArgumentException exception) {
        if (exception == null || exception.getMessage() == null) {
            return false;
        }

        String message = exception.getMessage().toLowerCase();
        return message.contains("duplikat")
                || message.contains("already exists")
                || message.contains("already exist")
                || message.contains("full catalog")
                || message.contains("pełnej bazie");
    }

    private String buildDuplicatePlantMessage(String species, String variety, String rootstock) {
        return """
                Taka roślina już istnieje i nie może zostać zapisana w trybie pełnej bazy roślin.

                Wykryta kombinacja:
                - Gatunek: %s
                - Odmiana: %s
                - Podkładka: %s

                Zmień dane albo wyłącz tryb pełnej bazy roślin w Ustawieniach, jeśli chcesz pracować w prostszym katalogu lokalnym.
                """.formatted(
                displayValue(species),
                displayValue(variety),
                displayValue(rootstock)
        );
    }

    private String displayValue(String value) {
        if (value == null || value.trim().isBlank()) {
            return "—";
        }
        return value.trim();
    }

    private String getComboValue(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return null;
        }

        String editorText = comboBox.isEditable() && comboBox.getEditor() != null
                ? comboBox.getEditor().getText()
                : null;

        if (editorText != null && !editorText.isBlank()) {
            return editorText.trim();
        }

        String value = comboBox.getValue();
        return value == null ? null : value.trim();
    }

    private void setComboValue(ComboBox<String> comboBox, String value) {
        String normalized = value == null ? "" : value.trim();
        comboBox.setValue(normalized);
        if (comboBox.isEditable() && comboBox.getEditor() != null) {
            comboBox.getEditor().setText(normalized);
        }
    }

    private void refreshResolvedEppoCodePresentation() {
        EppoCode matchedCode = findMatchingEppoCode(
                getComboValue(speciesField),
                getComboValue(latinSpeciesNameField)
        );

        if (eppoCodeInfoLabel == null) {
            return;
        }

        if (matchedCode != null) {
            eppoCodeInfoLabel.setText("Wpis w Słownikach EPPO: " + matchedCode.getCode());
            eppoCodeInfoLabel.setVisible(true);
            eppoCodeInfoLabel.setManaged(true);
            return;
        }

        eppoCodeInfoLabel.setText("Brak wpisów dla tego gatunku w Słownikach EPPO");
        eppoCodeInfoLabel.setVisible(true);
        eppoCodeInfoLabel.setManaged(true);
    }

    private String resolveCurrentEppoCode(String species, String latinSpeciesName) {
        EppoCode matchedCode = findMatchingEppoCode(species, latinSpeciesName);
        if (matchedCode != null) {
            return matchedCode.getCode();
        }
        return null;
    }

    private EppoCode findMatchingEppoCode(String species, String latinSpeciesName) {
        if (eppoCodeService == null) {
            return null;
        }

        String normalizedSpecies = normalizeText(species);
        String normalizedLatin = normalizeText(latinSpeciesName);

        for (EppoCode code : eppoCodeService.getAll()) {
            if (code == null) {
                continue;
            }

            String codeSpecies = normalizeText(code.getSpeciesName());
            if (!notBlank(codeSpecies)) {
                codeSpecies = normalizeText(code.getCommonName());
            }

            String codeLatin = normalizeText(code.getLatinSpeciesName());
            if (!notBlank(codeLatin)) {
                codeLatin = normalizeText(code.getScientificName());
            }

            if (notBlank(normalizedSpecies) && notBlank(codeSpecies) && equalsIgnoreCase(normalizedSpecies, codeSpecies)) {
                return code;
            }

            if (notBlank(normalizedLatin) && notBlank(codeLatin) && equalsIgnoreCase(normalizedLatin, codeLatin)) {
                return code;
            }
        }

        return null;
    }

    private void refreshPassportRequirementPresentation() {
        boolean globallyRequired = appSettingsService != null && appSettingsService.isPlantPassportRequiredForAll();

        if (globallyRequired) {
            passportRequiredBox.setSelected(true);
            passportRequiredBox.setDisable(true);
            if (passportRequirementInfoLabel != null) {
                passportRequirementInfoLabel.setText(
                        "W Ustawieniach wybrano: wymagaj paszportu dla wszystkich roślin. "
                );
                passportRequirementInfoLabel.setVisible(true);
                passportRequirementInfoLabel.setManaged(true);
            }
            return;
        }

        passportRequiredBox.setDisable(false);
        if (passportRequirementInfoLabel != null) {
            passportRequirementInfoLabel.setText(
                    "Wymaganie paszportu możesz ustawić indywidualnie dla tej rośliny."
            );
            passportRequirementInfoLabel.setVisible(true);
            passportRequirementInfoLabel.setManaged(true);
        }
    }

    private boolean resolveCurrentPassportRequirement() {
        if (appSettingsService != null && appSettingsService.isPlantPassportRequiredForAll()) {
            return true;
        }
        return passportRequiredBox.isSelected();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? "" : normalized;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isBlank();
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
        return DialogUtil.confirmDiscardChanges(plant == null ? "rośliny" : "edycji rośliny");
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
                normalizeText(getComboValue(speciesField)),
                normalizeText(getComboValue(varietyField)),
                normalizeText(getComboValue(rootstockField)),
                normalizeText(getComboValue(latinSpeciesNameField)),
                String.valueOf(resolveCurrentPassportRequirement()),
                normalizeText(visibilityStatusBox == null ? null : visibilityStatusBox.getValue())
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

            if (!DialogUtil.confirmDiscardChanges(plant == null ? "rośliny" : "edycji rośliny")) {
                event.consume();
                return;
            }

            closeConfirmed = true;
        });
        closeGuardInstalled = true;
    }

    private Stage resolveStage() {
        if (speciesField != null && speciesField.getScene() != null && speciesField.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }
}