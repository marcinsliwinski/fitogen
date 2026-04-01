package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.AppUserService;
import com.egen.fitogen.service.BackupService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.DocumentTypeService;
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
    @FXML private Label moduleReadinessLabel;
    @FXML private Label recommendedActionLabel;

    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final BackupService backupService = AppContext.getBackupService();
    private final DocumentTypeService documentTypeService = AppContext.getDocumentTypeService();
    private final AppUserService appUserService = AppContext.getAppUserService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();

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
                "Aktualizacja bazy roślin: brak jeszcze integracji z serwerem, ale można już ocenić gotowość środowiska przed wdrożeniem importu i aktualizacji danych referencyjnych."
        );

        moduleReadinessLabel.setText(buildReadinessSummary(readinessScore));
        recommendedActionLabel.setText(buildRecommendation(hasBackup, issuerComplete, hasDocumentTypes, hasUsers, hasDefaultUser));
    }

    private String buildReadinessSummary(int readinessScore) {
        if (readinessScore >= 5) {
            return "Gotowość modułu: wysoka. Konfiguracja administracyjna jest domknięta i moduł Updates może być dalej rozwijany o realne mechanizmy aktualizacji danych i aplikacji.";
        }
        if (readinessScore >= 3) {
            return "Gotowość modułu: średnia. Podstawy są przygotowane, ale przed wdrożeniem realnych aktualizacji warto domknąć brakujące elementy operacyjne.";
        }
        return "Gotowość modułu: niska. Najpierw warto uporządkować konfigurację systemu, zanim moduł Updates zacznie wykonywać operacje na danych lub plikach aplikacji.";
    }

    private String buildRecommendation(boolean hasBackup,
                                       boolean issuerComplete,
                                       boolean hasDocumentTypes,
                                       boolean hasUsers,
                                       boolean hasDefaultUser) {
        if (!hasBackup) {
            return "Zalecane następne działanie: wykonaj backup w Ustawieniach, zanim moduł Updates dostanie operacje modyfikujące dane.";
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
        return "Zalecane następne działanie: można bezpiecznie przejść do budowy realnego mechanizmu aktualizacji danych referencyjnych i później aplikacji.";
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
