package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.dto.ImprovementSubmissionDraft;
import com.egen.fitogen.dto.ImprovementSubmissionResult;
import com.egen.fitogen.service.ImprovementSubmissionService;
import com.egen.fitogen.ui.router.ViewManager;
import com.egen.fitogen.ui.util.DialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImprovementController {

    @FXML private ComboBox<String> improvementTypeBox;
    @FXML private TextField improvementTitleField;
    @FXML private TextArea improvementDescriptionArea;
    @FXML private TextArea improvementExpectedBenefitArea;
    @FXML private ComboBox<String> improvementPriorityBox;
    @FXML private ListView<Path> improvementAttachmentsList;
    @FXML private Label improvementStatusLabel;
    @FXML private Label improvementDeliveryModeLabel;
    @FXML private Label improvementTechnicalContextLabel;

    private final ObservableList<Path> attachments = FXCollections.observableArrayList();
    private final ImprovementSubmissionService improvementSubmissionService = new ImprovementSubmissionService(
            AppContext.getAppSettingsService(),
            AppContext.getDatabaseProfileService(),
            AppContext.getAuditLogService()
    );

    @FXML
    public void initialize() {
        if (improvementTypeBox != null) {
            improvementTypeBox.getItems().setAll(
                    "Nowa funkcja",
                    "Ulepszenie istniejącej funkcji",
                    "Usprawnienie UI",
                    "Import / eksport CSV",
                    "EPPO / ZP",
                    "Numeracja",
                    "Wydajność",
                    "Integracja",
                    "Inne"
            );
            improvementTypeBox.setValue("Ulepszenie istniejącej funkcji");
        }

        if (improvementPriorityBox != null) {
            improvementPriorityBox.getItems().setAll("Niski", "Średni", "Wysoki");
            improvementPriorityBox.setValue("Średni");
        }

        if (improvementAttachmentsList != null) {
            improvementAttachmentsList.setItems(attachments);
            improvementAttachmentsList.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(Path item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.toAbsolutePath().normalize().toString());
                }
            });
        }

        if (improvementDeliveryModeLabel != null) {
            improvementDeliveryModeLabel.setText(
                    "Kanał główny: API. Jeśli wysyłka API się nie powiedzie, system zaproponuje ręczne zgłoszenie na adres fallback ustawiony w Ustawieniach."
            );
        }
        if (improvementTechnicalContextLabel != null) {
            improvementTechnicalContextLabel.setText(improvementSubmissionService.buildTechnicalContextSummary());
        }
        if (improvementStatusLabel != null) {
            improvementStatusLabel.setText("Wypełnij formularz i wybierz „Wyślij zgłoszenie”.");
        }
    }

    @FXML
    private void addAttachments() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Wybierz załączniki do zgłoszenia ulepszenia");
        List<File> selectedFiles = chooser.showOpenMultipleDialog(getWindow());
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return;
        }

        List<Path> newPaths = new ArrayList<>();
        for (File file : selectedFiles) {
            if (file != null) {
                Path path = file.toPath().toAbsolutePath().normalize();
                if (!attachments.contains(path)) {
                    newPaths.add(path);
                }
            }
        }
        attachments.addAll(newPaths);
        if (improvementStatusLabel != null) {
            improvementStatusLabel.setText("Dodano załączniki: " + newPaths.size() + ".");
        }
    }

    @FXML
    private void removeSelectedAttachment() {
        if (improvementAttachmentsList == null) {
            return;
        }
        Path selected = improvementAttachmentsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Załączniki", "Wybierz załącznik z listy.");
            return;
        }
        attachments.remove(selected);
        if (improvementStatusLabel != null) {
            improvementStatusLabel.setText("Usunięto wybrany załącznik.");
        }
    }

    @FXML
    private void submitImprovement() {
        String title = safe(improvementTitleField == null ? null : improvementTitleField.getText());
        String description = safe(improvementDescriptionArea == null ? null : improvementDescriptionArea.getText());
        if (title.isBlank()) {
            DialogUtil.showWarning("Zgłoś ulepszenie", "Podaj krótki tytuł zgłoszenia.");
            return;
        }
        if (description.isBlank()) {
            DialogUtil.showWarning("Zgłoś ulepszenie", "Opisz zgłaszane ulepszenie.");
            return;
        }

        ImprovementSubmissionDraft draft = improvementSubmissionService.createDraft(
                safe(improvementTypeBox == null ? null : improvementTypeBox.getValue()),
                title,
                description,
                safe(improvementExpectedBenefitArea == null ? null : improvementExpectedBenefitArea.getText()),
                safe(improvementPriorityBox == null ? null : improvementPriorityBox.getValue()),
                new ArrayList<>(attachments)
        );

        try {
            ImprovementSubmissionResult result = improvementSubmissionService.submitViaApi(draft);
            if (improvementStatusLabel != null) {
                improvementStatusLabel.setText(result.getMessage());
            }
            DialogUtil.showSuccess("Zgłoszenie ulepszenia zostało wysłane przez API.");
            clearForm();
        } catch (Exception apiException) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Zgłoś ulepszenie");
            alert.setHeaderText("Nie udało się wysłać zgłoszenia przez API.");
            alert.setContentText((apiException.getMessage() == null || apiException.getMessage().isBlank()
                    ? "Brak szczegółów błędu."
                    : apiException.getMessage())
                    + "\n\nCzy przygotować ręczne zgłoszenie e-mail?");
            Optional<ButtonType> decision = alert.showAndWait();
            if (decision.isEmpty() || decision.get() != ButtonType.OK) {
                if (improvementStatusLabel != null) {
                    improvementStatusLabel.setText("Wysyłka API nie powiodła się. Użytkownik zrezygnował z ręcznego fallbacku.");
                }
                return;
            }

            try {
                ImprovementSubmissionResult fallback = improvementSubmissionService.prepareManualFallback(draft);
                if (improvementStatusLabel != null) {
                    improvementStatusLabel.setText(fallback.getMessage());
                }
                String extra = fallback.getDraftFile() == null
                        ? ""
                        : "\nSzkic zgłoszenia zapisano w pliku: " + fallback.getDraftFile().toAbsolutePath().normalize();
                DialogUtil.showInfo("Zgłoś ulepszenie", fallback.getMessage() + extra);
                clearForm();
            } catch (Exception manualException) {
                manualException.printStackTrace();
                DialogUtil.showError("Zgłoś ulepszenie", "Nie udało się przygotować ręcznego zgłoszenia: " + manualException.getMessage());
            }
        }
    }

    @FXML
    private void clearForm() {
        if (improvementTypeBox != null) {
            improvementTypeBox.setValue("Ulepszenie istniejącej funkcji");
        }
        if (improvementTitleField != null) {
            improvementTitleField.clear();
        }
        if (improvementDescriptionArea != null) {
            improvementDescriptionArea.clear();
        }
        if (improvementExpectedBenefitArea != null) {
            improvementExpectedBenefitArea.clear();
        }
        if (improvementPriorityBox != null) {
            improvementPriorityBox.setValue("Średni");
        }
        attachments.clear();
        if (improvementStatusLabel != null) {
            improvementStatusLabel.setText("Formularz wyczyszczony.");
        }
    }

    @FXML
    private void openSettings() {
        SettingsController.requestInitialTab("Dane podmiotu");
        ViewManager.show(ViewManager.SETTINGS);
    }

    @FXML
    private void openHelp() {
        ViewManager.show(ViewManager.HELP);
    }

    private Window getWindow() {
        if (improvementTitleField != null && improvementTitleField.getScene() != null) {
            return improvementTitleField.getScene().getWindow();
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
