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
            DialogUtil.showError("Błąd podglądu", "Nie udało się załadować podglądu dokumentu. Sprawdź, czy dokument nadal istnieje i spróbuj ponownie.");
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
        if (warningSummaryLabel != null) {
            warningSummaryLabel.setText("Ostrzeżenia i uwagi operacyjne");
        }
        if (warningArea != null) {
            warningArea.setText(buildOperationalWarningsText(preview));
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
            DialogUtil.showWarning("Brak danych", "Nie załadowano danych dokumentu do zapisu PDF.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Zapisz dokument jako plik PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Plik PDF", "*.pdf"));
        chooser.setInitialFileName(buildPdfFileName(currentPreview));

        File selectedFile = chooser.showSaveDialog(getStage());
        if (selectedFile == null) {
            return;
        }

        try {
            documentPdfService.export(currentPreview, selectedFile);
            DialogUtil.showSuccess(buildPdfSaveSuccessMessage(selectedFile));
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu PDF", "Nie udało się zapisać dokumentu jako pliku PDF.");
        }
    }


    private String buildPreviewSummary(DocumentPreviewDTO preview) {
        int itemsCount = preview.getItems() == null ? 0 : preview.getItems().size();
        StringBuilder builder = new StringBuilder();
        builder.append("Status: ").append(valueOrDash(preview.getStatusLabel())).append(". ");
        builder.append("Typ dokumentu: ").append(valueOrDash(preview.getDocumentType())).append(". ");
        builder.append("Pozycje: ").append(itemsCount).append(". ");
        builder.append("Łączna ilość: ").append(preview.getTotalQty()).append('.');
        if (preview.isCancelled()) {
            builder.append(" Dokument jest anulowany.");
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
            UiTextUtil.appendParagraph(builder, "Dokument ma status aktywny.");
        }

        UiTextUtil.appendLabelValue(builder, "Pozycje dokumentu", itemsCount);
        UiTextUtil.appendLabelValue(builder, "Pozycje z wymaganiem paszportu", passportRequiredCount);
        UiTextUtil.appendLabelValue(builder, "Łączna ilość", preview.getTotalQty());
        UiTextUtil.appendEmptyLine(builder);

        if (passportRequiredCount > 0) {
            UiTextUtil.appendParagraph(builder, "Uwaga: co najmniej jedna pozycja wymaga paszportu. Zweryfikuj finalne oznaczenia i komplet danych przed wydrukiem lub eksportem PDF.");
        }

        if (safe(preview.getCustomerPhytosanitaryNumber()).isBlank()) {
            UiTextUtil.appendParagraph(builder, "Odbiorca nie ma wpisanego numeru fitosanitarnego. Jeśli dokument lub kierunek wysyłki tego wymaga, uzupełnij dane kontrahenta.");
        }

        if (itemsCount == 0) {
            builder.append("Dokument nie zawiera pozycji. Taki podgląd traktuj jako niekompletny.");
        } else {
            builder.append("Podgląd ma charakter informacyjny. Ostateczną walidację pozycji wykonuj w formularzu dokumentu przed zatwierdzeniem operacji.");
        }
        return builder.toString();
    }

    private String buildEppoInfoText(DocumentPreviewDTO preview) {
        StringBuilder builder = new StringBuilder();
        UiTextUtil.appendParagraph(builder, "Ta sekcja ma charakter informacyjny i nie zmienia treści dokumentu.");
        UiTextUtil.appendLabelValue(builder, "Klient", valueOrDash(preview.getCustomerName()));
        UiTextUtil.appendLabelValue(builder, "Typ dokumentu", valueOrDash(preview.getDocumentType()));
        UiTextUtil.appendLabelValue(builder, "Status", valueOrDash(preview.getStatusLabel()));
        UiTextUtil.appendLabelValue(builder, "Liczba pozycji", preview.getItems() == null ? 0 : preview.getItems().size());
        UiTextUtil.appendLabelValue(builder, "Łączna ilość", preview.getTotalQty());
        UiTextUtil.appendEmptyLine(builder);
        if (preview.isCancelled()) {
            UiTextUtil.appendParagraph(builder, "Ostrzeżenie: dokument został anulowany. Nie używaj go jako aktywnego dokumentu operacyjnego.");
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

    private String buildPdfSaveSuccessMessage(File selectedFile) {
        return "Zapis pliku PDF zakończył się powodzeniem:\n" + selectedFile.getAbsolutePath();
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