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
import java.util.List;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionBorderTracker;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.PlanetaryType;
import org.junit.jupiter.api.Test;

/**
 * Direct unit tests for {@link PirateMissionTargetFinder}'s border-preference tiers, moved from the equivalent
 * {@code getMissionTargetList} pirate scenarios in {@code RandomFactionGeneratorTest} now that the logic lives here.
 */
public class PirateMissionTargetFinderTest {

    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);

    private static Faction createTestFaction(final String id, final boolean periphery) {
        Faction f = mock(Faction.class);
        when(f.getShortName()).thenReturn(id);
        when(f.isPeriphery()).thenReturn(periphery);
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
     * Builds a system with no faction data at all (an empty {@code Set}, not a singleton placeholder faction) &mdash;
     * the actual shape returned by {@link PlanetarySystem#getFactionSet} for many real, populated systems that were
     * never tagged with an owner in the source data.
     */
    private static PlanetarySystem createUnownedTestSystem(final double x, final double y) {
        PlanetarySystem p = mock(PlanetarySystem.class);
        when(p.getX()).thenReturn(x);
        when(p.getY()).thenReturn(y);
        when(p.getFactionSet(any())).thenReturn(Collections.emptySet());
        when(p.getId()).thenReturn(String.format("(%3.1f,%3.1f)", x, y));
        return p;
    }

    /**
     * Builds a connector system (see {@link PlanetarySystem#isConnector()}) with no faction data, whose primary planet
     * is of the given type.
     */
    private static PlanetarySystem createConnectorTestSystem(final double x, final double y,
          final PlanetaryType planetType) {
        Planet primaryPlanet = mock(Planet.class);
        when(primaryPlanet.getPlanetType()).thenReturn(planetType);

        PlanetarySystem p = mock(PlanetarySystem.class);
        when(p.getX()).thenReturn(x);
        when(p.getY()).thenReturn(y);
        when(p.getFactionSet(any())).thenReturn(Collections.emptySet());
        when(p.getId()).thenReturn(String.format("(%3.1f,%3.1f)", x, y));
        when(p.isConnector()).thenReturn(true);
        when(p.getPrimaryPlanet()).thenReturn(primaryPlanet);
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
     * Regression test: a pirate defender is more likely holed up in genuinely lawless, uncontested space than on a
     * system it (or anyone else) officially claims, so a nearby empty system should be preferred over the pirate
     * faction's own directly-owned system.
     */
    @Test
    public void testFindDefenderTargetsPrefersEmptySystem() {
        Faction attackerFaction = createTestFaction("ATTACKER", false);
        Faction pirateFaction = createTestFaction("PIR", false);
        Faction emptyFaction = createTestFaction("UND", false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem pirateOwnedSystem = createTestSystem(1, 0, pirateFaction);
        PlanetarySystem emptySystem = createTestSystem(2, 0, emptyFaction);

        List<PlanetarySystem> systems = List.of(attackerSystem, pirateOwnedSystem, emptySystem);
        FactionBorderTracker tracker = buildTracker(systems);
        PirateMissionTargetFinder finder = new PirateMissionTargetFinder(tracker);
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.findDefenderTargets(attackerFaction, pirateFaction, location,
              tracker.getRadius(), TEST_DATE);

        assertEquals(List.of(emptySystem), targets,
              "A pirate defender should prefer a nearby empty/lawless system over its own directly-owned system");
    }

    /**
     * Regression test: a system with no faction data at all (an empty {@link java.util.Set}, as real, populated but
     * never-owned systems actually report) must still count as an empty/lawless system, not just one whose sole faction
     * is a recognized placeholder code like "UND".
     */
    @Test
    public void testFindDefenderTargetsTreatsSystemWithNoFactionDataAsEmpty() {
        Faction attackerFaction = createTestFaction("ATTACKER", false);
        Faction pirateFaction = createTestFaction("PIR", false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem pirateOwnedSystem = createTestSystem(1, 0, pirateFaction);
        PlanetarySystem unownedSystem = createUnownedTestSystem(2, 0);

        List<PlanetarySystem> systems = List.of(attackerSystem, pirateOwnedSystem, unownedSystem);
        FactionBorderTracker tracker = buildTracker(systems);
        PirateMissionTargetFinder finder = new PirateMissionTargetFinder(tracker);
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.findDefenderTargets(attackerFaction, pirateFaction, location,
              tracker.getRadius(), TEST_DATE);

        assertEquals(List.of(unownedSystem), targets,
              "A system with no faction data at all should still be treated as empty/lawless space");
    }

    /**
     * Regression test: connector systems are synthetic jump-path waypoints with no lore behind them and are normally
     * excluded from mission targeting, but a pirate defender may still hide out at one if its primary planet is
     * terrestrial (i.e. a place a raiding band could plausibly occupy).
     */
    @Test
    public void testFindDefenderTargetsIncludesTerrestrialConnectorSystem() {
        Faction attackerFaction = createTestFaction("ATTACKER", false);
        Faction pirateFaction = createTestFaction("PIR", false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem terrestrialConnector = createConnectorTestSystem(1, 0, PlanetaryType.TERRESTRIAL);

        List<PlanetarySystem> systems = List.of(attackerSystem, terrestrialConnector);
        FactionBorderTracker tracker = buildTracker(systems);
        PirateMissionTargetFinder finder = new PirateMissionTargetFinder(tracker);
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.findDefenderTargets(attackerFaction, pirateFaction, location,
              tracker.getRadius(), TEST_DATE);

        assertEquals(List.of(terrestrialConnector), targets,
              "A connector system with a terrestrial primary planet should be a valid pirate hideout");
    }

    /**
     * Regression test: a connector system whose primary planet is not terrestrial (e.g. a gas giant or asteroid belt)
     * is not a plausible pirate hideout, so it must not be picked even though it has no faction data.
     */
    @Test
    public void testFindDefenderTargetsExcludesNonTerrestrialConnectorSystem() {
        Faction attackerFaction = createTestFaction("ATTACKER", false);
        Faction pirateFaction = createTestFaction("PIR", false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem gasGiantConnector = createConnectorTestSystem(1, 0, PlanetaryType.GAS_GIANT);

        List<PlanetarySystem> systems = List.of(attackerSystem, gasGiantConnector);
        FactionBorderTracker tracker = buildTracker(systems);
        PirateMissionTargetFinder finder = new PirateMissionTargetFinder(tracker);
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.findDefenderTargets(attackerFaction, pirateFaction, location,
              tracker.getRadius(), TEST_DATE);

        assertTrue(targets.isEmpty(),
              "A connector system with a non-terrestrial primary planet should never be picked as a pirate hideout");
    }

    /**
     * Regression test: with no empty system nearby, a pirate defender should favor the attacker's own border with a
     * Periphery neighbor (the classic "past the frontier" pirate haunt) over a border with a core neighbor.
     */
    @Test
    public void testFindDefenderTargetsPrefersPeripheryBorder() {
        Faction attackerFaction = createTestFaction("ATTACKER", false);
        Faction pirateFaction = createTestFaction("PIR", false);
        Faction peripheryNeighbor = createTestFaction("PER", true);
        Faction coreNeighbor = createTestFaction("CORE", false);

        PlanetarySystem attackerSystemNearPeriphery = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem peripherySystem = createTestSystem(1, 0, peripheryNeighbor);
        PlanetarySystem attackerSystemNearCore = createTestSystem(20, 0, attackerFaction);
        PlanetarySystem coreSystem = createTestSystem(21, 0, coreNeighbor);

        List<PlanetarySystem> systems = List.of(attackerSystemNearPeriphery, peripherySystem, attackerSystemNearCore,
              coreSystem);
        FactionBorderTracker tracker = buildTracker(systems);
        PirateMissionTargetFinder finder = new PirateMissionTargetFinder(tracker);
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.findDefenderTargets(attackerFaction, pirateFaction, location,
              tracker.getRadius(), TEST_DATE);

        assertEquals(List.of(attackerSystemNearPeriphery), targets,
              "A pirate defender should prefer the attacker's border with a Periphery neighbor over one with a core "
                    + "neighbor");
    }

    /**
     * Regression test: with no empty system and no Periphery neighbor, a pirate defender should fall back to the
     * attacker's border with any neighbor rather than returning nothing.
     */
    @Test
    public void testFindDefenderTargetsFallsBackToAnyBorder() {
        Faction attackerFaction = createTestFaction("ATTACKER", false);
        Faction pirateFaction = createTestFaction("PIR", false);
        Faction coreNeighbor = createTestFaction("CORE", false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem coreSystem = createTestSystem(1, 0, coreNeighbor);

        List<PlanetarySystem> systems = List.of(attackerSystem, coreSystem);
        FactionBorderTracker tracker = buildTracker(systems);
        PirateMissionTargetFinder finder = new PirateMissionTargetFinder(tracker);
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.findDefenderTargets(attackerFaction, pirateFaction, location,
              tracker.getRadius(), TEST_DATE);

        assertEquals(List.of(attackerSystem), targets,
              "With no Periphery neighbor, a pirate defender should fall back to the attacker's border with any "
                    + "neighbor");
    }

    /**
     * Regression test: a pirate attacker raids from the frontier, so it favors the defender's border with a Periphery
     * neighbor over a border with a core neighbor.
     */
    @Test
    public void testFindAttackerTargetsPrefersPeripheryBorder() {
        Faction pirateFaction = createTestFaction("PIR", false);
        Faction defenderFaction = createTestFaction("DEFENDER", false);
        Faction peripheryNeighbor = createTestFaction("PER", true);
        Faction coreNeighbor = createTestFaction("CORE", false);

        PlanetarySystem defenderSystemNearPeriphery = createTestSystem(0, 0, defenderFaction);
        PlanetarySystem peripherySystem = createTestSystem(1, 0, peripheryNeighbor);
        PlanetarySystem defenderSystemNearCore = createTestSystem(20, 0, defenderFaction);
        PlanetarySystem coreSystem = createTestSystem(21, 0, coreNeighbor);

        List<PlanetarySystem> systems = List.of(defenderSystemNearPeriphery, peripherySystem, defenderSystemNearCore,
              coreSystem);
        FactionBorderTracker tracker = buildTracker(systems);
        PirateMissionTargetFinder finder = new PirateMissionTargetFinder(tracker);
        ILocation location = createTestLocation(defenderFaction);

        List<PlanetarySystem> targets = finder.findAttackerTargets(pirateFaction, defenderFaction, location,
              tracker.getRadius());

        assertEquals(List.of(defenderSystemNearPeriphery), targets,
              "A pirate attacker should prefer the defender's border with a Periphery neighbor over one with a core "
                    + "neighbor");
    }

    /**
     * Regression test: with no Periphery neighbor, a pirate attacker should fall back to the defender's border with any
     * neighbor rather than returning nothing.
     */
    @Test
    public void testFindAttackerTargetsFallsBackToAnyBorder() {
        Faction pirateFaction = createTestFaction("PIR", false);
        Faction defenderFaction = createTestFaction("DEFENDER", false);
        Faction coreNeighbor = createTestFaction("CORE", false);

        PlanetarySystem defenderSystem = createTestSystem(0, 0, defenderFaction);
        PlanetarySystem coreSystem = createTestSystem(1, 0, coreNeighbor);

        List<PlanetarySystem> systems = List.of(defenderSystem, coreSystem);
        FactionBorderTracker tracker = buildTracker(systems);
        PirateMissionTargetFinder finder = new PirateMissionTargetFinder(tracker);
        ILocation location = createTestLocation(defenderFaction);

        List<PlanetarySystem> targets = finder.findAttackerTargets(pirateFaction, defenderFaction, location,
              tracker.getRadius());

        assertEquals(List.of(defenderSystem), targets,
              "With no Periphery neighbor, a pirate attacker should fall back to the defender's border with any "
                    + "neighbor");
    }

    /**
     * Regression test: when the defender has no identifiable border at all (no neighboring factions), the attacker tier
     * finds nothing, leaving the broader "strike anywhere the defender holds" fallback to {@link MissionTargetFinder}.
     */
    @Test
    public void testFindAttackerTargetsReturnsEmptyWhenNoBorder() {
        Faction pirateFaction = createTestFaction("PIR", false);
        Faction defenderFaction = createTestFaction("DEFENDER", false);

        PlanetarySystem defenderSystem1 = createTestSystem(0, 0, defenderFaction);
        PlanetarySystem defenderSystem2 = createTestSystem(1, 0, defenderFaction);

        List<PlanetarySystem> systems = List.of(defenderSystem1, defenderSystem2);
        FactionBorderTracker tracker = buildTracker(systems);
        PirateMissionTargetFinder finder = new PirateMissionTargetFinder(tracker);
        ILocation location = createTestLocation(defenderFaction);

        List<PlanetarySystem> targets = finder.findAttackerTargets(pirateFaction, defenderFaction, location,
              tracker.getRadius());

        assertTrue(targets.isEmpty(),
              "With no neighboring faction to form a border, there should be no border-based target");
    }
}
