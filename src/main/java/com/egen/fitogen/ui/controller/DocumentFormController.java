package com.egen.fitogen.ui.controller;

import javafx.util.StringConverter;
import javafx.application.Platform;
import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.database.SqliteContrahentRepository;
import com.egen.fitogen.database.SqlitePlantBatchRepository;
import com.egen.fitogen.database.SqlitePlantRepository;
import com.egen.fitogen.dto.DocumentDTO;
import com.egen.fitogen.dto.DocumentItemDTO;
import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.DocumentStatus;
import com.egen.fitogen.model.DocumentType;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.model.PlantBatchStatus;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.repository.PlantBatchRepository;
import com.egen.fitogen.repository.PlantRepository;
import com.egen.fitogen.service.AppUserService;
import com.egen.fitogen.service.DocumentService;
import com.egen.fitogen.service.DocumentTypeService;
import com.egen.fitogen.service.EppoAdvisoryService;
import com.egen.fitogen.service.EppoCodePlantLinkService;
import com.egen.fitogen.service.PassportAdvisoryService;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.UiTextUtil;
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
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DocumentFormController {

    @FXML private TextField documentNumberField;
    @FXML private ComboBox<DocumentType> documentTypeBox;
    @FXML private ComboBox<Contrahent> contrahentBox;
    @FXML private DatePicker issueDateField;
    @FXML private ComboBox<AppUser> createdByBox;
    @FXML private TextArea commentsField;
    @FXML private Label statusValueLabel;
    @FXML private TableView<DocumentItemRow> itemsTable;
    @FXML private TableColumn<DocumentItemRow, Number> colLp;
    @FXML private TableColumn<DocumentItemRow, DocumentItemRow> colPlant;
    @FXML private TableColumn<DocumentItemRow, DocumentItemRow> colBatch;
    @FXML private TableColumn<DocumentItemRow, Number> colQty;
    @FXML private TableColumn<DocumentItemRow, Boolean> colPassport;
    @FXML private TableColumn<DocumentItemRow, DocumentItemRow> colActions;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label eppoInfoSummaryLabel;
    @FXML private TextArea eppoInfoArea;

    private final DocumentService documentService = AppContext.getDocumentService();
    private final DocumentTypeService documentTypeService = AppContext.getDocumentTypeService();
    private final AppUserService appUserService = AppContext.getAppUserService();
    private final EppoCodePlantLinkService eppoCodePlantLinkService = AppContext.getEppoCodePlantLinkService();
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

        issueDateField.setValue(LocalDate.now());
        updateStatusLabel(DocumentStatus.ACTIVE);
        documentNumberField.setEditable(false);
        documentNumberField.setText("Zostanie nadany automatycznie przy zapisie");

        selectDefaultUser();
        ensureTrailingBlankRow();
        configureEppoInfoArea();
        refreshEppoInfo();
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
        refreshEppoInfo();
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
        documentTypeBox.setEditable(false);
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
    }

    private void configureContrahentBox() {
        contrahentBox.setItems(FXCollections.observableArrayList(allContrahents));
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
        contrahentBox.valueProperty().addListener((obs, oldVal, newVal) -> refreshEppoInfo());
    }

    private void configureTable() {
        itemsTable.setItems(rows);
        itemsTable.setEditable(false);

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
            autoAppendIfReady(row);
            itemsTable.refresh();
            refreshEppoInfo();
        });

        row.batchProperty().addListener((obs, oldVal, newVal) -> {
            autoAppendIfReady(row);
            itemsTable.refresh();
        });

        row.qtyProperty().addListener((obs, oldVal, newVal) -> {
            autoAppendIfReady(row);
            refreshEppoInfo();
        });

        row.passportRequiredProperty().addListener((obs, oldVal, newVal) -> {
            autoAppendIfReady(row);
            refreshEppoInfo();
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
    }

    private void openAddBatchForRow(DocumentItemRow row) {
        if (row == null || row.getPlant() == null) {
            DialogUtil.showWarning("Brak rośliny", "Najpierw wybierz roślinę dla pozycji dokumentu.");
            return;
        }

        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/plant_batch_form.fxml"));
        try {
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 900, 760);
            java.net.URL stylesheetUrl = getClass().getResource("/styles/app.css");
            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            }

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Dodaj partię roślin");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setMinWidth(860);
            stage.setMinHeight(700);

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
            });

            stage.showAndWait();
            itemsTable.refresh();

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd", "Nie udało się otworzyć formularza partii roślin.");
        }
    }

    private void configureEppoInfoArea() {
        if (eppoInfoArea != null) {
            eppoInfoArea.setEditable(false);
            eppoInfoArea.setWrapText(true);
        }
    }

    private void refreshEppoInfo() {
        if (eppoInfoArea == null || eppoInfoSummaryLabel == null) {
            return;
        }

        Contrahent contrahent = contrahentBox.getValue();
        String clientCountryCode = contrahent != null ? safe(contrahent.getCountryCode()).toUpperCase() : "";

        List<String> messages = new ArrayList<>();

        if (contrahent == null) {
            eppoInfoSummaryLabel.setText("Informacje EPPO dla klienta");
            eppoInfoArea.setText("Wybierz klienta, aby zobaczyć informacyjne dopasowanie EPPO dla pozycji dokumentu.");
            return;
        }

        boolean missingCountryCode = clientCountryCode.isBlank();

        for (int i = 0; i < rows.size(); i++) {
            DocumentItemRow row = rows.get(i);
            if (row == null || row.isBlank()) {
                continue;
            }

            Plant plant = resolvePlantForRow(row);
            if (plant == null) {
                messages.add("Pozycja " + (i + 1) + ": brak wybranej rośliny.");
                continue;
            }

            messages.add(buildRowAdvisoryMessage(i + 1, row, plant, clientCountryCode, missingCountryCode));
        }

        String summarySuffix = missingCountryCode
                ? " [brak kodu kraju klienta]"
                : " [" + clientCountryCode + "]";
        eppoInfoSummaryLabel.setText("Informacje EPPO dla klienta: " + safe(contrahent.getName()) + summarySuffix);

        if (messages.isEmpty()) {
            eppoInfoArea.setText("Dodaj pozycje dokumentu, aby zobaczyć informacyjne dopasowanie EPPO dla kraju klienta.");
            return;
        }

        eppoInfoArea.setText(String.join("\n\n", messages));
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
                EppoAdvisoryService.AdvisoryResult eppoResult = eppoAdvisoryService
                        .analyzePlantForCountry(plant, clientCountryCode, i + 1, "Pozycja");
                if (eppoResult.elevatedRisk()) {
                    elevatedRiskCount++;
                    details.add("Pozycja " + (i + 1) + ": znaleziono dopasowane strefy EPPO dla kraju klienta.");
                }
                if (eppoResult.missingCode()) {
                    missingCodeCount++;
                    details.add("Pozycja " + (i + 1) + ": brak powiązanego kodu EPPO w słowniku.");
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
                message.append("- ").append(details.get(i)).append(UiTextUtil.NL);
            }
            if (details.size() > limit) {
                message.append("- ... oraz ").append(details.size() - limit).append(" kolejnych uwag").append(UiTextUtil.NL);
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
        DocumentType selectedType = documentTypeBox.getValue();
        if (selectedType == null) {
            throw new IllegalArgumentException("Wybierz typ dokumentu.");
        }
        AppUser selectedUser = createdByBox.getValue();
        if (selectedUser == null) {
            throw new IllegalArgumentException("Wybierz użytkownika w polu „Utworzył”.");
        }
        Contrahent selectedContrahent = contrahentBox.getValue();
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
        dto.setDocumentNumber(safe(documentNumberField.getText()));
        dto.setDocumentType(selectedType.getName());
        dto.setIssueDate(issueDateField.getValue());
        dto.setContrahentId(selectedContrahent.getId());
        dto.setCreatedBy(selectedUser.getDisplayName());
        dto.setComments(commentsField.getText());
        dto.setItems(items);
        return dto;
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private class PlantEditingCell extends TableCell<DocumentItemRow, DocumentItemRow> {
        private final ComboBox<Plant> plantBox = new ComboBox<>();

        PlantEditingCell() {
            plantBox.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(plantBox, Priority.ALWAYS);
            plantBox.setItems(FXCollections.observableArrayList(allPlants));
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
            plantBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                DocumentItemRow row = getCurrentRow();
                if (row == null) {
                    return;
                }
                if (row.getPlant() != newVal) {
                    row.setPlant(newVal);
                }
                itemsTable.getSelectionModel().select(row);
            });
        }

        @Override
        protected void updateItem(DocumentItemRow item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            plantBox.setValue(item.getPlant());
            setGraphic(plantBox);
        }

        private DocumentItemRow getCurrentRow() {
            return getTableRow() != null ? getTableRow().getItem() : null;
        }
    }

    private class BatchEditingCell extends TableCell<DocumentItemRow, DocumentItemRow> {
        private final ComboBox<PlantBatch> batchBox = new ComboBox<>();
        private boolean handlingSpecialOption = false;

        BatchEditingCell() {
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

            batchBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (handlingSpecialOption) {
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

                Platform.runLater(() -> itemsTable.getSelectionModel().select(row));
            });
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
            batchBox.setItems(FXCollections.observableArrayList(options));
            batchBox.setDisable(item.getPlant() == null);
            batchBox.setValue(item.getBatch());
            handlingSpecialOption = false;

            setGraphic(batchBox);
        }

        private DocumentItemRow getCurrentRow() {
            return getTableRow() != null ? getTableRow().getItem() : null;
        }
    }

    private class QtyEditingCell extends TableCell<DocumentItemRow, Number> {
        private final TextField qtyField = new TextField();

        QtyEditingCell() {
            qtyField.textProperty().addListener((obs, oldVal, newVal) -> {
                DocumentItemRow row = getCurrentRow();
                if (row == null || internalRowChange) {
                    return;
                }
                int parsed = 0;
                if (newVal != null && !newVal.isBlank()) {
                    try {
                        parsed = Integer.parseInt(newVal.trim());
                    } catch (NumberFormatException ignored) {
                        parsed = 0;
                    }
                }
                row.setQty(parsed);
                itemsTable.getSelectionModel().select(row);
            });
        }

        @Override
        protected void updateItem(Number item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            internalRowChange = true;
            qtyField.setText(item == null || item.intValue() <= 0 ? "" : String.valueOf(item.intValue()));
            internalRowChange = false;
            setGraphic(qtyField);
        }

        private DocumentItemRow getCurrentRow() {
            return getTableRow() != null ? getTableRow().getItem() : null;
        }
    }

    private class PassportEditingCell extends TableCell<DocumentItemRow, Boolean> {
        private final CheckBox checkBox = new CheckBox();

        PassportEditingCell() {
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
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
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            checkBox.setSelected(Boolean.TRUE.equals(item));
            setGraphic(checkBox);
        }

        private DocumentItemRow getCurrentRow() {
            return getTableRow() != null ? getTableRow().getItem() : null;
        }
    }

    private class ActionsCell extends TableCell<DocumentItemRow, DocumentItemRow> {
        private final Button deleteButton = new Button("🗑");

        ActionsCell() {
            deleteButton.getStyleClass().add("button-danger");
            deleteButton.setOnAction(event -> {
                DocumentItemRow row = getCurrentRow();
                if (row == null) {
                    return;
                }
                rows.remove(row);
                ensureTrailingBlankRow();
                refreshEppoInfo();
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
