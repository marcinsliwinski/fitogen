package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.config.DatabaseConfig;
import com.egen.fitogen.domain.NumberingConfig;
import com.egen.fitogen.dto.ContrahentImportPreviewResult;
import com.egen.fitogen.dto.EppoDictionaryImportPreviewResult;
import com.egen.fitogen.dto.PlantBatchImportPreviewResult;
import com.egen.fitogen.dto.CsvImportExecutionResult;
import com.egen.fitogen.dto.PlantImportPreviewResult;
import com.egen.fitogen.domain.NumberingSectionType;
import com.egen.fitogen.domain.NumberingType;
import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.DocumentType;
import com.egen.fitogen.model.IssuerProfile;
import com.egen.fitogen.dto.DatabaseProfileInfo;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.AppUserService;
import com.egen.fitogen.service.AuditLogService;
import com.egen.fitogen.service.BackupService;
import com.egen.fitogen.service.DatabaseProfileService;
import com.egen.fitogen.service.ContrahentCsvExportService;
import com.egen.fitogen.service.ContrahentCsvImportService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.DocumentCsvExportService;
import com.egen.fitogen.service.DocumentCsvImportService;
import com.egen.fitogen.service.DocumentTypeService;
import com.egen.fitogen.service.EppoDictionaryCsvExportService;
import com.egen.fitogen.service.EppoDictionaryCsvImportService;
import com.egen.fitogen.service.NumberingConfigService;
import com.egen.fitogen.service.PlantCsvExportService;
import com.egen.fitogen.service.PlantBatchCsvExportService;
import com.egen.fitogen.service.PlantBatchCsvImportService;
import com.egen.fitogen.service.PlantCsvImportService;
import com.egen.fitogen.ui.util.ComboBoxAutoComplete;
import com.egen.fitogen.ui.util.CountryDirectory;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.router.ViewManager;
import com.egen.fitogen.ui.util.ValidationUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.egen.fitogen.ui.util.UiTextUtil;
import com.egen.fitogen.database.SqliteDocumentItemRepository;
import com.egen.fitogen.database.SqliteDocumentRepository;

public class SettingsController {

    private static final DateTimeFormatter BACKUP_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static String pendingTabTitle;
    private static boolean pendingCreateDatabaseFlow;

    @FXML private TabPane settingsTabPane;
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
    @FXML private Label syncScopeLabel;
    @FXML private Button synchronizeCounterButton;
    @FXML private ComboBox<DatabaseProfileInfo> databaseProfileBox;
    @FXML private TextField newDatabaseNameField;
    @FXML private Label databaseFolderLabel;
    @FXML private Label databasePathLabel;
    @FXML private Label databaseProfileStatusLabel;
    @FXML private Label databaseMissingStatusLabel;
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
    @FXML private Label customCountryUsageLabel;
    @FXML private Label customCountryUsageCountsLabel;
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
    @FXML private CheckBox issuerNoStreetCheckBox;
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
    @FXML private Button plantsCsvApplyButton;
    @FXML private Label contrahentsCsvColumnsLabel;
    @FXML private Label contrahentsCsvStatusLabel;
    @FXML private TextArea contrahentsCsvPreviewArea;
    @FXML private Button contrahentsCsvApplyButton;
    @FXML private Label documentsCsvColumnsLabel;
    @FXML private Label documentsCsvStatusLabel;
    @FXML private TextArea documentsCsvPreviewArea;
    @FXML private Button documentsCsvApplyButton;

    @FXML private Label plantBatchesCsvColumnsLabel;
    @FXML private Label plantBatchesCsvStatusLabel;
    @FXML private TextArea plantBatchesCsvPreviewArea;
    @FXML private Button plantBatchesCsvApplyButton;

    @FXML private Label eppoDictionaryCsvColumnsLabel;
    @FXML private Label eppoDictionaryCsvStatusLabel;
    @FXML private TextArea eppoDictionaryCsvPreviewArea;
    @FXML private Button eppoDictionaryCsvApplyButton;

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
    private final DatabaseProfileService databaseProfileService = AppContext.getDatabaseProfileService();
    private final DocumentTypeService documentTypeService = AppContext.getDocumentTypeService();
    private final AppUserService appUserService = AppContext.getAppUserService();
    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final AuditLogService auditLogService = AppContext.getAuditLogService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();
    private final PlantCsvImportService plantCsvImportService = new PlantCsvImportService(AppContext.getPlantService(), AppContext.getAppSettingsService());
    private final PlantCsvExportService plantCsvExportService = new PlantCsvExportService(AppContext.getPlantService());
    private final ContrahentCsvImportService contrahentCsvImportService = new ContrahentCsvImportService(AppContext.getContrahentService(), AppContext.getCountryDirectoryService());
    private final ContrahentCsvExportService contrahentCsvExportService = new ContrahentCsvExportService(AppContext.getContrahentService());
    private final DocumentCsvImportService documentCsvImportService = new DocumentCsvImportService(AppContext.getDocumentService(), AppContext.getContrahentService(), AppContext.getPlantBatchService(), AppContext.getAuditLogService());
    private final DocumentCsvExportService documentCsvExportService = new DocumentCsvExportService(new SqliteDocumentRepository(), new SqliteDocumentItemRepository(), AppContext.getContrahentService(), AppContext.getPlantBatchService());
    private final PlantBatchCsvImportService plantBatchCsvImportService = new PlantBatchCsvImportService(AppContext.getPlantBatchService(), AppContext.getPlantService(), AppContext.getContrahentService());
    private final PlantBatchCsvExportService plantBatchCsvExportService = new PlantBatchCsvExportService(AppContext.getPlantBatchService(), AppContext.getPlantService(), AppContext.getContrahentService());

    private final EppoDictionaryCsvImportService eppoDictionaryCsvImportService = new EppoDictionaryCsvImportService(
            AppContext.getEppoCodeService(),
            AppContext.getEppoZoneService(),
            AppContext.getEppoCodeSpeciesLinkService(),
            AppContext.getEppoCodeZoneLinkService()
    );
    private final EppoDictionaryCsvExportService eppoDictionaryCsvExportService = new EppoDictionaryCsvExportService(
            AppContext.getEppoCodeService(),
            AppContext.getEppoCodeSpeciesLinkService(),
            AppContext.getEppoCodeZoneLinkService()
    );

    private boolean loading;
    private boolean updatingDefaultUserSelection;
    private boolean updatingIssuerCountryFields;
    private boolean loadingDatabaseProfileSelection;
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
    private PlantImportPreviewResult lastPlantsCsvPreviewResult;
    private ContrahentImportPreviewResult lastContrahentsCsvPreviewResult;
    private com.egen.fitogen.dto.DocumentImportPreviewResult lastDocumentsCsvPreviewResult;
    private PlantBatchImportPreviewResult lastPlantBatchesCsvPreviewResult;
    private EppoDictionaryImportPreviewResult lastEppoDictionaryCsvPreviewResult;


    public static void requestInitialTab(String tabTitle) {
        pendingTabTitle = tabTitle;
    }

    public static void requestCreateNewDatabaseFlow() {
        pendingTabTitle = "Kopia zapasowa i baza";
        pendingCreateDatabaseFlow = true;
    }

