package com.egen.fitogen.ui.router;

import com.egen.fitogen.ui.util.DialogUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ViewManager {

    public static final String DASHBOARD = "dashboard";
    public static final String BATCHES = "batches";
    public static final String DOCUMENTS = "documents";
    public static final String PLANTS = "plants";
    public static final String CONTRAHENTS = "contrahents";
    public static final String SETTINGS = "settings";
    public static final String UPDATES = "updates";
    public static final String HELP = "help";
    public static final String EPPO_ADMIN = "eppoAdmin";

    private static StackPane container;
    private static String currentView;

    private static final Map<String, String> VIEWS = new HashMap<>();

    static {
        VIEWS.put(DASHBOARD, "view/dashboard.fxml");
        VIEWS.put(BATCHES, "view/plant_batches.fxml");
        VIEWS.put(DOCUMENTS, "view/documents.fxml");
        VIEWS.put(PLANTS, "view/plants.fxml");
        VIEWS.put(CONTRAHENTS, "view/contrahents.fxml");
        VIEWS.put(SETTINGS, "view/settings.fxml");
        VIEWS.put(UPDATES, "view/updates.fxml");
        VIEWS.put(HELP, "view/help.fxml");
        VIEWS.put(EPPO_ADMIN, "view/eppo_admin.fxml");
    }

    private ViewManager() {
    }

    public static void setContainer(StackPane pane) {
        container = pane;
    }

    public static void show(String viewKey) {
        try {
            if (container == null) {
                throw new IllegalStateException("View container is not initialized.");
            }

            String relativePath = VIEWS.get(viewKey);
            if (relativePath == null) {
                throw new IllegalArgumentException("Unknown view: " + viewKey);
            }

            URL resourceUrl = resolveResource(relativePath);
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Node node = loader.load();
            container.getChildren().setAll(node);
            currentView = viewKey;

        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd widoku", "Nie udało się otworzyć widoku: " + viewKey + ".");
        }
    }

    public static void refreshCurrent() {
        if (currentView != null) {
            show(currentView);
        }
    }

    public static String getCurrentView() {
        return currentView;
    }

    private static URL resolveResource(String relativeResourcePath) {
        ClassLoader classLoader = ViewManager.class.getClassLoader();

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
                "Nie znaleziono zasobu widoku: " + relativeResourcePath +
                        ". Sprawdź, czy src/main/resources jest oznaczony jako Resources Root lub czy zasoby są kopiowane do classpath."
        );
    }
}
