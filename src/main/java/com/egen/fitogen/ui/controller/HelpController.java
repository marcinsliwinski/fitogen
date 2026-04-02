package com.egen.fitogen.ui.controller;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.model.AppUser;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.AppUserService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.DocumentTypeService;
import com.egen.fitogen.ui.router.ViewManager;
import com.egen.fitogen.ui.util.CountryDirectory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.List;
import java.util.Optional;

public class HelpController {

    @FXML private Label startupChecklistLabel;
    @FXML private Label dailyWorkflowLabel;
    @FXML private Label documentsTipsLabel;
    @FXML private Label plantsTipsLabel;
    @FXML private Label eppoTipsLabel;
    @FXML private Label settingsTipsLabel;
    @FXML private Label systemReadinessLabel;
    @FXML private Label nextActionLabel;
    @FXML private Label supportScopeLabel;

    private final AppSettingsService appSettingsService = AppContext.getAppSettingsService();
    private final DocumentTypeService documentTypeService = AppContext.getDocumentTypeService();
    private final AppUserService appUserService = AppContext.getAppUserService();
    private final CountryDirectoryService countryDirectoryService = AppContext.getCountryDirectoryService();

    @FXML
    public void initialize() {
        loadOperationalHelp();
    }

    @FXML
    private void refreshHelp() {
        loadOperationalHelp();
    }

    @FXML
    private void openDashboard() {
        ViewManager.show(ViewManager.DASHBOARD);
    }

    @FXML
    private void openSettings() {
        ViewManager.show(ViewManager.SETTINGS);
    }

    @FXML
    private void openDocuments() {
        ViewManager.show(ViewManager.DOCUMENTS);
    }

    @FXML
    private void openPlantBatches() {
        ViewManager.show(ViewManager.BATCHES);
    }

    @FXML
    private void openPlants() {
        ViewManager.show(ViewManager.PLANTS);
    }

    @FXML
    private void openContrahents() {
        ViewManager.show(ViewManager.CONTRAHENTS);
    }

    @FXML
    private void openEppoAdmin() {
        ViewManager.show(ViewManager.EPPO_ADMIN);
    }

    @FXML
    private void openUpdates() {
        ViewManager.show(ViewManager.UPDATES);
    }

    private void loadOperationalHelp() {
        int documentTypesCount = documentTypeService.getAll().size();
        int usersCount = appUserService.getAll().size();
        Optional<Integer> defaultUserId = appUserService.getDefaultUserId();
        int countryEntriesCount = countryDirectoryService.getEntries().size();
        int customCountryEntriesCount = countryDirectoryService.getCustomEntries().size();

        startupChecklistLabel.setText(buildStartupChecklist(documentTypesCount, usersCount, defaultUserId.isPresent()));
        dailyWorkflowLabel.setText(buildDailyWorkflow());
        documentsTipsLabel.setText(buildDocumentsTips(documentTypesCount));
        plantsTipsLabel.setText(buildPlantsTips());
        eppoTipsLabel.setText(buildEppoTips(countryEntriesCount, customCountryEntriesCount));
        settingsTipsLabel.setText(buildSettingsTips(usersCount, defaultUserId));
        systemReadinessLabel.setText(buildSystemReadiness(documentTypesCount, usersCount, defaultUserId.isPresent()));
        nextActionLabel.setText(buildNextAction(documentTypesCount, usersCount, defaultUserId.isPresent()));
        supportScopeLabel.setText(buildSupportScope(defaultUserId));
    }

    private String buildStartupChecklist(int documentTypesCount, int usersCount, boolean hasDefaultUser) {
        String issuerStatus = appSettingsService.isIssuerProfileComplete() ? "kompletne" : "do uzupełnienia";
        String backupStatus = hasText(appSettingsService.getLastBackupAt()) ? "wykonany" : "jeszcze niewykonany";

        return "1. Uzupełnij dane podmiotu w Ustawieniach — obecny status: " + issuerStatus + ".\n"
                + "2. Skonfiguruj użytkowników i użytkownika domyślnego — użytkowników: " + usersCount
                + ", domyślny: " + yesNo(hasDefaultUser) + ".\n"
                + "3. Dodaj typy dokumentów i sprawdź numerator — typów dokumentów: " + documentTypesCount + ".\n"
                + "4. Wykonaj backup przed większymi zmianami — status backupu: " + backupStatus + ".\n"
                + "5. Dopiero potem uzupełniaj rośliny, kontrahentów, partie i dokumenty.";
    }

    private String buildDailyWorkflow() {
        return "Najczęstszy porządek pracy: Rośliny → Kontrahenci → Partie roślin → Dokumenty → Podgląd dokumentu.\n"
                + "Z listy dokumentów przycisk Drukuj otwiera podgląd, a dopiero z podglądu wykonuje się druk lub PDF.\n"
                + "Partie i dokumenty anulowane pozostają w systemie jako rekordy soft delete ze statusem.";
    }

    private String buildDocumentsTips(int documentTypesCount) {
        return "Dokumenty korzystają z typów dokumentów, statusów i podglądu. Obecnie skonfigurowano "
                + documentTypesCount + " typ(y) dokumentów.\n"
                + "W formularzu dokumentu najpierw wybiera się roślinę, a dopiero potem partię dostępną dla tej rośliny.\n"
                + "Anulowane partie nie powinny być używane w aktywnych dokumentach.";
    }

