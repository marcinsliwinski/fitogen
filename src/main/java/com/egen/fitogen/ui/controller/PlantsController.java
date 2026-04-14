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
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import com.egen.fitogen.ui.util.UiTextUtil;

public class PlantsController {

    @FXML private TableView<Plant> table;
    @FXML private TableColumn<Plant, Integer> colId;
    @FXML private TableColumn<Plant, String> colSpecies;
    @FXML private TableColumn<Plant, String> colVariety;
    @FXML private TableColumn<Plant, String> colRootstock;
    @FXML private TableColumn<Plant, String> colLatin;
    @FXML private TableColumn<Plant, String> colEppo;
    @FXML private TableColumn<Plant, String> colPassportRequired;
    @FXML private TextField searchField;
    @FXML private Label catalogModeInfoLabel;
    @FXML private Label globalPassportInfoLabel;
    @FXML private Label filterStatusLabel;
    @FXML private Label filterSummaryLabel;

    private final PlantService plantService = AppContext.getPlantService();
    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();

    private final ObservableList<Plant> masterData = FXCollections.observableArrayList();
    private FilteredList<Plant> filteredData;

    @FXML
    public void initialize() {
        configureColumns();
        configureTableBehavior();
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
                new SimpleStringProperty(resolvePassportColumnText(cell.getValue()))
        );
    }



    private void configureTableBehavior() {
        if (table == null) {
            return;
        }

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Brak roślin do wyświetlenia."));
    }
    private void configureRowFactory() {
        table.setRowFactory(tv -> {
            TableRow<Plant> row = new TableRow<>() {
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
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    table.getSelectionModel().select(row.getItem());
                    editPlant();
                }
            });

            return row;
        });
    }

    private void configureSearch() {
        filteredData = new FilteredList<>(masterData, p -> true);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        if (table != null) {
            table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateFilterSummary());
        }

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
                    || safeContains(resolvePassportColumnText(plant), keyword)
                    || safeContains(plant.getVisibilityStatus(), keyword);
        });

        updateFilterSummary();
    }


    private void updateFilterSummary() {
        if (filterStatusLabel == null || filterSummaryLabel == null || filteredData == null) {
            return;
        }

        String keyword = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim();
        long totalCount = masterData.size();
        long visibleCount = filteredData.size();
        boolean fullCatalogEnabled = appSettingsService.isPlantFullCatalogEnabled();
        long usedCount = filteredData.stream().filter(plant -> !isUnusedPlant(plant)).count();
        long unusedCount = filteredData.stream().filter(this::isUnusedPlant).count();
        boolean passportRequiredForAll = appSettingsService.isPlantPassportRequiredForAll();
        long passportRequiredCount = passportRequiredForAll
                ? filteredData.size()
                : filteredData.stream().filter(Plant::isPassportRequired).count();

        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("Łącznie roślin: ").append(totalCount)
                .append(". Widoczne po filtrach: ").append(visibleCount)
                .append(". Używane: ").append(usedCount)
                .append(". Nieużywane: ").append(unusedCount)
                .append(". Wymagają paszportu: ").append(passportRequiredCount)
                .append(passportRequiredForAll ? " (ustawienie globalne)" : "")
                .append(". Tryb pełnej bazy: ").append(fullCatalogEnabled ? "włączony" : "wyłączony").append('.');
        if (!keyword.isBlank()) {
            statusBuilder.append(UiTextUtil.buildQuotedFilterSuffix("Fraza", keyword));
        }
        filterStatusLabel.setText(statusBuilder.toString());

        Plant selected = table == null ? null : table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = filteredData.stream().findFirst().orElse(null);
        }

        if (selected == null) {
            filterSummaryLabel.setText("Brak roślin spełniających bieżące filtry.");
            return;
        }

        filterSummaryLabel.setText(buildPlantSummary(selected));
    }

    private String buildPlantSummary(Plant plant) {
        return "Podgląd rośliny: " + buildPlantLabel(plant)
                + ", nazwa łacińska " + firstNonBlank(safe(plant.getLatinSpeciesName()).trim(), "—")
                + ", kod EPPO " + firstNonBlank(safe(plant.getEppoCode()).trim(), "—")
                + ", paszport " + resolvePassportSummaryText(plant)
                + ", status indeksu " + firstNonBlank(safe(plant.getVisibilityStatus()).trim(), "—")
                + ".";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
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

        boolean passportRequiredForAll = appSettingsService.isPlantPassportRequiredForAll();
        if (globalPassportInfoLabel != null) {
            globalPassportInfoLabel.setVisible(passportRequiredForAll);
            globalPassportInfoLabel.setManaged(passportRequiredForAll);
            globalPassportInfoLabel.setText(
                    passportRequiredForAll
                            ? "Globalne ustawienie wymusza paszport dla wszystkich roślin — kolumna Paszport pokazuje stan wynikający z ustawień systemu, niezależnie od lokalnej wartości zapisanej w rekordzie."
                            : ""
            );
        }
    }


    private String resolvePassportColumnText(Plant plant) {
        if (plant == null) {
            return "";
        }

        if (appSettingsService.isPlantPassportRequiredForAll()) {
            return "Tak (globalnie)";
        }

        return plant.isPassportRequired() ? "Tak" : "Nie";
    }

    private String resolvePassportSummaryText(Plant plant) {
        if (plant == null) {
            return "niewymagany";
        }

        if (appSettingsService.isPlantPassportRequiredForAll()) {
            return "wymagany globalnie";
        }

        return plant.isPassportRequired() ? "wymagany" : "niewymagany";
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
    private void clearFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        applyFilters();
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
