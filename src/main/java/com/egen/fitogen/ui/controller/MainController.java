package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.ui.router.ViewManager;
import java.awt.Desktop;
import java.net.URI;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController {

    private static MainController instance;

    @FXML
    private StackPane content;

    @FXML
    private VBox newsFeedBox;

    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();

    public static void requestRefreshSystemNews() {
        if (instance != null) {
            instance.refreshNewsFeed();
        }
    }

    @FXML
    public void initialize() {
        instance = this;
        if (newsFeedBox != null) {
            newsFeedBox.managedProperty().bind(newsFeedBox.visibleProperty());
        }
        ViewManager.setContainer(content);
        ViewManager.show(ViewManager.DASHBOARD);
        refreshNewsFeed();
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

    @FXML
    private void openImprovement() {
        navigate(ViewManager.IMPROVEMENT);
    }

    @FXML
    private void openEgenLabsWebsite() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("https://www.egenlabs.eu"));
            }
        } catch (Exception ignored) {
        }
    }

    private void navigate(String viewKey) {
        ViewManager.show(viewKey);
        refreshNewsFeed();
    }

    private void refreshNewsFeed() {
        if (newsFeedBox == null) {
            return;
        }

        newsFeedBox.getChildren().clear();

        if (appSettingsService != null && !appSettingsService.isIssuerProfileComplete()) {
            newsFeedBox.getChildren().add(buildNewsFeedItem(
                    "Uzupełnij dane podmiotu",
                    "Profil szkółki jest niekompletny. Uzupełnij dane podmiotu, aby dokumenty i wydruki korzystały z pełnych danych.",
                    "Przejdź do danych podmiotu",
                    "news-feed-warning",
                    () -> {
                        SettingsController.requestInitialTab("Dane podmiotu");
                        navigate(ViewManager.SETTINGS);
                    }
            ));
        }

        if (DatabaseConfig.isTestDatabase(DatabaseConfig.getDatabaseFilePath())) {
            newsFeedBox.getChildren().add(buildNewsFeedItem(
                    "Pracujesz na wersji testowej",
                    "To jest profil testowy bazy danych. Załóż nowy profil roboczy, aby oddzielić dane testowe od produkcyjnych.",
                    "Załóż nowy profil",
                    "news-feed-info",
                    () -> {
                        SettingsController.requestCreateNewDatabaseFlow();
                        navigate(ViewManager.SETTINGS);
                    }
            ));
        }

        newsFeedBox.setVisible(!newsFeedBox.getChildren().isEmpty());
    }

    private HBox buildNewsFeedItem(
            String title,
            String message,
            String actionLabel,
            String styleClass,
            Runnable action
    ) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("news-feed-title");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("news-feed-message");

        VBox textBox = new VBox(4, titleLabel, messageLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button actionButton = new Button(actionLabel);
        actionButton.getStyleClass().add("button-secondary");
        actionButton.setOnAction(event -> action.run());

        HBox item = new HBox(16, textBox, actionButton);
        item.getStyleClass().addAll("news-feed-item", styleClass);
        item.setPadding(new Insets(14, 16, 14, 16));
        return item;
    }
}
