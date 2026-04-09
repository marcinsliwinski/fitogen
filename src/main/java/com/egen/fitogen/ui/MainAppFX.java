package com.egen.fitogen.ui;

import atlantafx.base.theme.PrimerLight;
import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.database.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.egen.fitogen.ui.util.WindowSizingUtil;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainAppFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        DatabaseInitializer.initDatabase();
        AppContext.init();

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        URL mainViewUrl = resolveResource("view/main.fxml");
        FXMLLoader loader = new FXMLLoader(mainViewUrl);

        try {
            Scene scene = new Scene(
                    loader.load(),
                    WindowSizingUtil.resolveInitialWidth(1280),
                    WindowSizingUtil.resolveInitialHeight(860)
            );
            stage.setTitle("Fito Gen Essentials");
            stage.setScene(scene);
            WindowSizingUtil.applyStageSize(stage, 1280, 860, 1100, 760);
            stage.show();
        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się załadować głównego widoku aplikacji: view/main.fxml", e);
        }
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
