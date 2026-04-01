package com.egen.fitogen.service;

import com.egen.fitogen.domain.NumberingConfig;
import com.egen.fitogen.domain.NumberingSectionType;
import com.egen.fitogen.domain.NumberingType;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.repository.NumberingConfigRepository;

public class NumberingConfigService {

    private final NumberingConfigRepository repository;
    private final NumberingService numberingService;
    private final AuditLogService auditLogService;

    public NumberingConfigService(
            NumberingConfigRepository repository,
            NumberingService numberingService,
            AuditLogService auditLogService) {

        this.repository = repository;
        this.numberingService = numberingService;
        this.auditLogService = auditLogService;
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
            NumberingConfig saved = repository.findByType(config.getType());
            log("CREATE", config.getType(), null, saved != null ? saved : config);
            return;
        }

        config.setId(existing.getId());
        repository.update(config);
        log("UPDATE", config.getType(), existing, config);
    }

    public String preview(NumberingConfig config) {
        PlantBatch sampleBatch = new PlantBatch();
        sampleBatch.setInteriorBatchNo("INT-001");
        sampleBatch.setExteriorBatchNo("EXT-001");

        return numberingService.previewNumber(config, sampleBatch);
    }

    private void log(String actionType, NumberingType type, NumberingConfig previous, NumberingConfig current) {
        if (auditLogService == null) {
            return;
        }

        String previousDescription = describe(previous);
        String currentDescription = describe(current);

        if ("UPDATE".equals(actionType) && previousDescription.equals(currentDescription)) {
            return;
        }

        String description = "Zapisano konfigurację numeracji dla typu " + type
                + ". Poprzednio: " + previousDescription
                + ". Obecnie: " + currentDescription;

        auditLogService.log("NUMBERING_CONFIG", current != null ? current.getId() : null, actionType, description);
    }

    private String describe(NumberingConfig config) {
        if (config == null) {
            return "[brak danych]";
        }

        return "s1=" + section(config.getSection1Type(), config.getSection1StaticValue(), config.getSection1Separator())
                + ", s2=" + section(config.getSection2Type(), config.getSection2StaticValue(), config.getSection2Separator())
                + ", s3=" + section(config.getSection3Type(), config.getSection3StaticValue(), config.getSection3Separator())
                + ", counter=" + config.getCurrentCounter();
    }

    private String section(NumberingSectionType type, String staticValue, String separator) {
        return value(type) + "|" + value(staticValue) + "|" + value(separator);
    }

    private String value(Object value) {
        if (value == null) {
            return "[puste]";
        }
        String textValue = String.valueOf(value).trim();
        return textValue.isBlank() ? "[puste]" : textValue;
    }

    private boolean isEffectivelyEmpty(NumberingConfig config) {
        return config.getSection1Type() == null
                && config.getSection2Type() == null
                && config.getSection3Type() == null;
    }
}