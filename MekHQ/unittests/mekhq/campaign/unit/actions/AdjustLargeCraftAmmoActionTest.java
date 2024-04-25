/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;

import static mekhq.campaign.parts.AmmoUtilities.getAmmoType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

public class AdjustLargeCraftAmmoActionTest {
    @Test
    public void onlyAcceptsEntitiesUsingBayWeapons() {
        Campaign campaign = mock(Campaign.class);
        Quartermaster quartermaster = mock(Quartermaster.class);
        when(campaign.getQuartermaster()).thenReturn(quartermaster);
        Unit unit = mock(Unit.class);
        when(unit.getCampaign()).thenReturn(campaign);
        Entity entity = mock(Entity.class);
        when(entity.usesWeaponBays()).thenReturn(false);
        when(unit.getEntity()).thenReturn(entity);

        AdjustLargeCraftAmmoAction action = new AdjustLargeCraftAmmoAction();

        action.execute(campaign, unit);

        verify(unit, times(0)).addPart(any());
        verify(quartermaster, times(0)).addPart(any(), anyInt());
    }

    @Test
    public void doesNothingWithNoAmmoBays() {
        Campaign campaign = mock(Campaign.class);
        Quartermaster quartermaster = mock(Quartermaster.class);
        when(campaign.getQuartermaster()).thenReturn(quartermaster);
        Unit unit = mock(Unit.class);
        when(unit.getCampaign()).thenReturn(campaign);
        Entity entity = mock(Entity.class);
        when(entity.usesWeaponBays()).thenReturn(true);
        when(entity.getAmmo()).thenReturn(new ArrayList<>());
        when(unit.getEntity()).thenReturn(entity);

        AdjustLargeCraftAmmoAction action = new AdjustLargeCraftAmmoAction();

        action.execute(campaign, unit);

        verify(unit, times(0)).addPart(any());
        verify(quartermaster, times(0)).addPart(any(), anyInt());
    }

    @Test
    public void addsMissingBins() {
        Campaign campaign = mock(Campaign.class);
        Quartermaster quartermaster = mock(Quartermaster.class);
        when(campaign.getQuartermaster()).thenReturn(quartermaster);

        // Put together a unit with 1 bay, with 1 ammo type
        Unit unit = mock(Unit.class);
        when(unit.getCampaign()).thenReturn(campaign);
        Entity entity = mock(Entity.class);
        when(entity.usesWeaponBays()).thenReturn(true);
        when(entity.getWeight()).thenReturn(500.0);
        int equipmentNum = 11;
        AmmoMounted bin0 = mock(AmmoMounted.class);
        doReturn(equipmentNum).when(entity).getEquipmentNum(eq(bin0));
        AmmoType ammoType0 = getAmmoType("ISLRM20 Ammo");
        when(bin0.getType()).thenReturn(ammoType0);
        int capacity = 10;
        when(bin0.getSize()).thenReturn((double) capacity);
        when(bin0.getOriginalShots()).thenReturn(capacity * ammoType0.getShots());
        int onHand = capacity - 1;
        when(bin0.getBaseShotsLeft()).thenReturn(onHand * ammoType0.getShots());
        ArrayList<AmmoMounted> ammo = new ArrayList<>();
        ammo.add(bin0);
        when(entity.getAmmo()).thenReturn(ammo);
        int bayEquipmentNum = 18;
        WeaponMounted bay = mock(WeaponMounted.class);
        doReturn(bay).when(entity).getBayByAmmo(eq(bin0));
        doReturn(bayEquipmentNum).when(entity).getEquipmentNum(eq(bay));
        when(unit.getEntity()).thenReturn(entity);
        // Handle parts being added to units having their unit set
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());

        AdjustLargeCraftAmmoAction action = new AdjustLargeCraftAmmoAction();

        action.execute(campaign, unit);

        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(unit, times(1)).addPart(partCaptor.capture());

        Part addedPart = partCaptor.getValue();
        verify(quartermaster, times(1)).addPart(eq(addedPart), eq(0));

        assertInstanceOf(LargeCraftAmmoBin.class, addedPart);

