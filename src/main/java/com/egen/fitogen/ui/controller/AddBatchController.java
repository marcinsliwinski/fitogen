package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.domain.Contrahent;
import com.egen.fitogen.domain.Plant;
import com.egen.fitogen.domain.PlantBatch;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.repository.PlantRepository;
import com.egen.fitogen.service.PlantBatchService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;

public class AddBatchController {

    @FXML
    private TextField batchNo;

    @FXML
    private TextField qty;

    @FXML
    private TextField country;

    @FXML
    private ComboBox<Plant> plantBox;

    @FXML
    private ComboBox<Contrahent> contrahentBox;

    private final PlantBatchService plantBatchService =
            AppContext.getPlantBatchService();

    private final PlantRepository plantRepository =
            new com.egen.fitogen.database.SqlitePlantRepository();

    private final ContrahentRepository contrahentRepository =
            new com.egen.fitogen.database.SqliteContrahentRepository();

    @FXML
    public void initialize() {

        plantBox.setItems(
                FXCollections.observableArrayList(
                        plantRepository.findAll()
                )
        );

        contrahentBox.setItems(
                FXCollections.observableArrayList(
                        contrahentRepository.findAll()
                )
        );
    }

    @FXML
    private void saveBatch() {

        Plant plant = plantBox.getValue();
        Contrahent contrahent = contrahentBox.getValue();

        if (plant == null || contrahent == null) {
            return;
        }

        PlantBatch batch = new PlantBatch();

        batch.setInteriorBatchNo(batchNo.getText());
        batch.setQty(Integer.parseInt(qty.getText()));
        batch.setManufacturerCountryCode(country.getText());

        batch.setPlantId(plant.getId());
        batch.setContrahentId(contrahent.getId());

        batch.setCreationDate(LocalDate.now());

        plantBatchService.addBatch(batch);

        close();
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {

        Stage stage =
                (Stage) batchNo.getScene().getWindow();

        stage.close();
    }
}