    private String buildPlantsTips() {
        return "Moduł Roślin obsługuje CRUD, wyszukiwarkę, status używany / nieużywany oraz ustawienia pełnej bazy roślin i globalnego wymagania paszportów.\n"
                + "Kod EPPO na roślinie ma charakter informacyjny, a główna wiedza referencyjna pozostaje w module EPPO Admin.";
    }

    private String buildEppoTips(int countryEntriesCount, int customCountryEntriesCount) {
        return "EPPO Admin jest warstwą wiedzy referencyjnej: kod EPPO może być powiązany z wieloma gatunkami i wieloma krajami / strefami.\n"
                + "Pełny wspólny słownik krajów nie jest zarządzany w EPPO, tylko w Ustawieniach → Słowniki."
                + " Aktualnie wspólny słownik zawiera " + countryEntriesCount + " wpis(y), w tym własne użytkownika: "
                + customCountryEntriesCount + ".\n"
                + "Baza rekordów EPPO powinna być zawężana wyszukiwarką, a nie kliknięciem w tabeli kodów.";
    }

    private String buildSettingsTips(int usersCount, Optional<Integer> defaultUserId) {
        String defaultUserLabel = defaultUserId
                .flatMap(id -> appUserService.getAll().stream().filter(user -> user.getId() == id).findFirst())
                .map(this::describeUser)
                .orElse("brak ustawionego użytkownika domyślnego");

        return "Ustawienia obejmują backup, typy dokumentów, użytkowników, dane wystawcy, numerator, ustawienia roślin, słowniki i Audit Log.\n"
                + "Skonfigurowani użytkownicy: " + usersCount + ", użytkownik domyślny: " + defaultUserLabel + ".\n"
                + "Zmiany administracyjne są już zapisywane w Audit Log i można je przeglądać z filtrowaniem w zakładce Audit Log.";
    }

    private String buildSystemReadiness(int documentTypesCount, int usersCount, boolean hasDefaultUser) {
        boolean issuerComplete = appSettingsService.isIssuerProfileComplete();
        boolean hasBackup = hasText(appSettingsService.getLastBackupAt());
        boolean hasDocumentTypes = documentTypesCount > 0;
        boolean hasUsers = usersCount > 0;

        int score = 0;
        if (issuerComplete) score++;
        if (hasBackup) score++;
        if (hasDocumentTypes) score++;
        if (hasUsers) score++;
        if (hasDefaultUser) score++;

        if (score >= 5) {
            return "Stan środowiska: wysoka gotowość operacyjna. Podstawowa konfiguracja systemu jest domknięta i można bezpiecznie przechodzić do codziennej pracy oraz dalszego rozwoju modułów.";
        }
        if (score >= 3) {
            return "Stan środowiska: średnia gotowość operacyjna. System nadaje się do pracy, ale warto jeszcze domknąć brakujące elementy administracyjne w Ustawieniach.";
        }
        return "Stan środowiska: niska gotowość operacyjna. Najpierw uporządkuj Ustawienia, backup i konfigurację użytkowników oraz typów dokumentów.";
    }

    private String buildNextAction(int documentTypesCount, int usersCount, boolean hasDefaultUser) {
        if (!appSettingsService.isIssuerProfileComplete()) {
            return "Najważniejszy następny krok: uzupełnij dane podmiotu w Ustawieniach, aby dokumenty i środowisko administracyjne były kompletne.";
        }
        if (!hasText(appSettingsService.getLastBackupAt())) {
            return "Najważniejszy następny krok: wykonaj backup w Ustawieniach przed większymi zmianami danych lub konfiguracji.";
        }
        if (usersCount <= 0) {
            return "Najważniejszy następny krok: dodaj co najmniej jednego użytkownika aplikacji w Ustawieniach.";
        }
        if (!hasDefaultUser) {
            return "Najważniejszy następny krok: ustaw użytkownika domyślnego, aby Audit Log i działania administracyjne były bardziej precyzyjne.";
        }
        if (documentTypesCount <= 0) {
            return "Najważniejszy następny krok: dodaj typ dokumentu w Ustawieniach, zanim użytkownicy zaczną wystawiać dokumenty operacyjnie.";
        }
        return "Najważniejszy następny krok: możesz pracować operacyjnie i przejść do dalszego rozwoju modułów Updates, importów lub walidacji paszportowej.";
    }

    private String buildSupportScope(Optional<Integer> defaultUserId) {
        String defaultUserLabel = defaultUserId
                .flatMap(id -> appUserService.getAll().stream().filter(user -> user.getId() == id).findFirst())
                .map(this::describeUser)
                .orElse("brak");

        return "Ten ekran pomaga w szybkim starcie, codziennym workflow i ocenie gotowości systemu.\n"
                + "Ostatni zapisany backup: " + valueOrDefault(appSettingsService.getLastBackupAt(), "brak") + ".\n"
                + "Użytkownik domyślny: " + defaultUserLabel + ".\n"
                + "Do bardziej szczegółowej kontroli zmian użyj: Ustawienia → Audit Log.";
    }

    private String describeUser(AppUser user) {
        if (user == null) {
            return "brak";
        }
        String displayName = user.getDisplayName();
        return hasText(displayName) ? displayName.trim() : "brak";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String yesNo(boolean value) {
        return value ? "tak" : "nie";
    }

    private String valueOrDefault(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }
}
