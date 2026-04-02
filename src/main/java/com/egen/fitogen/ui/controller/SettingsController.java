package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.domain.NumberingConfig;
import com.egen.fitogen.domain.NumberingSectionType;
import com.egen.fitogen.domain.NumberingType;
import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.model.DocumentType;
import com.egen.fitogen.model.IssuerProfile;
import com.egen.fitogen.dto.ContrahentImportPreviewResult;
import com.egen.fitogen.dto.ContrahentImportPreviewRow;
import com.egen.fitogen.dto.PlantImportPreviewResult;
import com.egen.fitogen.dto.PlantImportPreviewRow;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.AppUserService;
import com.egen.fitogen.service.BackupService;
import com.egen.fitogen.service.ContrahentCsvImportService;
import com.egen.fitogen.service.ContrahentService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.DocumentTypeService;
import com.egen.fitogen.service.NumberingConfigService;
import com.egen.fitogen.service.PlantCsvExportService;
import com.egen.fitogen.service.PlantCsvImportService;
import com.egen.fitogen.service.PlantService;
import com.egen.fitogen.ui.util.CountryDirectory;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SettingsController {

    private static final DateTimeFormatter BACKUP_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private ComboBox<NumberingType> numberingTypeBox;
    @FXML private ComboBox<NumberingSectionType> section1TypeBox;
    @FXML private TextField section1StaticValueField;
    @FXML private TextField section1SeparatorField;
    @FXML private ComboBox<NumberingSectionType> section2TypeBox;
    @FXML private TextField section2StaticValueField;
    @FXML private TextField section2SeparatorField;
    @FXML private ComboBox<NumberingSectionType> section3TypeBox;
    @FXML private TextField section3StaticValueField;
    @FXML private TextField section3SeparatorField;
    @FXML private TextField currentCounterField;
    @FXML private Label previewLabel;
    @FXML private Label infoLabel;
    @FXML private Label databasePathLabel;
    @FXML private Label backupStatusLabel;
    @FXML private ListView<DocumentType> documentTypesList;
    @FXML private TextField documentTypeNameField;
    @FXML private TextField documentTypeCodeField;
    @FXML private ListView<CountryDirectory.CountryEntry> customCountryEntriesList;
    @FXML private TextField customCountryNameField;
    @FXML private TextField customCountryCodeField;
    @FXML private Label countryDictionaryStatusLabel;
    @FXML private ListView<AppUser> usersList;
    @FXML private TextField userFirstNameField;
    @FXML private TextField userLastNameField;
    @FXML private ComboBox<AppUser> defaultUserBox;

    @FXML private TextField issuerNameField;
    @FXML private ComboBox<String> issuerCountryField;
    @FXML private ComboBox<String> issuerCountryCodeField;
    @FXML private TextField issuerPostalCodeField;
    @FXML private TextField issuerCityField;
    @FXML private TextField issuerStreetField;
    @FXML private TextField issuerPhytosanitaryNumberField;
    @FXML private Label issuerStatusLabel;

    @FXML private CheckBox plantPassportRequiredForAllCheckBox;
    @FXML private Label plantPassportModeStatusLabel;
    @FXML private CheckBox plantFullCatalogEnabledCheckBox;
    @FXML private Label plantCatalogModeStatusLabel;

    @FXML private Label importExportScopeSummaryLabel;
    @FXML private Label plantsImportSyncLabel;
    @FXML private Label eppoImportSyncLabel;
    @FXML private Label countriesImportSyncLabel;
    @FXML private Label contrahentsImportOnlyLabel;
    @FXML private Label documentsImportOnlyLabel;
    @FXML private Label exportCsvPlanLabel;
    @FXML private Label importExportRecommendationLabel;

    @FXML private Label plantsImportPreviewStatusLabel;
    @FXML private Label plantsImportPreviewFileLabel;
    @FXML private Label plantsImportPreviewSummaryLabel;
    @FXML private TableView<PlantImportPreviewRow> plantsImportPreviewTable;
    @FXML private TableColumn<PlantImportPreviewRow, Number> plantsColRowNumber;
    @FXML private TableColumn<PlantImportPreviewRow, String> plantsColSpecies;
    @FXML private TableColumn<PlantImportPreviewRow, String> plantsColVariety;
    @FXML private TableColumn<PlantImportPreviewRow, String> plantsColRootstock;
    @FXML private TableColumn<PlantImportPreviewRow, String> plantsColEppoCode;
    @FXML private TableColumn<PlantImportPreviewRow, Boolean> plantsColPassportRequired;
    @FXML private TableColumn<PlantImportPreviewRow, String> plantsColVisibilityStatus;
    @FXML private TableColumn<PlantImportPreviewRow, String> plantsColImportStatus;
    @FXML private TableColumn<PlantImportPreviewRow, String> plantsColImportMessage;

    @FXML private Label plantsExportStatusLabel;
    @FXML private Label plantsExportFileLabel;
    @FXML private Label plantsExportSummaryLabel;

    @FXML private Label contrahentsImportPreviewStatusLabel;
    @FXML private Label contrahentsImportPreviewFileLabel;
    @FXML private Label contrahentsImportPreviewSummaryLabel;
    @FXML private TableView<ContrahentImportPreviewRow> contrahentsImportPreviewTable;
    @FXML private TableColumn<ContrahentImportPreviewRow, Number> contrahentsColRowNumber;
    @FXML private TableColumn<ContrahentImportPreviewRow, String> contrahentsColName;
    @FXML private TableColumn<ContrahentImportPreviewRow, String> contrahentsColCountry;
    @FXML private TableColumn<ContrahentImportPreviewRow, String> contrahentsColCountryCode;
    @FXML private TableColumn<ContrahentImportPreviewRow, String> contrahentsColCity;
    @FXML private TableColumn<ContrahentImportPreviewRow, String> contrahentsColPostalCode;
    @FXML private TableColumn<ContrahentImportPreviewRow, String> contrahentsColPhytosanitaryNumber;
    @FXML private TableColumn<ContrahentImportPreviewRow, Boolean> contrahentsColSupplier;
    @FXML private TableColumn<ContrahentImportPreviewRow, Boolean> contrahentsColClient;
    @FXML private TableColumn<ContrahentImportPreviewRow, String> contrahentsColImportStatus;
    @FXML private TableColumn<ContrahentImportPreviewRow, String> contrahentsColImportMessage;

    private final NumberingConfigService numberingConfigService = AppContext.getNumberingConfigService();
    private final BackupService backupService = AppContext.getBackupService();
    private final DocumentTypeService documentTypeService = AppContext.getDocumentTypeService();
    private final AppUserService appUserService = AppContext.getAppUserService();
    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();
    private final PlantService plantService = AppContext.getPlantService();
    private final ContrahentService contrahentService = AppContext.getContrahentService();
    private final PlantCsvImportService plantCsvImportService =
            new PlantCsvImportService(plantService, appSettingsService);
    private final PlantCsvExportService plantCsvExportService =
            new PlantCsvExportService(plantService);
    private final ContrahentCsvImportService contrahentCsvImportService =
            new ContrahentCsvImportService(contrahentService, countryDirectoryService);

    private boolean loading;
    private boolean updatingDefaultUserSelection;
    private boolean updatingIssuerCountryFields;
    private int currentConfigId;
    private DocumentType editingDocumentType;
    private CountryDirectory.CountryEntry editingCustomCountryEntry;
    private AppUser editingUser;

    @FXML
    public void initialize() {
        configureTypeBoxes();
        configureListeners();
        configureUserControls();
        configureCustomCountryControls();
        configureIssuerCountryControls();

        numberingTypeBox.getItems().setAll(NumberingType.values());
        numberingTypeBox.setValue(NumberingType.DOCUMENT);

        databasePathLabel.setText(backupService.getDatabaseFilePath().toString());

        configureDictionarySelections();
        refreshDocumentTypes();
        refreshUsers();
        loadDefaultUser();
        loadIssuerProfile();
        loadCustomCountryEntries();
        loadPlantPassportMode();
        loadPlantCatalogMode();
        refreshBackupStatus();
        configureImportExportTables();
        resetPlantsImportPreviewState();
        resetPlantsExportState();
        resetContrahentsImportPreviewState();
        loadImportExportOverview();
        loadConfig(NumberingType.DOCUMENT);
    }

    private void configureUserControls() {
        defaultUserBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AppUser item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDisplayName());
            }
        });

        defaultUserBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AppUser item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Brak domyślnego użytkownika" : item.getDisplayName());
            }
        });

        defaultUserBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(AppUser object) {
                return object == null ? "" : object.getDisplayName();
            }

            @Override
            public AppUser fromString(String string) {
                return null;
            }
        });

        defaultUserBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingDefaultUserSelection) {
                return;
            }
            appUserService.setDefaultUserId(newVal != null ? newVal.getId() : null);
        });
    }

    private void configureCustomCountryControls() {
        if (customCountryEntriesList == null) {
            return;
        }

        customCountryEntriesList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(CountryDirectory.CountryEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.country() + " (" + item.countryCode() + ")");
            }
        });
    }

    private void configureIssuerCountryControls() {
        configureEditableCombo(issuerCountryField);
        configureEditableCombo(issuerCountryCodeField);

        attachAutocomplete(issuerCountryField, countryDirectoryService::getCountries);
        attachAutocomplete(issuerCountryCodeField, countryDirectoryService::getCodes);

        if (issuerCountryField != null && issuerCountryField.getEditor() != null) {
            issuerCountryField.getEditor().textProperty().addListener((obs, oldVal, newVal) -> syncIssuerCodeFromCountry());
        }

        if (issuerCountryCodeField != null && issuerCountryCodeField.getEditor() != null) {
            issuerCountryCodeField.getEditor().textProperty().addListener((obs, oldVal, newVal) -> syncIssuerCountryFromCode());
        }

        issuerCountryField.valueProperty().addListener((obs, oldVal, newVal) -> syncIssuerCodeFromCountry());
        issuerCountryCodeField.valueProperty().addListener((obs, oldVal, newVal) -> syncIssuerCountryFromCode());
    }

    private void configureDictionarySelections() {
        documentTypesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editingDocumentType = newVal;
            if (newVal == null) {
                documentTypeNameField.clear();
                documentTypeCodeField.clear();
                return;
            }
            documentTypeNameField.setText(newVal.getName());
            documentTypeCodeField.setText(newVal.getCode());
        });

        customCountryEntriesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editingCustomCountryEntry = newVal;
            if (newVal == null) {
                customCountryNameField.clear();
                customCountryCodeField.clear();
                return;
            }
            customCountryNameField.setText(newVal.country());
            customCountryCodeField.setText(newVal.countryCode());
        });

        usersList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editingUser = newVal;
            if (newVal == null) {
                userFirstNameField.clear();
                userLastNameField.clear();
                return;
            }
            userFirstNameField.setText(newVal.getFirstName());
            userLastNameField.setText(newVal.getLastName());
        });
    }

    private void refreshDocumentTypes() {
        documentTypesList.setItems(FXCollections.observableArrayList(documentTypeService.getAll()));
    }

    private void refreshUsers() {
        List<AppUser> users = appUserService.getAll();
        usersList.setItems(FXCollections.observableArrayList(users));
        defaultUserBox.setItems(FXCollections.observableArrayList(users));
    }

    private void loadDefaultUser() {
        updatingDefaultUserSelection = true;
        try {
            Integer defaultUserId = appUserService.getDefaultUserId().orElse(null);
            if (defaultUserId == null) {
                defaultUserBox.setValue(null);
                return;
            }

            for (AppUser user : defaultUserBox.getItems()) {
                if (user.getId() == defaultUserId) {
                    defaultUserBox.setValue(user);
                    return;
                }
            }

            defaultUserBox.setValue(null);
        } finally {
            updatingDefaultUserSelection = false;
        }
    }


    private void loadCustomCountryEntries() {
        if (customCountryEntriesList == null) {
            return;
        }

        List<CountryDirectory.CountryEntry> entries = countryDirectoryService.getCustomEntries();
        customCountryEntriesList.setItems(FXCollections.observableArrayList(entries));
        updateCountryDictionaryStatus();
    }

    @FXML
    private void saveCustomCountryEntry() {
        try {
            String country = ValidationUtil.requireText(customCountryNameField.getText(), "Kraj");
            String code = ValidationUtil.requireText(customCountryCodeField.getText(), "Kod kraju").toUpperCase();

            if (editingCustomCountryEntry != null
                    && !editingCustomCountryEntry.country().equalsIgnoreCase(country.trim())) {
                countryDirectoryService.deleteCustomEntry(
                        editingCustomCountryEntry.country(),
                        editingCustomCountryEntry.countryCode()
                );
            }

            countryDirectoryService.saveCustomEntry(country, code);
            loadCustomCountryEntries();
            refreshSharedCountryCombos();
            customCountryEntriesList.getSelectionModel().select(
                    countryDirectoryService.getCustomEntries().stream()
                            .filter(entry -> entry.country().equalsIgnoreCase(country.trim()))
                            .findFirst()
                            .orElse(null)
            );
            DialogUtil.showSuccess(editingCustomCountryEntry == null
                    ? "Wpis słownika krajów został dodany."
                    : "Wpis słownika krajów został zaktualizowany.");
        } catch (IllegalArgumentException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu", "Nie udało się zapisać wpisu słownika krajów.");
        }
    }

    @FXML
    private void deleteCustomCountryEntry() {
        CountryDirectory.CountryEntry selected = customCountryEntriesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz wpis słownika krajów do usunięcia.");
            return;
        }

        if (!DialogUtil.confirmDelete(selected.country() + " (" + selected.countryCode() + ")")) {
            return;
        }

        countryDirectoryService.deleteCustomEntry(selected.country(), selected.countryCode());
        loadCustomCountryEntries();
        refreshSharedCountryCombos();
        clearCustomCountryEntryForm();
        DialogUtil.showSuccess("Wpis słownika krajów został usunięty.");
    }

    @FXML
    private void clearCustomCountryEntryForm() {
        editingCustomCountryEntry = null;
        if (customCountryEntriesList != null) {
            customCountryEntriesList.getSelectionModel().clearSelection();
        }
        if (customCountryNameField != null) {
            customCountryNameField.clear();
        }
        if (customCountryCodeField != null) {
            customCountryCodeField.clear();
        }
        updateCountryDictionaryStatus();
    }

    private void updateCountryDictionaryStatus() {
        if (countryDictionaryStatusLabel == null) {
            return;
        }

        int customCount = countryDirectoryService.getCustomEntries().size();
        int totalCount = countryDirectoryService.getEntries().size();
        countryDictionaryStatusLabel.setText(
                "Wspólny słownik zawiera " + totalCount + " pozycji, w tym " + customCount
                        + " wpisów własnych. Jest używany przez Kontrahentów, EPPO i dane podmiotu."
        );
    }

    private void refreshSharedCountryCombos() {
        if (issuerCountryField != null) {
            issuerCountryField.setItems(FXCollections.observableArrayList(countryDirectoryService.getCountries()));
        }
        if (issuerCountryCodeField != null) {
            issuerCountryCodeField.setItems(FXCollections.observableArrayList(countryDirectoryService.getCodes()));
        }
    }

    private void loadPlantPassportMode() {
        boolean enabled = appSettingsService.isPlantPassportRequiredForAll();
        if (plantPassportRequiredForAllCheckBox != null) {
            plantPassportRequiredForAllCheckBox.setSelected(enabled);
        }
        updatePlantPassportModeStatus(enabled);
    }

    @FXML
    private void savePlantPassportMode() {
        try {
            boolean enabled = plantPassportRequiredForAllCheckBox != null && plantPassportRequiredForAllCheckBox.isSelected();
            appSettingsService.setPlantPassportRequiredForAll(enabled);
            updatePlantPassportModeStatus(enabled);
            DialogUtil.showSuccess("Ustawienie paszportów roślin zostało zapisane.");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu", "Nie udało się zapisać ustawienia paszportów roślin.");
        }
    }

    private void updatePlantPassportModeStatus(boolean enabled) {
        if (plantPassportModeStatusLabel == null) {
            return;
        }

        plantPassportModeStatusLabel.setText(
                enabled
                        ? "Wymaganie paszportów jest globalnie włączone. W formularzu rośliny pole będzie zaznaczone i zablokowane do edycji."
                        : "Wymaganie paszportów nie jest globalnie wymuszone. Każda roślina może mieć to ustawienie określone indywidualnie."
        );
    }

    private void loadPlantCatalogMode() {
        boolean enabled = appSettingsService.isPlantFullCatalogEnabled();
        if (plantFullCatalogEnabledCheckBox != null) {
            plantFullCatalogEnabledCheckBox.setSelected(enabled);
        }
        updatePlantCatalogModeStatus(enabled);
    }

    @FXML
    private void savePlantCatalogMode() {
        try {
            boolean enabled = plantFullCatalogEnabledCheckBox != null && plantFullCatalogEnabledCheckBox.isSelected();
            appSettingsService.setPlantFullCatalogEnabled(enabled);
            updatePlantCatalogModeStatus(enabled);
            DialogUtil.showSuccess("Tryb bazy roślin został zapisany.");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu", "Nie udało się zapisać trybu bazy roślin.");
        }
    }

    private void updatePlantCatalogModeStatus(boolean enabled) {
        if (plantCatalogModeStatusLabel == null) {
            return;
        }

        plantCatalogModeStatusLabel.setText(
                enabled
                        ? "Pełna baza roślin jest włączona. Lista Roślin pokazuje także pozycje nieużywane."
                        : "Pełna baza roślin jest wyłączona. Lista Roślin ukrywa pozycje oznaczone jako „Nieużywany”."
        );
    }

    @FXML
    private void clearDefaultUser() {
        defaultUserBox.setValue(null);
    }

    @FXML
    private void saveDocumentType() {
        try {
            DocumentType type = editingDocumentType != null ? editingDocumentType : new DocumentType();
            type.setName(documentTypeNameField.getText());
            type.setCode(documentTypeCodeField.getText());

            documentTypeService.save(type);
            refreshDocumentTypes();
            clearDocumentTypeForm();
            DialogUtil.showSuccess("Typ dokumentu został zapisany.");
        } catch (Exception e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        }
    }

    @FXML
    private void deleteDocumentType() {
        DocumentType selected = documentTypesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz typ dokumentu do usunięcia.");
            return;
        }

        if (!DialogUtil.confirmDelete(selected.toString())) {
            return;
        }

        documentTypeService.delete(selected.getId());
        refreshDocumentTypes();
        clearDocumentTypeForm();
        DialogUtil.showSuccess("Typ dokumentu został usunięty.");
    }

    @FXML
    private void clearDocumentTypeForm() {
        editingDocumentType = null;
        documentTypesList.getSelectionModel().clearSelection();
        documentTypeNameField.clear();
        documentTypeCodeField.clear();
    }

    @FXML
    private void saveUser() {
        try {
            AppUser user = editingUser != null ? editingUser : new AppUser();
            user.setFirstName(userFirstNameField.getText());
            user.setLastName(userLastNameField.getText());

            appUserService.save(user);
            refreshUsers();
            clearUserForm();
            loadDefaultUser();
            DialogUtil.showSuccess("Użytkownik został zapisany.");
        } catch (Exception e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        }
    }

    @FXML
    private void deleteUser() {
        AppUser selected = usersList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz użytkownika do usunięcia.");
            return;
        }

        if (!DialogUtil.confirmDelete(selected.toString())) {
            return;
        }

        appUserService.delete(selected.getId());
        refreshUsers();
        clearUserForm();
        loadDefaultUser();
        DialogUtil.showSuccess("Użytkownik został usunięty.");
    }

    @FXML
    private void clearUserForm() {
        editingUser = null;
        usersList.getSelectionModel().clearSelection();
        userFirstNameField.clear();
        userLastNameField.clear();
    }

    private void configureTypeBoxes() {
        StringConverter<NumberingType> numberingTypeConverter = new StringConverter<>() {
            @Override
            public String toString(NumberingType value) {
                if (value == null) {
                    return "";
                }

                return switch (value) {
                    case DOCUMENT -> "Dokumenty";
                    case BATCH -> "Partie roślin";
                };
            }

            @Override
            public NumberingType fromString(String string) {
                return null;
            }
        };

        numberingTypeBox.setConverter(numberingTypeConverter);

        StringConverter<NumberingSectionType> sectionTypeConverter = new StringConverter<>() {
            @Override
            public String toString(NumberingSectionType value) {
                if (value == null) {
                    return "";
                }

                return switch (value) {
                    case AUTO_INCREMENT -> "AUTO_INCREMENT";
                    case YEAR -> "YEAR";
                    case MONTH -> "MONTH";
                    case WEEK -> "WEEK";
                    case INTERIOR_BATCH -> "INTERIOR_BATCH";
                    case EXTERIOR_BATCH_NO -> "EXTERIOR_BATCH_NO";
                    case STATIC_TEXT -> "STATIC_TEXT";
                };
            }

            @Override
            public NumberingSectionType fromString(String string) {
                return null;
            }
        };

        section1TypeBox.setConverter(sectionTypeConverter);
        section2TypeBox.setConverter(sectionTypeConverter);
        section3TypeBox.setConverter(sectionTypeConverter);

        section1TypeBox.getItems().setAll(NumberingSectionType.values());
        section2TypeBox.getItems().setAll(NumberingSectionType.values());
        section3TypeBox.getItems().setAll(NumberingSectionType.values());
    }

    private void configureListeners() {
        numberingTypeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadConfig(newVal);
            }
        });

        section1TypeBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        section2TypeBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        section3TypeBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        section1StaticValueField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        section2StaticValueField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        section3StaticValueField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        section1SeparatorField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        section2SeparatorField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        section3SeparatorField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        currentCounterField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview());
    }

    private void loadConfig(NumberingType type) {
        loading = true;
        NumberingConfig config = numberingConfigService.getConfigOrDefault(type);
        currentConfigId = config.getId();

        section1TypeBox.setValue(config.getSection1Type());
        section1StaticValueField.setText(safe(config.getSection1StaticValue()));
        section1SeparatorField.setText(safe(config.getSection1Separator()));
        section2TypeBox.setValue(config.getSection2Type());
        section2StaticValueField.setText(safe(config.getSection2StaticValue()));
        section2SeparatorField.setText(safe(config.getSection2Separator()));
        section3TypeBox.setValue(config.getSection3Type());
        section3StaticValueField.setText(safe(config.getSection3StaticValue()));
        section3SeparatorField.setText(safe(config.getSection3Separator()));
        currentCounterField.setText(String.valueOf(config.getCurrentCounter()));

        loading = false;
        updatePreview();
    }

    private void updatePreview() {
        if (loading) {
            return;
        }

        try {
            NumberingConfig config = buildConfigFromForm();
            String preview = numberingConfigService.preview(config);
            previewLabel.setText(preview == null || preview.isBlank() ? "—" : preview);
            infoLabel.setText("Podgląd pokazuje kolejny numer dla bieżącej konfiguracji.");
        } catch (Exception e) {
            previewLabel.setText("—");
            infoLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void save() {
        try {
            NumberingConfig config = buildConfigFromForm();
            numberingConfigService.saveConfig(config);
            DialogUtil.showSuccess("Konfiguracja numeratora została zapisana.");
            loadConfig(config.getType());
        } catch (IllegalArgumentException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu", "Nie udało się zapisać konfiguracji numeratora.");
        }
    }

    @FXML
    private void restoreDefaults() {
        NumberingType selectedType = numberingTypeBox.getValue();
        if (selectedType == null) {
            DialogUtil.showWarning("Brak typu", "Wybierz typ numeracji.");
            return;
        }

        NumberingConfig defaultConfig = numberingConfigService.buildDefaultConfig(selectedType);
        defaultConfig.setId(currentConfigId);

        loading = true;
        section1TypeBox.setValue(defaultConfig.getSection1Type());
        section1StaticValueField.setText(safe(defaultConfig.getSection1StaticValue()));
        section1SeparatorField.setText(safe(defaultConfig.getSection1Separator()));
        section2TypeBox.setValue(defaultConfig.getSection2Type());
        section2StaticValueField.setText(safe(defaultConfig.getSection2StaticValue()));
        section2SeparatorField.setText(safe(defaultConfig.getSection2Separator()));
        section3TypeBox.setValue(defaultConfig.getSection3Type());
        section3StaticValueField.setText(safe(defaultConfig.getSection3StaticValue()));
        section3SeparatorField.setText(safe(defaultConfig.getSection3Separator()));
        currentCounterField.setText("0");
        loading = false;
        updatePreview();
    }

    @FXML
    private void saveIssuerProfile() {
        try {
            appSettingsService.saveIssuerProfile(buildIssuerProfileFromForm());
            loadIssuerProfile();
            DialogUtil.showSuccess("Dane podmiotu zostały zapisane.");
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu", "Nie udało się zapisać danych podmiotu.");
        }
    }

    @FXML
    private void clearIssuerProfileForm() {
        issuerNameField.clear();
        setComboValue(issuerCountryField, "");
        setComboValue(issuerCountryCodeField, "");
        issuerPostalCodeField.clear();
        issuerCityField.clear();
        issuerStreetField.clear();
        issuerPhytosanitaryNumberField.clear();
        issuerStatusLabel.setText("Formularz wyczyszczony. Zapisz, aby usunąć dane z ustawień.");
    }

    private void loadIssuerProfile() {
        IssuerProfile profile = appSettingsService.getIssuerProfile();
        issuerNameField.setText(safe(profile.getNurseryName()));
        setComboValue(issuerCountryField, safe(profile.getCountry()));
        setComboValue(issuerCountryCodeField, safe(profile.getCountryCode()));
        issuerPostalCodeField.setText(safe(profile.getPostalCode()));
        issuerCityField.setText(safe(profile.getCity()));
        issuerStreetField.setText(safe(profile.getStreet()));
        issuerPhytosanitaryNumberField.setText(safe(profile.getPhytosanitaryNumber()));
        updateIssuerStatusLabel();
    }

    private IssuerProfile buildIssuerProfileFromForm() {
        IssuerProfile profile = new IssuerProfile();
        profile.setNurseryName(safe(issuerNameField.getText()));
        profile.setCountry(safe(getComboValue(issuerCountryField)));
        profile.setCountryCode(safe(getComboValue(issuerCountryCodeField)));
        profile.setPostalCode(safe(issuerPostalCodeField.getText()));
        profile.setCity(safe(issuerCityField.getText()));
        profile.setStreet(safe(issuerStreetField.getText()));
        profile.setPhytosanitaryNumber(safe(issuerPhytosanitaryNumberField.getText()));
        return profile;
    }

    private void updateIssuerStatusLabel() {
        issuerStatusLabel.setText(
                appSettingsService.isIssuerProfileComplete()
                        ? "Profil podmiotu jest kompletny i gotowy do użycia w kolejnych modułach."
                        : "Profil podmiotu jest niekompletny. Dashboard będzie mógł to później sygnalizować."
        );
    }

    private void refreshBackupStatus() {
        String lastBackupAt = appSettingsService.getLastBackupAt();
        String lastBackupPath = appSettingsService.getLastBackupPath();

        if (lastBackupAt.isBlank()) {
            backupStatusLabel.setText("Backup nie został jeszcze wykonany.");
            return;
        }

        if (lastBackupPath.isBlank()) {
            backupStatusLabel.setText("Ostatni backup: " + lastBackupAt);
            return;
        }

        backupStatusLabel.setText("Ostatni backup: " + lastBackupAt + "\nLokalizacja: " + lastBackupPath);
    }

    @FXML
    private void createBackup() {
        try {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Wybierz katalog dla backupu");
            Window window = previewLabel.getScene() != null ? previewLabel.getScene().getWindow() : null;
            File selectedDirectory = chooser.showDialog(window);
            if (selectedDirectory == null) {
                return;
            }

            Path backupPath = backupService.createBackup(selectedDirectory.toPath());
            String backupTimestamp = LocalDateTime.now().format(BACKUP_DATE_TIME_FORMATTER);
            appSettingsService.saveLastBackup(backupPath.toString(), backupTimestamp);
            refreshBackupStatus();
            DialogUtil.showSuccess("Backup został utworzony:\n" + backupPath);
        } catch (IllegalArgumentException | IllegalStateException e) {
            backupStatusLabel.setText(e.getMessage());
            DialogUtil.showWarning("Backup", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            backupStatusLabel.setText("Nie udało się utworzyć backupu.");
            DialogUtil.showError("Błąd backupu", "Nie udało się utworzyć backupu bazy danych.");
        }
    }

    private NumberingConfig buildConfigFromForm() {
        NumberingType selectedType = numberingTypeBox.getValue();
        if (selectedType == null) {
            throw new IllegalArgumentException("Wybierz typ numeracji.");
        }

        NumberingConfig config = new NumberingConfig();
        config.setId(currentConfigId);
        config.setType(selectedType);
        config.setSection1Type(section1TypeBox.getValue());
        config.setSection1StaticValue(safe(section1StaticValueField.getText()));
        config.setSection1Separator(safe(section1SeparatorField.getText()));
        config.setSection2Type(section2TypeBox.getValue());
        config.setSection2StaticValue(safe(section2StaticValueField.getText()));
        config.setSection2Separator(safe(section2SeparatorField.getText()));
        config.setSection3Type(section3TypeBox.getValue());
        config.setSection3StaticValue(safe(section3StaticValueField.getText()));
        config.setSection3Separator(safe(section3SeparatorField.getText()));

        try {
            String rawCounter = safe(currentCounterField.getText());
            config.setCurrentCounter(rawCounter.isBlank() ? 0 : Integer.parseInt(rawCounter));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Licznik musi być liczbą całkowitą.");
        }

        return config;
    }

    private void syncIssuerCodeFromCountry() {
        if (updatingIssuerCountryFields) {
            return;
        }

        String country = getComboValue(issuerCountryField);
        if (country == null || country.isBlank()) {
            return;
        }

        String detectedCode = countryDirectoryService.findCodeByCountry(country);
        if (detectedCode == null || detectedCode.isBlank()) {
            return;
        }

        updatingIssuerCountryFields = true;
        try {
            setComboValue(issuerCountryCodeField, detectedCode);
        } finally {
            updatingIssuerCountryFields = false;
        }
    }

    private void syncIssuerCountryFromCode() {
        if (updatingIssuerCountryFields) {
            return;
        }

        String code = getComboValue(issuerCountryCodeField);
        if (code == null || code.isBlank()) {
            return;
        }

        String detectedCountry = countryDirectoryService.findCountryByCode(code);
        if (detectedCountry == null || detectedCountry.isBlank()) {
            return;
        }

        updatingIssuerCountryFields = true;
        try {
            setComboValue(issuerCountryField, detectedCountry);
        } finally {
            updatingIssuerCountryFields = false;
        }
    }

    private void configureEditableCombo(ComboBox<String> comboBox) {
        comboBox.setEditable(true);
        comboBox.setMaxWidth(Double.MAX_VALUE);
    }

    private void attachAutocomplete(ComboBox<String> comboBox, Supplier<List<String>> sourceSupplier) {
        comboBox.setItems(FXCollections.observableArrayList(sourceSupplier.get()));

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (!comboBox.isFocused() && !comboBox.getEditor().isFocused()) {
                return;
            }

            String currentText = comboBox.getEditor().getText();
            List<String> filtered = filterValues(sourceSupplier.get(), newValue);
            comboBox.setItems(FXCollections.observableArrayList(filtered));

            if (comboBox.getEditor() != null && currentText != null && !currentText.equals(comboBox.getEditor().getText())) {
                comboBox.getEditor().setText(currentText);
                comboBox.getEditor().positionCaret(currentText.length());
            }
        });

        comboBox.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                comboBox.setItems(FXCollections.observableArrayList(sourceSupplier.get()));
            }
        });

        comboBox.setOnMouseClicked(event ->
                comboBox.setItems(FXCollections.observableArrayList(sourceSupplier.get()))
        );

        comboBox.setOnAction(event -> {
            String selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                comboBox.getEditor().setText(selected);
            }
        });
    }

    private List<String> filterValues(List<String> source, String typedValue) {
        String keyword = typedValue == null ? "" : typedValue.trim().toLowerCase();
        List<String> result = new ArrayList<>();

        for (String value : source) {
            if (value == null || value.isBlank()) {
                continue;
            }

            if (keyword.isBlank() || value.toLowerCase().contains(keyword)) {
                result.add(value);
            }
        }

        return result;
    }

    private String getComboValue(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return "";
        }

        String editorText = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
        if (editorText != null && !editorText.isBlank()) {
            return editorText;
        }

        String value = comboBox.getValue();
        return value == null ? "" : value;
    }

    private void setComboValue(ComboBox<String> comboBox, String value) {
        if (comboBox == null) {
            return;
        }
        comboBox.setValue(value);
        if (comboBox.getEditor() != null) {
            comboBox.getEditor().setText(value == null ? "" : value);
        }
    }


