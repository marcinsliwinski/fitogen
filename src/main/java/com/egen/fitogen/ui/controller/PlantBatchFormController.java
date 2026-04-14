package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.database.SqliteContrahentRepository;
import com.egen.fitogen.database.SqlitePlantRepository;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoZone;
import com.egen.fitogen.model.IssuerProfile;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.model.PlantBatchStatus;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.repository.PlantRepository;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.EppoCodePlantLinkService;
import com.egen.fitogen.service.EppoCodeZoneLinkService;
import com.egen.fitogen.service.PlantBatchService;
import com.egen.fitogen.ui.util.ComboBoxAutoComplete;
import com.egen.fitogen.ui.util.DialogUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PlantBatchFormController {

    @FXML private ComboBox<Plant> plantComboBox;
    @FXML private CheckBox internalSourceCheckBox;
    @FXML private ComboBox<Contrahent> sourceOriginComboBox;
    @FXML private TextField interiorBatchNoField;
    @FXML private TextField exteriorBatchNoField;
    @FXML private TextField qtyField;
    @FXML private TextField ageYearsField;
    @FXML private DatePicker creationDatePicker;
    @FXML private TextField manufacturerCountryCodeField;
    @FXML private TextField fitoQualificationCategoryField;
    @FXML private TextField eppoCodeField;
    @FXML private CheckBox zpOriginCheckBox;
    @FXML private Label zpEligibilityHintLabel;
    @FXML private TextArea restrictionsInfoArea;
    @FXML private TextArea commentsArea;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final PlantBatchService plantBatchService = AppContext.getPlantBatchService();
    private final PlantRepository plantRepository = new SqlitePlantRepository();
    private final ContrahentRepository contrahentRepository = new SqliteContrahentRepository();
    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final EppoCodePlantLinkService eppoCodePlantLinkService = AppContext.getEppoCodePlantLinkService();
    private final EppoCodeZoneLinkService eppoCodeZoneLinkService = AppContext.getEppoCodeZoneLinkService();

    private List<Plant> availablePlants = List.of();
    private List<Contrahent> availableSourceOrigins = List.of();
    private Integer editingBatchId;
    private PlantBatchStatus currentStatus = PlantBatchStatus.ACTIVE;
    private Plant preselectedPlant;
    private Runnable onSaveSuccess;
    private String initialSnapshot = "";
    private boolean saveCompleted;
    private boolean closeConfirmed;
    private boolean closeGuardInstalled;
    private boolean loadingExistingBatch;

    @FXML
    public void initialize() {
        loadPlants();
        loadSourceOrigins();

        if (interiorBatchNoField != null) {
            interiorBatchNoField.setDisable(true);
        }

        configureInputPresentation();
        configureComboBoxes();
        installFieldTooltips();
        configureInfoArea(restrictionsInfoArea);

        if (preselectedPlant != null && plantComboBox != null) {
            plantComboBox.setValue(preselectedPlant);
        }

        if (creationDatePicker != null && creationDatePicker.getValue() == null) {
            creationDatePicker.setValue(LocalDate.now());
        }
        if (ageYearsField != null && isBlank(ageYearsField.getText())) {
            ageYearsField.setText("1");
        }

        if (internalSourceCheckBox != null) {
            internalSourceCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                applyInternalSourceMode(newVal);
                refreshOriginDerivedFields();
                refreshInteriorBatchPreview();
                refreshRestrictionsState();
            });
            applyInternalSourceMode(internalSourceCheckBox.isSelected());
        }

        if (sourceOriginComboBox != null) {
            sourceOriginComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                refreshOriginDerivedFields();
                refreshInteriorBatchPreview();
                refreshRestrictionsState();
            });
        }

        if (plantComboBox != null) {
            plantComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                refreshPlantDerivedFields();
                refreshInteriorBatchPreview();
            });
        }

        if (exteriorBatchNoField != null) {
            exteriorBatchNoField.textProperty().addListener((obs, oldVal, newVal) -> refreshInteriorBatchPreview());
        }

        if (manufacturerCountryCodeField != null) {
            manufacturerCountryCodeField.textProperty().addListener((obs, oldVal, newVal) -> refreshRestrictionsState());
        }

        if (creationDatePicker != null) {
            creationDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                refreshInteriorBatchPreview();
                refreshAgeFromCreationDate();
            });
        }

        if (zpOriginCheckBox != null) {
            zpOriginCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> refreshRestrictionsState());
        }

        refreshOriginDerivedFields();
        refreshPlantDerivedFields();
        refreshInteriorBatchPreview();
        refreshAgeFromCreationDate();

        Platform.runLater(() -> {
            installWindowCloseGuard();
            markCurrentStateAsClean();
        });
    }

    public void setPlantBatch(PlantBatch plantBatch) {
        if (plantBatch == null) {
            return;
        }

        loadingExistingBatch = true;
        try {
            editingBatchId = plantBatch.getId();
            currentStatus = plantBatch.getStatus() != null ? plantBatch.getStatus() : PlantBatchStatus.ACTIVE;

            selectPlant(plantBatch.getPlantId());
            if (internalSourceCheckBox != null) {
                internalSourceCheckBox.setSelected(plantBatch.isInternalSource());
            }
            if (interiorBatchNoField != null) {
                interiorBatchNoField.setText(plantBatch.getInteriorBatchNo());
            }
            if (exteriorBatchNoField != null) {
                exteriorBatchNoField.setText(plantBatch.getExteriorBatchNo());
            }
            if (qtyField != null) {
                qtyField.setText(String.valueOf(plantBatch.getQty()));
            }
            if (ageYearsField != null) {
                int ageYears = plantBatch.getAgeYears() > 0 ? plantBatch.getAgeYears() : deriveAgeYears(plantBatch.getCreationDate());
                ageYearsField.setText(String.valueOf(Math.max(1, ageYears)));
            }
            if (creationDatePicker != null) {
                creationDatePicker.setValue(plantBatch.getCreationDate());
            }
            if (manufacturerCountryCodeField != null) {
                manufacturerCountryCodeField.setText(plantBatch.getManufacturerCountryCode());
            }
            if (fitoQualificationCategoryField != null) {
                fitoQualificationCategoryField.setText(plantBatch.getFitoQualificationCategory());
            }
            if (eppoCodeField != null) {
                eppoCodeField.setText(plantBatch.getEppoCode());
            }
            if (commentsArea != null) {
                commentsArea.setText(plantBatch.getComments());
            }
            if (zpOriginCheckBox != null) {
                zpOriginCheckBox.setSelected(!isBlank(plantBatch.getZpZone()));
            }

            if (plantBatch.isInternalSource()) {
                if (sourceOriginComboBox != null) {
                    sourceOriginComboBox.setValue(null);
                }
                applyInternalSourceMode(true);
            } else {
                selectSourceOrigin(plantBatch.getContrahentId());
                applyInternalSourceMode(false);
            }

            refreshOriginDerivedFields();
            refreshPlantDerivedFields();

            if (currentStatus == PlantBatchStatus.CANCELLED) {
                applyCancelledMode();
            }

            markCurrentStateAsClean();
        } finally {
            loadingExistingBatch = false;
        }
    }

    public void setPreselectedPlant(Plant plant) {
        this.preselectedPlant = plant;
        if (plantComboBox != null) {
            plantComboBox.setValue(plant);
        }
        refreshPlantDerivedFields();
        refreshInteriorBatchPreview();

        if (editingBatchId == null) {
            markCurrentStateAsClean();
        }
    }

    public void setOnSaveSuccess(Runnable onSaveSuccess) {
        this.onSaveSuccess = onSaveSuccess;
    }

    @FXML
    private void save() {
        if (currentStatus == PlantBatchStatus.CANCELLED) {
            DialogUtil.showWarning("Partia anulowana", "Anulowana partia roślin nie może być edytowana.");
            return;
        }

        if (!validateForm()) {
            return;
        }

        try {
            PlantBatch batch = buildBatchFromForm();

            if (editingBatchId == null) {
                plantBatchService.addBatch(batch);
                DialogUtil.showSuccess("Partia roślin została zapisana.");
            } else {
                batch.setId(editingBatchId);
                plantBatchService.updateBatch(batch);
                DialogUtil.showSuccess("Partia roślin została zaktualizowana.");
            }

            if (onSaveSuccess != null) {
                onSaveSuccess.run();
            }
            saveCompleted = true;
            closeConfirmed = true;
            close();
        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Nie udało się zapisać partii roślin.";
            DialogUtil.showError("Błąd zapisu", message);
        }
    }

    @FXML
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

    private void loadPlants() {
        availablePlants = plantRepository.findAll().stream()
                .sorted(Comparator.comparing(this::formatPlantSummary, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (plantComboBox != null) {
            plantComboBox.setItems(FXCollections.observableArrayList(availablePlants));
        }
    }

    private void loadSourceOrigins() {
        availableSourceOrigins = contrahentRepository.findAll().stream()
                .filter(Contrahent::isSupplier)
                .sorted(Comparator.comparing(Contrahent::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        if (sourceOriginComboBox != null) {
            sourceOriginComboBox.setItems(FXCollections.observableArrayList(availableSourceOrigins));
        }
    }

    private void configureComboBoxes() {
        ComboBoxAutoComplete.bindSelectionAutocomplete(plantComboBox, availablePlants, this::formatPlantSummary);
        ComboBoxAutoComplete.bindSelectionAutocomplete(sourceOriginComboBox, availableSourceOrigins, contrahent -> contrahent == null ? "" : safe(contrahent.getName()));
    }

    private void selectPlant(int plantId) {
        if (plantComboBox == null) {
            return;
        }
        for (Plant plant : plantComboBox.getItems()) {
            if (plant.getId() == plantId) {
                plantComboBox.setValue(plant);
                return;
            }
        }
    }

    private void selectSourceOrigin(int contrahentId) {
        if (sourceOriginComboBox == null) {
            return;
        }
        for (Contrahent contrahent : sourceOriginComboBox.getItems()) {
            if (contrahent.getId() == contrahentId) {
                sourceOriginComboBox.setValue(contrahent);
                return;
            }
        }
    }

    private void applyInternalSourceMode(boolean internal) {
        if (sourceOriginComboBox != null) {
            sourceOriginComboBox.setDisable(internal);
            if (internal) {
                sourceOriginComboBox.setValue(null);
            }
        }

        if (internal && manufacturerCountryCodeField != null && isBlank(manufacturerCountryCodeField.getText())) {
            IssuerProfile issuerProfile = appSettingsService.getIssuerProfile();
            if (issuerProfile != null && !isBlank(issuerProfile.getCountryCode())) {
                manufacturerCountryCodeField.setText(issuerProfile.getCountryCode().trim().toUpperCase());
            }
        }
    }

    private void refreshOriginDerivedFields() {
        if (manufacturerCountryCodeField == null) {
            return;
        }

        if (editingBatchId != null && !isBlank(manufacturerCountryCodeField.getText())) {
            return;
        }

        if (internalSourceCheckBox != null && internalSourceCheckBox.isSelected()) {
            IssuerProfile issuerProfile = appSettingsService.getIssuerProfile();
            if (issuerProfile != null && !isBlank(issuerProfile.getCountryCode())) {
                manufacturerCountryCodeField.setText(issuerProfile.getCountryCode().trim().toUpperCase());
            }
            return;
        }

        Contrahent selectedSource = sourceOriginComboBox != null ? sourceOriginComboBox.getValue() : null;
        if (selectedSource != null && !isBlank(selectedSource.getCountryCode())) {
            manufacturerCountryCodeField.setText(selectedSource.getCountryCode().trim().toUpperCase());
        }
    }

    private void refreshPlantDerivedFields() {
        Plant selectedPlant = plantComboBox != null ? plantComboBox.getValue() : null;
        List<EppoCode> linkedCodes = getLinkedCodes(selectedPlant);

        if (eppoCodeField != null && (editingBatchId == null || isBlank(eppoCodeField.getText()))) {
            eppoCodeField.setText(resolveSuggestedEppoCode(selectedPlant, linkedCodes));
        }

        refreshRestrictionsState();
    }

    private void refreshRestrictionsState() {
        Plant selectedPlant = plantComboBox != null ? plantComboBox.getValue() : null;
        String countryCode = manufacturerCountryCodeField == null ? "" : safeUpper(manufacturerCountryCodeField.getText());
        List<EppoCode> linkedCodes = getLinkedCodes(selectedPlant);
        Map<EppoCode, List<EppoZone>> zoneMap = buildZoneMap(linkedCodes);
        boolean hasAnyRestrictions = zoneMap.values().stream().anyMatch(list -> list != null && !list.isEmpty());
        boolean canMarkAsProtected = hasCountryMatch(zoneMap, countryCode);

        if (zpOriginCheckBox != null) {
            if (!canMarkAsProtected) {
                zpOriginCheckBox.setSelected(false);
            }
            zpOriginCheckBox.setDisable(!canMarkAsProtected || currentStatus == PlantBatchStatus.CANCELLED);
        }

        if (zpEligibilityHintLabel != null) {
            boolean showHint = selectedPlant != null && !countryCode.isBlank() && !canMarkAsProtected;
            zpEligibilityHintLabel.setManaged(showHint);
            zpEligibilityHintLabel.setVisible(showHint);
            zpEligibilityHintLabel.setText(showHint
                    ? "Kraj pochodzenia lub roślina nie znajdują się w słownikach EPPO i ZP."
                    : "");
        }

        if (restrictionsInfoArea != null) {
            restrictionsInfoArea.setText(buildRestrictionsMessage(zoneMap, countryCode, zpOriginCheckBox != null && zpOriginCheckBox.isSelected()));
        }
    }

    private List<EppoCode> getLinkedCodes(Plant selectedPlant) {
        if (selectedPlant == null) {
            return List.of();
        }
        return eppoCodePlantLinkService.getCodesForPlant(selectedPlant.getId()).stream()
                .sorted(Comparator.comparing(code -> safeUpper(code.getCode())))
                .toList();
    }

    private Map<EppoCode, List<EppoZone>> buildZoneMap(List<EppoCode> linkedCodes) {
        Map<EppoCode, List<EppoZone>> zoneMap = new LinkedHashMap<>();
        if (linkedCodes == null) {
            return zoneMap;
        }

        for (EppoCode code : linkedCodes) {
            List<EppoZone> zones = eppoCodeZoneLinkService.getZonesForCode(code.getId()).stream()
                    .filter(zone -> zone != null)
                    .sorted(Comparator.comparing(zone -> safeUpper(zone.getCountryCode()) + "|" + safeUpper(zone.getCode()) + "|" + safe(zone.getName())))
                    .toList();
            zoneMap.put(code, zones);
        }
        return zoneMap;
    }

    private boolean hasCountryMatch(Map<EppoCode, List<EppoZone>> zoneMap, String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return false;
        }
        return zoneMap.values().stream()
                .flatMap(List::stream)
                .map(zone -> safeUpper(zone.getCountryCode()))
                .anyMatch(countryCode::equals);
    }

    private String buildRestrictionsMessage(Map<EppoCode, List<EppoZone>> zoneMap, String countryCode, boolean fromProtectedZone) {
        if (zoneMap.isEmpty()) {
            return "Brak obostrzeń dla wybranego gatunku.";
        }

        List<String> codeLines = zoneMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> formatRestrictionLine(entry.getKey(), entry.getValue()))
                .toList();

        if (codeLines.isEmpty()) {
            return "Brak obostrzeń dla wybranego gatunku.";
        }

        StringBuilder message = new StringBuilder();
        message.append("Kod EPPO: kraje, których dotyczy ze słowników EPPO:")
                .append("\n")
                .append(String.join("\n", codeLines));

        boolean hasCountryMatch = hasCountryMatch(zoneMap, countryCode);
        message.append("\n\n");
        if (fromProtectedZone && hasCountryMatch) {
            message.append("W związku z obostrzeniami wybraną partię roślin możesz wysłać do stref chronionych:")
                    .append("\n")
                    .append(String.join("\n", codeLines));
        } else {
            message.append("W związku z obostrzeniami wybranej partii roślin nie możesz wysłać do stref chronionych:")
                    .append("\n")
                    .append(String.join("\n", codeLines));
        }

        return message.toString();
    }

    private String formatRestrictionLine(EppoCode code, List<EppoZone> zones) {
        Set<String> uniqueCountries = new LinkedHashSet<>();
        for (EppoZone zone : zones) {
            String countryCode = safeUpper(zone.getCountryCode());
            String countryName = safe(zone.getName());
            if (!countryCode.isBlank() && !countryName.isBlank()) {
                uniqueCountries.add(countryCode + " (" + countryName + ")");
            } else if (!countryCode.isBlank()) {
                uniqueCountries.add(countryCode);
            } else if (!countryName.isBlank()) {
                uniqueCountries.add(countryName);
            }
        }

        String codeValue = code == null ? "—" : safeUpper(code.getCode());
        String countries = uniqueCountries.isEmpty() ? "brak przypisanych krajów" : String.join(", ", uniqueCountries);
        return codeValue + ": " + countries;
    }

    private String resolveSuggestedEppoCode(Plant selectedPlant, List<EppoCode> linkedCodes) {
        if (linkedCodes != null && !linkedCodes.isEmpty()) {
            return safeUpper(linkedCodes.get(0).getCode());
        }
        if (selectedPlant != null && !isBlank(selectedPlant.getEppoCode())) {
            return safeUpper(selectedPlant.getEppoCode());
        }
        return "";
    }

    private String formatPlantSummary(Plant plant) {
        if (plant == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if (!isBlank(plant.getSpecies())) {
            parts.add(plant.getSpecies().trim());
        }
        if (!isBlank(plant.getRootstock())) {
            parts.add(plant.getRootstock().trim());
        }
        if (!isBlank(plant.getVariety())) {
            parts.add(plant.getVariety().trim());
        }
        if (!parts.isEmpty()) {
            return String.join(" ", parts);
        }
        return safe(plant.getLatinSpeciesName());
    }

    private void configureInputPresentation() {
        installTrimSupport(exteriorBatchNoField);
        installTrimSupport(fitoQualificationCategoryField);
        installTrimSupport(commentsArea);
        installUppercaseSupport(manufacturerCountryCodeField);
        installUppercaseSupport(eppoCodeField);

        if (qtyField != null) {
            qtyField.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().matches("\\d*") ? change : null
            ));
        }
        if (ageYearsField != null) {
            ageYearsField.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().matches("\\d*") ? change : null
            ));
        }
    }

    private void installFieldTooltips() {
        setTooltip(plantComboBox, "Pole wymagane. Wybór rośliny wpływa na obostrzenia EPPO oraz numer partii wewnętrznej.");
        setTooltip(internalSourceCheckBox, "Zaznacz, jeśli partia pochodzi z własnej szkółki. Wtedy źródło pochodzenia nie jest wymagane.");
        setTooltip(sourceOriginComboBox, "Wybierz dostawcę partii, jeśli partia nie jest wewnętrzna.");
        setTooltip(interiorBatchNoField, "Numer partii wewnętrznej jest podglądem nadawanym automatycznie dla partii własnych.");
        setTooltip(exteriorBatchNoField, "Pole opcjonalne. Uzupełnij numer partii od dostawcy, jeśli został nadany zewnętrznie.");
        setTooltip(qtyField, "Pole wymagane. Wpisz ilość jako liczbę całkowitą większą od zera.");
        setTooltip(ageYearsField, "Pole wymagane. Podaj wiek partii w pełnych latach.");
        setTooltip(creationDatePicker, "Data utworzenia partii wpływa na numerację i historię zmian.");
        setTooltip(manufacturerCountryCodeField, "Wpisz kod kraju pochodzenia, np. PL. Pole jest automatycznie uzupełniane dla partii wewnętrznych i zapisywane wielkimi literami.");
        setTooltip(fitoQualificationCategoryField, "Pole opcjonalne. Uzupełnij kategorię kwalifikacji, jeśli jest wymagana dla danej partii.");
        setTooltip(eppoCodeField, "Kod EPPO jest podpowiadany na podstawie wybranej rośliny i słowników EPPO.");
        setTooltip(zpOriginCheckBox, "Zaznacz, jeśli partia pochodzi z obszaru objętego dopasowaną strefą chronioną ZP.");
        setTooltip(restrictionsInfoArea, "Informacja tylko do odczytu. Pokazuje kraje/strefy wynikające z powiązań EPPO dla wybranej rośliny.");
        setTooltip(commentsArea, "Pole opcjonalne na uwagi operacyjne dotyczące partii roślin.");
    }

    private void installTrimSupport(TextField field) {
        if (field == null) {
            return;
        }

        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                field.setText(normalizeSpaces(field.getText()));
            }
        });
    }

    private void installTrimSupport(TextArea area) {
        if (area == null) {
            return;
        }

        area.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                area.setText(normalizeSpaces(area.getText()));
            }
        });
    }

    private void installUppercaseSupport(TextField field) {
        if (field == null) {
            return;
        }

        field.textProperty().addListener((obs, oldValue, newValue) -> {
            String normalized = normalizeUppercase(newValue);
            if (!Objects.equals(newValue, normalized)) {
                field.setText(normalized);
                field.positionCaret(normalized.length());
            }
        });
    }

    private void setTooltip(javafx.scene.control.Control control, String text) {
        if (control != null && text != null && !text.isBlank()) {
            control.setTooltip(new Tooltip(text));
        }
    }

    private void configureInfoArea(TextArea area) {
        if (area == null) {
            return;
        }
        area.setEditable(false);
        area.setWrapText(true);
        area.setFocusTraversable(false);
    }

    private void refreshInteriorBatchPreview() {
        if (interiorBatchNoField == null || editingBatchId != null) {
            return;
        }

        if (internalSourceCheckBox == null || !internalSourceCheckBox.isSelected()) {
            interiorBatchNoField.clear();
            return;
        }

        try {
            PlantBatch previewContext = buildPreviewContext();
            String previewNumber = plantBatchService.previewNextInternalBatchNumber(previewContext);
            interiorBatchNoField.setText(previewNumber != null ? previewNumber : "");
        } catch (Exception e) {
            interiorBatchNoField.clear();
        }
    }

    private void refreshAgeFromCreationDate() {
        if (ageYearsField == null || editingBatchId != null || !isBlank(ageYearsField.getText())) {
            return;
        }
        ageYearsField.setText(String.valueOf(deriveAgeYears(creationDatePicker == null ? null : creationDatePicker.getValue())));
    }

    private int deriveAgeYears(LocalDate creationDate) {
        if (creationDate == null) {
            return 1;
        }
        LocalDate referenceDate = LocalDate.now();
        if (referenceDate.isBefore(creationDate)) {
            return 1;
        }
        return Math.max(1, Period.between(creationDate, referenceDate).getYears());
    }

    private PlantBatch buildPreviewContext() {
        PlantBatch batch = new PlantBatch();
        batch.setInternalSource(true);

        if (plantComboBox != null && plantComboBox.getValue() != null) {
            batch.setPlantId(plantComboBox.getValue().getId());
        }
        if (exteriorBatchNoField != null) {
            batch.setExteriorBatchNo(normalizeSpaces(exteriorBatchNoField.getText()));
        }
        if (creationDatePicker != null) {
            batch.setCreationDate(creationDatePicker.getValue() != null ? creationDatePicker.getValue() : LocalDate.now());
        } else {
            batch.setCreationDate(LocalDate.now());
        }
        return batch;
    }

    private boolean validateForm() {
        if (plantComboBox == null || plantComboBox.getValue() == null) {
            DialogUtil.showWarning("Brak danych", "Wybierz roślinę.");
            return false;
        }

        if (internalSourceCheckBox != null && !internalSourceCheckBox.isSelected()
                && (sourceOriginComboBox == null || sourceOriginComboBox.getValue() == null)) {
            DialogUtil.showWarning("Brak danych", "Wybierz źródło pochodzenia albo zaznacz „Wewnętrzne”.");
            return false;
        }

        if (isBlank(qtyField == null ? null : qtyField.getText())) {
            DialogUtil.showWarning("Brak danych", "Uzupełnij ilość.");
            return false;
        }
        if (isBlank(ageYearsField == null ? null : ageYearsField.getText())) {
            DialogUtil.showWarning("Brak danych", "Uzupełnij wiek partii w latach.");
            return false;
        }

        try {
            int qty = Integer.parseInt(qtyField.getText().trim());
            if (qty <= 0) {
                DialogUtil.showWarning("Nieprawidłowa ilość", "Ilość musi być większa od zera.");
                return false;
            }
        } catch (NumberFormatException e) {
            DialogUtil.showWarning("Nieprawidłowa ilość", "Ilość musi być liczbą całkowitą.");
            return false;
        }

        try {
            int ageYears = Integer.parseInt(ageYearsField.getText().trim());
            if (ageYears <= 0) {
                DialogUtil.showWarning("Nieprawidłowy wiek", "Wiek partii musi być większy od zera.");
                return false;
            }
        } catch (NumberFormatException e) {
            DialogUtil.showWarning("Nieprawidłowy wiek", "Wiek partii musi być liczbą całkowitą.");
            return false;
        }

        return true;
    }

    private PlantBatch buildBatchFromForm() {
        PlantBatch batch = new PlantBatch();
        batch.setPlantId(plantComboBox.getValue().getId());
        batch.setInteriorBatchNo(normalizeUppercase(interiorBatchNoField == null ? null : interiorBatchNoField.getText()));
        batch.setExteriorBatchNo(normalizeSpaces(exteriorBatchNoField == null ? null : exteriorBatchNoField.getText()));
        batch.setQty(Integer.parseInt(qtyField.getText().trim()));
        batch.setAgeYears(Integer.parseInt(ageYearsField.getText().trim()));
        batch.setCreationDate(creationDatePicker == null ? null : creationDatePicker.getValue());
        batch.setManufacturerCountryCode(normalizeUppercase(manufacturerCountryCodeField == null ? null : manufacturerCountryCodeField.getText()));
        batch.setFitoQualificationCategory(normalizeSpaces(fitoQualificationCategoryField == null ? null : fitoQualificationCategoryField.getText()));
        batch.setEppoCode(normalizeUppercase(eppoCodeField == null ? null : eppoCodeField.getText()));
        batch.setZpZone(resolveSelectedZpZoneCodes());
        batch.setInternalSource(internalSourceCheckBox != null && internalSourceCheckBox.isSelected());
        batch.setContrahentId(batch.isInternalSource() || sourceOriginComboBox == null || sourceOriginComboBox.getValue() == null
                ? 0
                : sourceOriginComboBox.getValue().getId());
        batch.setComments(normalizeSpaces(commentsArea == null ? null : commentsArea.getText()));
        batch.setStatus(currentStatus);
        return batch;
    }

    private String resolveSelectedZpZoneCodes() {
        if (zpOriginCheckBox == null || !zpOriginCheckBox.isSelected()) {
            return "";
        }

        Plant selectedPlant = plantComboBox == null ? null : plantComboBox.getValue();
        String countryCode = manufacturerCountryCodeField == null ? "" : safeUpper(manufacturerCountryCodeField.getText());
        if (selectedPlant == null || countryCode.isBlank()) {
            return "";
        }

        List<String> zoneCodes = getLinkedCodes(selectedPlant).stream()
                .flatMap(code -> eppoCodeZoneLinkService.getZonesForCode(code.getId()).stream())
                .filter(zone -> zone != null && countryCode.equals(safeUpper(zone.getCountryCode())))
                .map(EppoZone::getCode)
                .map(this::safeUpper)
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();

        return zoneCodes.isEmpty() ? "" : String.join(", ", zoneCodes);
    }

    private void applyCancelledMode() {
        disableEditorControls(true);

        if (saveButton != null) {
            saveButton.setDisable(true);
            saveButton.setText("Partia anulowana");
        }

        DialogUtil.showWarning(
                "Partia anulowana",
                "Otwarta partia roślin jest anulowana i została przełączona w tryb tylko do odczytu."
        );
    }

    private void disableEditorControls(boolean disabled) {
        setDisableIfPresent(plantComboBox, disabled);
        setDisableIfPresent(internalSourceCheckBox, disabled);
        setDisableIfPresent(sourceOriginComboBox, disabled);
        setDisableIfPresent(interiorBatchNoField, true);
        setDisableIfPresent(exteriorBatchNoField, disabled);
        setDisableIfPresent(qtyField, disabled);
        setDisableIfPresent(ageYearsField, disabled);
        setDisableIfPresent(creationDatePicker, disabled);
        setDisableIfPresent(manufacturerCountryCodeField, disabled);
        setDisableIfPresent(fitoQualificationCategoryField, disabled);
        setDisableIfPresent(eppoCodeField, disabled);
        setDisableIfPresent(zpOriginCheckBox, disabled);
        setDisableIfPresent(commentsArea, disabled);
        setDisableIfPresent(restrictionsInfoArea, true);
    }

    private void setDisableIfPresent(Node node, boolean disabled) {
        if (node != null) {
            node.setDisable(disabled);
        }
    }

    private String safeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizeUppercase(String value) {
        return normalizeSpaces(value).toUpperCase();
    }

    private String normalizeSpaces(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? "" : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private boolean canCloseWindow() {
        if (closeConfirmed || saveCompleted || !hasUnsavedChanges()) {
            return true;
        }
        return DialogUtil.confirmDiscardChanges(editingBatchId == null ? "partii roślin" : "edycji partii roślin");
    }

    private boolean hasUnsavedChanges() {
        return !Objects.equals(initialSnapshot, buildFormSnapshot());
    }

    private void markCurrentStateAsClean() {
        initialSnapshot = buildFormSnapshot();
        saveCompleted = false;
        closeConfirmed = false;
    }

    private String buildFormSnapshot() {
        Plant selectedPlant = plantComboBox == null ? null : plantComboBox.getValue();
        Contrahent selectedSource = sourceOriginComboBox == null ? null : sourceOriginComboBox.getValue();

        return String.join("|",
                String.valueOf(selectedPlant == null ? 0 : selectedPlant.getId()),
                String.valueOf(internalSourceCheckBox != null && internalSourceCheckBox.isSelected()),
                String.valueOf(selectedSource == null ? 0 : selectedSource.getId()),
                safeUpper(interiorBatchNoField == null ? null : interiorBatchNoField.getText()),
                safeUpper(exteriorBatchNoField == null ? null : exteriorBatchNoField.getText()),
                safeUpper(qtyField == null ? null : qtyField.getText()),
                safeUpper(ageYearsField == null ? null : ageYearsField.getText()),
                String.valueOf(creationDatePicker == null ? null : creationDatePicker.getValue()),
                safeUpper(manufacturerCountryCodeField == null ? null : manufacturerCountryCodeField.getText()),
                safeUpper(fitoQualificationCategoryField == null ? null : fitoQualificationCategoryField.getText()),
                safeUpper(eppoCodeField == null ? null : eppoCodeField.getText()),
                String.valueOf(zpOriginCheckBox != null && zpOriginCheckBox.isSelected()),
                safeUpper(commentsArea == null ? null : commentsArea.getText()),
                String.valueOf(currentStatus)
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

            if (!DialogUtil.confirmDiscardChanges(editingBatchId == null ? "partii roślin" : "edycji partii roślin")) {
                event.consume();
                return;
            }

            closeConfirmed = true;
        });
        closeGuardInstalled = true;
    }

    private Stage resolveStage() {
        Node owner = cancelButton != null ? cancelButton : (saveButton != null ? saveButton : plantComboBox);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }
}
