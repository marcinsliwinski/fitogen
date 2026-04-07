package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.EppoCode;
import com.egen.fitogen.model.EppoCodeSpeciesLink;
import com.egen.fitogen.model.EppoZone;
import com.egen.fitogen.model.Plant;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;
import com.egen.fitogen.ui.util.UiTextUtil;

public class EppoAdminController {

    @FXML private Label codeCountLabel;
    @FXML private Label zoneCountLabel;
    @FXML private Label plantCountLabel;

    @FXML private TextField codeSearchField;
    @FXML private Label codeSummaryLabel;
    @FXML private TableView<EppoCode> codeTable;
    @FXML private TableColumn<EppoCode, Integer> colCodeId;
    @FXML private TableColumn<EppoCode, String> colCodeValue;
    @FXML private TableColumn<EppoCode, String> colCodeName;
    @FXML private TableColumn<EppoCode, String> colCodeLatinName;
    @FXML private TableColumn<EppoCode, String> colStatus;

    @FXML private Label selectedCodeLabel;
    @FXML private Label selectedCodeSummaryLabel;
    @FXML private Label selectedCodeAssignmentsLabel;
    @FXML private Label selectedCodeLegacyLabel;
    @FXML private Label recordSummaryLabel;
    @FXML private TextField recordSearchField;
    @FXML private Label recordDetailLabel;
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
        configureRowFactories();
        configureCodeSearch();
        configureRecordSearch();
        configureTableBehavior();
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

        if (recordTable != null) {
            recordTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        }
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


    private void configureTableBehavior() {
        if (codeTable != null) {
            codeTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            codeTable.setPlaceholder(new Label("Brak kodów EPPO do wyświetlenia."));
        }
        if (recordTable != null) {
            recordTable.setPlaceholder(new Label("Brak rekordów EPPO do wyświetlenia."));
        }
    }