private void configureImportExportTables() {
    if (plantsImportPreviewTable != null) {
        plantsColRowNumber.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getRowNumber()));
        plantsColSpecies.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getSpecies())));
        plantsColVariety.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getVariety())));
        plantsColRootstock.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getRootstock())));
        plantsColEppoCode.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getEppoCode())));
        plantsColPassportRequired.setCellValueFactory(cell -> new javafx.beans.property.SimpleBooleanProperty(cell.getValue().isPassportRequired()));
        plantsColVisibilityStatus.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getVisibilityStatus())));
        plantsColImportStatus.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getStatus())));
        plantsColImportMessage.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getMessage())));
    }

    if (contrahentsImportPreviewTable != null) {
        contrahentsColRowNumber.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getRowNumber()));
        contrahentsColName.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getName())));
        contrahentsColCountry.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getCountry())));
        contrahentsColCountryCode.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getCountryCode())));
        contrahentsColCity.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getCity())));
        contrahentsColPostalCode.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getPostalCode())));
        contrahentsColPhytosanitaryNumber.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getPhytosanitaryNumber())));
        contrahentsColSupplier.setCellValueFactory(cell -> new javafx.beans.property.SimpleBooleanProperty(cell.getValue().isSupplier()));
        contrahentsColClient.setCellValueFactory(cell -> new javafx.beans.property.SimpleBooleanProperty(cell.getValue().isClient()));
        contrahentsColImportStatus.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getStatus())));
        contrahentsColImportMessage.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(nullSafe(cell.getValue().getMessage())));
    }
}

