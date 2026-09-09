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
package mekhq.gui.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog;
import mekhq.campaign.personnel.quartermaster.RepairKitCatalog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EquipmentSummaryPanelTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private static Person person(String armorKit) {
        return person(armorKit, null);
    }

    private static Person person(String armorKit, String toolKit) {
        Person person = mock(Person.class);
        when(person.getArmorKitName()).thenReturn(armorKit);
        when(person.getRepairKitName()).thenReturn(toolKit);
        when(person.hasRepairKit(anyString())).thenAnswer(invocation -> invocation.getArgument(0).equals(toolKit));
        return person;
    }

    @Test
    void hasEquipmentIsTrueWhenTheTechCarriesAToolKit() {
        assertTrue(EquipmentSummaryPanel.hasEquipment(
              person(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME, RepairKitCatalog.KIT_BASIC_TOOLKIT)));
    }

    @Test
    void hasEquipmentIsTrueWhenWearingANonDefaultArmorKit() {
        assertTrue(EquipmentSummaryPanel.hasEquipment(person(ArmorKitCatalog.KIT_MEKWARRIOR_ADVANCED)));
    }

    @Test
    void hasEquipmentIsFalseForOnlyTheDefaultCoverallsAndNoKits() {
        assertFalse(EquipmentSummaryPanel.hasEquipment(person(ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME)));
    }

    @Test
    void hasEquipmentIsFalseWhenTheArmorKitIsNullAndNoKitsAreCarried() {
        assertFalse(EquipmentSummaryPanel.hasEquipment(person(null)));
    }
}
