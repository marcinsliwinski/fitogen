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
import com.egen.fitogen.ui.util.ComboBoxAutoComplete;
import com.egen.fitogen.ui.util.CountryDirectory;
import com.egen.fitogen.ui.util.DialogUtil;
import com.egen.fitogen.ui.util.ValidationUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EppoCodeFormController {

    @FXML private TextField codeField;
    @FXML private TextField polishNameField;
    @FXML private TextField latinNameField;
    @FXML private ComboBox<String> statusBox;

    @FXML private ComboBox<String> speciesDisplayField;
    @FXML private Button speciesConnectButton;
    @FXML private Button speciesDisconnectButton;
    @FXML private Label speciesSummaryLabel;
    @FXML private TableView<EppoCodeSpeciesLink> speciesTable;
    @FXML private TableColumn<EppoCodeSpeciesLink, String> colSpeciesDisplay;

    @FXML private ComboBox<String> zonePickerField;
    @FXML private Button zoneConnectButton;
    @FXML private Button zoneDisconnectButton;
    @FXML private Label zoneSummaryLabel;
    @FXML private TableView<EppoZone> zoneTable;
    @FXML private TableColumn<EppoZone, String> colZoneCode;
    @FXML private TableColumn<EppoZone, String> colZoneDisplay;
    @FXML private TableColumn<EppoZone, String> colZoneStatus;

    private final EppoCodeService eppoCodeService = AppContext.getEppoCodeService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();
    private final PlantService plantService = AppContext.getPlantService();
    private final EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService = AppContext.getEppoCodeSpeciesLinkService();
    private final EppoZoneService eppoZoneService = AppContext.getEppoZoneService();
    private final EppoCodeZoneLinkService eppoCodeZoneLinkService = AppContext.getEppoCodeZoneLinkService();

    private static final String ZONE_SOURCE_EPPO = "istniejący rekord EPPO";
    private static final String ZONE_SOURCE_SHARED_DIRECTORY = "wspólny słownik";

    private final Map<String, String> speciesToLatinMap = new LinkedHashMap<>();
    private final Map<String, String> latinToSpeciesMap = new LinkedHashMap<>();
    private final Map<String, EppoZone> zoneDisplayMap = new LinkedHashMap<>();
    private final Set<String> persistedZoneSignatures = new LinkedHashSet<>();
    private final ObservableList<EppoCodeSpeciesLink> assignedSpeciesData = FXCollections.observableArrayList();
    private final ObservableList<EppoZone> assignedZoneData = FXCollections.observableArrayList();

    private EppoCode eppoCode;

    @FXML
    public void initialize() {
        statusBox.getItems().addAll("ACTIVE", "INACTIVE");
        statusBox.setValue("ACTIVE");

        configureSuggestions();
        configureSpeciesTable();
        configureZoneTable();
        configureActionStateListeners();
        updateSpeciesSummary();
        updateZoneSummary();
        updateSpeciesActionState();
        updateZoneActionState();
    }

    public void setEppoCode(EppoCode eppoCode) {
        this.eppoCode = eppoCode;

        if (eppoCode == null) {
            updateSpeciesActionState();
            updateZoneActionState();
            return;
        }

        codeField.setText(nullSafe(eppoCode.getCode()));
        polishNameField.setText(nullSafe(firstNonBlank(eppoCode.getCommonName(), eppoCode.getSpeciesName())));
        latinNameField.setText(nullSafe(firstNonBlank(eppoCode.getScientificName(), eppoCode.getLatinSpeciesName())));
        statusBox.setValue(notBlank(eppoCode.getStatus()) ? eppoCode.getStatus() : "ACTIVE");

        assignedSpeciesData.setAll(eppoCodeSpeciesLinkService.getEffectiveSpeciesLinks(eppoCode.getId()));
        if (assignedSpeciesData.isEmpty()) {
            String speciesName = firstNonBlank(eppoCode.getSpeciesName(), eppoCode.getCommonName());
            String latinSpeciesName = firstNonBlank(eppoCode.getLatinSpeciesName(), eppoCode.getScientificName());
            if (notBlank(speciesName) || notBlank(latinSpeciesName)) {
                assignedSpeciesData.add(new EppoCodeSpeciesLink(0, eppoCode.getId(), speciesName, latinSpeciesName));
            }
        }

        assignedZoneData.setAll(eppoCodeZoneLinkService.getZonesForCode(eppoCode.getId()).stream()
                .filter(this::isMeaningfulZone)
                .collect(Collectors.toList()));

        updateSpeciesSummary();
        updateZoneSummary();
        updateSpeciesActionState();
        updateZoneActionState();
    }

    @FXML
    private void addSpeciesLink() {
        try {
            ParsedSpecies parsed = resolveCommittedSpecies();
            if (!notBlank(parsed.speciesName()) && !notBlank(parsed.latinSpeciesName())) {
                throw new IllegalArgumentException("Wprowadź gatunek lub nazwę łacińską.");
            }

            if (findAssignedSpeciesLink(parsed) != null) {
                throw new IllegalArgumentException("Ten gatunek jest już przypisany do kodu EPPO.");
            }

            assignedSpeciesData.add(new EppoCodeSpeciesLink(
                    0,
                    0,
                    normalizeDisplay(parsed.speciesName()),
                    normalizeDisplay(parsed.latinSpeciesName())
            ));
            speciesTable.getSelectionModel().selectLast();
            updateSpeciesSummary();
            updateSpeciesActionState();
        } catch (IllegalArgumentException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        }
    }

    @FXML
    private void removeSelectedSpeciesLink() {
        ParsedSpecies parsed = resolveCommittedSpeciesSilently();
        EppoCodeSpeciesLink toRemove = parsed == null ? null : findAssignedSpeciesLink(parsed);

        if (toRemove == null) {
            toRemove = speciesTable.getSelectionModel().getSelectedItem();
        }

        if (toRemove == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz albo wskaż przypisany gatunek do odłączenia.");
            return;
        }

        assignedSpeciesData.remove(toRemove);
        speciesTable.getSelectionModel().clearSelection();
        updateSpeciesSummary();
        updateSpeciesActionState();
    }

    @FXML
    private void addZoneLink() {
        try {
            EppoZone selectedZone = resolveCommittedZone(true);
            if (selectedZone == null) {
                throw new IllegalArgumentException("Wybierz kraj / strefę EPPO z listy podpowiedzi.");
            }

            if (findAssignedZone(selectedZone) != null) {
                throw new IllegalArgumentException("Ten kraj / strefa EPPO jest już przypisany do kodu.");
            }

            assignedZoneData.add(selectedZone);
            zoneTable.getSelectionModel().selectLast();
            updateZoneSummary();
            updateZoneActionState();
        } catch (IllegalArgumentException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        }
    }

    @FXML
    private void removeSelectedZoneLink() {
        EppoZone zoneFromField = resolveCommittedZone(false);
        EppoZone toRemove = zoneFromField == null ? null : findAssignedZone(zoneFromField);

        if (toRemove == null) {
            toRemove = zoneTable.getSelectionModel().getSelectedItem();
        }

        if (toRemove == null) {
            DialogUtil.showWarning("Brak wyboru", "Wybierz albo wskaż przypisany kraj / strefę EPPO do odłączenia.");
            return;
        }

        assignedZoneData.remove(toRemove);
        zoneTable.getSelectionModel().clearSelection();
        updateZoneSummary();
        updateZoneActionState();
    }

    @FXML
    private void save() {
        try {
            String code = ValidationUtil.requireText(codeField.getText(), "Kod EPPO");
            String polishName = ValidationUtil.optionalText(polishNameField.getText());
            String latinName = ValidationUtil.optionalText(latinNameField.getText());
            boolean passportRequired = eppoCode != null && eppoCode.isPassportRequired();
            String status = ValidationUtil.requireText(statusBox.getValue(), "Status");

            List<EppoCodeSpeciesLink> normalizedSpeciesLinks = normalizeSpeciesLinks(assignedSpeciesData);
            if (normalizedSpeciesLinks.isEmpty()) {
                throw new IllegalArgumentException("Dodaj przynajmniej jeden przypisany gatunek dla kodu EPPO.");
            }

            EppoCodeSpeciesLink primaryLink = normalizedSpeciesLinks.get(0);

            EppoCode entity = eppoCode != null
                    ? eppoCode
                    : new EppoCode(0, "", "", "", "", "", false, "ACTIVE");

            entity.setCode(code);
            entity.setSpeciesName(primaryLink.getSpeciesName());
            entity.setLatinSpeciesName(primaryLink.getLatinSpeciesName());
            entity.setCommonName(polishName);
            entity.setScientificName(latinName);
            entity.setPassportRequired(passportRequired);
            entity.setStatus(status);

            eppoCodeService.save(entity);

            EppoCode persisted = entity.getId() > 0 ? entity : eppoCodeService.getByCode(code);
            if (persisted == null || persisted.getId() <= 0) {
                throw new IllegalStateException("Nie udało się odnaleźć zapisanego kodu EPPO.");
            }

            eppoCodeSpeciesLinkService.replaceSpeciesForCode(persisted.getId(), normalizedSpeciesLinks);

            List<EppoZone> persistedZones = ensurePersistedZones(assignedZoneData);
            assignedZoneData.setAll(persistedZones);

            List<Integer> selectedZoneIds = persistedZones.stream()
                    .map(EppoZone::getId)
                    .filter(zoneId -> zoneId > 0)
                    .distinct()
                    .toList();
            eppoCodeZoneLinkService.replaceZonesForCode(persisted.getId(), selectedZoneIds);

            if (eppoCode == null) {
                DialogUtil.showSuccess("Kod EPPO został dodany razem z przypisanymi gatunkami i krajami.");
            } else {
                DialogUtil.showSuccess("Kod EPPO został zaktualizowany razem z przypisanymi gatunkami i krajami.");
            }

            close();
        } catch (IllegalArgumentException e) {
            DialogUtil.showWarning("Błędne dane", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Błąd zapisu", "Nie udało się zapisać kodu EPPO.");
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private void configureSuggestions() {
        speciesDisplayField.setEditable(true);
        zonePickerField.setEditable(true);

        List<String> speciesSuggestions = new ArrayList<>();
        Set<String> uniqueSpeciesDisplays = new LinkedHashSet<>();

        for (Plant plant : plantService.getAllPlants()) {
            String species = normalizeDisplay(plant.getSpecies());
            String latin = normalizeDisplay(plant.getLatinSpeciesName());
            if (!notBlank(species) && !notBlank(latin)) {
                continue;
            }

            if (notBlank(species) && notBlank(latin)) {
                speciesToLatinMap.putIfAbsent(normalizeKey(species), latin);
                latinToSpeciesMap.putIfAbsent(normalizeKey(latin), species);
            }

            String display = buildSpeciesDisplay(species, latin);
            if (notBlank(display) && uniqueSpeciesDisplays.add(display)) {
                speciesSuggestions.add(display);
            }
        }
        ComboBoxAutoComplete.bindEditable(speciesDisplayField, speciesSuggestions);

        zoneDisplayMap.clear();
        persistedZoneSignatures.clear();
        List<String> zoneSuggestions = new ArrayList<>();
        Set<String> uniqueZoneDisplays = new LinkedHashSet<>();
        List<EppoZone> availableZones = eppoZoneService.getAll();

        for (EppoZone zone : availableZones) {
            if (!isMeaningfulZone(zone)) {
                continue;
            }
            persistedZoneSignatures.add(buildZoneSignature(zone));
            addZoneSuggestion(zone, ZONE_SOURCE_EPPO, zoneSuggestions, uniqueZoneDisplays);
        }

        for (CountryDirectory.CountryEntry entry : countryDirectoryService.getEntries()) {
            EppoZone zone = findExistingZoneForCountryEntry(entry, availableZones);
            String sourceLabel = ZONE_SOURCE_EPPO;
            if (zone == null) {
                zone = buildSharedCountryZone(entry);
                sourceLabel = ZONE_SOURCE_SHARED_DIRECTORY;
            }
            addZoneSuggestion(zone, sourceLabel, zoneSuggestions, uniqueZoneDisplays);
        }

        ComboBoxAutoComplete.bindEditable(zonePickerField, zoneSuggestions);
    }

    private void configureSpeciesTable() {
        colSpeciesDisplay.setCellValueFactory(cell -> new SimpleStringProperty(
                buildSpeciesDisplay(cell.getValue().getSpeciesName(), cell.getValue().getLatinSpeciesName())
        ));
        speciesTable.setItems(assignedSpeciesData);
        speciesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        speciesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void configureZoneTable() {
        colZoneCode.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getCode())));
        colZoneDisplay.setCellValueFactory(cell -> new SimpleStringProperty(buildZoneDisplayWithSource(cell.getValue())));
        colZoneStatus.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getStatus())));
        zoneTable.setItems(assignedZoneData);
        zoneTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        zoneTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void configureActionStateListeners() {
        if (speciesDisplayField.getEditor() != null) {
            speciesDisplayField.getEditor().textProperty().addListener((obs, oldValue, newValue) -> updateSpeciesActionState());
        }
        speciesDisplayField.valueProperty().addListener((obs, oldValue, newValue) -> updateSpeciesActionState());
        speciesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateSpeciesActionState());

        if (zonePickerField.getEditor() != null) {
            zonePickerField.getEditor().textProperty().addListener((obs, oldValue, newValue) -> updateZoneActionState());
        }
        zonePickerField.valueProperty().addListener((obs, oldValue, newValue) -> updateZoneActionState());
        zoneTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateZoneActionState());
    }

    private void updateSpeciesSummary() {
        if (speciesSummaryLabel != null) {
            speciesSummaryLabel.setText("Przypisane gatunki: " + assignedSpeciesData.size());
        }
    }

    private void updateZoneSummary() {
        if (zoneSummaryLabel != null) {
            zoneSummaryLabel.setText("Przypisane kraje / strefy EPPO: " + assignedZoneData.size());
        }
    }

    private void updateSpeciesActionState() {
        ParsedSpecies parsed = resolveCommittedSpeciesSilently();
        EppoCodeSpeciesLink matched = parsed == null ? null : findAssignedSpeciesLink(parsed);

        if (matched != null) {
            speciesTable.getSelectionModel().select(matched);
            speciesConnectButton.setDisable(true);
            speciesDisconnectButton.setDisable(false);
            return;
        }

        speciesTable.getSelectionModel().clearSelection();
        boolean hasInput = parsed != null && (notBlank(parsed.speciesName()) || notBlank(parsed.latinSpeciesName()));
        speciesConnectButton.setDisable(!hasInput);
        speciesDisconnectButton.setDisable(true);
    }

    private void updateZoneActionState() {
        EppoZone selectedZone = resolveCommittedZone(false);
        EppoZone matched = selectedZone == null ? null : findAssignedZone(selectedZone);

        if (matched != null) {
            zoneTable.getSelectionModel().select(matched);
            zoneConnectButton.setDisable(true);
            zoneDisconnectButton.setDisable(false);
            return;
        }

        zoneTable.getSelectionModel().clearSelection();
        boolean hasValidSelection = selectedZone != null;
        zoneConnectButton.setDisable(!hasValidSelection);
        zoneDisconnectButton.setDisable(true);
    }

    private ParsedSpecies resolveCommittedSpecies() {
        String speciesDisplay = ValidationUtil.optionalText(ComboBoxAutoComplete.getCommittedValue(speciesDisplayField));
        return parseSpeciesDisplay(speciesDisplay);
    }

    private ParsedSpecies resolveCommittedSpeciesSilently() {
        String speciesDisplay = ComboBoxAutoComplete.getCommittedValue(speciesDisplayField);
        return parseSpeciesDisplay(speciesDisplay);
    }

    private EppoCodeSpeciesLink findAssignedSpeciesLink(ParsedSpecies parsed) {
        if (parsed == null) {
            return null;
        }

        String normalizedSpecies = normalizeDisplay(parsed.speciesName());
        String normalizedLatin = normalizeDisplay(parsed.latinSpeciesName());

        for (EppoCodeSpeciesLink existing : assignedSpeciesData) {
            if (sameNormalized(existing.getSpeciesName(), normalizedSpecies)
                    && sameNormalized(existing.getLatinSpeciesName(), normalizedLatin)) {
                return existing;
            }
        }
        return null;
    }

    private EppoZone resolveCommittedZone(boolean requireValidValue) {
        String zoneDisplay = ComboBoxAutoComplete.getCommittedValue(zonePickerField);
        String normalized = normalizeKey(zoneDisplay);

        if (!notBlank(normalized)) {
            return null;
        }

        EppoZone zone = zoneDisplayMap.get(normalized);
        if (zone == null && requireValidValue) {
            throw new IllegalArgumentException("Wybierz kraj / strefę EPPO z listy podpowiedzi.");
        }
        return zone;
    }

    private EppoZone findAssignedZone(EppoZone zone) {
        if (zone == null) {
            return null;
        }

        String targetSignature = buildZoneSignature(zone);
        for (EppoZone existing : assignedZoneData) {
            if (existing.getId() > 0 && zone.getId() > 0 && existing.getId() == zone.getId()) {
                return existing;
            }
            if (sameNormalized(buildZoneSignature(existing), targetSignature)) {
                return existing;
            }
        }
        return null;
    }

    private List<EppoCodeSpeciesLink> normalizeSpeciesLinks(List<EppoCodeSpeciesLink> rawLinks) {
        List<EppoCodeSpeciesLink> result = new ArrayList<>();
        Set<String> signatures = new LinkedHashSet<>();

        for (EppoCodeSpeciesLink rawLink : rawLinks) {
            if (rawLink == null) {
                continue;
            }

            String speciesName = normalizeDisplay(rawLink.getSpeciesName());
            String latinSpeciesName = normalizeDisplay(rawLink.getLatinSpeciesName());
            if (!notBlank(speciesName) && !notBlank(latinSpeciesName)) {
                continue;
            }

            String signature = normalizeKey(speciesName) + "||" + normalizeKey(latinSpeciesName);
            if (!signatures.add(signature)) {
                continue;
            }

            result.add(new EppoCodeSpeciesLink(0, 0, speciesName, latinSpeciesName));
        }

        return result;
    }

    private ParsedSpecies parseSpeciesDisplay(String value) {
        String normalized = normalizeDisplay(value);
        if (!notBlank(normalized)) {
            return new ParsedSpecies(null, null);
        }

        if (normalized.contains(" / ")) {
            String[] parts = normalized.split("\\s/\\s", 2);
            String species = normalizeDisplay(parts.length > 0 ? parts[0] : null);
            String latin = normalizeDisplay(parts.length > 1 ? parts[1] : null);
            return new ParsedSpecies(species, latin);
        }

        String key = normalizeKey(normalized);
        String latin = speciesToLatinMap.get(key);
        if (notBlank(latin)) {
            return new ParsedSpecies(normalized, latin);
        }

        String species = latinToSpeciesMap.get(key);
        if (notBlank(species)) {
            return new ParsedSpecies(species, normalized);
        }

        return new ParsedSpecies(normalized, null);
    }

    private String buildSpeciesDisplay(String speciesName, String latinSpeciesName) {
        String species = normalizeDisplay(speciesName);
        String latin = normalizeDisplay(latinSpeciesName);
        if (notBlank(species) && notBlank(latin)) {
            return species + " / " + latin;
        }
        return notBlank(species) ? species : latin;
    }

    private void addZoneSuggestion(EppoZone zone, String sourceLabel, List<String> zoneSuggestions, Set<String> uniqueZoneDisplays) {
        if (!isMeaningfulZone(zone)) {
            return;
        }

        String display = buildZoneDisplay(zone);
        String displayWithSource = buildZoneDisplayWithSource(zone, sourceLabel);
        if (notBlank(displayWithSource) && uniqueZoneDisplays.add(displayWithSource)) {
            zoneSuggestions.add(displayWithSource);
            zoneDisplayMap.put(normalizeKey(displayWithSource), zone);
            if (notBlank(display)) {
                zoneDisplayMap.putIfAbsent(normalizeKey(display), zone);
            }
        }
    }

    private EppoZone findExistingZoneForCountryEntry(CountryDirectory.CountryEntry entry, List<EppoZone> availableZones) {
        if (entry == null || availableZones == null) {
            return null;
        }

        String entryCountry = normalizeKey(entry.country());
        String entryCode = normalizeKey(entry.countryCode());
        for (EppoZone zone : availableZones) {
            if (zone == null) {
                continue;
            }

            String zoneCode = normalizeKey(zone.getCode());
            String zoneCountryCode = normalizeKey(zone.getCountryCode());
            String zoneName = normalizeKey(zone.getName());

            if (entryCode != null && (entryCode.equals(zoneCountryCode) || entryCode.equals(zoneCode))) {
                return zone;
            }
            if (entryCountry != null && entryCountry.equals(zoneName)) {
                return zone;
            }
        }
        return null;
    }

    private EppoZone buildSharedCountryZone(CountryDirectory.CountryEntry entry) {
        if (entry == null) {
            return null;
        }
        String code = normalizeDisplay(firstNonBlank(entry.countryCode(), entry.countryCode()));
        String name = normalizeDisplay(entry.country());
        String countryCode = normalizeDisplay(entry.countryCode());
        return new EppoZone(0, code, name, countryCode, "ACTIVE");
    }

    private List<EppoZone> ensurePersistedZones(List<EppoZone> zones) {
        List<EppoZone> persistedZones = new ArrayList<>();
        Set<String> signatures = new LinkedHashSet<>();

        for (EppoZone zone : zones) {
            EppoZone persisted = ensurePersistedZone(zone);
            if (persisted == null || persisted.getId() <= 0) {
                continue;
            }

            String signature = buildZoneSignature(persisted);
            if (signatures.add(signature)) {
                persistedZones.add(persisted);
            }
        }

        return persistedZones;
    }

    private EppoZone ensurePersistedZone(EppoZone zone) {
        if (!isMeaningfulZone(zone)) {
            return null;
        }

        if (zone.getId() > 0) {
            return zone;
        }

        String code = normalizeDisplay(firstNonBlank(zone.getCode(), zone.getCountryCode()));
        if (!notBlank(code)) {
            throw new IllegalArgumentException("Wybrany kraj / strefa EPPO nie ma poprawnego kodu.");
        }

        EppoZone existingByCode = eppoZoneService.getByCode(code);
        if (existingByCode != null) {
            return existingByCode;
        }

        EppoZone newZone = new EppoZone(
                0,
                code,
                normalizeDisplay(zone.getName()),
                normalizeDisplay(firstNonBlank(zone.getCountryCode(), code)),
                normalizeDisplay(firstNonBlank(zone.getStatus(), "ACTIVE"))
        );
        eppoZoneService.save(newZone);

        EppoZone persisted = eppoZoneService.getByCode(code);
        if (persisted == null || persisted.getId() <= 0) {
            throw new IllegalStateException("Nie udało się zapisać kraju / strefy EPPO: " + code + ".");
        }
        return persisted;
    }

    private String buildZoneSignature(EppoZone zone) {
        if (zone == null) {
            return null;
        }
        return normalizeKey(firstNonBlank(zone.getCode(), zone.getCountryCode()))
                + "||" + normalizeKey(zone.getName())
                + "||" + normalizeKey(zone.getCountryCode());
    }

    private String buildZoneDisplay(EppoZone zone) {
        if (zone == null) {
            return "";
        }
        String prefix = firstNonBlank(zone.getCountryCode(), zone.getCode());
        String name = normalizeDisplay(zone.getName());
        if (notBlank(prefix) && notBlank(name)) {
            return prefix + " / " + name;
        }
        return notBlank(prefix) ? prefix : nullSafe(name);
    }

    private String buildZoneDisplayWithSource(EppoZone zone) {
        return buildZoneDisplayWithSource(zone, determineZoneSourceLabel(zone));
    }

    private String buildZoneDisplayWithSource(EppoZone zone, String sourceLabel) {
        String display = buildZoneDisplay(zone);
        if (!notBlank(display)) {
            return "";
        }
        if (!notBlank(sourceLabel)) {
            return display;
        }
        return display + " [" + sourceLabel + "]";
    }

    private String determineZoneSourceLabel(EppoZone zone) {
        if (!isMeaningfulZone(zone)) {
            return null;
        }
        if (zone.getId() > 0) {
            return ZONE_SOURCE_EPPO;
        }
        String signature = buildZoneSignature(zone);
        return signature != null && persistedZoneSignatures.contains(signature)
                ? ZONE_SOURCE_EPPO
                : ZONE_SOURCE_SHARED_DIRECTORY;
    }

    private boolean sameNormalized(String left, String right) {
        String leftNormalized = normalizeKey(left);
        String rightNormalized = normalizeKey(right);
        if (leftNormalized == null && rightNormalized == null) {
            return true;
        }
        return leftNormalized != null && leftNormalized.equals(rightNormalized);
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized.toLowerCase();
    }

    private String normalizeDisplay(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isBlank();
    }

    private boolean isMeaningfulZone(EppoZone zone) {
        return zone != null && (notBlank(zone.getCode()) || notBlank(zone.getName()) || notBlank(zone.getCountryCode()));
    }

    private String firstNonBlank(String first, String second) {
        return notBlank(first) ? first : second;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private void close() {
        Stage stage = (Stage) codeField.getScene().getWindow();
        stage.close();
    }

    private record ParsedSpecies(String speciesName, String latinSpeciesName) {}
}