private void loadImportExportOverview() {
    if (importExportScopeSummaryLabel == null) {
        return;
    }

    int plantCount = plantService.getAllPlants().size();
    int contrahentCount = contrahentService.getAllContrahents().size();
    int countryCount = countryDirectoryService.getEntries().size();

    importExportScopeSummaryLabel.setText(
            "Importy i eksporty CSV są prowadzone wyłącznie w Ustawieniach. Zakres docelowy rozdziela procesy import-only oraz import + aktualizacja z serwera."
    );
    plantsImportSyncLabel.setText(
            "Rośliny — import CSV + późniejsza aktualizacja z serwera. Aktualny stan: " + plantCount + " rekordów. Preview CSV działa w trybie tylko do odczytu, a eksport CSV jest już dostępny."
    );
    eppoImportSyncLabel.setText(
            "EPPO — import + aktualizacja z serwera. Docelowo obejmie kody EPPO razem z gatunkami i krajami/strefami przypisanymi do kodów."
    );
    countriesImportSyncLabel.setText(
            "Lista krajów — import + aktualizacja z serwera. Aktualny stan: " + countryCount + " wpisów wspólnego słownika krajów."
    );
    contrahentsImportOnlyLabel.setText(
            "Kontrahenci — tylko import CSV. Aktualny stan: " + contrahentCount + " rekordów. Preview CSV działa w trybie tylko do odczytu."
    );
    documentsImportOnlyLabel.setText(
            "Dokumenty — tylko import CSV. To obszar o najwyższej złożoności walidacyjnej, dlatego pozostaje po roślinach i kontrahentach."
    );
    exportCsvPlanLabel.setText(
            "Eksport CSV jest rozwijany w dokładnie tych samych standardach co import: spójne kolumny, przewidywalny zakres danych, bezpieczny zapis pliku i czytelny status operacji. Pierwszy etap obejmuje eksport Plants CSV."
    );
    importExportRecommendationLabel.setText(
            "Najbezpieczniejszy następny krok po tym etapie: zostawić importy CSV wyłącznie w Ustawieniach, a dalej rozszerzać eksport Plants oraz preview/import-only dla kontrahentów."
    );
}

