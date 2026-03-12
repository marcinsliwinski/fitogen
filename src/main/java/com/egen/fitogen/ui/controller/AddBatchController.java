package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.domain.PlantBatch;
import com.egen.fitogen.service.PlantBatchService;
import javafx.fxml.FXML;
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
    private TextField plantId;

    @FXML
    private TextField contrahentId;

    private final PlantBatchService plantBatchService =
            AppContext.getPlantBatchService();

    @FXML
    private void saveBatch() {

        PlantBatch batch = new PlantBatch();

        batch.setInteriorBatchNo(batchNo.getText());
        batch.setQty(Integer.parseInt(qty.getText()));
        batch.setManufacturerCountryCode(country.getText());
        batch.setPlantId(Integer.parseInt(plantId.getText()));
        batch.setContrahentId(Integer.parseInt(contrahentId.getText()));

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