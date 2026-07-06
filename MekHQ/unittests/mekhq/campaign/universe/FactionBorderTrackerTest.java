/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.FactionBorderTracker.RegionHex;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

public class FactionBorderTrackerTest {

    private final Faction factionUs = createFaction("us", false, false);
    private final Faction factionThem = createFaction("them", false, false);

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
            public Collection<PlanetarySystem> getSystemList() {
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
        // Real Euclidean distance based on the stubbed coordinates, so radius-based filtering (used by the
        // getBorders/getBorderSystems overloads that take an ILocation) behaves like production instead of
        // Mockito's unstubbed 0.0 default.
        when(system.getDistanceTo(ArgumentMatchers.any(PlanetarySystem.class))).thenAnswer(invocation -> {
            PlanetarySystem other = invocation.getArgument(0);
            return Math.hypot(x - other.getX(), y - other.getY());
        });
        return system;
    }

    /**
     * Builds a mock {@link ILocation} whose current system sits at the given coordinates. The owning faction is
     * irrelevant here since the location's system is only ever used as the origin point for distance checks, never
     * returned as part of a scanned region.
     */
    private ILocation createLocationAt(final double x, final double y) {
        PlanetarySystem system = createSystem(x, y, factionUs);
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(system);
        return location;
    }

    @Test
    public void testFactionBorderTrackerAllPlanets() {
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 1, 1);

        List<PlanetarySystem> border = tracker.getBorderSystems(factionUs, factionThem);

