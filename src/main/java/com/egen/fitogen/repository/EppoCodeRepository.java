package com.egen.fitogen.repository;

import com.egen.fitogen.model.EppoCode;

import java.util.List;

public interface EppoCodeRepository {

    List<EppoCode> findAll();

    EppoCode findById(int id);

    EppoCode findByCode(String code);

    void save(EppoCode eppoCode);

    void update(EppoCode eppoCode);

    void deleteById(int id);
}