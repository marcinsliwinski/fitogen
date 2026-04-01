package com.egen.fitogen.repository;

import com.egen.fitogen.model.EppoCodeSpeciesLink;

import java.util.List;

public interface EppoCodeSpeciesLinkRepository {

    List<EppoCodeSpeciesLink> findAll();

    List<EppoCodeSpeciesLink> findByEppoCodeId(int eppoCodeId);

    boolean exists(int eppoCodeId, String speciesName, String latinSpeciesName);

    void save(EppoCodeSpeciesLink link);

    void deleteById(int id);

    void deleteByEppoCodeId(int eppoCodeId);
}
