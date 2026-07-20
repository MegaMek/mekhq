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
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.compute.Compute;
import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.mission.contractGeneration.GlobalEmployerTableValue;
import mekhq.campaign.mission.newContract.EnemySelectionProfile;
import mekhq.campaign.mission.newContract.MissionLocationProfile;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.campaign.universe.factionHints.FactionHints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class RandomFactionGeneratorTest {

    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);

    private Faction isFaction;
    private Faction clanFaction;
    private Faction peripheryFaction;
    private Faction innerISFaction;
    private Faction rebelFaction;
    private FactionBorderTracker borderTracker;

    /**
     * Backs the mocked {@link Factions#getInstance()} roster; {@link RandomFactionGenerator#buildEnemyMap} consults it
     * to find war partners with no presence in the immediate search area. Tests that introduce additional ad hoc
     * factions (e.g. a distant belligerent) must add them here too.
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
        rebelFaction = createTestFaction(Faction.REBEL_FACTION_CODE, true, false);

        allFactions = new ArrayList<>(List.of(isFaction, clanFaction, peripheryFaction, innerISFaction, rebelFaction));
        Factions factions = mock(Factions.class);
        when(factions.getFactions()).thenReturn(allFactions);
        // Resolves by short name against allFactions, plus the synthetic REBEL fallback faction used by
        // RandomFactionGenerator#getEnemy when no employer is supplied or no valid enemy candidate is found.
        when(factions.getFaction(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            if (Faction.REBEL_FACTION_CODE.equals(code)) {
                return rebelFaction;
            }
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
        // By default a faction is considered valid in any year for the purposes of these tests;
        // individual tests can override this for extinction-specific scenarios.
        when(f.validIn(any(LocalDate.class))).thenReturn(true);
        when(f.validIn(anyInt())).thenReturn(true);
        // Non-Clan factions use mercenaries by default (Faction#isUsesMercenaries' real default); Clans generally
        // don't, mirroring the historical CW/CSF-only exceptions. Individual tests can override this.
        when(f.isUsesMercenaries(anyInt())).thenReturn(!clan);
        // Mirror the real data's AGGREGATE + PIRATE/REBEL/MERC tagging for the "self-conflicting" test factions, so
        // RandomFactionGenerator#isSelfConflictingFaction (isAggregate() && (isPirate() || isRebel() ||
        // isMercenary())) behaves the same against these mocks as it does against the real, YAML-loaded factions.
        boolean isPirateLike = id.equals(Faction.PIRATE_FACTION_CODE) || id.equals(Faction.BANDIT_CASTE_FACTION_CODE);
        boolean isRebelLike = id.equals(Faction.REBEL_FACTION_CODE);
        boolean isMercLike = id.equals(Faction.MERCENARY_FACTION_CODE);
        when(f.isPirate()).thenReturn(isPirateLike);
        when(f.isRebel()).thenReturn(isRebelLike);
        when(f.isMercenary()).thenReturn(isMercLike);
        when(f.isAggregate()).thenReturn(isPirateLike || isRebelLike || isMercLike);
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
        return new RandomFactionGenerator(buildTestTracker(systems), new FactionHints());
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
        Factions.getInstance();
        when(Factions.getInstance().getFaction(Faction.REBEL_FACTION_CODE)).thenReturn(rebelFaction);
        ILocation location = createTestLocation(isFaction);

        assertNotNull(rfg.getEmployerFaction(location, TEST_DATE));
    }

    @Test
    public void testGetRandomEnemy() {
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        Faction enemy = rfg.getRandomEnemy(false, location, TEST_DATE, isFaction);

        assertNotEquals("PIR", enemy.getShortName());
        assertNotEquals(isFaction.getShortName(), enemy.getShortName());
    }

    /**
     * Regression test: allied factions must never be selected as an enemy, with no exceptions.
     */
    @Test
    public void testGetRandomEnemyExcludesAlliedFaction() {
        FactionHints hints = new FactionHints();
        RandomFactionGenerator rfg = new RandomFactionGenerator(createTestBorderTracker(), hints);
        hints.addAlliance("", null, null, isFaction, peripheryFaction);
        ILocation location = createTestLocation(isFaction);

        for (int i = 0; i < 500; i++) {
            Faction enemy = rfg.getRandomEnemy(false, location, TEST_DATE, isFaction);
            assertNotEquals(peripheryFaction.getShortName(), enemy.getShortName(),
                  "Allied faction must never be chosen as an enemy");
        }
    }

    /**
     * Regression test: a faction at war with the employer must always be a valid enemy target, even one with no systems
     * within the search area, as long as the border tracker knows it controls territory elsewhere on the map.
     */
    @Test
    public void testGetRandomEnemyGuaranteesAtWarFactionOutsideSearchRadius() {
        Faction warFaction = createTestFaction("WAR", false, false);
        allFactions.add(warFaction);

        PlanetarySystem nearSystem = createTestSystem(0, 0, isFaction);
        PlanetarySystem farSystem = mock(PlanetarySystem.class);
        when(farSystem.getFactionSet(any())).thenReturn(Collections.singleton(warFaction));
        when(farSystem.getDistanceTo(any(PlanetarySystem.class))).thenReturn(1000.0);
        when(nearSystem.getDistanceTo(any(PlanetarySystem.class))).thenReturn(0.0);

        List<PlanetarySystem> systems = List.of(nearSystem, farSystem);
        FactionBorderTracker tracker = buildTestTracker(systems, 5);

        FactionHints hints = new FactionHints();
        hints.addWar("", null, null, isFaction, warFaction);
        when(Factions.getInstance().getFaction(Faction.REBEL_FACTION_CODE)).thenReturn(rebelFaction);
        RandomFactionGenerator rfg = new RandomFactionGenerator(tracker, hints);
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(nearSystem);

        boolean warFactionSeen = false;
        for (int i = 0; i < 200; i++) {
            Faction enemy = rfg.getRandomEnemy(false, location, TEST_DATE, isFaction);
            if (warFaction.getShortName().equals(enemy.getShortName())) {
                warFactionSeen = true;
                break;
            }
        }
        assertTrue(warFactionSeen,
              "At-war faction with no systems in the search radius should still be a valid target");
    }

    /**
     * Regression test: during covert operations, an allied faction is a rare-but-possible enemy target, unlike normal
     * operations where allies are always excluded with no exceptions (see
     * {@link #testGetRandomEnemyExcludesAlliedFaction}).
     */
    @Test
    public void testGetRandomEnemyCovertOpsCanTargetAllies() {
        FactionHints hints = new FactionHints();
        RandomFactionGenerator rfg = new RandomFactionGenerator(createTestBorderTracker(), hints);
        hints.addAlliance("", null, null, isFaction, peripheryFaction);
        ILocation location = createTestLocation(isFaction);

        boolean allySeen = false;
        for (int i = 0; i < 2000; i++) {
            Faction enemy = rfg.getRandomEnemy(true, location, TEST_DATE, isFaction);
            if (peripheryFaction.getShortName().equals(enemy.getShortName())) {
                allySeen = true;
                break;
            }
        }
        assertTrue(allySeen, "Covert operations should occasionally be able to target an allied faction");
    }

    /**
     * Regression test: two factions with no direct alliance record, but each individually allied with the same third
     * faction (e.g. two member states of a superpower), should inherit that shared ally's protection and never be
     * chosen as each other's enemy.
     */
    @Test
    public void testGetRandomEnemyExcludesFactionAlliedThroughSharedSuperpower() {
        Faction superpower = createTestFaction("SUPER", false, false);
        FactionHints hints = new FactionHints();
        RandomFactionGenerator rfg = new RandomFactionGenerator(createTestBorderTracker(), hints);
        // Two separate two-party alliances - isFaction and peripheryFaction are never listed together.
        hints.addAlliance("", null, null, superpower, isFaction);
        hints.addAlliance("", null, null, superpower, peripheryFaction);
        ILocation location = createTestLocation(isFaction);

        for (int i = 0; i < 500; i++) {
            Faction enemy = rfg.getRandomEnemy(false, location, TEST_DATE, isFaction);
            assertNotEquals(peripheryFaction.getShortName(), enemy.getShortName(),
                  "A faction allied with the same superpower should never be chosen as an enemy, even without a "
                        + "direct alliance record");
        }
    }

    /**
     * Regression test: the shared-ally inheritance in
     * {@link #testGetRandomEnemyExcludesFactionAlliedThroughSharedSuperpower} must not apply when factionHints directly
     * contradicts it with an explicit war record between the two factions.
     */
    @Test
    public void testGetRandomEnemyAtWarOverridesSharedSuperpowerAlliance() {
        Faction superpower = createTestFaction("SUPER", false, false);
        Faction warFaction = createTestFaction("WAR", false, false);
        allFactions.add(warFaction);

        PlanetarySystem nearSystem = createTestSystem(0, 0, isFaction);
        PlanetarySystem farSystem = mock(PlanetarySystem.class);
        when(farSystem.getFactionSet(any())).thenReturn(Collections.singleton(warFaction));
        when(farSystem.getDistanceTo(any(PlanetarySystem.class))).thenReturn(1000.0);
        when(nearSystem.getDistanceTo(any(PlanetarySystem.class))).thenReturn(0.0);

        List<PlanetarySystem> systems = List.of(nearSystem, farSystem);
        FactionBorderTracker tracker = buildTestTracker(systems, 5);

        FactionHints hints = new FactionHints();
        hints.addAlliance("", null, null, superpower, isFaction);
        hints.addAlliance("", null, null, superpower, warFaction);
        hints.addWar("", null, null, isFaction, warFaction);
        RandomFactionGenerator rfg = new RandomFactionGenerator(tracker, hints);
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(nearSystem);

        boolean warFactionSeen = false;
        for (int i = 0; i < 200; i++) {
            Faction enemy = rfg.getRandomEnemy(false, location, TEST_DATE, isFaction);
            if (warFaction.getShortName().equals(enemy.getShortName())) {
                warFactionSeen = true;
                break;
            }
        }
        assertTrue(warFactionSeen,
              "A direct war record should override the inherited shared-superpower alliance");
    }

    /**
     * Regression test: a pirate employer bypasses all diplomatic checks (neutral, allied, etc.) and can target any
     * valid faction with a presence in the area.
     */
    @Test
    public void testGetRandomEnemyPirateEmployerIgnoresNeutralStatus() {
        Faction pirateFaction = createTestFaction("PIR", false, false);
        when(pirateFaction.isPirate()).thenReturn(true);

        FactionHints hints = new FactionHints();
        hints.addNeutralFaction(peripheryFaction);
        RandomFactionGenerator rfg = new RandomFactionGenerator(createTestBorderTracker(), hints);
        ILocation location = createTestLocation(isFaction);

        boolean neutralSeen = false;
        for (int i = 0; i < 500; i++) {
            Faction enemy = rfg.getRandomEnemy(false, location, TEST_DATE, pirateFaction);
            if (peripheryFaction.getShortName().equals(enemy.getShortName())) {
                neutralSeen = true;
                break;
            }
        }
        assertTrue(neutralSeen, "A pirate employer should be able to target even a neutral faction");
    }

    /**
     * Regression test: {@link RandomFactionGenerator#getRandomEnemy} falls back to the REBEL faction rather than
     * returning {@code null} when no employer is supplied.
     */
    @Test
    public void testGetRandomEnemyIndependentFallbackForNullEmployer() {
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        Faction enemy = rfg.getRandomEnemy(false, location, TEST_DATE, null);

        assertEquals(rebelFaction, enemy, "A null employer should fall back to the REBEL faction");
    }

    @Test
    public void testGetRandomEnemyList() {
        RandomFactionGenerator rfg = createTestRFG();

        List<String> enemyList = rfg.getEnemyList(clanFaction);

        assertFalse(enemyList.contains(clanFaction.getShortName()));
        assertTrue(enemyList.contains(isFaction.getShortName()));
        assertTrue(enemyList.contains(peripheryFaction.getShortName()));
        assertTrue(enemyList.contains(innerISFaction.getShortName()));
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
     * Regression test for MekHQ issue #6451: an extinct faction (e.g. Aurigan Coalition past 3028) must not be returned
     * as a current/employer faction even if stale planet ownership data still lists it as the controller of some
     * systems.
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
     * Regression test: {@code addAnyContainedFactions} previously re-checked the host faction's eligibility instead of
     * the contained faction's, so a Clan contained faction could slip past the Clan filter and be chosen as an
     * employer. Marking the contained faction (not the host) as a Clan must exclude it.
     */
    @Test
    public void testClanContainedFactionExcludedFromEmployers() {
        when(innerISFaction.isClan()).thenReturn(true);
        when(innerISFaction.isUsesMercenaries(anyInt())).thenReturn(false);
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

        Faction chosen = rfg.getRandomEmployerFaction(location, TEST_DATE, null, false);

        assertNotNull(chosen, "Employer faction should not be null");
        assertEquals(peripheryFaction.getShortName(), chosen.getShortName());
    }

    @Test
    public void testRandomEmployerFactionNullWhenLocationHasNoSystem() {
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(null);

        assertNull(rfg.getRandomEmployerFaction(location, TEST_DATE, null, false));
    }

    @Test
    public void testRandomEmployerFactionExcludesClanController() {
        RandomFactionGenerator rfg = createIsolatedRfg(clanFaction);
        ILocation location = createTestLocation(clanFaction);

        assertNull(rfg.getRandomEmployerFaction(location, TEST_DATE, null, true),
              "A Clan-controlled area with no eligible contained faction should have no employer");
    }

    @Test
    public void testRandomEmployerFactionExtinctControllerExcluded() {
        when(isFaction.validIn(any(LocalDate.class))).thenReturn(false);
        when(isFaction.validIn(anyInt())).thenReturn(false);
        RandomFactionGenerator rfg = createIsolatedRfg(isFaction);
        ILocation location = createTestLocation(isFaction);

        assertNull(rfg.getRandomEmployerFaction(location, TEST_DATE, null, false),
              "Extinct controlling faction should not be chosen, nor unlock its contained faction");
    }

    @Test
    public void testContainedFactionReachableViaRandomEmployerFaction() {
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        boolean innerSeen = false;
        for (int i = 0; i < 500; i++) {
            Faction chosen = rfg.getRandomEmployerFaction(location, TEST_DATE, null, false);
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
        when(innerISFaction.isUsesMercenaries(anyInt())).thenReturn(false);
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        for (int i = 0; i < 500; i++) {
            Faction chosen = rfg.getRandomEmployerFaction(location, TEST_DATE, null, true);
            assertNotNull(chosen, "Employer faction should not be null");
            assertNotEquals(innerISFaction.getShortName(), chosen.getShortName(),
                  "Clan contained faction must never be chosen as an employer");
        }
    }

    /**
     * {@link RandomFactionGenerator#getEmployerFaction(ILocation, LocalDate)} is a convenience wrapper around
     * {@link RandomFactionGenerator#getRandomEmployerFaction} with no employer-type filtering; verify it draws from the
     * same candidate pool as calling that method directly with a {@code null} employer type.
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

    /**
     * Regression test: {@link RandomFactionGenerator#getRandomEmployerFaction} must filter candidates by the requested
     * {@link GlobalEmployerTableValue} power tier, excluding any faction (controlling or contained) whose tier doesn't
     * match.
     */
    @Test
    public void testRandomEmployerFactionFiltersByEmployerType() {
        when(isFaction.isMinorPower()).thenReturn(true);
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        for (int i = 0; i < 500; i++) {
            Faction chosen = rfg.getRandomEmployerFaction(location,
                  TEST_DATE,
                  GlobalEmployerTableValue.MINOR_POWER,
                  false);
            assertNotNull(chosen, "Employer faction should not be null");
            assertEquals(isFaction.getShortName(), chosen.getShortName(),
                  "Only factions matching the requested employer power tier should be returned, including excluding "
                        + "an otherwise-eligible contained faction whose own tier doesn't match");
        }
    }

    /**
     * Regression test: {@code isMercenaryCampaign} must exclude any faction that doesn't use mercenaries, not just
     * Clans (see {@link #testRandomEmployerFactionExcludesClanController} for the Clan-specific case).
     */
    @Test
    public void testRandomEmployerFactionMercenaryCampaignExcludesNonMercenaryUsingFaction() {
        when(peripheryFaction.isUsesMercenaries(anyInt())).thenReturn(false);
        RandomFactionGenerator rfg = createIsolatedRfg(peripheryFaction);
        ILocation location = createTestLocation(peripheryFaction);

        assertNull(rfg.getRandomEmployerFaction(location, TEST_DATE, null, true),
              "A non-Clan faction that doesn't use mercenaries should still be excluded during a mercenary campaign");
    }

    /**
     * Regression test: a faction at war with the employer has its weight floored to at least 10 before being
     * quadrupled, guaranteeing it remains a valid, heavily-weighted target even with no base presence in the search
     * area (e.g. distant warring factions during the early Clan Invasion or the Reunification Wars).
     */
    @Test
    public void testAdjustEnemyWeightFloorsAndQuadruplesForAtWarFaction() {
        FactionHints hints = mock(FactionHints.class);
        when(hints.isAtWarWith(isFaction, peripheryFaction, TEST_DATE)).thenReturn(true);
        RandomFactionGenerator rfg = new RandomFactionGenerator(borderTracker, hints);

        double weight = rfg.adjustEnemyWeight(0, isFaction, peripheryFaction, TEST_DATE, false);

        assertEquals(40.0, weight,
              "A war partner with zero base presence should be floored to a weight of 10 before quadrupling");
    }

    /**
     * Regression test: a rival's base weight is doubled.
     */
    @Test
    public void testAdjustEnemyWeightDoublesForRival() {
        FactionHints hints = mock(FactionHints.class);
        when(hints.isRivalOf(isFaction, peripheryFaction, TEST_DATE)).thenReturn(true);
        RandomFactionGenerator rfg = new RandomFactionGenerator(borderTracker, hints);

        double weight = rfg.adjustEnemyWeight(5, isFaction, peripheryFaction, TEST_DATE, false);

        assertEquals(10.0, weight, "A rival's base weight should be doubled");
    }

    /**
     * Regression test: WoB's weight is doubled during the Jihad.
     */
    @Test
    public void testAdjustEnemyWeightDoublesForWoBDuringJihad() {
        Faction wobFaction = createTestFaction("WOB", false, false);
        when(wobFaction.isWoB()).thenReturn(true);
        RandomFactionGenerator rfg = new RandomFactionGenerator(borderTracker, mock(FactionHints.class));

        double weight = rfg.adjustEnemyWeight(5, isFaction, wobFaction, TEST_DATE, true);

        assertEquals(10.0, weight, "WoB's weight should double during the Jihad");
    }

    /**
     * Regression test: ComStar's weight against a Clan target is divided by 12, since ComStar has few targets and would
     * otherwise fight the Clans too often between Tukayyid and the Jihad.
     */
    @Test
    public void testAdjustEnemyWeightDividesByTwelveForComStarVsClan() {
        Faction comStar = createTestFaction("CS", false, false);
        RandomFactionGenerator rfg = new RandomFactionGenerator(borderTracker, mock(FactionHints.class));

        double weight = rfg.adjustEnemyWeight(12, comStar, clanFaction, TEST_DATE, false);

        assertEquals(1.0, weight, "ComStar's weight against a Clan target should be divided by 12");
    }

    /**
     * The population-weighted mission-target pick scores population on a log10 scale plus a bonus for a major HPG, so
     * major worlds are favored without drowning out everything else.
     */
    @Test
    public void testPopulationWeightScalesWithPopulationAndHpg() {
        PlanetarySystem majorWorld = mock(PlanetarySystem.class);
        when(majorWorld.getPopulation(TEST_DATE)).thenReturn(1_000_000_000L);
        when(majorWorld.getHPG(TEST_DATE)).thenReturn(HPGRating.A);

        assertEquals(13, RandomFactionGenerator.populationWeight(majorWorld, TEST_DATE),
              "A billion-population world with an A-rated HPG should weigh 1 + log10(1e9) + 3");
    }

    /**
     * A system with no population or HPG data must still get a weight of 1, so it remains a pickable (if unlikely)
     * mission target instead of silently dropping out of the weighted pool.
     */
    @Test
    public void testPopulationWeightFloorsAtOneWithNoData() {
        PlanetarySystem barrenWorld = mock(PlanetarySystem.class);

        assertEquals(1, RandomFactionGenerator.populationWeight(barrenWorld, TEST_DATE),
              "A system with no population or HPG data should still carry the minimum weight of 1");
    }

    /**
     * {@code industrialWeight} scores only industry and output (production capacity), not raw materials or agriculture
     * (self-sufficiency): a fully industrialized, high-output world should score the maximum 8, and a world with none
     * of either should score 0.
     */
    @Test
    public void testIndustrialWeightScoresIndustryAndOutputOnly() {
        PlanetarySystem factoryWorld = mock(PlanetarySystem.class);
        when(factoryWorld.getSocioIndustrial(TEST_DATE)).thenReturn(
              new SocioIndustrialData(PlanetarySophistication.C, PlanetaryRating.A, PlanetaryRating.F,
                    PlanetaryRating.A, PlanetaryRating.F));

        assertEquals(8, RandomFactionGenerator.industrialWeight(factoryWorld, TEST_DATE),
              "An A-rated industry and A-rated output world should score the maximum of 8, ignoring raw materials "
                    + "and agriculture");

        PlanetarySystem barrenWorld = mock(PlanetarySystem.class);
        when(barrenWorld.getSocioIndustrial(TEST_DATE)).thenReturn(
              new SocioIndustrialData(PlanetarySophistication.C, PlanetaryRating.F, PlanetaryRating.A,
                    PlanetaryRating.F, PlanetaryRating.A));

        assertEquals(0, RandomFactionGenerator.industrialWeight(barrenWorld, TEST_DATE),
              "An F-rated industry and F-rated output world should score 0, even with top raw materials and "
                    + "agriculture");
    }

    /**
     * {@code missionTargetWeight} only adds the industrial score for industrially-weighted profiles (HIGH_VALUE,
     * INVASION); a profile like INTERIOR_POPULATED should ignore a world's industrial capacity entirely.
     */
    @Test
    public void testMissionTargetWeightAddsIndustrialScoreOnlyForIndustriallyWeightedProfiles() {
        PlanetarySystem factoryWorld = mock(PlanetarySystem.class);
        when(factoryWorld.getSocioIndustrial(TEST_DATE)).thenReturn(
              new SocioIndustrialData(PlanetarySophistication.C, PlanetaryRating.A, PlanetaryRating.C,
                    PlanetaryRating.A, PlanetaryRating.C));

        int baseWeight = RandomFactionGenerator.populationWeight(factoryWorld, TEST_DATE);

        assertEquals(baseWeight,
              RandomFactionGenerator.missionTargetWeight(factoryWorld, TEST_DATE,
                    MissionLocationProfile.INTERIOR_POPULATED),
              "A non-industrially-weighted profile should ignore industrial capacity entirely");
        assertEquals(baseWeight + 16,
              RandomFactionGenerator.missionTargetWeight(factoryWorld, TEST_DATE, MissionLocationProfile.HIGH_VALUE),
              "HIGH_VALUE should add the industrial score (8) times the industrial weight multiplier (2)");
        assertEquals(baseWeight + 16,
              RandomFactionGenerator.missionTargetWeight(factoryWorld, TEST_DATE, MissionLocationProfile.INVASION),
              "INVASION should add the same industrial bonus as HIGH_VALUE");
    }

    /**
     * PIRATES (pirate hunting): the enemy is baked into the contract type - the pirate faction, or the Bandit Caste for
     * a Clan employer.
     */
    @Test
    public void testGetRandomEnemyProfilePirates() {
        Faction pirates = createTestFaction(Faction.PIRATE_FACTION_CODE, false, false);
        Faction banditCaste = createTestFaction(Faction.BANDIT_CASTE_FACTION_CODE, false, true);
        allFactions.add(pirates);
        allFactions.add(banditCaste);
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        assertEquals(pirates, rfg.getRandomEnemy(location, TEST_DATE, isFaction, EnemySelectionProfile.PIRATES),
              "A non-Clan employer hunting pirates should face the pirate faction");
        assertEquals(banditCaste, rfg.getRandomEnemy(location, TEST_DATE, clanFaction, EnemySelectionProfile.PIRATES),
              "A Clan employer hunting pirates should face the Bandit Caste");
    }

    /**
     * REBELS (riot duty): a riot is internal unrest, so the enemy is always the rebel faction.
     */
    @Test
    public void testGetRandomEnemyProfileRebels() {
        allFactions.add(rebelFaction);
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        assertEquals(rebelFaction, rfg.getRandomEnemy(location, TEST_DATE, isFaction, EnemySelectionProfile.REBELS),
              "A riot-duty enemy should always be the rebel faction");
    }

    /**
     * RAIDERS (cadre duty): a rear-area posting is harassed by irregulars - pirates or rebels - never by a peer state's
     * line regiments.
     */
    @Test
    public void testGetRandomEnemyProfileRaidersReturnsPiratesOrRebels() {
        Faction pirates = createTestFaction(Faction.PIRATE_FACTION_CODE, false, false);
        allFactions.add(pirates);
        allFactions.add(rebelFaction);
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(isFaction);

        Faction enemy = rfg.getRandomEnemy(location, TEST_DATE, isFaction, EnemySelectionProfile.RAIDERS);

        assertTrue(Set.of(pirates, rebelFaction).contains(enemy),
              "A cadre-duty enemy should be an irregular force (pirates or rebels), not a peer state");
    }

    /**
     * RAIDERS with the pirate faction itself as the employer can still pick pirates as their own enemy: rival pirate
     * bands harassing each other's rear-area postings makes just as much sense as pirates harassing a real government
     * (see {@code isSelfConflictingFaction}).
     */
    @Test
    public void testGetRandomEnemyProfileRaidersCanPickPirateEmployerItself() {
        Faction pirates = createTestFaction(Faction.PIRATE_FACTION_CODE, false, false);
        allFactions.add(pirates);
        allFactions.add(rebelFaction);
        RandomFactionGenerator rfg = createTestRFG();
        ILocation location = createTestLocation(pirates);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(2)).thenReturn(1);

            assertEquals(pirates, rfg.getRandomEnemy(location, TEST_DATE, pirates, EnemySelectionProfile.RAIDERS),
                  "A pirate employer's rear-area harassers should be able to be pirates as well as rebels");
        }
    }

    /**
     * The "aggregate" factions - pirates, the Bandit Caste, rebels, mercenaries - can each generate contracts against
     * themselves with no {@code factionHints} war record required: their countless independent bands, cells, and
     * companies fight each other constantly without any specific war ever being declared.
     */
    @Test
    public void testBuildEnemyMapAllowsAggregateFactionsToFightThemselvesWithoutAWarRecord() {
        for (String aggregateCode : List.of(Faction.PIRATE_FACTION_CODE, Faction.BANDIT_CASTE_FACTION_CODE,
              Faction.REBEL_FACTION_CODE, Faction.MERCENARY_FACTION_CODE)) {
            Faction aggregateFaction = createTestFaction(aggregateCode, false, false);
            List<PlanetarySystem> systems = List.of(createTestSystem(0, 0, aggregateFaction));
            RandomFactionGenerator rfg = new RandomFactionGenerator(buildTestTracker(systems),
                  mock(FactionHints.class));
            ILocation location = createTestLocation(aggregateFaction);

            WeightedIntMap<Faction> enemyMap = rfg.buildEnemyMap(false, location, TEST_DATE, aggregateFaction);

            assertTrue(enemyMap.containsValue(aggregateFaction),
                  aggregateCode + " should be able to generate contracts against itself with no war record");
        }
    }

    /**
     * A non-aggregate faction (an ordinary government) must still be excluded as its own enemy with no war record, even
     * though it controls the only system in range - the self-conflict exception is specific to the aggregate factions,
     * not a general "sole candidate" carve-out.
     */
    @Test
    public void testBuildEnemyMapStillExcludesOrdinaryFactionFromItselfWithoutAWarRecord() {
        List<PlanetarySystem> systems = List.of(createTestSystem(0, 0, isFaction));
        RandomFactionGenerator rfg = new RandomFactionGenerator(buildTestTracker(systems), mock(FactionHints.class));
        ILocation location = createTestLocation(isFaction);

        WeightedIntMap<Faction> enemyMap = rfg.buildEnemyMap(false, location, TEST_DATE, isFaction);

        assertFalse(enemyMap.containsValue(isFaction),
              "An ordinary government should still need a recorded civil war to be its own enemy");
    }

    /**
     * AT_WAR (and by extension OCCUPYING_POWER, which shares the same belligerent search): an aggregate-faction
     * employer can be matched against itself with no war record, since
     * {@code RandomFactionGenerator#findEnemiesAtWarWith(Faction, LocalDate)} carries the
     * same self-conflict exception as {@link RandomFactionGenerator#buildEnemyMap}.
     */
    @Test
    public void testGetRandomEnemyProfileAtWarAllowsMercenaryEmployerToFightItselfWithoutAWarRecord() {
        Faction mercFaction = createTestFaction(Faction.MERCENARY_FACTION_CODE, false, false);
        allFactions.add(mercFaction);
        List<PlanetarySystem> systems = List.of(createTestSystem(0, 0, mercFaction));
        RandomFactionGenerator rfg = new RandomFactionGenerator(buildTestTracker(systems), mock(FactionHints.class));
        ILocation location = createTestLocation(mercFaction);

        assertEquals(mercFaction, rfg.getRandomEnemy(location, TEST_DATE, mercFaction, EnemySelectionProfile.AT_WAR),
              "A mercenary employer's open-warfare contract should be able to target a rival mercenary company with "
                    + "no recorded war");
    }

    /**
     * AT_WAR (planetary assault, relief duty): an open-warfare contract should be against a faction the employer is
     * actually at war with, when one exists.
     */
    @Test
    public void testGetRandomEnemyProfileAtWarPrefersBelligerent() {
        FactionHints hints = mock(FactionHints.class);
        when(hints.isAtWarWith(isFaction, clanFaction, TEST_DATE)).thenReturn(true);
        RandomFactionGenerator rfg = new RandomFactionGenerator(borderTracker, hints);
        ILocation location = createTestLocation(isFaction);

        assertEquals(clanFaction, rfg.getRandomEnemy(location, TEST_DATE, isFaction, EnemySelectionProfile.AT_WAR),
              "An open-warfare contract should be against the employer's actual war partner when one exists");
    }

    /**
     * AT_WAR is a preference, not a restriction: an employer at war with nobody still gets an enemy from the standard
     * pool rather than failing generation.
     */
    @Test
    public void testGetRandomEnemyProfileAtWarFallsBackToStandardPoolWithoutAWar() {
        RandomFactionGenerator rfg = new RandomFactionGenerator(borderTracker, mock(FactionHints.class));
        ILocation location = createTestLocation(isFaction);

        Faction enemy = rfg.getRandomEnemy(location, TEST_DATE, isFaction, EnemySelectionProfile.AT_WAR);

        assertTrue(Set.of(clanFaction, peripheryFaction).contains(enemy),
              "With no active war, the enemy should come from the standard regional pool");
    }

    /**
     * OCCUPYING_POWER (guerrilla warfare): among the employer's war partners, prefer one that occupies a world recently
     * taken from the employer - the same lookback the OCCUPIED_TERRITORY location tier uses, so the flipped-world
     * location search can actually fire.
     */
    @Test
    public void testGetRandomEnemyProfileOccupyingPowerPrefersRecentConqueror() {
        Faction occupier = createTestFaction("OCCUPIER", false, false);
        Faction distantBelligerent = createTestFaction("DISTANT", false, false);
        allFactions.add(occupier);
        allFactions.add(distantBelligerent);

        PlanetarySystem employerHome = createTestSystem(0, 0, isFaction);
        // Owned by the employer until 3020, by the occupier since - a conquest within the lookback window.
        PlanetarySystem occupiedSystem = createTestSystem(2, 0, occupier);
        when(occupiedSystem.getFactionSet(any())).thenAnswer(invocation -> {
            LocalDate when = invocation.getArgument(0);
            return when.isBefore(LocalDate.of(3020, 1, 1)) ?
                         Collections.singleton(isFaction) :
                         Collections.singleton(occupier);
        });
        PlanetarySystem distantBelligerentSystem = createTestSystem(4, 0, distantBelligerent);

        List<PlanetarySystem> systems = List.of(employerHome, occupiedSystem, distantBelligerentSystem);
        FactionBorderTracker tracker = buildTestTracker(systems);

        FactionHints hints = mock(FactionHints.class);
        when(hints.isAtWarWith(isFaction, occupier, TEST_DATE)).thenReturn(true);
        when(hints.isAtWarWith(isFaction, distantBelligerent, TEST_DATE)).thenReturn(true);
        RandomFactionGenerator rfg = new RandomFactionGenerator(tracker, hints);
        ILocation location = mock(ILocation.class);
        when(location.getCurrentSystem()).thenReturn(employerHome);

        assertEquals(occupier,
              rfg.getRandomEnemy(location, TEST_DATE, isFaction, EnemySelectionProfile.OCCUPYING_POWER),
              "A guerrilla contract should be against the war partner that actually occupies the employer's "
                    + "recently lost worlds");
    }

    /**
     * A recorded war overrides a direct alliance that hasn't been expunged from the data: the Star League and the
     * Amaris-held Terran Hegemony were nominally allied for the whole League era yet at war from the coup on, and the
     * war must win.
     */
    @Test
    public void testGetRandomEnemyWarOverridesDirectAlliance() {
        Faction formerAlly = createTestFaction("FORMER_ALLY", false, false);
        allFactions.add(formerAlly);

        PlanetarySystem employerSystem = createTestSystem(0, 0, isFaction);
        PlanetarySystem formerAllySystem = createTestSystem(1, 0, formerAlly);
        FactionHints hints = mock(FactionHints.class);
        when(hints.isAlliedWith(isFaction, formerAlly, TEST_DATE)).thenReturn(true);
        when(hints.isAtWarWith(isFaction, formerAlly, TEST_DATE)).thenReturn(true);
        RandomFactionGenerator rfg = new RandomFactionGenerator(
              buildTestTracker(List.of(employerSystem, formerAllySystem)), hints);
        ILocation location = createTestLocation(isFaction);

        assertEquals(formerAlly, rfg.getRandomEnemy(false, location, TEST_DATE, isFaction),
              "A faction recorded both allied and at war should be a valid enemy: the war record wins");
    }

    /**
     * {@code hasAnyTerritory} accepts a contained-faction host's territory in place of the faction's own, so a landless
     * government hosted by territorial states (e.g. the Star League) still counts as having something to defend.
     */
    @Test
    public void testHasAnyTerritoryAcceptsContainedFactionHost() {
        Faction leagueFaction = createTestFaction("LEAGUE", false, false);
        Faction hostFaction = createTestFaction("HOST", false, false);
        Faction hostlessFaction = createTestFaction("HOSTLESS", false, false);

        PlanetarySystem hostSystem = createTestSystem(0, 0, hostFaction);
        FactionHints hints = new FactionHints();
        hints.addContainedFaction(hostFaction, leagueFaction, null, null, 0.5);
        RandomFactionGenerator rfg = new RandomFactionGenerator(buildTestTracker(List.of(hostSystem)), hints);

        assertTrue(rfg.hasAnyTerritory(leagueFaction, TEST_DATE),
              "A landless faction hosted by a territorial state has territory to defend");
        assertFalse(rfg.hasAnyTerritory(hostlessFaction, TEST_DATE),
              "A landless faction with no host has no territory at all");
    }

    /**
     * A faction explicitly recorded at war with itself (a civil war, e.g. the FedCom Civil War) can generate contracts
     * against itself.
     */
    @Test
    public void testGetRandomEnemyCivilWarAllowsSelfAsEnemy() {
        List<PlanetarySystem> systems = List.of(createTestSystem(0, 0, isFaction));
        FactionHints hints = mock(FactionHints.class);
        when(hints.isAtWarWith(isFaction, isFaction, TEST_DATE)).thenReturn(true);
        RandomFactionGenerator rfg = new RandomFactionGenerator(buildTestTracker(systems), hints);
        ILocation location = createTestLocation(isFaction);

        assertEquals(isFaction, rfg.getRandomEnemy(false, location, TEST_DATE, isFaction),
              "A faction at war with itself should be able to generate contracts against itself");
    }

    /**
     * Without an explicit self-war record in factionHints, a faction is never its own enemy - the civil-war exception
     * must be gated strictly on the recorded war state.
     */
    @Test
    public void testGetRandomEnemyExcludesSelfWithoutCivilWar() {
        List<PlanetarySystem> systems = List.of(createTestSystem(0, 0, isFaction));
        when(Factions.getInstance().getFaction(Faction.REBEL_FACTION_CODE)).thenReturn(rebelFaction);
        RandomFactionGenerator rfg = new RandomFactionGenerator(buildTestTracker(systems), mock(FactionHints.class));
        ILocation location = createTestLocation(isFaction);

        assertEquals(rebelFaction, rfg.getRandomEnemy(false, location, TEST_DATE, isFaction),
              "Without a recorded self-war, a faction alone in the area should find no enemy but the REBEL "
                    + "fallback");
    }

    /**
     * AT_WAR (planetary assault, relief duty): a civil-war employer's own rebellious half is a genuine belligerent, so
     * open-warfare contracts can be generated against itself.
     */
    @Test
    public void testGetRandomEnemyProfileAtWarCanPickSelfDuringCivilWar() {
        List<PlanetarySystem> systems = List.of(createTestSystem(0, 0, isFaction));
        FactionHints hints = mock(FactionHints.class);
        when(hints.isAtWarWith(isFaction, isFaction, TEST_DATE)).thenReturn(true);
        RandomFactionGenerator rfg = new RandomFactionGenerator(buildTestTracker(systems), hints);
        ILocation location = createTestLocation(isFaction);

        assertEquals(isFaction, rfg.getRandomEnemy(location, TEST_DATE, isFaction, EnemySelectionProfile.AT_WAR),
              "During a civil war, an open-warfare contract should be able to target the employer's own "
                    + "rebellious half");
    }

    /**
     * COVERT (espionage, sabotage, terrorism, assassination): the covert rules make allies rare-but-possible targets,
     * where the default rules exclude them outright.
     */
    @Test
    public void testGetRandomEnemyProfileCovertCanTargetAllyThatDefaultExcludes() {
        Faction ally = createTestFaction("ALLY", false, false);
        allFactions.add(ally);

        PlanetarySystem employerSystem = createTestSystem(0, 0, isFaction);
        PlanetarySystem allySystem = createTestSystem(1, 0, ally);
        List<PlanetarySystem> systems = List.of(employerSystem, allySystem);
        FactionBorderTracker tracker = buildTestTracker(systems);

        FactionHints hints = mock(FactionHints.class);
        when(hints.isAlliedWith(isFaction, ally, TEST_DATE)).thenReturn(true);
        RandomFactionGenerator rfg = new RandomFactionGenerator(tracker, hints);
        ILocation location = createTestLocation(isFaction);

        assertEquals(ally, rfg.getRandomEnemy(location, TEST_DATE, isFaction, EnemySelectionProfile.COVERT),
              "Under covert rules, an ally should be a possible (if rare) target");
        assertEquals(rebelFaction,
              rfg.getRandomEnemy(location, TEST_DATE, isFaction, EnemySelectionProfile.DEFAULT),
              "Under default rules, an ally is excluded outright, leaving only the REBEL fallback here");
    }
}
