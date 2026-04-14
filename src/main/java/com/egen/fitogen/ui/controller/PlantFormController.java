package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.EppoCodeService;
import com.egen.fitogen.service.PlantService;
import com.egen.fitogen.ui.util.ComboBoxAutoComplete;
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

import java.util.List;
import java.util.Objects;

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
    private ComboBox<String> defaultDocumentTypeBox;

    @FXML
    private ComboBox<String> visibilityStatusBox;

    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final EppoCodeService eppoCodeService = AppContext.getEppoCodeService();

    private PlantService plantService;
    private Plant plant;
    private boolean syncingSpeciesFields;
    private boolean refreshingDependentSuggestions;
    private String initialSnapshot = "";
    private boolean saveCompleted;
    private boolean closeConfirmed;
    private boolean closeGuardInstalled;

    private static final String DEFAULT_DOCUMENT_LABEL_NURSERY_SUPPLIER = "Szkółkarski dokument dostawcy";
    private static final String DEFAULT_DOCUMENT_LABEL_SUPPLIER = "Dokument dostawcy";

    @FXML
    public void initialize() {
        ComboBoxAutoComplete.bindEditable(visibilityStatusBox, List.of("Używany", "Nieużywany"));
        visibilityStatusBox.setValue("Używany");
        ComboBoxAutoComplete.bindEditable(defaultDocumentTypeBox, List.of(
                DEFAULT_DOCUMENT_LABEL_NURSERY_SUPPLIER,
                DEFAULT_DOCUMENT_LABEL_SUPPLIER
        ));
        defaultDocumentTypeBox.setValue(DEFAULT_DOCUMENT_LABEL_NURSERY_SUPPLIER);
        passportRequiredBox.setSelected(false);

        configureEditableCombo(speciesField);
        configureEditableCombo(varietyField);
        configureEditableCombo(rootstockField);
        configureEditableCombo(latinSpeciesNameField);

        installComboTrimSupport(speciesField);
        installComboTrimSupport(varietyField);
        installComboTrimSupport(rootstockField);
        installComboTrimSupport(latinSpeciesNameField);
        installComboTrimSupport(defaultDocumentTypeBox);
        installComboTrimSupport(visibilityStatusBox);
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
        defaultDocumentTypeBox.setValue(toDocumentLabel(plant.getDefaultDocumentType()));

        refreshDependentPlantSuggestions();
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
            String defaultDocumentType = resolveDefaultDocumentTypeCode();
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
                        visibilityStatus,
                        defaultDocumentType
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
                plant.setDefaultDocumentType(defaultDocumentType);

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

        ComboBoxAutoComplete.bindEditable(speciesField, plantService::getSpeciesSuggestions);
        ComboBoxAutoComplete.bindEditable(varietyField,
                () -> plantService.getVarietySuggestionsForSpecies(getSelectedSpeciesForSuggestions()));
        ComboBoxAutoComplete.bindEditable(rootstockField,
                () -> plantService.getRootstockSuggestionsForSpecies(getSelectedSpeciesForSuggestions()));
        ComboBoxAutoComplete.bindEditable(latinSpeciesNameField, plantService::getLatinSpeciesNameSuggestions);
        refreshDependentPlantSuggestions();
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
        setTooltip(defaultDocumentTypeBox, "Wybierz, na którym typie dokumentu ta roślina powinna pojawiać się domyślnie.");
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
            if (comboBox == speciesField) {
                refreshDependentPlantSuggestions();
            }
            refreshResolvedEppoCodePresentation();
        });

        if (comboBox.getEditor() != null) {
            comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
                synchronizeSpeciesFields(comboBox);
                if (comboBox == speciesField) {
                    refreshDependentPlantSuggestions();
                }
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


    private void refreshDependentPlantSuggestions() {
        if (plantService == null || refreshingDependentSuggestions) {
            return;
        }

        refreshingDependentSuggestions = true;
        try {
            ComboBoxAutoComplete.refreshSource(
                    varietyField,
                    plantService.getVarietySuggestionsForSpecies(getSelectedSpeciesForSuggestions())
            );
            ComboBoxAutoComplete.refreshSource(
                    rootstockField,
                    plantService.getRootstockSuggestionsForSpecies(getSelectedSpeciesForSuggestions())
            );
        } finally {
            refreshingDependentSuggestions = false;
        }
    }

    private String getSelectedSpeciesForSuggestions() {
        return normalizeText(getComboValue(speciesField));
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

    private String resolveDefaultDocumentTypeCode() {
        String code = mapDocumentLabelToCode(getComboValue(defaultDocumentTypeBox));
        if (code != null) {
            return code;
        }
        throw new IllegalArgumentException("Domyślny dokument musi być wybrany z listy dostępnych opcji.");
    }

    private String mapDocumentLabelToCode(String selectedLabel) {
        String normalized = normalizeText(selectedLabel);

        if (DEFAULT_DOCUMENT_LABEL_NURSERY_SUPPLIER.equals(normalized)) {
            return Plant.DEFAULT_DOCUMENT_NURSERY_SUPPLIER;
        }

        if (DEFAULT_DOCUMENT_LABEL_SUPPLIER.equals(normalized)) {
            return Plant.DEFAULT_DOCUMENT_SUPPLIER;
        }

        return null;
    }

    private String toDocumentLabel(String documentTypeCode) {
        if (Plant.DEFAULT_DOCUMENT_SUPPLIER.equals(documentTypeCode)) {
            return DEFAULT_DOCUMENT_LABEL_SUPPLIER;
        }
        return DEFAULT_DOCUMENT_LABEL_NURSERY_SUPPLIER;
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
                normalizeText(mapDocumentLabelToCode(getComboValue(defaultDocumentTypeBox))),
                normalizeText(visibilityStatusBox == null ? null : getComboValue(visibilityStatusBox))
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