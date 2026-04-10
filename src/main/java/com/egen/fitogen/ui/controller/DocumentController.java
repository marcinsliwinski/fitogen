package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.database.SqliteContrahentRepository;
import com.egen.fitogen.database.SqliteDocumentRepository;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.DocumentStatus;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.repository.DocumentRepository;
import com.egen.fitogen.service.DocumentService;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ModalViewUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.egen.fitogen.ui.util.UiTextUtil;

public class DocumentController {

    @FXML private TableView<Document> table;
    @FXML private TableColumn<Document, Integer> colId;
    @FXML private TableColumn<Document, String> colNumber;
    @FXML private TableColumn<Document, String> colType;
    @FXML private TableColumn<Document, String> colContrahent;
    @FXML private TableColumn<Document, String> colCreatedBy;
    @FXML private TableColumn<Document, String> colIssueDate;
    @FXML private TableColumn<Document, String> colComments;
    @FXML private TableColumn<Document, String> colStatus;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterBox;
    @FXML private Label filterStatusLabel;
    @FXML private Label filterSummaryLabel;

    private final DocumentRepository documentRepository = new SqliteDocumentRepository();
    private final ContrahentRepository contrahentRepository = new SqliteContrahentRepository();
    private final DocumentService documentService = AppContext.getDocumentService();

    private final ObservableList<Document> masterData = FXCollections.observableArrayList();
    private final Map<Integer, String> contrahentNames = new HashMap<>();
    private FilteredList<Document> filtered;

    @FXML
    public void initialize() {
        configureColumns();
        configureRowFactory();
        configureStatusFilter();
        configureSearch();
        configureTableBehavior();
        refresh();
    }

