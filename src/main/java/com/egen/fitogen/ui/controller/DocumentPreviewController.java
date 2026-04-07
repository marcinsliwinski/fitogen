package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.dto.DocumentPreviewDTO;
import com.egen.fitogen.dto.DocumentPreviewItemDTO;
import com.egen.fitogen.service.DocumentPdfService;
import com.egen.fitogen.service.DocumentRenderService;
import com.egen.fitogen.ui.util.DialogUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class DocumentPreviewController {

    @FXML private VBox printableRoot;
    @FXML private Label documentNumberLabel;
    @FXML private Label documentTypeLabel;
    @FXML private Label statusLabel;
    @FXML private Label issueDateLabel;
    @FXML private Label createdByLabel;
    @FXML private Label contrahentNameLabel;
    @FXML private Label contrahentAddressLabel;
    @FXML private Label previewSummaryLabel;
    @FXML private Label issuerNameLabel;
    @FXML private Label issuerAddressLabel;
    @FXML private Label issuerPhytosanitaryNumberLabel;
    @FXML private Label customerNameLabel;
    @FXML private Label customerAddressLabel;
    @FXML private Label customerPhytosanitaryNumberLabel;
    @FXML private Label eppoInfoSummaryLabel;
    @FXML private TextArea eppoInfoArea;
    @FXML private VBox commentsSection;
    @FXML private Label commentsLabel;
    @FXML private Label cancelledBadge;
    @FXML private TableView<DocumentPreviewItemDTO> itemsTable;
    @FXML private TableColumn<DocumentPreviewItemDTO, Number> colLp;
    @FXML private TableColumn<DocumentPreviewItemDTO, String> colPlant;
    @FXML private TableColumn<DocumentPreviewItemDTO, String> colBatch;
    @FXML private TableColumn<DocumentPreviewItemDTO, Number> colQty;
    @FXML private TableColumn<DocumentPreviewItemDTO, String> colPassport;

    private final DocumentRenderService documentRenderService = new DocumentRenderService(AppContext.getDocumentService());
    private final DocumentPdfService documentPdfService = new DocumentPdfService();

    private DocumentPreviewDTO currentPreview;

    @FXML
    public void initialize() {
        colLp.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getLp()));
        colPlant.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPlantName()));
        colBatch.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBatchNumber()));
        colQty.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQty()));
        colPassport.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPassportLabel()));

        if (itemsTable != null) {
            itemsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            itemsTable.setPlaceholder(new Label("Brak pozycji dokumentu do wyświetlenia."));
        }
        if (eppoInfoArea != null) {
            eppoInfoArea.setEditable(false);
            eppoInfoArea.setWrapText(true);
            eppoInfoArea.setFocusTraversable(false);
        }

        commentsSection.managedProperty().bind(commentsSection.visibleProperty());
        cancelledBadge.managedProperty().bind(cancelledBadge.visibleProperty());
    }

    public void setDocumentId(int documentId) {
        try {
            DocumentPreviewDTO preview = documentRenderService.buildPreview(documentId);
            applyPreview(preview);
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd podglądu", "Nie udało się załadować podglądu dokumentu.");
            close();
        }
    }

    private void applyPreview(DocumentPreviewDTO preview) {
        currentPreview = preview;

        documentNumberLabel.setText(valueOrDash(preview.getDocumentNumber()));
        documentTypeLabel.setText(valueOrDash(preview.getDocumentType()));
        statusLabel.setText(valueOrDash(preview.getStatusLabel()));
        issueDateLabel.setText(valueOrDash(preview.getIssueDateLabel()));
        createdByLabel.setText(valueOrDash(preview.getCreatedBy()));
        contrahentNameLabel.setText(valueOrDash(preview.getContrahentName()));
        contrahentAddressLabel.setText(valueOrDash(preview.getContrahentAddress()));
        commentsLabel.setText(valueOrDash(preview.getComments()));
        commentsSection.setVisible(preview.getComments() != null && !preview.getComments().isBlank());
        cancelledBadge.setVisible(preview.isCancelled());
        itemsTable.setItems(FXCollections.observableArrayList(preview.getItems()));

        if (previewSummaryLabel != null) {
            previewSummaryLabel.setText(buildPreviewSummary(preview));
        }
        if (issuerNameLabel != null) {
            issuerNameLabel.setText(valueOrDash(preview.getIssuerName()));
        }
        if (issuerAddressLabel != null) {
            issuerAddressLabel.setText(joinPreviewLines(preview.getIssuerAddressLine1(), preview.getIssuerAddressLine2()));
        }
        if (issuerPhytosanitaryNumberLabel != null) {
            issuerPhytosanitaryNumberLabel.setText(buildPhytosanitaryLabel(preview.getIssuerPhytosanitaryNumber()));
        }
        if (customerNameLabel != null) {
            customerNameLabel.setText(valueOrDash(preview.getCustomerName()));
        }
        if (customerAddressLabel != null) {
            customerAddressLabel.setText(joinPreviewLines(preview.getCustomerAddressLine1(), preview.getCustomerAddressLine2()));
        }
        if (customerPhytosanitaryNumberLabel != null) {
            customerPhytosanitaryNumberLabel.setText(buildPhytosanitaryLabel(preview.getCustomerPhytosanitaryNumber()));
        }
        if (eppoInfoSummaryLabel != null) {
            eppoInfoSummaryLabel.setText("Informacje EPPO i paszportowe");
        }
        if (eppoInfoArea != null) {
            eppoInfoArea.setText(buildEppoInfoText(preview));
        }
    }

    @FXML
    private void printDocument() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            DialogUtil.showError("Błąd drukowania", "Nie udało się uruchomić drukarki.");
            return;
        }

        if (!job.showPrintDialog(getStage())) {
            return;
        }

        Region nodeToPrint = printableRoot;
        double originalScaleX = nodeToPrint.getScaleX();
        double originalScaleY = nodeToPrint.getScaleY();

        try {
            nodeToPrint.applyCss();
            nodeToPrint.layout();

            double nodeWidth = computeNodeWidth(nodeToPrint);
            PageLayout pageLayout = job.getJobSettings().getPageLayout();
            double printableWidth = pageLayout.getPrintableWidth();
            double scale = nodeWidth <= 0 ? 1 : Math.min(1, printableWidth / nodeWidth);

            nodeToPrint.setScaleX(scale);
            nodeToPrint.setScaleY(scale);

            boolean success = job.printPage(nodeToPrint);
            if (success) {
                job.endJob();
            } else {
                DialogUtil.showError("Błąd drukowania", "Nie udało się wydrukować dokumentu.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd drukowania", "Nie udało się wydrukować dokumentu.");
        } finally {
            nodeToPrint.setScaleX(originalScaleX);
            nodeToPrint.setScaleY(originalScaleY);
        }
    }

    @FXML
    private void exportPdf() {
        if (currentPreview == null) {
            DialogUtil.showWarning("Brak danych", "Nie załadowano danych dokumentu do eksportu.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Zapisz dokument jako PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Plik PDF", "*.pdf"));
        chooser.setInitialFileName(buildPdfFileName(currentPreview));

        File selectedFile = chooser.showSaveDialog(getStage());
        if (selectedFile == null) {
            return;
        }

        try {
            documentPdfService.export(currentPreview, selectedFile);
            DialogUtil.showSuccess("Dokument został zapisany jako PDF:\n" + selectedFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd eksportu", "Nie udało się zapisać dokumentu jako PDF.");
        }
    }


    private String buildPreviewSummary(DocumentPreviewDTO preview) {
        int itemsCount = preview.getItems() == null ? 0 : preview.getItems().size();
        StringBuilder builder = new StringBuilder();
        builder.append("Status: ").append(valueOrDash(preview.getStatusLabel()));
        builder.append(" | Typ: ").append(valueOrDash(preview.getDocumentType()));
        builder.append(" | Pozycje: ").append(itemsCount);
        builder.append(" | Łączna ilość: ").append(preview.getTotalQty());
        if (preview.isCancelled()) {
            builder.append(" | Ostrzeżenie: dokument jest anulowany");
        }
        return builder.toString();
    }

    private String buildPhytosanitaryLabel(String value) {
        String safeValue = safe(value);
        if (safeValue.isBlank()) {
            return "Nr fitosanitarny: —";
        }
        return "Nr fitosanitarny: " + safeValue;
    }

    private String joinPreviewLines(String line1, String line2) {
        String safeLine1 = safe(line1);
        String safeLine2 = safe(line2);
        if (safeLine1.isBlank()) {
            return valueOrDash(safeLine2);
        }
        if (safeLine2.isBlank()) {
            return safeLine1;
        }
        return safeLine1 + "\n" + safeLine2;
    }

    private String buildEppoInfoText(DocumentPreviewDTO preview) {
        StringBuilder builder = new StringBuilder();
        builder.append("Ta sekcja ma charakter informacyjny i nie zmienia treści dokumentu.\n\n");
        builder.append("Klient: ").append(valueOrDash(preview.getCustomerName())).append("\n");
        builder.append("Typ dokumentu: ").append(valueOrDash(preview.getDocumentType())).append("\n");
        builder.append("Status: ").append(valueOrDash(preview.getStatusLabel())).append("\n");
        builder.append("Liczba pozycji: ").append(preview.getItems() == null ? 0 : preview.getItems().size()).append("\n");
        builder.append("Łączna ilość: ").append(preview.getTotalQty()).append("\n\n");
        if (preview.isCancelled()) {
            builder.append("Ostrzeżenie: dokument został anulowany. Nie używaj go jako aktywnego dokumentu operacyjnego.\n\n");
        }
        builder.append("Uwaga: szczegółowe dopasowanie EPPO dla kraju klienta jest rozwijane w formularzu dokumentu i dalszych etapach modułu referencyjnego.");
        return builder.toString();
    }

    private String buildPdfFileName(DocumentPreviewDTO preview) {
        String number = valueOrDash(preview.getDocumentNumber()).replaceAll("[\\\\/:*?\"<>|]", "_");
        if (number.equals("—")) {
            number = "dokument";
        }
        return number + ".pdf";
    }

    private double computeNodeWidth(Node node) {
        double width = node.getBoundsInLocal().getWidth();
        if (width > 0) {
            return width;
        }
        if (node instanceof Region region) {
            return region.prefWidth(-1);
        }
        return 800;
    }

    @FXML
    private void close() {
        getStage().close();
    }

    private Stage getStage() {
        return (Stage) printableRoot.getScene().getWindow();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}