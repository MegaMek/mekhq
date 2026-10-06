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

import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEK_TECH;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_BASIC_TOOLKIT;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_DESCARTES_MK_XXV;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_PERSONAL_COMPUTER;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_WEAPON;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.REPAIR_KIT_ROLL_BONUS;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.person;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.wireEquipmentKits;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_WEAPONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KitProfession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Coverage for the per-role profession lookup that decides each kit slot's default, and for the kit checks reading
 * both equipment-kit slots.
 */
class EquipmentKitCatalogSlotsTest {
    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    // region professionFor
    @Test
    void professionForMapsEveryKitRoleAndNothingElse() {
        Map<PersonnelRole, KitProfession> expected = new EnumMap<>(PersonnelRole.class);
        expected.put(PersonnelRole.MEK_TECH, KitProfession.MEK_TECH);
        expected.put(PersonnelRole.MECHANIC, KitProfession.MECHANIC);
        expected.put(PersonnelRole.AERO_TEK, KitProfession.AERO_TEK);
        expected.put(PersonnelRole.BA_TECH, KitProfession.BA_TECH);
        expected.put(PersonnelRole.VESSEL_PILOT, KitProfession.VESSEL_CREW);
        expected.put(PersonnelRole.VESSEL_GUNNER, KitProfession.VESSEL_CREW);
        expected.put(PersonnelRole.VESSEL_CREW, KitProfession.VESSEL_CREW);
        expected.put(PersonnelRole.VESSEL_NAVIGATOR, KitProfession.VESSEL_CREW);
        expected.put(PersonnelRole.ASTECH, KitProfession.ASTECH);
        expected.put(PersonnelRole.DOCTOR, KitProfession.DOCTOR);
        expected.put(PersonnelRole.MEDIC, KitProfession.MEDIC);
        expected.put(PersonnelRole.ADMINISTRATOR, KitProfession.ADMIN);

        for (PersonnelRole role : PersonnelRole.values()) {
            assertEquals(expected.get(role), EquipmentKitCatalog.professionFor(role), role.name());
        }
    }

    @Test
    void administratorsHaveTheAdminProfession() {
        // The role first reported as missing its default kit.
        assertEquals(KitProfession.ADMIN, EquipmentKitCatalog.professionFor(ADMINISTRATOR));
    }

    @Test
    void professionForIsNullForNullOrANonKitRole() {
        assertNull(EquipmentKitCatalog.professionFor(null));
        assertNull(EquipmentKitCatalog.professionFor(PersonnelRole.MEKWARRIOR));
        assertNull(EquipmentKitCatalog.professionFor(NONE));
    }

    @Test
    void everyProfessionHasADefaultKitOption() {
        for (KitProfession profession : KitProfession.values()) {
            assertTrue(EquipmentKitIssuer.defaultKitOption(profession) != null, profession.name());
        }
    }
    // endregion professionFor

    // region two-slot checks
    @Test
    void hasToolKitIsTrueForARepairKitInTheSecondarySlot() {
        Person tech = person(MEK_TECH, NONE);
        wireEquipmentKits(tech, KIT_DESCARTES_MK_XXV, KIT_WEAPON);
        assertTrue(EquipmentKitCatalog.hasToolKit(tech));
    }

    @Test
    void hasToolKitIsFalseForADescartesScannerAlone() {
        Person tech = person(MEK_TECH, NONE);
        wireEquipmentKits(tech, KIT_DESCARTES_MK_XXV, null);
        assertFalse(EquipmentKitCatalog.hasToolKit(tech));
    }

    @Test
    void hasToolKitIsFalseForNonTechKitsInBothSlots() {
        Person admin = person(ADMINISTRATOR, NONE);
        wireEquipmentKits(admin, KIT_PERSONAL_COMPUTER, KIT_DESCARTES_MK_XXV);
        assertFalse(EquipmentKitCatalog.hasToolKit(admin));
    }

    @Test
    void kitSkillBonusesCombineBothSlots() {
        Person person = person(MEK_TECH, ADMINISTRATOR);
        wireEquipmentKits(person, KIT_WEAPON, KIT_PERSONAL_COMPUTER);

        Map<String, Integer> bonuses = EquipmentKitCatalog.kitSkillBonuses(person);

        assertEquals(REPAIR_KIT_ROLL_BONUS, bonuses.get(S_TECH_WEAPONS));
        assertEquals(1, bonuses.get(S_ADMIN));
    }

    @Test
    void maintenanceBonusReadsTheSecondarySlot() {
        Person tech = person(MEK_TECH, NONE);
        wireEquipmentKits(tech, KIT_BASIC_TOOLKIT, KIT_DESCARTES_MK_XXV);
        assertEquals(2, EquipmentKitCatalog.maintenanceBonus(tech));
    }
    // endregion two-slot checks
}
