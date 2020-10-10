/*
 * StripUnitActionTest.java
 *
 * Copyright (C) 2020 MegaMek team
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

package mekhq.campaign.parts;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import megamek.common.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class PartTest {
    @Test
    public void sparePart() {
        Part part = new MekSensor();

        assertNull(part.getUnitId());
        assertNull(part.getParentPart());
        assertNull(part.getRefitId());
        assertFalse(part.isReservedForReplacement());
        assertTrue(part.isSpare());
    }

    @Test
    public void hasUnitIsNotSpare() {
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.getWeight()).thenReturn(20.0);

        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        Part part = new MekSensor();
        part.setUnit(mockUnit);

        assertNotNull(part.getUnitId());
        assertNull(part.getParentPart());
        assertNull(part.getRefitId());
        assertFalse(part.isReservedForReplacement());
        assertFalse(part.isSpare());
    }

    @Test
    public void isReservedForRefitNotSpare() {
        Part part = new MekSensor();
        part.setRefitId(UUID.randomUUID());

        assertNull(part.getUnitId());
        assertNull(part.getParentPart());
        assertNotNull(part.getRefitId());
        assertFalse(part.isReservedForReplacement());
        assertFalse(part.isSpare());
    }

    @Test
    public void isChildPartNotSpare() {
        Part parent = mock(Part.class);

        Part part = new MekSensor();
        part.setParentPart(parent);

        assertNull(part.getUnitId());
        assertNotNull(part.getParentPart());
        assertNull(part.getRefitId());
        assertFalse(part.isReservedForReplacement());
        assertFalse(part.isSpare());
    }

    @Test
    public void isReservedForReplacementNotSpare() {
        Person mockTech = mock(Person.class);
        when(mockTech.getId()).thenReturn(UUID.randomUUID());

        Part part = new MekSensor();
        part.setReserveId(mockTech);

        assertNull(part.getUnitId());
        assertNull(part.getParentPart());
        assertNull(part.getRefitId());
        assertTrue(part.isReservedForReplacement());
        assertFalse(part.isSpare());
    }
}
