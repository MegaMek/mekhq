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
package mekhq.campaign.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
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
import mekhq.campaign.universe.factionHints.FactionHints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RandomFactionGeneratorTest {

    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);

    private Faction isFaction;
    private Faction clanFaction;
    private Faction peripheryFaction;
    private Faction innerISFaction;
    private FactionBorderTracker borderTracker;

    @BeforeEach
    public void init() {
        borderTracker = createTestBorderTracker();
    }

    private FactionBorderTracker createTestBorderTracker() {
        isFaction = createTestFaction("IS", false, false);
        clanFaction = createTestFaction("Clan", false, true);
        peripheryFaction = createTestFaction("Periphery", true, false);
        innerISFaction = createTestFaction("IS2", false, false);

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

        FactionBorderTracker tracker = new FactionBorderTracker(0, 0, -1) {
            @Override
            protected Collection<PlanetarySystem> getSystemList() {
                return systems;
            }
        };
        tracker.setDefaultBorderSize(2.5, 10, 2.5);
        return tracker;
    }

    private static Faction createTestFaction(final String id, final boolean periphery, final boolean clan) {
        Faction f = mock(Faction.class);
        when(f.getShortName()).thenReturn(id);
        when(f.isPeriphery()).thenReturn(periphery);
        when(f.isClan()).thenReturn(clan);
        // By default a faction is considered valid in any year for the purposes of these tests;
        // individual tests can override this for extinction-specific scenarios.
        when(f.validIn(any(LocalDate.class))).thenReturn(true);
        when(f.validIn(anyInt())).thenReturn(true);
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

    private RandomFactionGenerator createTestRFG() {
        return new RandomFactionGenerator(borderTracker, createTestHints());
    }

    /**
     * Builds a mock {@link ILocation} whose current system is controlled solely by the given faction.
     */
    private ILocation createTestLocation(final Faction controllingFaction) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getFactionSet(any())).thenReturn(Collections.singleton(controllingFaction));
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(system);
        return location;
    }

    /**
     * Builds a {@link RandomFactionGenerator} whose border tracker (and thus its area search) contains only a single
     * system controlled by the given faction, with no faction hints. Used to isolate
     * {@link RandomFactionGenerator#getEmployerFaction(ILocation, LocalDate)}/
     * {@link RandomFactionGenerator#getRandomEmployerFaction} area searches to a single faction for deterministic
     * assertions, since {@link #createTestRFG()}'s shared region contains multiple factions.
     */
    private RandomFactionGenerator createIsolatedRfg(final Faction faction) {
        List<PlanetarySystem> systems = Collections.singletonList(createTestSystem(0, 0, faction));
        FactionBorderTracker tracker = new FactionBorderTracker(0, 0, -1) {
            @Override
            protected Collection<PlanetarySystem> getSystemList() {
                return systems;
            }
        };
        tracker.setDefaultBorderSize(2.5, 10, 2.5);
        return new RandomFactionGenerator(tracker, new FactionHints());
    }

    @Test
    public void testCurrentFactions() {
        RandomFactionGenerator rfg = createTestRFG();
        Set<String> factions = rfg.getCurrentFactions();

        assertTrue(factions.contains(isFaction.getShortName()));
        assertTrue(factions.contains(innerISFaction.getShortName()));
        assertTrue(factions.contains(peripheryFaction.getShortName()));
        assertTrue(factions.contains(clanFaction.getShortName()));
    }

    @Test
    public void testGetEmployers() {
        RandomFactionGenerator rfg = createTestRFG();

        Set<String> employers = rfg.getEmployerSet();

        assertTrue(employers.contains(isFaction.getShortName()));
        assertTrue(employers.contains(innerISFaction.getShortName()));
        assertTrue(employers.contains(peripheryFaction.getShortName()));
        assertFalse(employers.contains(clanFaction.getShortName()));
    }

    @Test
    public void testGetEmployer() {
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        assertNotNull(rfg.getEmployerFaction(location, TEST_DATE));
    }

    @Test
    public void testGetEnemy() {
        RandomFactionGenerator rfg = createTestRFG();

        String enemy = rfg.getEnemy(isFaction, false);

        assertNotEquals("PIR", enemy);
        assertNotEquals(isFaction.getShortName(), enemy);
    }

    @Test
    public void testGetEnemyList() {
        RandomFactionGenerator rfg = createTestRFG();

        List<String> enemyList = rfg.getEnemyList(clanFaction);

        assertFalse(enemyList.contains(clanFaction.getShortName()));
        assertTrue(enemyList.contains(isFaction.getShortName()));
        assertTrue(enemyList.contains(peripheryFaction.getShortName()));
        assertTrue(enemyList.contains(innerISFaction.getShortName()));
    }

    @Test
    public void testGetMissionTarget() {
        RandomFactionGenerator rfg = createTestRFG();

        assertFalse(rfg.getMissionTargetList(peripheryFaction, isFaction).isEmpty());
        assertFalse(rfg.getMissionTargetList(peripheryFaction, innerISFaction).isEmpty());
        assertFalse(rfg.getMissionTargetList(innerISFaction, peripheryFaction).isEmpty());
    }

    @Test
    public void testAlliance() {
        FactionHints hints = new FactionHints();
        RandomFactionGenerator rfg = new RandomFactionGenerator(createTestBorderTracker(), hints);
        hints.addAlliance("", null, null, isFaction, peripheryFaction);

        List<String> enemyList = rfg.getEnemyList(isFaction);

        assertFalse(enemyList.contains(peripheryFaction.getShortName()));
    }

    @Test
    public void testCivilWar() {
        FactionHints hints = new FactionHints();
        RandomFactionGenerator rfg = new RandomFactionGenerator(createTestBorderTracker(), hints);
        hints.addWar("", null, null, isFaction, isFaction);

        List<String> enemyList = rfg.getEnemyList(isFaction);

        assertFalse(enemyList.contains(isFaction.getShortName()));
    }

    @Test
    public void testNeutralFaction() {
        FactionHints hints = new FactionHints();
        RandomFactionGenerator rfg = new RandomFactionGenerator(createTestBorderTracker(), hints);
        hints.addNeutralFaction(peripheryFaction);
        hints.addNeutralExceptions("", null, null, peripheryFaction, clanFaction);

        List<String> enemyList = rfg.getEnemyList(peripheryFaction);

        assertFalse(enemyList.contains(isFaction.getShortName()));
        assertTrue(enemyList.contains(clanFaction.getShortName()));
    }

    @Test
    public void testContainedFactionOpponents() {
        FactionHints hints = createTestHints();
        RandomFactionGenerator rfg = new RandomFactionGenerator(createTestBorderTracker(), hints);
        hints.addContainedFaction(isFaction, innerISFaction, null, null, 0.5,
              Collections.singletonList(clanFaction));

        List<String> enemyList = rfg.getEnemyList(innerISFaction);

        assertFalse(enemyList.contains(isFaction.getShortName()));
        assertTrue(enemyList.contains(clanFaction.getShortName()));
    }

    /**
     * Regression test for MekHQ issue #6451: an extinct faction (e.g. Aurigan Coalition
     * past 3028) must not be returned as a current/employer faction even if stale planet
     * ownership data still lists it as the controller of some systems.
     */
    @Test
    public void testExtinctFactionExcludedFromCurrentFactions() {
        when(isFaction.validIn(any(LocalDate.class))).thenReturn(false);
        when(isFaction.validIn(anyInt())).thenReturn(false);
        RandomFactionGenerator rfg = createTestRFG();

        Set<String> factions = rfg.getCurrentFactions();

        assertFalse(factions.contains(isFaction.getShortName()),
              "Extinct faction should not appear in the current faction set");
    }

    @Test
    public void testExtinctFactionExcludedFromEmployers() {
        when(isFaction.validIn(any(LocalDate.class))).thenReturn(false);
        when(isFaction.validIn(anyInt())).thenReturn(false);
        RandomFactionGenerator rfg = createTestRFG();

        Set<String> employers = rfg.getEmployerSet();

        assertFalse(employers.contains(isFaction.getShortName()),
              "Extinct faction should not appear in the employer set");
    }

    @Test
    public void testExtinctContainedFactionExcluded() {
        // innerISFaction is the contained (fallback) faction; mark it extinct.
        when(innerISFaction.validIn(any(LocalDate.class))).thenReturn(false);
        when(innerISFaction.validIn(anyInt())).thenReturn(false);
        RandomFactionGenerator rfg = createTestRFG();

        Set<String> factions = rfg.getCurrentFactions();
        Set<String> employers = rfg.getEmployerSet();

        assertFalse(factions.contains(innerISFaction.getShortName()),
              "Extinct contained faction should not appear in the current faction set");
        assertFalse(employers.contains(innerISFaction.getShortName()),
              "Extinct contained faction should not appear in the employer set");
    }

    @Test
    public void testExtinctFactionExcludedFromEnemyList() {
        when(peripheryFaction.validIn(any(LocalDate.class))).thenReturn(false);
        when(peripheryFaction.validIn(anyInt())).thenReturn(false);
        RandomFactionGenerator rfg = createTestRFG();

        List<String> enemyList = rfg.getEnemyList(isFaction);

        assertFalse(enemyList.contains(peripheryFaction.getShortName()),
              "Extinct faction should not appear in the enemy list");
    }

    @Test
    public void testExtinctFactionExcludedAsPirateOpponent() {
        // Pirates take the unfiltered border-region branch in buildEnemyMap; ensure the
        // validity guard still excludes extinct factions there.
        Faction pirates = createTestFaction("PIR", false, false);
        when(peripheryFaction.validIn(any(LocalDate.class))).thenReturn(false);
        when(peripheryFaction.validIn(anyInt())).thenReturn(false);
        RandomFactionGenerator rfg = createTestRFG();

        List<String> enemyList = rfg.getEnemyList(pirates);

        assertFalse(enemyList.contains(peripheryFaction.getShortName()),
              "Extinct faction should not appear in pirate/comstar enemy list");
    }

    /**
     * Regression test for the actual contract-generation selection path used by AtB: an extinct faction must never be
     * returned by {@link RandomFactionGenerator#getEmployerFaction(ILocation, LocalDate)}. With no other faction
     * controlling anything in the search area, an extinct controller yields no employer at all.
     */
    @Test
    public void testExtinctFactionNeverChosenAsEmployer() {
        when(isFaction.validIn(any(LocalDate.class))).thenReturn(false);
        when(isFaction.validIn(anyInt())).thenReturn(false);
        RandomFactionGenerator rfg = createIsolatedRfg(isFaction);
        ILocation location = createTestLocation(isFaction);

        assertNull(rfg.getEmployerFaction(location, TEST_DATE),
              "Extinct faction must never be chosen as an employer");
    }

    /**
     * Verifies that a contained/fallback faction is reachable via
     * {@link RandomFactionGenerator#getEmployerFaction(ILocation, LocalDate)}.
     */
    @Test
    public void testContainedFactionCanBeChosenAsEmployer() {
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        boolean innerSeen = false;
        for (int i = 0; i < 500; i++) {
            Faction chosen = rfg.getEmployerFaction(location, TEST_DATE);
            assertNotNull(chosen, "Employer faction should not be null");
            if (innerISFaction.getShortName().equals(chosen.getShortName())) {
                innerSeen = true;
                break;
            }
        }
        assertTrue(innerSeen,
              "Contained faction should be selectable by getEmployerFaction()");
    }

    /**
     * Regression test: {@code addAnyContainedFactions} previously re-checked the host faction's eligibility instead
     * of the contained faction's, so a Clan contained faction could slip past the Clan filter and be chosen as an
     * employer. Marking the contained faction (not the host) as a Clan must exclude it.
     */
    @Test
    public void testClanContainedFactionExcludedFromEmployers() {
        when(innerISFaction.isClan()).thenReturn(true);
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        for (int i = 0; i < 500; i++) {
            Faction chosen = rfg.getEmployerFaction(location, TEST_DATE);
            assertNotNull(chosen, "Employer faction should not be null");
            assertNotEquals(innerISFaction.getShortName(), chosen.getShortName(),
                  "Clan contained faction must never be chosen as an employer");
        }
    }

    @Test
    public void testRandomEmployerFactionReturnsControllingFaction() {
        RandomFactionGenerator rfg = createIsolatedRfg(peripheryFaction);
        ILocation location = createTestLocation(peripheryFaction);

        Faction chosen = rfg.getRandomEmployerFaction(location, TEST_DATE, null);

        assertNotNull(chosen, "Employer faction should not be null");
        assertEquals(peripheryFaction.getShortName(), chosen.getShortName());
    }

    @Test
    public void testRandomEmployerFactionNullWhenLocationHasNoSystem() {
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(null);

        assertNull(rfg.getRandomEmployerFaction(location, TEST_DATE, null));
    }

    @Test
    public void testRandomEmployerFactionExcludesClanController() {
        RandomFactionGenerator rfg = createIsolatedRfg(clanFaction);
        ILocation location = createTestLocation(clanFaction);

        assertNull(rfg.getRandomEmployerFaction(location, TEST_DATE, null),
              "A Clan-controlled area with no eligible contained faction should have no employer");
    }

    @Test
    public void testRandomEmployerFactionExtinctControllerExcluded() {
        when(isFaction.validIn(any(LocalDate.class))).thenReturn(false);
        when(isFaction.validIn(anyInt())).thenReturn(false);
        RandomFactionGenerator rfg = createIsolatedRfg(isFaction);
        ILocation location = createTestLocation(isFaction);

        assertNull(rfg.getRandomEmployerFaction(location, TEST_DATE, null),
              "Extinct controlling faction should not be chosen, nor unlock its contained faction");
    }

    @Test
    public void testContainedFactionReachableViaRandomEmployerFaction() {
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        boolean innerSeen = false;
        for (int i = 0; i < 500; i++) {
            Faction chosen = rfg.getRandomEmployerFaction(location, TEST_DATE, null);
            assertNotNull(chosen, "Employer faction should not be null");
            if (innerISFaction.getShortName().equals(chosen.getShortName())) {
                innerSeen = true;
                break;
            }
        }
        assertTrue(innerSeen, "Contained faction should be selectable by getRandomEmployerFaction()");
    }

    /**
     * Same regression as {@link #testClanContainedFactionExcludedFromEmployers}, verified against the new
     * location-based selection method.
     */
    @Test
    public void testRandomEmployerFactionExcludesClanContainedFaction() {
        when(innerISFaction.isClan()).thenReturn(true);
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        for (int i = 0; i < 500; i++) {
            Faction chosen = rfg.getRandomEmployerFaction(location, TEST_DATE, null);
            assertNotNull(chosen, "Employer faction should not be null");
            assertNotEquals(innerISFaction.getShortName(), chosen.getShortName(),
                  "Clan contained faction must never be chosen as an employer");
        }
    }

    /**
     * {@link RandomFactionGenerator#getEmployerFaction(ILocation, LocalDate)} is a convenience wrapper around
     * {@link RandomFactionGenerator#getRandomEmployerFaction} with no employer-type filtering; verify it draws from
     * the same candidate pool as calling that method directly with a {@code null} employer type.
     */
    @Test
    public void testGetEmployerFactionMatchesRandomEmployerFactionCandidatePool() {
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        Set<String> candidates = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            Faction chosen = rfg.getEmployerFaction(location, TEST_DATE);
            assertNotNull(chosen, "Employer faction should not be null");
            candidates.add(chosen.getShortName());
        }

        assertEquals(Set.of(isFaction.getShortName(), peripheryFaction.getShortName(), innerISFaction.getShortName()),
              candidates,
              "getEmployerFaction should draw from every eligible faction in the search area, plus contained factions");
    }
}