@FXML
private void previewPlantsImportFile() {
    File selectedFile = chooseCsvOpenFile("Wybierz plik CSV do preview importu roślin");
    if (selectedFile == null) {
        return;
    }

    try {
        PlantImportPreviewResult result = plantCsvImportService.preview(selectedFile.toPath());
        plantsImportPreviewTable.getItems().setAll(result.getRows());
        plantsImportPreviewFileLabel.setText("Plik preview: " + selectedFile.getAbsolutePath());
        plantsImportPreviewSummaryLabel.setText(
                "Wiersze: " + result.getTotalRowsCount()
                        + ", nowe: " + result.getNewRowsCount()
                        + ", istniejące: " + result.getMatchingExistingCount()
                        + ", duplikaty: " + result.getDuplicateInFileCount()
                        + ", nieprawidłowe: " + result.getInvalidRowsCount()
                        + ", delimiter: '" + printableDelimiter(result.getDelimiter()) + "'."
        );
        String headersText = result.getResolvedHeaders().isEmpty()
                ? "brak rozpoznanych nagłówków"
                : String.join(", ", result.getResolvedHeaders());
        plantsImportPreviewStatusLabel.setText(
                "Preview importu Plants działa w trybie tylko do odczytu. Rozpoznane kolumny: "
                        + headersText + ". Zapis do bazy nie jest jeszcze aktywny."
        );
    } catch (Exception e) {
        plantsImportPreviewTable.getItems().clear();
        plantsImportPreviewFileLabel.setText("Plik preview: " + selectedFile.getAbsolutePath());
        plantsImportPreviewSummaryLabel.setText("Preview importu Plants nie powiódł się.");
        plantsImportPreviewStatusLabel.setText("Błąd preview importu Plants: " + e.getMessage());
    }
}

