/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.markets.contractMarket;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.gui.dialog.markets.contractMarket.ContractReadout.Row;
import mekhq.gui.dialog.markets.contractMarket.ContractReadout.Section;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Writes the offers on a contract market board to a PDF, so players can read them over away from MekHQ - printed at
 * the table, say, or passed around before a session.
 *
 * <p>The document opens with a title block naming the campaign, date and board, then gives each offer as its dossier
 * reads: the name, employer and status line, the assessment and badges, the briefing, and the Terms, Intel and Money
 * sections. Every page carries a page count. Text is set in the bundled Noto Sans, which covers far more of the names
 * MekHQ generates than the standard PDF fonts; if it cannot be found, Helvetica is used, and any character it cannot
 * show is replaced.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractPdfExporter {
    private static final MMLogger LOGGER = MMLogger.create(ContractPdfExporter.class);
    private static final String RESOURCE_BUNDLE = ChaosContractMarketDialog.RESOURCE_BUNDLE;

    private static final String FONT_FOLDER = MHQConstants.FONT_DIRECTORY + "Noto Sans/";

    private static final float MARGIN = 54;
    private static final float TITLE_SIZE = 20;
    private static final float OFFER_TITLE_SIZE = 14;
    private static final float HEADING_SIZE = 10.5f;
    private static final float BODY_SIZE = 9.5f;
    private static final float SMALL_SIZE = 8;
    private static final float LEADING = 1.35f;
    private static final float LABEL_COLUMN = 0.42f;
    /** Room an offer's opening lines need; an offer that would start with less left on the page starts a new one. */
    private static final float OFFER_MINIMUM_SPACE = 150;

    private static final Color INK = new Color(0x1E, 0x24, 0x2B);
    private static final Color MUTED = new Color(0x5C, 0x66, 0x70);
    private static final Color ACCENT = new Color(0x0E, 0x6E, 0x80);
    private static final Color RULE = new Color(0xC8, 0xD0, 0xD6);
    private static final Color HIGHLIGHT = new Color(0x8A, 0x5A, 0x00);

    private final PDDocument document;
    private final PDFont regular;
    private final PDFont bold;
    private final PDFont italic;
    private final PDRectangle pageSize = PDRectangle.LETTER;
    private final List<PDPage> pages = new ArrayList<>();

    private PDPageContentStream stream;
    private float y;

    private ContractPdfExporter(PDDocument document) {
        this.document = document;
        this.regular = loadFont(document, "NotoSans-Regular.ttf", Standard14Fonts.FontName.HELVETICA);
        this.bold = loadFont(document, "NotoSans-Bold.ttf", Standard14Fonts.FontName.HELVETICA_BOLD);
        this.italic = loadFont(document, "NotoSans-Italic.ttf", Standard14Fonts.FontName.HELVETICA_OBLIQUE);
    }

    /**
     * Writes the offers to a PDF file.
     *
     * @param outputFile   the destination file
     * @param campaignName the campaign's name, for the title block
     * @param date         the campaign date, already formatted, for the title block
     * @param boardName    the market board the offers come from, for the title block
     * @param offers       the offers, in the order they should appear
     *
     * @throws IOException if the file cannot be written
     * @author Illiani
     * @since 0.51.01
     */
    public static void export(File outputFile, String campaignName, String date, String boardName,
          List<ContractReadout> offers) throws IOException {
        try (PDDocument document = new PDDocument()) {
            write(document, campaignName, date, boardName, offers);
            document.save(outputFile);
        }
    }

    /**
     * Writes the offers as a PDF to a stream.
     *
     * @see #export(File, String, String, String, List)
     */
    public static void export(OutputStream outputStream, String campaignName, String date, String boardName,
          List<ContractReadout> offers) throws IOException {
        try (PDDocument document = new PDDocument()) {
            write(document, campaignName, date, boardName, offers);
            document.save(outputStream);
        }
    }

    private static void write(PDDocument document, String campaignName, String date, String boardName,
          List<ContractReadout> offers) throws IOException {
        ContractPdfExporter exporter = new ContractPdfExporter(document);
        try {
            exporter.writeTitle(campaignName, date, boardName, offers.size());
            for (ContractReadout offer : offers) {
                exporter.writeOffer(offer);
            }
        } finally {
            exporter.closeStream();
        }
        exporter.writePageNumbers();
    }

    // region Content

    private void writeTitle(String campaignName, String date, String boardName, int offerCount) throws IOException {
        newPage();
        line(getTextAt(RESOURCE_BUNDLE, "pdf.contractMarket.title").toUpperCase(Locale.ROOT), bold, TITLE_SIZE,
              ACCENT, MARGIN);
        line(String.join(ContractReadout.separator(), campaignName, date, boardName), regular, BODY_SIZE + 1, INK,
              MARGIN);
        line(getFormattedTextAt(RESOURCE_BUNDLE, "pdf.contractMarket.offerCount", offerCount), regular, BODY_SIZE,
              MUTED, MARGIN);
        y -= 6;
        rule(ACCENT, 1.2f);
        y -= 10;
    }

    private void writeOffer(ContractReadout offer) throws IOException {
        ensureSpace(OFFER_MINIMUM_SPACE);

        line(offer.employerLine().toUpperCase(Locale.ROOT), bold, SMALL_SIZE, MUTED, MARGIN);
        wrapped(offer.title().toUpperCase(Locale.ROOT), bold, OFFER_TITLE_SIZE, ACCENT, MARGIN, contentWidth());
        wrapped(offer.statusLine(), regular, BODY_SIZE, INK, MARGIN, contentWidth());

        List<String> markers = new ArrayList<>();
        markers.add(offer.assessment());
        markers.addAll(offer.badges());
        wrapped(String.join(ContractReadout.separator(), markers), bold, BODY_SIZE, HIGHLIGHT, MARGIN, contentWidth());
        y -= 6;

        if (!offer.briefing().isBlank()) {
            if (offer.speaker() != null) {
                line(offer.speaker(), bold, BODY_SIZE, INK, MARGIN + 10);
            }
            int startPage = pages.size();
            float top = y;
            wrapped(offer.briefing(), italic, BODY_SIZE, MUTED, MARGIN + 10, contentWidth() - 10);
            // The cyan rule down the left of the contact's words, as in the dossier. A briefing broken across pages
            // goes without one rather than drawing it down the wrong page.
            if (pages.size() == startPage) {
                bar(MARGIN + 2, top - BODY_SIZE * 0.3f, y - BODY_SIZE * 0.3f);
            }
            y -= 6;
        }

        for (Section section : offer.sections()) {
            ensureSpace(HEADING_SIZE * LEADING * 3);
            y -= 4;
            line(section.heading().toUpperCase(Locale.ROOT), bold, HEADING_SIZE, ACCENT, MARGIN);
            rule(RULE, 0.6f);
            y -= 3;
            for (Row row : section.rows()) {
                row(row);
            }
            for (String note : section.notes()) {
                wrapped(note, italic, BODY_SIZE, MUTED, MARGIN, contentWidth());
            }
        }

        y -= 2;
        wrapped(offer.caveat(), italic, SMALL_SIZE, MUTED, MARGIN, contentWidth());
        y -= 10;
        rule(RULE, 0.8f);
        y -= 14;
    }

    // endregion Content

    // region Layout

    private float contentWidth() {
        return pageSize.getWidth() - (2 * MARGIN);
    }

    private void newPage() throws IOException {
        closeStream();
        PDPage page = new PDPage(pageSize);
        document.addPage(page);
        pages.add(page);
        stream = new PDPageContentStream(document, page);
        y = pageSize.getHeight() - MARGIN;
    }

    private void closeStream() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }

    /** Starts a new page if fewer than {@code height} points are left above the bottom margin. */
    private void ensureSpace(float height) throws IOException {
        if ((stream == null) || (y - height < MARGIN)) {
            newPage();
        }
    }

    /** Sets a single line of text at {@code x}, moving down a line first. */
    private void line(String text, PDFont font, float size, Color color, float x) throws IOException {
        ensureSpace(size * LEADING);
        y -= size * LEADING;
        draw(text, font, size, color, x, y);
    }

    /** Sets text wrapped to {@code width}, keeping its line breaks. */
    private void wrapped(String text, PDFont font, float size, Color color, float x, float width) throws IOException {
        for (String wrappedLine : wrap(text, font, size, width)) {
            line(wrappedLine, font, size, color, x);
        }
    }

    /** A label/value row: the label down the left column and the value, wrapped, down the right. */
    private void row(Row row) throws IOException {
        float labelWidth = contentWidth() * LABEL_COLUMN;
        float valueX = MARGIN + labelWidth;
        PDFont valueFont = row.highlight() ? bold : regular;
        Color valueColor = row.highlight() ? HIGHLIGHT : INK;

        List<String> labelLines = wrap(row.label(), regular, BODY_SIZE, labelWidth - 8);
        List<String> valueLines = wrap(row.value(), valueFont, BODY_SIZE, contentWidth() - labelWidth);
        int lineCount = Math.max(labelLines.size(), valueLines.size());
        ensureSpace(lineCount * BODY_SIZE * LEADING);
        for (int i = 0; i < lineCount; i++) {
            y -= BODY_SIZE * LEADING;
            if (i < labelLines.size()) {
                draw(labelLines.get(i), regular, BODY_SIZE, MUTED, MARGIN, y);
            }
            if (i < valueLines.size()) {
                draw(valueLines.get(i), valueFont, BODY_SIZE, valueColor, valueX, y);
            }
        }
    }

    private void rule(Color color, float thickness) throws IOException {
        ensureSpace(thickness + 2);
        y -= 3;
        stream.setStrokingColor(color);
        stream.setLineWidth(thickness);
        stream.moveTo(MARGIN, y);
        stream.lineTo(pageSize.getWidth() - MARGIN, y);
        stream.stroke();
    }

    private void bar(float x, float top, float bottom) throws IOException {
        stream.setStrokingColor(ACCENT);
        stream.setLineWidth(1.5f);
        stream.moveTo(x, top);
        stream.lineTo(x, bottom);
        stream.stroke();
    }

    private void draw(String text, PDFont font, float size, Color color, float x, float baseline)
          throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.setNonStrokingColor(color);
        stream.newLineAtOffset(x, baseline);
        stream.showText(printable(text, font));
        stream.endText();
    }

    private void writePageNumbers() throws IOException {
        int total = pages.size();
        for (int i = 0; i < total; i++) {
            try (PDPageContentStream footer = new PDPageContentStream(document, pages.get(i), AppendMode.APPEND,
                  true, true)) {
                String text = printable(getFormattedTextAt(RESOURCE_BUNDLE, "pdf.contractMarket.page", i + 1, total),
                      regular);
                float width = textWidth(text, regular, SMALL_SIZE);
                footer.beginText();
                footer.setFont(regular, SMALL_SIZE);
                footer.setNonStrokingColor(MUTED);
                footer.newLineAtOffset(pageSize.getWidth() - MARGIN - width, MARGIN / 2);
                footer.showText(text);
                footer.endText();
            }
        }
    }

    // endregion Layout

    // region Text

    /**
     * Breaks text into lines no wider than {@code width}, at word boundaries where it can and mid-word where a single
     * word is too long. Line breaks in the text are kept.
     */
    List<String> wrap(String text, PDFont font, float size, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            String printable = printable(paragraph, font);
            if (printable.isBlank()) {
                lines.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : printable.split(" +")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (textWidth(candidate, font, size) <= width) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                // A word wider than the line on its own is broken wherever it overflows.
                String remainder = word;
                while (textWidth(remainder, font, size) > width && remainder.length() > 1) {
                    int fit = remainder.length() - 1;
                    while (fit > 1 && textWidth(remainder.substring(0, fit), font, size) > width) {
                        fit--;
                    }
                    lines.add(remainder.substring(0, fit));
                    remainder = remainder.substring(fit);
                }
                current.append(remainder);
            }
            lines.add(current.toString());
        }
        return lines;
    }

    private static float textWidth(String text, PDFont font, float size) throws IOException {
        return font.getStringWidth(text) / 1000 * size;
    }

    /**
     * Makes text safe to show in the given font: control characters become spaces and any character the font has no
     * glyph for becomes a question mark, as PDFBox refuses to set either.
     */
    static String printable(String text, PDFont font) {
        String cleaned = text.replaceAll("\\p{Cntrl}", " ");
        try {
            font.encode(cleaned);
            return cleaned;
        } catch (IOException | IllegalArgumentException ignored) {
            // Some character in the text is not in the font; find it below.
        }

        StringBuilder result = new StringBuilder();
        cleaned.codePoints().forEach(codePoint -> {
            String character = new String(Character.toChars(codePoint));
            try {
                font.encode(character);
                result.append(character);
            } catch (IOException | IllegalArgumentException ex) {
                result.append('?');
            }
        });
        return result.toString();
    }

    private static PDFont loadFont(PDDocument document, String fileName, Standard14Fonts.FontName fallback) {
        File fontFile = new File(FONT_FOLDER, fileName);
        if (fontFile.isFile()) {
            try {
                TrueTypeFont font = new TTFParser().parse(new RandomAccessReadBufferedFile(fontFile));
                // Noto Sans's glyph substitutions swap the digits for alternates PDFBox embeds without a Unicode
                // mapping, so every figure would print but could not be searched or copied out of the PDF.
                font.setEnableGsub(false);
                // Subsetting reads the font when the document is saved, so it stays open until the document closes.
                document.registerTrueTypeFontForClosing(font);
                return PDType0Font.load(document, font, true);
            } catch (IOException ex) {
                LOGGER.warn(ex, "Unable to load {} for the contract PDF; falling back to {}.", fontFile, fallback);
            }
        }
        return new PDType1Font(fallback);
    }

    // endregion Text
}
