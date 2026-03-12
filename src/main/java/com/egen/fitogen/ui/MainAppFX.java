package com.egen.fitogen.ui;

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

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/dashboard.fxml")
        );

        Scene scene = new Scene(loader.load(), 1000, 700);

        stage.setTitle("Fitogen");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}