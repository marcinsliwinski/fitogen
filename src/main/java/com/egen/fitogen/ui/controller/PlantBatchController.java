package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.database.SqliteContrahentRepository;
import com.egen.fitogen.database.SqlitePlantRepository;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.model.PlantBatchStatus;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.repository.PlantRepository;
import com.egen.fitogen.service.PlantBatchService;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.egen.fitogen.ui.util.UiTextUtil;

public class PlantBatchController {

    @FXML private TableView<PlantBatch> table;
    @FXML private TableColumn<PlantBatch, Integer> colId;
    @FXML private TableColumn<PlantBatch, String> colPlant;
    @FXML private TableColumn<PlantBatch, String> colInteriorBatchNo;
    @FXML private TableColumn<PlantBatch, String> colExteriorBatchNo;
    @FXML private TableColumn<PlantBatch, Integer> colQty;
    @FXML private TableColumn<PlantBatch, String> colCreationDate;
    @FXML private TableColumn<PlantBatch, String> colSourceOrigin;
    @FXML private TableColumn<PlantBatch, String> colStatus;
    @FXML private TextField searchField;
    @FXML private Label filterStatusLabel;
    @FXML private Label filterSummaryLabel;

    private final PlantBatchService plantBatchService = AppContext.getPlantBatchService();
    private final PlantRepository plantRepository = new SqlitePlantRepository();
    private final ContrahentRepository contrahentRepository = new SqliteContrahentRepository();

    private final ObservableList<PlantBatch> masterData = FXCollections.observableArrayList();
    private FilteredList<PlantBatch> filteredData;
    private final Map<Integer, String> plantNames = new HashMap<>();
    private final Map<Integer, String> contrahentNames = new HashMap<>();

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
        colPlant.setCellValueFactory(cell ->
                new SimpleStringProperty(buildPlantLabel(cell.getValue().getPlantId()))
        );
        colInteriorBatchNo.setCellValueFactory(new PropertyValueFactory<>("interiorBatchNo"));
        colExteriorBatchNo.setCellValueFactory(new PropertyValueFactory<>("exteriorBatchNo"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colCreationDate.setCellValueFactory(cell ->
                new SimpleStringProperty(formatDate(cell.getValue().getCreationDate()))
        );
        colSourceOrigin.setCellValueFactory(cell ->
                new SimpleStringProperty(getSourceOriginLabel(cell.getValue()))
        );
        colStatus.setCellValueFactory(cell ->
                new SimpleStringProperty(formatStatus(cell.getValue().getStatus()))
        );
    }


    private void configureTableBehavior() {
        if (table == null) {
            return;
        }

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Brak partii roślin do wyświetlenia."));
    }

    private void configureRowFactory() {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(PlantBatch item, boolean empty) {
                super.updateItem(item, empty);

                getStyleClass().remove("cancelled-row");

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                if (item.getStatus() == PlantBatchStatus.CANCELLED) {
                    getStyleClass().add("cancelled-row");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void configureSearch() {
        filteredData = new FilteredList<>(masterData, b -> true);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        if (table != null) {
            table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateFilterSummary());
        }

        SortedList<PlantBatch> sorted = new SortedList<>(filteredData);
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

        filteredData.setPredicate(batch -> {
            if (keyword.isBlank()) {
                return true;
            }

            return contains(buildPlantLabel(batch.getPlantId()), keyword)
                    || contains(batch.getInteriorBatchNo(), keyword)
                    || contains(batch.getExteriorBatchNo(), keyword)
                    || contains(getSourceOriginLabel(batch), keyword)
                    || contains(batch.getManufacturerCountryCode(), keyword)
                    || contains(batch.getFitoQualificationCategory(), keyword)
                    || contains(batch.getEppoCode(), keyword)
                    || contains(batch.getZpZone(), keyword)
                    || contains(batch.getComments(), keyword)
                    || contains(formatStatus(batch.getStatus()), keyword)
                    || contains(batch.isInternalSource() ? "wewnętrzne" : "zewnętrzne", keyword)
                    || String.valueOf(batch.getQty()).contains(keyword)
                    || contains(formatDate(batch.getCreationDate()), keyword);
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
        long activeCount = filteredData.stream()
                .filter(batch -> batch.getStatus() == null || batch.getStatus() == PlantBatchStatus.ACTIVE)
                .count();
        long cancelledCount = filteredData.stream()
                .filter(batch -> batch.getStatus() == PlantBatchStatus.CANCELLED)
                .count();

        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("Łącznie partii: ").append(totalCount)
                .append(". Widoczne po filtrze: ").append(visibleCount)
                .append(". Aktywne: ").append(activeCount)
                .append(". Anulowane: ").append(cancelledCount).append('.');
        if (!keyword.isBlank()) {
            statusBuilder.append(UiTextUtil.buildQuotedFilterSuffix("Fraza", keyword));
        }
        filterStatusLabel.setText(statusBuilder.toString());

        PlantBatch selected = table == null ? null : table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = filteredData.stream().findFirst().orElse(null);
        }

        if (selected == null) {
            filterSummaryLabel.setText("Brak partii roślin spełniających bieżący filtr.");
            return;
        }

        filterSummaryLabel.setText(buildBatchSummary(selected));
    }

    private String buildBatchSummary(PlantBatch batch) {
        return "Podgląd partii: roślina " + safe(buildPlantLabel(batch.getPlantId()))
                + ", nr wewnętrzny " + safe(batch.getInteriorBatchNo())
                + ", nr zewnętrzny " + safe(batch.getExteriorBatchNo())
                + ", źródło " + safe(getSourceOriginLabel(batch))
                + ", status " + safe(formatStatus(batch.getStatus()))
                + ", ilość " + batch.getQty()
                + ".";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void refresh() {
        loadPlantNames();
        loadContrahentNames();
        masterData.setAll(plantBatchService.getAllBatches());
        applyFilters();
    }

    private void loadPlantNames() {
        plantNames.clear();
        List<Plant> plants = plantRepository.findAll();
        for (Plant plant : plants) {
            plantNames.put(plant.getId(), buildPlantName(plant));
        }
    }

    private void loadContrahentNames() {
        contrahentNames.clear();
        List<Contrahent> contrahents = contrahentRepository.findAll();
        for (Contrahent contrahent : contrahents) {
            contrahentNames.put(contrahent.getId(), contrahent.getName());
        }
    }

    private String buildPlantName(Plant plant) {
        StringBuilder sb = new StringBuilder();

        if (plant.getSpecies() != null && !plant.getSpecies().isBlank()) {
            sb.append(plant.getSpecies());
        }
        if (plant.getVariety() != null && !plant.getVariety().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" / ");
            }
            sb.append(plant.getVariety());
        }
        if (plant.getRootstock() != null && !plant.getRootstock().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" / ");
            }
            sb.append(plant.getRootstock());
        }

        return sb.isEmpty() ? "Roślina ID: " + plant.getId() : sb.toString();
    }

    private String buildPlantLabel(int plantId) {
        return plantNames.getOrDefault(plantId, "Roślina ID: " + plantId);
    }

    private String getSourceOriginLabel(PlantBatch batch) {
        if (batch.isInternalSource()) {
            return "Wewnętrzne (dane podmiotu)";
        }
        return contrahentNames.getOrDefault(batch.getContrahentId(), "");
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : "";
    }

    private String formatStatus(PlantBatchStatus status) {
        if (status == null) {
            return "Aktywna";
        }

        return switch (status) {
            case ACTIVE -> "Aktywna";
            case CANCELLED -> "Anulowana";
        };
    }

    @FXML
    private void clearFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        applyFilters();
    }

