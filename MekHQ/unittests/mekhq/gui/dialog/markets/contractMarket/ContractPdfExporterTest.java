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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mekhq.gui.dialog.markets.contractMarket.ContractReadout.Row;
import mekhq.gui.dialog.markets.contractMarket.ContractReadout.Section;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ContractPdfExporter}: the document must open, carry every offer, flow onto as many pages as it
 * needs, and survive names the standard PDF fonts cannot show.
 */
class ContractPdfExporterTest {
    private static ContractReadout readout(String title, String employer, String briefing) {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            rows.add(new Row("Label " + i, "Value " + i, i == 0));
        }
        return new ContractReadout(employer,
              title,
              "Objective Raid · Galatea · 6-month term · 12 days transit",
              "Even Fight 5/10",
              List.of("Proving Ground"),
              "Leutnant Katrina Steiner",
              briefing,
              List.of(new Section("Terms", rows, List.of("Characteristics: Career Maker")),
                    new Section("Intel", rows, List.of()),
                    new Section("Money", rows, List.of())),
              "Estimate only.");
    }

    private static byte[] export(List<ContractReadout> offers) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ContractPdfExporter.export(output, "Gray Death Legion", "3051-03-04", "Mercenary Work", offers);
        return output.toByteArray();
    }

    @Test
    void theDocumentOpensAndNamesEveryOffer() throws IOException {
        byte[] pdf = export(List.of(readout("Operation Sundowner", "Lyran Alliance", "We need you."),
              readout("Operation Daybreak", "Federated Suns", "Hold the line.")));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("OPERATION SUNDOWNER"));
            assertTrue(text.contains("OPERATION DAYBREAK"));
            assertTrue(text.contains("Gray Death Legion"));
            assertTrue(text.contains("Hold the line."));
        }
    }

    @Test
    void manyOffersFlowOntoFurtherPagesEachNumbered() throws IOException {
        List<ContractReadout> offers = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            offers.add(readout("Operation " + i, "Employer " + i, "A briefing. ".repeat(40)));
        }

        try (PDDocument document = Loader.loadPDF(export(offers))) {
            int pages = document.getNumberOfPages();
            assertTrue(pages > 1, "twelve offers cannot fit on one page");
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Page " + pages + " of " + pages));
        }
    }

    @Test
    void anEmptyBoardStillMakesADocument() throws IOException {
        try (PDDocument document = Loader.loadPDF(export(List.of()))) {
            assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void namesOutsideTheLatinAlphabetDoNotBreakTheExport() throws IOException {
        byte[] pdf = export(List.of(readout("Ōkami Raid", "Draconis Combine 竜", "Ряд \u0007 control")));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void aWordWiderThanTheLineIsBrokenRatherThanOverflowing() throws IOException {
        String longWord = "Supercalifragilistic".repeat(20);

        try (PDDocument document = Loader.loadPDF(export(List.of(readout(longWord, "Employer", longWord))))) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.replaceAll("\\s", "").contains(longWord.toUpperCase()));
        }
    }

    @Test
    void charactersAFontCannotShowAreReplaced() {
        PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        assertEquals("Okami ?", ContractPdfExporter.printable("Okami 竜", helvetica));
        assertEquals("tab here", ContractPdfExporter.printable("tab\there", helvetica));
    }
}
