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
import com.egen.fitogen.service.EppoAdvisoryService;
import com.egen.fitogen.service.EppoCodePlantLinkService;
import com.egen.fitogen.service.EppoCodeZoneLinkService;
import com.egen.fitogen.service.PlantBatchService;
import com.egen.fitogen.ui.util.DialogUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.egen.fitogen.service.PassportAdvisoryService;

public class PlantBatchFormController {

    @FXML private ComboBox<Plant> plantComboBox;
    @FXML private CheckBox internalSourceCheckBox;
    @FXML private ComboBox<Contrahent> sourceOriginComboBox;
    @FXML private TextField interiorBatchNoField;
    @FXML private TextField exteriorBatchNoField;
    @FXML private TextField qtyField;
    @FXML private DatePicker creationDatePicker;
    @FXML private TextField manufacturerCountryCodeField;
    @FXML private TextField fitoQualificationCategoryField;
    @FXML private TextField eppoCodeField;
    @FXML private TextField zpZoneField;
    @FXML private TextArea eppoLinkedCodesInfoArea;
    @FXML private TextArea eppoProtectedZonesInfoArea;
    @FXML private TextArea eppoZpSuggestionInfoArea;
    @FXML private TextArea passportAdvisoryInfoArea;
    @FXML private TextArea commentsArea;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final PlantBatchService plantBatchService = AppContext.getPlantBatchService();
    private final PlantRepository plantRepository = new SqlitePlantRepository();
    private final ContrahentRepository contrahentRepository = new SqliteContrahentRepository();
    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final EppoAdvisoryService eppoAdvisoryService = AppContext.getEppoAdvisoryService();
    private final EppoCodePlantLinkService eppoCodePlantLinkService = AppContext.getEppoCodePlantLinkService();
    private final EppoCodeZoneLinkService eppoCodeZoneLinkService = AppContext.getEppoCodeZoneLinkService();

    private Integer editingBatchId;
    private PlantBatchStatus currentStatus = PlantBatchStatus.ACTIVE;
    private Plant preselectedPlant;
    private Runnable onSaveSuccess;
    private String initialSnapshot = "";
    private boolean saveCompleted;
    private boolean closeConfirmed;
    private boolean closeGuardInstalled;

    private final PassportAdvisoryService passportAdvisoryService = AppContext.getPassportAdvisoryService();

