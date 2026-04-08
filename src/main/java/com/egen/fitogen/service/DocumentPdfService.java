package com.egen.fitogen.service;

import com.egen.fitogen.dto.DocumentPreviewDTO;
import com.egen.fitogen.dto.DocumentPreviewItemDTO;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class DocumentPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public void export(DocumentPreviewDTO preview, File outputFile) {
        if (preview == null) {
            throw new IllegalArgumentException("Brak danych dokumentu do eksportu PDF.");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Nie wybrano pliku PDF.");
        }

        Document pdf = new Document(PageSize.A4, 36, 36, 36, 36);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFile);
            PdfWriter.getInstance(pdf, out);
            pdf.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font smallBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            pdf.add(new Paragraph("Fito Gen Essentials — Podgląd dokumentu", smallBold));
            Paragraph title = new Paragraph(
                    safe(preview.getDocumentType()) + "  " + safe(preview.getDocumentNumber()),
                    titleFont
            );
            title.setSpacingBefore(8);
            title.setSpacingAfter(10);
            pdf.add(title);

            if (preview.isCancelled()) {
                Paragraph cancelled = new Paragraph(
                        "ANULOWANY",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)
                );
                cancelled.setSpacingAfter(8);
                pdf.add(cancelled);
            }

            PdfPTable meta = new PdfPTable(new float[]{1f, 1f});
            meta.setWidthPercentage(100);
            meta.setSpacingAfter(12);
            meta.addCell(infoCell("Numer dokumentu", safe(preview.getDocumentNumber()), sectionFont, normalFont));
            meta.addCell(infoCell(
                    "Data wystawienia",
                    preview.getIssueDate() != null ? preview.getIssueDate().format(DATE_FORMATTER) : "",
                    sectionFont, normalFont
            ));
            meta.addCell(infoCell("Typ dokumentu", safe(preview.getDocumentType()), sectionFont, normalFont));
            meta.addCell(infoCell("Utworzył", safe(preview.getCreatedBy()), sectionFont, normalFont));
            meta.addCell(infoCell("Status", safe(preview.getStatusLabel()), sectionFont, normalFont));
            meta.addCell(infoCell("Łączna ilość", String.valueOf(preview.getTotalQty()), sectionFont, normalFont));
            pdf.add(meta);

            PdfPTable parties = new PdfPTable(new float[]{1f, 1f});
            parties.setWidthPercentage(100);
            parties.setSpacingAfter(12);
            parties.addCell(blockCell(
                    "Wystawca",
                    preview.getIssuerName(),
                    preview.getIssuerAddressLine1(),
                    preview.getIssuerAddressLine2(),
                    preview.getIssuerPhytosanitaryNumber(),
                    sectionFont,
                    normalFont
            ));
            parties.addCell(blockCell(
                    "Klient",
                    preview.getCustomerName(),
                    preview.getCustomerAddressLine1(),
                    preview.getCustomerAddressLine2(),
                    preview.getCustomerPhytosanitaryNumber(),
                    sectionFont,
                    normalFont
            ));
            pdf.add(parties);

            PdfPTable items = new PdfPTable(new float[]{0.6f, 3.6f, 2.0f, 1.0f, 1.3f});
            items.setWidthPercentage(100);
            items.setSpacingAfter(12);
            addHeader(items, "Lp", smallBold);
            addHeader(items, "Roślina", smallBold);
            addHeader(items, "Partia", smallBold);
            addHeader(items, "Ilość", smallBold);
            addHeader(items, "Paszport", smallBold);

            for (DocumentPreviewItemDTO item : preview.getItems()) {
                items.addCell(bodyCell(String.valueOf(item.getLp()), normalFont));
                items.addCell(bodyCell(safe(item.getPlantName()), normalFont));
                items.addCell(bodyCell(safe(item.getBatchNumber()), normalFont));
                items.addCell(bodyCell(String.valueOf(item.getQty()), normalFont));
                items.addCell(bodyCell(safe(item.getPassportLabel()), normalFont));
            }
            pdf.add(items);

            if (preview.getComments() != null && !preview.getComments().isBlank()) {
                pdf.add(new Paragraph("Uwagi", sectionFont));
                Paragraph comments = new Paragraph(preview.getComments(), normalFont);
                comments.setSpacingAfter(12);
                pdf.add(comments);
            }

            PdfPTable signatures = new PdfPTable(new float[]{1f, 1f});
            signatures.setWidthPercentage(100);
            signatures.addCell(signatureCell("Podpis wystawcy", normalFont));
            signatures.addCell(signatureCell("Podpis odbiorcy", normalFont));
            pdf.add(signatures);

        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się wyeksportować dokumentu do PDF.", e);
        } finally {
            if (pdf.isOpen()) {
                pdf.close();
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private PdfPCell infoCell(String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        return cell;
    }

    private PdfPCell blockCell(
            String section,
            String line1,
            String line2,
            String line3,
            String line4,
            Font sectionFont,
            Font normalFont
    ) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.addElement(new Paragraph(section, sectionFont));
        if (!safe(line1).isBlank()) cell.addElement(new Paragraph(line1, normalFont));
        if (!safe(line2).isBlank()) cell.addElement(new Paragraph(line2, normalFont));
        if (!safe(line3).isBlank()) cell.addElement(new Paragraph(line3, normalFont));
        if (!safe(line4).isBlank()) cell.addElement(new Paragraph("Nr fitosanitarny: " + line4, normalFont));
        return cell;
    }

    private void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private PdfPCell bodyCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell signatureCell(String label, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(70f);
        cell.setPaddingTop(45f);
        cell.setPhrase(new Phrase(label, font));
        return cell;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}