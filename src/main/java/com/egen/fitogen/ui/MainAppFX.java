package com.egen.fitogen.ui;

import atlantafx.base.theme.PrimerLight;
import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.database.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainAppFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        DatabaseInitializer.initDatabase();
        AppContext.init();

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("/view/main.fxml"));

        Scene scene = new Scene(loader.load(), 1200, 800);

        stage.setTitle("Fitogen");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}