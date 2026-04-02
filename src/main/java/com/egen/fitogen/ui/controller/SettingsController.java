package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.domain.NumberingConfig;
import com.egen.fitogen.domain.NumberingSectionType;
import com.egen.fitogen.domain.NumberingType;
import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.model.DocumentType;
import com.egen.fitogen.model.IssuerProfile;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.AppUserService;
import com.egen.fitogen.service.BackupService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.DocumentTypeService;
import com.egen.fitogen.service.NumberingConfigService;
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
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
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

    private final NumberingConfigService numberingConfigService = AppContext.getNumberingConfigService();
    private final BackupService backupService = AppContext.getBackupService();
    private final DocumentTypeService documentTypeService = AppContext.getDocumentTypeService();
    private final AppUserService appUserService = AppContext.getAppUserService();
    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();

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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}