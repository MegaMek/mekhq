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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.gui.dialog.glossary.GuideSearch.PageHit;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GuideSearch}: finding words on a guide's pages.
 */
class GuideSearchTest {
    private static final List<String> PAGES = List.of("Ransom offers arrive. A RANSOM is paid.", "No mention here.",
          "The ransom of prisoners");

    @Test
    void everyPlaceIsFoundIgnoringCase() {
        List<PageHit> hits = GuideSearch.find(PAGES, "Ransom");

        assertEquals(List.of(new PageHit(1, 0, 6), new PageHit(1, 24, 6), new PageHit(3, 4, 6)), hits);
    }

    @Test
    void aSingleLetterFindsNothing() {
        assertTrue(GuideSearch.find(PAGES, "a").isEmpty());
        assertEquals("", GuideSearch.normalise(" a "));
    }

    @Test
    void aPhraseBrokenAcrossLinesInTheQueryStillMatches() {
        assertEquals(List.of(new PageHit(3, 4, 19)), GuideSearch.find(PAGES, "ransom\n  of prisoners"));
    }

    @Test
    void lowerCaseKeepsTheTextsLength() {
        String text = "\u0130stanbul RANSOM";

        assertEquals(text.length(), GuideSearch.lower(text).length());
        assertTrue(GuideSearch.lower(text).endsWith("ransom"));
    }
}
