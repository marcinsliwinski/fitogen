package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.database.SqliteContrahentRepository;
import com.egen.fitogen.database.SqliteDocumentRepository;
import com.egen.fitogen.database.SqlitePlantRepository;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.DocumentStatus;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.model.PlantBatchStatus;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.repository.DocumentRepository;
import com.egen.fitogen.repository.PlantRepository;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.ContrahentService;
import com.egen.fitogen.service.PlantBatchService;
import com.egen.fitogen.service.PlantService;
import com.egen.fitogen.ui.router.ViewManager;
import com.egen.fitogen.ui.util.ModalViewUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class DashboardController {

    @FXML private Label plantsCountLabel;
    @FXML private Label contrahentsCountLabel;
    @FXML private Label batchesCountLabel;
    @FXML private Label documentsCountLabel;

    @FXML private Label backupStatusValueLabel;
    @FXML private Label issuerStatusValueLabel;
    @FXML private Label documentsStatusValueLabel;
    @FXML private Label batchesStatusValueLabel;

    @FXML private VBox issuerIncompleteBox;

    @FXML private VBox recentDocumentsBox;
    @FXML private VBox recentBatchesBox;

    @FXML private Button completeIssuerButton;

    private final PlantService plantService = AppContext.getPlantService();
    private final ContrahentService contrahentService = AppContext.getContrahentService();
    private final PlantBatchService plantBatchService = AppContext.getPlantBatchService();
    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();

    private final PlantRepository plantRepository = new SqlitePlantRepository();
    private final ContrahentRepository contrahentRepository = new SqliteContrahentRepository();
    private final DocumentRepository documentRepository = new SqliteDocumentRepository();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        if (issuerIncompleteBox != null) {
            issuerIncompleteBox.managedProperty().bind(issuerIncompleteBox.visibleProperty());
        }
        refreshDashboard();
    }

    private void refreshDashboard() {
        List<Plant> plants = plantService.getAllPlants();
        List<Contrahent> contrahents = contrahentService.getAllContrahents();
        List<PlantBatch> batches = plantBatchService.getAllBatches();
        List<Document> documents = documentRepository.findAll();

        plantsCountLabel.setText(String.valueOf(plants.size()));
        contrahentsCountLabel.setText(String.valueOf(contrahents.size()));
        batchesCountLabel.setText(String.valueOf(batches.size()));
        documentsCountLabel.setText(String.valueOf(documents.size()));

        fillSystemState(documents, batches);
        updateIssuerCompletenessSection();
        fillRecentDocuments(documents);
        fillRecentBatches(batches);
    }

    private void fillSystemState(List<Document> documents, List<PlantBatch> batches) {
        String lastBackupAt = appSettingsService.getLastBackupAt();
        String backupText = lastBackupAt == null || lastBackupAt.isBlank()
                ? "Brak wykonanej kopii zapasowej"
                : "Ostatnia kopia zapasowa: " + lastBackupAt;
        backupStatusValueLabel.setText(backupText);

        issuerStatusValueLabel.setText(
                appSettingsService.isIssuerProfileComplete()
                        ? "Dane podmiotu są kompletne"
                        : "Dane podmiotu są niekompletne"
        );

        long activeDocuments = documents.stream()
                .filter(document -> document.getStatus() == null || document.getStatus() == DocumentStatus.ACTIVE)
                .count();
        long cancelledDocuments = documents.stream()
                .filter(document -> document.getStatus() == DocumentStatus.CANCELLED)
                .count();

        long activeBatches = batches.stream()
                .filter(batch -> batch.getStatus() == null || batch.getStatus() == PlantBatchStatus.ACTIVE)
                .count();
        long cancelledBatches = batches.stream()
                .filter(batch -> batch.getStatus() == PlantBatchStatus.CANCELLED)
                .count();

        documentsStatusValueLabel.setText("Aktywne: " + activeDocuments + " | Anulowane: " + cancelledDocuments);
        batchesStatusValueLabel.setText("Aktywne: " + activeBatches + " | Anulowane: " + cancelledBatches);
    }

    private void updateIssuerCompletenessSection() {
        boolean complete = appSettingsService.isIssuerProfileComplete();
        issuerIncompleteBox.setVisible(!complete);
    }

    private void fillRecentDocuments(List<Document> documents) {
        recentDocumentsBox.getChildren().clear();

        documents.stream()
                .sorted(Comparator
                        .comparing(Document::getIssueDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Document::getId, Comparator.reverseOrder()))
                .limit(5)
                .forEach(document -> {
                    Hyperlink link = new Hyperlink(
                            safe(document.getDocumentNumber())
                                    + " • "
                                    + safe(document.getDocumentType())
                                    + " • "
                                    + getContrahentName(document.getContrahentId())
                                    + " • "
                                    + (document.getIssueDate() != null
                                    ? document.getIssueDate().format(dateFormatter)
                                    : "")
                    );
                    link.setWrapText(true);
                    link.setMaxWidth(Double.MAX_VALUE);
                    applyLinkStatusStyle(link, document.getStatus() == DocumentStatus.CANCELLED, false);
                    link.setOnAction(event -> openDocumentPreview(document));
                    recentDocumentsBox.getChildren().add(link);
                });

        if (recentDocumentsBox.getChildren().isEmpty()) {
            recentDocumentsBox.getChildren().add(new Label("Brak dokumentów do wyświetlenia."));
        }
    }

    private void fillRecentBatches(List<PlantBatch> batches) {
        recentBatchesBox.getChildren().clear();

        batches.stream()
                .sorted(Comparator
                        .comparing(PlantBatch::getCreationDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PlantBatch::getId, Comparator.reverseOrder()))
                .limit(5)
                .forEach(batch -> {
                    Hyperlink link = new Hyperlink(
                            getPlantLabel(batch.getPlantId())
                                    + " • "
                                    + batchLabel(batch)
                                    + " • ilość: "
                                    + batch.getQty()
                    );
                    link.setWrapText(true);
                    link.setMaxWidth(Double.MAX_VALUE);
                    applyLinkStatusStyle(link, batch.getStatus() == PlantBatchStatus.CANCELLED, false);
                    link.setOnAction(event -> openBatchPreview(batch));
                    recentBatchesBox.getChildren().add(link);
                });

        if (recentBatchesBox.getChildren().isEmpty()) {
            recentBatchesBox.getChildren().add(new Label("Brak partii do wyświetlenia."));
        }
    }

    private void applyLinkStatusStyle(Hyperlink link, boolean cancelled, boolean inactive) {
        if (link == null) {
            return;
        }

        link.getStyleClass().removeAll("cancelled-row", "inactive-row");

        if (cancelled) {
            link.getStyleClass().add("cancelled-row");
        } else if (inactive) {
            link.getStyleClass().add("inactive-row");
        }
    }

    private void openDocumentPreview(Document document) {
        ModalViewUtil.openModal(
                "/view/document_preview.fxml",
                "Podgląd dokumentu",
                980, 760,
                920, 700,
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

    private String getPlantLabel(int plantId) {
        Plant plant = plantRepository.findAll().stream()
                .filter(p -> p.getId() == plantId)
                .findFirst()
                .orElse(null);

        if (plant == null) {
            return "Nieznana roślina";
        }

        String species = safe(plant.getSpecies()).trim();
        String rootstock = safe(plant.getRootstock()).trim();
        String variety = safe(plant.getVariety()).trim();

        StringBuilder sb = new StringBuilder();
        if (!species.isEmpty()) {
            sb.append(species);
        }
        if (!rootstock.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append("/");
            }
            sb.append(rootstock);
        }
        if (!variety.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append("'").append(variety).append("'");
        }

        return sb.isEmpty() ? "Roślina ID: " + plantId : sb.toString();
    }

    private String getContrahentName(int contrahentId) {
        Contrahent contrahent = contrahentRepository.findAll().stream()
                .filter(c -> c.getId() == contrahentId)
                .findFirst()
                .orElse(null);

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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @FXML
    private void openSettings() {
        ViewManager.show(ViewManager.SETTINGS);
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