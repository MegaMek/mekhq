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
package mekhq.gui.dialog.glossary;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.utilities.glossary.GlossaryText;
import mekhq.gui.dialog.glossary.GlossaryRichText.Block;
import mekhq.gui.dialog.glossary.GlossaryRichText.Run;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

/**
 * Writes glossary entries to a PDF, so players can read them away from MekHQ: printed at the table, or kept beside the
 * rulebooks.
 *
 * <p>The document opens with a title block and, if asked, a contents page listing every entry with its page. Each
 * entry follows with its version, title and text, set from the entry's HTML with its headings, lists, quotations,
 * tables, bold and italic kept. A link to another entry in the same document jumps to it; a link to an entry left out
 * reads as plain text. The reader's sidebar can carry a bookmark for each entry, and every page carries a page count.
 * Text is set in the bundled Noto Sans; if it cannot be found, Helvetica is used, and any character it cannot show is
 * replaced.</p>
 *
 * <p>The page layout follows the contract market's PDF export, so the two read as a set.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class GlossaryPdfExporter {
    private static final MMLogger LOGGER = MMLogger.create(GlossaryPdfExporter.class);
    private static final String RESOURCE_BUNDLE = GlossaryConsole.RESOURCE_BUNDLE;

    private static final String FONT_FOLDER = MHQConstants.FONT_DIRECTORY + "Noto Sans/";

    private static final float MARGIN = 54;
    private static final float TITLE_SIZE = 20;
    private static final float ENTRY_TITLE_SIZE = 14;
    private static final float[] HEADING_SIZES = { 12.5f, 11.5f, 10.5f };
    private static final float BODY_SIZE = 9.5f;
    private static final float SMALL_SIZE = 8;
    private static final float LEADING = 1.35f;
    private static final float PARAGRAPH_GAP = 4;
    private static final float LIST_INDENT = 14;
    private static final float QUOTE_INDENT = 12;
    private static final float CELL_GAP = 8;
    /** Room an entry's opening lines need; an entry that would start with less left on the page starts a new one. */
    private static final float ENTRY_MINIMUM_SPACE = 110;

    private static final Color INK = new Color(0x1E, 0x24, 0x2B);
    private static final Color MUTED = new Color(0x5C, 0x66, 0x70);
    private static final Color ACCENT = new Color(0x0E, 0x6E, 0x80);
    private static final Color RULE = new Color(0xC8, 0xD0, 0xD6);

    private static final Pattern WORD = Pattern.compile("\\S+");

    /** The paper the document is laid out for. */
    public enum PageSize {
        LETTER(PDRectangle.LETTER),
        A4(PDRectangle.A4);

        private final PDRectangle rectangle;

        PageSize(PDRectangle rectangle) {
            this.rectangle = rectangle;
        }
    }

    /**
     * What the document includes.
     *
     * @param pageSize     the paper
     * @param contents     {@code true} to open with a contents page
     * @param bookmarks    {@code true} to give each entry a bookmark in the reader's sidebar
     * @param links        {@code true} to make links between included entries jump to them
     * @param referencedBy {@code true} to end each entry with the entries that link to it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record Options(PageSize pageSize, boolean contents, boolean bookmarks, boolean links,
          boolean referencedBy) {
        /** @return the usual choices: Letter paper, with contents, bookmarks and links */
        public static Options defaults() {
            return new Options(PageSize.LETTER, true, true, true, false);
        }
    }

    /**
     * One entry to write.
     *
     * @param key          the entry's lookup name, which links to it use
     * @param title        the entry's title
     * @param versionLine  the line above the title saying when the entry changed
     * @param html         the entry's text
     * @param referencedBy the titles of the entries that link to it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record Entry(String key, String title, String versionLine, String html, List<String> referencedBy) {}

    /** Where an entry starts, for links and bookmarks to jump to. */
    private record Target(PDPage page, float top) {}

    /** A link drawn on a page, waiting for its target to be placed. */
    private record PendingLink(PDPage page, PDRectangle area, String key) {}

    /** A word set in one style, with whether a space comes before it. */
    private record Word(String text, PDFont font, Color color, @Nullable String linkKey, boolean spaceBefore,
          boolean lineBreak) {}

    /** A word placed on a line, at an offset from the line's start. */
    private record Placed(Word word, float offset, float width) {}

    private final PDDocument document;
    private final Options options;
    private final PDRectangle pageSize;
    private final PDFont regular;
    private final PDFont bold;
    private final PDFont italic;
    private final PDFont boldItalic;
    private final Set<String> includedKeys = new HashSet<>();
    private final List<PDPage> entryPages = new ArrayList<>();
    private final List<PDPage> frontPages = new ArrayList<>();
    private final Map<String, Target> targets = new HashMap<>();
    private final List<PendingLink> pendingLinks = new ArrayList<>();

    private @Nullable PDPageContentStream stream;
    private @Nullable PDPage page;
    private boolean isPageOpen;
    private float y;

    /** While measuring, pages are counted rather than made, and nothing is drawn. */
    private boolean isMeasuring;
    private int measuredPages;
    /** Front matter is placed before this page, the first entry's, once the entries are laid out. */
    private @Nullable PDPage insertBefore;

    private GlossaryPdfExporter(PDDocument document, Options options) {
        this.document = document;
        this.options = options;
        this.pageSize = options.pageSize().rectangle;
        this.regular = loadFont(document, "NotoSans-Regular.ttf", Standard14Fonts.FontName.HELVETICA);
        this.bold = loadFont(document, "NotoSans-Bold.ttf", Standard14Fonts.FontName.HELVETICA_BOLD);
        this.italic = loadFont(document, "NotoSans-Italic.ttf", Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        this.boldItalic = loadFont(document, "NotoSans-BoldItalic.ttf",
              Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
    }

    /**
     * Writes glossary entries to a PDF file.
     *
     * @param outputFile the destination file
     * @param title      the document's title
     * @param subtitle   the line under the title, such as the MekHQ version
     * @param entries    the entries, in the order they should appear
     * @param options    what the document includes
     *
     * @return the number of pages written
     *
     * @throws IOException if the file cannot be written
     * @author Illiani
     * @since 0.51.01
     */
    public static int export(File outputFile, String title, String subtitle, List<Entry> entries, Options options)
          throws IOException {
        try (PDDocument document = new PDDocument()) {
            write(document, title, subtitle, entries, options);
            document.save(outputFile);
            return document.getNumberOfPages();
        }
    }

    /**
     * Writes glossary entries as a PDF to a stream.
     *
     * @return the number of pages written
     *
     * @see #export(File, String, String, List, Options)
     */
    public static int export(OutputStream outputStream, String title, String subtitle, List<Entry> entries,
          Options options) throws IOException {
        try (PDDocument document = new PDDocument()) {
            write(document, title, subtitle, entries, options);
            document.save(outputStream);
            return document.getNumberOfPages();
        }
    }

    private static void write(PDDocument document, String title, String subtitle, List<Entry> entries,
          Options options) throws IOException {
        PDDocumentInformation information = document.getDocumentInformation();
        information.setTitle(title);
        information.setCreator("MekHQ " + MHQConstants.VERSION);

        GlossaryPdfExporter exporter = new GlossaryPdfExporter(document, options);
        entries.forEach(entry -> exporter.includedKeys.add(entry.key()));
        try {
            if (options.contents() && !entries.isEmpty()) {
                // The contents need each entry's page, so the entries are laid out first and the title and contents
                // are then placed in front of them.
                for (Entry entry : entries) {
                    exporter.writeEntry(entry);
                }
                exporter.closeStream();
                int frontPageCount = exporter.measureFrontMatter(title, subtitle, entries);
                exporter.insertBefore = exporter.entryPages.get(0);
                exporter.writeFrontMatter(title, subtitle, entries, frontPageCount);
            } else {
                exporter.writeTitle(title, subtitle, entries.size());
                for (Entry entry : entries) {
                    exporter.writeEntry(entry);
                }
            }
        } finally {
            exporter.closeStream();
        }
        exporter.resolveLinks();
        if (options.bookmarks()) {
            exporter.writeOutline(entries);
        }
        exporter.writePageFooters(title);
    }

    // region Content

    private void writeTitle(String title, String subtitle, int entryCount) throws IOException {
        newPage();
        wrapped(title.toUpperCase(Locale.ROOT), bold, TITLE_SIZE, ACCENT, MARGIN, contentWidth());
        wrapped(subtitle, regular, BODY_SIZE + 1, INK, MARGIN, contentWidth());
        wrapped(getFormattedTextAt(RESOURCE_BUNDLE, "pdf.glossary.entryCount", entryCount), regular, BODY_SIZE,
              MUTED, MARGIN, contentWidth());
        y -= 6;
        rule(ACCENT, 1.2f);
        y -= 14;
    }

    private int measureFrontMatter(String title, String subtitle, List<Entry> entries) throws IOException {
        isMeasuring = true;
        measuredPages = 0;
        isPageOpen = false;
        writeTitle(title, subtitle, entries.size());
        writeContents(entries, 0);
        isMeasuring = false;
        isPageOpen = false;
        return measuredPages;
    }

    private void writeFrontMatter(String title, String subtitle, List<Entry> entries, int frontPageCount)
          throws IOException {
        writeTitle(title, subtitle, entries.size());
        writeContents(entries, frontPageCount);
        closeStream();
    }

    private void writeContents(List<Entry> entries, int frontPageCount) throws IOException {
        wrapped(getTextAt(RESOURCE_BUNDLE, "pdf.glossary.contents").toUpperCase(Locale.ROOT), bold, HEADING_SIZES[1],
              ACCENT, MARGIN, contentWidth());
        rule(RULE, 0.6f);
        y -= 3;

        float numberColumn = textWidth("0000", regular, BODY_SIZE) + 12;
        for (Entry entry : entries) {
            List<String> lines = wrap(entry.title(), regular, BODY_SIZE, contentWidth() - numberColumn);
            ensureSpace(lines.size() * BODY_SIZE * LEADING);
            float top = y;
            for (String line : lines) {
                y -= BODY_SIZE * LEADING;
                draw(line, regular, BODY_SIZE, INK, MARGIN, y);
            }
            Target target = targets.get(entry.key());
            if (target != null) {
                String number = Integer.toString(frontPageCount + entryPages.indexOf(target.page()) + 1);
                float width = textWidth(number, regular, BODY_SIZE);
                draw(number, regular, BODY_SIZE, MUTED, pageSize.getWidth() - MARGIN - width, y);
                if (options.links()) {
                    addLink(new PDRectangle(MARGIN, y - (BODY_SIZE * 0.3f), contentWidth(),
                          top - y + (BODY_SIZE * 0.1f)), entry.key());
                }
            }
        }
    }

    private void writeEntry(Entry entry) throws IOException {
        ensureSpace(ENTRY_MINIMUM_SPACE);
        if (!isMeasuring && page != null) {
            targets.put(entry.key(), new Target(page, y + 6));
        }

        wrapped(entry.versionLine().toUpperCase(Locale.ROOT), bold, SMALL_SIZE, MUTED, MARGIN, contentWidth());
        wrapped(entry.title().toUpperCase(Locale.ROOT), bold, ENTRY_TITLE_SIZE, ACCENT, MARGIN, contentWidth());
        rule(RULE, 0.6f);
        y -= 4;

        for (Block block : GlossaryRichText.parse(entry.html())) {
            writeBlock(block);
        }

        if (options.referencedBy() && !entry.referencedBy().isEmpty()) {
            y -= 2;
            wrapped(getTextAt(RESOURCE_BUNDLE, "pdf.glossary.referencedBy") + " "
                          + String.join(", ", entry.referencedBy()), italic, SMALL_SIZE, MUTED, MARGIN,
                  contentWidth());
        }
        y -= 8;
        rule(RULE, 0.8f);
        y -= 14;
    }

    private void writeBlock(Block block) throws IOException {
        float x = MARGIN;
        float width = contentWidth();
        if (block.quoted()) {
            x += QUOTE_INDENT;
            width -= QUOTE_INDENT;
        }

        switch (block.kind()) {
            case HEADING -> {
                float size = HEADING_SIZES[Math.min(block.level(), HEADING_SIZES.length) - 1];
                // A heading keeps a couple of lines of what follows it on the same page.
                ensureSpace(size * LEADING + (BODY_SIZE * LEADING * 2));
                y -= 4;
                flow(block.runs(), size, block.level() <= 2 ? ACCENT : INK, true, false, x, width,
                      block.centered());
                y -= 2;
            }
            case PARAGRAPH -> {
                float top = y;
                int startPage = pageNumberNow();
                flow(block.runs(), BODY_SIZE, block.quoted() ? MUTED : INK, false, block.quoted(), x, width,
                      block.centered());
                // The rule down the left of a quotation, as the reader shows it. A quotation broken across pages goes
                // without one rather than drawing it down the wrong page.
                if (block.quoted() && pageNumberNow() == startPage) {
                    bar(MARGIN + 2, top - (BODY_SIZE * 0.3f), y - (BODY_SIZE * 0.3f));
                }
                y -= PARAGRAPH_GAP;
            }
            case LIST_ITEM -> {
                float indent = LIST_INDENT * block.level();
                List<Line> lines = layOut(block.runs(), BODY_SIZE, INK, false, false, width - indent);
                for (int i = 0; i < lines.size(); i++) {
                    ensureSpace(BODY_SIZE * LEADING);
                    y -= BODY_SIZE * LEADING;
                    if (i == 0) {
                        float markerWidth = textWidth(block.marker(), regular, BODY_SIZE);
                        draw(block.marker(), regular, BODY_SIZE, MUTED, x + indent - markerWidth - 4, y);
                    }
                    drawLine(lines.get(i), x + indent, 0);
                }
                y -= PARAGRAPH_GAP / 2;
            }
            case TABLE_ROW -> writeTableRow(block, x, width);
        }
    }

    private void writeTableRow(Block block, float x, float width) throws IOException {
        int columns = block.cells().size();
        float cellWidth = (width - (CELL_GAP * (columns - 1))) / columns;
        List<List<Line>> cellLines = new ArrayList<>();
        int rowLines = 1;
        for (List<Run> cell : block.cells()) {
            List<Line> lines = layOut(cell, BODY_SIZE, INK, block.header(), false, cellWidth);
            cellLines.add(lines);
            rowLines = Math.max(rowLines, lines.size());
        }
        ensureSpace(rowLines * BODY_SIZE * LEADING + 4);
        float top = y;
        for (int column = 0; column < columns; column++) {
            y = top;
            for (Line line : cellLines.get(column)) {
                y -= BODY_SIZE * LEADING;
                drawLine(line, x + (column * (cellWidth + CELL_GAP)), 0);
            }
        }
        y = top - (rowLines * BODY_SIZE * LEADING);
        y -= 2;
        rule(block.header() ? ACCENT : RULE, block.header() ? 0.8f : 0.4f, x, x + width);
        y -= 1;
    }

    // endregion Content

    // region Rich text

    /** One line of placed words, its total width, and the size it was set at. */
    private record Line(List<Placed> words, float width, float size) {}

    /** Sets runs of styled text wrapped to {@code width}, moving down a line for each. */
    private void flow(List<Run> runs, float size, Color color, boolean forceBold, boolean forceItalic, float x,
          float width, boolean centered) throws IOException {
        for (Line line : layOut(runs, size, color, forceBold, forceItalic, width)) {
            ensureSpace(size * LEADING);
            y -= size * LEADING;
            drawLine(line, x, centered ? (width - line.width()) / 2 : 0);
        }
    }

    private List<Line> layOut(List<Run> runs, float size, Color color, boolean forceBold, boolean forceItalic,
          float width) throws IOException {
        List<Line> lines = new ArrayList<>();
        List<Placed> current = new ArrayList<>();
        float cursor = 0;
        for (Word word : words(runs, color, forceBold, forceItalic)) {
            if (word.lineBreak()) {
                lines.add(new Line(current, cursor, size));
                current = new ArrayList<>();
                cursor = 0;
                continue;
            }
            float space = (word.spaceBefore() && !current.isEmpty()) ? textWidth(" ", word.font(), size) : 0;
            float wordWidth = textWidth(word.text(), word.font(), size);
            if (!current.isEmpty() && (cursor + space + wordWidth > width)) {
                lines.add(new Line(current, cursor, size));
                current = new ArrayList<>();
                cursor = 0;
                space = 0;
            }
            if (wordWidth <= width) {
                current.add(new Placed(word, cursor + space, wordWidth));
                cursor += space + wordWidth;
                continue;
            }

            // A word wider than the line on its own is broken wherever it overflows.
            String remainder = word.text();
            while (textWidth(remainder, word.font(), size) > width && remainder.length() > 1) {
                int fit = remainder.length() - 1;
                while (fit > 1 && textWidth(remainder.substring(0, fit), word.font(), size) > width) {
                    fit--;
                }
                String piece = remainder.substring(0, fit);
                float pieceWidth = textWidth(piece, word.font(), size);
                lines.add(new Line(List.of(new Placed(withText(word, piece), 0, pieceWidth)), pieceWidth, size));
                remainder = remainder.substring(fit);
            }
            float remainderWidth = textWidth(remainder, word.font(), size);
            current.add(new Placed(withText(word, remainder), 0, remainderWidth));
            cursor = remainderWidth;
        }
        if (!current.isEmpty()) {
            lines.add(new Line(current, cursor, size));
        }
        return lines;
    }

    private static Word withText(Word word, String text) {
        return new Word(text, word.font(), word.color(), word.linkKey(), word.spaceBefore(), false);
    }

    /** Splits runs into words, each with its font, colour and link. */
    private List<Word> words(List<Run> runs, Color color, boolean forceBold, boolean forceItalic) {
        List<Word> words = new ArrayList<>();
        boolean pendingSpace = false;
        for (Run run : runs) {
            if (run.isLineBreak()) {
                words.add(new Word("", regular, color, null, false, true));
                pendingSpace = false;
                continue;
            }
            PDFont font = font(forceBold || run.bold(), forceItalic || run.italic());
            String linkKey = linkKey(run.link());
            Color wordColor = linkKey != null ? ACCENT : color;
            String text = run.text();
            Matcher matcher = WORD.matcher(text);
            int previousEnd = 0;
            while (matcher.find()) {
                boolean spaceBefore = pendingSpace || matcher.start() > previousEnd;
                words.add(new Word(printable(matcher.group(), font), font, wordColor, linkKey, spaceBefore, false));
                previousEnd = matcher.end();
                pendingSpace = false;
            }
            // White space after the last word, or a run of nothing else, separates it from the next run's words.
            if (previousEnd < text.length()) {
                pendingSpace = true;
            }
        }
        return words;
    }

    /** The entry a link jumps to, if links are wanted and that entry is in the document. */
    private @Nullable String linkKey(@Nullable String link) {
        if (!options.links() || link == null || !link.startsWith(GlossaryText.GLOSSARY_LINK_PREFIX)) {
            return null;
        }
        String key = link.substring(GlossaryText.GLOSSARY_LINK_PREFIX.length()).strip();
        return includedKeys.contains(key) ? key : null;
    }

    private void drawLine(Line line, float x, float offset) throws IOException {
        for (Placed placed : line.words()) {
            Word word = placed.word();
            float wordX = x + offset + placed.offset();
            draw(word.text(), word.font(), line.size(), word.color(), wordX, y);
            if (word.linkKey() != null) {
                addLink(new PDRectangle(wordX, y - (line.size() * 0.25f), placed.width(), line.size() * 1.1f),
                      word.linkKey());
            }
        }
    }

    private PDFont font(boolean isBold, boolean isItalic) {
        if (isBold && isItalic) {
            return boldItalic;
        }
        return isBold ? bold : (isItalic ? italic : regular);
    }

    // endregion Rich text

    // region Layout

    private float contentWidth() {
        return pageSize.getWidth() - (2 * MARGIN);
    }

    private int pageNumberNow() {
        return isMeasuring ? measuredPages : (entryPages.size() + frontPages.size());
    }

    private void newPage() throws IOException {
        // Closing the last page's stream marks no page open, so this comes before the new page is marked open.
        closeStream();
        isPageOpen = true;
        y = pageSize.getHeight() - MARGIN;
        if (isMeasuring) {
            measuredPages++;
            return;
        }
        PDPage newPage = new PDPage(pageSize);
        if (insertBefore != null) {
            document.getPages().insertBefore(newPage, insertBefore);
            frontPages.add(newPage);
        } else {
            document.addPage(newPage);
            entryPages.add(newPage);
        }
        page = newPage;
        stream = new PDPageContentStream(document, newPage);
    }

    private void closeStream() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }
        isPageOpen = false;
    }

    /** Starts a new page if fewer than {@code height} points are left above the bottom margin. */
    private void ensureSpace(float height) throws IOException {
        if (!isPageOpen || (y - height < MARGIN)) {
            newPage();
        }
    }

    /** Sets text wrapped to {@code width}, one line at a time. */
    private void wrapped(String text, PDFont font, float size, Color color, float x, float width) throws IOException {
        for (String line : wrap(text, font, size, width)) {
            ensureSpace(size * LEADING);
            y -= size * LEADING;
            draw(line, font, size, color, x, y);
        }
    }

    private void rule(Color color, float thickness) throws IOException {
        rule(color, thickness, MARGIN, pageSize.getWidth() - MARGIN);
    }

    private void rule(Color color, float thickness, float fromX, float toX) throws IOException {
        ensureSpace(thickness + 2);
        y -= 3;
        if (isMeasuring || stream == null) {
            return;
        }
        stream.setStrokingColor(color);
        stream.setLineWidth(thickness);
        stream.moveTo(fromX, y);
        stream.lineTo(toX, y);
        stream.stroke();
    }

    private void bar(float x, float top, float bottom) throws IOException {
        if (isMeasuring || stream == null) {
            return;
        }
        stream.setStrokingColor(ACCENT);
        stream.setLineWidth(1.5f);
        stream.moveTo(x, top);
        stream.lineTo(x, bottom);
        stream.stroke();
    }

    private void draw(String text, PDFont font, float size, Color color, float x, float baseline)
          throws IOException {
        if (isMeasuring || stream == null || text.isEmpty()) {
            return;
        }
        stream.beginText();
        stream.setFont(font, size);
        stream.setNonStrokingColor(color);
        stream.newLineAtOffset(x, baseline);
        stream.showText(printable(text, font));
        stream.endText();
    }

    private void addLink(PDRectangle area, String key) {
        if (!isMeasuring && page != null) {
            pendingLinks.add(new PendingLink(page, area, key));
        }
    }

    /** Turns the links drawn on each page into jumps to where their entries start. */
    private void resolveLinks() {
        Map<PDPage, List<PDAnnotation>> annotationsByPage = new HashMap<>();
        for (PendingLink pending : pendingLinks) {
            Target target = targets.get(pending.key());
            if (target == null) {
                continue;
            }
            PDAnnotationLink link = new PDAnnotationLink();
            link.setRectangle(pending.area());
            PDBorderStyleDictionary border = new PDBorderStyleDictionary();
            border.setWidth(0);
            link.setBorderStyle(border);
            PDActionGoTo action = new PDActionGoTo();
            action.setDestination(destination(target));
            link.setAction(action);
            annotationsByPage.computeIfAbsent(pending.page(), key -> new ArrayList<>()).add(link);
        }
        annotationsByPage.forEach((linkPage, links) -> {
            try {
                List<PDAnnotation> annotations = new ArrayList<>(linkPage.getAnnotations());
                annotations.addAll(links);
                linkPage.setAnnotations(annotations);
            } catch (IOException ex) {
                LOGGER.warn(ex, "Unable to add links to a glossary PDF page");
            }
        });
    }

    private static PDPageXYZDestination destination(Target target) {
        PDPageXYZDestination destination = new PDPageXYZDestination();
        destination.setPage(target.page());
        destination.setTop(Math.round(target.top()));
        return destination;
    }

    /** Gives the reader's sidebar a bookmark for the contents, if any, and each entry. */
    private void writeOutline(List<Entry> entries) {
        PDDocumentOutline outline = new PDDocumentOutline();
        if (!frontPages.isEmpty()) {
            PDOutlineItem contents = new PDOutlineItem();
            contents.setTitle(getTextAt(RESOURCE_BUNDLE, "pdf.glossary.contents"));
            contents.setDestination(destination(new Target(frontPages.get(0), pageSize.getHeight() - MARGIN)));
            outline.addLast(contents);
        }
        for (Entry entry : entries) {
            Target target = targets.get(entry.key());
            if (target != null) {
                PDOutlineItem item = new PDOutlineItem();
                item.setTitle(entry.title());
                item.setDestination(destination(target));
                outline.addLast(item);
            }
        }
        outline.openNode();
        document.getDocumentCatalog().setDocumentOutline(outline);
        document.getDocumentCatalog().setPageMode(PageMode.USE_OUTLINES);
    }

    private void writePageFooters(String title) throws IOException {
        int total = document.getNumberOfPages();
        int number = 0;
        for (PDPage footerPage : document.getPages()) {
            number++;
            try (PDPageContentStream footer = new PDPageContentStream(document, footerPage, AppendMode.APPEND,
                  true, true)) {
                String pageText = printable(getFormattedTextAt(RESOURCE_BUNDLE, "pdf.glossary.page", number, total),
                      regular);
                float width = textWidth(pageText, regular, SMALL_SIZE);
                footer.beginText();
                footer.setFont(regular, SMALL_SIZE);
                footer.setNonStrokingColor(MUTED);
                footer.newLineAtOffset(pageSize.getWidth() - MARGIN - width, MARGIN / 2);
                footer.showText(pageText);
                footer.endText();

                footer.beginText();
                footer.setFont(regular, SMALL_SIZE);
                footer.setNonStrokingColor(MUTED);
                footer.newLineAtOffset(MARGIN, MARGIN / 2);
                footer.showText(printable(title, regular));
                footer.endText();
            }
        }
    }

    // endregion Layout

    // region Text

    /**
     * Breaks plain text into lines no wider than {@code width}, at word boundaries where it can and mid-word where a
     * single word is too long.
     */
    private List<String> wrap(String text, PDFont font, float size, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Line line : layOut(List.of(new Run(text, false, false, null)), size, INK, font == bold,
              font == italic, width)) {
            StringBuilder builder = new StringBuilder();
            for (Placed placed : line.words()) {
                if (!builder.isEmpty() && placed.word().spaceBefore()) {
                    builder.append(' ');
                }
                builder.append(placed.word().text());
            }
            lines.add(builder.toString());
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
                LOGGER.warn(ex, "Unable to load {} for the glossary PDF; falling back to {}.", fontFile, fallback);
            }
        }
        return new PDType1Font(fallback);
    }

    // endregion Text
}
