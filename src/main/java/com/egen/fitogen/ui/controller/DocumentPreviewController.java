package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.dto.DocumentPreviewDTO;
import com.egen.fitogen.dto.DocumentPreviewItemDTO;
import com.egen.fitogen.service.DocumentPdfService;
import com.egen.fitogen.service.DocumentRenderService;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.UiTextUtil;
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

    private static final double TABLE_ROW_HEIGHT = 36;
    private static final double TABLE_HEADER_HEIGHT = 34;
    private static final double TABLE_MIN_HEIGHT = 180;
    private static final double TABLE_MAX_HEIGHT = 360;

    @FXML private VBox printableRoot;
    @FXML private Label documentTypeTitleLabel;
    @FXML private Label documentNumberTitleLabel;
    @FXML private Label previewSummaryLabel;
    @FXML private Label statusLabel;
    @FXML private Label issueDateLabel;
    @FXML private Label createdByLabel;
    @FXML private Label totalQtyLabel;
    @FXML private Label issuerNameLabel;
    @FXML private Label issuerAddressLabel;
    @FXML private Label issuerPhytosanitaryNumberLabel;
    @FXML private Label customerNameLabel;
    @FXML private Label customerAddressLabel;
    @FXML private Label customerPhytosanitaryNumberLabel;
    @FXML private Label warningSummaryLabel;
    @FXML private TextArea warningArea;
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
            itemsTable.setFixedCellSize(TABLE_ROW_HEIGHT);
            itemsTable.setFocusTraversable(false);
        }
        if (warningArea != null) {
            warningArea.setEditable(false);
            warningArea.setWrapText(true);
            warningArea.setFocusTraversable(false);
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

        documentTypeTitleLabel.setText(valueOrDash(preview.getDocumentType()));
        documentNumberTitleLabel.setText(buildDocumentNumberTitle(preview));
        statusLabel.setText(valueOrDash(preview.getStatusLabel()));
        issueDateLabel.setText(valueOrDash(preview.getIssueDateLabel()));
        createdByLabel.setText(valueOrDash(preview.getCreatedBy()));
        totalQtyLabel.setText(String.valueOf(preview.getTotalQty()));
        commentsLabel.setText(valueOrDash(preview.getComments()));
        commentsSection.setVisible(preview.getComments() != null && !preview.getComments().isBlank());
        cancelledBadge.setVisible(preview.isCancelled());
        itemsTable.setItems(FXCollections.observableArrayList(preview.getItems()));
        updateItemsTableHeight(preview);

        previewSummaryLabel.setText(buildPreviewSummary(preview));
        issuerNameLabel.setText(valueOrDash(preview.getIssuerName()));
        issuerAddressLabel.setText(joinPreviewLines(preview.getIssuerAddressLine1(), preview.getIssuerAddressLine2()));
        issuerPhytosanitaryNumberLabel.setText(buildPhytosanitaryLabel(preview.getIssuerPhytosanitaryNumber()));
        customerNameLabel.setText(valueOrDash(preview.getCustomerName()));
        customerAddressLabel.setText(joinPreviewLines(preview.getCustomerAddressLine1(), preview.getCustomerAddressLine2()));
        customerPhytosanitaryNumberLabel.setText(buildPhytosanitaryLabel(preview.getCustomerPhytosanitaryNumber()));
        warningSummaryLabel.setText("Uwagi operacyjne do weryfikacji");
        warningArea.setText(buildOperationalWarningsText(preview));
        eppoInfoSummaryLabel.setText("Informacje EPPO i paszportowe");
        eppoInfoArea.setText(buildEppoInfoText(preview));
    }

    private void updateItemsTableHeight(DocumentPreviewDTO preview) {
        if (itemsTable == null) {
            return;
        }

        int itemsCount = preview.getItems() == null ? 0 : preview.getItems().size();
        double preferredHeight = TABLE_HEADER_HEIGHT + Math.max(1, itemsCount) * TABLE_ROW_HEIGHT + 12;
        preferredHeight = Math.max(TABLE_MIN_HEIGHT, Math.min(TABLE_MAX_HEIGHT, preferredHeight));

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

    private String buildDocumentNumberTitle(DocumentPreviewDTO preview) {
        String number = safe(preview.getDocumentNumber());
        if (number.isBlank()) {
            return "Numer dokumentu: —";
        }
        return "Numer dokumentu: " + number;
    }

    private String buildPreviewSummary(DocumentPreviewDTO preview) {
        StringBuilder builder = new StringBuilder();
        builder.append("Klient: ").append(valueOrDash(preview.getCustomerName()));
        builder.append(" • Data wystawienia: ").append(valueOrDash(preview.getIssueDateLabel()));
        builder.append(" • Status: ").append(valueOrDash(preview.getStatusLabel()));
        if (preview.isCancelled()) {
            builder.append(" • Dokument anulowany");
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
        return safeLine1 + UiTextUtil.NL + safeLine2;
    }

    private String buildOperationalWarningsText(DocumentPreviewDTO preview) {
        int itemsCount = preview.getItems() == null ? 0 : preview.getItems().size();
        long passportRequiredCount = preview.getItems() == null
                ? 0
                : preview.getItems().stream()
                .filter(item -> "Tak".equalsIgnoreCase(safe(item.getPassportLabel())))
                .count();

        StringBuilder builder = new StringBuilder();
        if (preview.isCancelled()) {
            UiTextUtil.appendParagraph(builder, "Dokument został anulowany. Traktuj go wyłącznie jako zapis historyczny i nie używaj go do bieżącej obsługi operacyjnej.");
        } else {
            UiTextUtil.appendParagraph(builder, "Układ wydruku i PDF odpowiada sekcji drukowalnej powyżej. Przed finalnym użyciem sprawdź tylko dane biznesowe.");
        }

        UiTextUtil.appendLabelValue(builder, "Pozycje dokumentu", itemsCount);
        UiTextUtil.appendLabelValue(builder, "Pozycje z wymaganiem paszportu", passportRequiredCount);
        UiTextUtil.appendLabelValue(builder, "Łączna ilość", preview.getTotalQty());
        UiTextUtil.appendEmptyLine(builder);

        if (passportRequiredCount > 0) {
            UiTextUtil.appendParagraph(builder, "Co najmniej jedna pozycja wymaga paszportu. Zweryfikuj oznaczenia i komplet danych przed wydrukiem lub eksportem PDF.");
        }

        if (safe(preview.getCustomerPhytosanitaryNumber()).isBlank()) {
            UiTextUtil.appendParagraph(builder, "Odbiorca nie ma wpisanego numeru fitosanitarnego. Jeśli dokument lub kierunek wysyłki tego wymaga, uzupełnij dane kontrahenta.");
        }

        if (itemsCount == 0) {
            builder.append("Dokument nie zawiera pozycji. Taki podgląd traktuj jako niekompletny.");
        } else {
            builder.append("Sekcje informacyjne poniżej nie są częścią wydruku i PDF — służą wyłącznie do weryfikacji w aplikacji.");
        }
        return builder.toString();
    }

    private String buildEppoInfoText(DocumentPreviewDTO preview) {
        StringBuilder builder = new StringBuilder();
        UiTextUtil.appendParagraph(builder, "Ta sekcja ma charakter informacyjny i nie zmienia treści wydruku dokumentu.");
        UiTextUtil.appendLabelValue(builder, "Klient", valueOrDash(preview.getCustomerName()));
        UiTextUtil.appendLabelValue(builder, "Typ dokumentu", valueOrDash(preview.getDocumentType()));
        UiTextUtil.appendLabelValue(builder, "Status", valueOrDash(preview.getStatusLabel()));
        UiTextUtil.appendLabelValue(builder, "Liczba pozycji", preview.getItems() == null ? 0 : preview.getItems().size());
        UiTextUtil.appendLabelValue(builder, "Łączna ilość", preview.getTotalQty());
        UiTextUtil.appendEmptyLine(builder);
        if (preview.isCancelled()) {
            UiTextUtil.appendParagraph(builder, "Ostrzeżenie: dokument został anulowany. Nie używaj go jako aktywnego dokumentu operacyjnego.");
        }
        builder.append("Szczegółowe dopasowanie EPPO dla kraju klienta pozostaje elementem roboczym i nie jest drukowane w finalnym układzie dokumentu.");
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
