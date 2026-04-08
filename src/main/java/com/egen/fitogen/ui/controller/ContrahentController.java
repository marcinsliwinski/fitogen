package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.service.ContrahentService;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ModalViewUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import com.egen.fitogen.ui.util.UiTextUtil;

public class ContrahentController {

    @FXML private TableView<Contrahent> table;
    @FXML private TableColumn<Contrahent, Integer> colId;
    @FXML private TableColumn<Contrahent, String> colName;
    @FXML private TableColumn<Contrahent, String> colCountry;
    @FXML private TableColumn<Contrahent, String> colCity;
    @FXML private TableColumn<Contrahent, String> colPhyto;
    @FXML private TextField searchField;
    @FXML private Label filterStatusLabel;
    @FXML private Label filterSummaryLabel;

    private final ContrahentService contrahentService = AppContext.getContrahentService();

    private final ObservableList<Contrahent> masterData = FXCollections.observableArrayList();
    private FilteredList<Contrahent> filteredData;

    @FXML
    public void initialize() {
        configureColumns();
        configureTableBehavior();
        configureSearch();
        refresh();
    }

    private void configureColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCountry.setCellValueFactory(new PropertyValueFactory<>("country"));
        colCity.setCellValueFactory(new PropertyValueFactory<>("city"));
        colPhyto.setCellValueFactory(new PropertyValueFactory<>("phytosanitaryNumber"));
    }

    private void configureTableBehavior() {
        if (table == null) {
            return;
        }

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Brak kontrahentów do wyświetlenia."));
    }

    private void configureSearch() {
        filteredData = new FilteredList<>(masterData, p -> true);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        if (table != null) {
            table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateFilterSummary());
        }

        SortedList<Contrahent> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
    }

    private void applyFilters() {
        if (filteredData == null) {
            return;
        }

        String keyword = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        filteredData.setPredicate(contrahent -> {
            if (keyword.isBlank()) {
                return true;
            }

            return safeContains(contrahent.getName(), keyword)
                    || safeContains(contrahent.getCountry(), keyword)
                    || safeContains(contrahent.getCountryCode(), keyword)
                    || safeContains(contrahent.getCity(), keyword)
                    || safeContains(contrahent.getPostalCode(), keyword)
                    || safeContains(contrahent.getStreet(), keyword)
                    || safeContains(contrahent.getPhytosanitaryNumber(), keyword);
        });

        updateFilterSummary();
        if (table != null) {
            table.sort();
        }
    }

    private void updateFilterSummary() {
        if (filterStatusLabel == null || filterSummaryLabel == null || filteredData == null) {
            return;
        }

        String keyword = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim();
        long totalCount = masterData.size();
        long visibleCount = filteredData.size();
        long supplierCount = filteredData.stream().filter(Contrahent::isSupplier).count();
        long clientCount = filteredData.stream().filter(Contrahent::isClient).count();

        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("Łącznie kontrahentów: ").append(totalCount)
                .append(". Widoczne po filtrze: ").append(visibleCount)
                .append(". Dostawcy: ").append(supplierCount)
                .append(". Odbiorcy: ").append(clientCount).append('.');
        if (!keyword.isBlank()) {
            statusBuilder.append(UiTextUtil.buildQuotedFilterSuffix("Fraza", keyword));
        }
        filterStatusLabel.setText(statusBuilder.toString());

        Contrahent selected = table == null ? null : table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = filteredData.stream().findFirst().orElse(null);
        }

        if (selected == null) {
            filterSummaryLabel.setText("Brak kontrahentów spełniających bieżący filtr.");
            return;
        }

        filterSummaryLabel.setText(buildContrahentSummary(selected));
    }

    private String buildContrahentSummary(Contrahent contrahent) {
        return "Podgląd kontrahenta: " + firstNonBlank(safe(contrahent.getName()).trim(), "—")
                + ", kraj " + firstNonBlank(safe(contrahent.getCountry()).trim(), "—")
                + ", kod kraju " + firstNonBlank(safe(contrahent.getCountryCode()).trim(), "—")
                + ", miasto " + firstNonBlank(safe(contrahent.getCity()).trim(), "—")
                + ", numer fitosanitarny " + firstNonBlank(safe(contrahent.getPhytosanitaryNumber()).trim(), "—")
                + ", role " + buildRoleLabel(contrahent)
                + ".";
    }

    private String buildRoleLabel(Contrahent contrahent) {
        if (contrahent.isSupplier() && contrahent.isClient()) {
            return "dostawca i odbiorca";
        }
        if (contrahent.isSupplier()) {
            return "dostawca";
        }
        if (contrahent.isClient()) {
            return "odbiorca";
        }
        return "brak przypisanej roli";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean safeContains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void refresh() {
        masterData.setAll(contrahentService.getAllContrahents());
        applyFilters();
    }

    @FXML
    private void clearFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        applyFilters();
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
        refresh();
    }

    @FXML
    private void editContrahent() {
        Contrahent selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz kontrahenta do edycji.");
            return;
        }

        ModalViewUtil.openModal(
                "/view/contrahent_form.fxml",
                "Edytuj kontrahenta",
                860, 660,
                820, 620,
                (ContrahentFormController controller) -> controller.setContrahent(selected)
        );
        refresh();
    }

    @FXML
    private void deleteContrahent() {
        Contrahent selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz kontrahenta do usunięcia.");
            return;
        }

        if (!DialogUtil.confirmDelete(selected.getName())) {
            return;
        }

        try {
            contrahentService.deleteContrahent(selected.getId());
            refresh();
            DialogUtil.showSuccess("Kontrahent został usunięty.");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd usuwania", "Nie udało się usunąć kontrahenta.");
        }
    }
}
