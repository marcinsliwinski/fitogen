package com.egen.fitogen.mapper;

import com.egen.fitogen.domain.Document;
import com.egen.fitogen.dto.DocumentDTO;

public class DocumentMapper {

    public static Document toEntity(DocumentDTO dto) {

        Document d = new Document();

        d.setId(dto.getId());
        d.setDocumentNumber(dto.getDocumentNumber());
        d.setDocumentType(dto.getDocumentType());
        d.setIssueDate(dto.getIssueDate());
        d.setContrahentId(dto.getContrahentId());
        d.setCreatedBy(dto.getCreatedBy());
        d.setComments(dto.getComments());

        return d;
    }
}