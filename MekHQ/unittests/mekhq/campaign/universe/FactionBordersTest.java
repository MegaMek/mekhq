/*
 * Copyright (c) 2018 - The MegaMek Team
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

public class FactionBordersTest {
    
    private Faction factionUs;
    private Faction factionThem;
    
    @Before
    public void init() {
        factionUs = createFaction("us", false);
        factionThem = createFaction("them", false);
    }
    
    private Faction createFaction(final String key, final boolean periphery) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn(key);
        when(faction.isPeriphery()).thenReturn(periphery);
        return faction;
    }
    
    private Planet createPlanet(final double x, final double y, Faction owner) {
        Planet planet = mock(Planet.class);
        when(planet.getX()).thenReturn(x);
        when(planet.getY()).thenReturn(y);
        String id = String.format("%f, %f", x, y);
        when(planet.getId()).thenReturn(id);
        when(planet.getFactionSet(any())).thenReturn(Collections.singleton(owner));
        return planet;
    }

    @Test
    public void testGetBorderPlanetsFactionBorders() {
        DateTime when = new DateTime();
        List<Planet> planets = new ArrayList<>();
        for (int x = -3; x <= 3; x += 2) {
            for (int y = -2; y <= 2; y += 2) {
                planets.add(createPlanet(x, y, factionThem));
            }
        }
        planets.add(createPlanet(0, 0, factionUs));
        FactionBorders us = new FactionBorders(factionUs, when, planets);
        FactionBorders them = new FactionBorders(factionThem, when, planets);
        
        List<Planet> border = us.getBorderPlanets(them, 1.1);
        
        assertEquals(border.size(), 2);
        for (Planet p : border) {
            assertEquals(Math.abs(p.getX()), 1, RegionPerimeter.EPSILON);
            assertEquals(p.getY(), 0, RegionPerimeter.EPSILON);
        }
    }

}