@FXML
private void clearPlantsImportPreview() {
    resetPlantsImportPreviewState();
}

private void resetPlantsImportPreviewState() {
    if (plantsImportPreviewTable != null) {
        plantsImportPreviewTable.getItems().clear();
    }
    if (plantsImportPreviewFileLabel != null) {
        plantsImportPreviewFileLabel.setText("Plik preview: nie wybrano pliku.");
    }
    if (plantsImportPreviewSummaryLabel != null) {
        plantsImportPreviewSummaryLabel.setText("Preview importu: brak danych do wyświetlenia.");
    }
    if (plantsImportPreviewStatusLabel != null) {
        plantsImportPreviewStatusLabel.setText(
                "Fundament importu CSV dla Plants jest przygotowany po stronie backendu w trybie preview-only. "
                        + plantCsvImportService.getSupportedColumnsSummary()
        );
    }
}

@FXML
private void exportPlantsCsv() {
    File targetFile = chooseCsvSaveFile("Zapisz eksport Plants CSV", "plants_export.csv");
    if (targetFile == null) {
        return;
    }

    try {
        Path exportedPath = plantCsvExportService.export(targetFile.toPath());
        plantsExportFileLabel.setText("Plik eksportu: " + exportedPath.toAbsolutePath().normalize());
        plantsExportSummaryLabel.setText(
                "Eksport Plants CSV zakończony powodzeniem. Wyeksportowano "
                        + plantService.getAllPlants().size()
                        + " rekordów."
        );
        plantsExportStatusLabel.setText(
                "Eksport działa w tych samych standardach zakresu kolumn co import preview. "
                        + plantCsvExportService.getSupportedColumnsSummary()
        );
    } catch (Exception e) {
        plantsExportFileLabel.setText("Plik eksportu: " + targetFile.getAbsolutePath());
        plantsExportSummaryLabel.setText("Eksport Plants CSV nie powiódł się.");
        plantsExportStatusLabel.setText("Błąd eksportu Plants CSV: " + e.getMessage());
    }
}

