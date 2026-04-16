package com.egen.fitogen.ui.controller;

import javafx.application.Platform;
import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.database.SqliteContrahentRepository;
import com.egen.fitogen.database.SqlitePlantBatchRepository;
import com.egen.fitogen.database.SqlitePlantRepository;
import com.egen.fitogen.dto.DocumentDTO;
import com.egen.fitogen.dto.DocumentPreviewDTO;
import com.egen.fitogen.dto.DocumentItemDTO;
import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoZone;
import com.egen.fitogen.model.DocumentStatus;
import com.egen.fitogen.model.DocumentType;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.model.PlantBatchStatus;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.repository.PlantBatchRepository;
import com.egen.fitogen.repository.PlantRepository;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.AppUserService;
import com.egen.fitogen.service.DocumentService;
import com.egen.fitogen.service.DocumentTypeService;
import com.egen.fitogen.service.EppoCodeService;
import com.egen.fitogen.service.EppoCodeSpeciesLinkService;
import com.egen.fitogen.service.EppoAdvisoryService;
import com.egen.fitogen.service.EppoCodePlantLinkService;
import com.egen.fitogen.service.EppoCodeZoneLinkService;
import com.egen.fitogen.service.DocumentPdfService;
import com.egen.fitogen.service.DocumentRenderService;
import com.egen.fitogen.service.PassportAdvisoryService;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ModalViewUtil;
import com.egen.fitogen.ui.util.UiTextUtil;
import com.egen.fitogen.ui.util.WindowSizingUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


public class DocumentFormController {

    @FXML private TextField documentNumberField;
    @FXML private ComboBox<DocumentType> documentTypeBox;
    @FXML private ComboBox<Contrahent> contrahentBox;
    @FXML private DatePicker issueDateField;
    @FXML private ComboBox<AppUser> createdByBox;
    @FXML private TextArea commentsField;
    @FXML private Label statusValueLabel;
    @FXML private CheckBox printPassportsBox;
    @FXML private TableView<DocumentItemRow> itemsTable;
    @FXML private TableColumn<DocumentItemRow, Number> colLp;
    @FXML private TableColumn<DocumentItemRow, DocumentItemRow> colPlant;
    @FXML private TableColumn<DocumentItemRow, DocumentItemRow> colBatch;
    @FXML private TableColumn<DocumentItemRow, Number> colQty;
    @FXML private TableColumn<DocumentItemRow, Boolean> colPassport;
    @FXML private TableColumn<DocumentItemRow, DocumentItemRow> colActions;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button previewButton;
    @FXML private Button pdfButton;

    private final DocumentService documentService = AppContext.getDocumentService();
    private final DocumentTypeService documentTypeService = AppContext.getDocumentTypeService();
    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final AppUserService appUserService = AppContext.getAppUserService();
    private final DocumentRenderService documentRenderService = new DocumentRenderService(documentService);
    private final DocumentPdfService documentPdfService = new DocumentPdfService();
    private final EppoCodePlantLinkService eppoCodePlantLinkService = AppContext.getEppoCodePlantLinkService();
    private final EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService = AppContext.getEppoCodeSpeciesLinkService();
    private final EppoCodeService eppoCodeService = AppContext.getEppoCodeService();
    private final EppoCodeZoneLinkService eppoCodeZoneLinkService = AppContext.getEppoCodeZoneLinkService();
    private final EppoAdvisoryService eppoAdvisoryService = AppContext.getEppoAdvisoryService();
    private final PassportAdvisoryService passportAdvisoryService = AppContext.getPassportAdvisoryService();
    private final ContrahentRepository contrahentRepository = new SqliteContrahentRepository();
    private final PlantBatchRepository plantBatchRepository = new SqlitePlantBatchRepository();
    private final PlantRepository plantRepository = new SqlitePlantRepository();

    private final ObservableList<DocumentType> allDocumentTypes = FXCollections.observableArrayList();
    private final ObservableList<AppUser> allUsers = FXCollections.observableArrayList();
    private final ObservableList<Contrahent> allContrahents = FXCollections.observableArrayList();
    private final ObservableList<Plant> allPlants = FXCollections.observableArrayList();
    private final ObservableList<PlantBatch> allBatches = FXCollections.observableArrayList();
    private final ObservableList<DocumentItemRow> rows = FXCollections.observableArrayList();
    private final Map<Integer, Contrahent> contrahentById = new HashMap<>();
    private final Map<Integer, Plant> plantById = new HashMap<>();

    private Document document;
    private boolean internalRowChange;

    @FXML
    public void initialize() {
        loadReferenceData();
        configureDocumentTypeBox();
        configureUsersBox();
        configureContrahentBox();
        configureTable();
        configurePrintPassportsBox();
        configureActionButtons();

        issueDateField.setValue(LocalDate.now());
        updateStatusLabel(DocumentStatus.ACTIVE);
        documentNumberField.setEditable(false);
        documentNumberField.setText("Zostanie nadany automatycznie przy zapisie");

        selectDefaultUser();
        ensureTrailingBlankRow();
        refreshValidationIndicators();
    }

    public void setDocument(Document document) {
        this.document = document;
        DocumentDTO dto = documentService.getDocumentDetails(document.getId());
        if (dto == null) {
            DialogUtil.showError("Błąd", "Nie udało się wczytać danych dokumentu.");
            return;
        }

        documentNumberField.setText(safe(dto.getDocumentNumber()));
        selectDocumentType(dto.getDocumentType());
        selectUser(dto.getCreatedBy());
        issueDateField.setValue(dto.getIssueDate());
        commentsField.setText(dto.getComments());
        if (printPassportsBox != null) {
            printPassportsBox.setSelected(dto.isPrintPassports());
        }
        selectContrahent(dto.getContrahentId());
        updateStatusLabel(dto.getStatus());
        if (dto.getStatus() == DocumentStatus.CANCELLED) {
            applyReadOnlyMode();
        }

        rows.clear();
        internalRowChange = true;
        if (dto.getItems() != null) {
            for (DocumentItemDTO item : dto.getItems()) {
                PlantBatch batch = findBatchById(item.getPlantBatchId());
                Plant plant = batch != null ? plantById.get(batch.getPlantId()) : null;
                DocumentItemRow row = createRow();
                row.setPlant(plant);
                row.setBatch(batch);
                row.setQty(item.getQty());
                row.setPassportRequired(item.isPassportRequired());
                row.setCommitted(true);
                rows.add(row);
            }
        }
        internalRowChange = false;
        ensureTrailingBlankRow();
        refreshValidationIndicators();
    }

    @FXML
    private void save() {
        try {
            DocumentDTO dto = buildDocumentDto();
            if (!confirmSaveWithWarnings()) {
                return;
            }

            if (document == null) {
                documentService.createDocument(dto);
                DialogUtil.showSuccess("Dokument został zapisany.");
            } else {
                dto.setId(document.getId());
                documentService.updateDocument(dto);
                DialogUtil.showSuccess("Dokument został zaktualizowany.");
            }
            close();
        } catch (IllegalArgumentException | IllegalStateException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu", "Nie udało się zapisać dokumentu.");
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        javafx.scene.Node owner = cancelButton != null ? cancelButton : (saveButton != null ? saveButton : itemsTable);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) {
            owner.getScene().getWindow().hide();
        }
    }

    private void loadReferenceData() {
        allDocumentTypes.setAll(documentTypeService.getAll());
        allUsers.setAll(appUserService.getAll());
        allContrahents.setAll(contrahentRepository.findAll());
        allPlants.setAll(plantRepository.findAll());
        allBatches.setAll(plantBatchRepository.findAll().stream()
                .filter(batch -> batch.getStatus() == null || batch.getStatus() == PlantBatchStatus.ACTIVE)
                .collect(Collectors.toList()));

        contrahentById.clear();
        for (Contrahent contrahent : allContrahents) {
            contrahentById.put(contrahent.getId(), contrahent);
        }

        plantById.clear();
        for (Plant plant : allPlants) {
            plantById.put(plant.getId(), plant);
        }
    }

