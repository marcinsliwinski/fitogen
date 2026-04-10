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

    private static final java.awt.Color GRID_COLOR = new java.awt.Color(85, 95, 105);
    private static final java.awt.Color STRIP_COLOR = new java.awt.Color(217, 217, 217);

    public void export(DocumentPreviewDTO preview, File outputFile) {
        if (preview == null) {
            throw new IllegalArgumentException("Brak danych dokumentu do eksportu PDF.");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Nie wybrano pliku PDF.");
        }

        Document pdf = new Document(PageSize.A4, 40, 40, 32, 32);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFile);
            PdfWriter.getInstance(pdf, out);
            pdf.open();

            Font issuerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17);
            Font numberFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font copyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font boxHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font cancelledFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            PdfPTable top = new PdfPTable(new float[]{1.25f, 0.95f});
            top.setWidthPercentage(100);
            top.setSpacingAfter(10);
            top.addCell(buildTopIssuerCell(preview, issuerFont, normalFont));
            top.addCell(buildTopMetaCell(preview, boxHeaderFont, normalFont));
            pdf.add(top);

            Paragraph subtitle = new Paragraph(buildSummary(preview), smallFont);
            subtitle.setSpacingAfter(10);
            pdf.add(subtitle);

            PdfPTable parties = new PdfPTable(new float[]{1f, 1f});
            parties.setWidthPercentage(100);
            parties.setSpacingAfter(12);
            parties.addCell(partyCell(
                    "Wystawca:",
                    preview.getIssuerName(),
                    preview.getIssuerAddressLine1(),
                    preview.getIssuerAddressLine2(),
                    preview.getIssuerPhytosanitaryNumber(),
                    boxHeaderFont,
                    normalFont
            ));
            parties.addCell(partyCell(
                    "Nabywca:",
                    preview.getCustomerName(),
                    preview.getCustomerAddressLine1(),
                    preview.getCustomerAddressLine2(),
                    preview.getCustomerPhytosanitaryNumber(),
                    boxHeaderFont,
                    normalFont
            ));
            pdf.add(parties);

            PdfPTable heading = new PdfPTable(new float[]{1f, 1.4f, 1f});
            heading.setWidthPercentage(100);
            heading.setSpacingAfter(12);
            heading.addCell(borderlessCell(new Phrase("", normalFont), PdfPCell.ALIGN_LEFT));
            heading.addCell(borderlessCell(new Phrase(buildDocumentTitle(preview), titleFont), PdfPCell.ALIGN_CENTER));
            heading.addCell(borderlessCell(new Phrase("ORYGINAŁ    KOPIA", copyFont), PdfPCell.ALIGN_RIGHT));
            pdf.add(heading);

            Paragraph number = new Paragraph(buildDocumentNumberTitle(preview), numberFont);
            number.setAlignment(Paragraph.ALIGN_CENTER);
            number.setSpacingAfter(10);
            pdf.add(number);

            if (preview.isCancelled()) {
                Paragraph cancelled = new Paragraph("ANULOWANY", cancelledFont);
                cancelled.setAlignment(Paragraph.ALIGN_CENTER);
                cancelled.setSpacingAfter(10);
                pdf.add(cancelled);
            }

            PdfPTable items = new PdfPTable(new float[]{0.55f, 3.5f, 2.1f, 0.85f, 1.15f});
            items.setWidthPercentage(100);
            items.setSpacingAfter(10);
            addHeader(items, "Lp", boxHeaderFont);
            addHeader(items, "Nazwa", boxHeaderFont);
            addHeader(items, "Partia", boxHeaderFont);
            addHeader(items, "Ilość", boxHeaderFont);
            addHeader(items, "Paszport", boxHeaderFont);

            if (preview.getItems() == null || preview.getItems().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Brak pozycji dokumentu.", normalFont));
                empty.setColspan(5);
                empty.setPadding(8);
                empty.setBorderColor(GRID_COLOR);
                items.addCell(empty);
            } else {
                for (DocumentPreviewItemDTO item : preview.getItems()) {
                    items.addCell(bodyCell(String.valueOf(item.getLp()), normalFont, PdfPCell.ALIGN_CENTER));
                    items.addCell(bodyCell(safe(item.getPlantName()), normalFont, PdfPCell.ALIGN_LEFT));
                    items.addCell(bodyCell(safe(item.getBatchNumber()), normalFont, PdfPCell.ALIGN_LEFT));
                    items.addCell(bodyCell(String.valueOf(item.getQty()), normalFont, PdfPCell.ALIGN_CENTER));
                    items.addCell(bodyCell(safe(item.getPassportLabel()), normalFont, PdfPCell.ALIGN_CENTER));
                }
            }
            pdf.add(items);

            PdfPTable totalsWrapper = new PdfPTable(new float[]{1.2f, 1f});
            totalsWrapper.setWidthPercentage(100);
            totalsWrapper.setSpacingAfter(10);
            totalsWrapper.addCell(borderlessCell(new Phrase("", normalFont), PdfPCell.ALIGN_LEFT));
            totalsWrapper.addCell(buildTotalsCell(preview, boxHeaderFont, normalFont));
            pdf.add(totalsWrapper);

            if (!safe(preview.getComments()).isBlank()) {
                Paragraph comments = new Paragraph("Uwagi: " + preview.getComments(), normalFont);
                comments.setSpacingAfter(12);
                pdf.add(comments);
            }

            PdfPTable signatures = new PdfPTable(new float[]{1f, 1f});
            signatures.setWidthPercentage(100);
            signatures.setSpacingBefore(8);
            signatures.addCell(signatureCell(
                    "Odebrał:",
                    "",
                    "Podpis osoby upoważnionej do odbioru dokumentu",
                    boxHeaderFont,
                    normalFont,
                    smallFont
            ));
            signatures.addCell(signatureCell(
                    "Wystawił:",
                    blankToDash(preview.getCreatedBy()),
                    "Podpis osoby upoważnionej do wystawienia dokumentu",
                    boxHeaderFont,
                    normalFont,
                    smallFont
            ));
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

    private PdfPCell buildTopIssuerCell(DocumentPreviewDTO preview, Font issuerFont, Font normalFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        cell.addElement(new Paragraph(blankToDash(preview.getIssuerName()), issuerFont));
        cell.addElement(new Paragraph(blankToDash(preview.getIssuerAddressLine1()), normalFont));
        cell.addElement(new Paragraph(blankToDash(preview.getIssuerAddressLine2()), normalFont));
        cell.addElement(new Paragraph("Nr fitosanitarny: " + blankToDash(preview.getIssuerPhytosanitaryNumber()), normalFont));
        return cell;
    }

    private PdfPCell buildTopMetaCell(DocumentPreviewDTO preview, Font labelFont, Font valueFont) {
        PdfPTable meta = new PdfPTable(new float[]{1.6f, 1f});
        meta.setWidthPercentage(100);
        addMetaStrip(meta, "Miejsce wystawienia:", blankToDash(preview.getIssuePlaceLabel()), labelFont, valueFont);
        addMetaStrip(meta, "Data wystawienia:", blankToDash(preview.getIssueDateLabel()), labelFont, valueFont);
        addMetaStrip(meta, "Status:", blankToDash(preview.getStatusLabel()), labelFont, valueFont);
        addMetaStrip(meta, "Łączna ilość:", preview.getTotalQty() + "", labelFont, valueFont);

        PdfPCell wrapper = new PdfPCell(meta);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0);
        return wrapper;
    }

    private void addMetaStrip(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(STRIP_COLOR);
        labelCell.setBorderColor(GRID_COLOR);
        labelCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(6);
        valueCell.setBorderColor(GRID_COLOR);
        valueCell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
        table.addCell(valueCell);
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
        PdfPTable content = new PdfPTable(1);
        content.setWidthPercentage(100);

        PdfPCell header = new PdfPCell(new Phrase(section, sectionFont));
        header.setBackgroundColor(STRIP_COLOR);
        header.setBorderColor(GRID_COLOR);
        header.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        header.setPadding(6);
        content.addCell(header);

        PdfPCell body = new PdfPCell();
        body.setBorderColor(GRID_COLOR);
        body.setPadding(8);
        body.addElement(new Paragraph(blankToDash(line1), normalFont));
        body.addElement(new Paragraph(blankToDash(line2), normalFont));
        body.addElement(new Paragraph(blankToDash(line3), normalFont));
        body.addElement(new Paragraph("Nr fitosanitarny: " + blankToDash(phytosanitaryNumber), normalFont));
        content.addCell(body);

        PdfPCell wrapper = new PdfPCell(content);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0);
        return wrapper;
    }

    private PdfPCell buildTotalsCell(DocumentPreviewDTO preview, Font labelFont, Font valueFont) {
        PdfPTable totals = new PdfPTable(new float[]{1.6f, 0.85f});
        totals.setWidthPercentage(100);
        addTotalRow(totals, "Liczba pozycji", String.valueOf(preview.getItems() == null ? 0 : preview.getItems().size()), labelFont, valueFont, false);
        addTotalRow(totals, "Razem do wydania", preview.getTotalQty() + " szt.", labelFont, valueFont, true);

        PdfPCell wrapper = new PdfPCell(totals);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0);
        return wrapper;
    }

    private void addTotalRow(PdfPTable totals, String label, String value, Font labelFont, Font valueFont, boolean strong) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(STRIP_COLOR);
        labelCell.setBorderColor(GRID_COLOR);
        labelCell.setPadding(7);
        totals.addCell(labelCell);

        Font effectiveValueFont = strong ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, valueFont.getSize()) : valueFont;
        PdfPCell valueCell = new PdfPCell(new Phrase(value, effectiveValueFont));
        valueCell.setBorderColor(GRID_COLOR);
        valueCell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
        valueCell.setPadding(7);
        totals.addCell(valueCell);
    }

    private void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setBackgroundColor(STRIP_COLOR);
        cell.setBorderColor(GRID_COLOR);
        table.addCell(cell);
    }

    private PdfPCell bodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(blankToDash(text), font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        cell.setBorderColor(GRID_COLOR);
        return cell;
    }

    private PdfPCell signatureCell(
            String headerText,
            String signerName,
            String footerText,
            Font headerFont,
            Font signerFont,
            Font footerFont
    ) {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);

        PdfPCell header = new PdfPCell(new Phrase(headerText, headerFont));
        header.setBackgroundColor(STRIP_COLOR);
        header.setBorderColor(GRID_COLOR);
        header.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        header.setPadding(6);
        box.addCell(header);

        PdfPCell body = new PdfPCell();
        body.setBorderColor(GRID_COLOR);
        body.setFixedHeight(86f);
        body.setPaddingTop(28f);
        body.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        body.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        if (!safe(signerName).isBlank()) {
            Paragraph signer = new Paragraph(signerName, signerFont);
            signer.setAlignment(Paragraph.ALIGN_CENTER);
            body.addElement(signer);
        }
        box.addCell(body);

        PdfPCell footer = new PdfPCell(new Phrase(footerText, footerFont));
        footer.setBorder(Rectangle.NO_BORDER);
        footer.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        footer.setPaddingTop(6);
        box.addCell(footer);

        PdfPCell wrapper = new PdfPCell(box);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0);
        return wrapper;
    }

    private PdfPCell borderlessCell(Phrase phrase, int alignment) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(0);
        return cell;
    }

    private String buildDocumentTitle(DocumentPreviewDTO preview) {
        String type = safe(preview.getDocumentType());
        return type.isBlank() ? "Dokument fitosanitarny" : type;
    }

    private String buildDocumentNumberTitle(DocumentPreviewDTO preview) {
        String number = safe(preview.getDocumentNumber());
        return number.isBlank() ? "nr —" : "nr " + number;
    }

    private String buildSummary(DocumentPreviewDTO preview) {
        return "Podgląd przygotowany do druku i PDF • Miejsce wystawienia: " + blankToDash(preview.getIssuePlaceLabel())
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
