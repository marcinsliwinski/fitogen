package com.egen.fitogen.ui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.function.Consumer;

public final class ModalViewUtil {

    private ModalViewUtil() {
    }

    public static <T> void openModal(
            String fxmlPath,
            String title,
            double width,
            double height,
            double minWidth,
            double minHeight,
            Consumer<T> controllerInitializer
    ) {
        try {
            URL resourceUrl = ModalViewUtil.class.getResource(fxmlPath);
            if (resourceUrl == null) {
                throw new IllegalStateException("Nie znaleziono widoku modalnego: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Scene scene = new Scene(loader.load(), width, height);

            URL stylesheetUrl = ModalViewUtil.class.getResource("/styles/app.css");
            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            }

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMinWidth(minWidth);
            stage.setMinHeight(minHeight);

            if (controllerInitializer != null) {
                @SuppressWarnings("unchecked")
                T controller = (T) loader.getController();
                controllerInitializer.accept(controller);
            }

            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd", "Nie udało się otworzyć okna: " + title + ".");
        }
    }
}