package com.egen.fitogen.service;

import com.egen.fitogen.dto.DocumentPreviewDTO;
import com.egen.fitogen.dto.DocumentPreviewItemDTO;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;

public class DocumentPdfService {

    public void export(DocumentPreviewDTO preview, File outputFile) {
        if (preview == null) {
            throw new IllegalArgumentException("Brak danych dokumentu do eksportu PDF.");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Nie wybrano pliku PDF.");
        }

        Document pdf = new Document(PageSize.A4, 42, 42, 36, 36);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFile);
            PdfWriter.getInstance(pdf, out);
            pdf.open();

            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font numberFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font cancelledFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            Paragraph brand = new Paragraph("Fito Gen Essentials", brandFont);
            brand.setSpacingAfter(4);
            pdf.add(brand);

            Paragraph subtitle = new Paragraph(buildSummary(preview), smallFont);
            subtitle.setSpacingAfter(12);
            pdf.add(subtitle);

            PdfPTable titleTable = new PdfPTable(new float[]{1.6f, 1f});
            titleTable.setWidthPercentage(100);
            titleTable.setSpacingAfter(10);
            titleTable.addCell(borderlessCell(new Paragraph(safe(preview.getDocumentType()), titleFont), 0, 0, 8, 0));
            titleTable.addCell(borderlessCell(new Paragraph(buildDocumentNumberTitle(preview), numberFont), 0, 0, 8, 0, PdfPCell.ALIGN_RIGHT));
            pdf.add(titleTable);

            if (preview.isCancelled()) {
                Paragraph cancelled = new Paragraph("ANULOWANY", cancelledFont);
                cancelled.setSpacingAfter(8);
                pdf.add(cancelled);
            }

            PdfPTable meta = new PdfPTable(new float[]{1f, 1.2f, 1f, 1.2f});
            meta.setWidthPercentage(100);
            meta.setSpacingAfter(12);
            addMetaRow(meta, "Status", safe(preview.getStatusLabel()), labelFont, normalFont);
            addMetaRow(meta, "Data wystawienia", safe(preview.getIssueDateLabel()), labelFont, normalFont);
            addMetaRow(meta, "Utworzył", safe(preview.getCreatedBy()), labelFont, normalFont);
            addMetaRow(meta, "Łączna ilość", String.valueOf(preview.getTotalQty()), labelFont, normalFont);
            pdf.add(meta);

            PdfPTable parties = new PdfPTable(new float[]{1f, 1f});
            parties.setWidthPercentage(100);
            parties.setSpacingAfter(12);
            parties.addCell(partyCell(
                    "Wystawca",
                    preview.getIssuerName(),
                    preview.getIssuerAddressLine1(),
                    preview.getIssuerAddressLine2(),
                    preview.getIssuerPhytosanitaryNumber(),
                    sectionFont,
                    normalFont
            ));
            parties.addCell(partyCell(
                    "Odbiorca",
                    preview.getCustomerName(),
                    preview.getCustomerAddressLine1(),
                    preview.getCustomerAddressLine2(),
                    preview.getCustomerPhytosanitaryNumber(),
                    sectionFont,
                    normalFont
            ));
            pdf.add(parties);

            PdfPTable items = new PdfPTable(new float[]{0.6f, 3.7f, 2.2f, 0.9f, 1.2f});
            items.setWidthPercentage(100);
            items.setSpacingAfter(10);
            addHeader(items, "Lp", labelFont);
            addHeader(items, "Roślina", labelFont);
            addHeader(items, "Partia", labelFont);
            addHeader(items, "Ilość", labelFont);
            addHeader(items, "Paszport", labelFont);

            for (DocumentPreviewItemDTO item : preview.getItems()) {
                items.addCell(bodyCell(String.valueOf(item.getLp()), normalFont, PdfPCell.ALIGN_CENTER));
                items.addCell(bodyCell(safe(item.getPlantName()), normalFont, PdfPCell.ALIGN_LEFT));
                items.addCell(bodyCell(safe(item.getBatchNumber()), normalFont, PdfPCell.ALIGN_LEFT));
                items.addCell(bodyCell(String.valueOf(item.getQty()), normalFont, PdfPCell.ALIGN_CENTER));
                items.addCell(bodyCell(safe(item.getPassportLabel()), normalFont, PdfPCell.ALIGN_CENTER));
            }

            if (preview.getItems() == null || preview.getItems().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Brak pozycji dokumentu.", normalFont));
                empty.setColspan(5);
                empty.setPadding(8);
                items.addCell(empty);
            }
            pdf.add(items);

