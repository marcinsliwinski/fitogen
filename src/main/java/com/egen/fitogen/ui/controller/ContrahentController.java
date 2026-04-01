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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class ContrahentController {

    @FXML private TableView<Contrahent> table;
    @FXML private TableColumn<Contrahent, Integer> colId;
    @FXML private TableColumn<Contrahent, String> colName;
    @FXML private TableColumn<Contrahent, String> colCountry;
    @FXML private TableColumn<Contrahent, String> colCity;
    @FXML private TableColumn<Contrahent, String> colPhyto;
    @FXML private TextField searchField;

    private final ContrahentService contrahentService = AppContext.getContrahentService();

    private final ObservableList<Contrahent> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configureColumns();
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

    private void configureSearch() {
        FilteredList<Contrahent> filtered = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String keyword = newVal == null ? "" : newVal.trim().toLowerCase();

            filtered.setPredicate(contrahent -> {
                if (keyword.isBlank()) {
                    return true;
                }

                return safeContains(contrahent.getName(), keyword)
                        || safeContains(contrahent.getCountry(), keyword)
                        || safeContains(contrahent.getCity(), keyword)
                        || safeContains(contrahent.getPhytosanitaryNumber(), keyword);
            });
        });

        SortedList<Contrahent> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
    }

    private boolean safeContains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void refresh() {
        masterData.setAll(contrahentService.getAllContrahents());
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