    private void configureColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNumber.setCellValueFactory(new PropertyValueFactory<>("documentNumber"));
        colType.setCellValueFactory(new PropertyValueFactory<>("documentType"));
        colContrahent.setCellValueFactory(cell ->
                new SimpleStringProperty(getContrahentName(cell.getValue().getContrahentId()))
        );
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        colIssueDate.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getIssueDate() != null
                                ? cell.getValue().getIssueDate().toString()
                                : ""
                )
        );
        colComments.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getComments() != null ? cell.getValue().getComments() : ""
                )
        );
        colStatus.setCellValueFactory(cell ->
                new SimpleStringProperty(formatStatus(cell.getValue().getStatus()))
        );
    }

    private void configureRowFactory() {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Document item, boolean empty) {
                super.updateItem(item, empty);

                getStyleClass().remove("cancelled-row");

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                if (item.getStatus() == DocumentStatus.CANCELLED) {
                    getStyleClass().add("cancelled-row");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void configureStatusFilter() {
        statusFilterBox.setItems(FXCollections.observableArrayList(
                "Wszystkie",
                "Aktywne",
                "Anulowane"
        ));
        statusFilterBox.setValue("Wszystkie");
    }

    private void configureSearch() {
        filtered = new FilteredList<>(masterData, d -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilterBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateFilterSummary());

        SortedList<Document> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
    }

    private void configureTableBehavior() {
        if (table == null) {
            return;
        }

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Brak dokumentów do wyświetlenia."));
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedStatus = statusFilterBox.getValue() == null ? "Wszystkie" : statusFilterBox.getValue();

        filtered.setPredicate(document -> matchesSearch(document, keyword) && matchesStatus(document, selectedStatus));
        updateFilterSummary();
    }

    private boolean matchesSearch(Document document, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }

        return contains(document.getDocumentNumber(), keyword)
                || contains(document.getDocumentType(), keyword)
                || contains(document.getCreatedBy(), keyword)
                || contains(document.getComments(), keyword)
                || contains(formatStatus(document.getStatus()), keyword)
                || contains(getContrahentName(document.getContrahentId()), keyword)
                || (document.getIssueDate() != null && document.getIssueDate().toString().contains(keyword));
    }

    private boolean matchesStatus(Document document, String selectedStatus) {
        DocumentStatus status = document.getStatus() == null ? DocumentStatus.ACTIVE : document.getStatus();

        return switch (selectedStatus) {
            case "Aktywne" -> status == DocumentStatus.ACTIVE;
            case "Anulowane" -> status == DocumentStatus.CANCELLED;
            default -> true;
        };
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void refresh() {
        loadContrahents();
        masterData.setAll(documentRepository.findAll());
        applyFilters();
        updateFilterSummary();
    }

    private void loadContrahents() {
        contrahentNames.clear();
        List<Contrahent> contrahents = contrahentRepository.findAll();
        for (Contrahent contrahent : contrahents) {
            contrahentNames.put(contrahent.getId(), contrahent.getName());
        }
    }

    private String getContrahentName(int contrahentId) {
        return contrahentNames.getOrDefault(contrahentId, "");
    }

    private String formatStatus(DocumentStatus status) {
        if (status == null) {
            return "Aktywny";
        }

        return switch (status) {
            case ACTIVE -> "Aktywny";
            case CANCELLED -> "Anulowany";
        };
    }


    private void updateFilterSummary() {
        if (filterStatusLabel == null || filterSummaryLabel == null || filtered == null) {
            return;
        }

        String keyword = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim();
        String selectedStatus = statusFilterBox == null || statusFilterBox.getValue() == null ? "Wszystkie" : statusFilterBox.getValue();
        long totalCount = masterData.size();
        long visibleCount = filtered.size();
        long activeCount = filtered.stream().filter(document -> (document.getStatus() == null ? DocumentStatus.ACTIVE : document.getStatus()) == DocumentStatus.ACTIVE).count();
        long cancelledCount = filtered.stream().filter(document -> (document.getStatus() == null ? DocumentStatus.ACTIVE : document.getStatus()) == DocumentStatus.CANCELLED).count();

        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("Łącznie dokumentów: ").append(totalCount)
                .append(". Widoczne po filtrach: ").append(visibleCount)
                .append(". Aktywne: ").append(activeCount)
                .append(". Anulowane: ").append(cancelledCount)
                .append(". Status listy: ").append(selectedStatus).append('.');
        if (!keyword.isBlank()) {
            statusBuilder.append(UiTextUtil.buildQuotedFilterSuffix("Fraza", keyword));
        }
        filterStatusLabel.setText(statusBuilder.toString());

        Document selected = table == null ? null : table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            filterSummaryLabel.setText(buildDocumentSummary(selected));
            return;
        }

        Document firstVisible = filtered.stream().findFirst().orElse(null);
        if (firstVisible != null) {
            filterSummaryLabel.setText(buildDocumentSummary(firstVisible));
        } else {
            filterSummaryLabel.setText("Brak dokumentów spełniających bieżące filtry.");
        }
    }

    private String buildDocumentSummary(Document document) {
        String number = safe(document.getDocumentNumber());
        String type = safe(document.getDocumentType());
        String contrahent = safe(getContrahentName(document.getContrahentId()));
        String issueDate = document.getIssueDate() == null ? "—" : document.getIssueDate().toString();
        String comments = safe(document.getComments());
        String commentsSuffix = comments.isBlank() ? "" : " Uwagi: " + comments;

        return "Dokument „" + number + "” | Typ: " + (type.isBlank() ? "—" : type)
                + " | Klient: " + (contrahent.isBlank() ? "—" : contrahent)
                + " | Data wystawienia: " + issueDate
                + " | Status: " + formatStatus(document.getStatus())
                + commentsSuffix;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @FXML
    private void clearFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        if (statusFilterBox != null) {
            statusFilterBox.setValue("Wszystkie");
        }
        applyFilters();
    }
    @FXML
    private void addDocument() {
        ModalViewUtil.openModal(
                "/view/document_form.fxml",
                "Dodaj dokument",
                1220, 880,
                1080, 760,
                (DocumentFormController controller) -> {
                }
        );
        refresh();
    }

    @FXML
    private void editDocument() {
        Document selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz dokument do edycji.");
            return;
        }

        if (selected.getStatus() == DocumentStatus.CANCELLED) {
            DialogUtil.showWarning(
                    "Dokument anulowany",
                    "Anulowany dokument nie może być edytowany."
            );
            return;
        }

        ModalViewUtil.openModal(
                "/view/document_form.fxml",
                "Edytuj dokument",
                1220, 880,
                1080, 760,
                (DocumentFormController controller) -> controller.setDocument(selected)
        );
        refresh();
    }

    @FXML
    private void previewDocument() {
        Document selected = getSelectedDocumentForAction("podglądu");
        if (selected == null) {
            return;
        }
        openDocumentPreview(selected);
    }

    @FXML
    private void printDocument() {
        Document selected = getSelectedDocumentForAction("drukowania");
        if (selected == null) {
            return;
        }
        openDocumentPreview(selected);
    }

    private void openDocumentPreview(Document selected) {
        ModalViewUtil.openModal(
                "/view/document_preview.fxml",
                "Podgląd dokumentu",
                1120, 900,
                980, 760,
                (DocumentPreviewController controller) -> controller.setDocumentId(selected.getId())
        );
    }

    private Document getSelectedDocumentForAction(String actionLabel) {
        Document selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz dokument do " + actionLabel + ".");
            return null;
        }
        return selected;
    }

    private String buildDocumentConfirmationLabel(Document document) {
        String number = document == null ? "" : safe(document.getDocumentNumber());
        String type = document == null ? "" : safe(document.getDocumentType());

        if (!number.isBlank() && !type.isBlank()) {
            return number + " („" + type + "”)";
        }
        if (!number.isBlank()) {
            return number;
        }
        if (!type.isBlank()) {
            return type;
        }
        return "wybrany dokument";
    }


    @FXML
    private void deleteDocument() {
        Document selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz dokument do anulowania.");
            return;
        }

        if (selected.getStatus() == DocumentStatus.CANCELLED) {
            DialogUtil.showWarning(
                    "Dokument już anulowany",
                    "Wybrany dokument został już wcześniej anulowany."
            );
            return;
        }

        if (!DialogUtil.confirmCancellation("dokumentu", buildDocumentConfirmationLabel(selected))) {
            return;
        }

        try {
            documentService.deleteDocument(selected.getId());
            refresh();
            DialogUtil.showSuccess("Dokument został anulowany.");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd anulowania", "Nie udało się anulować dokumentu.");
        }
    }
}