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
package mekhq.campaign.mission.newContract;

import static mekhq.MHQConstants.FORTRESS_REPUBLIC_END;
import static mekhq.MHQConstants.FORTRESS_REPUBLIC_START;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionBorderTracker;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionHints.FactionHints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MissionTargetFinder}'s own orchestration logic: territorial-host resolution, the special-attacker and
 * rebel-defender fallbacks, the shared-border/contained-faction/closest-system chain, and Fortress Republic filtering.
 * Delegation to {@link PirateMissionTargetFinder}/{@link ComStarMissionTargetFinder} is confirmed here at the wiring
 * level only; their own tiered fallback logic is tested directly in {@code PirateMissionTargetFinderTest} and
 * {@code ComStarMissionTargetFinderTest}.
 * <p>Moved from the equivalent {@code getMissionTargetList} scenarios in {@code RandomFactionGeneratorTest} now that
 * this logic lives here.</p>
 */
public class MissionTargetFinderTest {

    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);

    private Faction isFaction;
    private Faction clanFaction;
    private Faction peripheryFaction;
    private Faction innerISFaction;
    private FactionBorderTracker borderTracker;

    /**
     * Backs the mocked {@link Factions#getInstance()} roster; {@link MissionTargetFinder}'s {@code systemsOf} helper
     * consults it to resolve the Republic of the Sphere faction for Fortress Republic filtering.
     */
    private List<Faction> allFactions;

    @BeforeEach
    public void init() {
        borderTracker = createTestBorderTracker();
    }

    @AfterEach
    public void tearDown() {
        Factions.setInstance(null);
    }

    private FactionBorderTracker createTestBorderTracker() {
        isFaction = createTestFaction("IS", false, false);
        clanFaction = createTestFaction("Clan", false, true);
        peripheryFaction = createTestFaction("Periphery", true, false);
        innerISFaction = createTestFaction("IS2", false, false);

        allFactions = new ArrayList<>(List.of(isFaction, clanFaction, peripheryFaction, innerISFaction));
        Factions factions = mock(Factions.class);
        when(factions.getFactions()).thenReturn(allFactions);
        when(factions.getFaction(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            return allFactions.stream().filter(f -> f.getShortName().equals(code)).findFirst().orElse(null);
        });
        Factions.setInstance(factions);

        List<PlanetarySystem> systems = new ArrayList<>();
        for (int x = -2; x < 3; x++) {
            for (int y = -2; y < 3; y++) {
                if (x < 0) {
                    systems.add(createTestSystem(x, y, isFaction));
                }
                if (x > 0) {
                    systems.add(createTestSystem(x, y, clanFaction));
                }
                systems.add(createTestSystem(x, y + 10, peripheryFaction));
            }
        }

        FactionBorderTracker tracker = buildTestTracker(systems);
        return tracker;
    }

    private static Faction createTestFaction(final String id, final boolean periphery, final boolean clan) {
        Faction f = mock(Faction.class);
        when(f.getShortName()).thenReturn(id);
        when(f.isPeriphery()).thenReturn(periphery);
        when(f.isClan()).thenReturn(clan);
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

    private FactionHints createTestHints() {
        FactionHints hints = new FactionHints();
        hints.addContainedFaction(isFaction, innerISFaction, null, null, 0.5);
        return hints;
    }

    private MissionTargetFinder createTestFinder() {
        return new MissionTargetFinder(borderTracker, createTestHints());
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

    @Test
    public void testFind() {
        MissionTargetFinder finder = createTestFinder();
        ILocation location = createTestLocation(isFaction);

        assertFalse(finder.find(peripheryFaction, isFaction, location, TEST_DATE).isEmpty());
        assertFalse(finder.find(peripheryFaction, innerISFaction, location, TEST_DATE).isEmpty());
        assertFalse(finder.find(innerISFaction, peripheryFaction, location, TEST_DATE).isEmpty());
    }

    /**
     * Regression test: when neither a direct border nor a contained-faction proxy border exists between the two
     * factions, find() should fall back to the defender's system physically closest to the attacker instead of
     * returning nothing.
     */
    @Test
    public void testFindFallsBackToClosestDefenderSystem() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        // Placed far apart in x/y so the geometric border-adjacency check excludes them before ever consulting
        // getDistanceTo(), and no contained-faction relationship is configured (fresh FactionHints below).
        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem nearDefenderSystem = createTestSystem(1000, 1000, defenderFaction);
        PlanetarySystem farDefenderSystem = createTestSystem(2000, 2000, defenderFaction);

        when(nearDefenderSystem.getDistanceTo(attackerSystem)).thenReturn(50.0);
        when(farDefenderSystem.getDistanceTo(attackerSystem)).thenReturn(500.0);

        List<PlanetarySystem> systems = List.of(attackerSystem, nearDefenderSystem, farDefenderSystem);
        FactionBorderTracker tracker = buildTestTracker(systems);

        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE);

        assertEquals(List.of(nearDefenderSystem), targets,
              "With no border or contained-faction relationship, the closest defender system should be the sole target");
    }

    /**
     * Regression test: a defender guaranteed as a valid target regardless of local presence (e.g. a faction at war with
     * the attacker, chosen by {@code RandomFactionGenerator#buildEnemyMap} even with no systems in the search area)
     * must still resolve to a real system via the closest-system fallback, rather than leaving contract generation with
     * no valid location. The defender's own search must not be limited to the local search radius, even though the
     * attacker's is.
     */
    @Test
    public void testFindClosestDefenderSystemFallbackIgnoresRadiusForDefender() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        // Far outside the tracker's 5 ly search radius below - simulating a war partner with no local presence.
        PlanetarySystem distantDefenderSystem = createTestSystem(1000, 1000, defenderFaction);
        when(attackerSystem.getDistanceTo(any(PlanetarySystem.class))).thenReturn(0.0);
        when(distantDefenderSystem.getDistanceTo(any(PlanetarySystem.class))).thenReturn(2000.0);

        List<PlanetarySystem> systems = List.of(attackerSystem, distantDefenderSystem);
        FactionBorderTracker tracker = buildTestTracker(systems, 5);

        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(attackerSystem);

        List<PlanetarySystem> targets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE);

        assertEquals(List.of(distantDefenderSystem), targets,
              "A defender with no presence within the search radius should still resolve via the whole-map "
                    + "closest-system fallback, rather than yielding no target at all");
    }

    /**
     * Regression test: a pirate defender is routed to {@link PirateMissionTargetFinder}, whose own tiers are tested
     * directly in {@code PirateMissionTargetFinderTest}; this just confirms find() delegates and returns its result.
     */
    @Test
    public void testFindDelegatesToPirateFinderForPirateDefender() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction pirateFaction = createTestFaction("PIR", false, false);
        when(pirateFaction.isPirate()).thenReturn(true);
        Faction emptyFaction = createTestFaction("UND", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem emptySystem = createTestSystem(1, 0, emptyFaction);

        List<PlanetarySystem> systems = List.of(attackerSystem, emptySystem);
        FactionBorderTracker tracker = buildTestTracker(systems);

        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, pirateFaction, location, TEST_DATE);

        assertEquals(List.of(emptySystem), targets,
              "find() should delegate to the pirate finder for a pirate defender and return its result");
    }

    /**
     * Regression test: a pirate defender has no real territory, so unlike a genuine war partner, there's no "guaranteed
     * valid, just far away" pirate target for the closest-system fallback to reach for. If none of
     * {@link PirateMissionTargetFinder}'s tiers find a plausible hideout within the search radius, find() must return
     * no target at all rather than falling through to the whole-map closest-system fallback and landing on some
     * incidental, far-flung world a pirate band happens to have once held.
     */
    @Test
    public void testFindPirateDefenderNeverReachesOutsideSearchRadius() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction pirateFaction = createTestFaction("PIR", false, false);
        when(pirateFaction.isPirate()).thenReturn(true);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        // Far outside the tracker's 5 ly search radius below - simulating a pirate band that happens to hold some
        // distant world, not a genuine "guaranteed valid" target the way a real war partner would be.
        PlanetarySystem distantPirateSystem = createTestSystem(1000, 1000, pirateFaction);
        when(attackerSystem.getDistanceTo(any(PlanetarySystem.class))).thenReturn(0.0);
        when(distantPirateSystem.getDistanceTo(any(PlanetarySystem.class))).thenReturn(2000.0);

        List<PlanetarySystem> systems = List.of(attackerSystem, distantPirateSystem);
        FactionBorderTracker tracker = buildTestTracker(systems, 5);

        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(attackerSystem);

        List<PlanetarySystem> targets = finder.find(attackerFaction, pirateFaction, location, TEST_DATE);

        assertTrue(targets.isEmpty(),
              "A pirate defender with no plausible hideout within the search radius should never fall back to some "
                    + "far-flung pirate-held world reached only by ignoring the radius entirely");
    }

    /**
     * Regression test: a pirate attacker is routed to {@link PirateMissionTargetFinder}, whose own tiers are tested
     * directly in {@code PirateMissionTargetFinderTest}; this just confirms find() delegates and returns its result.
     */
    @Test
    public void testFindDelegatesToPirateFinderForPirateAttacker() {
        Faction pirateFaction = createTestFaction("PIR", false, false);
        when(pirateFaction.isPirate()).thenReturn(true);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);
        Faction peripheryNeighbor = createTestFaction("PER", true, false);

        PlanetarySystem defenderSystem = createTestSystem(0, 0, defenderFaction);
        PlanetarySystem peripherySystem = createTestSystem(1, 0, peripheryNeighbor);

        List<PlanetarySystem> systems = List.of(defenderSystem, peripherySystem);
        FactionBorderTracker tracker = buildTestTracker(systems);

        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(defenderFaction);

        List<PlanetarySystem> targets = finder.find(pirateFaction, defenderFaction, location, TEST_DATE);

        assertEquals(List.of(defenderSystem), targets,
              "find() should delegate to the pirate finder for a pirate attacker and return its result");
    }

    /**
     * Regression test: when the defender has no identifiable border at all, the pirate-attacker tier
     * ({@link PirateMissionTargetFinder}) finds nothing, so find() falls back to striking anywhere the defender holds
     * rather than finding no target.
     */
    @Test
    public void testFindPirateAttackerFallsBackToAllDefenderSystemsWhenNoBorder() {
        Faction pirateFaction = createTestFaction("PIR", false, false);
        when(pirateFaction.isPirate()).thenReturn(true);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        PlanetarySystem defenderSystem1 = createTestSystem(0, 0, defenderFaction);
        PlanetarySystem defenderSystem2 = createTestSystem(1, 0, defenderFaction);

        List<PlanetarySystem> systems = List.of(defenderSystem1, defenderSystem2);
        FactionBorderTracker tracker = buildTestTracker(systems);

        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(defenderFaction);

        List<PlanetarySystem> targets = finder.find(pirateFaction, defenderFaction, location, TEST_DATE);

        assertEquals(Set.of(defenderSystem1, defenderSystem2), new HashSet<>(targets),
              "With no neighboring faction to form a border, a pirate attacker should be able to target any system "
                    + "the defender controls");
    }

    /**
     * Regression test: a ComStar defender is routed to {@link ComStarMissionTargetFinder}, whose own filtering is
     * tested directly in {@code ComStarMissionTargetFinderTest}; this just confirms find() delegates and returns its
     * result.
     */
    @Test
    public void testFindDelegatesToComStarFinderForComStarDefender() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction comStarFaction = createTestFaction("CS", false, false);
        when(comStarFaction.isComStar()).thenReturn(true);
        when(comStarFaction.isComStarOrWoB()).thenReturn(true);

        PlanetarySystem attackerSystem = createTestSystem(-1, 0, attackerFaction);
        PlanetarySystem comStarOwnedSystem = createTestSystem(0, 0, comStarFaction);

        List<PlanetarySystem> systems = List.of(attackerSystem, comStarOwnedSystem);
        FactionBorderTracker tracker = buildTestTracker(systems);

        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, comStarFaction, location, TEST_DATE);

        assertEquals(List.of(comStarOwnedSystem), targets,
              "find() should delegate to the ComStar finder for a ComStar defender and return its result");
    }

    /**
     * Regression test: with no ComStar-owned or A/B-rated HPG system in range, find() returns no target for a ComStar
     * defender &mdash; unlike the pirate cases, this is a hard restriction with no fallback to the generic border-based
     * logic.
     */
    @Test
    public void testFindReturnsEmptyForComStarDefenderWhenNoQualifyingSystems() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction comStarFaction = createTestFaction("CS", false, false);
        when(comStarFaction.isComStar()).thenReturn(true);
        when(comStarFaction.isComStarOrWoB()).thenReturn(true);

        PlanetarySystem attackerSystem = createTestSystem(-1, 0, attackerFaction);

        List<PlanetarySystem> systems = List.of(attackerSystem);
        FactionBorderTracker tracker = buildTestTracker(systems);

        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, comStarFaction, location, TEST_DATE);

        assertTrue(targets.isEmpty(),
              "With no ComStar-owned or A/B-rated HPG system in range, there should be no valid target, and no "
                    + "fallback to the generic border logic");
    }

    /**
     * Regression test: a rebel uprising happens somewhere within the attacking government's own territory, so a rebel
     * defender resolves to any of the attacker's own systems rather than a shared border.
     */
    @Test
    public void testFindRebelDefenderTargetsAllAttackerSystems() {
        Faction rebelFaction = createTestFaction("REB", false, false);
        when(rebelFaction.isRebel()).thenReturn(true);
        MissionTargetFinder finder = createTestFinder();
        ILocation location = createTestLocation(isFaction);

        List<PlanetarySystem> targets = finder.find(isFaction, rebelFaction, location, TEST_DATE);

        Set<PlanetarySystem> expected = new HashSet<>(borderTracker.getBorders(isFaction).getSystems());
        assertEquals(expected, new HashSet<>(targets),
              "A rebel uprising should be targetable anywhere within the attacker's own territory");
    }

    /**
     * Regression test: a mission target must always be a system the defender actually owns. A landless defender with no
     * territory, no direct border, and no contained-faction-host relationship has nothing valid to target, so this
     * correctly yields no target rather than falling back to a neighbor's or the attacker's own systems.
     */
    @Test
    public void testFindReturnsEmptyForLandlessDefenderWithNoDirectRelationship() {
        Faction mercDefender = createTestFaction("MERC_DEF", false, false);
        when(mercDefender.isMercenary()).thenReturn(true);
        MissionTargetFinder finder = createTestFinder();
        ILocation location = createTestLocation(isFaction);

        List<PlanetarySystem> targets = finder.find(isFaction, mercDefender, location, TEST_DATE);

        assertTrue(targets.isEmpty(),
              "A landless defender with no direct relationship to the attacker should never fall back to a "
                    + "neighbor's or the attacker's own systems");
    }

    /**
     * Regression test: even when the attacker borders other regional factions closely, none of the attacker's own
     * systems (nor those third-party neighbors') should ever be picked as a mission target &mdash; only a system the
     * defender actually owns, however far away that may be (see
     * {@link #testFindClosestDefenderSystemFallbackIgnoresRadiusForDefender}).
     */
    @Test
    public void testFindNeverReturnsAttackerOrNeighborOwnedSystems() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction neighborFaction = createTestFaction("NEIGHBOR", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem neighborSystem = createTestSystem(1, 0, neighborFaction);
        PlanetarySystem distantDefenderSystem = createTestSystem(1000, 1000, defenderFaction);
        when(attackerSystem.getDistanceTo(any(PlanetarySystem.class))).thenReturn(0.0);
        when(neighborSystem.getDistanceTo(any(PlanetarySystem.class))).thenReturn(1.0);
        when(distantDefenderSystem.getDistanceTo(any(PlanetarySystem.class))).thenReturn(2000.0);

        List<PlanetarySystem> systems = List.of(attackerSystem, neighborSystem, distantDefenderSystem);
        FactionBorderTracker tracker = buildTestTracker(systems, 5);

        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(attackerSystem);

        List<PlanetarySystem> targets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE);

        assertEquals(List.of(distantDefenderSystem), targets,
              "Only the defender's own system should ever be returned, never the attacker's or a third-party "
                    + "neighbor's, even when those are nearby and the defender is not");
    }

    /**
     * Regression test: when the attacker and defender don't directly border each other, but the attacker is a
     * "contained" faction of some other regional faction with the defender listed as its opponent, that regional
     * faction's own border with the defender should be used as a proxy target.
     */
    @Test
    public void testFindContainedFactionOpponentFallback() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);
        Faction hostFaction = createTestFaction("HOST", false, false);

        // The attacker and defender are far apart (no direct border); the host is placed right next to the
        // defender so its own border can serve as a proxy.
        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem defenderSystem = createTestSystem(1000, 1000, defenderFaction);
        PlanetarySystem hostSystem = createTestSystem(1000, 1002, hostFaction);

        List<PlanetarySystem> systems = List.of(attackerSystem, defenderSystem, hostSystem);
        FactionBorderTracker tracker = buildTestTracker(systems);

        FactionHints hints = new FactionHints();
        // attackerFaction is "contained" under hostFaction, with defenderFaction as its designated opponent - even
        // though attackerFaction has territory of its own (so it isn't resolved away to hostFaction outright).
        hints.addContainedFaction(hostFaction,
              attackerFaction,
              null,
              null,
              1.0,
              Collections.singletonList(defenderFaction));

        MissionTargetFinder finder = new MissionTargetFinder(tracker, hints);
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE);

        assertEquals(List.of(defenderSystem), targets,
              "With no direct border, a contained-faction-opponent relationship should provide a proxy border");
    }

    /**
     * Regression test: two factions with no territory of their own (e.g. two pirate bands) have nowhere for a mission
     * to occur.
     */
    @Test
    public void testFindReturnsEmptyWhenBothFactionsAreLandless() {
        Faction pirateAttacker = createTestFaction("PIR_A", false, false);
        when(pirateAttacker.isPirate()).thenReturn(true);
        Faction pirateDefender = createTestFaction("PIR_D", false, false);
        when(pirateDefender.isPirate()).thenReturn(true);
        MissionTargetFinder finder = createTestFinder();
        ILocation location = createTestLocation(isFaction);

        List<PlanetarySystem> targets = finder.find(pirateAttacker, pirateDefender, location, TEST_DATE);

        assertTrue(targets.isEmpty(), "Two landless factions have no territory for a mission to occur on");
    }

    /**
     * Regression test: a faction with no territory of its own, no configured contained-faction host, and none of the
     * inherently-landless faction types (pirate/mercenary/ComStar/WoB/rebel) resolves to {@code null}, so no mission
     * target can be found.
     */
    @Test
    public void testFindReturnsEmptyWhenFactionHasNoTerritoryOrHost() {
        Faction homelessFaction = createTestFaction("HOMELESS", false, false);
        MissionTargetFinder finder = createTestFinder();
        ILocation location = createTestLocation(isFaction);

        List<PlanetarySystem> targets = finder.find(isFaction, homelessFaction, location, TEST_DATE);

        assertTrue(targets.isEmpty(),
              "A faction with no territory of its own and no configured host resolves to null, yielding no targets");
    }

    @Test
    public void testFindFortressRepublicExcludesAllRepublicSystemsDuringCoreWindow() {
        Faction rosFaction = createTestFaction(Faction.REPUBLIC_OF_THE_SPHERE_FACTION_CODE, false, false);
        allFactions.add(rosFaction);
        Faction mercAttacker = createTestFaction("MERC", false, false);
        when(mercAttacker.isMercenary()).thenReturn(true);

        PlanetarySystem terraSystem = createTestSystem(0, 0, rosFaction);
        when(terraSystem.getId()).thenReturn("Terra");
        PlanetarySystem otherRepublicSystem = createTestSystem(1, 0, rosFaction);
        when(otherRepublicSystem.getId()).thenReturn("Addicks");

        List<PlanetarySystem> systems = List.of(terraSystem, otherRepublicSystem);
        FactionBorderTracker tracker = buildTestTracker(systems);

        LocalDate fortressRepublicDate = FORTRESS_REPUBLIC_START.plusYears(1);
        tracker.setDate(fortressRepublicDate);
        // Blocks until the background recalculation triggered by setDate() has finished, so the tracker's cached
        // state (consulted by resolveTerritorialHost) is guaranteed to reflect fortressRepublicDate.
        tracker.getFactionsInRegion();

        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(rosFaction);

        List<PlanetarySystem> targets = finder.find(mercAttacker, rosFaction, location, fortressRepublicDate);

        assertTrue(targets.isEmpty(),
              "During the core Fortress Republic window, every Republic-owned system - Terra included - is "
                    + "off-limits as a target (see Faction#isDuringFortressRepublic)");
    }

    /**
     * Once the core Fortress Republic window ends, {@link Faction#isDuringFortressRepublic} keeps excluding Terra alone
     * through the wider Terra-only window, while every other Republic system becomes a valid target again.
     */
    @Test
    public void testFindFortressRepublicTerraOnlyExcludesOnlyTerra() {
        Faction rosFaction = createTestFaction(Faction.REPUBLIC_OF_THE_SPHERE_FACTION_CODE, false, false);
        allFactions.add(rosFaction);
        Faction mercAttacker = createTestFaction("MERC", false, false);
        when(mercAttacker.isMercenary()).thenReturn(true);

        PlanetarySystem terraSystem = createTestSystem(0, 0, rosFaction);
        when(terraSystem.getId()).thenReturn("Terra");
        PlanetarySystem otherRepublicSystem = createTestSystem(1, 0, rosFaction);
        when(otherRepublicSystem.getId()).thenReturn("Addicks");

        List<PlanetarySystem> systems = List.of(terraSystem, otherRepublicSystem);
        FactionBorderTracker tracker = buildTestTracker(systems);

        LocalDate terraOnlyDate = FORTRESS_REPUBLIC_END.plusMonths(6);
        tracker.setDate(terraOnlyDate);
        // Blocks until the background recalculation triggered by setDate() has finished, so the tracker's cached
        // state (consulted by resolveTerritorialHost) is guaranteed to reflect terraOnlyDate.
        tracker.getFactionsInRegion();

        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(rosFaction);

        List<PlanetarySystem> targets = finder.find(mercAttacker, rosFaction, location, terraOnlyDate);

        assertEquals(List.of(otherRepublicSystem), targets,
              "After the core Fortress Republic window ends, only Terra should remain off-limits as a target "
                    + "among Republic-owned systems");
    }

    /**
     * Stubs {@link PlanetarySystem#getDistanceTo} between every pair of the given mocked systems from their stubbed
     * coordinates: the border machinery's final range check uses real distances, which mocks otherwise report as 0.
     */
    private static void stubDistances(final PlanetarySystem... systems) {
        for (PlanetarySystem a : systems) {
            for (PlanetarySystem b : systems) {
                double distance = Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
                when(a.getDistanceTo(b)).thenReturn(distance);
            }
        }
    }

    /**
     * Builds a tracker over exactly the given systems with the standard test border sizes and an unlimited search
     * radius.
     */
    private static FactionBorderTracker buildTestTracker(final List<PlanetarySystem> systems) {
        return buildTestTracker(systems, -1);
    }

    /**
     * Builds a tracker over exactly the given systems with the standard test border sizes and the given search radius
     * in light years (negative for unlimited).
     */
    private static FactionBorderTracker buildTestTracker(final List<PlanetarySystem> systems, final double radius) {
        FactionBorderTracker tracker = new FactionBorderTracker(0, 0, radius) {
            @Override
            public Collection<PlanetarySystem> getSystemList() {
                return systems;
            }
        };
        tracker.setDefaultBorderSize(2.5, 10, 2.5);
        return tracker;
    }

    /**
     * REAR_AREA (cadre duty, retainers): the mission belongs in the defender's safe interior, so a defender system away
     * from the shared border must be preferred over the border world the default search would pick.
     */
    @Test
    public void testFindRearAreaProfilePrefersDefenderInterior() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem borderSystem = createTestSystem(2, 0, defenderFaction);
        PlanetarySystem interiorSystem = createTestSystem(30, 0, defenderFaction);
        stubDistances(attackerSystem, borderSystem, interiorSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(attackerSystem, borderSystem, interiorSystem));
        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE,
              MissionLocationProfile.REAR_AREA);

        assertEquals(List.of(interiorSystem), targets,
              "A rear-area contract should be placed on a defender system away from the shared border");
    }

    /**
     * REAR_AREA is a preference, not a restriction: when everything the defender holds in range is border, the search
     * must fall back to the default shared-border chain rather than failing generation.
     */
    @Test
    public void testFindRearAreaProfileFallsBackToBorderWhenNoInterior() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem borderSystem = createTestSystem(2, 0, defenderFaction);
        stubDistances(attackerSystem, borderSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(attackerSystem, borderSystem));
        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE,
              MissionLocationProfile.REAR_AREA);

        assertEquals(List.of(borderSystem), targets,
              "With no interior system available, a rear-area contract should fall back to the default border "
                    + "search rather than finding no target");
    }

    /**
     * INTERIOR_POPULATED (riot duty, security duty): unrest can flare up anywhere in the defender's space, so both
     * border and interior defender systems are valid, unlike the default border-only pool.
     */
    @Test
    public void testFindInteriorPopulatedProfileTargetsAllDefenderSystems() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem borderSystem = createTestSystem(2, 0, defenderFaction);
        PlanetarySystem interiorSystem = createTestSystem(30, 0, defenderFaction);
        stubDistances(attackerSystem, borderSystem, interiorSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(attackerSystem, borderSystem, interiorSystem));
        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE,
              MissionLocationProfile.INTERIOR_POPULATED);

        assertEquals(Set.of(borderSystem, interiorSystem), new HashSet<>(targets),
              "An internal-security contract should be able to land on any defender system in range, not just "
                    + "border worlds");
    }

    /**
     * DEEP_RAID (raids, assassination): hit-and-run strikes reach past the standard border depth (about twice as deep),
     * but not arbitrarily far into the defender's interior.
     */
    @Test
    public void testFindDeepRaidProfileReachesPastStandardBorder() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        // Within the standard 2.5 ly border...
        PlanetarySystem borderSystem = createTestSystem(2, 0, defenderFaction);
        // ...past it, but within the doubled deep-raid reach of 5 ly...
        PlanetarySystem nearInteriorSystem = createTestSystem(4, 0, defenderFaction);
        // ...and far beyond even that.
        PlanetarySystem deepInteriorSystem = createTestSystem(30, 0, defenderFaction);
        stubDistances(attackerSystem, borderSystem, nearInteriorSystem, deepInteriorSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(attackerSystem, borderSystem,
              nearInteriorSystem, deepInteriorSystem));
        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> defaultTargets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE);
        assertEquals(Set.of(borderSystem), new HashSet<>(defaultTargets),
              "Sanity check: the default search should stop at the standard border depth");

        List<PlanetarySystem> raidTargets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE,
              MissionLocationProfile.DEEP_RAID);
        assertEquals(Set.of(borderSystem, nearInteriorSystem), new HashSet<>(raidTargets),
              "A raid contract should reach past the standard border depth, but not arbitrarily deep");
    }

    /**
     * OCCUPIED_TERRITORY (guerrilla warfare): the preferred battleground is a world the defender recently took from the
     * attacker, whose population plausibly still sympathizes with the attacker.
     */
    @Test
    public void testFindOccupiedTerritoryProfilePrefersRecentlyConqueredSystems() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        // Owned by the attacker until 3020, by the defender since - i.e. conquered within the lookback window.
        PlanetarySystem recentlyConqueredSystem = createTestSystem(30, 0, defenderFaction);
        when(recentlyConqueredSystem.getFactionSet(any())).thenAnswer(invocation -> {
            LocalDate when = invocation.getArgument(0);
            return when.isBefore(LocalDate.of(3020, 1, 1)) ?
                         Collections.singleton(attackerFaction) :
                         Collections.singleton(defenderFaction);
        });
        // Both defender systems sit deep in the defender's space, so the recently-conquered one must win on its
        // ownership history alone, not merely on being away from the border.
        PlanetarySystem alwaysDefenderSystem = createTestSystem(28, 0, defenderFaction);
        stubDistances(attackerSystem, recentlyConqueredSystem, alwaysDefenderSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(attackerSystem, recentlyConqueredSystem,
              alwaysDefenderSystem));
        // The fresh border computations use the tracker's own date, which defaults to the wall-clock date; pin it to
        // the test date so the date-sensitive ownership mock above resolves consistently.
        tracker.setDate(TEST_DATE);
        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE,
              MissionLocationProfile.OCCUPIED_TERRITORY);

        assertEquals(List.of(recentlyConqueredSystem), targets,
              "A guerrilla contract should prefer a world the defender recently took from the attacker");
    }

    /**
     * OCCUPIED_TERRITORY without any recent conquest in range: the campaign belongs behind enemy lines, so defender
     * systems away from the shared border are preferred over the border worlds the default search would pick.
     */
    @Test
    public void testFindOccupiedTerritoryProfileFallsBackToDeepDefenderSystems() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem borderSystem = createTestSystem(2, 0, defenderFaction);
        PlanetarySystem deepSystem = createTestSystem(30, 0, defenderFaction);
        stubDistances(attackerSystem, borderSystem, deepSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(attackerSystem, borderSystem, deepSystem));
        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE,
              MissionLocationProfile.OCCUPIED_TERRITORY);

        assertEquals(List.of(deepSystem), targets,
              "With no recently conquered world in range, a guerrilla contract should land deep in the defender's "
                    + "space rather than on the contested border");
    }

    /**
     * INVASION (planetary assault): the invader can take, supply, and hold a world only from adjacent friendly
     * territory, so only defender systems on the shared border are valid - never the defender's interior.
     */
    @Test
    public void testFindInvasionProfileTargetsSharedBorderOnly() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem borderSystem = createTestSystem(2, 0, defenderFaction);
        PlanetarySystem interiorSystem = createTestSystem(30, 0, defenderFaction);
        stubDistances(attackerSystem, borderSystem, interiorSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(attackerSystem, borderSystem, interiorSystem));
        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE,
              MissionLocationProfile.INVASION);

        assertEquals(List.of(borderSystem), targets,
              "A planetary invasion should only ever target defender worlds on the shared border");
    }

    /**
     * INVASION is the one hard location restriction among the profiles: with no shared border at all, there must be no
     * target - never a fallback to the whole-map closest-defender-system placement that a DEFAULT contract would reach
     * for.
     */
    @Test
    public void testFindInvasionProfileFindsNoTargetWithoutASharedBorder() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        // Far beyond any border reach - only the whole-map closest-system fallback could ever land here.
        PlanetarySystem distantDefenderSystem = createTestSystem(1000, 1000, defenderFaction);
        stubDistances(attackerSystem, distantDefenderSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(attackerSystem, distantDefenderSystem));
        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> defaultTargets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE);
        assertEquals(List.of(distantDefenderSystem), defaultTargets,
              "Sanity check: a DEFAULT contract should still reach the distant defender via the closest-system "
                    + "fallback");

        List<PlanetarySystem> invasionTargets = finder.find(attackerFaction, defenderFaction, location, TEST_DATE,
              MissionLocationProfile.INVASION);
        assertTrue(invasionTargets.isEmpty(),
              "An invasion with no shared border has no viable target: the attacker could never hold a world so far "
                    + "from its own territory");
    }

    /**
     * Regression test: a contained faction whose only direct holdings are far outside the search region (e.g. the Star
     * League's far-periphery administrative worlds during the Amaris Civil War) must resolve to its contained-faction
     * host's territory, not be left stranded on its remote worlds by a controls-anything-anywhere check.
     */
    @Test
    public void testFindRedirectsContainedFactionWithOnlyRemoteHoldingsToItsHost() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction leagueFaction = createTestFaction("LEAGUE", false, false);
        Faction hostFaction = createTestFaction("HOST", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem hostSystem = createTestSystem(2, 0, hostFaction);
        // The contained faction's only direct holding, far outside the 5 ly search region below.
        PlanetarySystem remoteSystem = createTestSystem(1000, 1000, leagueFaction);
        stubDistances(attackerSystem, hostSystem, remoteSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(attackerSystem, hostSystem, remoteSystem), 5);
        FactionHints hints = new FactionHints();
        hints.addContainedFaction(hostFaction, leagueFaction, null, null, 0.5);
        MissionTargetFinder finder = new MissionTargetFinder(tracker, hints);
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, leagueFaction, location, TEST_DATE);

        assertEquals(List.of(hostSystem), targets,
              "A contained faction with only remote holdings should fight on its host's territory, not be stranded "
                    + "on a titular world on the far side of the map");
    }

    /**
     * Regression test: a contained faction's host can itself be landless at the time (the occupied Terran Hegemony
     * during the Amaris Civil War, still nominally hosting the Star League), so host resolution must skip past it to a
     * host that actually holds territory.
     */
    @Test
    public void testFindSkipsLandlessHostWhenResolvingContainedFaction() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction leagueFaction = createTestFaction("LEAGUE", false, false);
        Faction occupiedHostFaction = createTestFaction("OCCUPIED_HOST", false, false);
        Faction intactHostFaction = createTestFaction("INTACT_HOST", false, false);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem intactHostSystem = createTestSystem(2, 0, intactHostFaction);
        stubDistances(attackerSystem, intactHostSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(attackerSystem, intactHostSystem), 5);
        FactionHints hints = new FactionHints();
        hints.addContainedFaction(occupiedHostFaction, leagueFaction, null, null, 0.5);
        hints.addContainedFaction(intactHostFaction, leagueFaction, null, null, 0.5);
        MissionTargetFinder finder = new MissionTargetFinder(tracker, hints);
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, leagueFaction, location, TEST_DATE);

        assertEquals(List.of(intactHostSystem), targets,
              "Host resolution should skip a host that holds no territory and use one that does");
    }

    /**
     * Regression test: the mercenary faction has no fixed territory of its own beyond whatever handful of systems the
     * data happens to tag it as directly controlling. As the defender, it must be treated as contained within any
     * faction that hires mercenaries, not left stranded on that tiny footprint - otherwise every mercenary-defender
     * contract converges on the same one or two systems.
     */
    @Test
    public void testFindRedirectsMercenaryDefenderToAFactionThatHiresMercenaries() {
        Faction attackerFaction = createTestFaction("ATTACKER", false, false);
        Faction mercFaction = createTestFaction(Faction.MERCENARY_FACTION_CODE, false, false);
        when(mercFaction.isMercenary()).thenReturn(true);
        Faction hostFaction = createTestFaction("HOST_MERC_USER", false, false);
        when(hostFaction.validIn(any())).thenReturn(true);
        when(hostFaction.isUsesMercenaries(anyInt())).thenReturn(true);
        allFactions.add(mercFaction);
        allFactions.add(hostFaction);

        PlanetarySystem attackerSystem = createTestSystem(0, 0, attackerFaction);
        PlanetarySystem hostSystem = createTestSystem(2, 0, hostFaction);
        // The mercenary faction's only direct holding, far outside the 5 ly search region below.
        PlanetarySystem distantMercSystem = createTestSystem(1000, 1000, mercFaction);
        stubDistances(attackerSystem, hostSystem, distantMercSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(attackerSystem, hostSystem, distantMercSystem), 5);
        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(attackerFaction);

        List<PlanetarySystem> targets = finder.find(attackerFaction, mercFaction, location, TEST_DATE);

        assertEquals(List.of(hostSystem), targets,
              "A mercenary defender should fight on the territory of a nearby faction that hires mercenaries, not "
                    + "be stranded on its own distant, sparsely-held systems");
    }

    /**
     * Regression test: the mercenary-host redirect only applies to the mercenary faction as <em>defender</em>. As the
     * attacker, mercenaries must remain landless in the usual sense (able to strike anywhere the defender holds, per
     * {@code isSpecialAttacker}), even when a mercenary-hiring host is available nearby - redirecting the attacker too
     * would narrow its reach down to just that host's border with the defender.
     */
    @Test
    public void testFindMercenaryAttackerStrikesAnywhereDespiteAnAvailableMercenaryHost() {
        Faction mercFaction = createTestFaction(Faction.MERCENARY_FACTION_CODE, false, false);
        when(mercFaction.isMercenary()).thenReturn(true);
        Faction hostFaction = createTestFaction("HOST_MERC_USER", false, false);
        when(hostFaction.validIn(any())).thenReturn(true);
        when(hostFaction.isUsesMercenaries(anyInt())).thenReturn(true);
        Faction defenderFaction = createTestFaction("DEFENDER", false, false);
        allFactions.add(mercFaction);
        allFactions.add(hostFaction);
        allFactions.add(defenderFaction);

        PlanetarySystem hostSystem = createTestSystem(2, 0, hostFaction);
        // Borders hostSystem (within the default 2.5 ly border size).
        PlanetarySystem borderingDefenderSystem = createTestSystem(2.2, 0, defenderFaction);
        // Within the attacker's 5 ly search radius, but far enough from hostSystem to not border it.
        PlanetarySystem distantDefenderSystem = createTestSystem(4.9, 0, defenderFaction);
        stubDistances(hostSystem, borderingDefenderSystem, distantDefenderSystem);

        FactionBorderTracker tracker = buildTestTracker(List.of(hostSystem, borderingDefenderSystem,
              distantDefenderSystem), 5);
        MissionTargetFinder finder = new MissionTargetFinder(tracker, new FactionHints());
        ILocation location = createTestLocation(mercFaction);

        List<PlanetarySystem> targets = finder.find(mercFaction, defenderFaction, location, TEST_DATE);

        assertEquals(Set.of(borderingDefenderSystem, distantDefenderSystem), new HashSet<>(targets),
              "A mercenary attacker should still be able to strike any defender system in its search radius, not "
                    + "just ones bordering the mercenary-hiring host");
    }
}