            if (!safe(preview.getComments()).isBlank()) {
                Paragraph commentsTitle = new Paragraph("Uwagi", sectionFont);
                commentsTitle.setSpacingAfter(4);
                pdf.add(commentsTitle);

                Paragraph comments = new Paragraph(preview.getComments(), normalFont);
                comments.setSpacingAfter(12);
                pdf.add(comments);
            }

            PdfPTable signatures = new PdfPTable(new float[]{1f, 1f});
            signatures.setWidthPercentage(100);
            signatures.setSpacingBefore(14);
            signatures.addCell(signatureCell("Podpis wystawcy", smallFont));
            signatures.addCell(signatureCell("Podpis odbiorcy", smallFont));
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

    private void addMetaRow(PdfPTable table, String leftLabel, String leftValue, Font labelFont, Font valueFont) {
        table.addCell(metaCell(leftLabel, labelFont));
        table.addCell(metaValueCell(leftValue, valueFont));
    }

    private PdfPCell metaCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBorderWidth(0.8f);
        cell.setBorderColor(new java.awt.Color(220, 226, 232));
        return cell;
    }

    private PdfPCell metaValueCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(blankToDash(text), font));
        cell.setPadding(6);
        cell.setBorderWidth(0.8f);
        cell.setBorderColor(new java.awt.Color(220, 226, 232));
        return cell;
    }

    private PdfPCell partyCell(
            String section,
            String line1,
            String line2,
            String line3,
            String phytosanitaryNumber,
            Font sectionFont,
            Font normalFont
    ) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBorderWidth(0.8f);
        cell.setBorderColor(new java.awt.Color(220, 226, 232));
        cell.addElement(new Paragraph(section, sectionFont));
        if (!safe(line1).isBlank()) {
            cell.addElement(new Paragraph(line1, normalFont));
        }
        if (!safe(line2).isBlank()) {
            cell.addElement(new Paragraph(line2, normalFont));
        }
        if (!safe(line3).isBlank()) {
            cell.addElement(new Paragraph(line3, normalFont));
        }
        cell.addElement(new Paragraph("Nr fitosanitarny: " + blankToDash(phytosanitaryNumber), normalFont));
        return cell;
    }

    private void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setBorderWidth(0.8f);
        cell.setBorderColor(new java.awt.Color(220, 226, 232));
        table.addCell(cell);
    }

    private PdfPCell bodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(blankToDash(text), font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        cell.setBorderWidth(0.8f);
        cell.setBorderColor(new java.awt.Color(220, 226, 232));
        return cell;
    }

    private PdfPCell signatureCell(String label, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(label, font));
        cell.setFixedHeight(64f);
        cell.setPaddingTop(42f);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setVerticalAlignment(PdfPCell.ALIGN_BOTTOM);
        cell.setBorder(Rectangle.TOP);
        cell.setBorderWidthTop(0.8f);
        cell.setBorderColorTop(new java.awt.Color(130, 140, 150));
        return cell;
    }

    private PdfPCell borderlessCell(Paragraph paragraph, float paddingTop, float paddingRight, float paddingBottom, float paddingLeft) {
        return borderlessCell(paragraph, paddingTop, paddingRight, paddingBottom, paddingLeft, PdfPCell.ALIGN_LEFT);
    }

    private PdfPCell borderlessCell(Paragraph paragraph, float paddingTop, float paddingRight, float paddingBottom, float paddingLeft, int alignment) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPaddingTop(paddingTop);
        cell.setPaddingRight(paddingRight);
        cell.setPaddingBottom(paddingBottom);
        cell.setPaddingLeft(paddingLeft);
        cell.addElement(paragraph);
        return cell;
    }

    private String buildDocumentNumberTitle(DocumentPreviewDTO preview) {
        String number = safe(preview.getDocumentNumber());
        if (number.isBlank()) {
            return "Numer dokumentu: —";
        }
        return "Numer dokumentu: " + number;
    }

    private String buildSummary(DocumentPreviewDTO preview) {
        return "Klient: " + blankToDash(preview.getCustomerName())
                + " • Data wystawienia: " + blankToDash(preview.getIssueDateLabel())
                + " • Status: " + blankToDash(preview.getStatusLabel());
    }

    private String blankToDash(String value) {
        String safeValue = safe(value);
        return safeValue.isBlank() ? "—" : safeValue;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
