/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.unit.actions;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.parts.MissingMekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

import org.junit.Test;

import megamek.common.Entity;

public class RestoreUnitActionTest {
    @Test
    public void restoreUnitDoesNotSkipPartsAfterRepairingMissingParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Quartermaster mockQuartermaster = mock(Quartermaster.class);
        when(mockCampaign.getQuartermaster()).thenReturn(mockQuartermaster);
        Entity mockEntity = mock(Entity.class);
        Unit unit = new Unit(mockEntity, mockCampaign);

        Part part0 = mock(Part.class);
        when(part0.getUnit()).thenReturn(unit);
        when(part0.getCampaign()).thenReturn(mockCampaign);
        EquipmentPart part1 = mock(EquipmentPart.class);
        when(part1.getUnit()).thenReturn(unit);
        when(part1.getCampaign()).thenReturn(mockCampaign);
        MissingMekLocation missing0 = mock(MissingMekLocation.class);
        when(missing0.getUnit()).thenReturn(unit);
        when(missing0.getCampaign()).thenReturn(mockCampaign);
        doAnswer(inv -> {
            // Simulate MissingPart::fix calling remove(false)
            unit.removePart(missing0);
            return null;
        }).when(missing0).fix();
        MissingEquipmentPart missing1 = mock(MissingEquipmentPart.class);
        when(missing1.getUnit()).thenReturn(unit);
        when(missing1.getCampaign()).thenReturn(mockCampaign);
        doAnswer(inv -> {
            // Simulate MissingPart::fix calling remove(false)
            unit.removePart(missing1);
            return null;
        }).when(missing1).fix();

        unit.addPart(part0);
        unit.addPart(missing0);
        unit.addPart(missing1);
        unit.addPart(part1);

        RestoreUnitAction action = new RestoreUnitAction();

        action.execute(mockCampaign, unit);

        verify(part0, atLeastOnce()).needsFixing();
        verify(missing0, times(1)).fix();
        verify(missing1, times(1)).fix();
        verify(part1, atLeastOnce()).needsFixing();
    }
}
