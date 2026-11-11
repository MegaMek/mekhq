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

import java.util.List;

import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.dialog.glossary.GlossaryRichText.Block;
import mekhq.gui.dialog.glossary.GlossaryRichText.Kind;
import mekhq.gui.dialog.glossary.GlossaryRichText.Run;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for {@link GlossaryRichText}: glossary HTML must come out as the headings, paragraphs, lists and tables the
 * PDF lays out, with bold, italic and links kept.
 */
class GlossaryRichTextTest {
    @Test
    void headingsAndParagraphsBecomeBlocks() {
        List<Block> blocks = GlossaryRichText.parse("<h2>Title</h2><p>First.</p><p>Second.</p>");

        assertEquals(3, blocks.size());
        assertEquals(Kind.HEADING, blocks.get(0).kind());
        assertEquals(2, blocks.get(0).level());
        assertEquals("First.", blocks.get(1).plainText());
        assertEquals(Kind.PARAGRAPH, blocks.get(2).kind());
    }

    @Test
    void textOutsideAnyParagraphStillBecomesOne() {
        List<Block> blocks = GlossaryRichText.parse("Loose text<br>on two lines");

        assertEquals(1, blocks.size());
        assertTrue(blocks.get(0).runs().stream().anyMatch(Run::isLineBreak));
    }

    @Test
    void boldItalicAndLinksAreKept() {
        Block block = GlossaryRichText.parse("<p><b>Bold</b> <i>italic</i> <a href='GLOSSARY:HR_STRAIN'>link</a></p>")
                            .get(0);

        assertTrue(block.runs().stream().anyMatch(run -> run.bold() && run.text().strip().equals("Bold")));
        assertTrue(block.runs().stream().anyMatch(run -> run.italic() && run.text().strip().equals("italic")));
        assertTrue(block.runs().stream().anyMatch(run -> "GLOSSARY:HR_STRAIN".equals(run.link())));
    }

    @Test
    void orderedListsAreNumberedAndNestedListsGoDeeper() {
        List<Block> blocks = GlossaryRichText.parse("<ol><li>One</li><li>Two<ul><li>Inner</li></ul></li></ol>");

        assertEquals("1.", blocks.get(0).marker());
        assertEquals("2.", blocks.get(1).marker());
        Block inner = blocks.get(blocks.size() - 1);
        assertEquals("Inner", inner.plainText());
        assertEquals(2, inner.level());
    }

    @Test
    void quotationsAreMarked() {
        List<Block> blocks = GlossaryRichText.parse("<blockquote>Quoted words</blockquote><p>After</p>");

        assertTrue(blocks.get(0).quoted());
        assertFalse(blocks.get(1).quoted());
    }

    @Test
    void tableRowsKeepTheirCells() {
        List<Block> blocks = GlossaryRichText.parse(
              "<table><tr><th>Roll</th><th>Result</th></tr><tr><td>2</td><td>Disaster</td></tr></table>");

        assertEquals(2, blocks.size());
        assertTrue(blocks.get(0).header());
        assertEquals(2, blocks.get(1).cells().size());
        assertEquals("2 Disaster", blocks.get(1).plainText());
    }

    @Test
    void centredTextIsMarked() {
        assertTrue(GlossaryRichText.parse("<p style='text-align: center;'>Middle</p>").get(0).centered());
    }

    @ParameterizedTest
    @EnumSource(GlossaryEntry.class)
    void everyEntryReadsIntoAtLeastOneBlock(GlossaryEntry entry) {
        assertFalse(GlossaryRichText.parse(entry.getDefinition()).isEmpty(), entry + " read as nothing");
    }
}