    @FXML
    public void initialize() {
        configureTypeBoxes();
        configureListeners();
        configureUserControls();
        configureCustomCountryControls();
        configureIssuerCountryControls();
        configureDocumentTypeControls();
        configureAuditLogControls();
        configureDatabaseProfileControls();

        numberingTypeBox.getItems().setAll(NumberingType.values());
        numberingTypeBox.setValue(NumberingType.DOCUMENT);

        loadDatabaseProfileOverview();

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
        applyPendingNavigation();
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

        ComboBoxAutoComplete.bindEditable(issuerCountryField, countryDirectoryService::getCountries);
        ComboBoxAutoComplete.bindEditable(issuerCountryCodeField, countryDirectoryService::getCodes);

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
        auditLogTable.setPlaceholder(new Label("Brak wpisów dziennika audytu do wyświetlenia."));
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

        if (customCountryEntriesList != null) {
            customCountryEntriesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                handleCountryDictionarySelectionChange(newVal);
            });
        }

        if (allCountryEntriesList != null) {
            allCountryEntriesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (customCountryEntriesList == null || customCountryEntriesList.getSelectionModel().getSelectedItem() == null) {
                    handleCountryDictionarySelectionChange(newVal);
                }
            });
        }

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

    private void handleCountryDictionarySelectionChange(CountryDirectory.CountryEntry selectedEntry) {
        boolean customEntrySelected = isCustomCountryEntry(selectedEntry);
        editingCustomCountryEntry = customEntrySelected ? selectedEntry : null;

        if (customCountryNameField != null) {
            customCountryNameField.setText(customEntrySelected ? safe(selectedEntry.country()) : "");
        }
        if (customCountryCodeField != null) {
            customCountryCodeField.setText(customEntrySelected ? safe(selectedEntry.countryCode()) : "");
        }

        updateCountryDictionaryStatus();
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
        String selectedSharedKey = selectedShared == null ? selectedCustomKey : buildCustomCountryKey(selectedShared);

        customCountryMasterData.setAll(countryDirectoryService.getCustomEntries());
        allCountryMasterData.setAll(countryDirectoryService.getEntries());
        applyCustomCountryFilter(customCountrySearchField == null ? "" : customCountrySearchField.getText());

        if (customCountryEntriesList != null) {
            restoreCustomCountrySelection(selectedCustomKey);
        }

        restoreSharedCountrySelection(selectedSharedKey);

        if (customCountryEntriesList == null && allCountryEntriesList != null) {
            CountryDirectory.CountryEntry selectedEntry = allCountryEntriesList.getSelectionModel().getSelectedItem();
            if (selectedEntry == null && allCountryFilteredData != null && !allCountryFilteredData.isEmpty()) {
                selectedEntry = allCountryFilteredData.get(0);
                allCountryEntriesList.getSelectionModel().select(selectedEntry);
            }
            handleCountryDictionarySelectionChange(selectedEntry);
        }

        updateCountryDictionaryStatus();
    }

    @FXML
    private void saveCustomCountryEntry() {
        try {
            String country = ValidationUtil.requireText(customCountryNameField.getText(), "Kraj");
            String code = ValidationUtil.requireText(customCountryCodeField.getText(), "Kod kraju").toUpperCase();
            CountryDirectory.CountryEntry previousEntry = editingCustomCountryEntry;
            boolean editingExistingEntry = previousEntry != null;

            if (editingExistingEntry && isCustomCountryEntryChanged(previousEntry, country, code)) {
                List<String> usageDetails = findCountryEntryUsageDetails(previousEntry);
                if (!usageDetails.isEmpty()) {
                    DialogUtil.showWarning(
                            "Zmiana zablokowana",
                            buildCountryEntryUsageWarning(previousEntry, usageDetails)
                    );
                    return;
                }

                countryDirectoryService.deleteCustomEntry(
                        previousEntry.country(),
                        previousEntry.countryCode()
                );
            }

            countryDirectoryService.saveCustomEntry(country, code);
            loadCustomCountryEntries();
            refreshSharedCountryCombos();
            selectCustomCountryEntry(country, code);
            DialogUtil.showSuccess(editingExistingEntry
                    ? "Wpis słownika krajów został zaktualizowany."
                    : "Wpis słownika krajów został dodany.");
        } catch (IllegalArgumentException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu", "Nie udało się zapisać wpisu słownika krajów.");
        }
    }

    @FXML
    private void deleteCustomCountryEntry() {
        CountryDirectory.CountryEntry selected = getSelectedEditableCustomCountryEntry();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz własny wpis słownika krajów do usunięcia.");
            return;
        }

        List<String> usageDetails = findCountryEntryUsageDetails(selected);
        if (!usageDetails.isEmpty()) {
            DialogUtil.showWarning(
                    "Usunięcie zablokowane",
                    buildCountryEntryUsageWarning(selected, usageDetails)
            );
            return;
        }

        if (!DialogUtil.confirmDelete(selected.country() + " (" + selected.countryCode() + ")")) {
            return;
        }

        try {
            countryDirectoryService.deleteCustomEntry(selected.country(), selected.countryCode());
            loadCustomCountryEntries();
            refreshSharedCountryCombos();
            clearCustomCountryEntryForm();
            DialogUtil.showSuccess("Wpis słownika krajów został usunięty.");
        } catch (IllegalStateException e) {
            DialogUtil.showWarning("Nie można usunąć wpisu", e.getMessage());
        } catch (Exception e) {
            DialogUtil.showError("Błąd usuwania", "Nie udało się usunąć wpisu słownika krajów.");
        }
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
        if (customCountryEntriesList == null && allCountryEntriesList != null) {
            allCountryEntriesList.getSelectionModel().clearSelection();
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
        updateCustomCountryUsageStatus();
        if (countryDictionaryStatusLabel == null) {
            return;
        }

        int customCount = countryDirectoryService.getCustomEntries().size();
        int totalCount = countryDirectoryService.getEntries().size();
        int visibleSharedCount = allCountryFilteredData == null ? totalCount : allCountryFilteredData.size();
        String filter = customCountrySearchField == null ? "" : safe(customCountrySearchField.getText());
        String filterSuffix = UiTextUtil.buildQuotedFilterSuffix("Filtr", filter);
        countryDictionaryStatusLabel.setText(
                "Wspólny słownik zawiera " + totalCount + " pozycji, w tym " + customCount
                        + " wpisów własnych. Widoczne po filtrze: " + visibleSharedCount
                        + ". Jedna lista pokazuje cały katalog razem z oznaczeniem źródła rekordu."
                        + " Jest używany przez Kontrahentów, EPPO i dane podmiotu."
                        + filterSuffix
        );

        if (countryDictionarySummaryLabel != null) {
            CountryDirectory.CountryEntry selectedCustom = getSelectedEditableCustomCountryEntry();
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
        return "Wybrana pozycja wspólnego słownika: " + buildSharedCountryDisplay(entry)
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

    private void selectCustomCountryEntry(String country, String code) {
        String targetKey = buildCustomCountryKey(new CountryDirectory.CountryEntry(country, code));

        if (customCountryEntriesList != null) {
            for (CountryDirectory.CountryEntry entry : customCountryEntriesList.getItems()) {
                if (buildCustomCountryKey(entry).equalsIgnoreCase(targetKey)) {
                    customCountryEntriesList.getSelectionModel().select(entry);
                    return;
                }
            }
        }

        if (allCountryEntriesList != null) {
            for (CountryDirectory.CountryEntry entry : allCountryEntriesList.getItems()) {
                if (buildCustomCountryKey(entry).equalsIgnoreCase(targetKey)) {
                    allCountryEntriesList.getSelectionModel().select(entry);
                    handleCountryDictionarySelectionChange(entry);
                    return;
                }
            }
        }
    }

    private boolean isCustomCountryEntryChanged(CountryDirectory.CountryEntry previousEntry, String country, String code) {
        if (previousEntry == null) {
            return false;
        }
        String previousKey = buildCustomCountryKey(previousEntry).toLowerCase();
        String newKey = buildCustomCountryKey(new CountryDirectory.CountryEntry(country, code)).toLowerCase();
        return !previousKey.equals(newKey);
    }

    private List<String> findCountryEntryUsageDetails(CountryDirectory.CountryEntry entry) {
        List<String> details = new ArrayList<>();
        CountryEntryUsageSnapshot usageSnapshot = getCountryEntryUsageSnapshot(entry);
        if (usageSnapshot == null) {
            return details;
        }

        if (usageSnapshot.usedByIssuer()) {
            details.add("dane wystawcy");
        }

        List<String> matchingContrahents = usageSnapshot.matchingContrahents();
        if (!matchingContrahents.isEmpty()) {
            if (matchingContrahents.size() == 1) {
                details.add("kontrahent: " + matchingContrahents.get(0));
            } else if (matchingContrahents.size() <= 3) {
                details.add("kontrahenci: " + String.join(", ", matchingContrahents));
            } else {
                List<String> preview = matchingContrahents.subList(0, 3);
                details.add("kontrahenci: " + String.join(", ", preview)
                        + " oraz " + (matchingContrahents.size() - preview.size()) + " kolejnych");
            }
        }

        return details;
    }

    private CountryEntryUsageSnapshot getCountryEntryUsageSnapshot(CountryDirectory.CountryEntry entry) {
        if (entry == null) {
            return null;
        }

        IssuerProfile issuerProfile = appSettingsService.getIssuerProfile();
        boolean usedByIssuer = matchesCountryEntryUsage(
                issuerProfile == null ? null : issuerProfile.getCountry(),
                issuerProfile == null ? null : issuerProfile.getCountryCode(),
                entry
        );

        List<String> matchingContrahents = AppContext.getContrahentService().getAllContrahents().stream()
                .filter(contrahent -> matchesCountryEntryUsage(contrahent.getCountry(), contrahent.getCountryCode(), entry))
                .map(this::buildContrahentUsageLabel)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new CountryEntryUsageSnapshot(usedByIssuer, matchingContrahents);
    }

    private boolean matchesCountryEntryUsage(String country, String code, CountryDirectory.CountryEntry entry) {
        String normalizedEntryCountry = normalizeCountryValue(entry == null ? null : entry.country());
        String normalizedEntryCode = normalizeCountryCodeValue(entry == null ? null : entry.countryCode());
        String normalizedCountry = normalizeCountryValue(country);
        String normalizedCode = normalizeCountryCodeValue(code);

        if (normalizedEntryCountry == null && normalizedEntryCode == null) {
            return false;
        }

        boolean countryMatches = normalizedEntryCountry != null && normalizedEntryCountry.equals(normalizedCountry);
        boolean codeMatches = normalizedEntryCode != null && normalizedEntryCode.equals(normalizedCode);

        if (countryMatches && codeMatches) {
            return true;
        }
        if (countryMatches && normalizedCode == null) {
            return true;
        }
        return codeMatches && normalizedCountry == null;
    }

    private String buildCountryEntryUsageWarning(CountryDirectory.CountryEntry entry, List<String> usageDetails) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nie można usunąć ani podmienić wpisu ")
                .append(buildCustomCountryDisplay(entry))
                .append(", ponieważ jest już używany przez: ");

        for (int i = 0; i < usageDetails.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(usageDetails.get(i));
        }

        sb.append(". Najpierw zmień kraj w tych rekordach, a dopiero potem wróć do słownika.");
        return sb.toString();
    }

    private CountryDirectory.CountryEntry getSelectedEditableCustomCountryEntry() {
        CountryDirectory.CountryEntry selectedCustom = customCountryEntriesList == null
                ? null
                : customCountryEntriesList.getSelectionModel().getSelectedItem();
        if (selectedCustom != null) {
            return selectedCustom;
        }

        CountryDirectory.CountryEntry selectedShared = allCountryEntriesList == null
                ? null
                : allCountryEntriesList.getSelectionModel().getSelectedItem();
        return isCustomCountryEntry(selectedShared) ? selectedShared : null;
    }

    private void updateCustomCountryUsageStatus() {
        CountryDirectory.CountryEntry selected = getSelectedEditableCustomCountryEntry();

        CountryEntryUsageSnapshot usageSnapshot = getCountryEntryUsageSnapshot(selected);
        updateCustomCountryUsageSummaryLabel(selected, usageSnapshot);
        updateCustomCountryUsageCountsLabel(selected, usageSnapshot);
    }

    private void updateCustomCountryUsageSummaryLabel(CountryDirectory.CountryEntry selected,
                                                       CountryEntryUsageSnapshot usageSnapshot) {
        if (customCountryUsageLabel == null) {
            return;
        }

        if (selected == null || usageSnapshot == null) {
            customCountryUsageLabel.setText(
                    "Status użycia wybranego wpisu: brak zaznaczenia. Wybierz wpis własny, aby sprawdzić użycie w danych wystawcy i kontrahentach."
            );
            return;
        }

        List<String> usageDetails = findCountryEntryUsageDetails(selected);
        if (usageDetails.isEmpty()) {
            customCountryUsageLabel.setText(
                    "Status użycia wybranego wpisu: Nieużywany. Nie wykryto użycia w danych wystawcy ani u kontrahentów."
            );
            return;
        }

        customCountryUsageLabel.setText(
                "Status użycia wybranego wpisu: Używany przez " + String.join("; ", usageDetails) + "."
        );
    }

    private void updateCustomCountryUsageCountsLabel(CountryDirectory.CountryEntry selected,
                                                      CountryEntryUsageSnapshot usageSnapshot) {
        if (customCountryUsageCountsLabel == null) {
            return;
        }

        if (selected == null || usageSnapshot == null) {
            customCountryUsageCountsLabel.setText(
                    "Licznik powiązań: brak zaznaczenia. Po wyborze wpisu zobaczysz, czy używają go dane wystawcy oraz ilu kontrahentów."
            );
            return;
        }

        int issuerCount = usageSnapshot.usedByIssuer() ? 1 : 0;
        int contrahentCount = usageSnapshot.matchingContrahents().size();
        int totalLinks = issuerCount + contrahentCount;
        customCountryUsageCountsLabel.setText(
                "Licznik powiązań: łącznie " + totalLinks
                        + " (dane wystawcy: " + issuerCount
                        + ", kontrahenci: " + contrahentCount + ")."
        );
    }

    private record CountryEntryUsageSnapshot(boolean usedByIssuer, List<String> matchingContrahents) {
    }

    private String buildContrahentUsageLabel(Contrahent contrahent) {
        if (contrahent == null) {
            return "[brak nazwy]";
        }

        String name = safe(contrahent.getName());
        String city = safe(contrahent.getCity());
        if (name.isBlank()) {
            return city.isBlank() ? "ID=" + contrahent.getId() : "ID=" + contrahent.getId() + " (" + city + ")";
        }
        return city.isBlank() ? name : name + " (" + city + ")";
    }

    private String normalizeCountryValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeCountryCodeValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
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

        try {
            documentTypeService.delete(selected.getId());
            refreshDocumentTypes();
            clearDocumentTypeForm();
            DialogUtil.showSuccess("Typ dokumentu został usunięty.");
        } catch (IllegalStateException e) {
            DialogUtil.showWarning("Nie można usunąć typu dokumentu", e.getMessage());
        } catch (Exception e) {
            DialogUtil.showError("Błąd usuwania", "Nie udało się usunąć typu dokumentu.");
        }
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

        try {
            appUserService.delete(selected.getId());
            refreshUsers();
            clearUserForm();
            loadDefaultUser();
            DialogUtil.showSuccess("Użytkownik został usunięty.");
        } catch (IllegalStateException e) {
            DialogUtil.showWarning("Nie można usunąć użytkownika", e.getMessage());
        } catch (Exception e) {
            DialogUtil.showError("Błąd usuwania", "Nie udało się usunąć użytkownika.");
        }
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

        if (issuerNoStreetCheckBox != null) {
            issuerNoStreetCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                updateIssuerStreetPrompt();
                updateIssuerStatusLabel();
            });
        }

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
        updateSynchronizationScope(type);
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

    private void updateSynchronizationScope(NumberingType type) {
        String scopeLabel = switch (type == null ? NumberingType.DOCUMENT : type) {
            case DOCUMENT -> "Dokumenty";
            case BATCH -> "Partie roślin";
        };

        if (syncScopeLabel != null) {
            syncScopeLabel.setText("Synchronizacja licznika dotyczy aktualnie wybranego typu numeracji: " + scopeLabel + ".");
        }
        if (synchronizeCounterButton != null) {
            synchronizeCounterButton.setText("Synchronizuj licznik „" + scopeLabel + "” z bazą");
        }
    }

    @FXML
    private void synchronizeCounterWithDatabase() {
        NumberingType selectedType = numberingTypeBox.getValue();
        if (selectedType == null) {
            DialogUtil.showWarning("Brak typu", "Wybierz typ numeracji.");
            return;
        }

        try {
            String message = numberingConfigService.synchronizeCounterWithDatabase(selectedType);
            loadConfig(selectedType);
            infoLabel.setText(message);
            DialogUtil.showSuccess(message);
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Synchronizacja numeratora", "Nie udało się zsynchronizować licznika z bazą danych.");
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
        updateSynchronizationScope(selectedType);
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
        if (issuerNoStreetCheckBox != null) {
            issuerNoStreetCheckBox.setSelected(false);
        }
        issuerPhytosanitaryNumberField.clear();
        updateIssuerStreetPrompt();
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
        if (issuerNoStreetCheckBox != null) {
            issuerNoStreetCheckBox.setSelected(profile.isNoStreet());
        }
        issuerPhytosanitaryNumberField.setText(safe(profile.getPhytosanitaryNumber()));
        updateIssuerStreetPrompt();
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
        profile.setNoStreet(issuerNoStreetCheckBox != null && issuerNoStreetCheckBox.isSelected());
        profile.setPhytosanitaryNumber(safe(issuerPhytosanitaryNumberField.getText()));
        return profile;
    }


    private void updateIssuerStreetPrompt() {
        if (issuerStreetField == null) {
            return;
        }

        boolean noStreet = issuerNoStreetCheckBox != null && issuerNoStreetCheckBox.isSelected();
        issuerStreetField.setPromptText(noStreet ? "Numer domu miejscowości" : "Ulica i numer");
    }

    private void updateIssuerStatusLabel() {
        IssuerProfile profile = buildIssuerProfileFromForm();
        List<String> validationIssues = getIssuerProfileValidationIssues(profile);
        boolean countryPairConsistent = isIssuerCountryPairConsistent(profile);

        if (validationIssues.isEmpty() && countryPairConsistent) {
            issuerStatusLabel.setText("Profil podmiotu jest kompletny i gotowy do użycia w dokumentach, pulpicie i dalszych modułach.");
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
            missing.add(profile.isNoStreet() ? "numer domu miejscowości" : "ulica i numer");
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

        if (profile.isNoStreet()) {
            builder.append(", tryb: brak ulicy");
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

    private void applyPendingNavigation() {
        if (settingsTabPane != null && pendingTabTitle != null && !pendingTabTitle.isBlank()) {
            for (Tab tab : settingsTabPane.getTabs()) {
                if (pendingTabTitle.equals(tab.getText())) {
                    settingsTabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }

        boolean openCreateFlow = pendingCreateDatabaseFlow;
        pendingTabTitle = null;
        pendingCreateDatabaseFlow = false;

        if (openCreateFlow) {
            Platform.runLater(this::activateCreateNewDatabaseFlow);
        }
    }

    private void configureDatabaseProfileControls() {
        if (databaseProfileBox == null) {
            return;
        }

        databaseProfileBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DatabaseProfileInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toDisplayLabel());
            }
        });

        databaseProfileBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DatabaseProfileInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Wybierz bazę danych" : item.toDisplayLabel());
            }
        });

        databaseProfileBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (loadingDatabaseProfileSelection || newValue == null) {
                return;
            }
            updateDatabaseProfileSelectionState(newValue);
        });
    }

    private void loadDatabaseProfileOverview() {
        if (databasePathLabel == null) {
            return;
        }

        List<DatabaseProfileInfo> profiles = databaseProfileService.getAvailableProfiles();
        loadingDatabaseProfileSelection = true;
        try {
            if (databaseProfileBox != null) {
                databaseProfileBox.setItems(FXCollections.observableArrayList(profiles));
                DatabaseProfileInfo currentProfile = profiles.stream()
                        .filter(info -> !info.createNewOption() && info.current())
                        .findFirst()
                        .orElseGet(databaseProfileService::getCurrentProfileInfo);
                databaseProfileBox.setValue(currentProfile);
            }
        } finally {
            loadingDatabaseProfileSelection = false;
        }

        DatabaseProfileInfo current = databaseProfileService.getCurrentProfileInfo();
        if (databaseFolderLabel != null) {
            databaseFolderLabel.setText(DatabaseConfig.getProfilesDirectory().toString());
        }
        databasePathLabel.setText(current.databasePath() == null ? "Brak ścieżki bazy danych." : current.databasePath().toString());
        if (databaseProfileStatusLabel != null) {
            databaseProfileStatusLabel.setText(buildDatabaseProfileStatus(current));
        }
        if (databaseMissingStatusLabel != null) {
            databaseMissingStatusLabel.setText(buildMissingDatabaseStatus());
        }
        if (newDatabaseNameField != null) {
            newDatabaseNameField.setVisible(false);
            newDatabaseNameField.setManaged(false);
            newDatabaseNameField.clear();
        }
        updateDatabaseProfileSelectionState(databaseProfileBox == null ? null : databaseProfileBox.getValue());
    }

    private String buildMissingDatabaseStatus() {
        return DatabaseConfig.getMissingRememberedDatabasePath()
                .map(path -> {
                    String backupPath = BackupService.getRememberedBackupPath().map(Path::toString).orElse("brak zapamiętanego backupu");
                    return "Nie znaleziono ostatnio używanej bazy danych: " + path + ". Możesz utworzyć nową bazę albo odzyskać dane z ostatniego backupu: " + backupPath;
                })
                .orElse("Ostatnio używana baza danych została poprawnie odnaleziona.");
    }

    private String buildDatabaseProfileStatus(DatabaseProfileInfo current) {
        if (current == null) {
            return "Nie wybrano aktywnej bazy danych.";
        }

        if (!current.exists()) {
            String backupPath = BackupService.getRememberedBackupPath().map(Path::toString).orElse("brak dostępnej kopii");
            return "Aktywna baza nie istnieje w zapisanej lokalizacji. Utwórz nową bazę lub odtwórz dane z ostatniej kopii: " + backupPath;
        }

        String mode = current.testProfile() ? "Baza testowa" : "Baza robocza";
        return current.displayName() + " • " + mode + " • ostatnio używana baza została poprawnie odnaleziona.";
    }

    private void updateDatabaseProfileSelectionState(DatabaseProfileInfo selectedProfile) {
        boolean createMode = selectedProfile != null && selectedProfile.createNewOption();
        if (newDatabaseNameField != null) {
            newDatabaseNameField.setManaged(createMode);
            newDatabaseNameField.setVisible(createMode);
            if (!createMode) {
                newDatabaseNameField.clear();
            }
        }

        if (databaseProfileStatusLabel == null || selectedProfile == null) {
            return;
        }

        if (createMode) {
            databaseProfileStatusLabel.setText("Wybierz nazwę nowej bazy danych i potwierdź przyciskiem „Użyj wybranej bazy”.");
        } else if (!selectedProfile.exists()) {
            databaseProfileStatusLabel.setText("Wybrana baza danych nie istnieje już w zapisanej lokalizacji. Możesz utworzyć nową bazę albo odzyskać dane z backupu.");
        } else {
            databaseProfileStatusLabel.setText(buildDatabaseProfileStatus(selectedProfile));
        }
    }

    private void activateCreateNewDatabaseFlow() {
        if (databaseProfileBox == null) {
            return;
        }

        DatabaseProfileInfo createOption = databaseProfileBox.getItems().stream()
                .filter(DatabaseProfileInfo::createNewOption)
                .findFirst()
                .orElse(null);
        if (createOption == null) {
            return;
        }

        databaseProfileBox.setValue(createOption);
        updateDatabaseProfileSelectionState(createOption);
        if (newDatabaseNameField != null) {
            DatabaseConfig.getMissingRememberedDatabasePath()
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(name -> name.toLowerCase().endsWith(".db") ? name.substring(0, name.length() - 3) : name)
                    .ifPresent(newDatabaseNameField::setText);
            newDatabaseNameField.requestFocus();
            newDatabaseNameField.selectAll();
        }
    }

    @FXML
    private void applySelectedDatabaseProfile() {
        DatabaseProfileInfo selectedProfile = databaseProfileBox == null ? null : databaseProfileBox.getValue();
        if (selectedProfile == null) {
            DialogUtil.showWarning("Baza danych", "Wybierz bazę danych z listy.");
            return;
        }

        if (selectedProfile.createNewOption()) {
            String profileName = newDatabaseNameField == null ? "" : newDatabaseNameField.getText();
            if (profileName == null || profileName.isBlank()) {
                DialogUtil.showWarning("Nowa baza danych", "Podaj nazwę nowej bazy danych.");
                return;
            }
            DatabaseProfileInfo created = databaseProfileService.createAndActivateProfile(profileName);
            DialogUtil.showSuccess("Aktywowano bazę danych: " + created.displayName());
            ViewManager.refreshCurrent();
            MainController.requestRefreshSystemNews();
            return;
        }

        if (!selectedProfile.exists()) {
            DialogUtil.showWarning(
                    "Baza danych",
                    "Wybrana baza danych nie istnieje już w zapisanej lokalizacji. Utwórz nową bazę albo odzyskaj dane z ostatniego backupu."
            );
            return;
        }

        DatabaseProfileInfo current = databaseProfileService.getCurrentProfileInfo();
        if (current.databasePath() != null && current.databasePath().equals(selectedProfile.databasePath())) {
            return;
        }

        databaseProfileService.activateProfile(selectedProfile);
        DialogUtil.showSuccess("Przełączono aktywną bazę danych na: " + selectedProfile.displayName());
        ViewManager.refreshCurrent();
        MainController.requestRefreshSystemNews();
    }

    @FXML
    private void restoreDatabaseFromBackup() {
        try {
            Path sourceBackup = BackupService.getRememberedBackupPath().orElse(null);
            if (sourceBackup == null || !Files.exists(sourceBackup)) {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Wybierz backup bazy danych");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki baz danych", "*.db"));
                File selectedFile = chooser.showOpenDialog(getOwningWindow());
                if (selectedFile == null) {
                    return;
                }
                sourceBackup = selectedFile.toPath();
            }

            String suggestedName = DatabaseConfig.getMissingRememberedDatabasePath()
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(name -> name.toLowerCase().endsWith(".db") ? name.substring(0, name.length() - 3) : name)
                    .orElseGet(() -> {
                        DatabaseProfileInfo selected = databaseProfileBox == null ? null : databaseProfileBox.getValue();
                        if (selected != null && selected.createNewOption() && newDatabaseNameField != null && !newDatabaseNameField.getText().isBlank()) {
                            return newDatabaseNameField.getText().trim();
                        }
                        return "odzyskana_baza";
                    });

            DatabaseProfileInfo restored = databaseProfileService.restoreAndActivateProfile(sourceBackup, suggestedName);
            DialogUtil.showSuccess("Odzyskano i aktywowano bazę danych: " + restored.displayName());
            ViewManager.refreshCurrent();
            MainController.requestRefreshSystemNews();
        } catch (Exception e) {
            DialogUtil.showError("Odzyskiwanie bazy danych", e.getMessage());
        }
    }

    @FXML
    private void refreshDatabaseProfiles() {
        loadDatabaseProfileOverview();
    }

    private void refreshBackupStatus() {
        if (backupStatusLabel == null) {
            return;
        }

        String lastBackupAt = safe(appSettingsService.getLastBackupAt());
        String lastBackupPath = safe(appSettingsService.getLastBackupPath());

        if (lastBackupAt.isBlank()) {
            backupStatusLabel.setText("Kopia zapasowa nie została jeszcze wykonana.");
            return;
        }

        if (lastBackupPath.isBlank()) {
            backupStatusLabel.setText("Ostatnia kopia zapasowa: " + lastBackupAt);
            return;
        }

        backupStatusLabel.setText(UiTextUtil.buildPathMessage("Ostatnia kopia zapasowa: " + lastBackupAt, "Lokalizacja: " + lastBackupPath));
    }

    @FXML
    private void createBackup() {
        try {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Wybierz katalog dla kopii zapasowej");
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
                        "Utworzono kopię zapasową bazy danych w lokalizacji " + backupPath
                );
            }
            refreshBackupStatus();
            DialogUtil.showSuccess("Kopia zapasowa została utworzona:\n" + backupPath);
        } catch (IllegalArgumentException | IllegalStateException e) {
            backupStatusLabel.setText(e.getMessage());
            DialogUtil.showWarning("Kopia zapasowa", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            backupStatusLabel.setText("Nie udało się utworzyć kopii zapasowej.");
            DialogUtil.showError("Błąd kopii zapasowej", "Nie udało się utworzyć kopii zapasowej bazy danych.");
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
                "Liczba wpisów dziennika audytu: " + totalEntries
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
                auditLogStatusLabel.setText("Nie udało się odświeżyć dziennika audytu.");
            }
            DialogUtil.showError("Dziennik audytu", "Nie udało się odczytać wpisów dziennika audytu.");
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
            plantsCsvColumnsLabel.setText(plantCsvImportService.getSupportedColumnsSummary()
                    + UiTextUtil.NL
                    + plantCsvExportService.getSupportedColumnsSummary());
        }
        if (contrahentsCsvColumnsLabel != null) {
            contrahentsCsvColumnsLabel.setText(contrahentCsvImportService.getSupportedColumnsSummary()
                    + UiTextUtil.NL
                    + contrahentCsvExportService.getSupportedColumnsSummary());
        }
        if (documentsCsvColumnsLabel != null) {
            documentsCsvColumnsLabel.setText(documentCsvImportService.getSupportedColumnsSummary()
                    + UiTextUtil.NL
                    + documentCsvExportService.getSupportedColumnsSummary());
        }
        if (plantBatchesCsvColumnsLabel != null) {
            plantBatchesCsvColumnsLabel.setText(plantBatchCsvImportService.getSupportedColumnsSummary()
                    + UiTextUtil.NL
                    + plantBatchCsvExportService.getSupportedColumnsSummary());
        }
        if (eppoDictionaryCsvColumnsLabel != null) {
            eppoDictionaryCsvColumnsLabel.setText(eppoDictionaryCsvImportService.getSupportedColumnsSummary()
                    + UiTextUtil.NL
                    + eppoDictionaryCsvExportService.getSupportedColumnsSummary());
        }
        if (plantsCsvStatusLabel != null) {
            plantsCsvStatusLabel.setText(buildCsvInitialStatus("roślin"));
        }
        if (contrahentsCsvStatusLabel != null) {
            contrahentsCsvStatusLabel.setText(buildCsvInitialStatus("kontrahentów"));
        }
        if (documentsCsvStatusLabel != null) {
            documentsCsvStatusLabel.setText(buildCsvInitialStatus("dokumentów"));
        }
        if (plantBatchesCsvStatusLabel != null) {
            plantBatchesCsvStatusLabel.setText(buildCsvInitialStatus("partii roślin"));
        }
        if (eppoDictionaryCsvStatusLabel != null) {
            eppoDictionaryCsvStatusLabel.setText(buildCsvInitialStatus("słowników EPPO"));
        }
        clearPlantsCsvPreviewState();
        clearContrahentsCsvPreviewState();
        clearDocumentsCsvPreviewState();
        clearPlantBatchesCsvPreviewState();
        clearEppoDictionaryCsvPreviewState();
        resetPlantsCsvPreview();
        resetContrahentsCsvPreview();
        resetDocumentsCsvPreview();
        resetPlantBatchesCsvPreview();
        resetEppoDictionaryCsvPreview();
    }

    @FXML
    private void previewPlantsCsvImport() {
        Path selectedPath = chooseCsvToOpen("Wybierz plik CSV roślin");
        if (selectedPath == null) {
            return;
        }

        try {
            PlantImportPreviewResult result = plantCsvImportService.preview(selectedPath);
            lastPlantsCsvPreviewResult = result;
            updatePlantsCsvApplyButtonState();
            plantsCsvStatusLabel.setText(buildPlantsPreviewStatus(result));
            plantsCsvPreviewArea.setText(buildPlantsPreviewText(result));
        } catch (Exception e) {
            e.printStackTrace();
            clearPlantsCsvPreviewState();
            plantsCsvStatusLabel.setText(buildCsvPreviewErrorStatus("roślin"));
            DialogUtil.showError(buildCsvPreviewDialogTitle("roślin"), buildCsvPreviewErrorMessage("roślin"));
        }
    }

    @FXML
    private void applyPlantsCsvImport() {
        if (lastPlantsCsvPreviewResult == null) {
            DialogUtil.showWarning("Import CSV roślin", "Najpierw przygotuj podgląd importu roślin.");
            return;
        }
        if (lastPlantsCsvPreviewResult.getNewRowsCount() <= 0) {
            DialogUtil.showWarning("Import CSV roślin", "Brak nowych roślin do importu w ostatnim podglądzie.");
            updatePlantsCsvApplyButtonState();
            return;
        }
        if (!DialogUtil.confirmAction(
                "Import CSV roślin",
                "zaimportować do bazy",
                buildCsvImportConfirmationTarget("roślin", lastPlantsCsvPreviewResult.getSourceName(), lastPlantsCsvPreviewResult.getNewRowsCount(), lastPlantsCsvPreviewResult.getMatchingExistingCount(), lastPlantsCsvPreviewResult.getInvalidRowsCount() + lastPlantsCsvPreviewResult.getDuplicateInFileCount())
        )) {
            return;
        }

        try {
            CsvImportExecutionResult executionResult = plantCsvImportService.applyPreview(lastPlantsCsvPreviewResult);
            auditLogService.log("PlantCsvImport", null, "IMPORT", buildCsvImportAuditDescription("roślin", executionResult));
            plantsCsvStatusLabel.setText(buildCsvImportExecutionStatus("roślin", executionResult));
            plantsCsvPreviewArea.setText(buildCsvImportExecutionText("CSV roślin", executionResult, buildPlantsPreviewText(lastPlantsCsvPreviewResult)));
            clearPlantsCsvPreviewState();
            refreshAuditLog();
            DialogUtil.showSuccess(buildCsvImportSuccessDialogMessage("Rośliny", executionResult));
        } catch (Exception e) {
            e.printStackTrace();
            plantsCsvStatusLabel.setText(buildCsvImportErrorStatus("roślin"));
            DialogUtil.showError("Import CSV roślin", "Nie udało się wykonać importu roślin do bazy.");
        }
    }

    @FXML
    private void exportPlantsCsv() {
        Path selectedPath = chooseCsvToSave("Eksportuj rośliny do CSV", "rosliny-eksport.csv");
        if (selectedPath == null) {
            return;
        }

        try {
            Path exported = plantCsvExportService.export(selectedPath);
            plantsCsvStatusLabel.setText(buildCsvExportSuccessStatus("roślin", exported));
            DialogUtil.showSuccess(buildCsvExportSuccessDialogMessage("Rośliny", exported));
        } catch (Exception e) {
            e.printStackTrace();
            plantsCsvStatusLabel.setText(buildCsvExportErrorStatus("roślin"));
            DialogUtil.showError(buildCsvExportDialogTitle("roślin"), buildCsvExportErrorMessage("roślin"));
        }
    }

    @FXML
    private void previewContrahentsCsvImport() {
        Path selectedPath = chooseCsvToOpen("Wybierz plik CSV kontrahentów");
        if (selectedPath == null) {
            return;
        }

        try {
            ContrahentImportPreviewResult result = contrahentCsvImportService.preview(selectedPath);
            lastContrahentsCsvPreviewResult = result;
            updateContrahentsCsvApplyButtonState();
            contrahentsCsvStatusLabel.setText(buildContrahentsPreviewStatus(result));
            contrahentsCsvPreviewArea.setText(buildContrahentsPreviewText(result));
        } catch (Exception e) {
            e.printStackTrace();
            clearContrahentsCsvPreviewState();
            contrahentsCsvStatusLabel.setText(buildCsvPreviewErrorStatus("kontrahentów"));
            DialogUtil.showError(buildCsvPreviewDialogTitle("kontrahentów"), buildCsvPreviewErrorMessage("kontrahentów"));
        }
    }

    @FXML
    private void applyContrahentsCsvImport() {
        if (lastContrahentsCsvPreviewResult == null) {
            DialogUtil.showWarning("Import CSV kontrahentów", "Najpierw przygotuj podgląd importu kontrahentów.");
            return;
        }
        if (lastContrahentsCsvPreviewResult.getNewRowsCount() <= 0) {
            DialogUtil.showWarning("Import CSV kontrahentów", "Brak nowych kontrahentów do importu w ostatnim podglądzie.");
            updateContrahentsCsvApplyButtonState();
            return;
        }
        if (!DialogUtil.confirmAction(
                "Import CSV kontrahentów",
                "zaimportować do bazy",
                buildCsvImportConfirmationTarget("kontrahentów", lastContrahentsCsvPreviewResult.getSourceName(), lastContrahentsCsvPreviewResult.getNewRowsCount(), lastContrahentsCsvPreviewResult.getMatchingExistingCount(), lastContrahentsCsvPreviewResult.getInvalidRowsCount() + lastContrahentsCsvPreviewResult.getDuplicateInFileCount())
        )) {
            return;
        }

        try {
            CsvImportExecutionResult executionResult = contrahentCsvImportService.applyPreview(lastContrahentsCsvPreviewResult);
            auditLogService.log("ContrahentCsvImport", null, "IMPORT", buildCsvImportAuditDescription("kontrahentów", executionResult));
            contrahentsCsvStatusLabel.setText(buildCsvImportExecutionStatus("kontrahentów", executionResult));
            contrahentsCsvPreviewArea.setText(buildCsvImportExecutionText("CSV kontrahentów", executionResult, buildContrahentsPreviewText(lastContrahentsCsvPreviewResult)));
            clearContrahentsCsvPreviewState();
            refreshAuditLog();
            DialogUtil.showSuccess(buildCsvImportSuccessDialogMessage("Kontrahenci", executionResult));
        } catch (Exception e) {
            e.printStackTrace();
            contrahentsCsvStatusLabel.setText(buildCsvImportErrorStatus("kontrahentów"));
            DialogUtil.showError("Import CSV kontrahentów", "Nie udało się wykonać importu kontrahentów do bazy.");
        }
    }

    @FXML
    private void exportContrahentsCsv() {
        Path selectedPath = chooseCsvToSave("Eksportuj kontrahentów do CSV", "kontrahenci-eksport.csv");
        if (selectedPath == null) {
            return;
        }

        try {
            Path exported = contrahentCsvExportService.export(selectedPath);
            contrahentsCsvStatusLabel.setText(buildCsvExportSuccessStatus("kontrahentów", exported));
            DialogUtil.showSuccess(buildCsvExportSuccessDialogMessage("Kontrahenci", exported));
        } catch (Exception e) {
            e.printStackTrace();
            contrahentsCsvStatusLabel.setText(buildCsvExportErrorStatus("kontrahentów"));
            DialogUtil.showError(buildCsvExportDialogTitle("kontrahentów"), buildCsvExportErrorMessage("kontrahentów"));
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
            lastDocumentsCsvPreviewResult = result;
            updateDocumentsCsvApplyButtonState();
            documentsCsvStatusLabel.setText(buildDocumentsPreviewStatus(result));
            documentsCsvPreviewArea.setText(buildDocumentsPreviewText(result));
        } catch (Exception e) {
            e.printStackTrace();
            clearDocumentsCsvPreviewState();
            documentsCsvStatusLabel.setText(buildCsvPreviewErrorStatus("dokumentów"));
            DialogUtil.showError(buildCsvPreviewDialogTitle("dokumentów"), buildCsvPreviewErrorMessage("dokumentów"));
        }
    }

    @FXML
    private void applyDocumentsCsvImport() {
        if (lastDocumentsCsvPreviewResult == null) {
            DialogUtil.showWarning("Import CSV dokumentów", "Najpierw przygotuj podgląd importu dokumentów.");
            return;
        }
        if (lastDocumentsCsvPreviewResult.getDocumentCount() <= 0) {
            DialogUtil.showWarning("Import CSV dokumentów", "Brak nowych dokumentów do importu w ostatnim podglądzie.");
            updateDocumentsCsvApplyButtonState();
            return;
        }
        if (!DialogUtil.confirmAction(
                "Import CSV dokumentów",
                "zaimportować do bazy",
                buildCsvImportConfirmationTarget("dokumentów", lastDocumentsCsvPreviewResult.getSourceName(), lastDocumentsCsvPreviewResult.getDocumentCount(), lastDocumentsCsvPreviewResult.getMatchingExistingCount(), lastDocumentsCsvPreviewResult.getInvalidRowsCount() + lastDocumentsCsvPreviewResult.getDuplicateInFileCount())
        )) {
            return;
        }

        try {
            CsvImportExecutionResult executionResult = documentCsvImportService.applyPreview(lastDocumentsCsvPreviewResult);
            auditLogService.log("DocumentCsvImport", null, "IMPORT", buildDocumentsImportAuditDescription(executionResult));
            documentsCsvStatusLabel.setText(buildDocumentsImportExecutionStatus(executionResult));
            documentsCsvPreviewArea.setText(buildDocumentsImportExecutionText(executionResult, buildDocumentsPreviewText(lastDocumentsCsvPreviewResult)));
            clearDocumentsCsvPreviewState();
            refreshAuditLog();
            DialogUtil.showSuccess(buildDocumentsImportSuccessDialogMessage(executionResult));
        } catch (Exception e) {
            e.printStackTrace();
            documentsCsvStatusLabel.setText(buildCsvImportErrorStatus("dokumentów"));
            DialogUtil.showError("Import CSV dokumentów", "Nie udało się wykonać importu dokumentów do bazy.");
        }
    }

    @FXML
    private void exportDocumentsCsv() {
        Path selectedPath = chooseCsvToSave("Eksportuj dokumenty do CSV", "dokumenty-eksport.csv");
        if (selectedPath == null) {
            return;
        }

        try {
            Path exported = documentCsvExportService.export(selectedPath);
            documentsCsvStatusLabel.setText(buildCsvExportSuccessStatus("dokumentów", exported));
            DialogUtil.showSuccess(buildCsvExportSuccessDialogMessage("Dokumenty", exported));
        } catch (Exception e) {
            e.printStackTrace();
            documentsCsvStatusLabel.setText(buildCsvExportErrorStatus("dokumentów"));
            DialogUtil.showError(buildCsvExportDialogTitle("dokumentów"), buildCsvExportErrorMessage("dokumentów"));
        }
    }


    
    @FXML
    private void previewPlantBatchesCsvImport() {
        Path selectedPath = chooseCsvToOpen("Wybierz plik CSV partii roślin");
        if (selectedPath == null) {
            return;
        }

        try {
            PlantBatchImportPreviewResult result = plantBatchCsvImportService.preview(selectedPath);
            lastPlantBatchesCsvPreviewResult = result;
            updatePlantBatchesCsvApplyButtonState();
            plantBatchesCsvStatusLabel.setText(buildPlantBatchesPreviewStatus(result));
            plantBatchesCsvPreviewArea.setText(buildPlantBatchesPreviewText(result));
        } catch (Exception e) {
            e.printStackTrace();
            clearPlantBatchesCsvPreviewState();
            plantBatchesCsvStatusLabel.setText(buildCsvPreviewErrorStatus("partii roślin"));
            DialogUtil.showError(buildCsvPreviewDialogTitle("partii roślin"), buildCsvPreviewErrorMessage("partii roślin"));
        }
    }

    @FXML
    private void applyPlantBatchesCsvImport() {
        if (lastPlantBatchesCsvPreviewResult == null) {
            DialogUtil.showWarning("Import CSV partii roślin", "Najpierw przygotuj podgląd importu partii roślin.");
            return;
        }
        if (lastPlantBatchesCsvPreviewResult.getNewRowsCount() <= 0) {
            DialogUtil.showWarning("Import CSV partii roślin", "Brak nowych partii roślin do importu w ostatnim podglądzie.");
            updatePlantBatchesCsvApplyButtonState();
            return;
        }
        if (!DialogUtil.confirmAction(
                "Import CSV partii roślin",
                "zaimportować do bazy",
                buildCsvImportConfirmationTarget("partii roślin", lastPlantBatchesCsvPreviewResult.getSourceName(), lastPlantBatchesCsvPreviewResult.getNewRowsCount(), lastPlantBatchesCsvPreviewResult.getMatchingExistingCount(), lastPlantBatchesCsvPreviewResult.getInvalidRowsCount() + lastPlantBatchesCsvPreviewResult.getDuplicateInFileCount())
        )) {
            return;
        }

        try {
            CsvImportExecutionResult executionResult = plantBatchCsvImportService.applyPreview(lastPlantBatchesCsvPreviewResult);
            auditLogService.log("PlantBatchCsvImport", null, "IMPORT", buildCsvImportAuditDescription("partii roślin", executionResult));
            plantBatchesCsvStatusLabel.setText(buildCsvImportExecutionStatus("partii roślin", executionResult));
            plantBatchesCsvPreviewArea.setText(buildCsvImportExecutionText("CSV partii roślin", executionResult, buildPlantBatchesPreviewText(lastPlantBatchesCsvPreviewResult)));
            clearPlantBatchesCsvPreviewState();
            refreshAuditLog();
            DialogUtil.showSuccess(buildCsvImportSuccessDialogMessage("Partie roślin", executionResult));
        } catch (Exception e) {
            e.printStackTrace();
            plantBatchesCsvStatusLabel.setText(buildCsvImportErrorStatus("partii roślin"));
            DialogUtil.showError("Import CSV partii roślin", "Nie udało się wykonać importu partii roślin do bazy.");
        }
    }

    @FXML
    private void exportPlantBatchesCsv() {
        Path selectedPath = chooseCsvToSave("Eksportuj partie roślin do CSV", "partie-roslin-eksport.csv");
        if (selectedPath == null) {
            return;
        }

        try {
            Path exported = plantBatchCsvExportService.export(selectedPath);
            plantBatchesCsvStatusLabel.setText(buildCsvExportSuccessStatus("partii roślin", exported));
            DialogUtil.showSuccess(buildCsvExportSuccessDialogMessage("Partie roślin", exported));
        } catch (Exception e) {
            e.printStackTrace();
            plantBatchesCsvStatusLabel.setText(buildCsvExportErrorStatus("partii roślin"));
            DialogUtil.showError(buildCsvExportDialogTitle("partii roślin"), buildCsvExportErrorMessage("partii roślin"));
        }
    }

