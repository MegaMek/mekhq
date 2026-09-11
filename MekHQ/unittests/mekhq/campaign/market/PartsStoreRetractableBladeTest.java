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
package mekhq.campaign.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for issue #9756. The parts store used to stock Retractable Blades with no unit tonnage, so a loose
 * blade could never be matched against the damaged blade it was meant to replace.
 */
class PartsStoreRetractableBladeTest {

    private static final String RETRACTABLE_BLADE = "Retractable Blade";
    private static final int REPORTED_CHASSIS_TONNAGE = 80;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private List<EquipmentPart> stockedBlades(Campaign campaign) {
        PartsStore partsStore = new PartsStore();
        partsStore.stockWeaponsAmmoAndEquipment(campaign);

        EquipmentType bladeType = EquipmentType.get(RETRACTABLE_BLADE);
        return partsStore.getInventory()
                     .stream()
                     .filter(EquipmentPart.class::isInstance)
                     .map(EquipmentPart.class::cast)
                     .filter(part -> bladeType.equals(part.getType()))
                     .filter(part -> !part.isOmniPodded())
                     .toList();
    }

    /**
     * Every stocked blade must know which chassis weight it was built for, otherwise it can never be matched to the
     * blade it replaces.
     */
    @Test
    void everyStockedBladeCarriesAChassisTonnage() {
        List<EquipmentPart> blades = stockedBlades(mockCampaign());

        assertTrue(!blades.isEmpty(), "The parts store must stock Retractable Blades");
        for (EquipmentPart blade : blades) {
            assertTrue(blade.getUnitTonnage() > 0,
                  "Stocked Retractable Blades must record the chassis tonnage they were built for");
        }
    }

    /**
     * The reported case: with a damaged blade on an 80-ton chassis, the matching blade in the store must both be
     * counted as stock and be usable as the replacement.
     */
    @Test
    void stockedBladeMatchesADamagedBladeOnTheReportedChassis() {
        Campaign campaign = mockCampaign();
        EquipmentType bladeType = EquipmentType.get(RETRACTABLE_BLADE);

        Entity chassis = new BipedMek();
        chassis.setWeight(REPORTED_CHASSIS_TONNAGE);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(chassis);

        MissingEquipmentPart damagedBlade = new MissingEquipmentPart(REPORTED_CHASSIS_TONNAGE,
              bladeType,
              -1,
              campaign,
              bladeType.getTonnage(chassis, 1.0),
              1.0,
              false);
        damagedBlade.setUnit(unit);
        Part requiredBlade = damagedBlade.getNewPart();

        EquipmentPart storeBlade = stockedBlades(campaign).stream()
                                         .filter(part -> part.getUnitTonnage() == REPORTED_CHASSIS_TONNAGE)
                                         .findFirst()
                                         .orElse(null);

        assertNotNull(storeBlade, "The parts store must stock a blade for an 80-ton chassis");
        assertEquals(bladeType.getCost(chassis, false, Entity.LOC_NONE, 1.0),
              storeBlade.getStickerPrice().getAmount().doubleValue(),
              0.001,
              "The stocked blade must cost the same as the blade it replaces");
        assertTrue(requiredBlade.isSamePartType(storeBlade),
              "The stocked blade must be counted as available stock");
        assertTrue(damagedBlade.isAcceptableReplacement(storeBlade, false),
              "The stocked blade must be usable to repair the damaged blade");
    }
}
