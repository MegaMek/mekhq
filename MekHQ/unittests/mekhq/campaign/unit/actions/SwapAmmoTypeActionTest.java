/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.actions;

import static mekhq.campaign.parts.AmmoUtilities.getAmmoType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Mounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

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