@FXML
    private void previewEppoDictionaryCsvImport() {
        Path selectedPath = chooseCsvToOpen("Wybierz plik CSV słowników EPPO");
        if (selectedPath == null) {
            return;
        }

        try {
            EppoDictionaryImportPreviewResult result = eppoDictionaryCsvImportService.preview(selectedPath);
            lastEppoDictionaryCsvPreviewResult = result;
            updateEppoDictionaryCsvApplyButtonState();
            eppoDictionaryCsvStatusLabel.setText(buildEppoDictionaryPreviewStatus(result));
            eppoDictionaryCsvPreviewArea.setText(buildEppoDictionaryPreviewText(result));
        } catch (Exception e) {
            e.printStackTrace();
            clearEppoDictionaryCsvPreviewState();
            eppoDictionaryCsvStatusLabel.setText(buildCsvPreviewErrorStatus("słowników EPPO"));
            DialogUtil.showError(buildCsvPreviewDialogTitle("słowników EPPO"), buildCsvPreviewErrorMessage("słowników EPPO"));
        }
    }

    @FXML
    private void applyEppoDictionaryCsvImport() {
        if (lastEppoDictionaryCsvPreviewResult == null) {
            DialogUtil.showWarning("Import CSV słowników EPPO", "Najpierw przygotuj podgląd importu słowników EPPO.");
            return;
        }
        if (lastEppoDictionaryCsvPreviewResult.getImportableRowsCount() <= 0) {
            DialogUtil.showWarning("Import CSV słowników EPPO", "Brak nowych lub aktualizowanych relacji EPPO do importu w ostatnim podglądzie.");
            updateEppoDictionaryCsvApplyButtonState();
            return;
        }
        if (!DialogUtil.confirmAction(
                "Import CSV słowników EPPO",
                "zaimportować do bazy",
                buildCsvImportConfirmationTarget("słowników EPPO",
                        lastEppoDictionaryCsvPreviewResult.getSourceName(),
                        lastEppoDictionaryCsvPreviewResult.getImportableRowsCount(),
                        lastEppoDictionaryCsvPreviewResult.getMatchingExistingCount(),
                        lastEppoDictionaryCsvPreviewResult.getInvalidRowsCount() + lastEppoDictionaryCsvPreviewResult.getDuplicateInFileCount())
        )) {
            return;
        }

        try {
            CsvImportExecutionResult executionResult = eppoDictionaryCsvImportService.applyPreview(lastEppoDictionaryCsvPreviewResult);
            auditLogService.log("EppoDictionaryCsvImport", null, "IMPORT", buildCsvImportAuditDescription("słowników EPPO", executionResult));
            eppoDictionaryCsvStatusLabel.setText(buildCsvImportExecutionStatus("słowników EPPO", executionResult));
            eppoDictionaryCsvPreviewArea.setText(buildCsvImportExecutionText("CSV słowników EPPO", executionResult, buildEppoDictionaryPreviewText(lastEppoDictionaryCsvPreviewResult)));
            clearEppoDictionaryCsvPreviewState();
            refreshAuditLog();
            DialogUtil.showSuccess(buildCsvImportSuccessDialogMessage("Słowniki EPPO", executionResult));
        } catch (Exception e) {
            e.printStackTrace();
            eppoDictionaryCsvStatusLabel.setText(buildCsvImportErrorStatus("słowników EPPO"));
            DialogUtil.showError("Import CSV słowników EPPO", "Nie udało się wykonać importu słowników EPPO do bazy.");
        }
    }

    @FXML
    private void exportEppoDictionaryCsv() {
        Path selectedPath = chooseCsvToSave("Eksportuj słowniki EPPO do CSV", "slowniki-eppo-eksport.csv");
        if (selectedPath == null) {
            return;
        }

        try {
            Path exported = eppoDictionaryCsvExportService.export(selectedPath);
            eppoDictionaryCsvStatusLabel.setText(buildCsvExportSuccessStatus("słowników EPPO", exported));
            DialogUtil.showSuccess(buildCsvExportSuccessDialogMessage("Słowniki EPPO", exported));
        } catch (Exception e) {
            e.printStackTrace();
            eppoDictionaryCsvStatusLabel.setText(buildCsvExportErrorStatus("słowników EPPO"));
            DialogUtil.showError(buildCsvExportDialogTitle("słowników EPPO"), buildCsvExportErrorMessage("słowników EPPO"));
        }
    }


    @FXML
    private void clearPlantsCsvPreview() {
        clearPlantsCsvPreviewState();
        resetPlantsCsvPreview();
        if (plantsCsvStatusLabel != null) {
            plantsCsvStatusLabel.setText(buildCsvPreviewClearedStatus("roślin"));
        }
    }

    @FXML
    private void clearContrahentsCsvPreview() {
        clearContrahentsCsvPreviewState();
        resetContrahentsCsvPreview();
        if (contrahentsCsvStatusLabel != null) {
            contrahentsCsvStatusLabel.setText(buildCsvPreviewClearedStatus("kontrahentów"));
        }
    }

    @FXML
    private void clearDocumentsCsvPreview() {
        clearDocumentsCsvPreviewState();
        resetDocumentsCsvPreview();
        if (documentsCsvStatusLabel != null) {
            documentsCsvStatusLabel.setText(buildCsvPreviewClearedStatus("dokumentów"));
        }
    }

    @FXML
    private void clearPlantBatchesCsvPreview() {
        clearPlantBatchesCsvPreviewState();
        resetPlantBatchesCsvPreview();
        if (plantBatchesCsvStatusLabel != null) {
            plantBatchesCsvStatusLabel.setText(buildCsvPreviewClearedStatus("partii roślin"));
        }
    }

    @FXML
    private void clearEppoDictionaryCsvPreview() {
        clearEppoDictionaryCsvPreviewState();
        resetEppoDictionaryCsvPreview();
        if (eppoDictionaryCsvStatusLabel != null) {
            eppoDictionaryCsvStatusLabel.setText(buildCsvPreviewClearedStatus("słowników EPPO"));
        }
    }

    private void clearPlantsCsvPreviewState() {
        lastPlantsCsvPreviewResult = null;
        updatePlantsCsvApplyButtonState();
    }

    private void clearContrahentsCsvPreviewState() {
        lastContrahentsCsvPreviewResult = null;
        updateContrahentsCsvApplyButtonState();
    }

    private void clearDocumentsCsvPreviewState() {
        lastDocumentsCsvPreviewResult = null;
        updateDocumentsCsvApplyButtonState();
    }

    private void clearPlantBatchesCsvPreviewState() {
        lastPlantBatchesCsvPreviewResult = null;
        updatePlantBatchesCsvApplyButtonState();
    }

    private void clearEppoDictionaryCsvPreviewState() {
        lastEppoDictionaryCsvPreviewResult = null;
        updateEppoDictionaryCsvApplyButtonState();
    }

    private void updatePlantsCsvApplyButtonState() {
        if (plantsCsvApplyButton != null) {
            plantsCsvApplyButton.setDisable(lastPlantsCsvPreviewResult == null || lastPlantsCsvPreviewResult.getNewRowsCount() <= 0);
        }
    }

    private void updateContrahentsCsvApplyButtonState() {
        if (contrahentsCsvApplyButton != null) {
            contrahentsCsvApplyButton.setDisable(lastContrahentsCsvPreviewResult == null || lastContrahentsCsvPreviewResult.getNewRowsCount() <= 0);
        }
    }

    private void updateDocumentsCsvApplyButtonState() {
        if (documentsCsvApplyButton != null) {
            documentsCsvApplyButton.setDisable(lastDocumentsCsvPreviewResult == null || lastDocumentsCsvPreviewResult.getDocumentCount() <= 0);
        }
    }

    private void updatePlantBatchesCsvApplyButtonState() {
        if (plantBatchesCsvApplyButton != null) {
            plantBatchesCsvApplyButton.setDisable(lastPlantBatchesCsvPreviewResult == null || lastPlantBatchesCsvPreviewResult.getNewRowsCount() <= 0);
        }
    }

    private void updateEppoDictionaryCsvApplyButtonState() {
        if (eppoDictionaryCsvApplyButton != null) {
            eppoDictionaryCsvApplyButton.setDisable(lastEppoDictionaryCsvPreviewResult == null || lastEppoDictionaryCsvPreviewResult.getImportableRowsCount() <= 0);
        }
    }

    private String buildCsvImportConfirmationTarget(String entityGenitivePlural,
                                                    String sourceName,
                                                    int newCount,
                                                    int skippedCount,
                                                    int rejectedEstimate) {
        return sourceName
                + " — nowe: " + newCount
                + ", do pominięcia: " + skippedCount
                + ", do odrzucenia: " + rejectedEstimate
                + " (sekcja " + entityGenitivePlural + ")";
    }

    private String buildCsvImportExecutionStatus(String entityGenitivePlural, CsvImportExecutionResult result) {
        return "Import CSV " + entityGenitivePlural
                + " zakończony — dodano: " + result.getAddedCount()
                + ", pominięto: " + result.getSkippedCount()
                + ", odrzucono: " + result.getRejectedCount()
                + ".";
    }

    private String buildCsvImportErrorStatus(String entityGenitivePlural) {
        return "Nie udało się wykonać importu CSV " + entityGenitivePlural + ".";
    }

    private String buildCsvImportSuccessDialogMessage(String entityPlural, CsvImportExecutionResult result) {
        return entityPlural + " zaimportowano do bazy."
                + UiTextUtil.DOUBLE_NL
                + "Dodano: " + result.getAddedCount()
                + UiTextUtil.NL
                + "Pominięto: " + result.getSkippedCount()
                + UiTextUtil.NL
                + "Odrzucono: " + result.getRejectedCount();
    }

    private String buildCsvImportAuditDescription(String entityGenitivePlural, CsvImportExecutionResult result) {
        return "Import CSV " + entityGenitivePlural
                + " z pliku " + safe(result.getSourceName())
                + ": dodano " + result.getAddedCount()
                + ", pominięto " + result.getSkippedCount()
                + ", odrzucono " + result.getRejectedCount() + ".";
    }

    private String buildCsvImportExecutionText(String sectionLabel,
                                               CsvImportExecutionResult result,
                                               String previewText) {
        StringBuilder builder = new StringBuilder();
        UiTextUtil.appendSectionHeader(builder, "WYNIK IMPORTU");
        UiTextUtil.appendSummaryLine(builder, "Sekcja", sectionLabel);
        UiTextUtil.appendSummaryLine(builder, "Źródło", safe(result.getSourceName()));
        UiTextUtil.appendSummaryLine(builder, "Łącznie przeanalizowanych wierszy", result.getTotalRowsCount());
        UiTextUtil.appendSummaryLine(builder, "Dodano", result.getAddedCount());
        UiTextUtil.appendSummaryLine(builder, "Pominięto", result.getSkippedCount());
        UiTextUtil.appendSummaryLine(builder, "Odrzucono", result.getRejectedCount());
        UiTextUtil.appendParagraph(builder, "Import został wykonany na podstawie ostatniego podglądu. Aby ponowić import, wygeneruj nowy podgląd pliku.");
        UiTextUtil.appendIssuesSection(builder, "PROBLEMY", result.getProblems());

        if (previewText != null && !previewText.isBlank()) {
            UiTextUtil.appendSectionHeader(builder, "OSTATNI PODGLĄD");
            builder.append(previewText);
        }
        return builder.toString();
    }

    private String buildCsvInitialStatus(String entityGenitivePlural) {
        return "Wybierz plik CSV, aby przygotować podgląd importu " + entityGenitivePlural
                + ", albo zapisz aktualny eksport do pliku.";
    }

    private String buildCsvPreviewErrorStatus(String entityGenitivePlural) {
        return "Nie udało się przygotować podglądu importu " + entityGenitivePlural + ".";
    }

    private String buildCsvPreviewDialogTitle(String entityGenitivePlural) {
        return "Podgląd importu " + entityGenitivePlural;
    }

    private String buildCsvPreviewErrorMessage(String entityGenitivePlural) {
        return "Nie udało się odczytać ani przeanalizować pliku CSV " + entityGenitivePlural + ".";
    }

    private String buildCsvExportSuccessStatus(String entityGenitivePlural, Path exportedPath) {
        return "Eksport " + entityGenitivePlural + " zakończył się powodzeniem: " + exportedPath
                + ". Format pozostaje lokalnym standardem sekcji Ustawienia -> Import / Eksport CSV.";
    }

    private String buildCsvExportSuccessDialogMessage(String entityNominativePlural, Path exportedPath) {
        return entityNominativePlural + " zostały wyeksportowane do pliku:\n" + exportedPath;
    }

    private String buildCsvExportErrorStatus(String entityGenitivePlural) {
        return "Nie udało się wyeksportować " + entityGenitivePlural + " do pliku CSV.";
    }

    private String buildCsvExportDialogTitle(String entityGenitivePlural) {
        return "Eksport " + entityGenitivePlural;
    }

    private String buildCsvExportErrorMessage(String entityGenitivePlural) {
        return "Nie udało się wyeksportować " + entityGenitivePlural + " do pliku CSV.";
    }

    private String buildCsvPreviewClearedStatus(String entityGenitivePlural) {
        return "Podgląd importu " + entityGenitivePlural
                + " został wyczyszczony. Wybierz plik CSV, aby przygotować go ponownie.";
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
        return "CSV roślin — łącznie: " + result.getTotalRowsCount()
                + ", nowych: " + result.getNewRowsCount()
                + ", istniejących: " + result.getMatchingExistingCount()
                + ", duplikatów w pliku: " + result.getDuplicateInFileCount()
                + ", błędnych: " + result.getInvalidRowsCount()
                + ". Ocena: " + buildPlantsImportReadiness(result);
    }

    private String buildContrahentsPreviewStatus(com.egen.fitogen.dto.ContrahentImportPreviewResult result) {
        return "CSV kontrahentów — łącznie: " + result.getTotalRowsCount()
                + ", nowych: " + result.getNewRowsCount()
                + ", istniejących: " + result.getMatchingExistingCount()
                + ", duplikatów w pliku: " + result.getDuplicateInFileCount()
                + ", błędnych: " + result.getInvalidRowsCount()
                + ". Ocena: " + buildContrahentsImportReadiness(result);
    }

    private String buildDocumentsPreviewStatus(com.egen.fitogen.dto.DocumentImportPreviewResult result) {
        return "CSV dokumentów — łącznie wierszy: " + result.getTotalRowsCount()
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
                    "CSV roślin",
                    "Po uruchomieniu analizy zobaczysz tutaj podsumowanie pliku, nagłówki oraz próbkę wierszy."
            ));
        }
    }

    private void resetContrahentsCsvPreview() {
        if (contrahentsCsvPreviewArea != null) {
            contrahentsCsvPreviewArea.setText(UiTextUtil.buildEmptyPreviewText(
                    "CSV kontrahentów",
                    "Po uruchomieniu analizy zobaczysz tutaj podsumowanie pliku, nagłówki oraz próbkę wierszy."
            ));
        }
    }

    private void resetDocumentsCsvPreview() {
        if (documentsCsvPreviewArea != null) {
            documentsCsvPreviewArea.setText(UiTextUtil.buildEmptyPreviewText(
                    "CSV dokumentów",
                    "Po uruchomieniu analizy zobaczysz tutaj podsumowanie pliku, nagłówki oraz próbkę wierszy dokumentów i pozycji."
            ));
        }
    }

    private void resetPlantBatchesCsvPreview() {
        if (plantBatchesCsvPreviewArea != null) {
            plantBatchesCsvPreviewArea.setText(UiTextUtil.buildEmptyPreviewText(
                    "CSV partii roślin",
                    "Po uruchomieniu analizy zobaczysz tutaj podsumowanie pliku, nagłówki oraz próbkę partii roślin wraz z dopasowaniem roślin i źródeł pochodzenia."
            ));
        }
    }

    private String buildPlantBatchesPreviewStatus(PlantBatchImportPreviewResult result) {
        return "CSV partii roślin — łącznie: " + result.getTotalRowsCount()
                + ", nowych: " + result.getNewRowsCount()
                + ", istniejących: " + result.getMatchingExistingCount()
                + ", duplikatów w pliku: " + result.getDuplicateInFileCount()
                + ", błędnych: " + result.getInvalidRowsCount()
                + ". Ocena: " + buildPlantBatchesImportReadiness(result);
    }

        private void resetEppoDictionaryCsvPreview() {
        if (eppoDictionaryCsvPreviewArea != null) {
            eppoDictionaryCsvPreviewArea.setText(UiTextUtil.buildEmptyPreviewText(
                    "CSV słowników EPPO",
                    "Po uruchomieniu analizy zobaczysz tutaj podsumowanie pliku, nagłówki oraz próbkę relacji kod EPPO → gatunek i kod EPPO → strefa."
            ));
        }
    }

    private String buildPlantBatchesPreviewText(PlantBatchImportPreviewResult result) {
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

        UiTextUtil.appendSectionHeader(builder, "OCENA WALIDACJI");
        UiTextUtil.appendSummaryLine(builder, "Gotowość importu", buildPlantBatchesImportReadiness(result));
        UiTextUtil.appendSummaryLine(builder, "Rekomendacja", buildPlantBatchesImportRecommendation(result));
        UiTextUtil.appendEmptyLine(builder);

        UiTextUtil.appendSectionHeader(builder, "PRÓBKA WIERSZY");
        int previewLimit = Math.min(result.getRows().size(), 10);
        for (int i = 0; i < previewLimit; i++) {
            var row = result.getRows().get(i);
            builder.append("#").append(row.getRowNumber())
                    .append(" [").append(safe(row.getStatus())).append("] ")
                    .append(fallback(safe(row.getInteriorBatchNo()), fallback(safe(row.getExteriorBatchNo()), "[bez numeru]")))
                    .append(" | gatunek: ").append(safe(row.getPlantSpecies()));
            if (!safe(row.getPlantVariety()).isBlank()) {
                builder.append(" | odmiana: ").append(safe(row.getPlantVariety()));
            }
            if (!safe(row.getPlantRootstock()).isBlank()) {
                builder.append(" | podkładka: ").append(safe(row.getPlantRootstock()));
            }
            if (!safe(row.getSourceContrahentName()).isBlank()) {
                builder.append(" | źródło: ").append(safe(row.getSourceContrahentName()));
            }
            builder.append(" | ilość: ").append(row.getQty())
                    .append(" | wiek: ").append(row.getAgeYears())
                    .append(" | data: ").append(safe(row.getCreationDate()))
                    .append(" | wewnętrzna: ").append(row.isInternalSource() ? "tak" : "nie");
            if (!safe(row.getManufacturerCountryCode()).isBlank()) {
                builder.append(" | kraj: ").append(safe(row.getManufacturerCountryCode()));
            }
            if (!safe(row.getEppoCode()).isBlank()) {
                builder.append(" | EPPO: ").append(safe(row.getEppoCode()));
            }
            if (!safe(row.getBatchStatus()).isBlank()) {
                builder.append(" | status partii: ").append(safe(row.getBatchStatus()));
            }
            if (row.getMessage() != null && !row.getMessage().isBlank()) {
                builder.append(" | uwaga: ").append(row.getMessage());
            }
            builder.append(UiTextUtil.NL);
        }

        UiTextUtil.appendPreviewLimitNote(builder, previewLimit, result.getRows().size());
        List<String> problemRows = new ArrayList<>();
        for (var row : result.getRows()) {
            if (row.getMessage() != null && !row.getMessage().isBlank()) {
                problemRows.add("#" + row.getRowNumber() + " [" + row.getStatus() + "] " + row.getMessage());
            }
            if (problemRows.size() >= 6) {
                break;
            }
        }
        UiTextUtil.appendIssuesSection(builder, "NAJWAŻNIEJSZE UWAGI", problemRows);
        return builder.toString();
    }

    private String buildPlantBatchesImportReadiness(PlantBatchImportPreviewResult result) {
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
            return "brak nowych partii do importu";
        }
        return "gotowy do bezpiecznego importu";
    }

    private String buildPlantBatchesImportRecommendation(PlantBatchImportPreviewResult result) {
        if (result.getTotalRowsCount() == 0) {
            return "Sprawdź, czy plik zawiera nagłówek i co najmniej jeden wiersz danych partii.";
        }
        if (result.getInvalidRowsCount() > 0) {
            return "Najpierw popraw błędne wiersze, szczególnie brak dopasowania rośliny, źródła pochodzenia lub nieprawidłową ilość, wiek i datę.";
        }
        if (result.getDuplicateInFileCount() > 0) {
            return "Usuń duplikaty partii w samym pliku CSV, aby import był przewidywalny.";
        }
        if (result.getNewRowsCount() == 0) {
            return "Plik nie wniesie nowych partii. Zweryfikuj, czy to właściwy eksport lub zestaw danych.";
        }
        return "Podgląd wygląda spójnie. Możesz zaimportować tylko te partie, które nie istnieją jeszcze w bazie.";
    }

        private String buildEppoDictionaryPreviewStatus(EppoDictionaryImportPreviewResult result) {
        return "Podgląd importu słowników EPPO gotowy: nowych relacji " + result.getNewRowsCount()
                + ", aktualizacji " + result.getUpdateRowsCount()
                + ", istniejących " + result.getMatchingExistingCount()
                + ", duplikatów " + result.getDuplicateInFileCount()
                + ", błędnych " + result.getInvalidRowsCount() + ".";
    }

    private String buildEppoDictionaryPreviewText(EppoDictionaryImportPreviewResult result) {
        StringBuilder builder = new StringBuilder();
        appendPreviewFileSummary(builder,
                result.getSourceName(),
                result.getDelimiter(),
                result.getResolvedHeaders(),
                List.of(
                        new PreviewSummaryRow("Łącznie wierszy", result.getTotalRowsCount()),
                        new PreviewSummaryRow("Nowe relacje", result.getNewRowsCount()),
                        new PreviewSummaryRow("Relacje do aktualizacji", result.getUpdateRowsCount()),
                        new PreviewSummaryRow("Relacje istniejące", result.getMatchingExistingCount()),
                        new PreviewSummaryRow("Duplikaty w pliku", result.getDuplicateInFileCount()),
                        new PreviewSummaryRow("Błędne wiersze", result.getInvalidRowsCount())
                ));

        UiTextUtil.appendSectionHeader(builder, "OCENA WALIDACJI");
        UiTextUtil.appendSummaryLine(builder, "Gotowość importu", buildEppoDictionaryImportReadiness(result));
        UiTextUtil.appendSummaryLine(builder, "Rekomendacja", buildEppoDictionaryImportRecommendation(result));
        UiTextUtil.appendEmptyLine(builder);

        UiTextUtil.appendSectionHeader(builder, "PRÓBKA WIERSZY");
        int previewLimit = Math.min(result.getRows().size(), 12);
        for (int i = 0; i < previewLimit; i++) {
            var row = result.getRows().get(i);
            builder.append("#").append(row.getRowNumber())
                    .append(" [").append(safe(row.getStatus())).append("] ")
                    .append(safe(row.getRelationType()))
                    .append(" | EPPO: ").append(safe(row.getEppoCode()));
            if ("SPECIES".equalsIgnoreCase(safe(row.getRelationType()))) {
                if (!safe(row.getSpeciesName()).isBlank()) {
                    builder.append(" | gatunek: ").append(safe(row.getSpeciesName()));
                }
                if (!safe(row.getLatinSpeciesName()).isBlank()) {
                    builder.append(" | nazwa łacińska: ").append(safe(row.getLatinSpeciesName()));
                }
            } else if ("ZONE".equalsIgnoreCase(safe(row.getRelationType()))) {
                builder.append(" | strefa: ").append(safe(row.getZoneCode()));
                if (!safe(row.getZoneName()).isBlank()) {
                    builder.append(" (").append(safe(row.getZoneName())).append(")");
                }
                if (!safe(row.getCountryCode()).isBlank()) {
                    builder.append(" | kraj: ").append(safe(row.getCountryCode()));
                }
            }
            builder.append(" | paszport: ").append(row.isPassportRequired() ? "tak" : "nie");
            if (!safe(row.getCodeStatus()).isBlank()) {
                builder.append(" | status kodu: ").append(safe(row.getCodeStatus()));
            }
            if (!safe(row.getZoneStatus()).isBlank()) {
                builder.append(" | status strefy: ").append(safe(row.getZoneStatus()));
            }
            if (!safe(row.getMessage()).isBlank()) {
                builder.append(" | uwaga: ").append(safe(row.getMessage()));
            }
            builder.append(UiTextUtil.NL);
        }

        UiTextUtil.appendPreviewLimitNote(builder, previewLimit, result.getRows().size());
        List<String> problemRows = new ArrayList<>();
        for (var row : result.getRows()) {
            if (row.getMessage() != null && !row.getMessage().isBlank()) {
                problemRows.add("#" + row.getRowNumber() + " [" + row.getStatus() + "] " + row.getMessage());
            }
            if (problemRows.size() >= 6) {
                break;
            }
        }
        UiTextUtil.appendIssuesSection(builder, "NAJWAŻNIEJSZE UWAGI", problemRows);
        return builder.toString();
    }

    private String buildEppoDictionaryImportReadiness(EppoDictionaryImportPreviewResult result) {
        if (result.getInvalidRowsCount() > 0) {
            return "Wymaga korekty pliku";
        }
        if (result.getImportableRowsCount() > 0) {
            return "Gotowe do importu";
        }
        if (result.getMatchingExistingCount() > 0 && result.getImportableRowsCount() == 0) {
            return "Brak zmian do wykonania";
        }
        return "Brak danych do importu";
    }

    private String buildEppoDictionaryImportRecommendation(EppoDictionaryImportPreviewResult result) {
        if (result.getInvalidRowsCount() > 0) {
            return "Popraw błędne wiersze przed importem słowników EPPO.";
        }
        if (result.getImportableRowsCount() > 0) {
            return "Możesz bezpiecznie wykonać import relacji kod EPPO → gatunek i kod EPPO → strefa.";
        }
        return "Nie ma nowych ani aktualizowanych relacji do zapisania w bazie.";
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

        UiTextUtil.appendPreviewLimitNote(builder, previewLimit, result.getRows().size());
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

        UiTextUtil.appendPreviewLimitNote(builder, previewLimit, result.getRows().size());
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

        UiTextUtil.appendPreviewLimitNote(builder, previewLimit, result.getRows().size());
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
        return "gotowy do bezpiecznego importu";
    }

    private String buildDocumentsImportRecommendation(com.egen.fitogen.dto.DocumentImportPreviewResult result) {
        if (result.getTotalRowsCount() == 0) {
            return "Przygotuj plik z numerem dokumentu, typem, datą i pozycjami dokumentu.";
        }
        if (result.getInvalidRowsCount() > 0) {
            return "Najpierw popraw błędne dane dokumentu, brakujące powiązania lub niedozwolone partie roślin.";
        }
        if (result.getDuplicateInFileCount() > 0) {
            return "Usuń zduplikowane pozycje tego samego dokumentu przed dalszym użyciem pliku.";
        }
        if (result.getDocumentCount() == 0) {
            return "Plik wygląda poprawnie, ale nie wnosi nowych numerów dokumentów względem aktualnej bazy.";
        }
        return "Podgląd wygląda spójnie z lokalnym standardem CSV dokumentów i może przejść do świadomego importu.";
    }

    private String buildDocumentsImportExecutionStatus(CsvImportExecutionResult result) {
        return "Import CSV dokumentów zakończony — dodano dokumentów: " + result.getAddedCount()
                + ", pominięto dokumentów: " + result.getSkippedCount()
                + ", odrzucono dokumentów: " + result.getRejectedCount()
                + ".";
    }

    private String buildDocumentsImportSuccessDialogMessage(CsvImportExecutionResult result) {
        return "Dokumenty zaimportowano do bazy."
                + UiTextUtil.DOUBLE_NL
                + "Dodano dokumentów: " + result.getAddedCount()
                + UiTextUtil.NL
                + "Pominięto dokumentów: " + result.getSkippedCount()
                + UiTextUtil.NL
                + "Odrzucono dokumentów: " + result.getRejectedCount();
    }

    private String buildDocumentsImportAuditDescription(CsvImportExecutionResult result) {
        return "Import CSV dokumentów z pliku " + safe(result.getSourceName())
                + ": dodano dokumentów " + result.getAddedCount()
                + ", pominięto dokumentów " + result.getSkippedCount()
                + ", odrzucono dokumentów " + result.getRejectedCount() + ".";
    }

    private String buildDocumentsImportExecutionText(CsvImportExecutionResult result, String previewText) {
        StringBuilder builder = new StringBuilder();
        UiTextUtil.appendSectionHeader(builder, "WYNIK IMPORTU");
        UiTextUtil.appendSummaryLine(builder, "Sekcja", "CSV dokumentów");
        UiTextUtil.appendSummaryLine(builder, "Źródło", safe(result.getSourceName()));
        UiTextUtil.appendSummaryLine(builder, "Łącznie przeanalizowanych wierszy", result.getTotalRowsCount());
        UiTextUtil.appendSummaryLine(builder, "Dodano dokumentów", result.getAddedCount());
        UiTextUtil.appendSummaryLine(builder, "Pominięto dokumentów", result.getSkippedCount());
        UiTextUtil.appendSummaryLine(builder, "Odrzucono dokumentów", result.getRejectedCount());
        UiTextUtil.appendParagraph(builder, "Import został wykonany atomowo dla każdego numeru dokumentu. Aby ponowić import, wygeneruj nowy podgląd pliku.");
        UiTextUtil.appendIssuesSection(builder, "PROBLEMY", result.getProblems());

        if (previewText != null && !previewText.isBlank()) {
            UiTextUtil.appendSectionHeader(builder, "OSTATNI PODGLĄD");
            builder.append(previewText);
        }
        return builder.toString();
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
        return "gotowy do bezpiecznego importu";
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
        return "gotowy do bezpiecznego importu";
    }

    private String buildPlantsImportRecommendation(com.egen.fitogen.dto.PlantImportPreviewResult result) {
        if (result.getTotalRowsCount() == 0) {
            return "Sprawdź, czy plik zawiera nagłówek i co najmniej jeden wiersz danych.";
        }
        if (result.getInvalidRowsCount() > 0) {
            return "Najpierw popraw wiersze błędne, szczególnie brak gatunku i nieprawidłowy status widoczności.";
        }
        if (result.getDuplicateInFileCount() > 0) {
            return "Usuń duplikaty w samym pliku CSV, aby import był przewidywalny.";
        }
        if (result.getNewRowsCount() == 0) {
            return "Plik nie wniesie nowych roślin. Zweryfikuj, czy to właściwy eksport lub zestaw danych.";
        }
        return "Podgląd wygląda spójnie. Zachowaj ten sam standard kolumn w kolejnych plikach lokalnych CSV.";
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
        return "Podgląd wygląda spójnie. Zachowaj ten sam standard kolumn w kolejnych plikach lokalnych CSV.";
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
