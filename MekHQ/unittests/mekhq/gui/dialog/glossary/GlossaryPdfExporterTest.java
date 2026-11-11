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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.dialog.glossary.GlossaryPdfExporter.Entry;
import mekhq.gui.dialog.glossary.GlossaryPdfExporter.Options;
import mekhq.gui.dialog.glossary.GlossaryPdfExporter.PageSize;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GlossaryPdfExporter}: the document must open, carry every entry with its contents page and
 * bookmarks, link only to entries it includes, and survive text the standard fonts cannot show.
 */
class GlossaryPdfExporterTest {
    private static Entry entry(String key, String title, String html) {
        return new Entry(key, title, "Updated 0.51.01", html, List.of("Referring Entry"));
    }

    private static byte[] export(List<Entry> entries, Options options) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GlossaryPdfExporter.export(output, "MekHQ Glossary", "MekHQ 0.51.01", entries, options);
        return output.toByteArray();
    }

    private static List<Entry> manyEntries(int count) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(entry("KEY_" + i, "Entry " + i, "<p>" + "Some glossary text. ".repeat(60) + "</p>"
                                                           + "<p><a href='GLOSSARY:KEY_0'>Back to the first</a></p>"));
        }
        return entries;
    }

    @Test
    void theDocumentOpensAndNamesEveryEntry() throws IOException {
        byte[] pdf = export(List.of(entry("A", "Alpha Strike", "<p>Fire <b>everything</b>.</p>"),
              entry("B", "Battle Value", "<ul><li>One</li><li>Two</li></ul>")), Options.defaults());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("ALPHA STRIKE"));
            assertTrue(text.contains("BATTLE VALUE"));
            assertTrue(text.contains("everything"));
            assertTrue(text.contains("CONTENTS"));
        }
    }

    @Test
    void theContentsComeFirstAndPagesAreNumberedThroughout() throws IOException {
        try (PDDocument document = Loader.loadPDF(export(manyEntries(12), Options.defaults()))) {
            int pages = document.getNumberOfPages();
            assertTrue(pages > 2, "twelve long entries cannot fit on two pages");
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            assertTrue(stripper.getText(document).contains("CONTENTS"));
            stripper.setStartPage(pages);
            stripper.setEndPage(pages);
            assertTrue(stripper.getText(document).contains("Page " + pages + " of " + pages));
        }
    }

    @Test
    void everyEntryHasABookmark() throws IOException {
        try (PDDocument document = Loader.loadPDF(export(manyEntries(5), Options.defaults()))) {
            assertNotNull(document.getDocumentCatalog().getDocumentOutline());
            int count = 0;
            for (var ignored : document.getDocumentCatalog().getDocumentOutline().children()) {
                count++;
            }
            // One for the contents and one for each entry.
            assertEquals(6, count);
        }
    }

    @Test
    void bookmarksAreLeftOutWhenNotWanted() throws IOException {
        Options options = new Options(PageSize.A4, false, false, true, false);
        try (PDDocument document = Loader.loadPDF(export(manyEntries(3), options))) {
            assertNull(document.getDocumentCatalog().getDocumentOutline());
        }
    }

    @Test
    void linksJumpOnlyToEntriesInTheDocument() throws IOException {
        Options noContents = new Options(PageSize.LETTER, false, false, true, false);
        byte[] included = export(List.of(entry("A", "Alpha", "<p>See <a href='GLOSSARY:B'>Beta</a>.</p>"),
              entry("B", "Beta", "<p>Beta.</p>")), noContents);
        byte[] leftOut = export(List.of(entry("A", "Alpha", "<p>See <a href='GLOSSARY:B'>Beta</a>.</p>")),
              noContents);

        assertEquals(1, countLinks(included));
        assertEquals(0, countLinks(leftOut));
    }

    @Test
    void aPageSizeIsHonoured() throws IOException {
        Options a4 = new Options(PageSize.A4, false, false, false, false);
        try (PDDocument document = Loader.loadPDF(export(manyEntries(1), a4))) {
            assertEquals(595, Math.round(document.getPage(0).getMediaBox().getWidth()));
        }
    }

    @Test
    void anEmptyExportStillMakesADocument() throws IOException {
        try (PDDocument document = Loader.loadPDF(export(List.of(), Options.defaults()))) {
            assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void everyRealEntryExports() throws IOException {
        List<Entry> entries = new ArrayList<>();
        for (GlossaryEntry glossaryEntry : GlossaryEntry.values()) {
            entries.add(new Entry(glossaryEntry.getLookUpName(), glossaryEntry.getTitle(),
                  glossaryEntry.getVersionAddedOrLastUpdated().toString(), glossaryEntry.getDefinition(), List.of()));
        }

        try (PDDocument document = Loader.loadPDF(export(entries, Options.defaults()))) {
            assertTrue(document.getNumberOfPages() > entries.size() / 10);
        }
    }

    @Test
    void textTheStandardFontsCannotShowDoesNotBreakTheExport() throws IOException {
        byte[] pdf = export(List.of(entry("A", "\u014Ckami \u7ADC", "<p>\u0420\u044F\u0434 \u0007 control</p>")),
              Options.defaults());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertTrue(document.getNumberOfPages() >= 1);
        }
    }

    @Test
    void charactersAFontCannotShowAreReplaced() {
        PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        assertEquals("Okami ?", GlossaryPdfExporter.printable("Okami \u7ADC", helvetica));
    }

    private static int countLinks(byte[] pdf) throws IOException {
        int links = 0;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            for (PDPage page : document.getPages()) {
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation instanceof PDAnnotationLink) {
                        links++;
                    }
                }
            }
        }
        return links;
    }
}
