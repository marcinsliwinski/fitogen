package com.egen.fitogen.service;

import com.egen.fitogen.domain.NumberingConfig;
import com.egen.fitogen.domain.NumberingSectionType;
import com.egen.fitogen.domain.NumberingType;
import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.repository.DocumentRepository;
import com.egen.fitogen.repository.NumberingConfigRepository;
import com.egen.fitogen.repository.PlantBatchRepository;

public class NumberingConfigService {

    private final NumberingConfigRepository repository;
    private final NumberingService numberingService;
    private final AuditLogService auditLogService;
    private final DocumentRepository documentRepository;
    private final PlantBatchRepository plantBatchRepository;

    public NumberingConfigService(
            NumberingConfigRepository repository,
            NumberingService numberingService,
            AuditLogService auditLogService,
            DocumentRepository documentRepository,
            PlantBatchRepository plantBatchRepository) {

        this.repository = repository;
        this.numberingService = numberingService;
        this.auditLogService = auditLogService;
        this.documentRepository = documentRepository;
        this.plantBatchRepository = plantBatchRepository;
    }

    public NumberingConfig getConfigOrDefault(NumberingType type) {
        NumberingConfig config = repository.findByType(type);

        if (config == null) {
            NumberingConfig defaultConfig = buildDefaultConfig(type);
            repository.save(defaultConfig);
            return defaultConfig;
        }

        if (isEffectivelyEmpty(config)) {
            NumberingConfig defaultConfig = buildDefaultConfig(type);
            defaultConfig.setId(config.getId());
            defaultConfig.setCurrentCounter(config.getCurrentCounter());
            return defaultConfig;
        }

        return config;
    }

    public NumberingConfig buildDefaultConfig(NumberingType type) {
        NumberingConfig config = new NumberingConfig();
        config.setType(type);
        config.setCurrentCounter(0);

        if (type == NumberingType.DOCUMENT) {
            config.setSection1Type(NumberingSectionType.AUTO_INCREMENT);
            config.setSection1Separator("/");

            config.setSection2Type(NumberingSectionType.YEAR);
            config.setSection2Separator("-");

            config.setSection3Type(NumberingSectionType.STATIC_TEXT);
            config.setSection3StaticValue("FG");
            config.setSection3Separator("");
        } else if (type == NumberingType.BATCH) {
            config.setSection1Type(NumberingSectionType.YEAR);
            config.setSection1Separator("/");

            config.setSection2Type(NumberingSectionType.MONTH);
            config.setSection2Separator("/");

            config.setSection3Type(NumberingSectionType.AUTO_INCREMENT);
            config.setSection3Separator("");
        }

        return config;
    }

    public void saveConfig(NumberingConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Konfiguracja nie może być pusta.");
        }

        if (config.getType() == null) {
            throw new IllegalArgumentException("Typ numeracji jest wymagany.");
        }

        if (config.getCurrentCounter() < 0) {
            throw new IllegalArgumentException("Licznik nie może być ujemny.");
        }

        NumberingConfig existing = repository.findByType(config.getType());

        if (existing == null) {
            repository.save(config);
            logAudit(config, "CREATE", "Utworzono konfigurację numeracji: " + summarize(config));
            return;
        }

        NumberingConfig previous = copyOf(existing);
        config.setId(existing.getId());
        repository.update(config);
        logAudit(config, "UPDATE",
                "Zaktualizowano konfigurację numeracji: " + summarize(config)
                        + ". Wcześniej: " + summarize(previous));
    }

    public String preview(NumberingConfig config) {
        PlantBatch sampleBatch = new PlantBatch();
        sampleBatch.setInteriorBatchNo("INT-001");
        sampleBatch.setExteriorBatchNo("EXT-001");

        return numberingService.previewNumber(config, sampleBatch);
    }


    public String synchronizeCounterWithDatabase(NumberingType type) {
        NumberingConfig config = getConfigOrDefault(type);
        int previousCounter = config.getCurrentCounter();
        int synchronizedCounter = type == NumberingType.DOCUMENT
                ? resolveHighestDocumentCounter(config)
                : resolveHighestBatchCounter(config);

        config.setCurrentCounter(Math.max(0, synchronizedCounter));
        repository.update(config);

        String description = "Zsynchronizowano licznik " + type.name()
                + " z bazą: " + previousCounter + " -> " + config.getCurrentCounter();
        logAudit(config, "SYNC", description);

        return description;
    }

    private int resolveHighestDocumentCounter(NumberingConfig config) {
        if (documentRepository == null) {
            return config.getCurrentCounter();
        }

        int highest = 0;
        for (Document document : documentRepository.findAll()) {
            if (document == null) {
                continue;
            }

            int counter = numberingService.resolveCounterForNumber(
                    config,
                    document.getDocumentNumber(),
                    null,
                    document.getIssueDate()
            );
            if (counter > highest) {
                highest = counter;
            }
        }

        return highest;
    }

    private int resolveHighestBatchCounter(NumberingConfig config) {
        if (plantBatchRepository == null) {
            return config.getCurrentCounter();
        }

        int highest = 0;
        for (PlantBatch batch : plantBatchRepository.findAll()) {
            if (batch == null || batch.getInteriorBatchNo() == null || batch.getInteriorBatchNo().isBlank()) {
                continue;
            }

            int counter = numberingService.resolveCounterForNumber(
                    config,
                    batch.getInteriorBatchNo(),
                    batch,
                    batch.getCreationDate()
            );
            if (counter > highest) {
                highest = counter;
            }
        }

        return highest;
    }

    private boolean isEffectivelyEmpty(NumberingConfig config) {
        return config.getSection1Type() == null
                && config.getSection2Type() == null
                && config.getSection3Type() == null;
    }

    private NumberingConfig copyOf(NumberingConfig source) {
        if (source == null) {
            return null;
        }

        NumberingConfig copy = new NumberingConfig();
        copy.setId(source.getId());
        copy.setType(source.getType());
        copy.setSection1Type(source.getSection1Type());
        copy.setSection1StaticValue(source.getSection1StaticValue());
        copy.setSection1Separator(source.getSection1Separator());
        copy.setSection2Type(source.getSection2Type());
        copy.setSection2StaticValue(source.getSection2StaticValue());
        copy.setSection2Separator(source.getSection2Separator());
        copy.setSection3Type(source.getSection3Type());
        copy.setSection3StaticValue(source.getSection3StaticValue());
        copy.setSection3Separator(source.getSection3Separator());
        copy.setCurrentCounter(source.getCurrentCounter());
        return copy;
    }

    private void logAudit(NumberingConfig config, String actionType, String description) {
        if (auditLogService != null) {
            Integer entityId = config == null || config.getId() <= 0 ? null : config.getId();
            auditLogService.log("NUMBERING_CONFIG", entityId, actionType, description);
        }
    }

    private String summarize(NumberingConfig config) {
        if (config == null) {
            return "[brak danych]";
        }

        return "typ=" + config.getType()
                + ", sekcja1=" + config.getSection1Type()
                + ", sekcja2=" + config.getSection2Type()
                + ", sekcja3=" + config.getSection3Type()
                + ", licznik=" + config.getCurrentCounter();
    }

}