    private void configureDocumentTypeBox() {
        documentTypeBox.setItems(FXCollections.observableArrayList(allDocumentTypes));
        documentTypeBox.setMaxWidth(Double.MAX_VALUE);
        documentTypeBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DocumentType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatDocumentType(item));
            }
        });
        documentTypeBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DocumentType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatDocumentType(item));
            }
        });
        bindStableSelectionAutocomplete(documentTypeBox, allDocumentTypes, this::formatDocumentType, null);
    }

    private void configureUsersBox() {
        createdByBox.setItems(FXCollections.observableArrayList(allUsers));
        createdByBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AppUser item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDisplayName());
            }
        });
        createdByBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AppUser item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDisplayName());
            }
        });
        bindStableSelectionAutocomplete(createdByBox, allUsers, user -> user == null ? "" : safe(user.getDisplayName()), null);
    }

    private void configureContrahentBox() {
        contrahentBox.setItems(FXCollections.observableArrayList(allContrahents));
        contrahentBox.setMaxWidth(Double.MAX_VALUE);
        contrahentBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Contrahent item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : safe(item.getName()));
            }
        });
        contrahentBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Contrahent item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : safe(item.getName()));
            }
        });
        bindStableSelectionAutocomplete(contrahentBox, allContrahents, contrahent -> contrahent == null ? "" : safe(contrahent.getName()), this::refreshValidationIndicators);
        contrahentBox.valueProperty().addListener((obs, oldVal, newVal) -> refreshValidationIndicators());
    }

    private void configureTable() {
        itemsTable.setItems(rows);
        itemsTable.setEditable(true);

        colLp.setCellValueFactory(cell -> new SimpleIntegerProperty(rows.indexOf(cell.getValue()) + 1));
        colPlant.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colBatch.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        colQty.setCellValueFactory(cell -> cell.getValue().qtyProperty());
        colPassport.setCellValueFactory(cell -> cell.getValue().passportRequiredProperty());
        colActions.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));

        colPlant.setCellFactory(col -> new PlantEditingCell());
        colBatch.setCellFactory(col -> new BatchEditingCell());
        colQty.setCellFactory(col -> new QtyEditingCell());
        colPassport.setCellFactory(col -> new PassportEditingCell());
        colActions.setCellFactory(col -> new ActionsCell());

        itemsTable.setRowFactory(tv -> {
            TableRow<DocumentItemRow> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    bindRow(newVal);
                }
            });
            return row;
        });
    }

    private void configurePrintPassportsBox() {
        if (printPassportsBox == null) {
            return;
        }

        printPassportsBox.setSelected(false);
        printPassportsBox.selectedProperty().addListener((obs, oldVal, newVal) -> itemsTable.refresh());
    }

    private void configureActionButtons() {
        if (previewButton != null) {
            previewButton.setDisable(false);
        }
        if (pdfButton != null) {
            pdfButton.setDisable(false);
        }
    }

    private void bindRow(DocumentItemRow row) {
        if (row.isBound()) {
            return;
        }
        row.setBound(true);

        row.plantProperty().addListener((obs, oldVal, newVal) -> {
            if (internalRowChange) {
                return;
            }
            PlantBatch currentBatch = row.getBatch();
            if (currentBatch != null && (newVal == null || currentBatch.getPlantId() != newVal.getId())) {
                row.setBatch(null);
            }
            row.setPassportRequired(resolvePlantPassportRequired(newVal));
            autoAppendIfReady(row);
            itemsTable.refresh();
            refreshValidationIndicators();
        });

        row.batchProperty().addListener((obs, oldVal, newVal) -> {
            autoAppendIfReady(row);
            itemsTable.refresh();
            refreshValidationIndicators();
        });

        row.qtyProperty().addListener((obs, oldVal, newVal) -> {
            autoAppendIfReady(row);
            refreshValidationIndicators();
        });

        row.passportRequiredProperty().addListener((obs, oldVal, newVal) -> {
            autoAppendIfReady(row);
            refreshValidationIndicators();
        });
    }

    private void autoAppendIfReady(DocumentItemRow row) {
        if (internalRowChange) {
            return;
        }
        if (!row.isCommitted() && row.isComplete()) {
            row.setCommitted(true);
            ensureTrailingBlankRow();
            itemsTable.refresh();
        }
    }

    private void ensureTrailingBlankRow() {
        if (rows.isEmpty() || rows.stream().noneMatch(DocumentItemRow::isBlank)) {
            rows.add(createRow());
            return;
        }

        long blankCount = rows.stream().filter(DocumentItemRow::isBlank).count();
        if (blankCount > 1) {
            List<DocumentItemRow> blanks = rows.stream().filter(DocumentItemRow::isBlank).collect(Collectors.toList());
            for (int i = 1; i < blanks.size(); i++) {
                rows.remove(blanks.get(i));
            }
        }
    }

    private DocumentItemRow createRow() {
        DocumentItemRow row = new DocumentItemRow();
        bindRow(row);
        return row;
    }

    private List<PlantBatch> getAvailableBatchesForPlant(Plant plant, DocumentItemRow currentRow) {
        if (plant == null) {
            return List.of();
        }

        List<Integer> usedBatchIds = rows.stream()
                .filter(row -> row != currentRow)
                .map(DocumentItemRow::getBatch)
                .filter(batch -> batch != null)
                .map(PlantBatch::getId)
                .collect(Collectors.toList());

        return allBatches.stream()
                .filter(batch -> batch.getPlantId() == plant.getId())
                .filter(batch -> !usedBatchIds.contains(batch.getId()) || (currentRow.getBatch() != null && currentRow.getBatch().getId() == batch.getId()))
                .collect(Collectors.toList());
    }

    private void refreshBatches() {
        allBatches.setAll(plantBatchRepository.findAll().stream()
                .filter(batch -> batch.getStatus() == null || batch.getStatus() == PlantBatchStatus.ACTIVE)
                .collect(Collectors.toList()));
        itemsTable.refresh();
        refreshValidationIndicators();
    }

    private void openAddBatchForRow(DocumentItemRow row) {
        if (row == null || row.getPlant() == null) {
            DialogUtil.showWarning("Brak rośliny", "Najpierw wybierz roślinę dla pozycji dokumentu.");
            return;
        }

        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/plant_batch_form.fxml"));
        try {
            javafx.scene.Scene scene = new javafx.scene.Scene(
                    loader.load(),
                    WindowSizingUtil.resolveInitialWidth(1220),
                    WindowSizingUtil.resolveInitialHeight(980)
            );
            java.net.URL stylesheetUrl = getClass().getResource("/styles/app.css");
            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            }

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Dodaj partię roślin");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            WindowSizingUtil.applyStageSize(stage, 1220, 980, 1080, 860);

            PlantBatchFormController controller = loader.getController();
            controller.setPreselectedPlant(row.getPlant());
            controller.setOnSaveSuccess(() -> {
                refreshBatches();

                List<PlantBatch> batches = getAvailableBatchesForPlant(row.getPlant(), row);
                if (!batches.isEmpty()) {
                    PlantBatch newest = batches.stream()
                            .max(java.util.Comparator.comparingInt(PlantBatch::getId))
                            .orElse(batches.get(0));
                    row.setBatch(newest);
                }

                itemsTable.refresh();
                refreshValidationIndicators();
            });

            stage.showAndWait();
            itemsTable.refresh();
            refreshValidationIndicators();

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd", "Nie udało się otworzyć formularza partii roślin.");
        }
    }

    private void refreshValidationIndicators() {
        if (rows == null || itemsTable == null) {
            return;
        }

        for (DocumentItemRow row : rows) {
            if (row == null) {
                continue;
            }
            row.setValidationMessage(buildEppoValidationMessage(row));
        }

        itemsTable.refresh();
    }

    private String buildEppoValidationMessage(DocumentItemRow row) {
        if (row == null || row.isBlank()) {
            return "";
        }

        Contrahent contrahent = contrahentBox == null ? null : resolveSelectedContrahent();
        String clientCountryCode = contrahent == null ? "" : safe(contrahent.getCountryCode()).toUpperCase();
        if (clientCountryCode.isBlank()) {
            return "";
        }

        Plant plant = resolvePlantForRow(row);
        PlantBatch batch = row.getBatch();
        if (plant == null || batch == null) {
            return "";
        }

        List<EppoCode> requiredCodes = resolveRequiredCodesForPlantAndCountry(plant, clientCountryCode);
        if (requiredCodes.isEmpty()) {
            return "";
        }

        String batchCode = safe(batch.getEppoCode()).toUpperCase();
        for (EppoCode requiredCode : requiredCodes) {
            boolean protectedZoneMatches = batchMatchesRequiredProtectedZone(requiredCode, clientCountryCode, batch);
            if (protectedZoneMatches) {
                return "";
            }
        }

        String requiredCodesText = requiredCodes.stream()
                .map(EppoCode::getCode)
                .map(this::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        if (requiredCodesText.isBlank()) {
            requiredCodesText = "brak";
        }

        String batchZpValue = safe(batch.getZpZone());
        String batchCountry = safe(batch.getManufacturerCountryCode()).toUpperCase();
        return "Partia nie spełnia wymagań ZP dla wybranego gatunku i kraju klienta. Wymagany kod EPPO: "
                + requiredCodesText
                + ". Partia ma kod: " + (batchCode.isBlank() ? "brak" : batchCode)
                + ", ZP: " + (batchZpValue.isBlank() ? "brak" : batchZpValue)
                + ", kraj pochodzenia: " + (batchCountry.isBlank() ? "brak" : batchCountry)
                + ". Kraj klienta jest objęty tym kodem EPPO, ale kraj pochodzenia partii nie jest powiązany z tym samym kodem albo na partii nie zapisano oznaczenia 'Partia pochodzi z ZP'.";
    }

    private List<EppoCode> resolveRequiredCodesForPlantAndCountry(Plant plant, String clientCountryCode) {
        if (plant == null || safe(clientCountryCode).isBlank()) {
            return List.of();
        }

        Map<Integer, EppoCode> byId = new java.util.LinkedHashMap<>();
        List<EppoCode> linkedCodes = eppoCodePlantLinkService.getCodesForPlant(plant.getId());
        if (linkedCodes != null) {
            for (EppoCode code : linkedCodes) {
                if (code != null) {
                    byId.put(code.getId(), code);
                }
            }
        }

        String species = normalizeKey(plant.getSpecies());
        String latinSpecies = normalizeKey(plant.getLatinSpeciesName());
        if (!species.isBlank() || !latinSpecies.isBlank()) {
            for (EppoCode code : eppoCodeService.getAll()) {
                if (code == null) {
                    continue;
                }
                boolean speciesMatch = eppoCodeSpeciesLinkService.getEffectiveSpeciesLinks(code.getId()).stream()
                        .anyMatch(link -> matchesPlantSpeciesLink(species, latinSpecies, link.getSpeciesName(), link.getLatinSpeciesName()));
                if (speciesMatch) {
                    byId.put(code.getId(), code);
                }
            }
        }

        return byId.values().stream()
                .filter(code -> codeMatchesClientCountry(code, clientCountryCode))
                .toList();
    }

    private boolean matchesPlantSpeciesLink(String plantSpecies, String plantLatinSpecies, String linkSpecies, String linkLatinSpecies) {
        String normalizedLinkSpecies = normalizeKey(linkSpecies);
        String normalizedLinkLatinSpecies = normalizeKey(linkLatinSpecies);
        return (!plantSpecies.isBlank() && plantSpecies.equals(normalizedLinkSpecies))
                || (!plantLatinSpecies.isBlank() && plantLatinSpecies.equals(normalizedLinkLatinSpecies));
    }

    private boolean codeMatchesClientCountry(EppoCode code, String clientCountryCode) {
        if (code == null || safe(clientCountryCode).isBlank()) {
            return false;
        }

        List<EppoZone> zones = eppoCodeZoneLinkService.getZonesForCode(code.getId());
        if (zones == null || zones.isEmpty()) {
            return false;
        }

        return zones.stream()
                .filter(Objects::nonNull)
                .map(EppoZone::getCountryCode)
                .map(this::safe)
                .map(String::toUpperCase)
                .anyMatch(countryCode -> countryCode.equals(clientCountryCode));
    }

    private boolean batchMatchesRequiredProtectedZone(EppoCode requiredCode, String clientCountryCode, PlantBatch batch) {
        if (requiredCode == null || batch == null || safe(clientCountryCode).isBlank()) {
            return false;
        }

        List<EppoZone> linkedZones = eppoCodeZoneLinkService.getZonesForCode(requiredCode.getId()).stream()
                .filter(Objects::nonNull)
                .toList();
        if (linkedZones.isEmpty()) {
            return false;
        }

        boolean destinationCountryRequiresCode = linkedZones.stream()
                .map(EppoZone::getCountryCode)
                .map(this::safe)
                .map(String::toUpperCase)
                .anyMatch(clientCountryCode::equals);
        if (!destinationCountryRequiresCode) {
            return false;
        }

        String batchCountryCode = safe(batch.getManufacturerCountryCode()).toUpperCase();
        boolean sourceCountryMatchesRequiredCode = !batchCountryCode.isBlank() && linkedZones.stream()
                .map(EppoZone::getCountryCode)
                .map(this::safe)
                .map(String::toUpperCase)
                .anyMatch(batchCountryCode::equals);
        if (!sourceCountryMatchesRequiredCode) {
            return false;
        }

        String batchZonesRaw = safe(batch.getZpZone());
        if (batchZonesRaw.isBlank()) {
            return false;
        }

        List<String> batchZoneMarkers = java.util.Arrays.stream(batchZonesRaw.split(","))
                .map(this::safe)
                .map(String::toUpperCase)
                .filter(value -> !value.isBlank())
                .toList();
        if (batchZoneMarkers.isEmpty()) {
            return false;
        }

        java.util.Set<String> allowedBatchMarkers = linkedZones.stream()
                .flatMap(zone -> java.util.stream.Stream.of(zone.getCode(), zone.getCountryCode(), zone.getName()))
                .map(this::safe)
                .map(String::toUpperCase)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());

        boolean markedProtectedZoneMatchesRequiredCode = batchZoneMarkers.stream()
                .anyMatch(marker -> allowedBatchMarkers.contains(marker) || marker.equals("ZP") || marker.equals("TRUE") || marker.equals("TAK"));

        return markedProtectedZoneMatchesRequiredCode;
    }

    private Plant resolvePlantForRow(DocumentItemRow row) {
        if (row == null) {
            return null;
        }
        if (row.getPlant() != null) {
            return row.getPlant();
        }
        if (row.getBatch() != null) {
            return plantById.get(row.getBatch().getPlantId());
        }
        return null;
    }

    private String buildRowAdvisoryMessage(int rowNumber, DocumentItemRow row, Plant plant, String clientCountryCode, boolean missingCountryCode) {
        StringBuilder message = new StringBuilder();
        message.append("Pozycja ").append(rowNumber).append(" — ").append(formatPlantName(plant));

        PlantBatch batch = row.getBatch();
        if (batch == null) {
            message.append(UiTextUtil.NL)
                    .append("Status partii: brak wybranej partii dla pozycji.");
        } else if (batch.getStatus() == PlantBatchStatus.CANCELLED) {
            message.append(UiTextUtil.NL)
                    .append("Status partii: wybrana partia jest anulowana i nie może zostać użyta w aktywnym dokumencie.");
        } else {
            int availableQty = document == null
                    ? documentService.getAvailableQtyForBatch(batch.getId())
                    : documentService.getAvailableQtyForBatch(batch.getId(), document.getId());
            message.append(UiTextUtil.NL)
                    .append("Partia: ").append(formatBatchChoice(batch))
                    .append(UiTextUtil.NL)
                    .append("Dostępna ilość dla pozycji: ").append(availableQty);
        }

        PassportAdvisoryService.AdvisoryResult passportResult = passportAdvisoryService.analyzePlant(plant);
        message.append(UiTextUtil.DOUBLE_NL)
                .append(passportResult.message());

        if (missingCountryCode) {
            message.append(UiTextUtil.DOUBLE_NL)
                    .append("EPPO: klient nie ma uzupełnionego kodu kraju, więc dopasowanie kraju/strefy EPPO nie może zostać ocenione.");
            return message.toString();
        }

        EppoAdvisoryService.AdvisoryResult eppoResult = eppoAdvisoryService
                .analyzePlantForCountry(plant, clientCountryCode, rowNumber, "Pozycja");

        message.append(UiTextUtil.DOUBLE_NL)
                .append(eppoResult.message());

        return message.toString();
    }

    private String formatPlantName(Plant plant) {
        if (plant == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if (!safe(plant.getSpecies()).isBlank()) {
            parts.add(safe(plant.getSpecies()));
        }
        if (!safe(plant.getVariety()).isBlank()) {
            parts.add(safe(plant.getVariety()));
        }
        if (!safe(plant.getRootstock()).isBlank()) {
            parts.add("na " + safe(plant.getRootstock()));
        }

        if (!parts.isEmpty()) {
            return String.join(" ", parts);
        }

        return safe(plant.getLatinSpeciesName()).isBlank() ? ("Roślina ID " + plant.getId()) : safe(plant.getLatinSpeciesName());
    }

    private void updateStatusLabel(DocumentStatus status) {
        if (statusValueLabel == null) {
            return;
        }
        DocumentStatus effectiveStatus = status == null ? DocumentStatus.ACTIVE : status;
        statusValueLabel.setText(effectiveStatus == DocumentStatus.CANCELLED ? "Anulowany" : "Aktywny");
    }

    private void applyReadOnlyMode() {
        documentTypeBox.setDisable(true);
        contrahentBox.setDisable(true);
        issueDateField.setDisable(true);
        createdByBox.setDisable(true);
        commentsField.setDisable(true);
        if (printPassportsBox != null) {
            printPassportsBox.setDisable(true);
        }
        itemsTable.setDisable(true);
        saveButton.setDisable(true);
        saveButton.setText("Dokument anulowany");
    }


    private boolean confirmSaveWithWarnings() {
        Contrahent contrahent = contrahentBox.getValue();
        String clientCountryCode = contrahent != null ? safe(contrahent.getCountryCode()).toUpperCase() : "";

        int elevatedRiskCount = 0;
        int missingCodeCount = 0;
        int passportAttentionCount = 0;
        List<String> details = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            DocumentItemRow row = rows.get(i);
            if (row == null || row.isBlank()) {
                continue;
            }

            Plant plant = resolvePlantForRow(row);
            if (plant == null) {
                continue;
            }

            PassportAdvisoryService.AdvisoryResult passportResult = passportAdvisoryService.analyzePlant(plant);
            if (passportResult.attention()) {
                passportAttentionCount++;
            }

            if (!clientCountryCode.isBlank()) {
                String validationMessage = buildEppoValidationMessage(row);
                if (!validationMessage.isBlank()) {
                    elevatedRiskCount++;
                    details.add("Pozycja " + (i + 1) + ": " + validationMessage);
                }

                if (resolveRequiredCodesForPlantAndCountry(plant, clientCountryCode).isEmpty()) {
                    EppoAdvisoryService.AdvisoryResult eppoResult = eppoAdvisoryService
                            .analyzePlantForCountry(plant, clientCountryCode, i + 1, "Pozycja");
                    if (eppoResult.missingCode()) {
                        missingCodeCount++;
                        details.add("Pozycja " + (i + 1) + ": brak powiązanego kodu EPPO w słowniku.");
                    }
                }
            }

            if (passportResult.attention()) {
                details.add("Pozycja " + (i + 1) + ": uwaga paszportowa dla rośliny.");
            }
        }

        if (elevatedRiskCount == 0 && missingCodeCount == 0 && passportAttentionCount == 0) {
            return true;
        }

        StringBuilder message = new StringBuilder();
        message.append("Dokument zawiera pozycje wymagające uwagi.").append(UiTextUtil.DOUBLE_NL);
        UiTextUtil.appendSectionHeader(message, "PODSUMOWANIE");
        UiTextUtil.appendSummaryLine(message, "Podwyższone ryzyko EPPO", elevatedRiskCount);
        UiTextUtil.appendSummaryLine(message, "Brak kodu EPPO", missingCodeCount);
        UiTextUtil.appendSummaryLine(message, "Uwaga paszportowa", passportAttentionCount);

        if (!details.isEmpty()) {
            UiTextUtil.appendEmptyLine(message);
            UiTextUtil.appendSectionHeader(message, "SZCZEGÓŁY");
            int limit = Math.min(details.size(), 8);
            for (int i = 0; i < limit; i++) {
                UiTextUtil.appendBulletLine(message, details.get(i));
            }
            if (details.size() > limit) {
                UiTextUtil.appendBulletLine(message, "... oraz " + (details.size() - limit) + " kolejnych uwag");
            }
        }

        message.append(UiTextUtil.NL).append("Czy mimo to chcesz zapisać dokument?");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ostrzeżenie EPPO / paszportowe");
        alert.setHeaderText("Wykryto ostrzeżenia informacyjne");
        alert.setContentText(message.toString());

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private DocumentDTO buildDocumentDto() {
        commitHeaderSelections();
        commitPendingRowEditors();

        DocumentType selectedType = resolveSelectedDocumentType();
        if (selectedType == null) {
            throw new IllegalArgumentException("Wybierz typ dokumentu.");
        }
        AppUser selectedUser = resolveSelectedUser();
        if (selectedUser == null) {
            throw new IllegalArgumentException("Wybierz użytkownika w polu „Utworzył”.");
        }
        Contrahent selectedContrahent = resolveSelectedContrahent();
        if (selectedContrahent == null) {
            throw new IllegalArgumentException("Wybierz klienta.");
        }
        if (issueDateField.getValue() == null) {
            throw new IllegalArgumentException("Wybierz datę wystawienia.");
        }

        List<DocumentItemDTO> items = new ArrayList<>();
        for (DocumentItemRow row : rows) {
            if (row.isBlank()) {
                continue;
            }
            if (row.getPlant() == null) {
                throw new IllegalArgumentException("Każda pozycja dokumentu musi mieć wybraną roślinę.");
            }
            if (row.getBatch() == null) {
                throw new IllegalArgumentException("Każda pozycja dokumentu musi mieć wybraną partię.");
            }
            if (row.getBatch().getStatus() == PlantBatchStatus.CANCELLED) {
                throw new IllegalArgumentException("Nie można użyć anulowanej partii w aktywnym dokumencie.");
            }
            if (row.getBatch().getPlantId() != row.getPlant().getId()) {
                throw new IllegalArgumentException("Wybrana partia nie należy do wskazanej rośliny.");
            }
            if (row.getQty() <= 0) {
                throw new IllegalArgumentException("Ilość w pozycji dokumentu musi być większa od zera.");
            }

            DocumentItemDTO item = new DocumentItemDTO();
            item.setPlantBatchId(row.getBatch().getId());
            item.setQty(row.getQty());
            item.setPassportRequired(row.isPassportRequired());
            items.add(item);
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Dokument musi zawierać co najmniej jedną pozycję.");
        }

        DocumentDTO dto = new DocumentDTO();
        dto.setDocumentNumber(normalizeDocumentNumberForWorkflow(documentNumberField.getText()));
        dto.setDocumentType(selectedType.getName());
        dto.setIssueDate(issueDateField.getValue());
        dto.setContrahentId(selectedContrahent.getId());
        dto.setCreatedBy(selectedUser.getDisplayName());
        dto.setComments(commentsField.getText());
        dto.setPrintPassports(printPassportsBox != null && printPassportsBox.isSelected());
        dto.setItems(items);
        return dto;
    }

    private boolean isPrintPassportsEnabled() {
        return printPassportsBox != null && printPassportsBox.isSelected();
    }

    private boolean resolvePlantPassportRequired(Plant plant) {
        if (plant == null) {
            return false;
        }
        return appSettingsService.isPlantPassportRequiredForAll() || plant.isPassportRequired();
    }

    @FXML
    private void previewDocument() {
        try {
            DocumentPreviewDTO preview = buildPreviewFromForm();
            ModalViewUtil.openModal(
                    "/view/document_preview.fxml",
                    "Podgląd dokumentu",
                    1260, 980,
                    1120, 840,
                    (DocumentPreviewController controller) -> controller.setPreview(preview)
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd podglądu", "Nie udało się przygotować podglądu dokumentu.");
        }
    }

    @FXML
    private void exportPdf() {
        try {
            DocumentPreviewDTO preview = buildPreviewFromForm();
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Zapisz dokument jako PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki PDF", "*.pdf"));
            chooser.setInitialFileName(buildPdfFileName(preview));

            Stage stage = getStage();
            File outputFile = chooser.showSaveDialog(stage);
            if (outputFile == null) {
                return;
            }

            documentPdfService.export(preview, outputFile);
            DialogUtil.showSuccess("Dokument PDF został zapisany.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd eksportu", "Nie udało się zapisać dokumentu jako PDF.");
        }
    }

    private DocumentPreviewDTO buildPreviewFromForm() {
        DocumentDTO dto = buildDocumentDto();
        dto.setDocumentNumber(resolvePreviewDocumentNumber());
        if (dto.getStatus() == null) {
            dto.setStatus(document != null ? document.getStatus() : DocumentStatus.ACTIVE);
        }
        return documentRenderService.buildPreview(dto);
    }

    private String resolvePreviewDocumentNumber() {
        if (document != null && document.getDocumentNumber() != null && !document.getDocumentNumber().isBlank()) {
            return safe(document.getDocumentNumber());
        }
        String current = safe(documentNumberField.getText());
        if (current.equals("Zostanie nadany automatycznie przy zapisie")) {
            return "";
        }
        return current;
    }

    private String normalizeDocumentNumberForWorkflow(String value) {
        String normalized = safe(value);
        return normalized.equals("Zostanie nadany automatycznie przy zapisie") ? "" : normalized;
    }

    private String buildPdfFileName(DocumentPreviewDTO preview) {
        String baseName = safe(preview.getDocumentNumber());
        if (baseName.isBlank()) {
            baseName = safe(preview.getDocumentType());
        }
        if (baseName.isBlank()) {
            baseName = "dokument";
        }
        return baseName.replaceAll("[\\/:*?\"<>|]", "_") + ".pdf";
    }

    private void commitHeaderSelections() {
        commitHeaderSelection(documentTypeBox, allDocumentTypes, this::formatDocumentType);
        commitHeaderSelection(createdByBox, allUsers, user -> user == null ? "" : safe(user.getDisplayName()));
        commitHeaderSelection(contrahentBox, allContrahents, contrahent -> contrahent == null ? "" : safe(contrahent.getName()));
    }

    private void commitPendingRowEditors() {
        if (itemsTable == null) {
            return;
        }
        itemsTable.edit(-1, null);
        itemsTable.requestFocus();
        itemsTable.refresh();
    }

    private <T> void commitHeaderSelection(ComboBox<T> comboBox, List<T> sourceValues, Function<T, String> textProvider) {
        if (comboBox == null || comboBox.getEditor() == null || textProvider == null) {
            return;
        }

        T selected = comboBox.getValue();
        if (selected == null) {
            selected = findBestSelectionMatch(sourceValues, comboBox.getEditor().getText(), textProvider);
        }

        restoreComboItems(comboBox, sourceValues);
        if (selected != null) {
            comboBox.getSelectionModel().select(selected);
            comboBox.setValue(selected);
            comboBox.getEditor().setText(safe(textProvider.apply(selected)));
            comboBox.getEditor().positionCaret(comboBox.getEditor().getText().length());
        }
    }

    private DocumentType resolveSelectedDocumentType() {
        if (documentTypeBox == null) {
            return null;
        }
        DocumentType selected = documentTypeBox.getValue();
        if (selected != null) {
            return selected;
        }
        return findBestSelectionMatch(allDocumentTypes,
                documentTypeBox.getEditor() == null ? "" : documentTypeBox.getEditor().getText(),
                this::formatDocumentType);
    }

    private AppUser resolveSelectedUser() {
        if (createdByBox == null) {
            return null;
        }
        AppUser selected = createdByBox.getValue();
        if (selected != null) {
            return selected;
        }
        return findBestSelectionMatch(allUsers,
                createdByBox.getEditor() == null ? "" : createdByBox.getEditor().getText(),
                user -> user == null ? "" : safe(user.getDisplayName()));
    }

    private Contrahent resolveSelectedContrahent() {
        if (contrahentBox == null) {
            return null;
        }
        Contrahent selected = contrahentBox.getValue();
        if (selected != null) {
            return selected;
        }
        return findBestSelectionMatch(allContrahents,
                contrahentBox.getEditor() == null ? "" : contrahentBox.getEditor().getText(),
                contrahent -> contrahent == null ? "" : safe(contrahent.getName()));
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private <T> void bindStableSelectionAutocomplete(
            ComboBox<T> comboBox,
            List<T> sourceValues,
            Function<T, String> textProvider,
            Runnable afterCommit
    ) {
        if (comboBox == null || textProvider == null) {
            return;
        }

        ObservableList<T> masterItems = FXCollections.observableArrayList(sourceValues == null ? List.of() : sourceValues);
        final boolean[] internalChange = {false};

        comboBox.setEditable(true);
        comboBox.setItems(FXCollections.observableArrayList(masterItems));
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : safe(textProvider.apply(object));
            }

            @Override
            public T fromString(String string) {
                return findBestSelectionMatch(masterItems, string, textProvider);
            }
        });

        if (comboBox.getEditor() == null) {
            return;
        }

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (internalChange[0]) {
                return;
            }
            if (!comboBox.isFocused() && !comboBox.getEditor().isFocused()) {
                return;
            }

            String typedText = safe(newValue);
            int caretPosition = comboBox.getEditor().getCaretPosition();
            String normalizedTypedText = normalizeKey(typedText);
            List<T> filteredItems = masterItems.stream()
                    .filter(item -> normalizedTypedText.isBlank() || normalizeKey(textProvider.apply(item)).contains(normalizedTypedText))
                    .toList();

            internalChange[0] = true;
            try {
                comboBox.setItems(FXCollections.observableArrayList(filteredItems));
                if (!filteredItems.isEmpty() && !comboBox.isShowing()) {
                    comboBox.show();
                }
                if (!Objects.equals(comboBox.getEditor().getText(), typedText)) {
                    comboBox.getEditor().setText(typedText);
                }
                comboBox.getEditor().positionCaret(Math.min(caretPosition, comboBox.getEditor().getLength()));
            } finally {
                internalChange[0] = false;
            }
        });

        comboBox.setOnMouseClicked(event -> restoreComboItems(comboBox, masterItems));

        comboBox.setOnAction(event -> {
            if (internalChange[0]) {
                return;
            }
            commitStableSelection(comboBox, masterItems, textProvider, internalChange, afterCommit);
        });

        comboBox.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                restoreComboItems(comboBox, masterItems);
                return;
            }
            commitStableSelection(comboBox, masterItems, textProvider, internalChange, afterCommit);
        });

        comboBox.getEditor().focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused && !comboBox.isFocused()) {
                commitStableSelection(comboBox, masterItems, textProvider, internalChange, afterCommit);
            }
        });

        comboBox.getEditor().setOnAction(event -> commitStableSelection(comboBox, masterItems, textProvider, internalChange, afterCommit));
    }

    private <T> void commitStableSelection(
            ComboBox<T> comboBox,
            ObservableList<T> masterItems,
            Function<T, String> textProvider,
            boolean[] internalChange,
            Runnable afterCommit
    ) {
        if (comboBox == null || comboBox.getEditor() == null) {
            return;
        }

        String editorText = safe(comboBox.getEditor().getText());
        T selected = comboBox.getSelectionModel().getSelectedItem();
        if (selected == null || !normalizeKey(textProvider.apply(selected)).equals(normalizeKey(editorText))) {
            selected = findBestSelectionMatch(masterItems, editorText, textProvider);
        }

        internalChange[0] = true;
        try {
            restoreComboItems(comboBox, masterItems);
            if (selected != null) {
                comboBox.getSelectionModel().select(selected);
                comboBox.setValue(selected);
                String committedText = safe(textProvider.apply(selected));
                comboBox.getEditor().setText(committedText);
                comboBox.getEditor().positionCaret(committedText.length());
            } else if (editorText.isBlank()) {
                comboBox.getSelectionModel().clearSelection();
                comboBox.setValue(null);
                comboBox.getEditor().clear();
            } else {
                comboBox.getSelectionModel().clearSelection();
                comboBox.setValue(null);
                comboBox.getEditor().setText(editorText);
                comboBox.getEditor().positionCaret(editorText.length());
            }
        } finally {
            internalChange[0] = false;
        }

        if (afterCommit != null) {
            afterCommit.run();
        }
    }

    private <T> void restoreComboItems(ComboBox<T> comboBox, List<T> sourceValues) {
        if (comboBox == null) {
            return;
        }
        comboBox.setItems(FXCollections.observableArrayList(sourceValues == null ? List.of() : sourceValues));
    }

    private <T> T findBestSelectionMatch(List<T> sourceValues, String editorText, Function<T, String> textProvider) {
        if (sourceValues == null || textProvider == null) {
            return null;
        }

        String normalized = normalizeKey(editorText);
        if (normalized.isBlank()) {
            return null;
        }

        T exactMatch = sourceValues.stream()
                .filter(Objects::nonNull)
                .filter(item -> normalizeKey(textProvider.apply(item)).equals(normalized))
                .findFirst()
                .orElse(null);
        if (exactMatch != null) {
            return exactMatch;
        }

        List<T> prefixMatches = sourceValues.stream()
                .filter(Objects::nonNull)
                .filter(item -> normalizeKey(textProvider.apply(item)).startsWith(normalized))
                .toList();
        if (prefixMatches.size() == 1) {
            return prefixMatches.get(0);
        }

        List<T> containsMatches = sourceValues.stream()
                .filter(Objects::nonNull)
                .filter(item -> normalizeKey(textProvider.apply(item)).contains(normalized))
                .toList();
        if (containsMatches.size() == 1) {
            return containsMatches.get(0);
        }

        return null;
    }

    private Stage getStage() {

        if (saveButton != null && saveButton.getScene() != null) {
            return (Stage) saveButton.getScene().getWindow();
        }
        if (cancelButton != null && cancelButton.getScene() != null) {
            return (Stage) cancelButton.getScene().getWindow();
        }
        return null;
    }

    private void selectDefaultUser() {
        Optional<Integer> defaultUserId = appUserService.getDefaultUserId();
        if (defaultUserId.isEmpty()) {
            return;
        }
        for (AppUser user : allUsers) {
            if (user.getId() == defaultUserId.get()) {
                createdByBox.setValue(user);
                return;
            }
        }
    }

    private void selectDocumentType(String documentTypeName) {
        for (DocumentType type : allDocumentTypes) {
            if (safe(type.getName()).equalsIgnoreCase(safe(documentTypeName))) {
                documentTypeBox.setValue(type);
                return;
            }
        }
    }

    private void selectUser(String createdBy) {
        for (AppUser user : allUsers) {
            if (safe(user.getDisplayName()).equalsIgnoreCase(safe(createdBy))) {
                createdByBox.setValue(user);
                return;
            }
        }
    }

    private void selectContrahent(int contrahentId) {
        contrahentBox.setValue(contrahentById.get(contrahentId));
    }

    private PlantBatch findBatchById(int batchId) {
        for (PlantBatch batch : allBatches) {
            if (batch.getId() == batchId) {
                return batch;
            }
        }
        return null;
    }

    private boolean isInactivePlant(Plant plant) {
        return plant != null
                && plant.getVisibilityStatus() != null
                && plant.getVisibilityStatus().trim().equalsIgnoreCase("Nieużywany");
    }

    private void applyPlantCellStyle(ListCell<Plant> cell, Plant item, boolean empty) {
        if (cell == null) {
            return;
        }

        cell.getStyleClass().removeAll("inactive-row");
        if (!empty && isInactivePlant(item)) {
            cell.getStyleClass().add("inactive-row");
        }
    }

    private String formatDocumentType(DocumentType type) {
        if (type == null) {
            return "";
        }
        if (safe(type.getCode()).isBlank()) {
            return safe(type.getName());
        }
        return safe(type.getName()) + " / " + safe(type.getCode());
    }

    private String formatBatchChoice(PlantBatch batch) {
        if (batch == null) {
            return "";
        }
        if (isAddBatchOption(batch)) {
            return "➕ Dodaj nową partię…";
        }
        String contrahentName = "";
        Contrahent contrahent = contrahentById.get(batch.getContrahentId());
        if (contrahent != null) {
            contrahentName = safe(contrahent.getName());
        }
        String date = batch.getCreationDate() != null ? batch.getCreationDate().toString() : "—";
        return (contrahentName.isBlank() ? "Brak dostawcy" : contrahentName)
                + " / " + date
                + " / Ilość: " + batch.getQty()
                + " / Partia: " + formatBatchNumber(batch);
    }


    private PlantBatch createAddBatchOption(Plant plant) {
        PlantBatch option = new PlantBatch();
        option.setId(-1);
        option.setPlantId(plant != null ? plant.getId() : 0);
        option.setInteriorBatchNo("➕ Dodaj nową partię…");
        return option;
    }

    private boolean isAddBatchOption(PlantBatch batch) {
        return batch != null && batch.getId() == -1;
    }

    private String formatBatchNumber(PlantBatch batch) {
        if (batch == null) {
            return "";
        }
        if (!safe(batch.getInteriorBatchNo()).isBlank()) {
            return safe(batch.getInteriorBatchNo());
        }
        if (!safe(batch.getExteriorBatchNo()).isBlank()) {
            return safe(batch.getExteriorBatchNo());
        }
        return "Partia ID: " + batch.getId();
    }


    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean confirmPlantSelectionForCurrentDocumentType(Plant currentPlant, Plant selectedPlant) {
        if (selectedPlant == null || selectedPlant == currentPlant) {
            return true;
        }

        String preferredDocumentType = safe(selectedPlant.getDefaultDocumentType());
        if (preferredDocumentType.isBlank()) {
            return true;
        }

        DocumentType selectedDocumentType = documentTypeBox == null ? null : documentTypeBox.getValue();
        String currentDocumentTypeName = selectedDocumentType == null ? "" : safe(selectedDocumentType.getName());
        if (currentDocumentTypeName.isBlank()) {
            return true;
        }

        String preferredDocumentTypeName = mapPlantDefaultDocumentTypeToDocumentName(preferredDocumentType);
        if (preferredDocumentTypeName.isBlank() || preferredDocumentTypeName.equalsIgnoreCase(currentDocumentTypeName)) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Domyślny dokument rośliny");
        alert.setHeaderText("Wybrana roślina ma inny dokument domyślny.");
        alert.setContentText(
                "Dla rośliny „" + selectedPlant + "” ustawiono domyślnie: „" + preferredDocumentTypeName + "”.\n\n"
                        + "Aktualnie wybrany typ dokumentu to: „" + currentDocumentTypeName + "”.\n\n"
                        + "Czy chcesz kontynuować ten wybór?"
        );

        ButtonType continueButton = new ButtonType("Kontynuuj");
        ButtonType cancelButton = new ButtonType("Anuluj wybór", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(continueButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == continueButton;
    }

    private String mapPlantDefaultDocumentTypeToDocumentName(String defaultDocumentType) {
        if (Plant.DEFAULT_DOCUMENT_SUPPLIER.equals(defaultDocumentType)) {
            return "Dokument dostawcy";
        }
        if (Plant.DEFAULT_DOCUMENT_NURSERY_SUPPLIER.equals(defaultDocumentType)) {
            return "Szkółkarski dokument dostawcy";
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private class PlantEditingCell extends TableCell<DocumentItemRow, DocumentItemRow> {
        private final ComboBox<Plant> plantBox = new ComboBox<>();
        private final ObservableList<Plant> plantOptions = FXCollections.observableArrayList();
        private boolean syncingEditor;
        private boolean handlingAction;
        private boolean syncingSelection;

        PlantEditingCell() {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            plantBox.setEditable(true);
            plantBox.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(plantBox, Priority.ALWAYS);
            plantOptions.setAll(allPlants);
            plantBox.setItems(FXCollections.observableArrayList(plantOptions));
            plantBox.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(Plant item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.toString());
                    applyPlantCellStyle(this, item, empty);
                }
            });
            plantBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Plant item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.toString());
                    applyPlantCellStyle(this, item, empty);
                }
            });
            plantBox.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(Plant object) {
                    return object == null ? "" : object.toString();
                }

                @Override
                public Plant fromString(String string) {
                    return findMatchingPlant(string);
                }
            });
            if (plantBox.getEditor() != null) {
                plantBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                    if (syncingEditor || (!plantBox.isFocused() && (plantBox.getEditor() == null || !plantBox.getEditor().isFocused()))) {
                        return;
                    }
                    filterPlantOptions(newVal);
                });
                plantBox.getEditor().setOnAction(event -> commitPlantEditorSelection());
                plantBox.getEditor().focusedProperty().addListener((obs, oldVal, focused) -> {
                    if (!focused && !plantBox.isFocused()) {
                        commitPlantEditorSelection();
                    }
                });
                plantBox.focusedProperty().addListener((obs, oldVal, focused) -> {
                    if (!focused) {
                        commitPlantEditorSelection();
                    }
                });
            }
            plantBox.setOnAction(event -> commitPlantEditorSelection());
            plantBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (syncingSelection) {
                    return;
                }

                DocumentItemRow row = getCurrentRow();
                if (row == null) {
                    return;
                }

                Plant currentPlant = row.getPlant();
                if (currentPlant != newVal && newVal != null
                        && !confirmPlantSelectionForCurrentDocumentType(currentPlant, newVal)) {
                    handlingAction = true;
                    syncingSelection = true;
                    try {
                        plantBox.setValue(currentPlant);
                        if (!syncingEditor && plantBox.getEditor() != null) {
                            syncingEditor = true;
                            plantBox.getEditor().setText(currentPlant == null ? "" : currentPlant.toString());
                            syncingEditor = false;
                        }
                    } finally {
                        syncingSelection = false;
                        handlingAction = false;
                    }
                    return;
                }

                if (currentPlant != newVal) {
                    row.setPlant(newVal);
                }
                if (!syncingEditor && plantBox.getEditor() != null) {
                    syncingEditor = true;
                    plantBox.getEditor().setText(newVal == null ? "" : newVal.toString());
                    syncingEditor = false;
                }
                itemsTable.getSelectionModel().select(row);
            });
        }

        private void filterPlantOptions(String value) {
            if (handlingAction) {
                return;
            }
            String normalized = safe(value).toLowerCase();
            syncingSelection = true;
            try {
                if (normalized.isBlank()) {
                    plantBox.getItems().setAll(plantOptions);
                } else {
                    plantBox.getItems().setAll(plantOptions.filtered(plant -> plant != null && plant.toString().toLowerCase().contains(normalized)));
                }
            } finally {
                syncingSelection = false;
            }
            if (!plantBox.isShowing() && !plantBox.isDisabled()) {
                plantBox.show();
            }
        }

        private Plant findMatchingPlant(String text) {
            String normalized = safe(text).toLowerCase();
            if (normalized.isBlank()) {
                return null;
            }

            java.util.List<Plant> exactMatches = plantOptions.stream()
                    .filter(Objects::nonNull)
                    .filter(plant -> plant.toString().toLowerCase().equals(normalized))
                    .toList();
            if (!exactMatches.isEmpty()) {
                return exactMatches.get(0);
            }

            java.util.List<Plant> prefixMatches = plantOptions.stream()
                    .filter(Objects::nonNull)
                    .filter(plant -> plant.toString().toLowerCase().startsWith(normalized))
                    .toList();
            if (prefixMatches.size() == 1) {
                return prefixMatches.get(0);
            }

            java.util.List<Plant> containsMatches = plantOptions.stream()
                    .filter(Objects::nonNull)
                    .filter(plant -> plant.toString().toLowerCase().contains(normalized))
                    .toList();
            if (containsMatches.size() == 1) {
                return containsMatches.get(0);
            }

            return null;
        }

        private void commitPlantEditorSelection() {
            if (handlingAction) {
                return;
            }

            DocumentItemRow row = getCurrentRow();
            Plant currentPlant = row == null ? null : row.getPlant();
            Plant match = findMatchingPlant(plantBox.getEditor() != null ? plantBox.getEditor().getText() : "");

            if (match != null && !confirmPlantSelectionForCurrentDocumentType(currentPlant, match)) {
                handlingAction = true;
                syncingSelection = true;
                try {
                    plantBox.getItems().setAll(plantOptions);
                    plantBox.setValue(currentPlant);
                    if (plantBox.getEditor() != null) {
                        syncingEditor = true;
                        plantBox.getEditor().setText(currentPlant == null ? "" : currentPlant.toString());
                        syncingEditor = false;
                    }
                } finally {
                    syncingSelection = false;
                    handlingAction = false;
                }
                return;
            }

            handlingAction = true;
            syncingSelection = true;
            try {
                plantBox.getItems().setAll(plantOptions);
                if (match != null) {
                    plantBox.setValue(match);
                    if (row != null && row.getPlant() != match) {
                        row.setPlant(match);
                        itemsTable.getSelectionModel().select(row);
                    }
                } else if (plantBox.getEditor() != null) {
                    syncingEditor = true;
                    plantBox.getEditor().setText(currentPlant == null ? "" : currentPlant.toString());
                    syncingEditor = false;
                }
            } finally {
                syncingSelection = false;
                handlingAction = false;
            }
        }

        @Override
        protected void updateItem(DocumentItemRow item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            syncingSelection = true;
            handlingAction = true;
            try {
                plantOptions.setAll(allPlants);
                plantBox.getItems().setAll(plantOptions);
                plantBox.setValue(item.getPlant());
                if (plantBox.getEditor() != null) {
                    syncingEditor = true;
                    plantBox.getEditor().setText(item.getPlant() == null ? "" : item.getPlant().toString());
                    syncingEditor = false;
                }
            } finally {
                handlingAction = false;
                syncingSelection = false;
            }

            setGraphic(plantBox);
        }

        private DocumentItemRow getCurrentRow() {
            return getTableRow() != null ? getTableRow().getItem() : null;
        }
    }

    private class BatchEditingCell extends TableCell<DocumentItemRow, DocumentItemRow> {
        private final ComboBox<PlantBatch> batchBox = new ComboBox<>();
        private final ObservableList<PlantBatch> batchOptions = FXCollections.observableArrayList();
        private boolean handlingSpecialOption = false;
        private boolean syncingEditor;
        private boolean handlingAction;
        private boolean syncingSelection;

        BatchEditingCell() {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            batchBox.setEditable(true);
            batchBox.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(batchBox, Priority.ALWAYS);

            batchBox.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(PlantBatch item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : formatBatchChoice(item));
                }
            });

            batchBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(PlantBatch item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || isAddBatchOption(item)) {
                        setText("");
                    } else {
                        setText(formatBatchNumber(item));
                    }
                }
            });
            batchBox.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(PlantBatch object) {
                    if (object == null || isAddBatchOption(object)) {
                        return "";
                    }
                    return formatBatchNumber(object);
                }

                @Override
                public PlantBatch fromString(String string) {
                    return findMatchingBatch(string);
                }
            });
            if (batchBox.getEditor() != null) {
                batchBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                    if (syncingEditor || (!batchBox.isFocused() && (batchBox.getEditor() == null || !batchBox.getEditor().isFocused()))) {
                        return;
                    }
                    filterBatchOptions(newVal);
                });
                batchBox.getEditor().setOnAction(event -> commitBatchEditorSelection());
                batchBox.getEditor().focusedProperty().addListener((obs, oldVal, focused) -> {
                    if (!focused && !batchBox.isFocused()) {
                        commitBatchEditorSelection();
                    }
                });
                batchBox.focusedProperty().addListener((obs, oldVal, focused) -> {
                    if (!focused) {
                        commitBatchEditorSelection();
                    }
                });
            }

            batchBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (handlingSpecialOption || syncingSelection) {
                    return;
                }

                DocumentItemRow row = getCurrentRow();
                if (row == null) {
                    return;
                }

                if (isAddBatchOption(newVal)) {
                    handlingSpecialOption = true;

                    Platform.runLater(() -> {
                        try {
                            batchBox.getSelectionModel().clearSelection();
                            batchBox.setValue(row.getBatch());
                            openAddBatchForRow(row);
                        } finally {
                            handlingSpecialOption = false;
                        }
                    });

                    return;
                }

                if (row.getBatch() != newVal) {
                    row.setBatch(newVal);
                }

                if (!syncingEditor && batchBox.getEditor() != null) {
                    syncingEditor = true;
                    batchBox.getEditor().setText(newVal == null || isAddBatchOption(newVal) ? "" : formatBatchNumber(newVal));
                    syncingEditor = false;
                }

                Platform.runLater(() -> itemsTable.getSelectionModel().select(row));
            });
        }

        private void filterBatchOptions(String value) {
            if (handlingAction) {
                return;
            }
            String normalized = safe(value).toLowerCase();
            syncingSelection = true;
            try {
                if (normalized.isBlank()) {
                    batchBox.getItems().setAll(batchOptions);
                } else {
                    batchBox.getItems().setAll(batchOptions.filtered(batch -> batch != null
                            && (formatBatchChoice(batch).toLowerCase().contains(normalized)
                            || formatBatchNumber(batch).toLowerCase().contains(normalized))));
                }
            } finally {
                syncingSelection = false;
            }
            if (!batchBox.isShowing() && !batchBox.isDisabled()) {
                batchBox.show();
            }
        }

        private PlantBatch findMatchingBatch(String text) {
            String normalized = safe(text).toLowerCase();
            if (normalized.isBlank()) {
                return null;
            }

            java.util.List<PlantBatch> exactMatches = batchOptions.stream()
                    .filter(Objects::nonNull)
                    .filter(batch -> !isAddBatchOption(batch))
                    .filter(batch -> formatBatchNumber(batch).toLowerCase().equals(normalized)
                            || formatBatchChoice(batch).toLowerCase().equals(normalized))
                    .toList();
            if (!exactMatches.isEmpty()) {
                return exactMatches.get(0);
            }

            java.util.List<PlantBatch> prefixMatches = batchOptions.stream()
                    .filter(Objects::nonNull)
                    .filter(batch -> !isAddBatchOption(batch))
                    .filter(batch -> formatBatchNumber(batch).toLowerCase().startsWith(normalized)
                            || formatBatchChoice(batch).toLowerCase().startsWith(normalized))
                    .toList();
            if (prefixMatches.size() == 1) {
                return prefixMatches.get(0);
            }

            java.util.List<PlantBatch> containsMatches = batchOptions.stream()
                    .filter(Objects::nonNull)
                    .filter(batch -> !isAddBatchOption(batch))
                    .filter(batch -> formatBatchNumber(batch).toLowerCase().contains(normalized)
                            || formatBatchChoice(batch).toLowerCase().contains(normalized))
                    .toList();
            if (containsMatches.size() == 1) {
                return containsMatches.get(0);
            }

            return null;
        }

        private void commitBatchEditorSelection() {
            if (handlingAction) {
                return;
            }

            PlantBatch match = findMatchingBatch(batchBox.getEditor() != null ? batchBox.getEditor().getText() : "");
            handlingAction = true;
            syncingSelection = true;
            try {
                batchBox.getItems().setAll(batchOptions);
                if (match != null) {
                    batchBox.setValue(match);
                } else if (batchBox.getEditor() != null) {
                    syncingEditor = true;
                    batchBox.getEditor().setText(batchBox.getValue() == null || isAddBatchOption(batchBox.getValue()) ? "" : formatBatchNumber(batchBox.getValue()));
                    syncingEditor = false;
                }
            } finally {
                syncingSelection = false;
                handlingAction = false;
            }
        }

        @Override
        protected void updateItem(DocumentItemRow item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            List<PlantBatch> options = new ArrayList<>(getAvailableBatchesForPlant(item.getPlant(), item));
            if (item.getPlant() != null) {
                options.add(createAddBatchOption(item.getPlant()));
            }

            handlingSpecialOption = true;
            handlingAction = true;
            syncingSelection = true;
            try {
                batchOptions.setAll(options);
                batchBox.getItems().setAll(batchOptions);
                batchBox.setDisable(item.getPlant() == null);
                batchBox.setValue(item.getBatch());
                if (batchBox.getEditor() != null) {
                    syncingEditor = true;
                    batchBox.getEditor().setText(item.getBatch() == null ? "" : formatBatchNumber(item.getBatch()));
                    syncingEditor = false;
                }
            } finally {
                syncingSelection = false;
                handlingAction = false;
                handlingSpecialOption = false;
            }

            setGraphic(batchBox);
        }

        private DocumentItemRow getCurrentRow() {
            return getTableRow() != null ? getTableRow().getItem() : null;
        }
    }

    private class QtyEditingCell extends TableCell<DocumentItemRow, Number> {
        private final TextField qtyField = new TextField();
        private final TextFormatter<String> qtyFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.matches("\\d*") ? change : null;
        });
        private boolean syncingText;

        QtyEditingCell() {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            qtyField.setTextFormatter(qtyFormatter);
            qtyField.setOnAction(event -> commitQtyValue());
            qtyField.focusedProperty().addListener((obs, oldVal, focused) -> {
                if (!focused) {
                    commitQtyValue();
                }
            });
        }

        private void commitQtyValue() {
            DocumentItemRow row = getCurrentRow();
            if (row == null || internalRowChange || syncingText) {
                return;
            }

            String currentText = qtyField.getText() == null ? "" : qtyField.getText().trim();
            int parsed = 0;
            if (!currentText.isBlank()) {
                try {
                    parsed = Integer.parseInt(currentText);
                } catch (NumberFormatException ignored) {
                    parsed = 0;
                }
            }

            if (row.getQty() != parsed) {
                row.setQty(parsed);
                itemsTable.getSelectionModel().select(row);
            }
        }

        @Override
        protected void updateItem(Number item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }

            String newValue = item == null || item.intValue() <= 0 ? "" : String.valueOf(item.intValue());
            if (!qtyField.isFocused() || !Objects.equals(qtyField.getText(), newValue)) {
                syncingText = true;
                try {
                    if (!Objects.equals(qtyField.getText(), newValue)) {
                        qtyField.setText(newValue);
                    }
                } finally {
                    syncingText = false;
                }
            }
            setGraphic(qtyField);
        }

        private DocumentItemRow getCurrentRow() {
            return getTableRow() != null ? getTableRow().getItem() : null;
        }
    }

    private class PassportEditingCell extends TableCell<DocumentItemRow, Boolean> {
        private final CheckBox checkBox = new CheckBox();
        private final Label warningLabel = new Label("!");
        private final Tooltip warningTooltip = new Tooltip();
        private final HBox container = new HBox(8);
        private boolean syncingState;

        PassportEditingCell() {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            warningLabel.getStyleClass().add("eppo-warning-indicator");
            warningLabel.setManaged(false);
            warningLabel.setVisible(false);
            Tooltip.install(warningLabel, warningTooltip);
            container.getChildren().addAll(checkBox, warningLabel);

            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (syncingState) {
                    return;
                }
                DocumentItemRow row = getCurrentRow();
                if (row == null) {
                    return;
                }
                row.setPassportRequired(newVal);
                itemsTable.getSelectionModel().select(row);
            });
        }

        @Override
        protected void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);
            DocumentItemRow row = getCurrentRow();
            if (empty || row == null) {
                setGraphic(null);
                return;
            }

            syncingState = true;
            try {
                checkBox.setSelected(Boolean.TRUE.equals(item));
                checkBox.setDisable(!isPrintPassportsEnabled());
            } finally {
                syncingState = false;
            }

            String validationMessage = row.getValidationMessage();
            boolean hasWarning = validationMessage != null && !validationMessage.isBlank();
            warningLabel.setManaged(hasWarning);
            warningLabel.setVisible(hasWarning);
            warningTooltip.setText(hasWarning ? validationMessage : "");

            setGraphic(container);
        }

        private DocumentItemRow getCurrentRow() {
            return getTableRow() != null ? getTableRow().getItem() : null;
        }
    }

    private class ActionsCell extends TableCell<DocumentItemRow, DocumentItemRow> {
        private final Button deleteButton = new Button("🗑");

        ActionsCell() {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            deleteButton.getStyleClass().add("button-danger");
            deleteButton.setOnAction(event -> {
                DocumentItemRow row = getCurrentRow();
                if (row == null) {
                    return;
                }
                rows.remove(row);
                ensureTrailingBlankRow();
                refreshValidationIndicators();
            });
        }

        @Override
        protected void updateItem(DocumentItemRow item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            deleteButton.setDisable(rows.size() == 1 && item.isBlank());
            setGraphic(deleteButton);
        }

        private DocumentItemRow getCurrentRow() {
            return getTableRow() != null ? getTableRow().getItem() : null;
        }
    }

    public static class DocumentItemRow {
        private final ObjectProperty<Plant> plant = new SimpleObjectProperty<>();
        private final ObjectProperty<PlantBatch> batch = new SimpleObjectProperty<>();
        private final IntegerProperty qty = new SimpleIntegerProperty();
        private final BooleanProperty passportRequired = new SimpleBooleanProperty();
        private final BooleanProperty committed = new SimpleBooleanProperty(false);
        private final StringProperty validationMessage = new SimpleStringProperty("");
        private boolean bound;

        public Plant getPlant() { return plant.get(); }
        public void setPlant(Plant value) { plant.set(value); }
        public ObjectProperty<Plant> plantProperty() { return plant; }

        public PlantBatch getBatch() { return batch.get(); }
        public void setBatch(PlantBatch value) { batch.set(value); }
        public ObjectProperty<PlantBatch> batchProperty() { return batch; }

        public int getQty() { return qty.get(); }
        public void setQty(int value) { qty.set(value); }
        public IntegerProperty qtyProperty() { return qty; }

        public boolean isPassportRequired() { return passportRequired.get(); }
        public void setPassportRequired(boolean value) { passportRequired.set(value); }
        public BooleanProperty passportRequiredProperty() { return passportRequired; }

        public boolean isCommitted() { return committed.get(); }
        public void setCommitted(boolean value) { committed.set(value); }
        public BooleanProperty committedProperty() { return committed; }

        public String getValidationMessage() { return validationMessage.get(); }
        public void setValidationMessage(String value) { validationMessage.set(value); }
        public StringProperty validationMessageProperty() { return validationMessage; }

        public boolean isBlank() {
            return getPlant() == null && getBatch() == null && getQty() <= 0 && !isPassportRequired();
        }

        public boolean isComplete() {
            return getPlant() != null && getBatch() != null && getQty() > 0;
        }

        public boolean isBound() { return bound; }
        public void setBound(boolean bound) { this.bound = bound; }
    }
}
