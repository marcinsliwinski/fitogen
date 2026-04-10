package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.ui.router.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class MainController {

    private static MainController instance;

    @FXML
    private StackPane content;

    @FXML
    private HBox systemNewsBar;

    @FXML
    private Label systemNewsLabel;

    @FXML
    private Button systemNewsActionButton;

    @FXML
    public void initialize() {
        instance = this;
        ViewManager.setContainer(content);
        ViewManager.show(ViewManager.DASHBOARD);
        refreshSystemNews();
    }

    public static void requestRefreshSystemNews() {
        if (instance != null) {
            instance.refreshSystemNews();
        }
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
        refreshSystemNews();
    }

    private void refreshSystemNews() {
        if (systemNewsBar == null || systemNewsLabel == null || systemNewsActionButton == null) {
            return;
        }

        String updateMessage = AppContext.getAppSettingsService() == null
                ? ""
                : AppContext.getAppSettingsService().getSetting("updates.news_feed_message");

        clearNewsBarStyles();

        if (updateMessage != null && !updateMessage.isBlank()) {
            applyNewsBar(
                    updateMessage,
                    "Aktualizacje",
                    "news-feed-info",
                    () -> navigate(ViewManager.UPDATES)
            );
            return;
        }

        AppSettingsService appSettingsService = AppContext.getAppSettingsService();
        if (DatabaseConfig.isUsingTestDatabase()) {
            applyNewsBar(
                    "Pracujesz na wersji testowej. Załóż nowy profil, aby rozpocząć pracę na własnej bazie.",
                    "Nowa baza",
                    "news-feed-warning",
                    () -> {
                        SettingsController.requestOpenDatabaseManagement();
                        navigate(ViewManager.SETTINGS);
                    }
            );
            return;
        }

        if (appSettingsService != null && !appSettingsService.isIssuerProfileComplete()) {
            applyNewsBar(
                    "Uzupełnij dane podmiotu, aby dokumenty, preview i PDF korzystały z kompletnych danych szkółki.",
                    "Uzupełnij dane",
                    "news-feed-warning",
                    () -> {
                        SettingsController.requestOpenIssuerProfile();
                        navigate(ViewManager.SETTINGS);
                    }
            );
            return;
        }

        systemNewsBar.setManaged(false);
        systemNewsBar.setVisible(false);
        systemNewsActionButton.setOnAction(null);
    }

    private void applyNewsBar(String message, String actionLabel, String styleClass, Runnable action) {
        systemNewsLabel.setText(message);
        systemNewsActionButton.setText(actionLabel);
        systemNewsActionButton.setOnAction(event -> action.run());
        systemNewsBar.getStyleClass().add(styleClass);
        systemNewsBar.setManaged(true);
        systemNewsBar.setVisible(true);
    }

    private void clearNewsBarStyles() {
        systemNewsBar.getStyleClass().removeAll("news-feed-info", "news-feed-warning", "news-feed-success");
    }
}
