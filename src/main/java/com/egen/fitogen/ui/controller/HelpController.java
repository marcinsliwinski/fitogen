package com.egen.fitogen.ui.controller;

import com.egen.fitogen.ui.router.ViewManager;
import javafx.fxml.FXML;

public class HelpController {

    @FXML
    private void openDashboard() {
        ViewManager.show(ViewManager.DASHBOARD);
    }

    @FXML
    private void openSettings() {
        ViewManager.show(ViewManager.SETTINGS);
    }

    @FXML
    private void openDocuments() {
        ViewManager.show(ViewManager.DOCUMENTS);
    }

    @FXML
    private void openPlantBatches() {
        ViewManager.show(ViewManager.BATCHES);
    }
}
