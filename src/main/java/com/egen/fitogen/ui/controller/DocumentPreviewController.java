package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.dto.DocumentPreviewDTO;
import com.egen.fitogen.dto.DocumentPreviewItemDTO;
import com.egen.fitogen.dto.PassportPreviewDTO;
import com.egen.fitogen.util.EuFlagRenderer;
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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class DocumentPreviewController {

    private static final double TABLE_ROW_HEIGHT = 40;
    private static final double TABLE_HEADER_HEIGHT = 42;

    @FXML private VBox printablePagesContainer;
    @FXML private VBox printableRoot;
    @FXML private VBox passportPagesContainer;
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
    @FXML private HBox itemsSummaryRow;
    @FXML private Label summaryLabelCell;
    @FXML private Label totalQtyLabel;
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
        colLp.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getLp())));
        colPlant.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPlantName()));
        colBatch.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBatchNumber()));
        colAge.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBatchAgeLabel()));
        colCategory.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBatchCategoryLabel()));
        colQty.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQty()));

        colLp.setStyle("-fx-alignment: CENTER;");
        colBatch.setStyle("-fx-alignment: CENTER;");
        colAge.setStyle("-fx-alignment: CENTER;");
        colCategory.setStyle("-fx-alignment: CENTER;");
        colQty.setStyle("-fx-alignment: CENTER;");

        if (itemsTable != null) {
            itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            itemsTable.setPlaceholder(new Label("Brak pozycji dokumentu do wyświetlenia."));
            itemsTable.setFixedCellSize(TABLE_ROW_HEIGHT);
            itemsTable.setFocusTraversable(false);
        }

        if (summaryLabelCell != null) {
            summaryLabelCell.prefWidthProperty().bind(
                    colLp.widthProperty()
                            .add(colPlant.widthProperty())
                            .add(colBatch.widthProperty())
                            .add(colAge.widthProperty())
                            .add(colCategory.widthProperty())
            );
            summaryLabelCell.minWidthProperty().bind(summaryLabelCell.prefWidthProperty());
            summaryLabelCell.maxWidthProperty().bind(summaryLabelCell.prefWidthProperty());
            summaryLabelCell.setMinHeight(TABLE_HEADER_HEIGHT);
            summaryLabelCell.setPrefHeight(TABLE_HEADER_HEIGHT);
            summaryLabelCell.setMaxHeight(TABLE_HEADER_HEIGHT);
        }

        if (totalQtyLabel != null) {
            totalQtyLabel.prefWidthProperty().bind(colQty.widthProperty());
            totalQtyLabel.minWidthProperty().bind(totalQtyLabel.prefWidthProperty());
            totalQtyLabel.maxWidthProperty().bind(totalQtyLabel.prefWidthProperty());
            totalQtyLabel.setMinHeight(TABLE_HEADER_HEIGHT);
            totalQtyLabel.setPrefHeight(TABLE_HEADER_HEIGHT);
            totalQtyLabel.setMaxHeight(TABLE_HEADER_HEIGHT);
        }

        if (itemsSummaryRow != null) {
            itemsSummaryRow.setMinHeight(TABLE_HEADER_HEIGHT);
            itemsSummaryRow.setPrefHeight(TABLE_HEADER_HEIGHT);
            itemsSummaryRow.setMaxHeight(TABLE_HEADER_HEIGHT);
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

    public void setPreview(DocumentPreviewDTO preview) {
        try {
            applyPreview(preview);
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd podglądu", "Nie udało się przygotować podglądu dokumentu.");
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
        totalQtyLabel.setText(String.valueOf(preview.getTotalQty()));
        if (itemsSummaryRow != null) {
            itemsSummaryRow.setVisible(true);
            itemsSummaryRow.setManaged(true);
        }
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
        rebuildPassportPages(preview);
    }


    private void rebuildPassportPages(DocumentPreviewDTO preview) {
        if (passportPagesContainer == null) {
            return;
        }
        passportPagesContainer.getChildren().clear();

        if (preview == null || !preview.isPrintPassports() || preview.getPassports() == null || preview.getPassports().isEmpty()) {
            passportPagesContainer.setVisible(false);
            passportPagesContainer.setManaged(false);
            return;
        }

        java.util.List<PassportPreviewDTO> passports = preview.getPassports();
        for (int start = 0; start < passports.size(); start += 2) {
            int end = Math.min(start + 2, passports.size());
            passportPagesContainer.getChildren().add(buildPassportPage(passports.subList(start, end)));
        }

        passportPagesContainer.setVisible(true);
        passportPagesContainer.setManaged(true);
    }

    private VBox buildPassportPage(java.util.List<PassportPreviewDTO> passports) {
        VBox page = new VBox(18);
        page.setMaxWidth(940);
        page.getStyleClass().addAll("preview-sheet", "preview-sheet-a4", "preview-passport-page");

        for (PassportPreviewDTO passport : passports) {
            VBox block = new VBox(8);
            block.getStyleClass().add("preview-passport-block");

            Label headerLine = new Label(buildPassportHeader(passport));
            headerLine.setWrapText(true);
            headerLine.getStyleClass().add("preview-passport-meta-header");

            StackPane centeredCard = new StackPane();
            centeredCard.setAlignment(Pos.TOP_CENTER);
            centeredCard.getStyleClass().add("preview-passport-centered");

            VBox card = new VBox(0);
            card.getStyleClass().add("preview-passport-card");

            HBox topSection = new HBox(12);
            topSection.setAlignment(Pos.TOP_LEFT);
            topSection.getStyleClass().add("preview-passport-top");

            javafx.scene.image.ImageView flagView = new javafx.scene.image.ImageView(EuFlagRenderer.createFxImage(420, 280));
            flagView.setPreserveRatio(true);
            flagView.setFitWidth(145);
            flagView.setFitHeight(82);
            flagView.getStyleClass().add("preview-passport-flag-image");

            VBox topText = new VBox(4);
            topText.setAlignment(Pos.CENTER);
            HBox.setHgrow(topText, Priority.ALWAYS);
            Label line1 = new Label("Paszport roślin / Plant passport");
            line1.getStyleClass().add("preview-passport-header");
            topText.getChildren().add(line1);
            if (passport.isProtectedZone()) {
                Label line2 = new Label("Strefa chroniona / ZP");
                line2.getStyleClass().add("preview-passport-subheader");
                Label line3 = new Label(valueOrDash(passport.getProtectedZoneCode()));
                line3.getStyleClass().add("preview-passport-eppo-code");
                topText.getChildren().addAll(line2, line3);
            }
            topSection.getChildren().addAll(flagView, topText);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(6);
            grid.getStyleClass().add("preview-passport-grid");
            addPassportRow(grid, 0, "A", valueOrDash(passport.getBotanicalName()));
            addPassportRow(grid, 1, "B", valueOrDash(passport.getOperatorNumber()));
            addPassportRow(grid, 2, "C", valueOrDash(passport.getTraceabilityCode()));
            addPassportRow(grid, 3, "D", valueOrDash(passport.getOriginCountryCode()));

            StackPane bottomCentered = new StackPane(grid);
            bottomCentered.setAlignment(Pos.CENTER_LEFT);
            VBox.setVgrow(bottomCentered, Priority.ALWAYS);

            card.getChildren().addAll(topSection, bottomCentered);
            centeredCard.getChildren().add(card);
            block.getChildren().addAll(headerLine, centeredCard);
            page.getChildren().add(block);
        }

        return page;
    }

    private void addPassportRow(GridPane grid, int rowIndex, String code, String value) {
        Label codeLabel = new Label(code);
        codeLabel.getStyleClass().add("preview-passport-code");
        Label valueLabel = new Label(value);
        valueLabel.setWrapText(true);
        valueLabel.getStyleClass().add("preview-passport-value");
        grid.add(codeLabel, 0, rowIndex);
        grid.add(valueLabel, 1, rowIndex);
    }

    private String buildPassportHeader(PassportPreviewDTO passport) {
        return "Paszport roślin do dokumentu nr: " + valueOrDash(passport.getDocumentNumber())
                + " / Pozycja nr: " + passport.getItemNo()
                + " / Roślina: " + valueOrDash(passport.getPlantName())
                + " / Ilość: " + valueOrDash(passport.getQuantityLabel())
                + " / Kategoria: " + valueOrDash(passport.getCategoryLabel());
    }

    private void appendDetail(StringBuilder sb, String label, String value) {
        String safeValue = safe(value);
        if (safeValue.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("\n");
        }
        sb.append(label).append(": ").append(safeValue);
    }

    private java.util.List<Region> collectPrintablePages() {
        java.util.List<Region> pages = new java.util.ArrayList<>();
        if (printableRoot != null) {
            pages.add(printableRoot);
        }
        if (passportPagesContainer != null) {
            for (Node child : passportPagesContainer.getChildren()) {
                if (child instanceof Region region && child.isManaged()) {
                    pages.add(region);
                }
            }
        }
        return pages;
    }

    private ObservableList<DocumentPreviewItemDTO> buildDisplayItems(DocumentPreviewDTO preview) {
        ObservableList<DocumentPreviewItemDTO> displayItems = FXCollections.observableArrayList();
        if (preview.getItems() != null) {
            displayItems.addAll(preview.getItems());
        }
        return displayItems;
    }

    private void updateItemsTableHeight(DocumentPreviewDTO preview) {
        if (itemsTable == null) {
            return;
        }

        int itemsCount = preview.getItems() == null ? 0 : preview.getItems().size();
        double preferredHeight = TABLE_HEADER_HEIGHT + Math.max(1, itemsCount) * TABLE_ROW_HEIGHT + 2;

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

        try {
            PageLayout pageLayout = job.getJobSettings().getPageLayout();
            boolean success = true;
            for (Region page : collectPrintablePages()) {
                double originalScaleX = page.getScaleX();
                double originalScaleY = page.getScaleY();
                try {
                    page.applyCss();
                    page.layout();

                    double nodeWidth = computeNodeWidth(page);
                    double nodeHeight = computeNodeHeight(page);
                    double printableWidth = pageLayout.getPrintableWidth();
                    double printableHeight = pageLayout.getPrintableHeight();

                    double widthScale = nodeWidth <= 0 ? 1 : printableWidth / nodeWidth;
                    double heightScale = nodeHeight <= 0 ? 1 : printableHeight / nodeHeight;
                    double scale = Math.min(1, Math.min(widthScale, heightScale));

                    page.setScaleX(scale);
                    page.setScaleY(scale);

                    if (!job.printPage(page)) {
                        success = false;
                        break;
                    }
                } finally {
                    page.setScaleX(originalScaleX);
                    page.setScaleY(originalScaleY);
                }
            }

            if (success) {
                job.endJob();
            } else {
                DialogUtil.showError("Błąd drukowania", "Nie udało się wydrukować dokumentu.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd drukowania", "Nie udało się wydrukować dokumentu.");
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
