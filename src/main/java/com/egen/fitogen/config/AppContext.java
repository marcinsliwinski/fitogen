package com.egen.fitogen.config;

import com.egen.fitogen.database.SqliteAppSettingsRepository;
import com.egen.fitogen.database.SqliteAppUserRepository;
import com.egen.fitogen.database.SqliteAuditLogRepository;
import com.egen.fitogen.database.SqliteContrahentRepository;
import com.egen.fitogen.database.SqliteDocumentItemRepository;
import com.egen.fitogen.database.SqliteDocumentRepository;
import com.egen.fitogen.database.SqliteDocumentTypeRepository;
import com.egen.fitogen.database.SqliteEppoCodePlantLinkRepository;
import com.egen.fitogen.database.SqliteEppoCodeRepository;
import com.egen.fitogen.database.SqliteEppoCodeSpeciesLinkRepository;
import com.egen.fitogen.database.SqliteEppoCodeZoneLinkRepository;
import com.egen.fitogen.database.SqliteEppoZoneRepository;
import com.egen.fitogen.database.SqliteNumberingConfigRepository;
import com.egen.fitogen.database.SqlitePlantBatchRepository;
import com.egen.fitogen.database.SqlitePlantRepository;
import com.egen.fitogen.repository.AppSettingsRepository;
import com.egen.fitogen.repository.AppUserRepository;
import com.egen.fitogen.repository.AuditLogRepository;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.repository.DocumentItemRepository;
import com.egen.fitogen.repository.DocumentRepository;
import com.egen.fitogen.repository.DocumentTypeRepository;
import com.egen.fitogen.repository.EppoCodePlantLinkRepository;
import com.egen.fitogen.repository.EppoCodeRepository;
import com.egen.fitogen.repository.EppoCodeSpeciesLinkRepository;
import com.egen.fitogen.repository.EppoCodeZoneLinkRepository;
import com.egen.fitogen.repository.EppoZoneRepository;
import com.egen.fitogen.repository.NumberingConfigRepository;
import com.egen.fitogen.repository.PlantBatchRepository;
import com.egen.fitogen.repository.PlantRepository;
import com.egen.fitogen.service.AppSettingsService;
import com.egen.fitogen.service.AppUserService;
import com.egen.fitogen.service.AuditLogService;
import com.egen.fitogen.service.BackupService;
import com.egen.fitogen.service.ContrahentService;
import com.egen.fitogen.service.CountryDirectoryService;
import com.egen.fitogen.service.DocumentService;
import com.egen.fitogen.service.DocumentTypeService;
import com.egen.fitogen.service.EppoAdvisoryService;
import com.egen.fitogen.service.EppoCodePlantLinkService;
import com.egen.fitogen.service.EppoCodeService;
import com.egen.fitogen.service.EppoCodeSpeciesLinkService;
import com.egen.fitogen.service.EppoCodeZoneLinkService;
import com.egen.fitogen.service.EppoZoneService;
import com.egen.fitogen.service.NumberingConfigService;
import com.egen.fitogen.service.NumberingService;
import com.egen.fitogen.service.PlantBatchService;
import com.egen.fitogen.service.PlantService;
import com.egen.fitogen.service.PassportAdvisoryService;

public class AppContext {

    private static PlantRepository plantRepository;
    private static PlantBatchRepository plantBatchRepository;
    private static ContrahentRepository contrahentRepository;
    private static DocumentRepository documentRepository;
    private static DocumentItemRepository documentItemRepository;
    private static NumberingConfigRepository numberingConfigRepository;
    private static DocumentTypeRepository documentTypeRepository;
    private static AppSettingsRepository appSettingsRepository;
    private static AppUserRepository appUserRepository;
    private static AuditLogRepository auditLogRepository;
    private static EppoCodeRepository eppoCodeRepository;
    private static EppoCodePlantLinkRepository eppoCodePlantLinkRepository;
    private static EppoCodeSpeciesLinkRepository eppoCodeSpeciesLinkRepository;
    private static EppoZoneRepository eppoZoneRepository;
    private static EppoCodeZoneLinkRepository eppoCodeZoneLinkRepository;

    private static PlantService plantService;
    private static PlantBatchService plantBatchService;
    private static ContrahentService contrahentService;
    private static DocumentService documentService;
    private static NumberingService numberingService;
    private static NumberingConfigService numberingConfigService;
    private static BackupService backupService;
    private static DocumentTypeService documentTypeService;
    private static AppSettingsService appSettingsService;
    private static AppUserService appUserService;
    private static AuditLogService auditLogService;
    private static CountryDirectoryService countryDirectoryService;
    private static EppoCodeService eppoCodeService;
    private static EppoCodePlantLinkService eppoCodePlantLinkService;
    private static EppoCodeSpeciesLinkService eppoCodeSpeciesLinkService;
    private static EppoZoneService eppoZoneService;
    private static EppoCodeZoneLinkService eppoCodeZoneLinkService;
    private static EppoAdvisoryService eppoAdvisoryService;
    private static PassportAdvisoryService passportAdvisoryService;

