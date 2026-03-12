package com.egen.fitogen.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DashboardController {

    @FXML
    private void openPlantBatches() throws Exception{
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/plant_batches.fxml")
        );
    }

    @FXML
    private void openDocuments() {
        System.out.println("Documents clicked");
    }

    @FXML
    private void openContrahents() {
        System.out.println("Contrahents clicked");
    }
}