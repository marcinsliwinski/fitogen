package com.egen.fitogen.config;

import com.egen.fitogen.database.*;
import com.egen.fitogen.repository.*;
import com.egen.fitogen.service.*;

public class AppContext {

    private static PlantRepository plantRepository;
    private static PlantBatchRepository plantBatchRepository;
    private static ContrahentRepository contrahentRepository;
    private static DocumentRepository documentRepository;
    private static DocumentItemRepository documentItemRepository;

    private static PlantBatchService plantBatchService;
    private static ContrahentService contrahentService;
    private static DocumentService documentService;

    public static void init() {

        plantRepository = new SqlitePlantRepository();
        plantBatchRepository = new SqlitePlantBatchRepository();
        contrahentRepository = new SqliteContrahentRepository();
        documentRepository = new SqliteDocumentRepository();
        documentItemRepository = new SqliteDocumentItemRepository();

        plantBatchService = new PlantBatchService(plantBatchRepository);
        contrahentService = new ContrahentService(contrahentRepository);
        documentService = new DocumentService(documentRepository, documentItemRepository);
    }

    public static PlantBatchService getPlantBatchService() {
        return plantBatchService;
    }

    public static ContrahentService getContrahentService() {
        return contrahentService;
    }

    public static DocumentService getDocumentService() {
        return documentService;
    }
}