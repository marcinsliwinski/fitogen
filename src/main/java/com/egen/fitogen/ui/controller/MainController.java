package com.egen.fitogen.ui.controller;

import com.egen.fitogen.ui.router.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML
    private StackPane content;

    @FXML
    public void initialize() {

        ViewManager.setContainer(content);

        ViewManager.show("dashboard");

    }

    @FXML
    private void openDashboard() {

        ViewManager.show("dashboard");

    }

    @FXML
    private void openPlantBatches() {

        ViewManager.show("batches");

    }

    @FXML
    private void openPlants() {

        System.out.println("Plants view not implemented yet");

    }

    @FXML
    private void openContrahents() {

        System.out.println("Contrahents view not implemented yet");

    }

    @FXML
    private void openDocuments() {

        System.out.println("Documents view not implemented yet");

    }

    @FXML
    private void openSettings() {

        System.out.println("Settings view");

    }

    @FXML
    private void openUpdates() {

        System.out.println("Updates view");

    }

    @FXML
    private void openHelp() {

        System.out.println("Help view");

    }

}