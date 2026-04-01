package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoCodeSpeciesLink;
import com.egen.fitogen.model.EppoZone;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.EppoCodeService;
import com.egen.fitogen.service.EppoCodeSpeciesLinkService;
import com.egen.fitogen.service.EppoCodeZoneLinkService;
import com.egen.fitogen.service.EppoZoneService;
import com.egen.fitogen.service.PlantService;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ModalViewUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

public class EppoAdminController {

    @FXML private Label codeCountLabel;
    @FXML private Label zoneCountLabel;
    @FXML private Label plantCountLabel;
    @FXML private Label sharedCountryCountLabel;
    @FXML private ListView<String> sharedCountryList;

    @FXML private TextField codeSearchField;
    @FXML private TableView<EppoCode> codeTable;
    @FXML private TableColumn<EppoCode, Integer> colCodeId;
    @FXML private TableColumn<EppoCode, String> colCodeValue;
    @FXML private TableColumn<EppoCode, String> colCodeName;
    @FXML private TableColumn<EppoCode, String> colCodeLatinName;
    @FXML private TableColumn<EppoCode, String> colStatus;

    @FXML private Label selectedCodeLabel;
    @FXML private Label recordSummaryLabel;
    @FXML private TextField recordSearchField;
    @FXML private TableView<EppoRecordRow> recordTable;
    @FXML private TableColumn<EppoRecordRow, Integer> colRecordZoneId;
    @FXML private TableColumn<EppoRecordRow, String> colRecordEppoCode;
    @FXML private TableColumn<EppoRecordRow, String> colRecordEppoName;
    @FXML private TableColumn<EppoRecordRow, String> colRecordEppoLatinName;
    @FXML private TableColumn<EppoRecordRow, String> colRecordSpeciesName;
    @FXML private TableColumn<EppoRecordRow, String> colRecordSpeciesLatinName;
    @FXML private TableColumn<EppoRecordRow, String> colRecordCountry;
    @FXML private TableColumn<EppoRecordRow, String> colRecordStatus;

    private final EppoCodeService eppoCodeService = AppContext.getEppoCodeService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();
    private final EppoZoneService eppoZoneService = AppContext.getEppoZoneService();
    private final EppoCodeZoneLinkService eppoCodeZoneLinkService = AppContext.getEppoCodeZoneLinkService();
    private final EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService = AppContext.getEppoCodeSpeciesLinkService();
    private final PlantService plantService = AppContext.getPlantService();

    private final ObservableList<EppoCode> codeMasterData = FXCollections.observableArrayList();
    private final ObservableList<EppoRecordRow> recordMasterData = FXCollections.observableArrayList();

    private FilteredList<EppoCode> filteredCodeData;
    private FilteredList<EppoRecordRow> filteredRecordData;

    @FXML
    public void initialize() {
        configureCodeTable();
        configureRecordTable();
        configureSharedCountryList();
        configureRowFactories();
        configureCodeSearch();
        configureRecordSearch();
        configureSelectionBehavior();
        refresh();
    }

    @FXML
    private void addCode() {
        ModalViewUtil.openModal(
                "/view/eppo_code_form.fxml",
                "Dodaj kod EPPO",
                1220,
                980,
                1120,
                860,
                (EppoCodeFormController controller) -> controller.setEppoCode(null)
        );

        refresh();
    }

    @FXML
    private void editSelectedCode() {
        EppoCode selected = codeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz kod EPPO do edycji.");
            return;
        }

        ModalViewUtil.openModal(
                "/view/eppo_code_form.fxml",
                "Edytuj kod EPPO",
                1220,
                980,
                1120,
                860,
                (EppoCodeFormController controller) -> controller.setEppoCode(selected)
        );

