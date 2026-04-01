package com.egen.fitogen.repository;

import com.egen.fitogen.model.EppoZone;

import java.util.List;

public interface EppoZoneRepository {

    List<EppoZone> findAll();

    EppoZone findById(int id);

    EppoZone findByCode(String code);

    void save(EppoZone eppoZone);

    void update(EppoZone eppoZone);

    void deleteById(int id);
}