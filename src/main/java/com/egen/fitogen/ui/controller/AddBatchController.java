package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.repository.PlantRepository;
import com.egen.fitogen.service.PlantBatchService;
import com.egen.fitogen.ui.util.DialogUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;

public class AddBatchController {

    @FXML private TextField batchNo;
    @FXML private TextField qty;
    @FXML private TextField country;
    @FXML private ComboBox<Plant> plantBox;
    @FXML private ComboBox<Contrahent> contrahentBox;

    private final PlantBatchService plantBatchService = AppContext.getPlantBatchService();
    private final PlantRepository plantRepository = new com.egen.fitogen.database.SqlitePlantRepository();
    private final ContrahentRepository contrahentRepository = new com.egen.fitogen.database.SqliteContrahentRepository();

    private Plant preselectedPlant;
    private Runnable onBatchSaved;

    @FXML
    public void initialize() {
        plantBox.setItems(FXCollections.observableArrayList(plantRepository.findAll()));
        contrahentBox.setItems(FXCollections.observableArrayList(contrahentRepository.findAll()));

        if (preselectedPlant != null) {
            plantBox.setValue(preselectedPlant);
        }
    }

    public void setPreselectedPlant(Plant plant) {
        this.preselectedPlant = plant;
        if (plantBox != null) {
            plantBox.setValue(plant);
        }
    }

    public void setOnBatchSaved(Runnable onBatchSaved) {
        this.onBatchSaved = onBatchSaved;
    }

    @FXML
    private void saveBatch() {
        Plant plant = plantBox.getValue();
        Contrahent contrahent = contrahentBox.getValue();

        if (plant == null) {
            DialogUtil.showWarning("Brak danych", "Wybierz roślinę.");
            return;
        }
        if (qty.getText() == null || qty.getText().isBlank()) {
            DialogUtil.showWarning("Brak danych", "Uzupełnij ilość.");
            return;
        }

        int parsedQty;
        try {
            parsedQty = Integer.parseInt(qty.getText().trim());
            if (parsedQty <= 0) {
                DialogUtil.showWarning("Błędna ilość", "Ilość musi być większa od zera.");
                return;
            }
        } catch (NumberFormatException e) {
            DialogUtil.showWarning("Błędna ilość", "Ilość musi być liczbą całkowitą.");
            return;
        }

        PlantBatch batch = new PlantBatch();
        batch.setInteriorBatchNo(batchNo.getText());
        batch.setQty(parsedQty);
        batch.setManufacturerCountryCode(country.getText());
        batch.setPlantId(plant.getId());
        batch.setContrahentId(contrahent != null ? contrahent.getId() : 0);
        batch.setCreationDate(LocalDate.now());

        plantBatchService.addBatch(batch);
        if (onBatchSaved != null) {
            onBatchSaved.run();
        }
        close();
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) batchNo.getScene().getWindow();
        stage.close();
    }
}
