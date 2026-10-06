/*
 * Copyright (C) 2018-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionHints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;

import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RegionPerimeter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FactionHintsTest {

    private Faction createTestFaction(final String id) {
        Faction f = mock(Faction.class);
        when(f.getShortName()).thenReturn(id);
        return f;
    }

    @Test
    public void testIsAlliedWith() {
        FactionHints hints = new FactionHints();
        Faction f1 = createTestFaction("F1");
        Faction f2 = createTestFaction("F2");
        Faction f3 = createTestFaction("F3");
        hints.addAlliance("", null, null, f1, f2);
        LocalDate date = LocalDate.now();

        assertTrue(hints.isAlliedWith(f1, f2, date));
        assertTrue(hints.isAlliedWith(f2, f1, date));
        assertFalse(hints.isAlliedWith(f1, f3, date));
        assertFalse(hints.isAlliedWith(f2, f3, date));
    }

    @Test
    public void testIsRivalOf() {
        FactionHints hints = new FactionHints();
        Faction f1 = createTestFaction("F1");
        Faction f2 = createTestFaction("F2");
        Faction f3 = createTestFaction("F3");
        hints.addRivalry("", null, null, f1, f2);
        LocalDate date = LocalDate.now();

        assertTrue(hints.isRivalOf(f1, f2, date));
        assertTrue(hints.isRivalOf(f2, f1, date));
        assertFalse(hints.isRivalOf(f1, f3, date));
        assertFalse(hints.isRivalOf(f2, f3, date));
    }

    @Test
    public void testIsAtWarWith() {
        FactionHints hints = new FactionHints();
        Faction f1 = createTestFaction("F1");
        Faction f2 = createTestFaction("F2");
        Faction f3 = createTestFaction("F3");
        hints.addWar("", null, null, f1, f2);
        LocalDate date = LocalDate.now();

        assertTrue(hints.isAtWarWith(f1, f2, date));
        assertTrue(hints.isAtWarWith(f2, f1, date));
        assertFalse(hints.isAtWarWith(f1, f3, date));
        assertFalse(hints.isAtWarWith(f2, f3, date));
    }

    @Test
    public void testGetCurrentWar() {
        final String WAR_NAME = "World War XLII";
        FactionHints hints = new FactionHints();
        Faction f1 = createTestFaction("F1");
        Faction f2 = createTestFaction("F2");
        LocalDate start = LocalDate.of(3000, 1, 1);
        LocalDate end = LocalDate.of(3010, 1, 1);

        hints.addWar(WAR_NAME, start, end, f1, f2);

        LocalDate now = LocalDate.of(3005, 1, 1);

        assertEquals(WAR_NAME, hints.getCurrentWar(f1, f2, now));
        assertEquals(WAR_NAME, hints.getCurrentWar(f2, f1, now));
        // This test will fail if run between 3000 and 3010
        assertNull(hints.getCurrentWar(f1, f2, LocalDate.now()));
    }

    @Test
    public void testIsNeutralFactionException() {
        FactionHints hints = new FactionHints();
        Faction f1 = createTestFaction("F1");
        Faction f2 = createTestFaction("F2");
        Faction f3 = createTestFaction("F3");
        LocalDate now = LocalDate.now();

        hints.addNeutralFaction(f1);
        hints.addNeutralExceptions("", null, null, f1, f3);

        assertTrue(hints.isNeutral(f1));
        assertTrue(hints.isNeutral(f1, f2, now));
        assertFalse(hints.isNeutral(f1, f3, now));
    }

    @Test
    public void testGetContainedFactions() {
        FactionHints hints = new FactionHints();
        Faction outer = createTestFaction("outer");
        Faction inner = createTestFaction("inner");
        Faction opponent = createTestFaction("opponent");
        LocalDate now = LocalDate.now();

        hints.addContainedFaction(outer, inner, null, null, 0.5);

        assertTrue(hints.getContainedFactions(outer, now).contains(inner));
        assertEquals(hints.getContainedFactionHost(inner, now), outer);
        assertTrue(hints.isContainedFactionOpponent(outer, inner, opponent, now));
        Assertions.assertEquals(0.5, hints.getAltLocationFraction(outer, inner, now), RegionPerimeter.EPSILON);
    }

    @Test
    public void testIsContainedFactionOpponent() {
        FactionHints hints = new FactionHints();
        Faction outer = createTestFaction("outer");
        Faction inner = createTestFaction("inner");
        Faction opponent = createTestFaction("opponent");
        Faction nonOpponent = createTestFaction("nonOpponent");
        LocalDate now = LocalDate.now();

        hints.addContainedFaction(outer, inner, null, null, 0.5, Collections.singletonList(opponent));

        assertTrue(hints.isContainedFactionOpponent(outer, inner, opponent, now));
        assertFalse(hints.isContainedFactionOpponent(outer, inner, nonOpponent, now));
    }

    @Test
    public void testHasSameDiplomaticDataForIdenticalContent() {
        Faction factionOne = createTestFaction("F1");
        Faction factionTwo = createTestFaction("F2");
        Faction factionThree = createTestFaction("F3");
        LocalDate start = LocalDate.of(3050, 1, 1);
        LocalDate end = LocalDate.of(3052, 12, 31);

        FactionHints first = new FactionHints();
        FactionHints second = new FactionHints();
        for (FactionHints hints : new FactionHints[] { first, second }) {
            hints.addWar("Some War", start, end, factionOne, factionTwo);
            hints.addAlliance("", null, null, factionOne, factionThree);
            hints.addRivalry("", start, null, factionTwo, factionThree);
            hints.addNeutralFaction(factionThree);
            hints.addNeutralExceptions("", start, end, factionThree, factionOne);
            hints.addContainedFaction(factionOne, factionTwo, start, end, 0.25,
                  Collections.singletonList(factionThree));
        }

        assertTrue(first.hasSameDiplomaticData(second));
        assertTrue(second.hasSameDiplomaticData(first));
    }

    @Test
    public void testHasSameDiplomaticDataDetectsExtraWar() {
        Faction factionOne = createTestFaction("F1");
        Faction factionTwo = createTestFaction("F2");

        FactionHints first = new FactionHints();
        FactionHints second = new FactionHints();
        first.addWar("Some War", null, null, factionOne, factionTwo);
        second.addWar("Some War", null, null, factionOne, factionTwo);
        second.addWar("Custom War", LocalDate.of(3055, 1, 1), null, factionOne, factionTwo);

        assertFalse(first.hasSameDiplomaticData(second));
        assertFalse(second.hasSameDiplomaticData(first));
    }

    @Test
    public void testHasSameDiplomaticDataDetectsChangedDates() {
        Faction factionOne = createTestFaction("F1");
        Faction factionTwo = createTestFaction("F2");

        FactionHints first = new FactionHints();
        FactionHints second = new FactionHints();
        first.addWar("Some War", LocalDate.of(3050, 1, 1), LocalDate.of(3052, 12, 31), factionOne, factionTwo);
        second.addWar("Some War", LocalDate.of(3050, 1, 1), LocalDate.of(3053, 12, 31), factionOne, factionTwo);

        assertFalse(first.hasSameDiplomaticData(second));
    }

    @Test
    public void testHasSameDiplomaticDataDistinguishesRelationshipTypes() {
        Faction factionOne = createTestFaction("F1");
        Faction factionTwo = createTestFaction("F2");

        FactionHints first = new FactionHints();
        FactionHints second = new FactionHints();
        first.addWar("", null, null, factionOne, factionTwo);
        second.addRivalry("", null, null, factionOne, factionTwo);

        assertFalse(first.hasSameDiplomaticData(second),
              "A war and a rivalry between the same pair are different diplomatic data");
    }

    @Test
    public void testHasSameDiplomaticDataDetectsChangedFraction() {
        Faction outer = createTestFaction("outer");
        Faction inner = createTestFaction("inner");

        FactionHints first = new FactionHints();
        FactionHints second = new FactionHints();
        first.addContainedFaction(outer, inner, null, null, 0.25);
        second.addContainedFaction(outer, inner, null, null, 0.5);

        assertFalse(first.hasSameDiplomaticData(second));
    }
}
