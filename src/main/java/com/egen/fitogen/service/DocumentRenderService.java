package com.egen.fitogen.service;

import com.egen.fitogen.config.AppContext;
import com.egen.fitogen.database.SqliteContrahentRepository;
import com.egen.fitogen.database.SqlitePlantBatchRepository;
import com.egen.fitogen.database.SqlitePlantRepository;
import com.egen.fitogen.dto.DocumentDTO;
import com.egen.fitogen.dto.DocumentItemDTO;
import com.egen.fitogen.dto.DocumentPreviewDTO;
import com.egen.fitogen.dto.DocumentPreviewItemDTO;
import com.egen.fitogen.dto.PassportPreviewDTO;
import com.egen.fitogen.model.Contrahent;
import com.egen.fitogen.model.DocumentStatus;
import com.egen.fitogen.model.IssuerProfile;
import com.egen.fitogen.model.Plant;
import com.egen.fitogen.model.PlantBatch;
import com.egen.fitogen.repository.ContrahentRepository;
import com.egen.fitogen.repository.PlantBatchRepository;
import com.egen.fitogen.repository.PlantRepository;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public class DocumentRenderService {

    private static final DateTimeFormatter ISSUE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DocumentService documentService;
    private final ContrahentRepository contrahentRepository;
    private final PlantBatchRepository plantBatchRepository;
    private final PlantRepository plantRepository;
    private final AppSettingsService appSettingsService;

    public DocumentRenderService(DocumentService documentService) {
        this(
                documentService,
                new SqliteContrahentRepository(),
                new SqlitePlantBatchRepository(),
                new SqlitePlantRepository(),
                AppContext.getAppSettingsService()
        );
    }

    public DocumentRenderService(
            DocumentService documentService,
            ContrahentRepository contrahentRepository,
            PlantBatchRepository plantBatchRepository,
            PlantRepository plantRepository,
            AppSettingsService appSettingsService
    ) {
        this.documentService = documentService;
        this.contrahentRepository = contrahentRepository;
        this.plantBatchRepository = plantBatchRepository;
        this.plantRepository = plantRepository;
        this.appSettingsService = appSettingsService;
    }

    public DocumentPreviewDTO buildPreview(int documentId) {
        DocumentDTO document = documentService.getDocumentDetails(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Dokument nie istnieje.");
        }
        return buildPreview(document);
    }

    public DocumentPreviewDTO buildPreview(DocumentDTO document) {
        if (document == null) {
            throw new IllegalArgumentException("Brak danych dokumentu do podglądu.");
        }

        DocumentPreviewDTO preview = new DocumentPreviewDTO();
        preview.setDocumentId(document.getId());
        preview.setDocumentNumber(safe(document.getDocumentNumber()));
        preview.setDocumentType(safe(document.getDocumentType()));
        preview.setCreatedBy(safe(document.getCreatedBy()));
        preview.setComments(safe(document.getComments()));
        preview.setStatusLabel(formatStatus(document.getStatus()));
        preview.setCancelled(document.getStatus() == DocumentStatus.CANCELLED);
        preview.setIssueDate(document.getIssueDate());
        preview.setPrintPassports(document.isPrintPassports());
        preview.setIssueDateLabel(document.getIssueDate() != null ? document.getIssueDate().format(ISSUE_DATE_FORMATTER) : "");

        fillIssuer(preview);
        fillCustomer(preview, document.getContrahentId());
        fillItems(preview, document);
        fillPassports(preview, document);

        return preview;
    }

    private void fillIssuer(DocumentPreviewDTO preview) {
        IssuerProfile issuer = appSettingsService.getIssuerProfile();

        preview.setIssuePlaceLabel(safe(issuer.getCity()));
        preview.setIssuerName(safe(issuer.getNurseryName()));

        if (issuer != null && issuer.isNoStreet()) {
            preview.setIssuerAddressLine1(joinWithSpace(safe(issuer.getCity()), safe(issuer.getStreet())));
            preview.setIssuerAddressLine2(joinWithNewLine(
                    joinWithSpace(safe(issuer.getPostalCode()), safe(issuer.getCity())),
                    joinWithSpace(safe(issuer.getCountryCode()), safe(issuer.getCountry()))
            ));
        } else {
            preview.setIssuerAddressLine1(safe(issuer.getStreet()));
            preview.setIssuerAddressLine2(joinWithSpace(
                    safe(issuer.getPostalCode()),
                    joinWithSpace(safe(issuer.getCity()), joinWithSpace(safe(issuer.getCountryCode()), safe(issuer.getCountry())))
            ));
        }

        preview.setIssuerPhytosanitaryNumber(safe(issuer.getPhytosanitaryNumber()));
    }

    private void fillCustomer(DocumentPreviewDTO preview, int contrahentId) {
        Contrahent contrahent = contrahentRepository.findById(contrahentId);
        if (contrahent == null) {
            preview.setContrahentName("");
            preview.setContrahentAddress("");
            preview.setCustomerName("");
            preview.setCustomerAddressLine1("");
            preview.setCustomerAddressLine2("");
            preview.setCustomerPhytosanitaryNumber("");
            return;
        }

        String addressLine1;
        String addressLine2;
        if (contrahent.isNoStreet()) {
            addressLine1 = joinWithSpace(safe(contrahent.getCity()), safe(contrahent.getStreet()));
            addressLine2 = joinWithNewLine(
                    joinWithSpace(safe(contrahent.getPostalCode()), safe(contrahent.getCity())),
                    joinWithSpace(safe(contrahent.getCountryCode()), safe(contrahent.getCountry()))
            );
        } else {
            addressLine1 = safe(contrahent.getStreet());
            addressLine2 = joinWithNewLine(
                    joinWithSpace(safe(contrahent.getPostalCode()), safe(contrahent.getCity())),
                    joinWithSpace(safe(contrahent.getCountryCode()), safe(contrahent.getCountry()))
            );
        }

        preview.setContrahentName(safe(contrahent.getName()));
        preview.setContrahentAddress(joinWithNewLine(addressLine1, addressLine2));

        preview.setCustomerName(safe(contrahent.getName()));
        preview.setCustomerAddressLine1(addressLine1);
        preview.setCustomerAddressLine2(addressLine2);
        preview.setCustomerPhytosanitaryNumber(safe(contrahent.getPhytosanitaryNumber()));
    }

    private void fillItems(DocumentPreviewDTO preview, DocumentDTO document) {
        int lp = 1;
        int totalQty = 0;
        LocalDate referenceDate = document.getIssueDate() != null ? document.getIssueDate() : LocalDate.now();

        if (document.getItems() == null) {
            preview.setTotalQty(0);
            return;
        }

        for (DocumentItemDTO item : document.getItems()) {
            PlantBatch batch = plantBatchRepository.findById(item.getPlantBatchId());
            Plant plant = batch != null ? plantRepository.findById(batch.getPlantId()) : null;

            DocumentPreviewItemDTO previewItem = new DocumentPreviewItemDTO();
            previewItem.setLp(lp++);
            previewItem.setPlantName(plant != null ? safe(plant.toString()) : "");
            previewItem.setBatchNumber(batch != null ? resolveBatchNumber(batch) : "");
            previewItem.setBatchAgeLabel(batch != null ? buildBatchAgeLabel(batch, referenceDate) : "");
            previewItem.setBatchCategoryLabel(batch != null ? safe(batch.getFitoQualificationCategory()) : "");
            previewItem.setQty(item.getQty());
            previewItem.setPassportLabel(item.isPassportRequired() ? "Tak" : "Nie");

            preview.getItems().add(previewItem);
            totalQty += item.getQty();
        }

        preview.setTotalQty(totalQty);
    }


    private void fillPassports(DocumentPreviewDTO preview, DocumentDTO document) {
        preview.getPassports().clear();
        if (preview == null || document == null || !document.isPrintPassports() || document.getItems() == null) {
            return;
        }

        IssuerProfile issuer = appSettingsService.getIssuerProfile();
        int itemNo = 1;
        for (DocumentItemDTO item : document.getItems()) {
            if (!item.isPassportRequired()) {
                itemNo++;
                continue;
            }

            PlantBatch batch = plantBatchRepository.findById(item.getPlantBatchId());
            Plant plant = batch != null ? plantRepository.findById(batch.getPlantId()) : null;

            PassportPreviewDTO passport = new PassportPreviewDTO();
            passport.setItemNo(itemNo);
            passport.setPlantName(plant != null ? safe(plant.toString()) : "");
            passport.setBotanicalName(resolvePassportBotanicalName(plant));
            passport.setOperatorNumber(resolvePassportOperatorNumber(issuer));
            passport.setTraceabilityCode(batch != null ? resolveBatchNumber(batch) : "");
            passport.setOriginCountryCode(resolvePassportOriginCountryCode(batch, issuer));
            passport.setQuantityLabel(String.valueOf(Math.max(0, item.getQty())));
            passport.setEppoCode(plant != null ? safe(plant.getEppoCode()) : "");
            passport.setCategoryLabel(batch != null ? safe(batch.getFitoQualificationCategory()) : "");
            passport.setDocumentNumber(preview.getDocumentNumber());
            preview.getPassports().add(passport);
            itemNo++;
        }
    }

    private String resolvePassportBotanicalName(Plant plant) {
        if (plant == null) {
            return "";
        }
        String latin = safe(plant.getLatinSpeciesName());
        if (!latin.isBlank()) {
            return latin;
        }
        return safe(plant.toString());
    }

    private String resolvePassportOperatorNumber(IssuerProfile issuer) {
        if (issuer == null) {
            return "";
        }
        return safe(issuer.getPhytosanitaryNumber());
    }

    private String resolvePassportOriginCountryCode(PlantBatch batch, IssuerProfile issuer) {
        if (batch != null && !safe(batch.getManufacturerCountryCode()).isBlank()) {
            return safe(batch.getManufacturerCountryCode());
        }
        if (issuer != null) {
            return safe(issuer.getCountryCode());
        }
        return "";
    }

    private String buildBatchAgeLabel(PlantBatch batch, LocalDate referenceDate) {
        if (batch == null) {
            return "";
        }
        if (batch.getAgeYears() > 0) {
            return String.valueOf(batch.getAgeYears());
        }
        if (batch.getCreationDate() == null) {
            return "";
        }

        LocalDate batchDate = batch.getCreationDate();
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }
        if (referenceDate.isBefore(batchDate)) {
            return "1";
        }

        Period period = Period.between(batchDate, referenceDate);
        int ageInYears = Math.max(1, period.getYears());
        return String.valueOf(ageInYears);
    }

    private String resolveBatchNumber(PlantBatch batch) {
        if (batch.getInteriorBatchNo() != null && !batch.getInteriorBatchNo().isBlank()) {
            return batch.getInteriorBatchNo();
        }
        if (batch.getExteriorBatchNo() != null && !batch.getExteriorBatchNo().isBlank()) {
            return batch.getExteriorBatchNo();
        }
        return "Partia ID: " + batch.getId();
    }

    private String formatStatus(DocumentStatus status) {
        if (status == null || status == DocumentStatus.ACTIVE) {
            return "Aktywny";
        }
        return "Anulowany";
    }

    private String joinWithSpace(String left, String right) {
        String safeLeft = safe(left);
        String safeRight = safe(right);

        if (safeLeft.isBlank()) {
            return safeRight;
        }
        if (safeRight.isBlank()) {
            return safeLeft;
        }
        return safeLeft + " " + safeRight;
    }

    private String joinWithNewLine(String line1, String line2) {
        if (safe(line1).isBlank()) {
            return safe(line2);
        }
        if (safe(line2).isBlank()) {
            return safe(line1);
        }
        return safe(line1) + "\n" + safe(line2);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
