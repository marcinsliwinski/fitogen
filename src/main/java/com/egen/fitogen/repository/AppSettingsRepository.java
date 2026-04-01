package com.egen.fitogen.repository;

import java.util.Map;
import java.util.Optional;

public interface AppSettingsRepository {

    Optional<String> findValueByKey(String key);

    void upsert(String key, String value);

    Map<String, String> findAll();
}