        refresh();
    }

    @FXML
    private void refreshCodes() {
        refresh();
    }

    @FXML
    private void addZone() {
        ModalViewUtil.openModal(
                "/view/eppo_zone_form.fxml",
                "Dodaj kraj EPPO",
                720,
                390,
                660,
                350,
                (EppoZoneFormController controller) -> controller.setEppoZone(null)
        );

        refresh();
    }

    @FXML
    private void editSelectedZone() {
        EppoRecordRow selectedRecord = recordTable.getSelectionModel().getSelectedItem();
        if (selectedRecord == null || selectedRecord.getZoneId() == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz rekord z krajem / strefą EPPO do edycji.");
            return;
        }

        EppoZone selectedZone = zoneById(selectedRecord.getZoneId());
        if (selectedZone == null) {
            DialogUtil.showWarning("Brak danych", "Nie udało się odnaleźć wybranego kraju / strefy EPPO.");
            return;
        }

        ModalViewUtil.openModal(
                "/view/eppo_zone_form.fxml",
                "Edytuj kraj EPPO",
                720,
                390,
                660,
                350,
                (EppoZoneFormController controller) -> controller.setEppoZone(selectedZone)
        );

        refresh();
    }

    @FXML
    private void deleteSelectedZone() {
        EppoRecordRow selectedRecord = recordTable.getSelectionModel().getSelectedItem();
        if (selectedRecord == null || selectedRecord.getZoneId() == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz rekord z krajem / strefą EPPO do usunięcia.");
            return;
        }

        EppoZone selectedZone = zoneById(selectedRecord.getZoneId());
        if (selectedZone == null) {
            DialogUtil.showWarning("Brak danych", "Nie udało się odnaleźć wybranego kraju / strefy EPPO.");
            return;
        }

        String zoneLabel = selectedZone.getCode() + " - " + (selectedZone.getName() == null ? "" : selectedZone.getName());
        if (!DialogUtil.confirmDelete(zoneLabel)) {
            return;
        }

        try {
            eppoCodeZoneLinkService.deleteAllForZone(selectedZone.getId());
            eppoZoneService.delete(selectedZone.getId());
            DialogUtil.showSuccess("Kraj / strefa EPPO została usunięta.");
            refresh();

        } catch (IllegalArgumentException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd usuwania", "Nie udało się usunąć kraju / strefy EPPO.");
        }
    }

    @FXML
    private void refreshZones() {
        refresh();
    }

    private EppoZone zoneById(Integer zoneId) {
        if (zoneId == null || eppoZoneService == null) {
            return null;
        }
        return eppoZoneService.getAll().stream()
                .filter(this::isMeaningfulZone)
                .filter(zone -> zone.getId() == zoneId)
                .findFirst()
                .orElse(null);
    }

    private void configureCodeTable() {
        colCodeId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCodeValue.setCellValueFactory(new PropertyValueFactory<>("code"));
        colCodeName.setCellValueFactory(cell ->
                new SimpleStringProperty(buildCodePolishName(cell.getValue()))
        );
        colCodeLatinName.setCellValueFactory(cell ->
                new SimpleStringProperty(buildCodeLatinName(cell.getValue()))
        );
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void configureRecordTable() {
        colRecordZoneId.setCellValueFactory(cell ->
                new SimpleIntegerProperty(cell.getValue().getZoneId() == null ? 0 : cell.getValue().getZoneId()).asObject()
        );
        colRecordEppoCode.setCellValueFactory(new PropertyValueFactory<>("eppoCode"));
        colRecordEppoName.setCellValueFactory(new PropertyValueFactory<>("eppoName"));
        colRecordEppoLatinName.setCellValueFactory(new PropertyValueFactory<>("eppoLatinName"));
        colRecordSpeciesName.setCellValueFactory(new PropertyValueFactory<>("speciesName"));
        colRecordSpeciesLatinName.setCellValueFactory(new PropertyValueFactory<>("speciesLatinName"));
        colRecordCountry.setCellValueFactory(new PropertyValueFactory<>("country"));
        colRecordStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }


    private void configureSharedCountryList() {
        if (sharedCountryList == null) {
            return;
        }
        sharedCountryList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
            }
        });
    }

    private void configureRowFactories() {
        codeTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(EppoCode item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("inactive-row");

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                if (isInactiveStatus(item.getStatus())) {
                    getStyleClass().add("inactive-row");
                }
            }
        });

        recordTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(EppoRecordRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("inactive-row");

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                if (isInactiveStatus(item.getStatus())) {
                    getStyleClass().add("inactive-row");
                }
            }
        });
    }

    private boolean isInactiveStatus(String status) {
        return status != null && status.trim().equalsIgnoreCase("INACTIVE");
    }

    private void configureCodeSearch() {
        filteredCodeData = new FilteredList<>(codeMasterData, item -> true);

        if (codeSearchField != null) {
            codeSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyCodeFilter());
        }

        SortedList<EppoCode> sortedData = new SortedList<>(filteredCodeData);
        sortedData.comparatorProperty().bind(codeTable.comparatorProperty());
        codeTable.setItems(sortedData);
    }

    private void configureRecordSearch() {
        filteredRecordData = new FilteredList<>(recordMasterData, item -> true);

        if (recordSearchField != null) {
            recordSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyRecordFilter());
        }

        SortedList<EppoRecordRow> sortedData = new SortedList<>(filteredRecordData);
        sortedData.comparatorProperty().bind(recordTable.comparatorProperty());
        recordTable.setItems(sortedData);
    }

    private void configureSelectionBehavior() {
        codeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            updateSelectedCodeHeader(newValue);
            rebuildRecordRows(newValue);
            applyRecordFilter();
            updateRecordSummary(newValue);
        });
    }

    private void refresh() {
        EppoCode previouslySelectedCode = codeTable.getSelectionModel().getSelectedItem();
        Integer previouslySelectedCodeId = previouslySelectedCode == null ? null : previouslySelectedCode.getId();

        List<EppoCode> codes = eppoCodeService == null ? List.of() : eppoCodeService.getAll().stream()
                                                                     .filter(this::isMeaningfulCode)
                                                                     .toList();
        List<EppoZone> zones = eppoZoneService == null ? List.of() : eppoZoneService.getAll().stream()
                                                                     .filter(this::isMeaningfulZone)
                                                                     .toList();
        List<Plant> plants = plantService == null ? List.of() : plantService.getAllPlants().stream()
                                                                .filter(this::isMeaningfulPlant)
                                                                .toList();

        codeMasterData.setAll(codes);

        codeCountLabel.setText(String.valueOf(codes.size()));
        zoneCountLabel.setText(String.valueOf(zones.size()));
        plantCountLabel.setText(String.valueOf(plants.size()));
        if (sharedCountryCountLabel != null) {
            sharedCountryCountLabel.setText(String.valueOf(countryDirectoryService == null ? 0 : countryDirectoryService.getEntries().size()));
        }
        if (sharedCountryList != null) {
            List<String> sharedCountries = countryDirectoryService == null
                    ? List.of()
                    : countryDirectoryService.getEntries().stream()
                            .map(entry -> entry.country() + " (" + entry.countryCode() + ")")
                            .toList();
            sharedCountryList.setItems(FXCollections.observableArrayList(sharedCountries));
        }

        applyCodeFilter();
        restoreCodeSelection(previouslySelectedCodeId);
        ensureCodeSelection();

        EppoCode selectedCode = codeTable.getSelectionModel().getSelectedItem();
        updateSelectedCodeHeader(selectedCode);
        rebuildRecordRows(selectedCode);
        applyRecordFilter();
        updateRecordSummary(selectedCode);
    }

    private void ensureCodeSelection() {
        if (codeTable == null || !codeTable.getSelectionModel().isEmpty()) {
            return;
        }

        if (!codeTable.getItems().isEmpty()) {
            codeTable.getSelectionModel().selectFirst();
            codeTable.scrollTo(0);
        }
    }

    private void restoreCodeSelection(Integer codeId) {
        if (codeId == null) {
            return;
        }

        for (EppoCode code : codeTable.getItems()) {
            if (code.getId() == codeId) {
                codeTable.getSelectionModel().select(code);
                codeTable.scrollTo(code);
                break;
            }
        }
    }

    private void applyCodeFilter() {
        String keyword = codeSearchField == null || codeSearchField.getText() == null
                ? ""
                : codeSearchField.getText().trim().toLowerCase();

        filteredCodeData.setPredicate(item -> {
            if (!isMeaningfulCode(item)) {
                return false;
            }

            if (keyword.isBlank()) {
                return true;
            }

            return safeContains(item.getCode(), keyword)
                    || safeContains(buildCodePolishName(item), keyword)
                    || safeContains(buildCodeLatinName(item), keyword)
                    || safeContains(item.getStatus(), keyword);
        });
    }

    private void applyRecordFilter() {
        String keyword = recordSearchField == null || recordSearchField.getText() == null
                ? ""
                : recordSearchField.getText().trim().toLowerCase();

        filteredRecordData.setPredicate(item -> {
            if (item == null) {
                return false;
            }
            if (keyword.isBlank()) {
                return true;
            }
            return safeContains(item.getEppoCode(), keyword)
                    || safeContains(item.getEppoName(), keyword)
                    || safeContains(item.getEppoLatinName(), keyword)
                    || safeContains(item.getSpeciesName(), keyword)
                    || safeContains(item.getSpeciesLatinName(), keyword)
                    || safeContains(item.getCountry(), keyword)
                    || safeContains(item.getStatus(), keyword);
        });
    }

    private void rebuildRecordRows(EppoCode selectedCode) {
        recordMasterData.clear();

        if (selectedCode == null) {
            return;
        }

        List<EppoCodeSpeciesLink> speciesLinks = eppoCodeSpeciesLinkService == null
                ? List.of()
                : eppoCodeSpeciesLinkService.getEffectiveSpeciesLinks(selectedCode.getId()).stream()
                  .filter(this::isMeaningfulSpeciesLink)
                  .toList();

        List<EppoZone> linkedZones = eppoCodeZoneLinkService == null
                ? List.of()
                : eppoCodeZoneLinkService.getZonesForCode(selectedCode.getId()).stream()
                  .filter(this::isMeaningfulZone)
                  .toList();

        String eppoCode = firstNonBlank(selectedCode.getCode(), "—");
        String eppoName = buildCodePolishName(selectedCode);
        String eppoLatinName = buildCodeLatinName(selectedCode);
        String status = firstNonBlank(normalizeDisplayValue(selectedCode.getStatus()), "—");

        List<EppoRecordRow> rows = new ArrayList<>();

        if (!speciesLinks.isEmpty() && !linkedZones.isEmpty()) {
            for (EppoCodeSpeciesLink speciesLink : speciesLinks) {
                for (EppoZone zone : linkedZones) {
                    rows.add(new EppoRecordRow(
                            zone.getId(),
                            eppoCode,
                            eppoName,
                            eppoLatinName,
                            firstNonBlank(normalizeDisplayValue(speciesLink.getSpeciesName()), "—"),
                            firstNonBlank(normalizeDisplayValue(speciesLink.getLatinSpeciesName()), "—"),
                            buildCountryDisplay(zone),
                            status
                    ));
                }
            }
        } else if (!speciesLinks.isEmpty()) {
            for (EppoCodeSpeciesLink speciesLink : speciesLinks) {
                rows.add(new EppoRecordRow(
                        null,
                        eppoCode,
                        eppoName,
                        eppoLatinName,
                        firstNonBlank(normalizeDisplayValue(speciesLink.getSpeciesName()), "—"),
                        firstNonBlank(normalizeDisplayValue(speciesLink.getLatinSpeciesName()), "—"),
                        "—",
                        status
                ));
            }
        } else if (!linkedZones.isEmpty()) {
            for (EppoZone zone : linkedZones) {
                rows.add(new EppoRecordRow(
                        zone.getId(),
                        eppoCode,
                        eppoName,
                        eppoLatinName,
                        "—",
                        "—",
                        buildCountryDisplay(zone),
                        status
                ));
            }
        }

        recordMasterData.setAll(rows);
    }

    private String buildCountryDisplay(EppoZone zone) {
        if (zone == null) {
            return "—";
        }
        return firstNonBlank(
                combineCountryCodeAndName(zone.getCode(), zone.getName()),
                combineCountryCodeAndName(zone.getCountryCode(), zone.getName()),
                normalizeDisplayValue(zone.getName()),
                normalizeDisplayValue(zone.getCode()),
                "—"
        );
    }

    private String combineCountryCodeAndName(String code, String name) {
        String normalizedCode = normalizeDisplayValue(code);
        String normalizedName = normalizeDisplayValue(name);

        if (notBlank(normalizedCode) && notBlank(normalizedName)) {
            return normalizedCode + " / " + normalizedName;
        }
        return firstNonBlank(normalizedCode, normalizedName);
    }

    private void updateRecordSummary(EppoCode selectedCode) {
        if (recordSummaryLabel == null) {
            return;
        }

        if (selectedCode == null) {
            recordSummaryLabel.setText("Brak wybranego kodu EPPO.");
            return;
        }

        int speciesCount = eppoCodeSpeciesLinkService == null ? 0 : eppoCodeSpeciesLinkService.getEffectiveSpeciesLinks(selectedCode.getId()).size();
        int zoneCount = eppoCodeZoneLinkService == null ? 0 : eppoCodeZoneLinkService.getZonesForCode(selectedCode.getId()).size();
        recordSummaryLabel.setText(
                "Powiązane gatunki: " + speciesCount
                        + " | Powiązane kraje / strefy: " + zoneCount
                        + " | Wiersze w Bazie rekordów: " + recordTable.getItems().size()
        );
    }

    private void updateSelectedCodeHeader(EppoCode selectedCode) {
        if (selectedCodeLabel == null) {
            return;
        }
        if (selectedCode == null) {
            selectedCodeLabel.setText("Wybrany kod EPPO: —");
            return;
        }

        selectedCodeLabel.setText(
                "Wybrany kod EPPO: " + selectedCode.getCode()
                        + " | Nazwa polska: " + buildCodePolishName(selectedCode)
                        + " | Nazwa łacińska: " + buildCodeLatinName(selectedCode)
        );
    }

    private String buildCodePolishName(EppoCode code) {
        if (code == null) {
            return "—";
        }
        return firstNonBlank(
                normalizeDisplayValue(code.getCommonName()),
                normalizeDisplayValue(code.getDisplaySpeciesName()),
                "—"
        );
    }

    private String buildCodeLatinName(EppoCode code) {
        if (code == null) {
            return "—";
        }
        return firstNonBlank(
                normalizeDisplayValue(code.getScientificName()),
                normalizeDisplayValue(code.getDisplayLatinSpeciesName()),
                "—"
        );
    }

    private boolean isMeaningfulCode(EppoCode code) {
        return code != null && notBlank(code.getCode());
    }

    private boolean isMeaningfulZone(EppoZone zone) {
        return zone != null && (notBlank(zone.getCode()) || notBlank(zone.getName()) || notBlank(zone.getCountryCode()));
    }

    private boolean isMeaningfulPlant(Plant plant) {
        return plant != null && (notBlank(plant.getSpecies()) || notBlank(plant.getLatinSpeciesName()) || notBlank(plant.getVariety()));
    }

    private boolean isMeaningfulSpeciesLink(EppoCodeSpeciesLink link) {
        return link != null && (notBlank(link.getSpeciesName()) || notBlank(link.getLatinSpeciesName()));
    }

    private boolean safeContains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (notBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String normalizeDisplayValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    public static class EppoRecordRow {
        private final Integer zoneId;
        private final String eppoCode;
        private final String eppoName;
        private final String eppoLatinName;
        private final String speciesName;
        private final String speciesLatinName;
        private final String country;
        private final String status;

        public EppoRecordRow(Integer zoneId,
                             String eppoCode,
                             String eppoName,
                             String eppoLatinName,
                             String speciesName,
                             String speciesLatinName,
                             String country,
                             String status) {
            this.zoneId = zoneId;
            this.eppoCode = eppoCode;
            this.eppoName = eppoName;
            this.eppoLatinName = eppoLatinName;
            this.speciesName = speciesName;
            this.speciesLatinName = speciesLatinName;
            this.country = country;
            this.status = status;
        }

        public Integer getZoneId() {
            return zoneId;
        }

        public String getEppoCode() {
            return eppoCode;
        }

        public String getEppoName() {
            return eppoName;
        }

        public String getEppoLatinName() {
            return eppoLatinName;
        }

        public String getSpeciesName() {
            return speciesName;
        }

        public String getSpeciesLatinName() {
            return speciesLatinName;
        }

        public String getCountry() {
            return country;
        }

        public String getStatus() {
            return status;
        }
    }
}