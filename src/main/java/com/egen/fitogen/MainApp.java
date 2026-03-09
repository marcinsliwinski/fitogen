package com.egen.fitogen;

import com.egen.fitogen.database.SqliteContrahentRepository;
import com.egen.fitogen.database.SqliteDocumentRepository;
import com.egen.fitogen.database.SqlitePlantBatchRepository;
import com.egen.fitogen.domain.Contrahent;
import com.egen.fitogen.domain.Document;
import com.egen.fitogen.domain.PlantBatch;
import com.egen.fitogen.service.ContrahentService;
import com.egen.fitogen.service.DocumentService;
import com.egen.fitogen.service.PlantBatchService;

import java.time.LocalDate;
import java.util.List;

public class MainApp {
    public static void main(String[] args) {
        // Inicjalizacja repozytoriów
        ContrahentService contrahentService = new ContrahentService(new SqliteContrahentRepository());
        PlantBatchService plantBatchService = new PlantBatchService(new SqlitePlantBatchRepository());
        DocumentService documentService = new DocumentService(new SqliteDocumentRepository());

        // Dodawanie przykładowego kontrahenta
        Contrahent c1 = new Contrahent();
        c1.setName("Szkółka Zielona");
        c1.setCountryCode("PL");
        c1.setNip("1234567890");
        contrahentService.addContrahent(c1);

        // Dodawanie przykładowej partii
        PlantBatch b1 = new PlantBatch();
        b1.setInteriorBatchNo("INT001");
        b1.setExteriorBatchNo("EXT001");
        b1.setQty(100);
        b1.setCreationDate(LocalDate.now());
        b1.setContrahentId(c1.getId());
        plantBatchService.addBatch(b1);

        // Dodawanie przykładowego dokumentu
        Document d1 = new Document(b1.getId(), c1.getId(), "Dokument dostawcy", "Jan Kowalski", false, "Uwagi testowe", LocalDate.now());
        documentService.addDocument(d1);

        // Wyświetlenie wszystkich kontrahentów
        List<Contrahent> kontrahenci = contrahentService.getAllContrahents();
        System.out.println("=== Contrahents ===");
        kontrahenci.forEach(k -> System.out.println(k));

        // Wyświetlenie wszystkich partii
        List<PlantBatch> batches = plantBatchService.getAllBatches();
        System.out.println("=== Plant Batches ===");
        batches.forEach(System.out::println);

        // Wyświetlenie wszystkich dokumentów
        List<Document> documents = documentService.getAllDocuments();
        System.out.println("=== Documents ===");
        documents.forEach(System.out::println);
    }
}