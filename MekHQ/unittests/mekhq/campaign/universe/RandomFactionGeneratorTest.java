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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RandomFactionGeneratorTest {

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

        assertNotNull(rfg.getEmployer());
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
}
