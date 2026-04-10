package com.egen.fitogen.service;

import com.egen.fitogen.dto.DocumentPreviewDTO;
import com.egen.fitogen.dto.DocumentPreviewItemDTO;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DocumentPdfService {

    private static final java.awt.Color GRID_COLOR = new java.awt.Color(85, 95, 105);
    private static final java.awt.Color STRIP_COLOR = new java.awt.Color(217, 217, 217);
    private static final java.awt.Color FOOTER_COLOR = new java.awt.Color(107, 114, 128);
    private static final java.awt.Color TEXT_COLOR = new java.awt.Color(17, 24, 39);

    private static final String[] REGULAR_FONT_CANDIDATES = {
            "C:/Windows/Fonts/segoeui.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf"
    };

    private static final String[] BOLD_FONT_CANDIDATES = {
            "C:/Windows/Fonts/seguisb.ttf",
            "C:/Windows/Fonts/segoeuib.ttf",
            "C:/Windows/Fonts/arialbd.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf",
            "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
    };

    public void export(DocumentPreviewDTO preview, File outputFile) {
        if (preview == null) {
            throw new IllegalArgumentException("Brak danych dokumentu do eksportu PDF.");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Nie wybrano pliku PDF.");
        }

        Document pdf = new Document(PageSize.A4, 36, 36, 28, 28);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFile);
            PdfWriter.getInstance(pdf, out);
            pdf.open();

            Font titleFont = createUnicodeFont(16f, TEXT_COLOR, false);
            Font numberFont = createUnicodeFont(12f, TEXT_COLOR, false);
            Font boxHeaderFont = createUnicodeFont(9.5f, TEXT_COLOR, true);
            Font normalFont = createUnicodeFont(9.5f, TEXT_COLOR, false);
            Font partyNameFont = createUnicodeFont(9.5f, TEXT_COLOR, true);
            Font footerFont = createUnicodeFont(8f, FOOTER_COLOR, false);
            Font cancelledFont = createUnicodeFont(10.5f, TEXT_COLOR, true);

            PdfPTable top = new PdfPTable(new float[]{1.18f, 1.12f});
            top.setWidthPercentage(100);
            top.setSpacingAfter(12f);
            top.addCell(buildLogoPlaceholderCell());
            top.addCell(buildTopMetaCell(preview, boxHeaderFont, normalFont));
            pdf.add(top);

            PdfPTable parties = new PdfPTable(new float[]{1f, 1f});
            parties.setWidthPercentage(100);
            parties.setSpacingAfter(12f);
            parties.addCell(partyCell(
                    "Wystawca:",
                    preview.getIssuerName(),
                    preview.getIssuerAddressLine1(),
                    preview.getIssuerAddressLine2(),
                    preview.getIssuerPhytosanitaryNumber(),
                    boxHeaderFont,
                    partyNameFont,
                    normalFont
            ));
            parties.addCell(partyCell(
                    "Nabywca:",
                    preview.getCustomerName(),
                    preview.getCustomerAddressLine1(),
                    preview.getCustomerAddressLine2(),
                    preview.getCustomerPhytosanitaryNumber(),
                    boxHeaderFont,
                    partyNameFont,
                    normalFont
            ));
            pdf.add(parties);

            Paragraph title = new Paragraph(buildDocumentTitle(preview), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            pdf.add(title);

            Paragraph number = new Paragraph(buildDocumentNumberTitle(preview), numberFont);
            number.setAlignment(Element.ALIGN_CENTER);
            number.setSpacingAfter(12f);
            pdf.add(number);

            pdf.add(alignedLine("Państwowa Inspekcja Ochrony Roślin i Nasiennictwa", normalFont, Element.ALIGN_LEFT, 0f));
            pdf.add(alignedLine("PL / Jakość WE", normalFont, Element.ALIGN_LEFT, 8f));

            if (preview.isCancelled()) {
                Paragraph cancelled = new Paragraph("ANULOWANY", cancelledFont);
                cancelled.setAlignment(Element.ALIGN_CENTER);
                cancelled.setSpacingAfter(8f);
                pdf.add(cancelled);
            }

            PdfPTable items = new PdfPTable(new float[]{0.55f, 3.0f, 1.8f, 1.0f, 1.45f, 0.85f});
            items.setWidthPercentage(100);
            items.setSpacingAfter(10f);
            addHeader(items, "Lp", boxHeaderFont);
            addHeader(items, "Nazwa", boxHeaderFont);
            addHeader(items, "Partia", boxHeaderFont);
            addHeader(items, "Wiek", boxHeaderFont);
            addHeader(items, "Kategoria", boxHeaderFont);
            addHeader(items, "Ilość", boxHeaderFont);

            if (preview.getItems() == null || preview.getItems().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Brak pozycji dokumentu.", normalFont));
                empty.setColspan(6);
                empty.setPadding(8f);
                empty.setBorderColor(GRID_COLOR);
                items.addCell(empty);
            } else {
                for (DocumentPreviewItemDTO item : preview.getItems()) {
                    items.addCell(bodyCell(String.valueOf(item.getLp()), normalFont, PdfPCell.ALIGN_CENTER));
                    items.addCell(bodyCell(safe(item.getPlantName()), normalFont, PdfPCell.ALIGN_LEFT));
                    items.addCell(bodyCell(safe(item.getBatchNumber()), normalFont, PdfPCell.ALIGN_CENTER));
                    items.addCell(bodyCell(safe(item.getBatchAgeLabel()), normalFont, PdfPCell.ALIGN_CENTER));
                    items.addCell(bodyCell(safe(item.getBatchCategoryLabel()), normalFont, PdfPCell.ALIGN_CENTER));
                    items.addCell(bodyCell(String.valueOf(item.getQty()), normalFont, PdfPCell.ALIGN_CENTER));
                }
            }

            PdfPCell summaryLabel = new PdfPCell(new Phrase("Suma:", boxHeaderFont));
            summaryLabel.setColspan(5);
            summaryLabel.setPadding(7f);
            summaryLabel.setBackgroundColor(STRIP_COLOR);
            summaryLabel.setBorderColor(GRID_COLOR);
            summaryLabel.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            items.addCell(summaryLabel);

            PdfPCell summaryValue = new PdfPCell(new Phrase(String.valueOf(preview.getTotalQty()), normalFont));
            summaryValue.setPadding(7f);
            summaryValue.setBorderColor(GRID_COLOR);
            summaryValue.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            items.addCell(summaryValue);
            pdf.add(items);

            if (!safe(preview.getComments()).isBlank()) {
                Paragraph comments = new Paragraph("Uwagi: " + preview.getComments(), normalFont);
                comments.setSpacingAfter(10f);
                pdf.add(comments);
            }

            PdfPTable signatures = new PdfPTable(new float[]{1f, 1f});
            signatures.setWidthPercentage(100);
            signatures.setSpacingBefore(8f);
            signatures.addCell(signatureCell("Odebrał:", "", boxHeaderFont, normalFont));
            signatures.addCell(signatureCell("Wystawił:", blankToDash(preview.getCreatedBy()), boxHeaderFont, normalFont));
            pdf.add(signatures);

            Paragraph footer = new Paragraph("Fito Gen Essentials Powered by eGen Labs: www.egenlabs.eu", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(12f);
            pdf.add(footer);
        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się wyeksportować dokumentu do PDF.", e);
        } finally {
            try {
                if (pdf.isOpen()) {
                    pdf.close();
                }
            } catch (Exception ignored) {
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Paragraph alignedLine(String text, Font font, int alignment, float spacingAfter) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(alignment);
        paragraph.setSpacingAfter(spacingAfter);
        return paragraph;
    }

    private Font createUnicodeFont(float size, java.awt.Color color, boolean bold) {
        try {
            String fontPath = resolveFontPath(bold ? BOLD_FONT_CANDIDATES : REGULAR_FONT_CANDIDATES);
            BaseFont baseFont;
            if (fontPath != null) {
                baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } else {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.NOT_EMBEDDED);
            }
            return new Font(baseFont, size, bold ? Font.BOLD : Font.NORMAL, color);
        } catch (Exception ignored) {
            return new Font(Font.HELVETICA, size, bold ? Font.BOLD : Font.NORMAL, color);
        }
    }

    private String resolveFontPath(String[] candidates) {
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            Path path = Path.of(candidate);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return candidate;
            }
        }
        return null;
    }

    private PdfPCell buildLogoPlaceholderCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        cell.setMinimumHeight(42f);
        return cell;
    }

    private PdfPCell buildTopMetaCell(DocumentPreviewDTO preview, Font labelFont, Font valueFont) {
        PdfPTable meta = new PdfPTable(new float[]{1.35f, 1.05f});
        meta.setWidthPercentage(100);
        addMetaStrip(meta, "Miejsce wystawienia:", blankToDash(preview.getIssuePlaceLabel()), labelFont, valueFont);
        addMetaStrip(meta, "Data wystawienia:", blankToDash(preview.getIssueDateLabel()), labelFont, valueFont);

        PdfPCell wrapper = new PdfPCell(meta);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0f);
        wrapper.setVerticalAlignment(PdfPCell.ALIGN_TOP);
        return wrapper;
    }

    private void addMetaStrip(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(6f);
        labelCell.setFixedHeight(22f);
        labelCell.setNoWrap(true);
        labelCell.setBackgroundColor(STRIP_COLOR);
        labelCell.setBorderColor(GRID_COLOR);
        labelCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(6f);
        valueCell.setFixedHeight(22f);
        valueCell.setNoWrap(true);
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
            Font partyNameFont,
            Font normalFont
    ) {
        PdfPTable content = new PdfPTable(1);
        content.setWidthPercentage(100);

        PdfPCell header = new PdfPCell(new Phrase(section, sectionFont));
        header.setBackgroundColor(STRIP_COLOR);
        header.setBorderColor(GRID_COLOR);
        header.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        header.setPadding(6f);
        content.addCell(header);

        PdfPCell body = new PdfPCell();
        body.setBorderColor(GRID_COLOR);
        body.setPaddingTop(10f);
        body.setPaddingRight(10f);
        body.setPaddingBottom(10f);
        body.setPaddingLeft(10f);
        body.addElement(spacedParagraph(blankToDash(line1), partyNameFont, 6f));
        body.addElement(spacedParagraph(blankToDash(line2), normalFont, 3f));
        body.addElement(spacedParagraph(blankToDash(line3), normalFont, 6f));
        body.addElement(spacedParagraph("Nr fitosanitarny: " + blankToDash(phytosanitaryNumber), normalFont, 0f));
        content.addCell(body);

        PdfPCell wrapper = new PdfPCell(content);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0f);
        wrapper.setVerticalAlignment(PdfPCell.ALIGN_TOP);
        return wrapper;
    }


    private Paragraph spacedParagraph(String text, Font font, float spacingAfter) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingAfter(spacingAfter);
        paragraph.setLeading(11.5f);
        return paragraph;
    }

    private void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setBackgroundColor(STRIP_COLOR);
        cell.setBorderColor(GRID_COLOR);
        table.addCell(cell);
    }

    private PdfPCell bodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(blankToDash(text), font));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        cell.setBorderColor(GRID_COLOR);
        return cell;
    }

    private PdfPCell signatureCell(String headerText, String signerName, Font headerFont, Font signerFont) {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);

        PdfPCell header = new PdfPCell(new Phrase(headerText, headerFont));
        header.setBackgroundColor(STRIP_COLOR);
        header.setBorderColor(GRID_COLOR);
        header.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        header.setPadding(6f);
        box.addCell(header);

        PdfPCell body = new PdfPCell();
        body.setBorderColor(GRID_COLOR);
        body.setFixedHeight(86f);
        body.setPaddingTop(28f);
        body.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        body.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        if (!safe(signerName).isBlank()) {
            Paragraph signer = new Paragraph(signerName, signerFont);
            signer.setAlignment(Element.ALIGN_CENTER);
            body.addElement(signer);
        }
        box.addCell(body);

        PdfPCell wrapper = new PdfPCell(box);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0f);
        wrapper.setVerticalAlignment(PdfPCell.ALIGN_TOP);
        return wrapper;
    }

    private String buildDocumentTitle(DocumentPreviewDTO preview) {
        String type = safe(preview.getDocumentType());
        return type.isBlank() ? "Dokument fitosanitarny" : type;
    }

    private String buildDocumentNumberTitle(DocumentPreviewDTO preview) {
        String number = safe(preview.getDocumentNumber());
        return number.isBlank() ? "nr —" : "nr " + number;
    }

    private String blankToDash(String value) {
        String safeValue = safe(value);
        return safeValue.isBlank() ? "—" : safeValue;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
