package com.egen.fitogen.repository;

import com.egen.fitogen.model.EppoCodePlantLink;

import java.util.List;

public interface EppoCodePlantLinkRepository {

    List<EppoCodePlantLink> findAll();

    List<EppoCodePlantLink> findByEppoCodeId(int eppoCodeId);

    List<EppoCodePlantLink> findByPlantId(int plantId);

    boolean exists(int eppoCodeId, int plantId);

    void save(EppoCodePlantLink link);

    void deleteById(int id);

    void deleteByPair(int eppoCodeId, int plantId);

    void deleteByEppoCodeId(int eppoCodeId);

    void deleteByPlantId(int plantId);
}