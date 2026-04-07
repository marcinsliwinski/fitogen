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
import com.egen.fitogen.service.AuditLogService;
import com.egen.fitogen.service.BackupService;
import com.egen.fitogen.service.ContrahentCsvExportService;
import com.egen.fitogen.service.ContrahentCsvImportService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.DocumentCsvExportService;
import com.egen.fitogen.service.DocumentCsvImportService;
import com.egen.fitogen.service.DocumentTypeService;
import com.egen.fitogen.service.NumberingConfigService;
import com.egen.fitogen.service.PlantCsvExportService;
import com.egen.fitogen.service.PlantCsvImportService;
import com.egen.fitogen.ui.util.CountryDirectory;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ValidationUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
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
import com.egen.fitogen.ui.util.UiTextUtil;
import com.egen.fitogen.database.SqliteDocumentItemRepository;
import com.egen.fitogen.database.SqliteDocumentRepository;

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
    @FXML private TextField documentTypeSearchField;
    @FXML private Label documentTypeStatusLabel;
    @FXML private Label documentTypeSummaryLabel;
    @FXML private TextField documentTypeNameField;
    @FXML private TextField documentTypeCodeField;
    @FXML private ListView<CountryDirectory.CountryEntry> allCountryEntriesList;
    @FXML private ListView<CountryDirectory.CountryEntry> customCountryEntriesList;
    @FXML private TextField customCountrySearchField;
    @FXML private Label countryDictionarySummaryLabel;
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
    @FXML private Label issuerSummaryLabel;

    @FXML private CheckBox plantPassportRequiredForAllCheckBox;
    @FXML private Label plantPassportModeStatusLabel;
    @FXML private CheckBox plantFullCatalogEnabledCheckBox;
    @FXML private Label plantCatalogModeStatusLabel;

    @FXML private Label plantsCsvColumnsLabel;
    @FXML private Label plantsCsvStatusLabel;
    @FXML private TextArea plantsCsvPreviewArea;
    @FXML private Label contrahentsCsvColumnsLabel;
    @FXML private Label contrahentsCsvStatusLabel;
    @FXML private TextArea contrahentsCsvPreviewArea;
    @FXML private Label documentsCsvColumnsLabel;
    @FXML private Label documentsCsvStatusLabel;
    @FXML private TextArea documentsCsvPreviewArea;

    @FXML private Label auditLogStatusLabel;
    @FXML private TextField auditLogSearchField;
    @FXML private Label auditLogSummaryLabel;
    @FXML private TableView<com.egen.fitogen.model.AuditLogEntry> auditLogTable;
    @FXML private TableColumn<com.egen.fitogen.model.AuditLogEntry, String> colAuditChangedAt;
    @FXML private TableColumn<com.egen.fitogen.model.AuditLogEntry, String> colAuditActor;
    @FXML private TableColumn<com.egen.fitogen.model.AuditLogEntry, String> colAuditEntityType;
    @FXML private TableColumn<com.egen.fitogen.model.AuditLogEntry, Integer> colAuditEntityId;
    @FXML private TableColumn<com.egen.fitogen.model.AuditLogEntry, String> colAuditActionType;
    @FXML private TableColumn<com.egen.fitogen.model.AuditLogEntry, String> colAuditDescription;

    private final NumberingConfigService numberingConfigService = AppContext.getNumberingConfigService();
    private final BackupService backupService = AppContext.getBackupService();
    private final DocumentTypeService documentTypeService = AppContext.getDocumentTypeService();
    private final AppUserService appUserService = AppContext.getAppUserService();
    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final AuditLogService auditLogService = AppContext.getAuditLogService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();
    private final PlantCsvImportService plantCsvImportService = new PlantCsvImportService(AppContext.getPlantService(), AppContext.getAppSettingsService());
    private final PlantCsvExportService plantCsvExportService = new PlantCsvExportService(AppContext.getPlantService());
    private final ContrahentCsvImportService contrahentCsvImportService = new ContrahentCsvImportService(AppContext.getContrahentService(), AppContext.getCountryDirectoryService());
    private final ContrahentCsvExportService contrahentCsvExportService = new ContrahentCsvExportService(AppContext.getContrahentService());
    private final DocumentCsvImportService documentCsvImportService = new DocumentCsvImportService(AppContext.getDocumentService(), AppContext.getContrahentService(), AppContext.getPlantBatchService());
    private final DocumentCsvExportService documentCsvExportService = new DocumentCsvExportService(new SqliteDocumentRepository(), new SqliteDocumentItemRepository(), AppContext.getContrahentService(), AppContext.getPlantBatchService());

    private boolean loading;
    private boolean updatingDefaultUserSelection;
    private boolean updatingIssuerCountryFields;
    private int currentConfigId;
    private DocumentType editingDocumentType;
    private final ObservableList<DocumentType> documentTypeMasterData = FXCollections.observableArrayList();
    private FilteredList<DocumentType> documentTypeFilteredData;
    private CountryDirectory.CountryEntry editingCustomCountryEntry;
    private final ObservableList<CountryDirectory.CountryEntry> allCountryMasterData = FXCollections.observableArrayList();
    private FilteredList<CountryDirectory.CountryEntry> allCountryFilteredData;
    private final ObservableList<CountryDirectory.CountryEntry> customCountryMasterData = FXCollections.observableArrayList();
    private FilteredList<CountryDirectory.CountryEntry> customCountryFilteredData;
    private AppUser editingUser;
    private final ObservableList<com.egen.fitogen.model.AuditLogEntry> auditLogMasterData = FXCollections.observableArrayList();
    private FilteredList<com.egen.fitogen.model.AuditLogEntry> auditLogFilteredData;
    private SortedList<com.egen.fitogen.model.AuditLogEntry> auditLogSortedData;

    @FXML
    public void initialize() {
        configureTypeBoxes();
        configureListeners();
        configureUserControls();
        configureCustomCountryControls();
        configureIssuerCountryControls();
        configureDocumentTypeControls();
        configureAuditLogControls();

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
        loadCsvOverview();
        loadAuditLogOverview();
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
        if (allCountryEntriesList != null) {
            allCountryFilteredData = new FilteredList<>(allCountryMasterData, entry -> true);
            allCountryEntriesList.setItems(allCountryFilteredData);
            allCountryEntriesList.setPlaceholder(new Label("Brak pozycji wspólnego słownika krajów do wyświetlenia."));
            allCountryEntriesList.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(CountryDirectory.CountryEntry item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : buildSharedCountryDisplay(item));
                }
            });
            allCountryEntriesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && countryDictionarySummaryLabel != null) {
                    countryDictionarySummaryLabel.setText(buildSharedCountrySummary(newVal));
                }
            });
        }

        if (customCountryEntriesList != null) {
            customCountryFilteredData = new FilteredList<>(customCountryMasterData, entry -> true);
            customCountryEntriesList.setItems(customCountryFilteredData);
            customCountryEntriesList.setPlaceholder(new Label("Brak własnych wpisów słownika krajów do wyświetlenia."));
            customCountryEntriesList.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(CountryDirectory.CountryEntry item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : buildCustomCountryDisplay(item));
                }
            });
        }

        if (customCountrySearchField != null) {
            customCountrySearchField.textProperty().addListener((obs, oldVal, newVal) -> applyCustomCountryFilter(newVal));
        }
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

    private void configureDocumentTypeControls() {
        if (documentTypesList == null) {
            return;
        }

        documentTypeFilteredData = new FilteredList<>(documentTypeMasterData, item -> true);
        documentTypesList.setItems(documentTypeFilteredData);
        documentTypesList.setPlaceholder(new Label("Brak typów dokumentów do wyświetlenia."));
        documentTypesList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DocumentType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : buildDocumentTypeDisplay(item));
            }
        });

        if (documentTypeSearchField != null) {
            documentTypeSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyDocumentTypeFilter(newVal));
        }
    }

    private void applyDocumentTypeFilter(String rawFilter) {
        if (documentTypeFilteredData == null) {
            return;
        }

        String keyword = rawFilter == null ? "" : rawFilter.trim().toLowerCase();
        documentTypeFilteredData.setPredicate(type -> {
            if (keyword.isBlank()) {
                return true;
            }

            return containsIgnoreCase(type.getName(), keyword)
                    || containsIgnoreCase(type.getCode(), keyword)
                    || containsIgnoreCase(buildDocumentTypeDisplay(type), keyword);
        });

        updateDocumentTypeVisibleSummary(keyword);
    }

    private void updateDocumentTypeVisibleSummary(String keyword) {
        if (documentTypeStatusLabel == null || documentTypeSummaryLabel == null || documentTypeFilteredData == null) {
            return;
        }

        int total = documentTypeMasterData.size();
        int visible = documentTypeFilteredData.size();
        String filterSuffix = keyword == null || keyword.isBlank()
                ? ""
                : UiTextUtil.buildQuotedFilterSuffix("Filtr", keyword);

        documentTypeStatusLabel.setText(
                "Łącznie typów dokumentów: " + total
                        + ". Widoczne po filtrze: " + visible
                        + ". Słownik jest używany przez formularze i listy dokumentów."
                        + filterSuffix
        );

        String summary = documentTypeFilteredData.stream()
                .findFirst()
                .map(this::buildDocumentTypeSummary)
                .orElse("Brak typów dokumentów spełniających bieżący filtr.");
        documentTypeSummaryLabel.setText(summary);
    }

    private String buildDocumentTypeSummary(DocumentType type) {
        if (type == null) {
            return "";
        }

        String code = safe(type.getCode());
        if (code.isBlank()) {
            return "Wybrany słownik nie ma jeszcze ustawionego skrótu kodowego. Nazwa: " + safe(type.getName()) + ".";
        }
        return "Pierwszy widoczny typ: " + safe(type.getName()) + " (kod: " + code + ").";
    }

    private String buildDocumentTypeDisplay(DocumentType type) {
        if (type == null) {
            return "";
        }

        String name = safe(type.getName());
        String code = safe(type.getCode());
        if (code.isBlank()) {
            return name;
        }
        return name + " / " + code;
    }

    @FXML
    private void clearDocumentTypeFilter() {
        if (documentTypeSearchField != null) {
            documentTypeSearchField.clear();
        } else {
            applyDocumentTypeFilter("");
        }
    }

    private void configureAuditLogControls() {
        if (auditLogTable == null) {
            return;
        }

        auditLogFilteredData = new FilteredList<>(auditLogMasterData, entry -> true);
        auditLogSortedData = new SortedList<>(auditLogFilteredData);
        auditLogSortedData.comparatorProperty().bind(auditLogTable.comparatorProperty());
        auditLogTable.setItems(auditLogSortedData);
        auditLogTable.setPlaceholder(new Label("Brak wpisów Audit Log do wyświetlenia."));
        auditLogTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        auditLogTable.setEditable(false);
        auditLogTable.getSortOrder().clear();
        if (colAuditChangedAt != null) {
            colAuditChangedAt.setSortType(TableColumn.SortType.DESCENDING);
            auditLogTable.getSortOrder().add(colAuditChangedAt);
        }

        auditLogTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateAuditLogSelectionSummary(newVal));

        if (auditLogSearchField != null) {
            auditLogSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyAuditLogFilter(newVal));
        }
    }

    private void applyAuditLogFilter(String rawFilter) {
        if (auditLogFilteredData == null) {
            return;
        }

        String keyword = rawFilter == null ? "" : rawFilter.trim().toLowerCase();
        auditLogFilteredData.setPredicate(entry -> {
            if (keyword.isBlank()) {
                return true;
            }

            return containsIgnoreCase(entry.getChangedAt(), keyword)
                    || containsIgnoreCase(entry.getActor(), keyword)
                    || containsIgnoreCase(entry.getEntityType(), keyword)
                    || containsIgnoreCase(String.valueOf(entry.getEntityId()), keyword)
                    || containsIgnoreCase(entry.getActionType(), keyword)
                    || containsIgnoreCase(entry.getDescription(), keyword);
        });

        updateAuditLogVisibleSummary(keyword);
        if (auditLogTable != null) {
            auditLogTable.sort();
        }
    }

    private void configureDictionarySelections() {
        documentTypesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editingDocumentType = newVal;
            if (newVal == null) {
                documentTypeNameField.clear();
                documentTypeCodeField.clear();
                updateDocumentTypeVisibleSummary(documentTypeSearchField == null ? "" : documentTypeSearchField.getText());
                return;
            }
            documentTypeNameField.setText(newVal.getName());
            documentTypeCodeField.setText(newVal.getCode());
            if (documentTypeSummaryLabel != null) {
                documentTypeSummaryLabel.setText("Wybrany typ: " + buildDocumentTypeDisplay(newVal) + ".");
            }
        });

        customCountryEntriesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editingCustomCountryEntry = newVal;
            if (newVal == null) {
                customCountryNameField.clear();
                customCountryCodeField.clear();
                updateCountryDictionaryStatus();
                return;
            }
            customCountryNameField.setText(newVal.country());
            customCountryCodeField.setText(newVal.countryCode());
            if (countryDictionarySummaryLabel != null) {
                countryDictionarySummaryLabel.setText("Wybrany wpis własny: " + buildCustomCountryDisplay(newVal) + ".");
            }
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
        DocumentType selected = documentTypesList != null ? documentTypesList.getSelectionModel().getSelectedItem() : null;
        int selectedId = selected != null ? selected.getId() : -1;

        documentTypeMasterData.setAll(documentTypeService.getAll());
        applyDocumentTypeFilter(documentTypeSearchField == null ? "" : documentTypeSearchField.getText());

        if (documentTypesList != null && selectedId > 0) {
            for (DocumentType type : documentTypeFilteredData) {
                if (type.getId() == selectedId) {
                    documentTypesList.getSelectionModel().select(type);
                    break;
                }
            }
        }
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
        if (customCountryEntriesList == null && allCountryEntriesList == null) {
            return;
        }

        CountryDirectory.CountryEntry selectedCustom = customCountryEntriesList == null
                ? null
                : customCountryEntriesList.getSelectionModel().getSelectedItem();
        String selectedCustomKey = selectedCustom == null ? "" : buildCustomCountryKey(selectedCustom);

        CountryDirectory.CountryEntry selectedShared = allCountryEntriesList == null
                ? null
                : allCountryEntriesList.getSelectionModel().getSelectedItem();
        String selectedSharedKey = selectedShared == null ? "" : buildCustomCountryKey(selectedShared);

        customCountryMasterData.setAll(countryDirectoryService.getCustomEntries());
        allCountryMasterData.setAll(countryDirectoryService.getEntries());
        applyCustomCountryFilter(customCountrySearchField == null ? "" : customCountrySearchField.getText());
        restoreCustomCountrySelection(selectedCustomKey);
        restoreSharedCountrySelection(selectedSharedKey);
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
    private void clearCustomCountryFilter() {
        if (customCountrySearchField != null) {
            customCountrySearchField.clear();
        } else {
            applyCustomCountryFilter("");
        }
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
        int visibleSharedCount = allCountryFilteredData == null ? totalCount : allCountryFilteredData.size();
        int visibleCustomCount = customCountryFilteredData == null ? customCount : customCountryFilteredData.size();
        String filter = customCountrySearchField == null ? "" : safe(customCountrySearchField.getText());
        String filterSuffix = UiTextUtil.buildQuotedFilterSuffix("Filtr", filter);
        countryDictionaryStatusLabel.setText(
                "Wspólny słownik zawiera " + totalCount + " pozycji, w tym " + customCount
                        + " wpisów własnych. Widoczne po filtrze: katalog " + visibleSharedCount
                        + ", wpisy własne " + visibleCustomCount
                        + ". Jest używany przez Kontrahentów, EPPO i dane podmiotu."
                        + filterSuffix
        );

        if (countryDictionarySummaryLabel != null) {
            CountryDirectory.CountryEntry selectedCustom = customCountryEntriesList == null
                    ? null
                    : customCountryEntriesList.getSelectionModel().getSelectedItem();
            CountryDirectory.CountryEntry selectedShared = allCountryEntriesList == null
                    ? null
                    : allCountryEntriesList.getSelectionModel().getSelectedItem();

            if (selectedCustom != null) {
                countryDictionarySummaryLabel.setText("Wybrany wpis własny: " + buildCustomCountryDisplay(selectedCustom) + ".");
                return;
            }

            if (selectedShared != null) {
                countryDictionarySummaryLabel.setText(buildSharedCountrySummary(selectedShared));
                return;
            }

            if (allCountryFilteredData != null && !allCountryFilteredData.isEmpty()) {
                countryDictionarySummaryLabel.setText(buildSharedCountrySummary(allCountryFilteredData.get(0)));
                return;
            }

            if (customCountryFilteredData != null && !customCountryFilteredData.isEmpty()) {
                countryDictionarySummaryLabel.setText(buildCustomCountrySummary(customCountryFilteredData.get(0)));
                return;
            }

            countryDictionarySummaryLabel.setText("Brak pozycji wspólnego słownika krajów spełniających bieżący filtr.");
        }
    }

    private void applyCustomCountryFilter(String rawFilter) {
        if (customCountryFilteredData == null && allCountryFilteredData == null) {
            return;
        }

        String keyword = rawFilter == null ? "" : rawFilter.trim().toLowerCase();

        if (allCountryFilteredData != null) {
            allCountryFilteredData.setPredicate(entry -> matchesCountryDirectoryFilter(entry, keyword));
        }

        if (customCountryFilteredData != null) {
            customCountryFilteredData.setPredicate(entry -> matchesCountryDirectoryFilter(entry, keyword));
        }

        updateCountryDictionaryStatus();
    }

    private void restoreCustomCountrySelection(String selectedKey) {
        if (customCountryEntriesList == null || selectedKey == null || selectedKey.isBlank() || customCountryFilteredData == null) {
            return;
        }

        for (CountryDirectory.CountryEntry entry : customCountryFilteredData) {
            if (buildCustomCountryKey(entry).equalsIgnoreCase(selectedKey)) {
                customCountryEntriesList.getSelectionModel().select(entry);
                return;
            }
        }
    }

    private void restoreSharedCountrySelection(String selectedKey) {
        if (allCountryEntriesList == null || selectedKey == null || selectedKey.isBlank() || allCountryFilteredData == null) {
            return;
        }

        for (CountryDirectory.CountryEntry entry : allCountryFilteredData) {
            if (buildCustomCountryKey(entry).equalsIgnoreCase(selectedKey)) {
                allCountryEntriesList.getSelectionModel().select(entry);
                return;
            }
        }
    }

    private boolean matchesCountryDirectoryFilter(CountryDirectory.CountryEntry entry, String keyword) {
        if (entry == null) {
            return false;
        }
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return containsIgnoreCase(entry.country(), keyword)
                || containsIgnoreCase(entry.countryCode(), keyword)
                || containsIgnoreCase(buildCustomCountryDisplay(entry), keyword)
                || containsIgnoreCase(buildSharedCountryDisplay(entry), keyword)
                || containsIgnoreCase(resolveCountryEntrySourceLabel(entry), keyword);
    }

    private String buildCustomCountryDisplay(CountryDirectory.CountryEntry entry) {
        if (entry == null) {
            return "";
        }
        return safe(entry.country()) + " (" + safe(entry.countryCode()) + ")";
    }

    private String buildSharedCountryDisplay(CountryDirectory.CountryEntry entry) {
        if (entry == null) {
            return "";
        }
        return buildCustomCountryDisplay(entry) + " • " + resolveCountryEntrySourceLabel(entry);
    }

    private String buildCustomCountrySummary(CountryDirectory.CountryEntry entry) {
        if (entry == null) {
            return "Brak wybranego wpisu słownika krajów.";
        }
        return "Pierwszy widoczny wpis własny: " + buildCustomCountryDisplay(entry)
                + ". Wpisy własne są współdzielone przez Kontrahentów, EPPO i dane wystawcy.";
    }

    private String buildSharedCountrySummary(CountryDirectory.CountryEntry entry) {
        if (entry == null) {
            return "Brak pozycji wspólnego słownika krajów.";
        }
        return "Pierwsza widoczna pozycja wspólnego słownika: " + buildSharedCountryDisplay(entry)
                + ". Katalog bazowy i wpisy własne są współdzielone przez Kontrahentów, EPPO i dane wystawcy.";
    }

    private String resolveCountryEntrySourceLabel(CountryDirectory.CountryEntry entry) {
        return isCustomCountryEntry(entry) ? "wpis własny użytkownika" : "katalog bazowy";
    }

    private boolean isCustomCountryEntry(CountryDirectory.CountryEntry entry) {
        if (entry == null) {
            return false;
        }

        String entryKey = buildCustomCountryKey(entry);
        for (CountryDirectory.CountryEntry customEntry : countryDirectoryService.getCustomEntries()) {
            if (buildCustomCountryKey(customEntry).equalsIgnoreCase(entryKey)) {
                return true;
            }
        }
        return false;
    }

    private String buildCustomCountryKey(CountryDirectory.CountryEntry entry) {
        return safe(entry == null ? "" : entry.country()) + "|" + safe(entry == null ? "" : entry.countryCode());
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
        if (documentTypesList != null) {
            documentTypesList.getSelectionModel().clearSelection();
        }
        documentTypeNameField.clear();
        documentTypeCodeField.clear();
        updateDocumentTypeVisibleSummary(documentTypeSearchField == null ? "" : documentTypeSearchField.getText());
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

        attachIssuerProfileListener(issuerNameField);
        attachIssuerProfileListener(issuerPostalCodeField);
        attachIssuerProfileListener(issuerCityField);
        attachIssuerProfileListener(issuerStreetField);
        attachIssuerProfileListener(issuerPhytosanitaryNumberField);

        if (issuerCountryField != null) {
            issuerCountryField.valueProperty().addListener((obs, oldVal, newVal) -> updateIssuerStatusLabel());
            if (issuerCountryField.getEditor() != null) {
                issuerCountryField.getEditor().textProperty().addListener((obs, oldVal, newVal) -> updateIssuerStatusLabel());
            }
        }

        if (issuerCountryCodeField != null) {
            issuerCountryCodeField.valueProperty().addListener((obs, oldVal, newVal) -> updateIssuerStatusLabel());
            if (issuerCountryCodeField.getEditor() != null) {
                issuerCountryCodeField.getEditor().textProperty().addListener((obs, oldVal, newVal) -> updateIssuerStatusLabel());
            }
        }
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
            IssuerProfile profile = buildIssuerProfileFromForm();
            List<String> validationIssues = getIssuerProfileValidationIssues(profile);
            appSettingsService.saveIssuerProfile(profile);
            loadIssuerProfile();

            if (validationIssues.isEmpty()) {
                DialogUtil.showSuccess("Dane podmiotu zostały zapisane.");
            } else {
                DialogUtil.showWarning(
                        "Dane zapisane z brakami",
                        "Dane podmiotu zostały zapisane, ale profil nadal wymaga uzupełnienia:\n- "
                                + String.join("\n- ", validationIssues)
                );
            }
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
        updateIssuerStatusLabel();
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
        IssuerProfile profile = buildIssuerProfileFromForm();
        List<String> validationIssues = getIssuerProfileValidationIssues(profile);
        boolean countryPairConsistent = isIssuerCountryPairConsistent(profile);

        if (validationIssues.isEmpty() && countryPairConsistent) {
            issuerStatusLabel.setText("Profil podmiotu jest kompletny i gotowy do użycia w dokumentach, dashboardzie i dalszych modułach.");
        } else if (validationIssues.isEmpty()) {
            issuerStatusLabel.setText("Profil podmiotu jest prawie kompletny, ale para kraj / kod kraju wymaga ujednolicenia według wspólnego słownika.");
        } else {
            issuerStatusLabel.setText(
                    "Profil podmiotu wymaga uzupełnienia. Brakuje: " + String.join(", ", validationIssues)
                            + (countryPairConsistent ? "." : ". Dodatkowo para kraj / kod kraju nie jest zgodna ze wspólnym słownikiem.")
            );
        }

        if (issuerSummaryLabel != null) {
            issuerSummaryLabel.setText(buildIssuerSummary(profile, validationIssues, countryPairConsistent));
        }
    }

    private void attachIssuerProfileListener(TextField field) {
        if (field != null) {
            field.textProperty().addListener((obs, oldVal, newVal) -> updateIssuerStatusLabel());
        }
    }

    private List<String> getIssuerProfileValidationIssues(IssuerProfile profile) {
        List<String> missing = new ArrayList<>();
        if (profile == null) {
            missing.add("nazwa");
            missing.add("kraj");
            missing.add("kod pocztowy");
            missing.add("miasto");
            missing.add("ulica i numer");
            missing.add("nr fitosanitarny");
            return missing;
        }

        if (safe(profile.getNurseryName()).isBlank()) {
            missing.add("nazwa");
        }
        if (safe(profile.getCountry()).isBlank()) {
            missing.add("kraj");
        }
        if (safe(profile.getPostalCode()).isBlank()) {
            missing.add("kod pocztowy");
        }
        if (safe(profile.getCity()).isBlank()) {
            missing.add("miasto");
        }
        if (safe(profile.getStreet()).isBlank()) {
            missing.add("ulica i numer");
        }
        if (safe(profile.getPhytosanitaryNumber()).isBlank()) {
            missing.add("nr fitosanitarny");
        }
        return missing;
    }

    private boolean isIssuerCountryPairConsistent(IssuerProfile profile) {
        if (profile == null) {
            return true;
        }

        String country = safe(profile.getCountry());
        String code = safe(profile.getCountryCode()).toUpperCase();

        if (country.isBlank() || code.isBlank()) {
            return true;
        }

        String detectedCode = safe(countryDirectoryService.findCodeByCountry(country)).toUpperCase();
        String detectedCountry = safe(countryDirectoryService.findCountryByCode(code));

        boolean countryMatches = detectedCountry.isBlank() || detectedCountry.equalsIgnoreCase(country);
        boolean codeMatches = detectedCode.isBlank() || detectedCode.equalsIgnoreCase(code);
        return countryMatches && codeMatches;
    }

    private String buildIssuerSummary(IssuerProfile profile, List<String> validationIssues, boolean countryPairConsistent) {
        StringBuilder builder = new StringBuilder();
        builder.append("Podsumowanie formularza: ");

        if (safe(profile.getNurseryName()).isBlank()) {
            builder.append("brak nazwy podmiotu");
        } else {
            builder.append(safe(profile.getNurseryName()));
        }

        if (!safe(profile.getCity()).isBlank()) {
            builder.append(", ").append(safe(profile.getCity()));
        }

        if (!safe(profile.getCountry()).isBlank()) {
            builder.append(", kraj: ").append(safe(profile.getCountry()));
        }

        if (!safe(profile.getCountryCode()).isBlank()) {
            builder.append(" (").append(safe(profile.getCountryCode()).toUpperCase()).append(")");
        }

        builder.append(". ");

        if (validationIssues.isEmpty() && countryPairConsistent) {
            builder.append("Profil jest spójny ze wspólnym słownikiem krajów.");
        } else if (!countryPairConsistent) {
            builder.append("Ujednolić kraj i kod kraju zgodnie ze wspólnym słownikiem krajów.");
        } else {
            builder.append("Do uzupełnienia: ").append(String.join(", ", validationIssues)).append(".");
        }

        return builder.toString();
    }

    private void refreshBackupStatus() {
        if (backupStatusLabel == null) {
            return;
        }

        String lastBackupAt = safe(appSettingsService.getLastBackupAt());
        String lastBackupPath = safe(appSettingsService.getLastBackupPath());

        if (lastBackupAt.isBlank()) {
            backupStatusLabel.setText("Backup nie został jeszcze wykonany.");
            return;
        }

        if (lastBackupPath.isBlank()) {
            backupStatusLabel.setText("Ostatni backup: " + lastBackupAt);
            return;
        }

        backupStatusLabel.setText(UiTextUtil.buildPathMessage("Ostatni backup: " + lastBackupAt, "Lokalizacja: " + lastBackupPath));
    }

    @FXML
    private void createBackup() {
        try {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Wybierz katalog dla backupu");
            Window window = getOwningWindow();
            File selectedDirectory = chooser.showDialog(window);
            if (selectedDirectory == null) {
                return;
            }

            Path backupPath = backupService.createBackup(selectedDirectory.toPath());
            String backupTimestamp = LocalDateTime.now().format(BACKUP_DATE_TIME_FORMATTER);
            appSettingsService.saveLastBackup(backupPath.toString(), backupTimestamp);
            if (auditLogService != null) {
                auditLogService.log(
                        "APP_SETTINGS",
                        null,
                        "UPDATE",
                        "Utworzono backup bazy danych w lokalizacji " + backupPath
                );
            }
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



    private void configureAuditLogTable() {
        if (auditLogTable == null) {
            return;
        }

        colAuditChangedAt.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getChangedAt())));
        colAuditActor.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getActor())));
        colAuditEntityType.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getEntityType())));
        colAuditEntityId.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getEntityId()));
        colAuditActionType.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getActionType())));
        colAuditDescription.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getDescription())));
    }


    private void updateAuditLogVisibleSummary(String keyword) {
        if (auditLogStatusLabel == null || auditLogSummaryLabel == null || auditLogFilteredData == null) {
            return;
        }

        int totalEntries = auditLogService.getEntryCount();
        int visibleEntries = auditLogFilteredData.size();
        String filterSuffix = keyword == null || keyword.isBlank()
                ? ""
                : UiTextUtil.buildQuotedFilterSuffix("Filtr", keyword);

        auditLogStatusLabel.setText(
                "Liczba wpisów audit log: " + totalEntries
                        + ". Widoczne po filtrze: " + visibleEntries
                        + ". Tabela działa w trybie tylko do odczytu."
                        + filterSuffix
        );

        updateAuditLogSelectionSummary(auditLogTable == null ? null : auditLogTable.getSelectionModel().getSelectedItem());
    }


    private void updateAuditLogSelectionSummary(com.egen.fitogen.model.AuditLogEntry selectedEntry) {
        if (auditLogSummaryLabel == null) {
            return;
        }

        com.egen.fitogen.model.AuditLogEntry entryToDescribe = selectedEntry;
        if (entryToDescribe == null) {
            entryToDescribe = auditLogSortedData == null || auditLogSortedData.isEmpty()
                    ? null
                    : auditLogSortedData.get(0);
        }

        if (entryToDescribe == null) {
            auditLogSummaryLabel.setText("Brak wpisów spełniających bieżący filtr.");
            return;
        }

        String prefix = selectedEntry == null ? "Najnowszy widoczny wpis: " : "Wybrany wpis: ";
        auditLogSummaryLabel.setText(prefix + buildAuditLogEntrySummary(entryToDescribe));
    }

    private String buildAuditLogEntrySummary(com.egen.fitogen.model.AuditLogEntry entry) {
        if (entry == null) {
            return "Brak wpisów audytowych.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(safe(entry.getChangedAt()));
        builder.append(" | ").append(safe(entry.getActor()));
        builder.append(" | ").append(safe(entry.getEntityType()));
        if (entry.getEntityId() != null) {
            builder.append(" #").append(entry.getEntityId());
        }
        builder.append(" | ").append(safe(entry.getActionType()));
        if (!safe(entry.getDescription()).isBlank()) {
            builder.append(" | ").append(safe(entry.getDescription()));
        }
        return builder.toString();
    }

    private void loadAuditLogOverview() {
        if (auditLogTable == null) {
            return;
        }

        configureAuditLogTable();
        com.egen.fitogen.model.AuditLogEntry selected = auditLogTable.getSelectionModel().getSelectedItem();
        String selectionKey = buildAuditLogSelectionKey(selected);
        auditLogMasterData.setAll(auditLogService.getRecentEntries(200));
        applyAuditLogFilter(auditLogSearchField == null ? "" : auditLogSearchField.getText());
        restoreAuditLogSelection(selectionKey);
    }


    private void restoreAuditLogSelection(String selectionKey) {
        if (auditLogTable == null || selectionKey == null || selectionKey.isBlank() || auditLogSortedData == null) {
            return;
        }

        for (com.egen.fitogen.model.AuditLogEntry entry : auditLogSortedData) {
            if (buildAuditLogSelectionKey(entry).equals(selectionKey)) {
                auditLogTable.getSelectionModel().select(entry);
                auditLogTable.scrollTo(entry);
                return;
            }
        }
    }

    private String buildAuditLogSelectionKey(com.egen.fitogen.model.AuditLogEntry entry) {
        if (entry == null) {
            return "";
        }

        return safe(entry.getChangedAt())
                + "|" + safe(entry.getActor())
                + "|" + safe(entry.getEntityType())
                + "|" + safe(entry.getActionType())
                + "|" + safe(entry.getDescription());
    }

    @FXML
    private void refreshAuditLog() {
        try {
            loadAuditLogOverview();
        } catch (Exception e) {
            e.printStackTrace();
            if (auditLogStatusLabel != null) {
                auditLogStatusLabel.setText("Nie udało się odświeżyć Audit Log.");
            }
            DialogUtil.showError("Audit Log", "Nie udało się odczytać wpisów Audit Log.");
        }
    }

    @FXML
    private void clearAuditLogFilter() {
        if (auditLogSearchField != null) {
            auditLogSearchField.clear();
        } else {
            applyAuditLogFilter("");
        }
    }

    private void loadCsvOverview() {
        if (plantsCsvColumnsLabel != null) {
            plantsCsvColumnsLabel.setText(plantCsvImportService.getSupportedColumnsSummary() + " " + plantCsvExportService.getSupportedColumnsSummary());
        }
        if (contrahentsCsvColumnsLabel != null) {
            contrahentsCsvColumnsLabel.setText(contrahentCsvImportService.getSupportedColumnsSummary() + " " + contrahentCsvExportService.getSupportedColumnsSummary());
        }
        if (documentsCsvColumnsLabel != null) {
            documentsCsvColumnsLabel.setText(documentCsvImportService.getSupportedColumnsSummary() + " " + documentCsvExportService.getSupportedColumnsSummary());
        }
        if (plantsCsvStatusLabel != null) {
            plantsCsvStatusLabel.setText("Wybierz plik CSV, aby zobaczyć preview importu Plants, albo zapisz aktualny eksport do pliku.");
        }
        if (contrahentsCsvStatusLabel != null) {
            contrahentsCsvStatusLabel.setText("Wybierz plik CSV, aby zobaczyć preview importu Contrahents, albo zapisz aktualny eksport do pliku.");
        }
        if (documentsCsvStatusLabel != null) {
            documentsCsvStatusLabel.setText("Wybierz plik CSV, aby zobaczyć preview importu Documents, albo zapisz aktualny eksport do pliku.");
        }
        resetPlantsCsvPreview();
        resetContrahentsCsvPreview();
        resetDocumentsCsvPreview();
    }

    @FXML
    private void previewPlantsCsvImport() {
        Path selectedPath = chooseCsvToOpen("Wybierz plik CSV roślin");
        if (selectedPath == null) {
            return;
        }

        try {
            var result = plantCsvImportService.preview(selectedPath);
            plantsCsvStatusLabel.setText(buildPlantsPreviewStatus(result));
            plantsCsvPreviewArea.setText(buildPlantsPreviewText(result));
        } catch (Exception e) {
            e.printStackTrace();
            plantsCsvStatusLabel.setText("Nie udało się przygotować preview importu Plants.");
            DialogUtil.showError("Preview importu Plants CSV", "Nie udało się odczytać ani przeanalizować pliku CSV roślin.");
        }
    }

    @FXML
    private void exportPlantsCsv() {
        Path selectedPath = chooseCsvToSave("Eksportuj Plants do CSV", "plants-export.csv");
        if (selectedPath == null) {
            return;
        }

        try {
            Path exported = plantCsvExportService.export(selectedPath);
            plantsCsvStatusLabel.setText("Eksport Plants zakończony powodzeniem: " + exported + ". Format pozostaje lokalnym standardem Settings -> Import / Eksport CSV.");
            DialogUtil.showSuccess("Plants zostały wyeksportowane do pliku:\n" + exported);
        } catch (Exception e) {
            e.printStackTrace();
            plantsCsvStatusLabel.setText("Nie udało się wyeksportować Plants do CSV.");
            DialogUtil.showError("Eksport Plants CSV", "Nie udało się wyeksportować roślin do pliku CSV.");
        }
    }

    @FXML
    private void previewContrahentsCsvImport() {
        Path selectedPath = chooseCsvToOpen("Wybierz plik CSV kontrahentów");
        if (selectedPath == null) {
            return;
        }

        try {
            var result = contrahentCsvImportService.preview(selectedPath);
            contrahentsCsvStatusLabel.setText(buildContrahentsPreviewStatus(result));
            contrahentsCsvPreviewArea.setText(buildContrahentsPreviewText(result));
        } catch (Exception e) {
            e.printStackTrace();
            contrahentsCsvStatusLabel.setText("Nie udało się przygotować preview importu Contrahents.");
            DialogUtil.showError("Preview importu Contrahents CSV", "Nie udało się odczytać ani przeanalizować pliku CSV kontrahentów.");
        }
    }

    @FXML
    private void exportContrahentsCsv() {
        Path selectedPath = chooseCsvToSave("Eksportuj Contrahents do CSV", "contrahents-export.csv");
        if (selectedPath == null) {
            return;
        }

        try {
            Path exported = contrahentCsvExportService.export(selectedPath);
            contrahentsCsvStatusLabel.setText("Eksport Contrahents zakończony powodzeniem: " + exported + ". Format pozostaje lokalnym standardem Settings -> Import / Eksport CSV.");
            DialogUtil.showSuccess("Contrahents zostali wyeksportowani do pliku:\n" + exported);
        } catch (Exception e) {
            e.printStackTrace();
            contrahentsCsvStatusLabel.setText("Nie udało się wyeksportować Contrahents do CSV.");
            DialogUtil.showError("Eksport Contrahents CSV", "Nie udało się wyeksportować kontrahentów do pliku CSV.");
        }
    }


    @FXML
    private void previewDocumentsCsvImport() {
        Path selectedPath = chooseCsvToOpen("Wybierz plik CSV dokumentów");
        if (selectedPath == null) {
            return;
        }

        try {
            var result = documentCsvImportService.preview(selectedPath);
            documentsCsvStatusLabel.setText(buildDocumentsPreviewStatus(result));
            documentsCsvPreviewArea.setText(buildDocumentsPreviewText(result));
        } catch (Exception e) {
            e.printStackTrace();
            documentsCsvStatusLabel.setText("Nie udało się przygotować preview importu Documents.");
            DialogUtil.showError("Preview importu Documents CSV", "Nie udało się odczytać ani przeanalizować pliku CSV dokumentów.");
        }
    }

    @FXML
    private void exportDocumentsCsv() {
        Path selectedPath = chooseCsvToSave("Eksportuj Documents do CSV", "documents-export.csv");
        if (selectedPath == null) {
            return;
        }

        try {
            Path exported = documentCsvExportService.export(selectedPath);
            documentsCsvStatusLabel.setText("Eksport Documents zakończony powodzeniem: " + exported + ". Format pozostaje lokalnym standardem Settings -> Import / Eksport CSV.");
            DialogUtil.showSuccess("Documents zostały wyeksportowane do pliku:\n" + exported);
        } catch (Exception e) {
            e.printStackTrace();
            documentsCsvStatusLabel.setText("Nie udało się wyeksportować Documents do CSV.");
            DialogUtil.showError("Eksport Documents CSV", "Nie udało się wyeksportować dokumentów do pliku CSV.");
        }
    }


    @FXML
    private void clearPlantsCsvPreview() {
        resetPlantsCsvPreview();
        if (plantsCsvStatusLabel != null) {
            plantsCsvStatusLabel.setText("Preview Plants zostało wyczyszczone. Wybierz plik CSV, aby uruchomić analizę ponownie.");
        }
    }

    @FXML
    private void clearContrahentsCsvPreview() {
        resetContrahentsCsvPreview();
        if (contrahentsCsvStatusLabel != null) {
            contrahentsCsvStatusLabel.setText("Preview Contrahents zostało wyczyszczone. Wybierz plik CSV, aby uruchomić analizę ponownie.");
        }
    }

    @FXML
    private void clearDocumentsCsvPreview() {
        resetDocumentsCsvPreview();
        if (documentsCsvStatusLabel != null) {
            documentsCsvStatusLabel.setText("Preview Documents zostało wyczyszczone. Wybierz plik CSV, aby uruchomić analizę ponownie.");
        }
    }

    private Path chooseCsvToOpen(String title) {
        FileChooser chooser = createCsvFileChooser(title, null);
        Window window = getOwningWindow();
        File selectedFile = chooser.showOpenDialog(window);
        return selectedFile == null ? null : selectedFile.toPath();
    }

    private Path chooseCsvToSave(String title, String initialFileName) {
        FileChooser chooser = createCsvFileChooser(title, initialFileName);
        Window window = getOwningWindow();
        File selectedFile = chooser.showSaveDialog(window);
        return selectedFile == null ? null : selectedFile.toPath();
    }


    private Window getOwningWindow() {
        if (previewLabel != null && previewLabel.getScene() != null) {
            return previewLabel.getScene().getWindow();
        }
        if (backupStatusLabel != null && backupStatusLabel.getScene() != null) {
            return backupStatusLabel.getScene().getWindow();
        }
        return null;
    }

    private FileChooser createCsvFileChooser(String title, String initialFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki CSV", "*.csv"));
        if (initialFileName != null && !initialFileName.isBlank()) {
            chooser.setInitialFileName(initialFileName);
        }
        return chooser;
    }


    private String buildPlantsPreviewStatus(com.egen.fitogen.dto.PlantImportPreviewResult result) {
        return "Plants CSV — łącznie: " + result.getTotalRowsCount()
                + ", nowych: " + result.getNewRowsCount()
                + ", istniejących: " + result.getMatchingExistingCount()
                + ", duplikatów w pliku: " + result.getDuplicateInFileCount()
                + ", błędnych: " + result.getInvalidRowsCount()
                + ". Ocena: " + buildPlantsImportReadiness(result);
    }

    private String buildContrahentsPreviewStatus(com.egen.fitogen.dto.ContrahentImportPreviewResult result) {
        return "Contrahents CSV — łącznie: " + result.getTotalRowsCount()
                + ", nowych: " + result.getNewRowsCount()
                + ", istniejących: " + result.getMatchingExistingCount()
                + ", duplikatów w pliku: " + result.getDuplicateInFileCount()
                + ", błędnych: " + result.getInvalidRowsCount()
                + ". Ocena: " + buildContrahentsImportReadiness(result);
    }

    private String buildDocumentsPreviewStatus(com.egen.fitogen.dto.DocumentImportPreviewResult result) {
        return "Documents CSV — łącznie wierszy: " + result.getTotalRowsCount()
                + ", nowych wierszy: " + result.getNewRowsCount()
                + ", istniejących numerów: " + result.getMatchingExistingCount()
                + ", duplikatów w pliku: " + result.getDuplicateInFileCount()
                + ", błędnych: " + result.getInvalidRowsCount()
                + ", nowych dokumentów: " + result.getDocumentCount()
                + ". Ocena: " + buildDocumentsImportReadiness(result);
    }

    private void resetPlantsCsvPreview() {
        if (plantsCsvPreviewArea != null) {
            plantsCsvPreviewArea.setText(UiTextUtil.buildEmptyPreviewText(
                    "Plants",
                    "Po uruchomieniu analizy zobaczysz tutaj podsumowanie pliku, nagłówki oraz próbkę wierszy."
            ));
        }
    }

    private void resetContrahentsCsvPreview() {
        if (contrahentsCsvPreviewArea != null) {
            contrahentsCsvPreviewArea.setText(UiTextUtil.buildEmptyPreviewText(
                    "Contrahents",
                    "Po uruchomieniu analizy zobaczysz tutaj podsumowanie pliku, nagłówki oraz próbkę wierszy."
            ));
        }
    }

    private void resetDocumentsCsvPreview() {
        if (documentsCsvPreviewArea != null) {
            documentsCsvPreviewArea.setText(UiTextUtil.buildEmptyPreviewText(
                    "Documents",
                    "Po uruchomieniu analizy zobaczysz tutaj podsumowanie pliku, nagłówki oraz próbkę wierszy dokumentów i pozycji."
            ));
        }
    }

    private String buildPlantsPreviewText(com.egen.fitogen.dto.PlantImportPreviewResult result) {
        StringBuilder builder = new StringBuilder();
        appendPreviewFileSummary(builder,
                result.getSourceName(),
                result.getDelimiter(),
                result.getResolvedHeaders(),
                List.of(
                        new PreviewSummaryRow("Łącznie wierszy", result.getTotalRowsCount()),
                        new PreviewSummaryRow("Nowe rekordy", result.getNewRowsCount()),
                        new PreviewSummaryRow("Istniejące rekordy", result.getMatchingExistingCount()),
                        new PreviewSummaryRow("Duplikaty w pliku", result.getDuplicateInFileCount()),
                        new PreviewSummaryRow("Błędne wiersze", result.getInvalidRowsCount())
                ));

        appendPlantsValidationSection(builder, result);
        UiTextUtil.appendSectionHeader(builder, "PRÓBKA WIERSZY");
        int previewLimit = Math.min(result.getRows().size(), 10);
        for (int i = 0; i < previewLimit; i++) {
            var row = result.getRows().get(i);
            builder.append("#").append(row.getRowNumber())
                    .append(" [").append(row.getStatus()).append("] ")
                    .append(safe(row.getSpecies()));
            if (!safe(row.getVariety()).isBlank()) {
                builder.append(" | odmiana: ").append(safe(row.getVariety()));
            }
            if (!safe(row.getRootstock()).isBlank()) {
                builder.append(" | podkładka: ").append(safe(row.getRootstock()));
            }
            if (!safe(row.getEppoCode()).isBlank()) {
                builder.append(" | EPPO: ").append(safe(row.getEppoCode()));
            }
            builder.append(" | paszport: ").append(row.isPassportRequired() ? "tak" : "nie")
                    .append(" | widoczność: ").append(safe(row.getVisibilityStatus()));
            if (row.getMessage() != null && !row.getMessage().isBlank()) {
                builder.append(" | uwaga: ").append(row.getMessage());
            }
            builder.append(UiTextUtil.NL);
        }

        appendPlantIssuesSection(builder, result);
        return builder.toString();
    }

    private String buildContrahentsPreviewText(com.egen.fitogen.dto.ContrahentImportPreviewResult result) {
        StringBuilder builder = new StringBuilder();
        appendPreviewFileSummary(builder,
                result.getSourceName(),
                result.getDelimiter(),
                result.getResolvedHeaders(),
                List.of(
                        new PreviewSummaryRow("Łącznie wierszy", result.getTotalRowsCount()),
                        new PreviewSummaryRow("Nowe rekordy", result.getNewRowsCount()),
                        new PreviewSummaryRow("Istniejące rekordy", result.getMatchingExistingCount()),
                        new PreviewSummaryRow("Duplikaty w pliku", result.getDuplicateInFileCount()),
                        new PreviewSummaryRow("Błędne wiersze", result.getInvalidRowsCount())
                ));

        appendContrahentsValidationSection(builder, result);
        UiTextUtil.appendSectionHeader(builder, "PRÓBKA WIERSZY");
        int previewLimit = Math.min(result.getRows().size(), 10);
        for (int i = 0; i < previewLimit; i++) {
            var row = result.getRows().get(i);
            builder.append("#").append(row.getRowNumber())
                    .append(" [").append(row.getStatus()).append("] ")
                    .append(safe(row.getName()));
            if (!safe(row.getCountry()).isBlank()) {
                builder.append(" | kraj: ").append(safe(row.getCountry()));
            }
            if (!safe(row.getCountryCode()).isBlank()) {
                builder.append(" | kod: ").append(safe(row.getCountryCode()));
            }
            if (!safe(row.getCity()).isBlank()) {
                builder.append(" | miasto: ").append(safe(row.getCity()));
            }
            builder.append(" | dostawca: ").append(row.isSupplier() ? "tak" : "nie")
                    .append(" | odbiorca: ").append(row.isClient() ? "tak" : "nie");
            if (row.getMessage() != null && !row.getMessage().isBlank()) {
                builder.append(" | uwaga: ").append(row.getMessage());
            }
            builder.append(UiTextUtil.NL);
        }

        appendContrahentIssuesSection(builder, result);
        return builder.toString();
    }

    private void appendPlantsValidationSection(StringBuilder builder, com.egen.fitogen.dto.PlantImportPreviewResult result) {
        UiTextUtil.appendSectionHeader(builder, "OCENA WALIDACJI");
        UiTextUtil.appendSummaryLine(builder, "Gotowość importu", buildPlantsImportReadiness(result));
        UiTextUtil.appendSummaryLine(builder, "Rekomendacja", buildPlantsImportRecommendation(result));
        UiTextUtil.appendEmptyLine(builder);
    }

    private void appendContrahentsValidationSection(StringBuilder builder, com.egen.fitogen.dto.ContrahentImportPreviewResult result) {
        UiTextUtil.appendSectionHeader(builder, "OCENA WALIDACJI");
        UiTextUtil.appendSummaryLine(builder, "Gotowość importu", buildContrahentsImportReadiness(result));
        UiTextUtil.appendSummaryLine(builder, "Rekomendacja", buildContrahentsImportRecommendation(result));
        UiTextUtil.appendEmptyLine(builder);
    }

    private void appendContrahentIssuesSection(StringBuilder builder, com.egen.fitogen.dto.ContrahentImportPreviewResult result) {
        List<String> problemRows = new ArrayList<>();
        for (var row : result.getRows()) {
            if (row.getMessage() != null && !row.getMessage().isBlank()) {
                problemRows.add("#" + row.getRowNumber() + " [" + row.getStatus() + "] " + row.getMessage());
            }
            if (problemRows.size() >= 5) {
                break;
            }
        }

        UiTextUtil.appendIssuesSection(builder, "NAJWAŻNIEJSZE UWAGI", problemRows);
    }

    private void appendPlantIssuesSection(StringBuilder builder, com.egen.fitogen.dto.PlantImportPreviewResult result) {
        List<String> problemRows = new ArrayList<>();
        for (var row : result.getRows()) {
            if (row.getMessage() != null && !row.getMessage().isBlank()) {
                problemRows.add("#" + row.getRowNumber() + " [" + row.getStatus() + "] " + row.getMessage());
            }
            if (problemRows.size() >= 5) {
                break;
            }
        }

        UiTextUtil.appendIssuesSection(builder, "NAJWAŻNIEJSZE UWAGI", problemRows);
    }

    private void appendPreviewFileSummary(StringBuilder builder,
                                          String sourceName,
                                          char delimiter,
                                          List<String> resolvedHeaders,
                                          List<PreviewSummaryRow> summaryRows) {
        UiTextUtil.appendSectionHeader(builder, "PODSUMOWANIE PLIKU");
        UiTextUtil.appendSummaryLine(builder, "Źródło", sourceName);
        UiTextUtil.appendSummaryLine(builder, "Separator", printableDelimiter(delimiter));
        UiTextUtil.appendSummaryLine(builder, "Nagłówki", String.join(", ", resolvedHeaders));
        for (PreviewSummaryRow summaryRow : summaryRows) {
            UiTextUtil.appendSummaryLine(builder, summaryRow.label(), summaryRow.value());
        }
        UiTextUtil.appendEmptyLine(builder);
    }

    private record PreviewSummaryRow(String label, Object value) {
    }

    private String buildDocumentsPreviewText(com.egen.fitogen.dto.DocumentImportPreviewResult result) {
        StringBuilder builder = new StringBuilder();
        appendPreviewFileSummary(builder,
                result.getSourceName(),
                result.getDelimiter(),
                result.getResolvedHeaders(),
                List.of(
                        new PreviewSummaryRow("Łącznie wierszy", result.getTotalRowsCount()),
                        new PreviewSummaryRow("Nowe dokumenty", result.getDocumentCount()),
                        new PreviewSummaryRow("Nowe wiersze", result.getNewRowsCount()),
                        new PreviewSummaryRow("Istniejące numery dokumentów", result.getMatchingExistingCount()),
                        new PreviewSummaryRow("Duplikaty w pliku", result.getDuplicateInFileCount()),
                        new PreviewSummaryRow("Błędne wiersze", result.getInvalidRowsCount())
                ));

        appendDocumentsValidationSection(builder, result);
        UiTextUtil.appendSectionHeader(builder, "PRÓBKA WIERSZY");
        int previewLimit = Math.min(result.getRows().size(), 10);
        for (int i = 0; i < previewLimit; i++) {
            var row = result.getRows().get(i);
            builder.append("#").append(row.getRowNumber())
                    .append(" [").append(row.getRowStatus()).append("] ")
                    .append(safe(row.getDocumentNumber()))
                    .append(" | typ: ").append(safe(row.getDocumentType()))
                    .append(" | data: ").append(safe(row.getIssueDate()))
                    .append(" | status: ").append(safe(row.getStatus()))
                    .append(" | pozycja: ").append(row.getLineNo())
                    .append(" | partia: ").append(fallback(safe(row.getPlantBatchNumber()), fallback(safe(row.getPlantBatchId()), "[brak partii]")))
                    .append(" | ilość: ").append(row.getQty())
                    .append(" | paszport: ").append(row.isPassportRequired() ? "tak" : "nie");
            if (!safe(row.getContrahentName()).isBlank()) {
                builder.append(" | kontrahent: ").append(safe(row.getContrahentName()));
            }
            if (row.getMessage() != null && !row.getMessage().isBlank()) {
                builder.append(" | uwaga: ").append(row.getMessage());
            }
            builder.append(UiTextUtil.NL);
        }

        appendDocumentIssuesSection(builder, result);
        return builder.toString();
    }

    private void appendDocumentsValidationSection(StringBuilder builder, com.egen.fitogen.dto.DocumentImportPreviewResult result) {
        UiTextUtil.appendSectionHeader(builder, "OCENA WALIDACJI");
        UiTextUtil.appendSummaryLine(builder, "Gotowość importu", buildDocumentsImportReadiness(result));
        UiTextUtil.appendSummaryLine(builder, "Rekomendacja", buildDocumentsImportRecommendation(result));
        UiTextUtil.appendEmptyLine(builder);
    }

    private void appendDocumentIssuesSection(StringBuilder builder, com.egen.fitogen.dto.DocumentImportPreviewResult result) {
        List<String> problemRows = new ArrayList<>();
        for (var row : result.getRows()) {
            if (row.getMessage() != null && !row.getMessage().isBlank()) {
                problemRows.add("#" + row.getRowNumber() + " [" + row.getRowStatus() + "] " + row.getMessage());
            }
            if (problemRows.size() >= 5) {
                break;
            }
        }

        UiTextUtil.appendIssuesSection(builder, "NAJWAŻNIEJSZE UWAGI", problemRows);
    }

    private String buildDocumentsImportReadiness(com.egen.fitogen.dto.DocumentImportPreviewResult result) {
        if (result.getTotalRowsCount() == 0) {
            return "plik nie zawiera danych do analizy";
        }
        if (result.getInvalidRowsCount() > 0) {
            return "wymaga korekty przed importem";
        }
        if (result.getDuplicateInFileCount() > 0) {
            return "wymaga usunięcia duplikatów z pliku";
        }
        if (result.getDocumentCount() == 0) {
            return "brak nowych dokumentów do importu";
        }
        return "gotowy do bezpiecznego importu preview";
    }

    private String buildDocumentsImportRecommendation(com.egen.fitogen.dto.DocumentImportPreviewResult result) {
        if (result.getTotalRowsCount() == 0) {
            return "Przygotuj plik z numerem dokumentu, typem, datą i pozycjami dokumentu.";
        }
        if (result.getInvalidRowsCount() > 0) {
            return "Najpierw popraw błędne daty, pozycje lub brakujące powiązania z partiami.";
        }
        if (result.getDuplicateInFileCount() > 0) {
            return "Usuń zduplikowane pozycje tego samego dokumentu przed dalszym użyciem pliku.";
        }
        if (result.getDocumentCount() == 0) {
            return "Plik wygląda poprawnie, ale nie wnosi nowych numerów dokumentów względem aktualnej bazy.";
        }
        return "Format wygląda spójnie z lokalnym standardem Documents CSV w Settings.";
    }

    private String buildPlantsImportReadiness(com.egen.fitogen.dto.PlantImportPreviewResult result) {
        if (result.getTotalRowsCount() == 0) {
            return "plik nie zawiera danych do analizy";
        }
        if (result.getInvalidRowsCount() > 0) {
            return "wymaga korekty przed importem";
        }
        if (result.getDuplicateInFileCount() > 0) {
            return "wymaga usunięcia duplikatów z pliku";
        }
        if (result.getNewRowsCount() == 0) {
            return "brak nowych rekordów do importu";
        }
        return "gotowy do bezpiecznego importu preview";
    }

    private String buildContrahentsImportReadiness(com.egen.fitogen.dto.ContrahentImportPreviewResult result) {
        if (result.getTotalRowsCount() == 0) {
            return "plik nie zawiera danych do analizy";
        }
        if (result.getInvalidRowsCount() > 0) {
            return "wymaga korekty przed importem";
        }
        if (result.getDuplicateInFileCount() > 0) {
            return "wymaga usunięcia duplikatów z pliku";
        }
        if (result.getNewRowsCount() == 0) {
            return "brak nowych rekordów do importu";
        }
        return "gotowy do bezpiecznego importu preview";
    }

    private String buildPlantsImportRecommendation(com.egen.fitogen.dto.PlantImportPreviewResult result) {
        if (result.getTotalRowsCount() == 0) {
            return "Sprawdź, czy plik zawiera nagłówek i co najmniej jeden wiersz danych.";
        }
        if (result.getInvalidRowsCount() > 0) {
            return "Najpierw popraw wiersze błędne, szczególnie brak gatunku i nieprawidłowy visibilityStatus.";
        }
        if (result.getDuplicateInFileCount() > 0) {
            return "Usuń duplikaty w samym pliku CSV, aby import był przewidywalny.";
        }
        if (result.getNewRowsCount() == 0) {
            return "Plik nie wniesie nowych roślin. Zweryfikuj, czy to właściwy eksport lub zestaw danych.";
        }
        return "Preview wygląda spójnie. Zachowaj ten sam standard kolumn w kolejnych plikach lokalnych CSV.";
    }

    private String buildContrahentsImportRecommendation(com.egen.fitogen.dto.ContrahentImportPreviewResult result) {
        if (result.getTotalRowsCount() == 0) {
            return "Sprawdź, czy plik zawiera nagłówek i co najmniej jeden wiersz danych.";
        }
        if (result.getInvalidRowsCount() > 0) {
            return "Najpierw popraw wiersze błędne, szczególnie brak nazwy oraz niespójne pary kraj i kod kraju.";
        }
        if (result.getDuplicateInFileCount() > 0) {
            return "Usuń duplikaty w samym pliku CSV, aby import był przewidywalny.";
        }
        if (result.getNewRowsCount() == 0) {
            return "Plik nie wniesie nowych kontrahentów. Zweryfikuj, czy to właściwy zestaw danych.";
        }
        return "Preview wygląda spójnie. Zachowaj ten sam standard kolumn w kolejnych plikach lokalnych CSV.";
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String printableDelimiter(char delimiter) {
        if (delimiter == '\t') {
            return "TAB";
        }
        if (delimiter == ';') {
            return ";";
        }
        if (delimiter == ',') {
            return ",";
        }
        return String.valueOf(delimiter);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
