package com.egen.fitogen.repository;

import com.egen.fitogen.model.EppoCodeZoneLink;

import java.util.List;

public interface EppoCodeZoneLinkRepository {

    List<EppoCodeZoneLink> findAll();

    List<EppoCodeZoneLink> findByEppoCodeId(int eppoCodeId);

    List<EppoCodeZoneLink> findByEppoZoneId(int eppoZoneId);

    boolean exists(int eppoCodeId, int eppoZoneId);

    void save(EppoCodeZoneLink link);

    void deleteById(int id);

    void deleteByPair(int eppoCodeId, int eppoZoneId);

    void deleteByEppoCodeId(int eppoCodeId);

    void deleteByEppoZoneId(int eppoZoneId);
}