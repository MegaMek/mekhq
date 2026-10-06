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
package mekhq.campaign.parts.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regression tests for issue #9756: a Retractable Blade sitting in the warehouse was not recognised as a replacement
 * for a damaged blade, so the unit could not be repaired.
 *
 * <p>These tests deliberately use real {@link EquipmentType} instances rather than mocks. The defect lived in the
 * interaction between MekHQ's own pricing formula for unit-less parts and MegaMek's pricing formula for mounted
 * equipment, so a mocked equipment type cannot exercise it.</p>
 */
class RetractableBladeWarehouseTest {

    private static final String RETRACTABLE_BLADE = "Retractable Blade";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private Entity mekOfWeight(double weightInTons) {
        Entity mek = new BipedMek();
        mek.setWeight(weightInTons);
        return mek;
    }

    /**
     * Builds a loose warehouse part the way {@code PartsStore} stocks variable-tonnage equipment: no owning unit, and
     * the item weight set explicitly.
     */
    private EquipmentPart looseWarehousePart(Campaign campaign, EquipmentType equipmentType, double itemTonnage) {
        EquipmentPart loosePart = new EquipmentPart(0, equipmentType, -1, 1.0, false, campaign);
        loosePart.setEquipTonnage(itemTonnage);
        return loosePart;
    }

    /**
     * A loose blade must carry the same sticker price as the same blade mounted on a unit. Before the fix MekHQ
     * charged for the half-ton retraction mechanism twice, leaving every loose blade 5,000 C-bills too expensive.
     */
    @ParameterizedTest
    @ValueSource(doubles = { 20.0, 35.0, 55.0, 80.0, 100.0 })
    void looseBladeCostsTheSameAsAMountedBlade(double unitTonnage) {
        Campaign campaign = mockCampaign();
        EquipmentType bladeType = EquipmentType.get(RETRACTABLE_BLADE);
        Entity mek = mekOfWeight(unitTonnage);

        double mountedTonnage = bladeType.getTonnage(mek, 1.0);
        double mountedCost = bladeType.getCost(mek, false, Entity.LOC_NONE, 1.0);
        EquipmentPart loosePart = looseWarehousePart(campaign, bladeType, mountedTonnage);

        assertEquals(mountedCost,
              loosePart.getStickerPrice().getAmount().doubleValue(),
              0.001,
              "A loose Retractable Blade must cost the same as the identical mounted blade");
    }

    /**
     * The reported failure: a damaged blade on the reporter's 80-ton chassis could not be repaired from warehouse
     * stock. The store already carries a blade of the correct weight, so the replacement must be accepted.
     */
    @Test
    void warehouseBladeIsAcceptedAsAReplacementForADamagedBlade() {
        Campaign campaign = mockCampaign();
        EquipmentType bladeType = EquipmentType.get(RETRACTABLE_BLADE);
        Entity mek = mekOfWeight(80.0);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(mek);

        double mountedTonnage = bladeType.getTonnage(mek, 1.0);
        MissingEquipmentPart damagedBlade =
              new MissingEquipmentPart(80, bladeType, -1, campaign, mountedTonnage, 1.0, false);
        damagedBlade.setUnit(unit);

        EquipmentPart warehouseBlade = looseWarehousePart(campaign, bladeType, mountedTonnage);

        assertTrue(damagedBlade.isAcceptableReplacement(warehouseBlade, false),
              "Warehouse stock of the correct weight must be usable to repair a damaged Retractable Blade");
    }

    /**
     * A blade built for a heavier unit is a different item and must not be substituted.
     */
    @Test
    void warehouseBladeOfTheWrongWeightIsRejected() {
        Campaign campaign = mockCampaign();
        EquipmentType bladeType = EquipmentType.get(RETRACTABLE_BLADE);
        Entity mek = mekOfWeight(80.0);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(mek);

        MissingEquipmentPart damagedBlade =
              new MissingEquipmentPart(80, bladeType, -1, campaign, bladeType.getTonnage(mek, 1.0), 1.0, false);
        damagedBlade.setUnit(unit);

        EquipmentPart oversizedBlade =
              looseWarehousePart(campaign, bladeType, bladeType.getTonnage(mekOfWeight(100.0), 1.0));

        assertFalse(damagedBlade.isAcceptableReplacement(oversizedBlade, false),
              "A blade built for a 100-ton unit must not repair an 80-ton unit's blade");
    }

    /**
     * The blade fix changes a branch shared by every physical weapon, so the other clubs must keep pricing correctly.
     */
    @ParameterizedTest
    @ValueSource(strings = { "Hatchet", "Sword", "Mace", "Talons" })
    void otherPhysicalWeaponsStillPriceCorrectly(String equipmentName) {
        Campaign campaign = mockCampaign();
        EquipmentType clubType = EquipmentType.get(equipmentName);

        for (double unitTonnage : new double[] { 20.0, 55.0, 80.0, 100.0 }) {
            Entity mek = mekOfWeight(unitTonnage);
            double mountedCost = clubType.getCost(mek, false, Entity.LOC_NONE, 1.0);
            EquipmentPart loosePart = looseWarehousePart(campaign, clubType, clubType.getTonnage(mek, 1.0));

            assertEquals(mountedCost,
                  loosePart.getStickerPrice().getAmount().doubleValue(),
                  0.001,
                  equipmentName + " pricing must be unchanged at " + unitTonnage + " tons");
        }
    }
}
