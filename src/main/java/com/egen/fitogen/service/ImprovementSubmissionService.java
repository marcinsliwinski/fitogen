package com.egen.fitogen.service;

import com.egen.fitogen.dto.DatabaseProfileInfo;
import com.egen.fitogen.dto.ImprovementSubmissionDraft;
import com.egen.fitogen.dto.ImprovementSubmissionResult;
import com.egen.fitogen.model.IssuerProfile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class ImprovementSubmissionService {

    private static final String APP_VERSION = "1.0.0";

    private final AppSettingsService appSettingsService;
    private final DatabaseProfileService databaseProfileService;
    private final AuditLogService auditLogService;
    private final ImprovementSubmissionGateway apiGateway;
    private final ImprovementSubmissionGateway manualGateway;

    public ImprovementSubmissionService(AppSettingsService appSettingsService,
                                        DatabaseProfileService databaseProfileService,
                                        AuditLogService auditLogService) {
        this(appSettingsService, databaseProfileService, auditLogService,
                new ApiImprovementSubmissionGateway(),
                new ManualImprovementSubmissionGateway());
    }

    public ImprovementSubmissionService(AppSettingsService appSettingsService,
                                        DatabaseProfileService databaseProfileService,
                                        AuditLogService auditLogService,
                                        ImprovementSubmissionGateway apiGateway,
                                        ImprovementSubmissionGateway manualGateway) {
        this.appSettingsService = appSettingsService;
        this.databaseProfileService = databaseProfileService;
        this.auditLogService = auditLogService;
        this.apiGateway = apiGateway;
        this.manualGateway = manualGateway;
    }

    public ImprovementSubmissionDraft createDraft(String type,
                                                  String title,
                                                  String description,
                                                  String expectedBenefit,
                                                  String priority,
                                                  List<Path> attachments) {
        DatabaseProfileInfo currentProfile = databaseProfileService == null ? null : databaseProfileService.getCurrentProfileInfo();
        IssuerProfile issuerProfile = appSettingsService == null ? null : appSettingsService.getIssuerProfile();

        return new ImprovementSubmissionDraft(
                type,
                title,
                description,
                expectedBenefit,
                priority,
                attachments,
                appSettingsService == null ? AppSettingsService.DEFAULT_IMPROVEMENT_FALLBACK_EMAIL : appSettingsService.getImprovementFallbackEmail(),
                APP_VERSION,
                System.getProperty("os.name", "Nieznany system"),
                System.getProperty("java.version", "Nieznana wersja Java"),
                currentProfile == null ? "Brak aktywnego profilu" : currentProfile.displayName(),
                currentProfile == null || currentProfile.databasePath() == null ? "" : currentProfile.databasePath().toString(),
                issuerProfile == null ? "" : issuerProfile.getNurseryName(),
                LocalDateTime.now()
        );
    }

    public ImprovementSubmissionResult submitViaApi(ImprovementSubmissionDraft draft) throws Exception {
        ImprovementSubmissionResult result = apiGateway.submit(draft);
        log("API", "Wysłano zgłoszenie ulepszenia przez API: " + draft.getTitle() + ".");
        return result;
    }

    public ImprovementSubmissionResult prepareManualFallback(ImprovementSubmissionDraft draft) throws Exception {
        ImprovementSubmissionResult result = manualGateway.submit(draft);
        log("MANUAL_EMAIL", "Przygotowano ręczne zgłoszenie ulepszenia: " + draft.getTitle() + ".");
        return result;
    }

    public String buildTechnicalContextSummary() {
        DatabaseProfileInfo currentProfile = databaseProfileService == null ? null : databaseProfileService.getCurrentProfileInfo();
        IssuerProfile issuerProfile = appSettingsService == null ? null : appSettingsService.getIssuerProfile();
        return "Aplikacja: " + APP_VERSION
                + " | System: " + System.getProperty("os.name", "Nieznany")
                + " | Java: " + System.getProperty("java.version", "Nieznana")
                + " | Profil bazy: " + (currentProfile == null ? "Brak" : currentProfile.displayName())
                + " | Podmiot: " + (issuerProfile == null || issuerProfile.getNurseryName() == null || issuerProfile.getNurseryName().isBlank() ? "Brak danych" : issuerProfile.getNurseryName())
                + " | Fallback e-mail: " + (appSettingsService == null ? AppSettingsService.DEFAULT_IMPROVEMENT_FALLBACK_EMAIL : appSettingsService.getImprovementFallbackEmail());
    }

    private void log(String channel, String description) {
        if (auditLogService != null) {
            auditLogService.log("IMPROVEMENT_SUBMISSION", null, channel, description);
        }
    }
}
