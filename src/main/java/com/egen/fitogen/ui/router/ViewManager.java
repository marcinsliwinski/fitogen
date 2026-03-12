package com.egen.fitogen.ui.router;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

public class ViewManager {

    private static StackPane container;

    private static final Map<String, String> views = new HashMap<>();

    static {

        views.put("dashboard", "/view/dashboard.fxml");
        views.put("batches", "/view/plant_batches.fxml");
        views.put("documents", "/view/documents.fxml");
        views.put("plants", "/view/plants.fxml");
        views.put("contrahents", "/view/contrahents.fxml");
    }

    public static void setContainer(StackPane pane) {
        container = pane;
    }

    public static void show(String view) {

        try {

            String path = views.get(view);

            Node node =
                    FXMLLoader.load(ViewManager.class.getResource(path));

            container.getChildren().setAll(node);

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
}