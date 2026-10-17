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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;

/**
 * The details of a spare say what a buyer needs to tell otherwise identical entries apart. The battle armor Small VSP
 * Laser shares its name and price with the Mek version but weighs a quarter as much, so it says it is battle armor
 * (issue #2673). A targeting computer in the parts store showed "0 tons" for every weight, the weight of the unit it
 * was for, which store stock does not have; it now shows its own weight (issue #9542).
 */
class EquipmentPartDetailsTest {
    private static final String PARTS_RESOURCES = "mekhq.resources.Parts";

    private final Campaign campaign = PartsScenario.create().getCampaign();

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    /** A spare as the parts store stocks it: no unit, so a unit weight of zero. */
    private EquipmentPart storeSpare(String internalName) {
        EquipmentType equipmentType = EquipmentType.get(internalName);
        assertNotNull(equipmentType, internalName);
        return new EquipmentPart(0, equipmentType, -1, 1.0, false, campaign);
    }

    private static String battleArmorNote() {
        return getTextAt(PARTS_RESOURCES, "EquipmentPart.battleArmor");
    }

    @Test
    void theBattleArmorSmallVspLaserSaysItIsBattleArmor() {
        EquipmentPart battleArmorLaser = storeSpare("ISBASmallVSPLaser");

        assertEquals(0.5, battleArmorLaser.getTonnage());
        assertTrue(battleArmorLaser.getDetails().contains(battleArmorNote()), battleArmorLaser.getDetails());
    }

    @Test
    void theMekSmallVspLaserDoesNotSayItIsBattleArmor() {
        EquipmentPart mekLaser = storeSpare("ISSmallVSPLaser");

        assertEquals(2.0, mekLaser.getTonnage());
        assertFalse(mekLaser.getDetails().contains(battleArmorNote()), mekLaser.getDetails());
    }

    @Test
    void aStoreTargetingComputerShowsItsOwnWeight() {
        EquipmentPart targetingComputer = storeSpare("ISTargeting Computer");
        targetingComputer.setEquipTonnage(3);

        String details = targetingComputer.getDetails();

        assertTrue(details.contains(getFormattedTextAt(PARTS_RESOURCES, "EquipmentPart.tonnage", 3.0)), details);
        assertFalse(details.contains("0 tons"), details);
    }

    @Test
    void storeTargetingComputersOfDifferentWeightsHaveDifferentDetails() {
        EquipmentPart lightComputer = storeSpare("ISTargeting Computer");
        lightComputer.setEquipTonnage(2);
        EquipmentPart heavyComputer = storeSpare("ISTargeting Computer");
        heavyComputer.setEquipTonnage(5);

        assertNotEquals(lightComputer.getDetails(), heavyComputer.getDetails());
    }
}
