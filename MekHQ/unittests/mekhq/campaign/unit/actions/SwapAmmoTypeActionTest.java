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
import static mekhq.campaign.parts.AmmoUtilities.*;

import org.junit.Test;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Mounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;

public class SwapAmmoTypeActionTest {
    @Test
    public void swapAmmoTypeSwapsMunitions() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType currentType = getAmmoType("ISSRM6 Ammo");
        AmmoType newAmmoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create a full ammo bin on a unit.
        int equipmentNum = 18;
        AmmoBin currentBin = new AmmoBin(0, currentType, equipmentNum, 0, false, false, mockCampaign);

        Unit unit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(currentType);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        currentBin.setUnit(unit);

        // Ensure the ammo bin is full.
        assertEquals(0, currentBin.getShotsNeeded());

        SwapAmmoTypeAction action = new SwapAmmoTypeAction(currentBin, newAmmoType);

        // Swap the ammo types.
        action.execute(mockCampaign, unit);

        // Ensure the action swapped the ammo type ...
        assertEquals(newAmmoType, currentBin.getType());

        // ... and left us needing to reload the bin.
        assertEquals(newAmmoType.getShots(), currentBin.getShotsNeeded());
    }

    @Test
    public void swapAmmoTypeSwapsMunitionsOnlyForSelectedUnit() {
        Campaign mockCampaign = mock(Campaign.class);

        AmmoType currentType = getAmmoType("ISSRM6 Ammo");
        AmmoType newAmmoType = getAmmoType("ISSRM6 Inferno Ammo");

        // Create a full ammo bin on a unit.
        int equipmentNum = 18;
        AmmoBin currentBin = new AmmoBin(0, currentType, equipmentNum, 0, false, false, mockCampaign);

        Unit unit = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(mockEntity);
        Mounted mockMounted = mock(Mounted.class);
        when(mockMounted.getType()).thenReturn(currentType);
        when(mockEntity.getEquipment(eq(equipmentNum))).thenReturn(mockMounted);
        currentBin.setUnit(unit);

        // Ensure the ammo bin is full.
        assertEquals(0, currentBin.getShotsNeeded());

        // Create some other unit.
        Unit otherUnit = mock(Unit.class);

        SwapAmmoTypeAction action = new SwapAmmoTypeAction(currentBin, newAmmoType);

        // Try swapping the ammo types for the wrong unit.
        action.execute(mockCampaign, otherUnit);

        // Ensure the action DID NOT swap the ammo type ...
        assertEquals(currentType, currentBin.getType());

        // ... and left us with a FULL bin.
        assertEquals(0, currentBin.getShotsNeeded());
    }
}
