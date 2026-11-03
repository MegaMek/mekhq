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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import mekhq.campaign.utilities.glossary.DocumentationEntry;
import mekhq.gui.dialog.glossary.GuideDocument.OutlineItem;
import mekhq.gui.dialog.glossary.GuideDocument.PageSize;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for {@link GuideReader}: a guide's page sizes, outline and page text must all be read, and every guide that
 * ships with MekHQ must read.
 */
class GuideReaderTest {
    /** Writes a three-page guide: the second page turned on its side, with a two-level outline. */
    private static File sampleGuide(Path directory) throws IOException {
        File file = directory.resolve("sample.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            String[] texts = { "Introduction to prisoners", "Capture rules and ransom offers", "Conclusion" };
            for (String text : texts) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(font, 12);
                    stream.newLineAtOffset(72, 700);
                    stream.showText(text);
                    stream.endText();
                }
            }
            document.getPage(1).setRotation(90);

            PDDocumentOutline outline = new PDDocumentOutline();
            PDOutlineItem introduction = item(document, "Intro\u200Bduction", 0);
            PDOutlineItem rules = item(document, "Core Systems", 1);
            rules.addLast(item(document, "Ransom Events", 1));
            outline.addLast(introduction);
            outline.addLast(rules);
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(file);
        }
        return file;
    }

    private static PDOutlineItem item(PDDocument document, String title, int pageIndex) {
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(title);
        PDPageFitDestination destination = new PDPageFitDestination();
        destination.setPage(document.getPage(pageIndex));
        item.setDestination(destination);
        return item;
    }

    @Test
    void pagesOutlineAndTextAreRead(@TempDir Path directory) throws IOException {
        GuideDocument guide = GuideReader.read(sampleGuide(directory));

        assertEquals(3, guide.pageCount());
        assertEquals(3, guide.outline().size());
        assertEquals(new OutlineItem("Intro duction", 1, 0), guide.outline().get(0));
        assertEquals(new OutlineItem("Core Systems", 2, 0), guide.outline().get(1));
        assertEquals(new OutlineItem("Ransom Events", 2, 1), guide.outline().get(2));
        assertTrue(guide.pageTexts().get(1).contains("ransom offers"), guide.pageTexts().get(1));
        assertFalse(guide.pageTexts().get(0).contains("\n"), "page text is kept on one line for searching");
    }

    @Test
    void aTurnedPageIsMeasuredTurned(@TempDir Path directory) throws IOException {
        GuideDocument guide = GuideReader.read(sampleGuide(directory));
        PageSize upright = guide.pageSizes().get(0);
        PageSize turned = guide.pageSizes().get(1);

        assertTrue(upright.height() > upright.width());
        assertEquals(upright.width(), turned.height(), 0.01);
        assertEquals(upright.height(), turned.width(), 0.01);
    }

    @Test
    void theSectionIsTheLastBookmarkAtOrBeforeThePage(@TempDir Path directory) throws IOException {
        GuideDocument guide = GuideReader.read(sampleGuide(directory));

        assertEquals("Intro duction", guide.sectionAt(1).title());
        assertEquals("Ransom Events", guide.sectionAt(2).title());
        assertEquals("Ransom Events", guide.sectionAt(3).title());
    }

    @Test
    void invisibleCharactersAndRunsOfSpaceAreCleaned() {
        assertEquals("Prisoners of War & Rescue", GuideReader.clean("Prisoners of War\u200B&\u200BRescue"
                                                                          .replace("&", " & ")));
        assertEquals("one two", GuideReader.clean("  one\n\t two \uFEFF"));
    }

    @ParameterizedTest
    @EnumSource(DocumentationEntry.class)
    void everyGuideThatShipsWithMekHQReads(DocumentationEntry entry) throws IOException {
        GuideDocument guide = GuideReader.read(GuideReader.fileFor(entry));

        assertTrue(guide.pageCount() > 0, entry + " has no pages");
        assertEquals(guide.pageCount(), guide.pageTexts().size());
        assertTrue(guide.pageTexts().stream().anyMatch(text -> !text.isBlank()), entry + " has no text to search");
        for (OutlineItem item : guide.outline()) {
            assertTrue(item.page() >= 0 && item.page() <= guide.pageCount(), entry + ": " + item);
        }
    }
}
