package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.AppUserService;
import com.egen.fitogen.service.BackupService;
import com.egen.fitogen.service.ContrahentService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.DocumentService;
import com.egen.fitogen.service.DocumentTypeService;
import com.egen.fitogen.service.EppoCodeService;
import com.egen.fitogen.service.EppoZoneService;
import com.egen.fitogen.service.PlantService;
import com.egen.fitogen.ui.router.ViewManager;
import com.egen.fitogen.ui.util.CountryDirectory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class UpdatesController {

    private static final String APP_VERSION = "1.0.0";
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private Label appVersionLabel;
    @FXML private Label systemUpdateStatusLabel;
    @FXML private Label plantDatabaseStatusLabel;
    @FXML private Label lastBackupInfoLabel;
    @FXML private Label lastBackupPathLabel;
    @FXML private Label databaseLocationLabel;
    @FXML private Label databaseHealthLabel;
    @FXML private Label issuerProfileStatusLabel;
    @FXML private Label documentTypesStatusLabel;
    @FXML private Label usersStatusLabel;
    @FXML private Label countryDirectoryStatusLabel;
    @FXML private Label plantSettingsStatusLabel;
    @FXML private Label importReadinessLabel;
    @FXML private Label importDataScopeLabel;
    @FXML private Label importRecommendationLabel;
    @FXML private Label moduleReadinessLabel;
    @FXML private Label recommendedActionLabel;

    @FXML private Label plantsScopeLabel;
    @FXML private Label eppoScopeLabel;
    @FXML private Label countriesScopeLabel;
    @FXML private Label contrahentsScopeLabel;
    @FXML private Label documentsScopeLabel;
    @FXML private Label scopeStrategySummaryLabel;
    @FXML private Label nextDeliveryRecommendationLabel;

    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final BackupService backupService = AppContext.getBackupService();
    private final DocumentTypeService documentTypeService = AppContext.getDocumentTypeService();
    private final AppUserService appUserService = AppContext.getAppUserService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();
    private final PlantService plantService = AppContext.getPlantService();
    private final ContrahentService contrahentService = AppContext.getContrahentService();
    private final EppoCodeService eppoCodeService = AppContext.getEppoCodeService();
    private final EppoZoneService eppoZoneService = AppContext.getEppoZoneService();
    private final DocumentService documentService = AppContext.getDocumentService();

    @FXML
    public void initialize() {
        refreshStatus();
    }

    @FXML
    private void refreshStatus() {
        appVersionLabel.setText(APP_VERSION);
        loadBackupStatus();
        loadDatabaseStatus();
        loadIssuerStatus();
        loadDocumentTypesStatus();
        loadUsersStatus();
        loadCountryDirectoryStatus();
        loadPlantSettingsStatus();
        loadImportPreparationStatus();
        loadScopeRoadmapStatus();
        loadUpdateReadiness();
    }

    @FXML
    private void openSettings() {
        ViewManager.show(ViewManager.SETTINGS);
    }

    @FXML
    private void openHelp() {
        ViewManager.show(ViewManager.HELP);
    }

    private void loadBackupStatus() {
        String lastBackupAt = trimmedOrNull(appSettingsService.getLastBackupAt());
        String lastBackupPath = trimmedOrNull(appSettingsService.getLastBackupPath());

        if (lastBackupAt == null) {
            lastBackupInfoLabel.setText("Brak informacji o wykonanym backupie.");
        } else {
            lastBackupInfoLabel.setText("Ostatni zapisany backup: " + lastBackupAt);
        }

        if (lastBackupPath == null) {
            lastBackupPathLabel.setText("Ścieżka ostatniego backupu: brak zapisanej ścieżki.");
            return;
        }

        Path path = Path.of(lastBackupPath);
        String existsLabel = Files.exists(path) ? "plik dostępny" : "plik nie został odnaleziony";
        lastBackupPathLabel.setText("Ścieżka ostatniego backupu: " + path.toAbsolutePath().normalize() + " (" + existsLabel + ")");
    }

    private void loadDatabaseStatus() {
        Path databasePath = backupService.getDatabaseFilePath();
        databaseLocationLabel.setText("Plik bazy danych: " + databasePath);

        if (!Files.exists(databasePath)) {
            databaseHealthLabel.setText("Stan pliku bazy: brak pliku bazy danych. Moduł aktualizacji nie powinien wykonywać żadnych operacji.");
            return;
        }

        try {
            long sizeBytes = Files.size(databasePath);
            String modifiedAt = FILE_TIME_FORMATTER.format(
                    LocalDateTime.ofInstant(Files.getLastModifiedTime(databasePath).toInstant(), java.time.ZoneId.systemDefault())
            );
            databaseHealthLabel.setText(
                    "Stan pliku bazy: plik dostępny, rozmiar " + formatSize(sizeBytes) + ", ostatnia modyfikacja: " + modifiedAt + "."
            );
        } catch (Exception e) {
            databaseHealthLabel.setText("Stan pliku bazy: plik dostępny, ale nie udało się odczytać pełnych metadanych.");
        }
    }

    private void loadIssuerStatus() {
        boolean issuerComplete = appSettingsService.isIssuerProfileComplete();
        issuerProfileStatusLabel.setText(
                issuerComplete
                        ? "Dane podmiotu są kompletne. System jest przygotowany do bezpiecznych operacji administracyjnych i dokumentowych."
                        : "Dane podmiotu są niekompletne. Przed przyszłymi aktualizacjami operacyjnymi warto uzupełnić profil wystawcy w Ustawieniach."
        );
    }

    private void loadDocumentTypesStatus() {
        int count = documentTypeService.getAll().size();
        if (count <= 0) {
            documentTypesStatusLabel.setText("Typy dokumentów: brak wpisów. Przed rozszerzeniem automatyzacji warto zdefiniować co najmniej jeden typ dokumentu.");
            return;
        }
        documentTypesStatusLabel.setText("Typy dokumentów: skonfigurowano " + count + " wpis(y). Moduł dokumentów ma już bazę referencyjną do dalszego rozwoju.");
    }

    private void loadUsersStatus() {
        List<AppUser> users = appUserService.getAll();
        Optional<Integer> defaultUserId = appUserService.getDefaultUserId();
        String defaultUserLabel = defaultUserId
                .flatMap(id -> users.stream().filter(user -> user.getId() == id).findFirst())
                .map(this::describeUser)
                .orElse("brak ustawionego użytkownika domyślnego");

        if (users.isEmpty()) {
            usersStatusLabel.setText("Użytkownicy: brak wpisów. Ustaw co najmniej jednego użytkownika przed dalszym rozwijaniem procesów administracyjnych.");
            return;
        }

        usersStatusLabel.setText(
                "Użytkownicy: skonfigurowano " + users.size() + " wpis(y), domyślny użytkownik: " + defaultUserLabel + "."
        );
    }

    private void loadCountryDirectoryStatus() {
        List<CountryDirectory.CountryEntry> allEntries = countryDirectoryService.getEntries();
        List<CountryDirectory.CountryEntry> customEntries = countryDirectoryService.getCustomEntries();

        countryDirectoryStatusLabel.setText(
                "Wspólny słownik krajów: łącznie " + allEntries.size() + " wpis(y), w tym własne użytkownika: " + customEntries.size() + "."
        );
    }

    private void loadPlantSettingsStatus() {
        String fullCatalog = appSettingsService.isPlantFullCatalogEnabled() ? "włączony" : "wyłączony";
        String passportRequired = appSettingsService.isPlantPassportRequiredForAll() ? "włączone" : "wyłączone";

        plantSettingsStatusLabel.setText(
                "Ustawienia roślin: pełna baza roślin jest " + fullCatalog
                        + ", globalne wymaganie paszportów jest " + passportRequired + "."
        );
    }

    private void loadImportPreparationStatus() {
        boolean hasBackup = trimmedOrNull(appSettingsService.getLastBackupAt()) != null;
        boolean issuerComplete = appSettingsService.isIssuerProfileComplete();
        boolean hasUsers = !appUserService.getAll().isEmpty();
        boolean hasCountryDirectory = !countryDirectoryService.getEntries().isEmpty();

        int plantsCount = plantService.getAllPlants().size();
        int contrahentsCount = contrahentService.getAllContrahents().size();
        int documentsCount = documentService.getAllDocuments().size();

        importDataScopeLabel.setText(
                "Zakres danych: rośliny (" + plantsCount + "), kontrahenci (" + contrahentsCount + "), dokumenty (" + documentsCount + "). "
                        + "Importy i eksporty CSV są już prowadzone w Ustawieniach, a moduł Aktualizacje koncentruje się na przyszłym Server Update."
        );

        int readinessScore = 0;
        if (hasBackup) {
            readinessScore++;
        }
        if (issuerComplete) {
            readinessScore++;
        }
        if (hasUsers) {
            readinessScore++;
        }
        if (hasCountryDirectory) {
            readinessScore++;
        }

        importReadinessLabel.setText(buildImportReadinessSummary(readinessScore, hasBackup, hasCountryDirectory));
        importRecommendationLabel.setText(buildImportRecommendation(hasBackup, issuerComplete, hasUsers, hasCountryDirectory, plantsCount, contrahentsCount, documentsCount));
    }

    private void loadScopeRoadmapStatus() {
        int plantCount = plantService.getAllPlants().size();
        int eppoCodeCount = eppoCodeService.getAll().size();
        int eppoZoneCount = eppoZoneService.getAll().size();
        int countryCount = countryDirectoryService.getEntries().size();
        int contrahentCount = contrahentService.getAllContrahents().size();
        int documentCount = documentService.getAllDocuments().size();

        plantsScopeLabel.setText(
                "Plants — import + aktualizacja z serwera. Aktualny stan: "
                        + plantCount
                        + " rekordów. Import i eksport CSV są już obsługiwane w Ustawieniach, a ten moduł pozostaje miejscem dla przyszłej synchronizacji z serwera."
        );

        eppoScopeLabel.setText(
                "EPPO — import + aktualizacja z serwera. Aktualny stan: "
                        + eppoCodeCount
                        + " kodów EPPO i "
                        + eppoZoneCount
                        + " krajów/stref. Docelowo aktualizacja ma obejmować kody, gatunki i kraje/strefy powiązane z kodami."
        );

        countriesScopeLabel.setText(
                "Lista krajów — import + aktualizacja z serwera. Aktualny stan: "
                        + countryCount
                        + " wpisów w wspólnym słowniku krajów. Ten zakres musi pozostać wspólnym źródłem dla Settings, EPPO i kontrahentów."
        );

        contrahentsScopeLabel.setText(
                "Kontrahenci — tylko import. Aktualny stan: "
                        + contrahentCount
                        + " rekordów. Import powinien korzystać z wspólnego słownika krajów i nie wymaga osobnego mechanizmu aktualizacji z serwera."
        );

        documentsScopeLabel.setText(
                "Dokumenty — tylko import. Aktualny stan: "
                        + documentCount
                        + " rekordów. Ten obszar wymaga najostrzejszej walidacji zależności i dlatego nie powinien być mieszany z aktualizacją referencyjną z serwera."
        );

        scopeStrategySummaryLabel.setText(
                "Strategia modułu Aktualizacje: aplikacja może być aktualizowana razem lub równolegle z synchronizacją danych serwerowych, "
                        + "ale dane należy rozdzielić na dwa typy procesów: import-only (kontrahenci, dokumenty) oraz import + server update (plants, EPPO, kraje)."
        );

        nextDeliveryRecommendationLabel.setText(
                "Najbezpieczniejszy następny krok: zostawić importy i eksporty CSV wyłącznie w Ustawieniach, "
                        + "a tutaj wejść w projekt modelu Server Update dla Plants, EPPO i wspólnego słownika krajów."
        );
    }

    private void loadUpdateReadiness() {
        boolean hasBackup = trimmedOrNull(appSettingsService.getLastBackupAt()) != null;
        boolean issuerComplete = appSettingsService.isIssuerProfileComplete();
        boolean hasDocumentTypes = !documentTypeService.getAll().isEmpty();
        boolean hasUsers = !appUserService.getAll().isEmpty();
        boolean hasDefaultUser = appUserService.getDefaultUserId().isPresent();

        int readinessScore = 0;
        if (hasBackup) {
            readinessScore++;
        }
        if (issuerComplete) {
            readinessScore++;
        }
        if (hasDocumentTypes) {
            readinessScore++;
        }
        if (hasUsers) {
            readinessScore++;
        }
        if (hasDefaultUser) {
            readinessScore++;
        }

        systemUpdateStatusLabel.setText(
                "Aktualizacja aplikacji: ekran jest przygotowany operacyjnie, ale nie ma jeszcze wdrożonego silnika sprawdzania, pobierania i instalacji nowej wersji."
        );

        plantDatabaseStatusLabel.setText(
                "Server Update: docelowo obejmie Plants, EPPO i wspólny słownik krajów. Kontrahenci oraz dokumenty pozostają wyłącznie zakresem import-only i nie powinny wracać do tego modułu jako CSV."
        );

        moduleReadinessLabel.setText(buildReadinessSummary(readinessScore));
        recommendedActionLabel.setText(buildRecommendation(hasBackup, issuerComplete, hasDocumentTypes, hasUsers, hasDefaultUser));
    }

    private String buildReadinessSummary(int readinessScore) {
        if (readinessScore >= 5) {
            return "Gotowość modułu: wysoka. Konfiguracja administracyjna jest domknięta i moduł Aktualizacje może być dalej rozwijany o realne mechanizmy aktualizacji danych i aplikacji.";
        }
        if (readinessScore >= 3) {
            return "Gotowość modułu: średnia. Podstawy są przygotowane, ale przed wdrożeniem realnych aktualizacji warto domknąć brakujące elementy operacyjne.";
        }
        return "Gotowość modułu: niska. Najpierw warto uporządkować konfigurację systemu, zanim moduł Aktualizacje zacznie wykonywać operacje na danych lub plikach aplikacji.";
    }

    private String buildRecommendation(boolean hasBackup,
                                       boolean issuerComplete,
                                       boolean hasDocumentTypes,
                                       boolean hasUsers,
                                       boolean hasDefaultUser) {
        if (!hasBackup) {
            return "Zalecane następne działanie: wykonaj backup w Ustawieniach, zanim moduł Aktualizacje dostanie operacje modyfikujące dane.";
        }
        if (!issuerComplete) {
            return "Zalecane następne działanie: uzupełnij dane podmiotu w Ustawieniach, aby środowisko było kompletne operacyjnie.";
        }
        if (!hasUsers) {
            return "Zalecane następne działanie: dodaj co najmniej jednego użytkownika aplikacji w Ustawieniach.";
        }
        if (!hasDefaultUser) {
            return "Zalecane następne działanie: ustaw użytkownika domyślnego, żeby przyszłe procesy aktualizacji i audit były bardziej precyzyjne.";
        }
        if (!hasDocumentTypes) {
            return "Zalecane następne działanie: dodaj typy dokumentów w Ustawieniach, aby przygotować system do pełniejszej pracy operacyjnej.";
        }
        return "Zalecane następne działanie: można bezpiecznie przejść do budowy realnego mechanizmu Server Update dla danych referencyjnych i później aplikacji.";
    }

    private String buildImportReadinessSummary(int readinessScore, boolean hasBackup, boolean hasCountryDirectory) {
        if (readinessScore >= 4) {
            return "Gotowość pod zakresy danych: wysoka. Środowisko ma backup, dane administracyjne i wspólny słownik krajów, więc można projektować bezpieczne procesy importu i Server Update.";
        }
        if (readinessScore >= 2) {
            return "Gotowość pod zakresy danych: średnia. Da się rozpocząć projektowanie procesów, ale przed wdrożeniem zapisu warto domknąć backup i podstawy referencyjne.";
        }
        String suffix = !hasBackup
                ? " Najpierw wykonaj backup."
                : !hasCountryDirectory
                ? " Najpierw upewnij się, że wspólny słownik krajów jest gotowy."
                : "";
        return "Gotowość pod zakresy danych: niska. Fundament operacyjny nadal wymaga porządków." + suffix;
    }

    private String buildImportRecommendation(boolean hasBackup,
                                             boolean issuerComplete,
                                             boolean hasUsers,
                                             boolean hasCountryDirectory,
                                             int plantsCount,
                                             int contrahentsCount,
                                             int documentsCount) {
        if (!hasBackup) {
            return "Rekomendacja: zanim pojawi się Server Update, wykonaj backup bazy danych.";
        }
        if (!hasCountryDirectory) {
            return "Rekomendacja: uporządkuj wspólny słownik krajów, bo będzie potrzebny zarówno przy importach w Ustawieniach, jak i przy przyszłej synchronizacji z serwera.";
        }
        if (!hasUsers) {
            return "Rekomendacja: dodaj użytkownika domyślnego, aby przyszłe operacje aktualizacyjne mogły być lepiej opisane w Audit Log.";
        }
        if (!issuerComplete) {
            return "Rekomendacja: uzupełnij dane podmiotu, żeby środowisko było kompletne operacyjnie przed szerszym wejściem w automatyzację.";
        }
        if (plantsCount == 0) {
            return "Rekomendacja: import roślin jest już prowadzony w Ustawieniach, więc tutaj warto przygotowywać kolejne reguły dla zakresów Server Update.";
        }
        if (contrahentsCount == 0) {
            return "Rekomendacja: kontrahenci pozostają zakresem import-only w Ustawieniach z walidacją krajów i kodów krajów.";
        }
        if (documentsCount == 0) {
            return "Rekomendacja: dokumenty pozostają najpóźniejszym zakresem import-only, po domknięciu walidacji zależności roślina/partia/kontrahent.";
        }
        return "Rekomendacja: środowisko ma już dane bazowe, więc można przejść do projektowania reguł Server Update równolegle z dalszym rozwojem importów i eksportów w Ustawieniach.";
    }

    private String describeUser(AppUser user) {
        if (user == null) {
            return "brak";
        }
        String displayName = user.getDisplayName();
        return displayName == null || displayName.isBlank() ? "brak" : displayName;
    }

    private String trimmedOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatSize(long sizeBytes) {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        if (sizeBytes < 1024 * 1024) {
            return (sizeBytes / 1024) + " KB";
        }
        return String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
    }
}