    private void configureSelectionBehavior() {
        codeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            updateSelectedCodeHeader(newValue);
            updateSelectedCodeSummary(newValue);
            updateCodeSummary();
        });
        recordTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                updateRecordDetail(newValue)
        );
    }

    private void refresh() {
        EppoCode previouslySelectedCode = codeTable == null ? null : codeTable.getSelectionModel().getSelectedItem();
        Integer previouslySelectedCodeId = previouslySelectedCode == null ? null : previouslySelectedCode.getId();
        EppoRecordRow previouslySelectedRecord = recordTable == null ? null : recordTable.getSelectionModel().getSelectedItem();
        String previouslySelectedRecordSignature = buildRecordSignature(previouslySelectedRecord);

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

        applyCodeFilter();
        restoreCodeSelection(previouslySelectedCodeId);
        ensureCodeSelection();

        EppoCode selectedCode = codeTable == null ? null : codeTable.getSelectionModel().getSelectedItem();
        updateSelectedCodeHeader(selectedCode);
        updateSelectedCodeSummary(selectedCode);
        updateCodeSummary();
        rebuildRecordRows();
        applyRecordFilter();
        restoreRecordSelection(previouslySelectedRecordSignature, selectedCode);
        updateRecordSummary();
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

    private void restoreRecordSelection(String recordSignature, EppoCode selectedCode) {
        if (recordTable == null) {
            return;
        }

        ObservableList<EppoRecordRow> visibleRows = recordTable.getItems();
        if (visibleRows == null || visibleRows.isEmpty()) {
            recordTable.getSelectionModel().clearSelection();
            return;
        }

        EppoRecordRow matchingRecord = null;
        if (notBlank(recordSignature)) {
            for (EppoRecordRow row : visibleRows) {
                if (recordSignature.equals(buildRecordSignature(row))) {
                    matchingRecord = row;
                    break;
                }
            }
        }

        if (matchingRecord == null && selectedCode != null && notBlank(selectedCode.getCode())) {
            String selectedCodeValue = normalizeDisplayValue(selectedCode.getCode());
            for (EppoRecordRow row : visibleRows) {
                if (selectedCodeValue != null && selectedCodeValue.equalsIgnoreCase(normalizeDisplayValue(row.getEppoCode()))) {
                    matchingRecord = row;
                    break;
                }
            }
        }

        if (matchingRecord == null) {
            matchingRecord = visibleRows.get(0);
        }

        recordTable.getSelectionModel().select(matchingRecord);
        recordTable.scrollTo(matchingRecord);
    }

    private String buildRecordSignature(EppoRecordRow row) {
        if (row == null) {
            return null;
        }

        return String.join("||",
                firstNonBlank(normalizeDisplayValue(row.getEppoCode()), "—"),
                firstNonBlank(normalizeDisplayValue(row.getSpeciesLatinName()), "—"),
                firstNonBlank(normalizeDisplayValue(row.getSpeciesName()), "—"),
                firstNonBlank(normalizeDisplayValue(row.getCountry()), "—"),
                firstNonBlank(normalizeDisplayValue(row.getStatus()), "—")
        );
    }

    @FXML
    private void clearCodeFilter() {
        if (codeSearchField != null) {
            codeSearchField.clear();
        } else {
            applyCodeFilter();
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
        updateCodeSummary();
    }

    @FXML
    private void clearRecordFilter() {
        if (recordSearchField != null) {
            recordSearchField.clear();
        } else {
            applyRecordFilter();
        }
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
        updateRecordSummary();
    }

    private void rebuildRecordRows() {
        recordMasterData.clear();

        List<EppoCode> allCodes = eppoCodeService == null
                ? List.of()
                : eppoCodeService.getAll().stream()
                .filter(this::isMeaningfulCode)
                .toList();

        List<EppoRecordRow> rows = new ArrayList<>();

        for (EppoCode code : allCodes) {
            List<EppoCodeSpeciesLink> speciesLinks = eppoCodeSpeciesLinkService == null
                    ? List.of()
                    : eppoCodeSpeciesLinkService.getEffectiveSpeciesLinks(code.getId()).stream()
                    .filter(this::isMeaningfulSpeciesLink)
                    .toList();

            List<EppoZone> linkedZones = eppoCodeZoneLinkService == null
                    ? List.of()
                    : eppoCodeZoneLinkService.getZonesForCode(code.getId()).stream()
                    .filter(this::isMeaningfulZone)
                    .toList();

            String eppoCode = firstNonBlank(code.getCode(), "—");
            String eppoName = buildCodePolishName(code);
            String eppoLatinName = buildCodeLatinName(code);
            String status = firstNonBlank(normalizeDisplayValue(code.getStatus()), "—");

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
        }

        recordMasterData.setAll(rows);
    }



    private void updateCodeSummary() {
        if (codeSummaryLabel == null || filteredCodeData == null) {
            return;
        }

        long visibleCount = filteredCodeData.size();
        long activeCount = filteredCodeData.stream()
                .filter(item -> !isInactiveStatus(item.getStatus()))
                .count();
        long inactiveCount = filteredCodeData.stream()
                .filter(item -> isInactiveStatus(item.getStatus()))
                .count();

        String keyword = codeSearchField == null || codeSearchField.getText() == null
                ? ""
                : codeSearchField.getText().trim();
        String filterSuffix = UiTextUtil.buildQuotedFilterSuffix("Fraza", keyword);

        EppoCode selectedCode = codeTable == null ? null : codeTable.getSelectionModel().getSelectedItem();
        String selectedSuffix = selectedCode == null
                ? ""
                : " Wybrany kod: " + firstNonBlank(normalizeDisplayValue(selectedCode.getCode()), "—") + ".";

        codeSummaryLabel.setText(
                "Widoczne kody EPPO: " + visibleCount
                        + ". Aktywne: " + activeCount
                        + ". Nieaktywne: " + inactiveCount
                        + ". Kliknięcie w tabeli kodów nie filtruje Bazy rekordów."
                        + selectedSuffix
                        + filterSuffix
        );
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

    private void updateRecordSummary() {
        if (recordSummaryLabel == null || filteredRecordData == null) {
            return;
        }

        long codeCount = filteredRecordData.stream()
                .map(EppoRecordRow::getEppoCode)
                .filter(this::notBlank)
                .distinct()
                .count();
        long countryCount = filteredRecordData.stream()
                .map(EppoRecordRow::getCountry)
                .filter(this::notBlank)
                .filter(value -> !"—".equals(value))
                .distinct()
                .count();
        long speciesCount = filteredRecordData.stream()
                .map(EppoRecordRow::getSpeciesLatinName)
                .filter(this::notBlank)
                .filter(value -> !"—".equals(value))
                .distinct()
                .count();

        String keyword = recordSearchField == null || recordSearchField.getText() == null
                ? ""
                : recordSearchField.getText().trim();
        String filterSuffix = UiTextUtil.buildQuotedFilterSuffix("Fraza", keyword);

        recordSummaryLabel.setText(
                "Widoczne rekordy: " + filteredRecordData.size()
                        + " • Kody EPPO: " + codeCount
                        + " • Gatunki: " + speciesCount
                        + " • Kraje / strefy: " + countryCount
                        + filterSuffix
        );

        updateRecordDetail(recordTable == null ? null : recordTable.getSelectionModel().getSelectedItem());
    }

    private void updateRecordDetail(EppoRecordRow selectedRow) {
        if (recordDetailLabel == null || filteredRecordData == null) {
            return;
        }

        EppoRecordRow row = selectedRow != null ? selectedRow : filteredRecordData.stream().findFirst().orElse(null);
        if (row == null) {
            recordDetailLabel.setText("Wybierz wiersz w Bazie rekordów albo użyj filtra.");
            return;
        }

        String codeLabel = firstNonBlank(normalizeDisplayValue(row.getEppoCode()), "—");
        String statusLabel = firstNonBlank(normalizeDisplayValue(row.getStatus()), "—");
        String eppoLabel = buildDisplayPair(row.getEppoName(), row.getEppoLatinName());
        String speciesLabel = buildDisplayPair(row.getSpeciesName(), row.getSpeciesLatinName());
        String countryLabel = firstNonBlank(normalizeDisplayValue(row.getCountry()), "—");

        recordDetailLabel.setText(
                "Rekord: " + codeLabel
                        + " • " + statusLabel
                        + " • EPPO: " + eppoLabel
                        + "\nGatunek: " + speciesLabel
                        + " • Kraj / strefa: " + countryLabel
        );
    }

    private String buildDisplayPair(String primaryValue, String secondaryValue) {
        String normalizedPrimary = normalizeDisplayValue(primaryValue);
        String normalizedSecondary = normalizeDisplayValue(secondaryValue);

        if (notBlank(normalizedPrimary) && notBlank(normalizedSecondary)) {
            return normalizedPrimary + " / " + normalizedSecondary;
        }
        return firstNonBlank(normalizedPrimary, normalizedSecondary, "—");
    }

    private void updateSelectedCodeHeader(EppoCode selectedCode) {
        if (selectedCodeLabel == null) {
            return;
        }
        if (selectedCode == null) {
            selectedCodeLabel.setText("Kliknięcie na kod EPPO nie filtruje Bazy rekordów. Użyj wyszukiwarki, aby zawęzić wyniki.");
            return;
        }

        selectedCodeLabel.setText(
                "Wybrany kod EPPO: " + selectedCode.getCode()
                        + " | Kliknięcie nie filtruje Bazy rekordów — użyj wyszukiwarki, aby zawęzić wyniki."
        );
    }

    private void updateSelectedCodeSummary(EppoCode selectedCode) {
        if (selectedCodeSummaryLabel == null || selectedCodeAssignmentsLabel == null || selectedCodeLegacyLabel == null) {
            return;
        }

        if (selectedCode == null) {
            selectedCodeSummaryLabel.setText("Podsumowanie wybranego kodu EPPO pojawi się po zaznaczeniu wiersza.");
            selectedCodeAssignmentsLabel.setText("Przypisane gatunki i kraje / strefy będą pokazane tutaj.");
            selectedCodeLegacyLabel.setText("Informacja o legacy fallbacku gatunku pojawi się po zaznaczeniu kodu.");
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

        long activeZoneCount = linkedZones.stream().filter(zone -> !isInactiveStatus(zone.getStatus())).count();
        long inactiveZoneCount = linkedZones.stream().filter(zone -> isInactiveStatus(zone.getStatus())).count();

        selectedCodeSummaryLabel.setText(
                "Podsumowanie kodu EPPO: " + firstNonBlank(normalizeDisplayValue(selectedCode.getCode()), "—")
                        + ". Nazwa polska: " + buildCodePolishName(selectedCode)
                        + ". Nazwa łacińska: " + buildCodeLatinName(selectedCode)
                        + ". Status: " + firstNonBlank(normalizeDisplayValue(selectedCode.getStatus()), "—")
                        + ". Przypisane gatunki: " + speciesLinks.size()
                        + ". Przypisane kraje / strefy: " + linkedZones.size()
                        + "."
        );

        selectedCodeAssignmentsLabel.setText(
                "Podgląd przypisań: gatunki — " + buildSpeciesPreview(speciesLinks)
                        + ". Kraje / strefy — " + buildZonePreview(linkedZones)
                        + ". Aktywne kraje / strefy: " + activeZoneCount
                        + ". Nieaktywne kraje / strefy: " + inactiveZoneCount
                        + "."
        );

        selectedCodeLegacyLabel.setText(buildLegacySpeciesStatus(selectedCode));
    }

    private String buildLegacySpeciesStatus(EppoCode selectedCode) {
        if (selectedCode == null) {
            return "Informacja o legacy fallbacku gatunku pojawi się po zaznaczeniu kodu.";
        }

        List<EppoCodeSpeciesLink> explicitLinks = eppoCodeSpeciesLinkService == null
                ? List.of()
                : eppoCodeSpeciesLinkService.getByEppoCodeId(selectedCode.getId()).stream()
                .filter(this::isMeaningfulSpeciesLink)
                .toList();

        String legacySpeciesName = firstNonBlank(
                normalizeDisplayValue(selectedCode.getSpeciesName()),
                normalizeDisplayValue(selectedCode.getCommonName())
        );
        String legacyLatinName = firstNonBlank(
                normalizeDisplayValue(selectedCode.getLatinSpeciesName()),
                normalizeDisplayValue(selectedCode.getScientificName())
        );

        boolean hasLegacySpecies = notBlank(legacySpeciesName) || notBlank(legacyLatinName);
        boolean hasExplicitLinks = !explicitLinks.isEmpty();
        boolean legacyFallbackActive = hasLegacySpecies && !containsSpeciesSignature(explicitLinks, legacySpeciesName, legacyLatinName);

        if (hasExplicitLinks && !legacyFallbackActive) {
            return "Fallback legacy gatunku: nieużywany. Kod korzysta wyłącznie z właściwych przypisań gatunków.";
        }

        if (hasExplicitLinks) {
            return "Fallback legacy gatunku: aktywny równolegle z właściwymi przypisaniami. Legacy: "
                    + buildSpeciesDisplay(legacySpeciesName, legacyLatinName)
                    + ". Właściwe przypisania: " + explicitLinks.size() + ".";
        }

        if (legacyFallbackActive) {
            return "Fallback legacy gatunku: aktywny. Kod korzysta jeszcze z pola legacy: "
                    + buildSpeciesDisplay(legacySpeciesName, legacyLatinName)
                    + ".";
        }

        if (hasLegacySpecies) {
            return "Fallback legacy gatunku: obecny w danych, ale pokryty przez właściwe przypisania gatunków.";
        }

        return "Fallback legacy gatunku: brak. Kod nie ma ani legacy pola gatunku, ani właściwych przypisań gatunków.";
    }

    private boolean containsSpeciesSignature(List<EppoCodeSpeciesLink> speciesLinks, String speciesName, String latinSpeciesName) {
        if (speciesLinks == null || speciesLinks.isEmpty()) {
            return false;
        }

        String expectedSpecies = normalizeDisplayValue(speciesName);
        String expectedLatin = normalizeDisplayValue(latinSpeciesName);

        for (EppoCodeSpeciesLink link : speciesLinks) {
            if (link == null) {
                continue;
            }

            String linkSpecies = normalizeDisplayValue(link.getSpeciesName());
            String linkLatin = normalizeDisplayValue(link.getLatinSpeciesName());
            if (equalsNormalized(linkSpecies, expectedSpecies) && equalsNormalized(linkLatin, expectedLatin)) {
                return true;
            }
        }

        return false;
    }

    private boolean equalsNormalized(String first, String second) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return first.equalsIgnoreCase(second);
    }

    private String buildSpeciesDisplay(String speciesName, String latinSpeciesName) {
        String normalizedSpecies = normalizeDisplayValue(speciesName);
        String normalizedLatin = normalizeDisplayValue(latinSpeciesName);

        if (notBlank(normalizedSpecies) && notBlank(normalizedLatin)) {
            return normalizedSpecies + " / " + normalizedLatin;
        }

        return firstNonBlank(normalizedSpecies, normalizedLatin, "—");
    }

    private String buildSpeciesPreview(List<EppoCodeSpeciesLink> speciesLinks) {
        if (speciesLinks == null || speciesLinks.isEmpty()) {
            return "brak";
        }

        List<String> values = speciesLinks.stream()
                .map(link -> firstNonBlank(
                        normalizeDisplayValue(link.getLatinSpeciesName()),
                        normalizeDisplayValue(link.getSpeciesName())
                ))
                .filter(this::notBlank)
                .distinct()
                .limit(3)
                .toList();

        if (values.isEmpty()) {
            return "brak";
        }

        String preview = String.join(", ", values);
        if (speciesLinks.size() > values.size()) {
            preview += " i jeszcze " + (speciesLinks.size() - values.size());
        }
        return preview;
    }

    private String buildZonePreview(List<EppoZone> linkedZones) {
        if (linkedZones == null || linkedZones.isEmpty()) {
            return "brak";
        }

        List<String> values = linkedZones.stream()
                .map(this::buildCountryDisplay)
                .filter(this::notBlank)
                .filter(value -> !"—".equals(value))
                .distinct()
                .limit(3)
                .toList();

        if (values.isEmpty()) {
            return "brak";
        }

        String preview = String.join(", ", values);
        if (linkedZones.size() > values.size()) {
            preview += " i jeszcze " + (linkedZones.size() - values.size());
        }
        return preview;
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