private void resetPlantsExportState() {
    if (plantsExportFileLabel != null) {
        plantsExportFileLabel.setText("Plik eksportu: nie wykonano jeszcze eksportu.");
    }
    if (plantsExportSummaryLabel != null) {
        plantsExportSummaryLabel.setText("Eksport CSV: brak zapisanych wyników.");
    }
    if (plantsExportStatusLabel != null) {
        plantsExportStatusLabel.setText(
                "Eksport Plants CSV jest dostępny wyłącznie w Ustawieniach. "
                        + plantCsvExportService.getSupportedColumnsSummary()
        );
    }
}

@FXML
private void previewContrahentsImportFile() {
    File selectedFile = chooseCsvOpenFile("Wybierz plik CSV do preview importu kontrahentów");
    if (selectedFile == null) {
        return;
    }

    try {
        ContrahentImportPreviewResult result = contrahentCsvImportService.preview(selectedFile.toPath());
        contrahentsImportPreviewTable.getItems().setAll(result.getRows());
        contrahentsImportPreviewFileLabel.setText("Plik preview: " + selectedFile.getAbsolutePath());
        contrahentsImportPreviewSummaryLabel.setText(
                "Wiersze: " + result.getTotalRowsCount()
                        + ", nowe: " + result.getNewRowsCount()
                        + ", istniejące: " + result.getMatchingExistingCount()
                        + ", duplikaty: " + result.getDuplicateInFileCount()
                        + ", nieprawidłowe: " + result.getInvalidRowsCount()
                        + ", delimiter: '" + printableDelimiter(result.getDelimiter()) + "'."
        );
        String headersText = result.getResolvedHeaders().isEmpty()
                ? "brak rozpoznanych nagłówków"
                : String.join(", ", result.getResolvedHeaders());
        contrahentsImportPreviewStatusLabel.setText(
                "Preview importu kontrahentów działa w trybie tylko do odczytu. Rozpoznane kolumny: "
                        + headersText + ". Zapis do bazy nie jest jeszcze aktywny."
        );
    } catch (Exception e) {
        contrahentsImportPreviewTable.getItems().clear();
        contrahentsImportPreviewFileLabel.setText("Plik preview: " + selectedFile.getAbsolutePath());
        contrahentsImportPreviewSummaryLabel.setText("Preview importu kontrahentów nie powiódł się.");
        contrahentsImportPreviewStatusLabel.setText("Błąd preview importu kontrahentów: " + e.getMessage());
    }
}

