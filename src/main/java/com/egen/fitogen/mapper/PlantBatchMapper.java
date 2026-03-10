package com.egen.fitogen.mapper;

import com.egen.fitogen.domain.PlantBatch;
import com.egen.fitogen.dto.PlantBatchDTO;

public class PlantBatchMapper {

    public static PlantBatchDTO toDTO(PlantBatch batch) {

        PlantBatchDTO dto = new PlantBatchDTO();

        dto.setId(batch.getId());
        dto.setInteriorBatchNo(batch.getInteriorBatchNo());
        dto.setQty(batch.getQty());

        return dto;
    }
}