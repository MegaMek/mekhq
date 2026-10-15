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

package mekhq.campaign.personnel.quartermaster;

import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_BASIC_TOOLKIT;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_DESCARTES_MK_XXI;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_DESCARTES_MK_XXV;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_MEDICAL;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.NO_DEFAULT_KIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.Test;

/**
 * A technician's default kit goes into the slot they work from, so the choices offered for it must all count as a tool
 * kit. A Descartes scanner on its own left every new MekTech unable to repair when "Techs Need a Tool Kit" was on
 * (issue 10474).
 */
class EquipmentKitTechnicianDefaultsTest {

    @Test
    void technicianDefaultsOfferNoneFirst() {
        assertEquals(NO_DEFAULT_KIT, EquipmentKitCatalog.technicianOptionKitNames().getFirst());
    }

    @Test
    void technicianDefaultsLeaveOutTheDescartesScanners() {
        List<String> choices = EquipmentKitCatalog.technicianOptionKitNames();

        assertFalse(choices.contains(KIT_DESCARTES_MK_XXI));
        assertFalse(choices.contains(KIT_DESCARTES_MK_XXV));
        assertTrue(choices.contains(KIT_BASIC_TOOLKIT));
    }

    @Test
    void everyTechnicianDefaultCountsAsAToolKit() {
        List<String> kits = EquipmentKitCatalog.technicianOptionKitNames()
                                  .stream()
                                  .filter(kitName -> !NO_DEFAULT_KIT.equals(kitName))
                                  .toList();

        for (String kitName : kits) {
            Person technician = technicianCarrying(kitName);
            assertTrue(EquipmentKitCatalog.hasToolKit(technician), kitName + " should count as a tool kit");
        }
    }

    @Test
    void otherProfessionsStillOfferEveryKit() {
        List<String> choices = EquipmentKitCatalog.optionKitNames();

        assertTrue(choices.contains(KIT_DESCARTES_MK_XXI));
        assertTrue(choices.contains(KIT_MEDICAL));
    }

    @Test
    void aDescartesScannerAloneIsNotAToolKit() {
        assertFalse(EquipmentKitCatalog.hasToolKit(technicianCarrying(KIT_DESCARTES_MK_XXI)));
    }

    @Test
    void countsOnlyTheTechniciansWithoutAToolKit() {
        List<Person> technicians = List.of(technicianCarrying(KIT_DESCARTES_MK_XXI),
              technicianCarrying(KIT_BASIC_TOOLKIT),
              technicianCarrying(null));

        assertEquals(2, EquipmentKitCatalog.countWithoutToolKit(technicians));
    }

    @Test
    void countsADescartesScannerAlongsideAToolkitAsEquipped() {
        Person technician = KitTestSupport.person(PersonnelRole.MEK_TECH, PersonnelRole.NONE);
        KitTestSupport.wireEquipmentKits(technician, KIT_DESCARTES_MK_XXI, KIT_BASIC_TOOLKIT);

        assertEquals(0, EquipmentKitCatalog.countWithoutToolKit(List.of(technician)));
    }

    private static Person technicianCarrying(String kitName) {
        Person technician = KitTestSupport.person(PersonnelRole.MEK_TECH, PersonnelRole.NONE);
        KitTestSupport.wireEquipmentKits(technician, kitName, null);
        return technician;
    }
}
