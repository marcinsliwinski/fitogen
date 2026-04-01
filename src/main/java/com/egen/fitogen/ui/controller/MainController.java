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
        ViewManager.show(ViewManager.DASHBOARD);
    }

    @FXML
    private void openDashboard() {
        navigate(ViewManager.DASHBOARD);
    }

    @FXML
    private void openPlantBatches() {
        navigate(ViewManager.BATCHES);
    }

    @FXML
    private void openPlants() {
        navigate(ViewManager.PLANTS);
    }

    @FXML
    private void openContrahents() {
        navigate(ViewManager.CONTRAHENTS);
    }

    @FXML
    private void openDocuments() {
        navigate(ViewManager.DOCUMENTS);
    }

    @FXML
    private void openEppoAdmin() {
        navigate(ViewManager.EPPO_ADMIN);
    }

    @FXML
    private void openSettings() {
        navigate(ViewManager.SETTINGS);
    }

    @FXML
    private void openUpdates() {
        navigate(ViewManager.UPDATES);
    }

    @FXML
    private void openHelp() {
        navigate(ViewManager.HELP);
    }

    private void navigate(String viewKey) {
        ViewManager.show(viewKey);
    }
}