    @FXML
    public void initialize() {
        loadPlants();
        loadSourceOrigins();

        if (interiorBatchNoField != null) {
            interiorBatchNoField.setDisable(true);
        }

        configureInputPresentation();
        installFieldTooltips();

        configureInfoArea(eppoLinkedCodesInfoArea);
        configureInfoArea(eppoProtectedZonesInfoArea);
        configureInfoArea(eppoZpSuggestionInfoArea);
        configureInfoArea(passportAdvisoryInfoArea);

        if (preselectedPlant != null && plantComboBox != null) {
            plantComboBox.setValue(preselectedPlant);
        }

        if (creationDatePicker != null && creationDatePicker.getValue() == null) {
            creationDatePicker.setValue(LocalDate.now());
        }

        if (internalSourceCheckBox != null) {
            internalSourceCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                applyInternalSourceMode(newVal);
                refreshOriginDerivedFields();
                refreshInteriorBatchPreview();
            });
            applyInternalSourceMode(internalSourceCheckBox.isSelected());
        }

        if (sourceOriginComboBox != null) {
            sourceOriginComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                refreshOriginDerivedFields();
                refreshInteriorBatchPreview();
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
            manufacturerCountryCodeField.textProperty().addListener((obs, oldVal, newVal) -> refreshPlantDerivedFields());
        }

        if (creationDatePicker != null) {
            creationDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> refreshInteriorBatchPreview());
        }

        refreshOriginDerivedFields();
        refreshPlantDerivedFields();
        refreshInteriorBatchPreview();

        Platform.runLater(() -> {
            installWindowCloseGuard();
            markCurrentStateAsClean();
        });
    }

    public void setPlantBatch(PlantBatch plantBatch) {
        if (plantBatch == null) {
            return;
        }

        editingBatchId = plantBatch.getId();
        currentStatus = plantBatch.getStatus() != null ? plantBatch.getStatus() : PlantBatchStatus.ACTIVE;

        selectPlant(plantBatch.getPlantId());
        internalSourceCheckBox.setSelected(plantBatch.isInternalSource());
        interiorBatchNoField.setText(plantBatch.getInteriorBatchNo());
        exteriorBatchNoField.setText(plantBatch.getExteriorBatchNo());
        qtyField.setText(String.valueOf(plantBatch.getQty()));
        creationDatePicker.setValue(plantBatch.getCreationDate());
        manufacturerCountryCodeField.setText(plantBatch.getManufacturerCountryCode());
        fitoQualificationCategoryField.setText(plantBatch.getFitoQualificationCategory());
        eppoCodeField.setText(plantBatch.getEppoCode());
        zpZoneField.setText(plantBatch.getZpZone());
        commentsArea.setText(plantBatch.getComments());

        if (plantBatch.isInternalSource()) {
            sourceOriginComboBox.setValue(null);
            applyInternalSourceMode(true);
        } else {
            selectSourceOrigin(plantBatch.getContrahentId());
            applyInternalSourceMode(false);
        }

        refreshPlantDerivedFields();

        if (currentStatus == PlantBatchStatus.CANCELLED) {
            applyCancelledMode();
        }

        markCurrentStateAsClean();
    }

    @FXML
    private void save() {
        if (currentStatus == PlantBatchStatus.CANCELLED) {
            DialogUtil.showWarning(
                    "Partia anulowana",
                    "Anulowana partia roślin nie może być edytowana."
            );
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
                if (onSaveSuccess != null) {
                    onSaveSuccess.run();
                }
            } else {
                batch.setId(editingBatchId);
                plantBatchService.updateBatch(batch);
                DialogUtil.showSuccess("Partia roślin została zaktualizowana.");
                if (onSaveSuccess != null) {
                    onSaveSuccess.run();
                }
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

    private void loadPlants() {
        List<Plant> plants = plantRepository.findAll();
        plantComboBox.setItems(FXCollections.observableArrayList(plants));
    }

    private void loadSourceOrigins() {
        List<Contrahent> sourceOrigins = contrahentRepository.findAll().stream()
                .filter(Contrahent::isSupplier)
                .toList();
        sourceOriginComboBox.setItems(FXCollections.observableArrayList(sourceOrigins));
    }

    private void selectPlant(int plantId) {
        for (Plant plant : plantComboBox.getItems()) {
            if (plant.getId() == plantId) {
                plantComboBox.setValue(plant);
                break;
            }
        }
    }

    private void selectSourceOrigin(int contrahentId) {
        for (Contrahent contrahent : sourceOriginComboBox.getItems()) {
            if (contrahent.getId() == contrahentId) {
                sourceOriginComboBox.setValue(contrahent);
                break;
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

        if (internal && manufacturerCountryCodeField != null
                && isBlank(manufacturerCountryCodeField.getText())) {
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

        if (eppoCodeField != null && !(editingBatchId != null && !isBlank(eppoCodeField.getText()))) {
            if (selectedPlant != null && !isBlank(selectedPlant.getEppoCode())) {
                eppoCodeField.setText(selectedPlant.getEppoCode().trim().toUpperCase());
            } else if (editingBatchId == null) {
                eppoCodeField.clear();
            }
        }

        refreshEppoInformation(selectedPlant);
        refreshZpSuggestion(selectedPlant);
        refreshPassportAdvisory(selectedPlant);
    }

    private void refreshEppoInformation(Plant selectedPlant) {
        if (eppoLinkedCodesInfoArea == null || eppoProtectedZonesInfoArea == null) {
            return;
        }

        if (selectedPlant == null) {
            eppoLinkedCodesInfoArea.setText("Wybierz roślinę, aby zobaczyć powiązania EPPO.");
            eppoProtectedZonesInfoArea.setText("Brak danych.");
            return;
        }

        List<EppoCode> linkedCodes = eppoCodePlantLinkService.getCodesForPlant(selectedPlant.getId()).stream()
                .sorted(Comparator.comparing(code -> safeUpper(code.getCode())))
                .toList();

        if (linkedCodes.isEmpty()) {
            if (!isBlank(selectedPlant.getEppoCode())) {
                eppoLinkedCodesInfoArea.setText("Powiązany kod z karty rośliny: " + selectedPlant.getEppoCode().trim().toUpperCase());
            } else {
                eppoLinkedCodesInfoArea.setText("Dla tej rośliny nie ma jeszcze przypisanych kodów EPPO w module EPPO Admin.");
            }
            eppoProtectedZonesInfoArea.setText("Brak przypisanych stref chronionych dla tej rośliny.");
            return;
        }

        String linkedCodesText = linkedCodes.stream()
                .map(this::formatCodeSummary)
                .collect(Collectors.joining("\n"));
        eppoLinkedCodesInfoArea.setText(linkedCodesText);

        Map<Integer, EppoZone> uniqueZones = new LinkedHashMap<>();
        for (EppoCode code : linkedCodes) {
            List<EppoZone> zones = eppoCodeZoneLinkService.getZonesForCode(code.getId());
            for (EppoZone zone : zones) {
                uniqueZones.putIfAbsent(zone.getId(), zone);
            }
        }

        if (uniqueZones.isEmpty()) {
            eppoProtectedZonesInfoArea.setText("Dla powiązanych kodów EPPO nie przypisano jeszcze stref chronionych.");
            return;
        }

        String zonesText = uniqueZones.values().stream()
                .sorted(Comparator.comparing(zone -> safeUpper(zone.getCode()) + "|" + safeUpper(zone.getName())))
                .map(this::formatZoneSummary)
                .collect(Collectors.joining("\n"));
        eppoProtectedZonesInfoArea.setText(zonesText);
    }

    private void refreshZpSuggestion(Plant selectedPlant) {
        if (zpZoneField == null || eppoZpSuggestionInfoArea == null) {
            return;
        }

        if (selectedPlant == null) {
            eppoZpSuggestionInfoArea.setText("Wybierz roślinę, aby sprawdzić możliwe oznaczenie ZP.");
            if (editingBatchId == null) {
                zpZoneField.clear();
            }
            return;
        }

        String countryCode = manufacturerCountryCodeField != null ? safeUpper(manufacturerCountryCodeField.getText()) : "";
        if (isBlank(countryCode)) {
            eppoZpSuggestionInfoArea.setText("Uzupełnij kraj pochodzenia, aby sprawdzić dopasowanie stref chronionych EPPO.");
            if (editingBatchId == null) {
                zpZoneField.clear();
            }
            return;
        }

        List<EppoCode> linkedCodes = eppoCodePlantLinkService.getCodesForPlant(selectedPlant.getId());
        if (linkedCodes.isEmpty()) {
            eppoZpSuggestionInfoArea.setText("Brak powiązań EPPO dla tej rośliny — nie można zasugerować oznaczenia ZP.");
            if (editingBatchId == null) {
                zpZoneField.clear();
            }
            return;
        }

        List<EppoZone> matchingZones = linkedCodes.stream()
                .flatMap(code -> eppoCodeZoneLinkService.getZonesForCode(code.getId()).stream())
                .filter(zone -> countryCode.equals(safeUpper(zone.getCountryCode())))
                .sorted(Comparator.comparing(zone -> safeUpper(zone.getCode()) + "|" + safeUpper(zone.getName())))
                .toList();

        if (matchingZones.isEmpty()) {
            eppoZpSuggestionInfoArea.setText("Dla kraju " + countryCode + " nie znaleziono dopasowanej strefy chronionej EPPO dla wybranej rośliny.");
            if (editingBatchId == null) {
                zpZoneField.clear();
            }
            return;
        }

        if (matchingZones.size() == 1) {
            EppoZone zone = matchingZones.get(0);
            eppoZpSuggestionInfoArea.setText("Znaleziono 1 dopasowanie dla kraju " + countryCode + ":" + formatZoneSummary(zone) + "Pole ZP zostało uzupełnione automatycznie.");
            if (zpZoneField != null && (editingBatchId == null || isBlank(zpZoneField.getText()))) {
                zpZoneField.setText(safeUpper(zone.getCode()));
            }
            return;
        }

        String matchesText = matchingZones.stream()
                .map(this::formatZoneSummary)
                .distinct()
                .collect(Collectors.joining(""));

        eppoZpSuggestionInfoArea.setText("Dla kraju " + countryCode + " znaleziono kilka możliwych stref chronionych EPPO. " + "Wybierz właściwe oznaczenie ZP ręcznie: "
                        + matchesText);

        if (editingBatchId == null) {
            zpZoneField.clear();
        }
    }


    private void refreshPassportAdvisory(Plant selectedPlant) {
        if (passportAdvisoryInfoArea == null) {
            return;
        }

        if (selectedPlant == null) {
            passportAdvisoryInfoArea.setText("Wybierz roślinę, aby zobaczyć informację paszportową.");
            return;
        }

        PassportAdvisoryService.AdvisoryResult passportResult = passportAdvisoryService.analyzePlant(selectedPlant);
        StringBuilder message = new StringBuilder(passportResult.message());

        String countryCode = manufacturerCountryCodeField != null ? safeUpper(manufacturerCountryCodeField.getText()) : "";
        if (!isBlank(countryCode)) {
            EppoAdvisoryService.AdvisoryResult advisoryResult = eppoAdvisoryService
                    .analyzePlantForCountry(selectedPlant, countryCode, 1, "Partia");
            if (!isBlank(advisoryResult.message())) {
                message.append("\n\n").append(advisoryResult.message());
            }
        }

        passportAdvisoryInfoArea.setText(message.toString());
    }


    private String formatPlantSummary(Plant plant) {
        StringBuilder sb = new StringBuilder();

        if (!isBlank(plant.getSpecies())) {
            sb.append(plant.getSpecies().trim());
        }
        if (!isBlank(plant.getVariety())) {
            if (!sb.isEmpty()) {
                sb.append(" / ");
            }
            sb.append(plant.getVariety().trim());
        }
        if (!isBlank(plant.getRootstock())) {
            if (!sb.isEmpty()) {
                sb.append(" / ");
            }
            sb.append(plant.getRootstock().trim());
        }
        if (sb.isEmpty() && !isBlank(plant.getLatinSpeciesName())) {
            sb.append(plant.getLatinSpeciesName().trim());
        }
        if (sb.isEmpty()) {
            sb.append("ID ").append(plant.getId());
        }

        return sb.toString();
    }

    private String formatCodeSummary(EppoCode code) {
        StringBuilder sb = new StringBuilder();
        sb.append(safeUpper(code.getCode()));

        if (!isBlank(code.getScientificName())) {
            sb.append(" — ").append(code.getScientificName().trim());
        } else if (!isBlank(code.getCommonName())) {
            sb.append(" — ").append(code.getCommonName().trim());
        }

        if (code.isPassportRequired()) {
            sb.append(" [paszport wymagany]");
        }

        return sb.toString();
    }

    private String formatZoneSummary(EppoZone zone) {
        StringBuilder sb = new StringBuilder();

        if (!isBlank(zone.getCode())) {
            sb.append(zone.getCode().trim().toUpperCase());
        }

        if (!isBlank(zone.getName())) {
            if (!sb.isEmpty()) {
                sb.append(" — ");
            }
            sb.append(zone.getName().trim());
        }

        if (!isBlank(zone.getCountryCode())) {
            if (!sb.isEmpty()) {
                sb.append(" (");
            }
            sb.append(zone.getCountryCode().trim().toUpperCase());
            if (sb.charAt(sb.length() - 1) != ')') {
                sb.append(")");
            }
        }

        return sb.toString();
    }

    private void configureInputPresentation() {
        installTrimSupport(exteriorBatchNoField);
        installTrimSupport(fitoQualificationCategoryField);
        installTrimSupport(commentsArea);
        installUppercaseSupport(manufacturerCountryCodeField);
        installUppercaseSupport(eppoCodeField);
        installUppercaseSupport(zpZoneField);

        if (qtyField != null) {
            qtyField.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().matches("\\d*") ? change : null
            ));
        }
    }

    private void installFieldTooltips() {
        setTooltip(plantComboBox, "Pole wymagane. Wybór rośliny wpływa na sugestie EPPO, paszport i numer partii wewnętrznej.");
        setTooltip(internalSourceCheckBox, "Zaznacz, jeśli partia pochodzi z własnej szkółki. Wtedy źródło pochodzenia nie jest wymagane.");
        setTooltip(sourceOriginComboBox, "Wybierz dostawcę partii, jeśli partia nie jest wewnętrzna.");
        setTooltip(interiorBatchNoField, "Numer partii wewnętrznej jest podglądem nadawanym automatycznie dla partii własnych.");
        setTooltip(exteriorBatchNoField, "Pole opcjonalne. Uzupełnij numer partii od dostawcy, jeśli został nadany zewnętrznie.");
        setTooltip(qtyField, "Pole wymagane. Wpisz ilość jako liczbę całkowitą większą od zera.");
        setTooltip(creationDatePicker, "Data utworzenia partii wpływa na numerację i historię zmian.");
        setTooltip(manufacturerCountryCodeField, "Wpisz kod kraju pochodzenia, np. PL. Pole jest automatycznie uzupełniane dla partii wewnętrznych i zapisywane wielkimi literami.");
        setTooltip(fitoQualificationCategoryField, "Pole opcjonalne. Uzupełnij kategorię kwalifikacji, jeśli jest wymagana dla danej partii.");
        setTooltip(eppoCodeField, "Kod EPPO może zostać zasugerowany na podstawie wybranej rośliny i słowników EPPO.");
        setTooltip(zpZoneField, "Pole strefy chronionej ZP. Możesz wpisać wartość ręcznie albo skorzystać z podpowiedzi poniżej.");
        setTooltip(eppoLinkedCodesInfoArea, "Informacja tylko do odczytu. Pokazuje powiązane kody EPPO dla wybranej rośliny.");
        setTooltip(eppoProtectedZonesInfoArea, "Informacja tylko do odczytu. Pokazuje strefy chronione przypisane do powiązanych kodów EPPO.");
        setTooltip(eppoZpSuggestionInfoArea, "Informacja tylko do odczytu. Podpowiada strefę ZP na podstawie rośliny i kraju pochodzenia.");
        setTooltip(passportAdvisoryInfoArea, "Informacja tylko do odczytu. Pokazuje zalecenia paszportowe wynikające z rośliny i słowników EPPO.");
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
        if (interiorBatchNoField == null) {
            return;
        }

        if (editingBatchId != null) {
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
        if (plantComboBox.getValue() == null) {
            DialogUtil.showWarning("Brak danych", "Wybierz roślinę.");
            return false;
        }

        if (!internalSourceCheckBox.isSelected() && sourceOriginComboBox.getValue() == null) {
            DialogUtil.showWarning("Brak danych", "Wybierz źródło pochodzenia albo zaznacz „Wewnętrzne”.");
            return false;
        }

        if (qtyField.getText() == null || qtyField.getText().isBlank()) {
            DialogUtil.showWarning("Brak danych", "Uzupełnij ilość.");
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

        return true;
    }

    private PlantBatch buildBatchFromForm() {
        PlantBatch batch = new PlantBatch();
        batch.setPlantId(plantComboBox.getValue().getId());
        batch.setInteriorBatchNo(normalizeUppercase(interiorBatchNoField.getText()));
        batch.setExteriorBatchNo(normalizeSpaces(exteriorBatchNoField.getText()));
        batch.setQty(Integer.parseInt(qtyField.getText().trim()));
        batch.setCreationDate(creationDatePicker.getValue());
        batch.setManufacturerCountryCode(normalizeUppercase(manufacturerCountryCodeField.getText()));
        batch.setFitoQualificationCategory(normalizeSpaces(fitoQualificationCategoryField.getText()));
        batch.setEppoCode(normalizeUppercase(eppoCodeField.getText()));
        batch.setZpZone(normalizeUppercase(zpZoneField.getText()));
        batch.setInternalSource(internalSourceCheckBox.isSelected());
        batch.setContrahentId(
                internalSourceCheckBox.isSelected()
                        ? 0
                        : sourceOriginComboBox.getValue().getId()
        );
        batch.setComments(normalizeSpaces(commentsArea.getText()));
        batch.setStatus(currentStatus);
        return batch;
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
        setDisableIfPresent(interiorBatchNoField, disabled);
        setDisableIfPresent(exteriorBatchNoField, disabled);
        setDisableIfPresent(qtyField, disabled);
        setDisableIfPresent(creationDatePicker, disabled);
        setDisableIfPresent(manufacturerCountryCodeField, disabled);
        setDisableIfPresent(fitoQualificationCategoryField, disabled);
        setDisableIfPresent(eppoCodeField, disabled);
        setDisableIfPresent(zpZoneField, disabled);
        setDisableIfPresent(eppoLinkedCodesInfoArea, disabled);
        setDisableIfPresent(eppoProtectedZonesInfoArea, disabled);
        setDisableIfPresent(eppoZpSuggestionInfoArea, disabled);
        setDisableIfPresent(commentsArea, disabled);
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

        String normalized = value.trim().replaceAll("\s+", " ");
        return normalized.isBlank() ? "" : normalized;
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
        return !java.util.Objects.equals(initialSnapshot, buildFormSnapshot());
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
                String.valueOf(creationDatePicker == null ? null : creationDatePicker.getValue()),
                safeUpper(manufacturerCountryCodeField == null ? null : manufacturerCountryCodeField.getText()),
                safeUpper(fitoQualificationCategoryField == null ? null : fitoQualificationCategoryField.getText()),
                safeUpper(eppoCodeField == null ? null : eppoCodeField.getText()),
                safeUpper(zpZoneField == null ? null : zpZoneField.getText()),
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
