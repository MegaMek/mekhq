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
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class RandomFactionGeneratorTest {
    
    private Faction isFaction;
    private Faction clanFaction;
    private Faction peripheryFaction;
    private Faction innerISFaction;
    private FactionBorderTracker borderTracker;
    
    @Before
    public void init() {
        borderTracker = createTestBorderTracker();
    }
    
    private FactionBorderTracker createTestBorderTracker() {
        isFaction = createTestFaction("IS", false, false);
        clanFaction = createTestFaction("Clan", false, true);
        peripheryFaction = createTestFaction("Periphery", true, false);
        innerISFaction = createTestFaction("IS2", false, false);
        
        List<Planet> planets = new ArrayList<>();
        for (int x = -2; x < 3; x++) {
            for (int y = -2; y < 3; y++) {
                if (x < 0) {
                    planets.add(createTestPlanet(x, y, isFaction));
                }
                if (x > 0) {
                    planets.add(createTestPlanet(x, y, clanFaction));
                }
                planets.add(createTestPlanet(x, y + 10, peripheryFaction));
            }
        }
        
        FactionBorderTracker tracker = new FactionBorderTracker(0, 0, -1) {
            @Override
            protected Collection<Planet> getPlanetList() {
                return planets;
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
    
    private static Planet createTestPlanet(final double x, final double y, final Faction f) {
        Planet p = mock(Planet.class);
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

        assertFalse(rfg.getEmployer() == null);
    }
    
    @Test
    public void testGetEnemy() {
        RandomFactionGenerator rfg = createTestRFG();

        String enemy = rfg.getEnemy(isFaction, false);
        
        assertFalse(enemy.equals("PIR"));
        assertFalse(isFaction.getShortName().equals(enemy));
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

    @Test
    public void testWeightedMap() {
        RandomFactionGenerator.WeightedMap<Integer> map = new RandomFactionGenerator.WeightedMap<>();
        int total = 0;
        for (int i = 0; i < 6; i++) {
            map.add(i, i);
            total += i;
        }
        
        assertEquals(map.size(), 5);
        assertEquals(map.lastKey().intValue(), total);
        assertEquals(map.ceilingEntry(1).getValue().intValue(), 1);
        assertEquals(map.ceilingEntry(2).getValue().intValue(), 2);
        assertEquals(map.ceilingEntry(10).getValue().intValue(), 4);
        assertEquals(map.ceilingEntry(11).getValue().intValue(), 5);
        assertEquals(map.ceilingEntry(15).getValue().intValue(), 5);
    }

}