@FXML
private void clearContrahentsImportPreview() {
    resetContrahentsImportPreviewState();
}

private void resetContrahentsImportPreviewState() {
    if (contrahentsImportPreviewTable != null) {
        contrahentsImportPreviewTable.getItems().clear();
    }
    if (contrahentsImportPreviewFileLabel != null) {
        contrahentsImportPreviewFileLabel.setText("Plik preview: nie wybrano pliku.");
    }
    if (contrahentsImportPreviewSummaryLabel != null) {
        contrahentsImportPreviewSummaryLabel.setText("Preview importu: brak danych do wyświetlenia.");
    }
    if (contrahentsImportPreviewStatusLabel != null) {
        contrahentsImportPreviewStatusLabel.setText(
                "Fundament importu CSV dla kontrahentów jest przygotowany po stronie backendu w trybie preview-only. "
                        + contrahentCsvImportService.getSupportedColumnsSummary()
        );
    }
}

private File chooseCsvOpenFile(String title) {
    Window window = resolveWindow();

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle(title);
    fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Pliki CSV", "*.csv"),
            new FileChooser.ExtensionFilter("Pliki tekstowe", "*.txt", "*.tsv"),
            new FileChooser.ExtensionFilter("Wszystkie pliki", "*.*")
    );
    return fileChooser.showOpenDialog(window);
}

private File chooseCsvSaveFile(String title, String initialFileName) {
    Window window = resolveWindow();

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle(title);
    fileChooser.setInitialFileName(initialFileName);
    fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Pliki CSV", "*.csv")
    );
    return fileChooser.showSaveDialog(window);
}

private Window resolveWindow() {
    if (previewLabel != null && previewLabel.getScene() != null) {
        return previewLabel.getScene().getWindow();
    }
    return null;
}

private String printableDelimiter(char delimiter) {
    if (delimiter == '\t') {
        return "\\t";
    }
    return String.valueOf(delimiter);
}

private String nullSafe(String value) {
    return value == null ? "" : value;
}

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}