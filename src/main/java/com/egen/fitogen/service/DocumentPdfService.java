package com.egen.fitogen.service;

import com.egen.fitogen.dto.DocumentPreviewDTO;
import com.egen.fitogen.dto.DocumentPreviewItemDTO;
import com.egen.fitogen.dto.PassportPreviewDTO;
import com.egen.fitogen.util.EuFlagRenderer;
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
    private static final java.awt.Color EU_BLUE = new java.awt.Color(0, 51, 153);
    private static final java.awt.Color EU_YELLOW = new java.awt.Color(255, 204, 0);
    private static final java.awt.Color PASSPORT_PANEL = new java.awt.Color(245, 248, 252);

    private static final String[] REGULAR_FONT_CANDIDATES = {
            "C:/Windows/Fonts/segoeui.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf"
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

            Font titleFont = createUnicodeFont(16f, Font.BOLD, TEXT_COLOR);
            Font numberFont = createUnicodeFont(12f, Font.BOLD, TEXT_COLOR);
            Font boxHeaderFont = createUnicodeFont(9.5f, Font.BOLD, TEXT_COLOR);
            Font normalFont = createUnicodeFont(9.5f, Font.NORMAL, TEXT_COLOR);
            Font emphasizedFont = createUnicodeFont(9.5f, Font.BOLD, TEXT_COLOR);
            Font footerFont = createUnicodeFont(8f, Font.NORMAL, FOOTER_COLOR);
            Font cancelledFont = createUnicodeFont(10.5f, Font.BOLD, TEXT_COLOR);

            PdfPTable top = new PdfPTable(new float[]{0.9f, 1.22f});
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
                    normalFont,
                    emphasizedFont
            ));
            parties.addCell(partyCell(
                    "Nabywca:",
                    preview.getCustomerName(),
                    preview.getCustomerAddressLine1(),
                    preview.getCustomerAddressLine2(),
                    preview.getCustomerPhytosanitaryNumber(),
                    boxHeaderFont,
                    normalFont,
                    emphasizedFont
            ));
            pdf.add(parties);

            Paragraph title = new Paragraph(buildDocumentTitle(preview), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(3f);
            pdf.add(title);

            Paragraph number = new Paragraph(buildDocumentNumberTitle(preview), numberFont);
            number.setAlignment(Element.ALIGN_CENTER);
            number.setSpacingAfter(10f);
            pdf.add(number);

            pdf.add(alignedLine("Państwowa Inspekcja Ochrony Roślin i Nasiennictwa", normalFont, Element.ALIGN_LEFT, 0f));
            pdf.add(alignedLine("PL / Jakość WE", normalFont, Element.ALIGN_LEFT, 8f));

            if (preview.isCancelled()) {
                Paragraph cancelled = new Paragraph("ANULOWANY", cancelledFont);
                cancelled.setAlignment(Element.ALIGN_CENTER);
                cancelled.setSpacingAfter(8f);
                pdf.add(cancelled);
            }

            PdfPTable items = new PdfPTable(new float[]{0.5f, 4.4f, 1.55f, 0.65f, 1.25f, 0.8f});
            items.setWidthPercentage(100);
            items.setSpacingAfter(10f);
            addHeader(items, "Lp", boxHeaderFont);
            addHeader(items, "Nazwa rośliny (Gatunek, Podkładka, Odmiana)", boxHeaderFont);
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
            summaryValue.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
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

            addPassportPages(pdf, preview, titleFont, numberFont, boxHeaderFont, normalFont, emphasizedFont, footerFont);
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


    private void addPassportPages(Document pdf, DocumentPreviewDTO preview, Font titleFont, Font numberFont, Font boxHeaderFont, Font normalFont, Font emphasizedFont, Font footerFont) throws Exception {
        if (preview == null || !preview.isPrintPassports() || preview.getPassports() == null || preview.getPassports().isEmpty()) {
            return;
        }

        Font passportHeaderFont = createUnicodeFont(11f, Font.BOLD, TEXT_COLOR);
        Font passportSubHeaderFont = createUnicodeFont(9f, Font.BOLD, TEXT_COLOR);
        Font passportEppoFont = createUnicodeFont(8.5f, Font.BOLD, TEXT_COLOR);
        Font passportCodeFont = createUnicodeFont(11f, Font.BOLD, TEXT_COLOR);
        Font passportValueFont = createUnicodeFont(11f, Font.NORMAL, TEXT_COLOR);
        Font passportMetaFont = createUnicodeFont(10f, Font.NORMAL, TEXT_COLOR);
        Font passportMetaLabelFont = createUnicodeFont(10f, Font.BOLD, TEXT_COLOR);

        final float passportWidth = cmToPt(10f);
        final float passportHeight = cmToPt(6f);
        final int passportsPerPage = 2;

        boolean firstPassportPage = true;
        int index = 0;
        while (index < preview.getPassports().size()) {
            if (firstPassportPage) {
                pdf.newPage();
                firstPassportPage = false;
            } else {
                pdf.newPage();
            }

            for (int slot = 0; slot < passportsPerPage && index < preview.getPassports().size(); slot++, index++) {
                PassportPreviewDTO passport = preview.getPassports().get(index);
                Paragraph metaHeader = new Paragraph(buildPassportMetaHeader(passport), passportMetaFont);
                metaHeader.setSpacingBefore(slot == 0 ? 6f : 14f);
                metaHeader.setSpacingAfter(6f);
                pdf.add(metaHeader);

                PdfPTable card = buildPassportCard(passport, passportWidth, passportHeight, passportHeaderFont, passportSubHeaderFont, passportEppoFont, passportCodeFont, passportValueFont);
                pdf.add(card);
            }
        }
    }

    private PdfPTable buildPassportCard(PassportPreviewDTO passport,
                                        float passportWidth,
                                        float passportHeight,
                                        Font headerFont,
                                        Font subHeaderFont,
                                        Font eppoFont,
                                        Font codeFont,
                                        Font valueFont) throws Exception {
        PdfPTable outer = new PdfPTable(1);
        outer.setTotalWidth(passportWidth);
        outer.setLockedWidth(true);
        outer.setHorizontalAlignment(Element.ALIGN_CENTER);
        outer.setSpacingAfter(8f);

        PdfPTable card = new PdfPTable(new float[]{2.4f, 4.6f});
        card.setWidthPercentage(100);

        PdfPCell flagCell = new PdfPCell();
        flagCell.setPadding(6f);
        flagCell.setFixedHeight(passportHeight * 0.42f);
        flagCell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        flagCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        flagCell.setBorderColor(GRID_COLOR);
        flagCell.setBorderWidthLeft(1f);
        flagCell.setBorderWidthTop(1f);
        flagCell.setBorderWidthRight(0f);
        flagCell.setBorderWidthBottom(0f);
        com.lowagie.text.Image flagImage = com.lowagie.text.Image.getInstance(EuFlagRenderer.createPngBytes(560, 370));
        flagImage.scaleToFit(cmToPt(4.0f), cmToPt(1.85f));
        flagCell.addElement(flagImage);
        card.addCell(flagCell);

        PdfPCell topTextCell = new PdfPCell();
        topTextCell.setPadding(5f);
        topTextCell.setFixedHeight(passportHeight * 0.42f);
        topTextCell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        topTextCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        topTextCell.setBorderColor(GRID_COLOR);
        topTextCell.setBorderWidthLeft(0f);
        topTextCell.setBorderWidthTop(1f);
        topTextCell.setBorderWidthRight(1f);
        topTextCell.setBorderWidthBottom(0f);
        Paragraph line1 = new Paragraph("Paszport roślin / Plant passport", headerFont);
        line1.setAlignment(Element.ALIGN_CENTER);
        line1.setLeading(12f);
        line1.setSpacingAfter(passport.isProtectedZone() ? 2f : 0f);
        topTextCell.addElement(line1);
        if (passport.isProtectedZone()) {
            Paragraph line2 = new Paragraph("Strefa chroniona / ZP", subHeaderFont);
            line2.setAlignment(Element.ALIGN_CENTER);
            line2.setLeading(10f);
            line2.setSpacingAfter(1f);
            topTextCell.addElement(line2);
            Paragraph line3 = new Paragraph(blankToDash(passport.getProtectedZoneCode()), eppoFont);
            line3.setAlignment(Element.ALIGN_CENTER);
            line3.setLeading(9f);
            topTextCell.addElement(line3);
        }
        card.addCell(topTextCell);

        PdfPCell bottomCell = new PdfPCell();
        bottomCell.setColspan(2);
        bottomCell.setBorderColor(GRID_COLOR);
        bottomCell.setBorderWidthLeft(1f);
        bottomCell.setBorderWidthTop(0f);
        bottomCell.setBorderWidthRight(1f);
        bottomCell.setBorderWidthBottom(1f);
        bottomCell.setPadding(8f);
        bottomCell.setFixedHeight(passportHeight * 0.58f);

        PdfPTable details = new PdfPTable(new float[]{0.38f, 3.62f});
        details.setWidthPercentage(100);
        addPassportDetailRow(details, "A", blankToDash(passport.getBotanicalName()), codeFont, valueFont);
        addPassportDetailRow(details, "B", blankToDash(passport.getOperatorNumber()), codeFont, valueFont);
        addPassportDetailRow(details, "C", blankToDash(passport.getTraceabilityCode()), codeFont, valueFont);
        addPassportDetailRow(details, "D", blankToDash(passport.getOriginCountryCode()), codeFont, valueFont);

        PdfPTable detailsWrap = new PdfPTable(1);
        detailsWrap.setWidthPercentage(100);
        PdfPCell detailsInnerCell = new PdfPCell();
        detailsInnerCell.setBorder(Rectangle.NO_BORDER);
        detailsInnerCell.setFixedHeight(passportHeight * 0.58f - 16f);
        detailsInnerCell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        detailsInnerCell.addElement(details);
        detailsWrap.addCell(detailsInnerCell);
        bottomCell.addElement(detailsWrap);

        card.addCell(bottomCell);

        PdfPCell wrapper = new PdfPCell(card);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        outer.addCell(wrapper);
        return outer;
    }

    private String buildPassportMetaHeader(PassportPreviewDTO passport) {
        return "Paszport roślin do dokumentu nr: " + blankToDash(passport.getDocumentNumber())
                + " / Pozycja nr: " + passport.getItemNo()
                + " / Roślina: " + blankToDash(passport.getPlantName())
                + " / Ilość: " + blankToDash(passport.getQuantityLabel())
                + " / Kategoria: " + blankToDash(passport.getCategoryLabel());
    }

    private void addPassportDetailRow(PdfPTable table, String code, String value, Font codeFont, Font valueFont) {
        PdfPCell codeCell = new PdfPCell(new Phrase(code, codeFont));
        codeCell.setPadding(3f);
        codeCell.setBorder(Rectangle.NO_BORDER);
        codeCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        codeCell.setVerticalAlignment(PdfPCell.ALIGN_TOP);
        table.addCell(codeCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(3f);
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setVerticalAlignment(PdfPCell.ALIGN_TOP);
        table.addCell(valueCell);
    }

    private float cmToPt(float centimeters) {
        return centimeters * 28.3464567f;
    }

    private Paragraph alignedLine(String text, Font font, int alignment, float spacingAfter) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(alignment);
        paragraph.setSpacingAfter(spacingAfter);
        return paragraph;
    }

    private Font createUnicodeFont(float size, int style, java.awt.Color color) {
        try {
            String fontPath = resolveFontPath(REGULAR_FONT_CANDIDATES);
            BaseFont baseFont;
            if (fontPath != null) {
                baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } else {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.NOT_EMBEDDED);
            }
            return new Font(baseFont, size, style, color);
        } catch (Exception ignored) {
            return new Font(Font.HELVETICA, size, style, color);
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
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        return cell;
    }

    private PdfPCell buildTopMetaCell(DocumentPreviewDTO preview, Font labelFont, Font valueFont) {
        PdfPTable meta = new PdfPTable(new float[]{1f, 1f});
        meta.setSplitRows(false);
        meta.setKeepTogether(true);
        meta.setWidthPercentage(100);
        meta.getDefaultCell().setPadding(0f);

        addMetaStrip(meta, "Miejsce wystawienia:", blankToDash(preview.getIssuePlaceLabel()), labelFont, valueFont);
        addMetaStrip(meta, "Data wystawienia:", blankToDash(preview.getIssueDateLabel()), labelFont, valueFont);

        PdfPCell wrapper = new PdfPCell(meta);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0f);
        wrapper.setUseAscender(true);
        wrapper.setUseDescender(true);
        return wrapper;
    }

    private void addMetaStrip(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(4.5f);
        labelCell.setFixedHeight(26f);
        labelCell.setNoWrap(true);
        labelCell.setBackgroundColor(STRIP_COLOR);
        labelCell.setBorderColor(GRID_COLOR);
        labelCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        labelCell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        labelCell.setUseAscender(true);
        labelCell.setUseDescender(true);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(4.5f);
        valueCell.setFixedHeight(26f);
        valueCell.setNoWrap(true);
        valueCell.setBorderColor(GRID_COLOR);
        valueCell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        valueCell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        valueCell.setUseAscender(true);
        valueCell.setUseDescender(true);
        table.addCell(valueCell);
    }

    private PdfPCell partyCell(
            String section,
            String line1,
            String line2,
            String line3,
            String phytosanitaryNumber,
            Font sectionFont,
            Font normalFont,
            Font emphasizedFont
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
        body.setPadding(9f);
        addPartyLine(body, line1, emphasizedFont, false);
        addPartyLine(body, line2, normalFont, false);
        addPartyLine(body, line3, normalFont, false);
        addPartyLine(body, "Nr fitosanitarny: " + blankToDash(phytosanitaryNumber), normalFont, true);
        content.addCell(body);

        PdfPCell wrapper = new PdfPCell(content);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0f);
        return wrapper;
    }


    private void addPartyLine(PdfPCell body, String text, Font font, boolean alwaysShow) {
        String safeText = safe(text);
        if (!alwaysShow && safeText.isBlank()) {
            return;
        }

        String[] lines = (alwaysShow ? text : safeText).replace("", "").split("\n");
        for (String line : lines) {
            String normalized = alwaysShow ? line : safe(line);
            if (!alwaysShow && normalized.isBlank()) {
                continue;
            }
            Paragraph paragraph = new Paragraph(normalized, font);
            paragraph.setLeading(12.5f);
            paragraph.setSpacingAfter(1.5f);
            body.addElement(paragraph);
        }
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
