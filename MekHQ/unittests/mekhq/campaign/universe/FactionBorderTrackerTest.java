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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.universe;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class FactionBorderTrackerTest {

    private Faction factionUs;
    private Faction factionThem;
    
    @Before
    public void init() {
        factionUs = createFaction("us", false, false);
        factionThem = createFaction("them", false, false);
    }
    
    // Builds a sample universe with a faction "us" with one planet at (0, 0) and faction
    // "them" with planets on a 4x3 grid with 2 ly distance between adjacent planets
    private FactionBorderTracker buildTestTracker() {
        List<PlanetarySystem> systems = new ArrayList<>();
        for (int x = -3; x <= 3; x += 2) {
            for (int y = -2; y <= 2; y += 2) {
                systems.add(createSystem(x, y, factionThem));
            }
        }
        systems.add(createSystem(0, 0, factionUs));
        
        FactionBorderTracker tracker = new FactionBorderTracker() {
            @Override
            protected Collection<PlanetarySystem> getSystemList() {
                return systems;
            }
        };
        return tracker;
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
        when(system.getFactionSet(any())).thenReturn(Collections.singleton(owner));
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
        
        assertEquals(tracker.getBorders(factionUs), null);
        assertEquals(tracker.getBorders(factionThem), null);
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
        FactionBorderTracker.RegionHex hex = new FactionBorderTracker.RegionHex(0, 0, 1.0);
        
        assertFalse(hex.contains(0, 2));
        assertFalse(hex.contains(0, -2));
        assertFalse(hex.contains(2, 0.5));
        assertFalse(hex.contains(-2, 0.5));
    }

    @Test
    public void testHexRegionContainsReturnsTrueInnerBoundingRect() {
        FactionBorderTracker.RegionHex hex = new FactionBorderTracker.RegionHex(0, 0, 1.0);
        
        assertTrue(hex.contains(0.25, 0.5));
        assertTrue(hex.contains(0.25, -0.5));
        assertTrue(hex.contains(-0.25, 0.5));
        assertTrue(hex.contains(-0.25, -0.5));
    }

    @Test
    public void testHexRegionContainsNearSides() {
        FactionBorderTracker.RegionHex hex = new FactionBorderTracker.RegionHex(0, 0, 1.0);
        
        assertTrue(hex.contains(0.9, 0.1));
        assertFalse(hex.contains(0.9, 0.9));
    }

}
