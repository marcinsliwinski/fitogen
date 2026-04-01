package com.egen.fitogen.mapper;

import com.egen.fitogen.dto.DocumentDTO;
import com.egen.fitogen.dto.DocumentItemDTO;
import com.egen.fitogen.model.Document;
import com.egen.fitogen.model.DocumentItem;

public class DocumentMapper {

    private DocumentMapper() {
    }

    public static Document toEntity(DocumentDTO dto) {
        Document d = new Document();
        d.setId(dto.getId());
        d.setDocumentNumber(dto.getDocumentNumber());
        d.setDocumentType(dto.getDocumentType());
        d.setIssueDate(dto.getIssueDate());
        d.setContrahentId(dto.getContrahentId());
        d.setCreatedBy(dto.getCreatedBy());
        d.setComments(dto.getComments());
        d.setStatus(dto.getStatus());
        return d;
    }

    public static DocumentDTO toDto(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setDocumentNumber(document.getDocumentNumber());
        dto.setDocumentType(document.getDocumentType());
        dto.setIssueDate(document.getIssueDate());
        dto.setContrahentId(document.getContrahentId());
        dto.setCreatedBy(document.getCreatedBy());
        dto.setComments(document.getComments());
        dto.setStatus(document.getStatus());
        return dto;
    }

    public static DocumentItem toEntity(DocumentItemDTO dto) {
        DocumentItem item = new DocumentItem();
        item.setPlantBatchId(dto.getPlantBatchId());
        item.setQty(dto.getQty());
        item.setPassportRequired(dto.isPassportRequired());
        return item;
    }

    public static DocumentItemDTO toDto(DocumentItem item) {
        DocumentItemDTO dto = new DocumentItemDTO();
        dto.setPlantBatchId(item.getPlantBatchId());
        dto.setQty(item.getQty());
        dto.setPassportRequired(item.isPassportRequired());
        return dto;
    }
}