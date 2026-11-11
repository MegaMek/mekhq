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
package mekhq.campaign.utilities.glossary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GlossaryText}: the plain text search reads, and the links between entries.
 */
class GlossaryTextTest {
    @Test
    void wordsEitherSideOfAParagraphBreakStayApart() {
        assertEquals("the profession. In time", GlossaryText.plainText("<p>the profession.</p><p>In time</p>"));
        assertEquals("one two", GlossaryText.plainText("one<br>two"));
    }

    @Test
    void inlineTagsAreDroppedWithoutAddingSpace() {
        assertEquals("HR Strain", GlossaryText.plainText("<b>HR</b> <i>Str</i>ain"));
    }

    @Test
    void entitiesAreDecoded() {
        assertEquals("A & B < C", GlossaryText.plainText("A &amp; B &lt; C"));
        assertEquals("&lt;", GlossaryText.plainText("&amp;lt;"));
    }

    @Test
    void aBareAddressKeepsItsText() {
        assertEquals("see <https://megamek.org>", GlossaryText.plainText("see <https://megamek.org>"));
    }

    @Test
    void glossaryLinksAreListedOnceInOrder() {
        String html = "<a href='GLOSSARY:B'>b</a> <a href=\"GLOSSARY:A\">a</a> <a href='GLOSSARY:B'>again</a>"
                            + " <a href='DOCUMENTATION:GUIDE'>guide</a>";

        assertEquals(List.of("B", "A"), GlossaryText.glossaryLinks(html));
        assertEquals(List.of("GUIDE"), GlossaryText.documentationLinks(html));
    }
}
