package com.egen.fitogen.repository;

import com.egen.fitogen.domain.NumberingConfig;
import com.egen.fitogen.domain.NumberingType;

public interface NumberingConfigRepository {

    NumberingConfig findByType(NumberingType type);

    boolean existsByType(NumberingType type);

    void save(NumberingConfig config);

    void update(NumberingConfig config);
}