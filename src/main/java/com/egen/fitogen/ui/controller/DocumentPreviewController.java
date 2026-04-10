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
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class DocumentPreviewController {

    private static final double TABLE_ROW_HEIGHT = 38;
    private static final double TABLE_HEADER_HEIGHT = 36;

    @FXML private VBox printableRoot;
    @FXML private Label documentTypeTitleLabel;
    @FXML private Label documentNumberTitleLabel;
    @FXML private Label issueDateLabel;
    @FXML private Label issuePlaceLabel;
    @FXML private Label createdByLabel;
    @FXML private Label issuerPartyNameLabel;
    @FXML private Label issuerPartyAddressLabel;
    @FXML private Label issuerPartyPhytosanitaryNumberLabel;
    @FXML private Label customerNameLabel;
    @FXML private Label customerAddressLabel;
    @FXML private Label customerPhytosanitaryNumberLabel;
    @FXML private VBox commentsSection;
    @FXML private Label commentsLabel;
    @FXML private Label cancelledBadge;
    @FXML private TableView<DocumentPreviewItemDTO> itemsTable;
    @FXML private TableColumn<DocumentPreviewItemDTO, String> colLp;
    @FXML private TableColumn<DocumentPreviewItemDTO, String> colPlant;
    @FXML private TableColumn<DocumentPreviewItemDTO, String> colBatch;
    @FXML private TableColumn<DocumentPreviewItemDTO, String> colAge;
    @FXML private TableColumn<DocumentPreviewItemDTO, String> colCategory;
    @FXML private TableColumn<DocumentPreviewItemDTO, Number> colQty;

    private final DocumentRenderService documentRenderService = new DocumentRenderService(AppContext.getDocumentService());
    private final DocumentPdfService documentPdfService = new DocumentPdfService();

    private DocumentPreviewDTO currentPreview;

    @FXML
    public void initialize() {
        colLp.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isSummaryRow() ? "" : String.valueOf(cell.getValue().getLp())));
        colPlant.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPlantName()));
        colBatch.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBatchNumber()));
        colAge.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBatchAgeLabel()));
        colCategory.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBatchCategoryLabel()));
        colQty.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQty()));

        colLp.setStyle("-fx-alignment: CENTER;");
        colAge.setStyle("-fx-alignment: CENTER;");
        colQty.setStyle("-fx-alignment: CENTER;");

        if (itemsTable != null) {
            itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            itemsTable.setPlaceholder(new Label("Brak pozycji dokumentu do wyświetlenia."));
            itemsTable.setFixedCellSize(TABLE_ROW_HEIGHT);
            itemsTable.setFocusTraversable(false);
            itemsTable.setRowFactory(table -> new TableRow<>() {
                @Override
                protected void updateItem(DocumentPreviewItemDTO item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("preview-summary-row");
                    if (!empty && item != null && item.isSummaryRow()) {
                        getStyleClass().add("preview-summary-row");
                    }
                }
            });
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

        String issuerAddress = joinPreviewLines(preview.getIssuerAddressLine1(), preview.getIssuerAddressLine2());
        String customerAddress = joinPreviewLines(preview.getCustomerAddressLine1(), preview.getCustomerAddressLine2());

        documentTypeTitleLabel.setText(buildDocumentTitle(preview));
        documentNumberTitleLabel.setText(buildDocumentNumberTitle(preview));
        issueDateLabel.setText(valueOrDash(preview.getIssueDateLabel()));
        issuePlaceLabel.setText(valueOrDash(preview.getIssuePlaceLabel()));
        createdByLabel.setText(valueOrDash(preview.getCreatedBy()));
        commentsLabel.setText(valueOrDash(preview.getComments()));
        commentsSection.setVisible(preview.getComments() != null && !preview.getComments().isBlank());
        cancelledBadge.setVisible(preview.isCancelled());
        itemsTable.setItems(buildDisplayItems(preview));
        updateItemsTableHeight(preview);

        issuerPartyNameLabel.setText(valueOrDash(preview.getIssuerName()));
        issuerPartyAddressLabel.setText(issuerAddress);
        issuerPartyPhytosanitaryNumberLabel.setText(buildPhytosanitaryLabel(preview.getIssuerPhytosanitaryNumber()));
        customerNameLabel.setText(valueOrDash(preview.getCustomerName()));
        customerAddressLabel.setText(customerAddress);
        customerPhytosanitaryNumberLabel.setText(buildPhytosanitaryLabel(preview.getCustomerPhytosanitaryNumber()));
    }

    private ObservableList<DocumentPreviewItemDTO> buildDisplayItems(DocumentPreviewDTO preview) {
        ObservableList<DocumentPreviewItemDTO> displayItems = FXCollections.observableArrayList();
        if (preview.getItems() != null) {
            displayItems.addAll(preview.getItems());
        }

        DocumentPreviewItemDTO summaryRow = new DocumentPreviewItemDTO();
        summaryRow.setSummaryRow(true);
        summaryRow.setPlantName("Suma:");
        summaryRow.setQty(preview.getTotalQty());
        displayItems.add(summaryRow);
        return displayItems;
    }

    private void updateItemsTableHeight(DocumentPreviewDTO preview) {
        if (itemsTable == null) {
            return;
        }

        int itemsCount = (preview.getItems() == null ? 0 : preview.getItems().size()) + 1;
        double preferredHeight = TABLE_HEADER_HEIGHT + Math.max(1, itemsCount) * TABLE_ROW_HEIGHT + 8;

        itemsTable.setPrefHeight(preferredHeight);
        itemsTable.setMinHeight(preferredHeight);
        itemsTable.setMaxHeight(preferredHeight);
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
            double nodeHeight = computeNodeHeight(nodeToPrint);
            PageLayout pageLayout = job.getJobSettings().getPageLayout();
            double printableWidth = pageLayout.getPrintableWidth();
            double printableHeight = pageLayout.getPrintableHeight();

            double widthScale = nodeWidth <= 0 ? 1 : printableWidth / nodeWidth;
            double heightScale = nodeHeight <= 0 ? 1 : printableHeight / nodeHeight;
            double scale = Math.min(1, Math.min(widthScale, heightScale));

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

    private String buildDocumentTitle(DocumentPreviewDTO preview) {
        String type = safe(preview.getDocumentType());
        return type.isBlank() ? "Dokument fitosanitarny" : type;
    }

    private String buildDocumentNumberTitle(DocumentPreviewDTO preview) {
        String number = safe(preview.getDocumentNumber());
        return number.isBlank() ? "nr —" : "nr " + number;
    }

    private String buildPhytosanitaryLabel(String value) {
        String safeValue = safe(value);
        return safeValue.isBlank() ? "Nr fitosanitarny: —" : "Nr fitosanitarny: " + safeValue;
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
        return safeLine1 + System.lineSeparator() + safeLine2;
    }

    private String buildPdfFileName(DocumentPreviewDTO preview) {
        String number = valueOrDash(preview.getDocumentNumber()).replaceAll("[\\/:*?\"<>|]", "_");
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

    private double computeNodeHeight(Node node) {
        double height = node.getBoundsInLocal().getHeight();
        if (height > 0) {
            return height;
        }
        if (node instanceof Region region) {
            return region.prefHeight(-1);
        }
        return 1120;
    }

    private String valueOrDash(String value) {
        String safeValue = safe(value);
        return safeValue.isBlank() ? "—" : safeValue;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @FXML
    private void close() {
        Stage stage = getStage();
        if (stage != null) {
            stage.close();
        }
    }

    private Stage getStage() {
        return printableRoot != null && printableRoot.getScene() != null
                ? (Stage) printableRoot.getScene().getWindow()
                : null;
    }
}
