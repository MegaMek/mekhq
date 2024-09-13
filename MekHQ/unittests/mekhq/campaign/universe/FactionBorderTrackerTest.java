/*
 * Copyright (c) 2018 - The MegaMek Team. All Rights Reserved.
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

import mekhq.campaign.universe.FactionBorderTracker.RegionHex;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FactionBorderTrackerTest {

    private Faction factionUs = createFaction("us", false, false);
    private Faction factionThem = createFaction("them", false, false);

    // Builds a sample universe with a faction "us" with one planet at (0, 0) and
    // faction
    // "them" with planets on a 4x3 grid with 2 ly distance between adjacent planets
    private FactionBorderTracker buildTestTracker() {
        List<PlanetarySystem> systems = new ArrayList<>();
        for (int x = -3; x <= 3; x += 2) {
            for (int y = -2; y <= 2; y += 2) {
                systems.add(createSystem(x, y, factionThem));
            }
        }
        systems.add(createSystem(0, 0, factionUs));

        return new FactionBorderTracker() {
            @Override
            protected Collection<PlanetarySystem> getSystemList() {
                return systems;
            }
        };
    }

    private Faction createFaction(final String key, final boolean periphery, final boolean clan) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn(key);
        when(faction.isPeriphery()).thenReturn(periphery);
        when(faction.isClan()).thenReturn(clan);
        return faction;
    }

    private PlanetarySystem createSystem(final double x, final double y, Faction owner) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getX()).thenReturn(x);
        when(system.getY()).thenReturn(y);
        String id = String.format("%f, %f", x, y);
        when(system.getId()).thenReturn(id);
        when(system.getFactionSet(ArgumentMatchers.any())).thenReturn(Collections.singleton(owner));
        return system;
    }

    @Test
    public void testFactionBorderTrackerAllPlanets() throws InterruptedException {
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 1, 1);

        List<PlanetarySystem> border = tracker.getBorderSystems(factionUs, factionThem);

        assertEquals(tracker.getBorders(factionUs).getSystems().size(), 1);
        assertEquals(tracker.getBorders(factionThem).getSystems().size(), 12);
        assertEquals(border.size(), 2);
        for (PlanetarySystem p : border) {
            assertEquals(Math.abs(p.getX()), 1, RegionPerimeter.EPSILON);
            assertEquals(p.getY(), 0, RegionPerimeter.EPSILON);
        }
    }

    @Test
    public void testFactionBorderTrackerSmallerRegion() throws InterruptedException {
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 1, 1);
        tracker.setRegionRadius(1.1);

        List<PlanetarySystem> border = tracker.getBorderSystems(factionUs, factionThem);

        assertEquals(tracker.getBorders(factionUs).getSystems().size(), 1);
        assertEquals(tracker.getBorders(factionThem).getSystems().size(), 2);
        assertEquals(border.size(), 2);
    }

    @Test
    public void testFactionBorderTrackerEmptyRegion() throws InterruptedException {
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 1, 1);
        tracker.setRegionRadius(10);
        tracker.setRegionCenter(50, 0);

        List<PlanetarySystem> border = tracker.getBorderSystems(factionUs, factionThem);

        assertNull(tracker.getBorders(factionUs));
        assertNull(tracker.getBorders(factionThem));
        assertEquals(border.size(), 0);
    }

    @Test
    public void testDefaultBorderSize() {
        Faction is = createFaction("is", false, false);
        Faction periphery = createFaction("periphery", true, false);
        Faction clan = createFaction("clan", false, true);
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 2, 3);

        assertEquals(tracker.getBorderSize(is), 1, RegionPerimeter.EPSILON);
        assertEquals(tracker.getBorderSize(periphery), 2, RegionPerimeter.EPSILON);
        assertEquals(tracker.getBorderSize(clan), 3, RegionPerimeter.EPSILON);
    }

    @Test
    public void testSetBorderSize() {
        Faction is = createFaction("is", false, false);
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 2, 3);
        tracker.setBorderSize(is, 30);

        assertEquals(tracker.getBorderSize(is), 30, RegionPerimeter.EPSILON);
    }

    @Test
    public void testHexRegionContainsReturnsFalseOutsideBoundingRect() {
        RegionHex hex = new RegionHex(0, 0, 1.0);

        assertFalse(hex.contains(0, 2));
        assertFalse(hex.contains(0, -2));
        assertFalse(hex.contains(2, 0.5));
        assertFalse(hex.contains(-2, 0.5));
    }

    @Test
    public void testHexRegionContainsReturnsTrueInnerBoundingRect() {
        RegionHex hex = new RegionHex(0, 0, 1.0);

        assertTrue(hex.contains(0.25, 0.5));
        assertTrue(hex.contains(0.25, -0.5));
        assertTrue(hex.contains(-0.25, 0.5));
        assertTrue(hex.contains(-0.25, -0.5));
    }

    @Test
    public void testHexRegionContainsNearSides() {
        RegionHex hex = new RegionHex(0, 0, 1.0);

        assertTrue(hex.contains(0.9, 0.1));
        assertFalse(hex.contains(0.9, 0.9));
    }
}
