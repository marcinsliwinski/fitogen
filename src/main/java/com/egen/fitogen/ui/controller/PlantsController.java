package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.PlantService;
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
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class PlantsController {

    @FXML private TableView<Plant> table;
    @FXML private TableColumn<Plant, Integer> colId;
    @FXML private TableColumn<Plant, String> colSpecies;
    @FXML private TableColumn<Plant, String> colVariety;
    @FXML private TableColumn<Plant, String> colRootstock;
    @FXML private TableColumn<Plant, String> colLatin;
    @FXML private TableColumn<Plant, String> colEppo;
    @FXML private TableColumn<Plant, String> colPassportRequired;
    @FXML private TableColumn<Plant, String> colVisibility;
    @FXML private TextField searchField;
    @FXML private Label catalogModeInfoLabel;

    private final PlantService plantService = AppContext.getPlantService();
    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();

    private final ObservableList<Plant> masterData = FXCollections.observableArrayList();
    private FilteredList<Plant> filteredData;

    @FXML
    public void initialize() {
        configureColumns();
        configureRowFactory();
        configureSearch();
        refresh();
    }

    private void configureColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSpecies.setCellValueFactory(new PropertyValueFactory<>("species"));
        colVariety.setCellValueFactory(new PropertyValueFactory<>("variety"));
        colRootstock.setCellValueFactory(new PropertyValueFactory<>("rootstock"));
        colLatin.setCellValueFactory(new PropertyValueFactory<>("latinSpeciesName"));
        colEppo.setCellValueFactory(new PropertyValueFactory<>("eppoCode"));
        colPassportRequired.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().isPassportRequired() ? "Tak" : "Nie")
        );
        colVisibility.setCellValueFactory(new PropertyValueFactory<>("visibilityStatus"));
    }


    private void configureRowFactory() {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Plant item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("inactive-row");

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                if (isUnusedPlant(item)) {
                    getStyleClass().add("inactive-row");
                }
            }
        });
    }

    private void configureSearch() {
        filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        SortedList<Plant> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        boolean fullCatalogEnabled = appSettingsService.isPlantFullCatalogEnabled();

        filteredData.setPredicate(plant -> {
            if (plant == null) {
                return false;
            }

            if (!fullCatalogEnabled && isUnusedPlant(plant)) {
                return false;
            }

            if (keyword.isBlank()) {
                return true;
            }

            return safeContains(plant.getSpecies(), keyword)
                    || safeContains(plant.getVariety(), keyword)
                    || safeContains(plant.getRootstock(), keyword)
                    || safeContains(plant.getLatinSpeciesName(), keyword)
                    || safeContains(plant.getEppoCode(), keyword)
                    || safeContains(plant.isPassportRequired() ? "tak" : "nie", keyword)
                    || safeContains(plant.getVisibilityStatus(), keyword);
        });
    }

    private boolean isUnusedPlant(Plant plant) {
        return plant.getVisibilityStatus() != null
                && plant.getVisibilityStatus().trim().equalsIgnoreCase("Nieużywany");
    }

    private boolean safeContains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void refresh() {
        masterData.setAll(plantService.getAllPlants());
        updateCatalogModeInfo();
        applyFilters();
    }

    private void updateCatalogModeInfo() {
        boolean enabled = appSettingsService.isPlantFullCatalogEnabled();
        if (catalogModeInfoLabel != null) {
            catalogModeInfoLabel.setText(
                    enabled
                            ? "Tryb pełnej bazy roślin jest włączony — lista pokazuje także pozycje nieużywane."
                            : "Tryb pełnej bazy roślin jest wyłączony — lista ukrywa pozycje oznaczone jako „Nieużywany”."
            );
        }
    }

    private String buildPlantLabel(Plant plant) {
        if (plant == null) {
            return "";
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

        return sb.isEmpty() ? "wybraną roślinę" : sb.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @FXML
    private void addPlant() {
        ModalViewUtil.openModal(
                "/view/plant_form.fxml",
                "Dodaj roślinę",
                860, 760,
                820, 700,
                (PlantFormController controller) -> controller.setPlantService(plantService)
        );
        refresh();
    }

    @FXML
    private void editPlant() {
        Plant selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz roślinę do edycji.");
            return;
        }

        ModalViewUtil.openModal(
                "/view/plant_form.fxml",
                "Edytuj roślinę",
                860, 760,
                820, 700,
                (PlantFormController controller) -> {
                    controller.setPlantService(plantService);
                    controller.setPlant(selected);
                }
        );
        refresh();
    }

    @FXML
    private void deletePlant() {
        Plant selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz roślinę do usunięcia.");
            return;
        }

        if (!DialogUtil.confirmDelete(buildPlantLabel(selected))) {
            return;
        }

        try {
            plantService.deletePlant(selected.getId());
            refresh();
            DialogUtil.showSuccess("Roślina została usunięta.");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd usuwania", "Nie udało się usunąć rośliny.");
        }
    }
}