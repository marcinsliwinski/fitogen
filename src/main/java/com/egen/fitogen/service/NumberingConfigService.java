package com.egen.fitogen.service;

import com.egen.fitogen.domain.NumberingConfig;
import com.egen.fitogen.domain.NumberingSectionType;
import com.egen.fitogen.domain.NumberingType;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.repository.NumberingConfigRepository;

public class NumberingConfigService {

    private final NumberingConfigRepository repository;
    private final NumberingService numberingService;

    public NumberingConfigService(
            NumberingConfigRepository repository,
            NumberingService numberingService) {

        this.repository = repository;
        this.numberingService = numberingService;
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
            return;
        }

        config.setId(existing.getId());
        repository.update(config);
    }

    public String preview(NumberingConfig config) {
        PlantBatch sampleBatch = new PlantBatch();
        sampleBatch.setInteriorBatchNo("INT-001");
        sampleBatch.setExteriorBatchNo("EXT-001");

        return numberingService.previewNumber(config, sampleBatch);
    }

    private boolean isEffectivelyEmpty(NumberingConfig config) {
        return config.getSection1Type() == null
                && config.getSection2Type() == null
                && config.getSection3Type() == null;
    }
}