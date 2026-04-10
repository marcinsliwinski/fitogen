package com.egen.fitogen.ui;

import atlantafx.base.theme.PrimerLight;
import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.database.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import com.egen.fitogen.ui.util.WindowSizingUtil;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class MainAppFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        if (!ensureConfiguredDatabaseAvailable()) {
            stage.close();
            return;
        }
        DatabaseInitializer.initDatabase();
        AppContext.init();

        URL mainViewUrl = resolveResource("view/main.fxml");
        FXMLLoader loader = new FXMLLoader(mainViewUrl);

        try {
            Scene scene = new Scene(
                    loader.load(),
                    WindowSizingUtil.resolveInitialWidth(1360),
                    WindowSizingUtil.resolveInitialHeight(900)
            );
            stage.setTitle("Fito Gen Essentials");
            stage.setScene(scene);
            WindowSizingUtil.applyStageSize(stage, 1360, 900, 1180, 760);
            stage.show();
        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się załadować głównego widoku aplikacji: view/main.fxml", e);
        }
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Brak ostatnio używanej bazy");
        alert.setHeaderText("Nie znaleziono ostatnio używanej bazy danych.");
        alert.setContentText(
                "Zapamiętana baza danych nie istnieje już w zapisanej lokalizacji:\n"
                        + missingDatabasePath
                        + "\n\nMożesz utworzyć nową bazę profilu o tej nazwie albo odzyskać dane z ostatniej kopii zapasowej."
        );

        ButtonType createNewButton = new ButtonType("Utwórz nową bazę");
        ButtonType restoreButton = new ButtonType("Odzyskaj z backupu");
        ButtonType useFallbackButton = new ButtonType("Kontynuuj na dostępnej bazie");
        ButtonType cancelButton = new ButtonType("Zamknij", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(rememberedBackup.isPresent()
                ? new ButtonType[]{createNewButton, restoreButton, useFallbackButton, cancelButton}
                : new ButtonType[]{createNewButton, useFallbackButton, cancelButton});

        ButtonType result = alert.showAndWait().orElse(cancelButton);
        String missingName = missingDatabasePath.getFileName() == null ? "nowa_baza" : missingDatabasePath.getFileName().toString().replaceFirst("\\.db$", "");

        if (result == createNewButton) {
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Brak bazy danych");
        alert.setHeaderText("Nie znaleziono aktywnej bazy danych.");
        String content = "Aktywna baza danych nie istnieje już w zapisanej lokalizacji:\n"
                + configuredDatabasePath
                + "\n\nMożesz utworzyć nową bazę danych";
        if (rememberedBackup.isPresent()) {
            content += " lub odzyskać dane z ostatniej kopii zapasowej.";
        } else {
            content += ".";
        }
        alert.setContentText(content);

        ButtonType createNewButton = new ButtonType("Utwórz nową bazę");
        ButtonType restoreButton = new ButtonType("Odzyskaj z backupu");
        ButtonType cancelButton = new ButtonType("Zamknij", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(rememberedBackup.isPresent()
                ? new ButtonType[]{createNewButton, restoreButton, cancelButton}
                : new ButtonType[]{createNewButton, cancelButton});

        ButtonType result = alert.showAndWait().orElse(cancelButton);
        String targetName = configuredDatabasePath == null || configuredDatabasePath.getFileName() == null
                ? "fitogen"
                : configuredDatabasePath.getFileName().toString().replaceFirst("\\.db$", "");

        if (result == createNewButton) {
            DatabaseConfig.createDatabaseProfile(targetName);
            return true;
        }
        if (rememberedBackup.isPresent() && result == restoreButton) {
            DatabaseConfig.restoreDatabaseFromBackup(rememberedBackup.get(), targetName);
            return true;
        }

        return false;
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