    public static void init() {
        plantRepository = new SqlitePlantRepository();
        plantBatchRepository = new SqlitePlantBatchRepository();
        contrahentRepository = new SqliteContrahentRepository();
        documentRepository = new SqliteDocumentRepository();
        documentItemRepository = new SqliteDocumentItemRepository();
        numberingConfigRepository = new SqliteNumberingConfigRepository();
        documentTypeRepository = new SqliteDocumentTypeRepository();
        appSettingsRepository = new SqliteAppSettingsRepository();
        appUserRepository = new SqliteAppUserRepository();
        auditLogRepository = new SqliteAuditLogRepository();
        eppoCodeRepository = new SqliteEppoCodeRepository();
        eppoCodePlantLinkRepository = new SqliteEppoCodePlantLinkRepository();
        eppoCodeSpeciesLinkRepository = new SqliteEppoCodeSpeciesLinkRepository();
        eppoZoneRepository = new SqliteEppoZoneRepository();
        eppoCodeZoneLinkRepository = new SqliteEppoCodeZoneLinkRepository();

        numberingService = new NumberingService(numberingConfigRepository);
        numberingConfigService = new NumberingConfigService(numberingConfigRepository, numberingService);
        backupService = new BackupService();
        documentTypeService = new DocumentTypeService(documentTypeRepository);
        appSettingsService = new AppSettingsService(appSettingsRepository);
        appUserService = new AppUserService(appUserRepository);
        auditLogService = new AuditLogService(auditLogRepository, appUserService);
        eppoCodeService = new EppoCodeService(eppoCodeRepository);
        eppoCodeSpeciesLinkService = new EppoCodeSpeciesLinkService(
                eppoCodeSpeciesLinkRepository,
                eppoCodeRepository
        );
        eppoCodePlantLinkService = new EppoCodePlantLinkService(
                eppoCodePlantLinkRepository,
                eppoCodeRepository,
                plantRepository,
                eppoCodeSpeciesLinkService
        );
        eppoZoneService = new EppoZoneService(eppoZoneRepository);
        eppoCodeZoneLinkService = new EppoCodeZoneLinkService(
                eppoCodeZoneLinkRepository,
                eppoCodeRepository,
                eppoZoneRepository
        );
        eppoAdvisoryService = new EppoAdvisoryService(eppoCodePlantLinkService, eppoCodeZoneLinkService);

        plantService = new PlantService(plantRepository, appSettingsService, auditLogService);
        plantBatchService = new PlantBatchService(
                plantBatchRepository,
                numberingService,
                documentItemRepository,
                auditLogService
        );
        contrahentService = new ContrahentService(contrahentRepository, auditLogService);
        countryDirectoryService = new CountryDirectoryService(contrahentService, appSettingsService);
        documentService = new DocumentService(
                documentRepository,
                documentItemRepository,
                numberingService,
                plantBatchService
        );
        passportAdvisoryService = new PassportAdvisoryService(eppoCodePlantLinkService);
    }

    public static PlantService getPlantService() {
        return plantService;
    }

    public static PlantBatchService getPlantBatchService() {
        return plantBatchService;
    }

    public static ContrahentService getContrahentService() {
        return contrahentService;
    }

    public static DocumentService getDocumentService() {
        return documentService;
    }

    public static NumberingService getNumberingService() {
        return numberingService;
    }

    public static NumberingConfigService getNumberingConfigService() {
        return numberingConfigService;
    }

    public static NumberingConfigRepository getNumberingConfigRepository() {
        return numberingConfigRepository;
    }

    public static BackupService getBackupService() {
        return backupService;
    }

    public static DocumentTypeService getDocumentTypeService() {
        return documentTypeService;
    }

    public static AppSettingsService getAppSettingsService() {
        return appSettingsService;
    }

    public static AppUserService getAppUserService() {
        return appUserService;
    }

    public static CountryDirectoryService getCountryDirectoryService() {
        return countryDirectoryService;
    }

    public static EppoCodePlantLinkService getEppoCodePlantLinkService() {
        return eppoCodePlantLinkService;
    }

    public static EppoCodeSpeciesLinkService getEppoCodeSpeciesLinkService() {
        return eppoCodeSpeciesLinkService;
    }

    public static EppoCodeService getEppoCodeService() {
        return eppoCodeService;
    }

    public static EppoZoneService getEppoZoneService() {
        return eppoZoneService;
    }

    public static EppoCodeZoneLinkService getEppoCodeZoneLinkService() {
        return eppoCodeZoneLinkService;
    }

    public static EppoAdvisoryService getEppoAdvisoryService() {
        return eppoAdvisoryService;
    }

    public static PassportAdvisoryService getPassportAdvisoryService() {
        return passportAdvisoryService;
    }

    public static AuditLogService getAuditLogService() {
        return auditLogService;
    }
}
