package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.domain.PlantBatch;
import com.egen.fitogen.service.PlantBatchService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class PlantBatchController {

    @FXML
    private TableView<PlantBatch> table;

    @FXML
    private TableColumn<PlantBatch, Integer> colId;

    @FXML
    private TableColumn<PlantBatch, String> colBatch;

    @FXML
    private TableColumn<PlantBatch, Integer> colQty;

    @FXML
    private TableColumn<PlantBatch, String> colCountry;

    private final PlantBatchService plantBatchService =
            AppContext.getPlantBatchService();

    @FXML
    public void initialize() {

        colId.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        colBatch.setCellValueFactory(
                new PropertyValueFactory<>("interiorBatchNo")
        );

        colQty.setCellValueFactory(
                new PropertyValueFactory<>("qty")
        );

        colCountry.setCellValueFactory(
                new PropertyValueFactory<>("manufacturerCountryCode")
        );

        refresh();
    }

    private void refresh() {

        table.setItems(
                FXCollections.observableArrayList(
                        plantBatchService.getAllBatches()
                )
        );
    }

    @FXML
    private void addBatch() throws Exception {

        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("/view/add_batch.fxml"));

        Stage stage = new Stage();

        stage.setScene(new Scene(loader.load(), 400, 300));
        stage.setTitle("Dodaj partię");

        stage.showAndWait();

        refresh();   // ← KLUCZOWE
    }

    @FXML
    private void deleteBatch() {

        PlantBatch selected =
                table.getSelectionModel().getSelectedItem();

        if (selected == null) return;

        plantBatchService.deleteBatch(selected.getId());

        refresh();
    }
}