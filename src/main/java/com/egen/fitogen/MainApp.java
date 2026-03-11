package com.egen.fitogen;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.database.DatabaseInitializer;
import com.egen.fitogen.domain.Contrahent;
import com.egen.fitogen.dto.DocumentDTO;
import com.egen.fitogen.dto.DocumentItemDTO;
import com.egen.fitogen.service.ContrahentService;
import com.egen.fitogen.service.DocumentService;

import java.time.LocalDate;
import java.util.List;

public class MainApp {

    public static void main(String[] args) {

        DatabaseInitializer.initDatabase();

        System.out.println("Fitogen database ready!");

        // uruchomienie kontenera zależności
        AppContext.init();

        // pobranie serwisów z kontekstu
        ContrahentService contrahentService =
                AppContext.getContrahentService();

        DocumentService documentService =
                AppContext.getDocumentService();


        // ===== TEST CONTRAHENT =====

        Contrahent c = new Contrahent();

        c.setName("Szkółka Zielona");
        c.setCountry("Polska");
        c.setCountryCode("PL");
        c.setCity("Warszawa");
        c.setPostalCode("00-001");
        c.setStreet("Ogrodowa 1");
        c.setPhytosanitaryNumber("PL12345");

        contrahentService.addContrahent(c);

        System.out.println("=== Contrahents ===");

        contrahentService.getAllContrahents()
                .forEach(System.out::println);


        // ===== TEST DOKUMENTU =====

        DocumentDTO doc = new DocumentDTO();

        doc.setDocumentType("SUPPLIER_DOCUMENT");
        doc.setIssueDate(LocalDate.now());
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