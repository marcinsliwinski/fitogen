package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.DocumentStatus;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.model.PlantBatchStatus;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.AuditLogService;
import com.egen.fitogen.service.ContrahentService;
import com.egen.fitogen.service.PlantBatchService;
import com.egen.fitogen.service.PlantService;
import com.egen.fitogen.database.SqliteDocumentRepository;
import com.egen.fitogen.repository.DocumentRepository;
import com.egen.fitogen.ui.router.ViewManager;
import com.egen.fitogen.ui.util.ModalViewUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter BACKUP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private VBox backupCardBox;
    @FXML private Label backupStatusValueLabel;
    @FXML private Label backupStatusNoteLabel;

    @FXML private Label plantsCountLabel;
    @FXML private Label contrahentsCountLabel;
    @FXML private Label batchesCountLabel;
    @FXML private Label documentsCountLabel;

    @FXML private TableView<Document> recentDocumentsTable;
    @FXML private TableColumn<Document, String> colRecentDocumentNumber;
    @FXML private TableColumn<Document, String> colRecentDocumentType;
    @FXML private TableColumn<Document, String> colRecentDocumentContrahent;
    @FXML private TableColumn<Document, String> colRecentDocumentDate;
    @FXML private TableColumn<Document, String> colRecentDocumentStatus;

    @FXML private TableView<PlantBatch> recentBatchesTable;
    @FXML private TableColumn<PlantBatch, String> colRecentBatchPlant;
    @FXML private TableColumn<PlantBatch, String> colRecentBatchNumber;
    @FXML private TableColumn<PlantBatch, String> colRecentBatchQty;
    @FXML private TableColumn<PlantBatch, String> colRecentBatchDate;
    @FXML private TableColumn<PlantBatch, String> colRecentBatchStatus;

    private final PlantService plantService = AppContext.getPlantService();
    private final ContrahentService contrahentService = AppContext.getContrahentService();
    private final PlantBatchService plantBatchService = AppContext.getPlantBatchService();
    private final DocumentRepository documentRepository = new SqliteDocumentRepository();
    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final AuditLogService auditLogService = AppContext.getAuditLogService();

    private final Map<Integer, Plant> plantMap = new HashMap<>();
    private final Map<Integer, Contrahent> contrahentMap = new HashMap<>();

    @FXML
    public void initialize() {
        if (backupCardBox != null) {
            backupCardBox.getStyleClass().addAll("dashboard-stat-card", "dashboard-clickable-card");
        }
        configureRecentDocumentsTable();
        configureRecentBatchesTable();
        refreshDashboard();
    }

    private void refreshDashboard() {
        List<Plant> plants = plantService.getAllPlants();
        List<Contrahent> contrahents = contrahentService.getAllContrahents();
        List<PlantBatch> batches = plantBatchService.getAllBatches();
        List<Document> documents = documentRepository.findAll();

        plantMap.clear();
        for (Plant plant : plants) {
            plantMap.put(plant.getId(), plant);
        }

        contrahentMap.clear();
        for (Contrahent contrahent : contrahents) {
            contrahentMap.put(contrahent.getId(), contrahent);
        }

        plantsCountLabel.setText(String.valueOf(plants.size()));
        contrahentsCountLabel.setText(String.valueOf(contrahents.size()));
        batchesCountLabel.setText(String.valueOf(batches.size()));
        documentsCountLabel.setText(String.valueOf(documents.size()));

        fillBackupCard();
        fillRecentDocuments(documents);
        fillRecentBatches(batches);
    }

    private void configureRecentDocumentsTable() {
        if (recentDocumentsTable == null) {
            return;
        }

        recentDocumentsTable.setPlaceholder(new Label("Brak dokumentów do wyświetlenia."));
        recentDocumentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

                if (colRecentDocumentNumber != null) {
            colRecentDocumentNumber.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getDocumentNumber())));
        }
        if (colRecentDocumentType != null) {
            colRecentDocumentType.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getDocumentType())));
        }
        if (colRecentDocumentContrahent != null) {
            colRecentDocumentContrahent.setCellValueFactory(data -> new SimpleStringProperty(getContrahentName(data.getValue().getContrahentId())));
        }
        if (colRecentDocumentDate != null) {
            colRecentDocumentDate.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().getIssueDate())));
        }
        if (colRecentDocumentStatus != null) {
            colRecentDocumentStatus.setCellValueFactory(data -> new SimpleStringProperty(formatDocumentStatus(data.getValue().getStatus())));
        }

        recentDocumentsTable.setRowFactory(table -> {
            TableRow<Document> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openDocumentPreview(row.getItem());
                }
            });
            return row;
        });
    }

    private void configureRecentBatchesTable() {
        if (recentBatchesTable == null) {
            return;
        }

        recentBatchesTable.setPlaceholder(new Label("Brak partii do wyświetlenia."));
        recentBatchesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

                if (colRecentBatchPlant != null) {
            colRecentBatchPlant.setCellValueFactory(data -> new SimpleStringProperty(getPlantLabel(data.getValue().getPlantId())));
        }
        if (colRecentBatchNumber != null) {
            colRecentBatchNumber.setCellValueFactory(data -> new SimpleStringProperty(batchLabel(data.getValue())));
        }
        if (colRecentBatchQty != null) {
            colRecentBatchQty.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getQty())));
        }
        if (colRecentBatchDate != null) {
            colRecentBatchDate.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().getCreationDate())));
        }
        if (colRecentBatchStatus != null) {
            colRecentBatchStatus.setCellValueFactory(data -> new SimpleStringProperty(formatBatchStatus(data.getValue().getStatus())));
        }

        recentBatchesTable.setRowFactory(table -> {
            TableRow<PlantBatch> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openBatchPreview(row.getItem());
                }
            });
            return row;
        });
    }

    private void fillBackupCard() {
        if (backupStatusValueLabel == null || backupStatusNoteLabel == null || backupCardBox == null) {
            return;
        }

        backupCardBox.getStyleClass().remove("stat-card-alert");
        backupStatusValueLabel.getStyleClass().remove("stat-value-danger");

        String lastBackupAtRaw = safe(appSettingsService.getLastBackupAt());
        LocalDateTime lastBackupAt = parseBackupDate(lastBackupAtRaw);
        LocalDateTime latestAuditAt = auditLogService == null ? null : auditLogService.getLatestChangedAt();

        boolean staleBackup = lastBackupAt == null
                || (latestAuditAt != null && latestAuditAt.isAfter(lastBackupAt) && lastBackupAt.plusDays(7).isBefore(LocalDateTime.now()));

        if (staleBackup) {
            backupCardBox.getStyleClass().add("stat-card-alert");
            backupStatusValueLabel.getStyleClass().add("stat-value-danger");
            backupStatusValueLabel.setText("Zrób kopię");
            if (lastBackupAt == null) {
                backupStatusNoteLabel.setText("Brak wykonanej kopii zapasowej. Kliknij kafelek, aby przejść do sekcji kopii zapasowej.");
            } else {
                backupStatusNoteLabel.setText("Ostatnia kopia: " + lastBackupAtRaw + ". Od tego czasu w systemie zapisano nowe zmiany.");
            }
            return;
        }

        backupStatusValueLabel.setText(lastBackupAtRaw);
        backupStatusNoteLabel.setText("Kopia zapasowa jest aktualna.");
    }

    private void fillRecentDocuments(List<Document> documents) {
        if (recentDocumentsTable == null) {
            return;
        }

        recentDocumentsTable.setItems(FXCollections.observableArrayList(
                documents.stream()
                        .sorted(Comparator
                                .comparing(Document::getIssueDate, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(Document::getId, Comparator.reverseOrder()))
                        .limit(8)
                        .toList()
        ));
    }

    private void fillRecentBatches(List<PlantBatch> batches) {
        if (recentBatchesTable == null) {
            return;
        }

        recentBatchesTable.setItems(FXCollections.observableArrayList(
                batches.stream()
                        .sorted(Comparator
                                .comparing(PlantBatch::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(PlantBatch::getId, Comparator.reverseOrder()))
                        .limit(8)
                        .toList()
        ));
    }

    private LocalDateTime parseBackupDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(rawValue.trim(), BACKUP_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String getPlantLabel(int plantId) {
        Plant plant = plantMap.get(plantId);
        if (plant == null) {
            return "Nieznana roślina";
        }

        String species = safe(plant.getSpecies()).trim();
        String rootstock = safe(plant.getRootstock()).trim();
        String variety = safe(plant.getVariety()).trim();

        StringBuilder builder = new StringBuilder();
        if (!species.isEmpty()) {
            builder.append(species);
        }
        if (!rootstock.isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(rootstock);
        }
        if (!variety.isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(variety);
        }
        return builder.isEmpty() ? "Roślina ID: " + plantId : builder.toString();
    }

    private String getContrahentName(int contrahentId) {
        Contrahent contrahent = contrahentMap.get(contrahentId);
        return contrahent == null ? "Brak kontrahenta" : safe(contrahent.getName());
    }

    private String batchLabel(PlantBatch batch) {
        String interior = safe(batch.getInteriorBatchNo()).trim();
        String exterior = safe(batch.getExteriorBatchNo()).trim();
        if (!interior.isEmpty() && !exterior.isEmpty()) {
            return interior + " / " + exterior;
        }
        if (!interior.isEmpty()) {
            return interior;
        }
        if (!exterior.isEmpty()) {
            return exterior;
        }
        return "Partia ID: " + batch.getId();
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.format(DATE_FORMATTER);
    }

    private String formatDocumentStatus(DocumentStatus status) {
        return status == null || status == DocumentStatus.ACTIVE ? "Aktywny" : "Anulowany";
    }

    private String formatBatchStatus(PlantBatchStatus status) {
        return status == null || status == PlantBatchStatus.ACTIVE ? "Aktywna" : "Anulowana";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @FXML
    private void openBackupSettings() {
        SettingsController.requestInitialTab("Kopia zapasowa i baza");
        ViewManager.show(ViewManager.SETTINGS);
    }

    private void openDocumentPreview(Document document) {
        ModalViewUtil.openModal(
                "/view/document_preview.fxml",
                "Podgląd dokumentu",
                1120, 900,
                980, 760,
                (DocumentPreviewController controller) -> controller.setDocumentId(document.getId())
        );
        refreshDashboard();
    }

    private void openBatchPreview(PlantBatch batch) {
        ModalViewUtil.openModal(
                "/view/plant_batch_form.fxml",
                "Podgląd / edycja partii",
                900, 760,
                860, 720,
                (PlantBatchFormController controller) -> controller.setPlantBatch(batch)
        );
        refreshDashboard();
    }

    @FXML
    private void addPlant() {
        ModalViewUtil.openModal(
                "/view/plant_form.fxml",
                "Dodaj roślinę",
                860, 660,
                820, 620,
                (PlantFormController controller) -> controller.setPlantService(plantService)
        );
        refreshDashboard();
    }

    @FXML
    private void addContrahent() {
        ModalViewUtil.openModal(
                "/view/contrahent_form.fxml",
                "Dodaj kontrahenta",
                860, 660,
                820, 620,
                (ContrahentFormController controller) -> {
                }
        );
        refreshDashboard();
    }

    @FXML
    private void addBatch() {
        ModalViewUtil.openModal(
                "/view/plant_batch_form.fxml",
                "Dodaj partię",
                900, 760,
                860, 720,
                (PlantBatchFormController controller) -> {
                }
        );
        refreshDashboard();
    }

    @FXML
    private void addDocument() {
        ModalViewUtil.openModal(
                "/view/document_form.fxml",
                "Dodaj dokument",
                1180, 840,
                1100, 780,
                (DocumentFormController controller) -> {
                }
        );
        refreshDashboard();
    }
}
