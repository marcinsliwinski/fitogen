package com.egen.fitogen.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML
    private StackPane content;

    @FXML
    public void initialize() throws Exception {
        load("/view/dashboard.fxml");
    }

    @FXML
    private void openDashboard() throws Exception {
        load("/view/dashboard.fxml");
    }

    @FXML
    private void openPlantBatches() throws Exception {
        load("/view/plant_batches.fxml");
    }

    @FXML
    private void openDocuments() {
        System.out.println("Documents view");
    }

    @FXML
    private void openContrahents() {
        System.out.println("Contrahents view");
    }

    private void load(String path) throws Exception {

        Node view = FXMLLoader.load(
                getClass().getResource(path)
        );

        content.getChildren().setAll(view);
    }
}