package com.egen.fitogen.service;

import com.egen.fitogen.domain.NumberingConfig;
import com.egen.fitogen.domain.NumberingSectionType;
import com.egen.fitogen.domain.NumberingType;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.repository.NumberingConfigRepository;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NumberingService {

    private final NumberingConfigRepository numberingConfigRepository;

    public NumberingService(NumberingConfigRepository numberingConfigRepository) {
        this.numberingConfigRepository = numberingConfigRepository;
    }

    public String generateNextNumber(NumberingType type) {
        return generateNextNumber(type, null, LocalDate.now());
    }

    public String generateNextNumber(NumberingType type, PlantBatch batchContext) {
        return generateNextNumber(type, batchContext, LocalDate.now());
    }

    public String generateNextNumber(NumberingType type, PlantBatch batchContext, LocalDate dateContext) {
        NumberingConfig config = getOrCreateUsableConfig(type);

        validateConfig(config, batchContext);

        int nextCounter = config.getCurrentCounter() + 1;
        String generatedNumber = composeNumber(config, nextCounter, batchContext, dateContext);

        if (generatedNumber == null || generatedNumber.isBlank()) {
            throw new IllegalStateException("Nie udało się wygenerować numeru. Konfiguracja jest pusta.");
        }

        config.setCurrentCounter(nextCounter);
        numberingConfigRepository.update(config);

        return generatedNumber;
    }

    public String previewNumber(NumberingType type, PlantBatch batchContext) {
        NumberingConfig config = getOrCreateUsableConfig(type);
        validateConfig(config, batchContext);
        return composeNumber(config, config.getCurrentCounter() + 1, batchContext, LocalDate.now());
    }

    public String previewNumber(NumberingConfig config, PlantBatch batchContext) {
        if (config == null) {
            return "";
        }

        NumberingConfig effectiveConfig = isEffectivelyEmpty(config)
                ? buildDefaultConfig(config.getType())
                : config;

        validateConfig(effectiveConfig, batchContext);
        return composeNumber(effectiveConfig, effectiveConfig.getCurrentCounter() + 1, batchContext, LocalDate.now());
    }

    private NumberingConfig getOrCreateUsableConfig(NumberingType type) {
        NumberingConfig config = numberingConfigRepository.findByType(type);

        if (config == null) {
            NumberingConfig defaultConfig = buildDefaultConfig(type);
            numberingConfigRepository.save(defaultConfig);
            return defaultConfig;
        }

        if (isEffectivelyEmpty(config)) {
            NumberingConfig defaultConfig = buildDefaultConfig(type);
            defaultConfig.setId(config.getId());
            defaultConfig.setCurrentCounter(config.getCurrentCounter());
            numberingConfigRepository.update(defaultConfig);
            return defaultConfig;
        }

        return config;
    }

    private boolean isEffectivelyEmpty(NumberingConfig config) {
        return config.getSection1Type() == null
                && config.getSection2Type() == null
                && config.getSection3Type() == null;
    }

    private NumberingConfig buildDefaultConfig(NumberingType type) {
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

    private String composeNumber(
            NumberingConfig config,
            int counter,
            PlantBatch batchContext,
            LocalDate dateContext) {

        List<SectionValue> sections = new ArrayList<>();
        sections.add(buildSection(config.getSection1Type(), config.getSection1StaticValue(), config.getSection1Separator(), counter, batchContext, dateContext));
        sections.add(buildSection(config.getSection2Type(), config.getSection2StaticValue(), config.getSection2Separator(), counter, batchContext, dateContext));
        sections.add(buildSection(config.getSection3Type(), config.getSection3StaticValue(), config.getSection3Separator(), counter, batchContext, dateContext));

        StringBuilder result = new StringBuilder();

        for (SectionValue section : sections) {
            if (section == null || section.value().isBlank()) {
                continue;
            }

            result.append(section.value());

            if (section.separator() != null) {
                result.append(section.separator());
            }
        }

        return result.toString().trim();
    }

    private void validateConfig(NumberingConfig config, PlantBatch batchContext) {
        int autoIncrementCount = 0;

        autoIncrementCount += isAutoIncrement(config.getSection1Type()) ? 1 : 0;
        autoIncrementCount += isAutoIncrement(config.getSection2Type()) ? 1 : 0;
        autoIncrementCount += isAutoIncrement(config.getSection3Type()) ? 1 : 0;

        if (autoIncrementCount > 1) {
            throw new IllegalStateException("Konfiguracja numeracji może zawierać tylko jedną sekcję AUTO_INCREMENT.");
        }

        validateStaticText(config.getSection1Type(), config.getSection1StaticValue(), "section1");
        validateStaticText(config.getSection2Type(), config.getSection2StaticValue(), "section2");
        validateStaticText(config.getSection3Type(), config.getSection3StaticValue(), "section3");

        validateBatchContext(config.getSection1Type(), batchContext, "section1");
        validateBatchContext(config.getSection2Type(), batchContext, "section2");
        validateBatchContext(config.getSection3Type(), batchContext, "section3");
    }

    private void validateStaticText(NumberingSectionType sectionType, String staticValue, String sectionName) {
        if (sectionType == NumberingSectionType.STATIC_TEXT
                && (staticValue == null || staticValue.isBlank())) {
            throw new IllegalStateException("Sekcja " + sectionName + " ma typ STATIC_TEXT, ale nie ma ustawionej wartości.");
        }
    }

    private void validateBatchContext(NumberingSectionType sectionType, PlantBatch batchContext, String sectionName) {
        if ((sectionType == NumberingSectionType.INTERIOR_BATCH
                || sectionType == NumberingSectionType.EXTERIOR_BATCH_NO)
                && batchContext == null) {
            throw new IllegalStateException("Sekcja " + sectionName + " wymaga kontekstu partii.");
        }
    }

    private boolean isAutoIncrement(NumberingSectionType type) {
        return type == NumberingSectionType.AUTO_INCREMENT;
    }

    private SectionValue buildSection(
            NumberingSectionType type,
            String staticValue,
            String separator,
            int counter,
            PlantBatch batchContext,
            LocalDate dateContext) {

        if (type == null) {
            return null;
        }

        String value = resolveSectionValue(type, staticValue, counter, batchContext, dateContext);

        if (value == null || value.isBlank()) {
            return null;
        }

        return new SectionValue(value, separator);
    }

    private String resolveSectionValue(
            NumberingSectionType type,
            String staticValue,
            int counter,
            PlantBatch batchContext,
            LocalDate dateContext) {

        return switch (type) {
            case AUTO_INCREMENT -> String.valueOf(counter);
            case YEAR -> String.valueOf(dateContext.getYear());
            case MONTH -> String.format("%02d", dateContext.getMonthValue());
            case WEEK -> String.format("%02d", dateContext.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()));
            case STATIC_TEXT -> staticValue != null ? staticValue.trim() : "";
            case INTERIOR_BATCH -> batchContext != null && batchContext.getInteriorBatchNo() != null
                    ? batchContext.getInteriorBatchNo().trim()
                    : "";
            case EXTERIOR_BATCH_NO -> batchContext != null && batchContext.getExteriorBatchNo() != null
                    ? batchContext.getExteriorBatchNo().trim()
                    : "";
        };
    }

    private record SectionValue(String value, String separator) {
    }
}