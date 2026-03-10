package com.egen.fitogen.service;

import com.egen.fitogen.domain.Document;
import com.egen.fitogen.domain.DocumentItem;
import com.egen.fitogen.dto.DocumentDTO;
import com.egen.fitogen.dto.DocumentItemDTO;
import com.egen.fitogen.mapper.DocumentMapper;
import com.egen.fitogen.repository.DocumentItemRepository;
import com.egen.fitogen.repository.DocumentRepository;
import com.egen.fitogen.util.NumeratorService;

public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentItemRepository itemRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentItemRepository itemRepository) {

        this.documentRepository = documentRepository;
        this.itemRepository = itemRepository;
    }

    public void createDocument(DocumentDTO dto) {

        String number =
                NumeratorService.generateDocumentNumber(dto.getDocumentType());

        dto.setDocumentNumber(number);

        Document document = DocumentMapper.toEntity(dto);

        documentRepository.save(document);

        for (DocumentItemDTO itemDTO : dto.getItems()) {

            DocumentItem item = new DocumentItem();

            item.setDocumentId(document.getId());
            item.setPlantBatchId(itemDTO.getPlantBatchId());
            item.setQty(itemDTO.getQty());
            item.setPassportRequired(itemDTO.isPassportRequired());

            itemRepository.save(item);
        }
    }
}