        assertEquals(1, tracker.getBorders(factionUs).getSystems().size());
        assertEquals(12, tracker.getBorders(factionThem).getSystems().size());
        assertEquals(2, border.size());
        for (PlanetarySystem p : border) {
            assertEquals(1, Math.abs(p.getX()), RegionPerimeter.EPSILON);
            assertEquals(0, p.getY(), RegionPerimeter.EPSILON);
        }
    }

    @Test
    public void testFactionBorderTrackerSmallerRegion() {
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 1, 1);
        tracker.setRegionRadius(1.1);

        List<PlanetarySystem> border = tracker.getBorderSystems(factionUs, factionThem);

        assertEquals(1, tracker.getBorders(factionUs).getSystems().size());
        assertEquals(2, tracker.getBorders(factionThem).getSystems().size());
        assertEquals(2, border.size());
    }

    @Test
    public void testFactionBorderTrackerEmptyRegion() {
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 1, 1);
        tracker.setRegionRadius(10);
        tracker.setRegionCenter(50, 0);

        List<PlanetarySystem> border = tracker.getBorderSystems(factionUs, factionThem);

        assertNull(tracker.getBorders(factionUs));
        assertNull(tracker.getBorders(factionThem));
        assertEquals(0, border.size());
    }

    /**
     * Regression test: {@link FactionBorderTracker#getBorders(Faction, ILocation, double)} computes a fresh result
     * around an arbitrary location, entirely independent of the tracker's persistently cached region &mdash; even
     * when that cached region is centered somewhere else (or empty) at the time of the call.
     */
    @Test
    public void testGetBordersByLocationIgnoresCachedRegion() {
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 1, 1);
        // Push the tracker's cached region far away and tight, so the cached view has no presence for either faction.
        tracker.setRegionRadius(1);
        tracker.setRegionCenter(50, 0);
        assertNull(tracker.getBorders(factionUs), "Sanity check: cached region should have no presence for factionUs");
        assertNull(tracker.getBorders(factionThem), "Sanity check: cached region should have no presence for factionThem");

        ILocation location = createLocationAt(0, 0);

        FactionBorders usBorders = tracker.getBorders(factionUs, location, 1);
        FactionBorders themBorders = tracker.getBorders(factionThem, location, 1);

        assertNotNull(usBorders, "factionUs controls a system at the search origin");
        assertEquals(1, usBorders.getSystems().size());
        assertNotNull(themBorders, "factionThem controls systems within radius 1 of the search origin");
        assertEquals(2, themBorders.getSystems().size());
        // The cached region itself must remain untouched by the location-based query.
        assertNull(tracker.getBorders(factionUs));
        assertNull(tracker.getBorders(factionThem));
    }

    /**
     * Regression test: when no system of the requested faction falls within the given radius of the location, the
     * result is {@code null} rather than an empty, non-null {@link FactionBorders}.
     */
    @Test
    public void testGetBordersByLocationReturnsNullWhenFactionHasNoPresenceInRange() {
        FactionBorderTracker tracker = buildTestTracker();
        ILocation location = createLocationAt(0, 0);

        assertNull(tracker.getBorders(factionThem, location, 0.5),
              "No factionThem system is within 0.5 ly of the origin");
    }

    /**
     * Regression test: a location with no current system (e.g. a force in deep transit) yields {@code null}/an empty
     * result rather than throwing, for both the faction-borders and the two-faction proximity overloads.
     */
    @Test
    public void testGetBordersByLocationReturnsNullWhenLocationHasNoCurrentSystem() {
        FactionBorderTracker tracker = buildTestTracker();
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(null);

        assertNull(tracker.getBorders(factionUs, location, 10));
        assertNull(tracker.getBorders(factionUs, location));
        assertTrue(tracker.getBorderSystems(factionUs, factionThem, location, 10).isEmpty());
    }

    /**
     * Regression test: {@link FactionBorderTracker#getBorders(Faction, ILocation)} (no explicit radius) uses the
     * tracker's configured {@link FactionBorderTracker#getRadius()} as the search radius, including the "negative
     * radius means the whole map" convention.
     */
    @Test
    public void testGetBordersByLocationDefaultOverloadUsesTrackerRadius() {
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 1, 1);
        ILocation location = createLocationAt(0, 0);

        tracker.setRegionRadius(1);
        FactionBorders themNarrow = tracker.getBorders(factionThem, location);
        assertNotNull(themNarrow);
        assertEquals(2, themNarrow.getSystems().size(),
              "getBorders(Faction, ILocation) should use the tracker's configured radius as the search radius");

        tracker.setRegionRadius(-1);
        FactionBorders themAll = tracker.getBorders(factionThem, location);
        assertNotNull(themAll);
        assertEquals(12, themAll.getSystems().size(),
              "A negative tracker radius should include every system in the location-based search too");
    }

    /**
     * Regression test: {@link FactionBorderTracker#getBorderSystems(Faction, Faction, ILocation, double)} matches the
     * cached, region-based {@link FactionBorderTracker#getBorderSystems(Faction, Faction)} result when the location and
     * radius describe the same area, while remaining independent of the tracker's cached region.
     */
    @Test
    public void testGetBorderSystemsByLocationMatchesRegionBasedResult() {
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 1, 1);
        // Push the cached region far away; the location-based call below must not depend on it.
        tracker.setRegionRadius(1);
        tracker.setRegionCenter(50, 0);

        ILocation location = createLocationAt(0, 0);

        List<PlanetarySystem> border = tracker.getBorderSystems(factionUs, factionThem, location, 1);

        assertEquals(2, border.size());
        for (PlanetarySystem p : border) {
            assertEquals(1, Math.abs(p.getX()), RegionPerimeter.EPSILON);
            assertEquals(0, p.getY(), RegionPerimeter.EPSILON);
        }
    }

    @Test
    public void testDefaultBorderSize() {
        Faction is = createFaction("is", false, false);
        Faction periphery = createFaction("periphery", true, false);
        Faction clan = createFaction("clan", false, true);
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 2, 3);

        assertEquals(1, tracker.getBorderSize(is), RegionPerimeter.EPSILON);
        assertEquals(2, tracker.getBorderSize(periphery), RegionPerimeter.EPSILON);
        assertEquals(3, tracker.getBorderSize(clan), RegionPerimeter.EPSILON);
    }

    @Test
    public void testSetBorderSize() {
        Faction is = createFaction("is", false, false);
        FactionBorderTracker tracker = buildTestTracker();
        tracker.setDefaultBorderSize(1, 2, 3);
        tracker.setBorderSize(is, 30);

        assertEquals(30, tracker.getBorderSize(is), RegionPerimeter.EPSILON);
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
