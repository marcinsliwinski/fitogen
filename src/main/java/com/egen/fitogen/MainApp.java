package com.egen.fitogen;

import com.egen.fitogen.database.*;
import com.egen.fitogen.domain.Contrahent;
import com.egen.fitogen.dto.DocumentDTO;
import com.egen.fitogen.dto.DocumentItemDTO;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.repository.DocumentItemRepository;
import com.egen.fitogen.repository.DocumentRepository;
import com.egen.fitogen.service.ContrahentService;
import com.egen.fitogen.service.DocumentService;

import java.util.List;

public class MainApp {

    public static void main(String[] args) {

        DatabaseInitializer.initDatabase();

        System.out.println("Fitogen database ready!");

        ContrahentRepository repo = new SqliteContrahentRepository();

        ContrahentService contrahentService =
                new ContrahentService(repo);

        Contrahent c = new Contrahent();

        c.setName("Szkółka Zielona");
        c.setCountry("Polska");
        c.setCountryCode("PL");
        c.setCity("Warszawa");
        c.setPostalCode("00-001");
        c.setStreet("Ogrodowa 1");
        c.setPhytosanitaryNumber("PL12345");

        contrahentService.addContrahent(c);

        contrahentService.getAllContrahents()
                .forEach(System.out::println);

        DocumentRepository documentRepo = new SqliteDocumentRepository();
        DocumentItemRepository itemRepo = new SqliteDocumentItemRepository();

        DocumentService documentService =
                new DocumentService(documentRepo, itemRepo);

        DocumentDTO doc = new DocumentDTO();

        doc.setDocumentType("SUPPLIER_DOCUMENT");
        doc.setIssueDate(java.time.LocalDate.now());
        doc.setContrahentId(1);
        doc.setCreatedBy("admin");

        DocumentItemDTO item = new DocumentItemDTO();

        item.setPlantBatchId(1);
        item.setQty(50);
        item.setPassportRequired(true);

        doc.setItems(List.of(item));

        documentService.createDocument(doc);

        System.out.println("Test document created");
    }
}