    @FXML
    private void addBatch() {
        ModalViewUtil.openModal(
                "/view/plant_batch_form.fxml",
                "Dodaj partię roślin",
                900, 760,
                860, 720,
                (PlantBatchFormController controller) -> {
                }
        );
        refresh();
    }

    @FXML
    private void editBatch() {
        PlantBatch selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz partię roślin do edycji.");
            return;
        }

        if (selected.getStatus() == PlantBatchStatus.CANCELLED) {
            DialogUtil.showWarning(
                    "Partia anulowana",
                    "Anulowana partia roślin nie może być edytowana."
            );
            return;
        }

        ModalViewUtil.openModal(
                "/view/plant_batch_form.fxml",
                "Edytuj partię roślin",
                900, 760,
                860, 720,
                (PlantBatchFormController controller) -> controller.setPlantBatch(selected)
        );
        refresh();
    }

    @FXML
    private void deleteBatch() {
        PlantBatch selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz partię roślin do anulowania.");
            return;
        }

        if (selected.getStatus() == PlantBatchStatus.CANCELLED) {
            DialogUtil.showWarning(
                    "Partia już anulowana",
                    "Wybrana partia została już wcześniej anulowana."
            );
            return;
        }

        String label = buildPlantLabel(selected.getPlantId());

        if (!DialogUtil.confirmDelete(label.isBlank() ? "wybraną partię" : label)) {
            return;
        }

        try {
            plantBatchService.deleteBatch(selected.getId());
            refresh();
            DialogUtil.showSuccess("Partia roślin została anulowana.");
        } catch (IllegalStateException e) {
            DialogUtil.showWarning("Nie można anulować partii", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd anulowania", "Nie udało się anulować partii roślin.");
        }
    }
}
