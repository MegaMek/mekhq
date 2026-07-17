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
package mekhq.campaign.mission.newContract.targetFinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionBorderTracker;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HPGRating;
import org.junit.jupiter.api.Test;

/**
 * Direct unit tests for {@link ComStarMissionTargetFinder}, moved from the equivalent {@code getMissionTargetList}
 * ComStar scenarios in {@code RandomFactionGeneratorTest} now that the logic lives here.
 */
public class ComStarMissionTargetFinderTest {

    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);

    private static Faction createTestFaction(final String id) {
        Faction f = mock(Faction.class);
        when(f.getShortName()).thenReturn(id);
        return f;
    }

    private static PlanetarySystem createTestSystem(final double x, final double y, final Faction f) {
        PlanetarySystem p = mock(PlanetarySystem.class);
        when(p.getX()).thenReturn(x);
        when(p.getY()).thenReturn(y);
        when(p.getFactionSet(any())).thenReturn(Collections.singleton(f));
        when(p.getId()).thenReturn(String.format("(%3.1f,%3.1f)", x, y));
        return p;
    }

    /**
     * Builds a mock {@link ILocation} whose current system is controlled solely by the given faction.
     */
    private static ILocation createTestLocation(final Faction controllingFaction) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getFactionSet(any())).thenReturn(Collections.singleton(controllingFaction));
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(system);
        return location;
    }

    private static FactionBorderTracker buildTracker(List<PlanetarySystem> systems) {
        FactionBorderTracker tracker = new FactionBorderTracker(0, 0, -1) {
            @Override
            public Collection<PlanetarySystem> getSystemList() {
                return systems;
            }
        };
        tracker.setDefaultBorderSize(2.5, 10, 2.5);
        return tracker;
    }

    /**
     * Regression test: ComStar's HPG network matters more than its sovereign territory, so only its own systems or
     * systems with an A/B-rated HPG station are valid targets, regardless of borders.
     */
    @Test
    public void testFindValidSystemsOnlyIncludesOwnedOrHighRatedHPGSystems() {
        Faction comStarFaction = createTestFaction("CS");
        Faction otherFaction = createTestFaction("OTHER");

        PlanetarySystem comStarOwnedSystem = createTestSystem(0, 0, comStarFaction);
        PlanetarySystem highRatedHpgSystem = createTestSystem(1, 0, otherFaction);
        when(highRatedHpgSystem.getHPG(any())).thenReturn(HPGRating.A);
        PlanetarySystem lowRatedHpgSystem = createTestSystem(2, 0, otherFaction);
        when(lowRatedHpgSystem.getHPG(any())).thenReturn(HPGRating.C);
        PlanetarySystem noHpgSystem = createTestSystem(3, 0, otherFaction);

        List<PlanetarySystem> systems = List.of(comStarOwnedSystem, highRatedHpgSystem, lowRatedHpgSystem,
              noHpgSystem);
        FactionBorderTracker tracker = buildTracker(systems);
        ComStarMissionTargetFinder finder = new ComStarMissionTargetFinder(tracker);
        ILocation location = createTestLocation(comStarFaction);

        List<PlanetarySystem> targets = finder.findValidSystems(comStarFaction, location, tracker.getRadius(),
              TEST_DATE);

        assertEquals(Set.of(comStarOwnedSystem, highRatedHpgSystem), new HashSet<>(targets),
              "Only ComStar-owned systems or A/B-rated HPG systems should be valid targets against ComStar");
    }

    /**
     * Regression test: with no ComStar-owned or A/B-rated HPG system in range, there is no valid target &mdash; unlike
     * the pirate cases, this is a hard restriction with no fallback to a broader search.
     */
    @Test
    public void testFindValidSystemsReturnsEmptyWhenNoQualifyingSystemsInRange() {
        Faction comStarFaction = createTestFaction("CS");
        Faction otherFaction = createTestFaction("OTHER");

        PlanetarySystem lowRatedHpgSystem = createTestSystem(0, 0, otherFaction);
        when(lowRatedHpgSystem.getHPG(any())).thenReturn(HPGRating.C);

        List<PlanetarySystem> systems = List.of(lowRatedHpgSystem);
        FactionBorderTracker tracker = buildTracker(systems);
        ComStarMissionTargetFinder finder = new ComStarMissionTargetFinder(tracker);
        ILocation location = createTestLocation(comStarFaction);

        List<PlanetarySystem> targets = finder.findValidSystems(comStarFaction, location, tracker.getRadius(),
              TEST_DATE);

        assertTrue(targets.isEmpty(),
              "With no ComStar-owned or A/B-rated HPG system in range, there should be no valid target");
    }

    /**
     * Regression test: a location with no current system (e.g. a force in deep transit) yields an empty result rather
     * than throwing.
     */
    @Test
    public void testFindValidSystemsReturnsEmptyWhenLocationHasNoCurrentSystem() {
        Faction comStarFaction = createTestFaction("CS");
        FactionBorderTracker tracker = buildTracker(List.of());
        ComStarMissionTargetFinder finder = new ComStarMissionTargetFinder(tracker);
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(null);

        List<PlanetarySystem> targets = finder.findValidSystems(comStarFaction, location, tracker.getRadius(),
              TEST_DATE);

        assertTrue(targets.isEmpty(), "A location with no current system should yield no valid targets");
    }
}