        LargeCraftAmmoBin ammoBin = (LargeCraftAmmoBin) addedPart;
        assertEquals(ammoType0, ammoBin.getType());
        assertEquals(equipmentNum, ammoBin.getEquipmentNum());
        assertEquals(capacity, ammoBin.getCapacity(), 0.001);
        assertEquals((capacity - onHand) * ammoType0.getShots(), ammoBin.getShotsNeeded());
        assertEquals(bayEquipmentNum, ammoBin.getBayEqNum());
        assertEquals(bay, ammoBin.getBay());
    }

    @Test
    public void updatesExistingBayToMatchType() {
        Campaign campaign = mock(Campaign.class);
        Quartermaster quartermaster = mock(Quartermaster.class);
        when(campaign.getQuartermaster()).thenReturn(quartermaster);

        // Put together a unit with 1 bay, 1 ammo type, and 1 bin on the unit already
        Unit unit = mock(Unit.class);
        when(unit.getCampaign()).thenReturn(campaign);
        Entity entity = mock(Entity.class);
        when(entity.usesWeaponBays()).thenReturn(true);
        when(entity.getWeight()).thenReturn(500.0);
        int equipmentNum = 11;
        AmmoMounted bin0 = mock(AmmoMounted.class);
        doReturn(equipmentNum).when(entity).getEquipmentNum(eq(bin0));
        AmmoType ammoType0 = getAmmoType("ISLRM20 Ammo");
        when(bin0.getType()).thenReturn(ammoType0);
        ArrayList<AmmoMounted> ammo = new ArrayList<>();
        ammo.add(bin0);
        when(entity.getAmmo()).thenReturn(ammo);
        when(unit.getEntity()).thenReturn(entity);
        LargeCraftAmmoBin ammoBin = mock(LargeCraftAmmoBin.class);
        when(ammoBin.getEquipmentNum()).thenReturn(equipmentNum);
        when(unit.getParts()).thenReturn(Arrays.asList(new Part[] { ammoBin }));

        AdjustLargeCraftAmmoAction action = new AdjustLargeCraftAmmoAction();

        action.execute(campaign, unit);

        // Ensure we didn't add any new parts
        verify(unit, times(0)).addPart(any());
        verify(quartermaster, times(0)).addPart(any(), anyInt());

        // Ensure we updated the part's type
        verify(ammoBin, times(1)).changeMunition(eq(ammoType0));
    }

    @Test
    public void addsMissingBinsSkipsNonLargeCraftBins() {
        Campaign campaign = mock(Campaign.class);
        Quartermaster quartermaster = mock(Quartermaster.class);
        when(campaign.getQuartermaster()).thenReturn(quartermaster);

        // Put together a unit with 1 bay, with 1 ammo type
        Unit unit = mock(Unit.class);
        when(unit.getCampaign()).thenReturn(campaign);
        Entity entity = mock(Entity.class);
        when(entity.usesWeaponBays()).thenReturn(true);
        when(entity.getWeight()).thenReturn(500.0);
        int equipmentNum = 11;
        AmmoMounted bin0 = mock(AmmoMounted.class);
        doReturn(equipmentNum).when(entity).getEquipmentNum(eq(bin0));
        AmmoType ammoType0 = getAmmoType("ISLRM20 Ammo");
        when(bin0.getType()).thenReturn(ammoType0);
        int capacity = 10;
        when(bin0.getSize()).thenReturn((double) capacity);
        when(bin0.getOriginalShots()).thenReturn(capacity * ammoType0.getShots());
        int onHand = capacity - 1;
        when(bin0.getBaseShotsLeft()).thenReturn(onHand * ammoType0.getShots());
        ArrayList<AmmoMounted> ammo = new ArrayList<>();
        ammo.add(bin0);
        when(entity.getAmmo()).thenReturn(ammo);
        int bayEquipmentNum = 18;
        WeaponMounted bay = mock(WeaponMounted.class);
        doReturn(bay).when(entity).getBayByAmmo(eq(bin0));
        doReturn(bayEquipmentNum).when(entity).getEquipmentNum(eq(bay));
        when(unit.getEntity()).thenReturn(entity);
        AmmoBin otherAmmoBin = mock(AmmoBin.class);
        when(unit.getParts()).thenReturn(Arrays.asList(new Part[] { otherAmmoBin }));
        // Handle parts being added to units having their unit set
        doAnswer(inv -> {
            Part part = inv.getArgument(0);
            part.setUnit(unit);
            return null;
        }).when(unit).addPart(any());

        AdjustLargeCraftAmmoAction action = new AdjustLargeCraftAmmoAction();

        action.execute(campaign, unit);

        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(unit, times(1)).addPart(partCaptor.capture());

        Part addedPart = partCaptor.getValue();
        verify(quartermaster, times(1)).addPart(eq(addedPart), eq(0));

        assertInstanceOf(LargeCraftAmmoBin.class, addedPart);

        LargeCraftAmmoBin ammoBin = (LargeCraftAmmoBin) addedPart;
        assertEquals(ammoType0, ammoBin.getType());
        assertEquals(equipmentNum, ammoBin.getEquipmentNum());
        assertEquals(capacity, ammoBin.getCapacity(), 0.001);
        assertEquals((capacity - onHand) * ammoType0.getShots(), ammoBin.getShotsNeeded());
        assertEquals(bayEquipmentNum, ammoBin.getBayEqNum());
        assertEquals(bay, ammoBin.getBay());
    }

    @Test
    public void updatesExistingBayToMatchTypeSkipsNonLargeCraftBins() {
        Campaign campaign = mock(Campaign.class);
        Quartermaster quartermaster = mock(Quartermaster.class);
        when(campaign.getQuartermaster()).thenReturn(quartermaster);

        // Put together a unit with 1 bay, 1 ammo type, and 1 bin on the unit already
        Unit unit = mock(Unit.class);
        when(unit.getCampaign()).thenReturn(campaign);
        Entity entity = mock(Entity.class);
        when(entity.usesWeaponBays()).thenReturn(true);
        when(entity.getWeight()).thenReturn(500.0);
        int equipmentNum = 11;
        AmmoMounted bin0 = mock(AmmoMounted.class);
        doReturn(equipmentNum).when(entity).getEquipmentNum(eq(bin0));
        AmmoType ammoType0 = getAmmoType("ISLRM20 Ammo");
        when(bin0.getType()).thenReturn(ammoType0);
        ArrayList<AmmoMounted> ammo = new ArrayList<>();
        ammo.add(bin0);
        when(entity.getAmmo()).thenReturn(ammo);
        when(unit.getEntity()).thenReturn(entity);
        LargeCraftAmmoBin ammoBin = mock(LargeCraftAmmoBin.class);
        when(ammoBin.getEquipmentNum()).thenReturn(equipmentNum);
        AmmoBin otherAmmoBin = mock(AmmoBin.class);
        when(unit.getParts()).thenReturn(Arrays.asList(new Part[] { otherAmmoBin, ammoBin }));

        AdjustLargeCraftAmmoAction action = new AdjustLargeCraftAmmoAction();

        action.execute(campaign, unit);

        // Ensure we didn't add any new parts
        verify(unit, times(0)).addPart(any());
        verify(quartermaster, times(0)).addPart(any(), anyInt());

        // Ensure we updated the part's type
        verify(ammoBin, times(1)).changeMunition(eq(ammoType0));
    }
}
