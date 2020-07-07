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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public void testGetBorderPlanetsFactionBorders() {
        LocalDate when = LocalDate.now();
        List<PlanetarySystem> systems = new ArrayList<>();
        for (int x = -3; x <= 3; x += 2) {
            for (int y = -2; y <= 2; y += 2) {
                systems.add(createSystem(x, y, factionThem));
            }
        }
        systems.add(createSystem(0, 0, factionUs));
        FactionBorders us = new FactionBorders(factionUs, when, systems);
        FactionBorders them = new FactionBorders(factionThem, when, systems);

        List<PlanetarySystem> border = us.getBorderSystems(them, 1.1);

        assertEquals(border.size(), 2);
        for (PlanetarySystem p : border) {
            assertEquals(Math.abs(p.getX()), 1, RegionPerimeter.EPSILON);
            assertEquals(p.getY(), 0, RegionPerimeter.EPSILON);
        }
    }
}
