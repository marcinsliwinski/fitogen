package com.egen.fitogen.ui.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.function.Consumer;

public final class ProgressDialogUtil {

    private ProgressDialogUtil() {
    }

    public static <T> void runTaskWithProgress(Task<T> task,
                                               Window owner,
                                               String title,
                                               String header,
                                               Consumer<T> onSuccess,
                                               Consumer<Throwable> onError) {
        if (task == null) {
            throw new IllegalArgumentException("Task jest wymagany.");
        }

        Stage dialog = new Stage(StageStyle.UTILITY);
        dialog.setTitle(title == null || title.isBlank() ? "Trwa operacja" : title);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setResizable(false);

        Label headerLabel = new Label(header == null || header.isBlank() ? "Trwa operacja. Proszę czekać..." : header);
        headerLabel.setWrapText(true);

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.textProperty().bind(task.messageProperty());

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(420);
        progressBar.progressProperty().bind(task.progressProperty());

        VBox root = new VBox(12, headerLabel, progressBar, messageLabel);
        root.setPadding(new Insets(18));
        root.setPrefWidth(460);

        dialog.setScene(new Scene(root));
        dialog.setOnCloseRequest(event -> event.consume());

        task.setOnSucceeded(event -> {
            dialog.close();
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });

        task.setOnFailed(event -> {
            dialog.close();
            if (onError != null) {
                Throwable exception = task.getException();
                onError.accept(exception == null ? new IllegalStateException("Nieznany błąd zadania.") : exception);
            }
        });

        task.setOnCancelled(event -> dialog.close());

        Thread thread = new Thread(task, "fitogen-progress-task");
        thread.setDaemon(true);

        Platform.runLater(() -> {
            dialog.show();
            thread.start();
        });
    }
}
