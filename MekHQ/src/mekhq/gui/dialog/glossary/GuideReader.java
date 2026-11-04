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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import mekhq.campaign.utilities.glossary.DocumentationEntry;
import mekhq.gui.dialog.glossary.GuideDocument.OutlineItem;
import mekhq.gui.dialog.glossary.GuideDocument.PageSize;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Reads a guide's page sizes, outline and text with PDFBox.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class GuideReader {
    /** Bookmarks deeper than this are left out, so the outline stays a table of contents. */
    private static final int MAXIMUM_DEPTH = 1;
    /** Zero-width spaces and joiners, which some guides' word processors scatter through headings. */
    private static final Pattern INVISIBLE = Pattern.compile("[\\u200B-\\u200D\\uFEFF]");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private GuideReader() {
    }

    /**
     * @param entry a guide
     *
     * @return the guide's file
     */
    static File fileFor(DocumentationEntry entry) {
        return new File(entry.getFileAddress());
    }

    /**
     * Reads a guide from its file.
     *
     * @param file the guide
     *
     * @return what the viewer needs to know about it
     *
     * @throws IOException if the file cannot be read
     */
    static GuideDocument read(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            return read(document);
        }
    }

    /**
     * Reads a guide that is already open. The document is left open.
     *
     * @param document the guide
     *
     * @return what the viewer needs to know about it
     *
     * @throws IOException if the document cannot be read
     */
    static GuideDocument read(PDDocument document) throws IOException {
        List<PageSize> sizes = new ArrayList<>();
        for (PDPage page : document.getPages()) {
            sizes.add(pageSize(page));
        }

        List<OutlineItem> outline = new ArrayList<>();
        PDDocumentOutline root = document.getDocumentCatalog().getDocumentOutline();
        if (root != null) {
            readOutline(document, root, 0, outline);
        }

        List<String> texts = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            texts.add(clean(stripper.getText(document)));
        }
        return new GuideDocument(List.copyOf(sizes), List.copyOf(outline), List.copyOf(texts));
    }

    /** The size a page is shown at: its crop box, turned if the page is turned. */
    static PageSize pageSize(PDPage page) {
        PDRectangle box = page.getCropBox();
        int rotation = Math.floorMod(page.getRotation(), 360);
        boolean isTurned = rotation == 90 || rotation == 270;
        return isTurned ? new PageSize(box.getHeight(), box.getWidth()) : new PageSize(box.getWidth(), box.getHeight());
    }

    private static void readOutline(PDDocument document, PDOutlineNode node, int depth, List<OutlineItem> into)
          throws IOException {
        for (PDOutlineItem item : node.children()) {
            String title = clean(item.getTitle() == null ? "" : item.getTitle());
            if (!title.isEmpty()) {
                PDPage target = item.findDestinationPage(document);
                int page = target == null ? 0 : document.getPages().indexOf(target) + 1;
                into.add(new OutlineItem(title, page, depth));
            }
            if (depth < MAXIMUM_DEPTH && item.hasChildren()) {
                readOutline(document, item, depth + 1, into);
            }
        }
    }

    /** Drops invisible characters and collapses white space, so text can be searched and shown in one line. */
    static String clean(String text) {
        return SPACES.matcher(INVISIBLE.matcher(text).replaceAll(" ")).replaceAll(" ").strip();
    }
}
