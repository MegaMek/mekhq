/*
 * Copyright (c) 2018 The MegaMek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;

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

        assertEquals(hints.getCurrentWar(f1, f2, now), WAR_NAME);
        assertEquals(hints.getCurrentWar(f2, f1, now), WAR_NAME);
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
        assertEquals(hints.getAltLocationFraction(outer, inner, now), 0.5, RegionPerimeter.EPSILON);
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
}
