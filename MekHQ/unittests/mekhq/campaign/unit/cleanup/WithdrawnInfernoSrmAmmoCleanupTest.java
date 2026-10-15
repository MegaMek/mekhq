/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.cleanup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceQuartermaster;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.parts.InfantryAmmoStorage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.InfantryAmmoBin;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the campaign-load cleanup of Inferno ammo for infantry SRM launchers, which MegaMek withdrew per the
 * TechManual pp. 350-352 errata. Example: a Death Trike saved with its two-shot SRM launcher split one clip
 * standard, one clip Inferno.
 */
class WithdrawnInfernoSrmAmmoCleanupTest {

    private static AmmoType standardAmmo;
    private static AmmoType infernoAmmo;
    private static InfantryWeapon srmLauncher;

    private Campaign campaign;
    private ForceQuartermaster quartermaster;
    private LocalWarehouse warehouse;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
        standardAmmo = (AmmoType) EquipmentType.get(EquipmentTypeLookup.INFANTRY_AMMO);
        infernoAmmo = (AmmoType) EquipmentType.get(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        srmLauncher = (InfantryWeapon) EquipmentType.get("InfantryStandardSRM");
    }

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        quartermaster = mock(ForceQuartermaster.class);
        warehouse = mock(LocalWarehouse.class);
        when(campaign.getQuartermaster()).thenReturn(quartermaster);
    }

    private InfantryAmmoBin bin(AmmoType ammoType, int equipmentNumber, int clips, int fullShots, int loadedShots) {
        InfantryAmmoBin bin = mock(InfantryAmmoBin.class);
        when(bin.getType()).thenReturn(ammoType);
        when(bin.getWeaponType()).thenReturn(srmLauncher);
        when(bin.getEquipmentNum()).thenReturn(equipmentNumber);
        when(bin.getClips()).thenReturn(clips);
        when(bin.getFullShots()).thenReturn(fullShots);
        when(bin.getLoadedShots()).thenReturn(loadedShots);
        // As if the bin's old equipment number now pointed at another weapon's ammo: the mount-checked figure
        // would claim the bin is empty.
        when(bin.getShotsNeeded()).thenReturn(fullShots);
        when(bin.getCampaign()).thenReturn(campaign);
        when(bin.getWarehouse()).thenReturn(warehouse);
        return bin;
    }

    private static Unit unitWith(Part... parts) {
        Unit unit = mock(Unit.class);
        List<Part> unitParts = new ArrayList<>(List.of(parts));
        when(unit.getParts()).thenReturn(unitParts);
        when(unit.getName()).thenReturn("Death Trike");
        return unit;
    }

    @Test
    @DisplayName("only Inferno ammo for an infantry SRM launcher counts as withdrawn")
    void recognisesWithdrawnInfernoAmmo() {
        InfantryWeapon recoillessRifle = (InfantryWeapon) EquipmentType.get("InfantryLRR");

        assertTrue(WithdrawnInfernoSrmAmmoCleanup.isWithdrawnInfernoAmmo(infernoAmmo, srmLauncher),
              "Inferno ammo for an SRM launcher was withdrawn by the errata");
        assertFalse(WithdrawnInfernoSrmAmmoCleanup.isWithdrawnInfernoAmmo(standardAmmo, srmLauncher),
              "Standard SRM ammo is unaffected");
        assertFalse(WithdrawnInfernoSrmAmmoCleanup.isWithdrawnInfernoAmmo(infernoAmmo, recoillessRifle),
              "Weapons that still have an incendiary variant keep their second bin");
    }

    @Test
    @DisplayName("the Inferno bin is removed, its ammo returned as standard, and its clip given back")
    void removesInfernoBinAndRestoresStandardBin() {
        InfantryAmmoBin standardBin = bin(standardAmmo, 5, 1, 2, 2);
        InfantryAmmoBin infernoBin = bin(infernoAmmo, 6, 1, 2, 2);
        Unit unit = unitWith(standardBin, infernoBin);

        WithdrawnInfernoSrmAmmoCleanup.cleanUnit(unit);

        verify(unit).removePart(infernoBin);
        verify(warehouse).removePart(infernoBin);
        verify(quartermaster).addAmmo(warehouse, standardAmmo, srmLauncher, 2);
        verify(standardBin).changeCapacity(2);
        verify(unit, never()).removePart(standardBin);
    }

    @Test
    @DisplayName("an empty Inferno bin returns no ammo")
    void emptyInfernoBinReturnsNothing() {
        InfantryAmmoBin standardBin = bin(standardAmmo, 5, 1, 2, 2);
        InfantryAmmoBin infernoBin = bin(infernoAmmo, 6, 1, 2, 0);

        WithdrawnInfernoSrmAmmoCleanup.cleanUnit(unitWith(standardBin, infernoBin));

        verify(quartermaster, never()).addAmmo(any(LocalWarehouse.class), any(AmmoType.class),
              any(InfantryWeapon.class), anyInt());
        verify(standardBin).changeCapacity(2);
    }

    @Test
    @DisplayName("a unit without Inferno SRM ammo is left alone")
    void unitWithoutInfernoBinIsUntouched() {
        InfantryAmmoBin standardBin = bin(standardAmmo, 5, 2, 4, 4);
        Unit unit = unitWith(standardBin);

        WithdrawnInfernoSrmAmmoCleanup.cleanUnit(unit);

        verify(unit, never()).removePart(any());
        verify(standardBin, never()).changeCapacity(anyInt());
    }

    @Test
    @DisplayName("Inferno SRM ammo in a warehouse becomes standard ammo for the same launcher")
    void warehouseInfernoStockBecomesStandard() {
        InfantryAmmoStorage infernoStock = mock(InfantryAmmoStorage.class);
        when(infernoStock.getType()).thenReturn(infernoAmmo);
        when(infernoStock.getWeaponType()).thenReturn(srmLauncher);
        when(infernoStock.getShots()).thenReturn(4);
        when(infernoStock.getCampaign()).thenReturn(campaign);
        when(warehouse.getParts()).thenReturn(List.of(infernoStock));

        WithdrawnInfernoSrmAmmoCleanup.cleanWarehouse(warehouse);

        verify(warehouse).removePart(infernoStock);
        verify(quartermaster).addAmmo(warehouse, standardAmmo, srmLauncher, 4);
    }
}
