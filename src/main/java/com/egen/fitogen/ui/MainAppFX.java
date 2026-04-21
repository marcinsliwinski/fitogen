package com.egen.fitogen.ui;

import atlantafx.base.theme.PrimerLight;
import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.database.DatabaseInitializer;
import com.egen.fitogen.service.BootstrapStarterPackService;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ProgressDialogUtil;
import com.egen.fitogen.ui.util.WindowSizingUtil;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class MainAppFX extends Application {

    private static boolean pendingFg1BootstrapAfterStartup;

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        if (!ensureConfiguredDatabaseAvailable()) {
            stage.close();
            return;
        }

        DatabaseInitializer.initDatabase();
        AppContext.init();

        if (pendingFg1BootstrapAfterStartup) {
            runPendingStarterPackBootstrapWithProgress(stage);
            return;
        }

        showMainStage(stage);
    }

    private boolean ensureConfiguredDatabaseAvailable() {
        Optional<Path> missingRememberedDatabase = DatabaseConfig.getMissingRememberedDatabasePath();
        Optional<Path> rememberedBackup = DatabaseConfig.getRememberedBackupPath();

        if (missingRememberedDatabase.isPresent()) {
            return handleMissingRememberedDatabase(missingRememberedDatabase.get(), rememberedBackup);
        }

        Path configuredDatabasePath = DatabaseConfig.getDatabaseFilePath();
        if (configuredDatabasePath != null && Files.exists(configuredDatabasePath)) {
            return true;
        }

        return handleMissingActiveDatabase(configuredDatabasePath, rememberedBackup);
    }

    private boolean handleMissingRememberedDatabase(Path missingDatabasePath, Optional<Path> rememberedBackup) {
        boolean fg1Available = new BootstrapStarterPackService().isFg1PackageAvailable();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Brak ostatnio używanej bazy");
        alert.setHeaderText("Nie znaleziono ostatnio używanej bazy danych.");
        alert.setContentText(
                "Zapamiętana baza danych nie istnieje już w zapisanej lokalizacji:\n"
                        + missingDatabasePath
                        + "\n\nMożesz utworzyć nową bazę, zasilić ją pakietem FG1 albo odzyskać dane z ostatniej kopii zapasowej."
        );

        ButtonType createFg1Button = new ButtonType("Utwórz i zasil FG1");
        ButtonType createEmptyButton = new ButtonType("Utwórz pustą bazę");
        ButtonType restoreButton = new ButtonType("Odzyskaj z backupu");
        ButtonType useFallbackButton = new ButtonType("Użyj innej bazy");
        ButtonType cancelButton = new ButtonType("Zamknij", ButtonBar.ButtonData.CANCEL_CLOSE);

        if (rememberedBackup.isPresent()) {
            alert.getButtonTypes().setAll(fg1Available
                    ? new ButtonType[]{createFg1Button, createEmptyButton, restoreButton, useFallbackButton, cancelButton}
                    : new ButtonType[]{createEmptyButton, restoreButton, useFallbackButton, cancelButton});
        } else {
            alert.getButtonTypes().setAll(fg1Available
                    ? new ButtonType[]{createFg1Button, createEmptyButton, useFallbackButton, cancelButton}
                    : new ButtonType[]{createEmptyButton, useFallbackButton, cancelButton});
        }

        configureStartupAlert(alert);
        ButtonType result = alert.showAndWait().orElse(cancelButton);
        String missingName = missingDatabasePath.getFileName() == null
                ? "nowa_baza"
                : missingDatabasePath.getFileName().toString().replaceFirst("\\.db$", "");

        if (fg1Available && result == createFg1Button) {
            DatabaseConfig.createDatabaseProfile(missingName);
            pendingFg1BootstrapAfterStartup = true;
            return true;
        }
        if (result == createEmptyButton) {
            DatabaseConfig.createDatabaseProfile(missingName);
            return true;
        }
        if (rememberedBackup.isPresent() && result == restoreButton) {
            DatabaseConfig.restoreDatabaseFromBackup(rememberedBackup.get(), missingName);
            return true;
        }
        if (result == useFallbackButton) {
            DatabaseConfig.clearMissingRememberedDatabase();
            return true;
        }
        return false;
    }

    private boolean handleMissingActiveDatabase(Path configuredDatabasePath, Optional<Path> rememberedBackup) {
        boolean fg1Available = new BootstrapStarterPackService().isFg1PackageAvailable();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Brak bazy danych");
        alert.setHeaderText("Nie znaleziono aktywnej bazy danych.");
        String content = "Aktywna baza danych nie istnieje już w zapisanej lokalizacji:\n"
                + configuredDatabasePath
                + "\n\nMożesz utworzyć nową bazę danych";
        if (fg1Available) {
            content += ", od razu zasilić ją pakietem FG1";
        }
        if (rememberedBackup.isPresent()) {
            content += " lub odzyskać dane z ostatniej kopii zapasowej.";
        } else {
            content += ".";
        }
        alert.setContentText(content);

        ButtonType createFg1Button = new ButtonType("Utwórz i zasil FG1");
        ButtonType createEmptyButton = new ButtonType("Utwórz pustą bazę");
        ButtonType restoreButton = new ButtonType("Odzyskaj z backupu");
        ButtonType cancelButton = new ButtonType("Zamknij", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(rememberedBackup.isPresent()
                ? (fg1Available
                ? new ButtonType[]{createFg1Button, createEmptyButton, restoreButton, cancelButton}
                : new ButtonType[]{createEmptyButton, restoreButton, cancelButton})
                : (fg1Available
                ? new ButtonType[]{createFg1Button, createEmptyButton, cancelButton}
                : new ButtonType[]{createEmptyButton, cancelButton}));

        configureStartupAlert(alert);
        ButtonType result = alert.showAndWait().orElse(cancelButton);
        String targetName = configuredDatabasePath == null || configuredDatabasePath.getFileName() == null
                ? "fitogen"
                : configuredDatabasePath.getFileName().toString().replaceFirst("\\.db$", "");

        if (fg1Available && result == createFg1Button) {
            DatabaseConfig.createDatabaseProfile(targetName);
            pendingFg1BootstrapAfterStartup = true;
            return true;
        }
        if (result == createEmptyButton) {
            DatabaseConfig.createDatabaseProfile(targetName);
            return true;
        }
        if (rememberedBackup.isPresent() && result == restoreButton) {
            DatabaseConfig.restoreDatabaseFromBackup(rememberedBackup.get(), targetName);
            return true;
        }

        return false;
    }

    private void configureStartupAlert(Alert alert) {
        DialogUtil.applyReadableDecisionDialog(alert, 860, 940, 170, 205);
    }

    private void showMainStage(Stage stage) throws Exception {
        URL mainViewUrl = resolveResource("view/main.fxml");
        FXMLLoader loader = new FXMLLoader(mainViewUrl);

        try {
            Scene scene = new Scene(
                    loader.load(),
                    WindowSizingUtil.resolveInitialWidth(1420),
                    WindowSizingUtil.resolveInitialHeight(940)
            );
            stage.setTitle("Fito Gen Essentials");
            stage.setScene(scene);
            WindowSizingUtil.applyStageSize(stage, 1420, 940, 1220, 780);
            stage.show();
        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się załadować głównego widoku aplikacji: view/main.fxml", e);
        }
    }

    private void runPendingStarterPackBootstrapWithProgress(Stage stage) {
        pendingFg1BootstrapAfterStartup = false;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                updateMessage("Przygotowanie importu FG1...");
                updateProgress(0.0, 1.0);
                BootstrapStarterPackService starterPackService = new BootstrapStarterPackService();
                starterPackService.importFg1StarterPack((message, progress) -> {
                    updateMessage(message);
                    updateProgress(progress, 1.0);
                });
                updateMessage("Włączanie pełnego katalogu roślin...");
                updateProgress(0.96, 1.0);
                AppContext.getAppSettingsService().setPlantFullCatalogEnabled(true);
                updateMessage("Weryfikacja pakietu FG1...");
                updateProgress(0.99, 1.0);
                starterPackService.verifyFg1StarterPackImported();
                updateMessage("Pakiet FG1 gotowy.");
                updateProgress(1.0, 1.0);
                return null;
            }
        };

        ProgressDialogUtil.runTaskWithProgress(
                task,
                null,
                "Pakiet startowy FG1",
                "Tworzenie nowej bazy i ładowanie pakietu startowego FG1. Proszę czekać...",
                960,
                ignored -> {
                    try {
                        showMainStage(stage);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                },
                throwable -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Pakiet startowy FG1");
                    alert.setHeaderText("Nie udało się zasilić nowej bazy pakietem FG1.");
                    alert.setContentText(throwable == null ? "Nieznany błąd podczas importu FG1." : throwable.getMessage());
                    configureStartupAlert(alert);
                    alert.showAndWait();
                    try {
                        showMainStage(stage);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
        );
    }

    private URL resolveResource(String relativeResourcePath) {
        ClassLoader classLoader = MainAppFX.class.getClassLoader();

        URL resourceUrl = classLoader.getResource(relativeResourcePath);
        if (resourceUrl != null) {
            return resourceUrl;
        }

        Path filesystemPath = Path.of("src", "main", "resources").resolve(relativeResourcePath);
        if (Files.exists(filesystemPath)) {
            try {
                return filesystemPath.toUri().toURL();
            } catch (Exception e) {
                throw new IllegalStateException("Nie udało się zbudować URL dla zasobu: " + filesystemPath, e);
            }
        }

        throw new IllegalStateException(
                "Nie znaleziono zasobu FXML: " + relativeResourcePath +
                        ". Sprawdź, czy src/main/resources jest oznaczony jako Resources Root lub czy zasoby są kopiowane do classpath."
        );
    }

    public static void main(String[] args) {
        launch();
    }
}
