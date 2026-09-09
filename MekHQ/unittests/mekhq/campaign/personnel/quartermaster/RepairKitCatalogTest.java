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
import static mekhq.campaign.personnel.enums.PersonnelRole.AERO_TEK;
import static mekhq.campaign.personnel.enums.PersonnelRole.BA_TECH;
import static mekhq.campaign.personnel.enums.PersonnelRole.DOCTOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MECHANIC;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEK_TECH;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.quartermaster.RepairKitCatalog.*;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_MEDTECH;
import static mekhq.campaign.personnel.skills.SkillType.S_NAVIGATION;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static mekhq.campaign.personnel.skills.SkillType.S_SURVIVAL;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_ELECTRONIC;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MECHANICAL;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_WEAPONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RepairKitCatalogTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private static Person personWithRoles(PersonnelRole primary, PersonnelRole secondary) {
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(primary);
        when(person.getSecondaryRole()).thenReturn(secondary);
        return person;
    }

    /** A person carrying exactly the one named tool kit ({@code null} for none). */
    private static Person personWithKit(String kit) {
        Person person = mock(Person.class);
        when(person.getRepairKitName()).thenReturn(kit);
        when(person.hasRepairKit(org.mockito.ArgumentMatchers.anyString()))
              .thenAnswer(invocation -> invocation.getArgument(0).equals(kit));
        return person;
    }

    // region kit-name regression guards
    // The kit constants are compared as strings against MegaMek's equipment tables. If a kit is renamed in MegaMek the
    // lookup returns null and the kit silently drops out of the catalog — these tests turn that into a loud failure.
    @Test
    void everyIssuableKitNameStillResolvesToARealKit() {
        for (String name : RepairKitCatalog.allKitNames()) {
            assertNotNull(EquipmentType.get(name),
                  "Kit name '" + name + "' no longer resolves — it was renamed or removed in MegaMek");
        }
    }

    @Test
    void theGeneralGearTheLogicBranchesOnResolves() {
        for (String name : List.of(KIT_BASIC_TOOLKIT,
              KIT_DELUXE_TOOLKIT,
              KIT_DESCARTES_MK_XXI,
              KIT_DESCARTES_MK_XXV,
              KIT_FIELD_SURGICAL)) {
            assertNotNull(EquipmentType.get(name), "Kit name '" + name + "' no longer resolves");
        }
    }
    // endregion kit-name regression guards

    // region professionsFor
    @Test
    void professionsForMapsEachTechnicianRoleToItsProfession() {
        assertEquals(Set.of(KitProfession.MEK_TECH), RepairKitCatalog.professionsFor(personWithRoles(MEK_TECH, NONE)));
        assertEquals(Set.of(KitProfession.MECHANIC), RepairKitCatalog.professionsFor(personWithRoles(MECHANIC, NONE)));
        assertEquals(Set.of(KitProfession.AERO_TEK), RepairKitCatalog.professionsFor(personWithRoles(AERO_TEK, NONE)));
        assertEquals(Set.of(KitProfession.BA_TECH), RepairKitCatalog.professionsFor(personWithRoles(BA_TECH, NONE)));
    }

    @Test
    void professionsForNowIncludesDoctorsAndAdministrators() {
        assertEquals(Set.of(KitProfession.DOCTOR), RepairKitCatalog.professionsFor(personWithRoles(DOCTOR, NONE)));
        assertEquals(Set.of(KitProfession.ADMIN),
              RepairKitCatalog.professionsFor(personWithRoles(ADMINISTRATOR, NONE)));
    }

    @Test
    void professionsForReadsBothPrimaryAndSecondaryRoles() {
        assertEquals(Set.of(KitProfession.MEK_TECH, KitProfession.DOCTOR),
              RepairKitCatalog.professionsFor(personWithRoles(MEK_TECH, DOCTOR)));
    }

    @Test
    void professionsForIsEmptyForNonKitRolesAndNull() {
        assertTrue(RepairKitCatalog.professionsFor(personWithRoles(MEKWARRIOR, NONE)).isEmpty());
        assertTrue(RepairKitCatalog.professionsFor(null).isEmpty());
    }
    // endregion professionsFor

    // region repairBonus
    @Test
    void repairBonusGivesTheSpecialistBonusForACoveredSkill() {
        Person tech = personWithKit(KIT_WEAPON);
        assertEquals(REPAIR_KIT_ROLL_BONUS, RepairKitCatalog.repairBonus(tech, S_TECH_WEAPONS));
    }

    @Test
    void repairBonusIsZeroForASkillTheOwnedKitDoesNotCover() {
        Person tech = personWithKit(KIT_WEAPON);
        assertEquals(0, RepairKitCatalog.repairBonus(tech, S_TECH_ELECTRONIC));
    }

    @Test
    void deluxeToolkitGivesTheGeneralBonusToAnyRepairSkill() {
        Person tech = personWithKit(KIT_DELUXE_TOOLKIT);
        assertEquals(DELUXE_TOOLKIT_ROLL_BONUS, RepairKitCatalog.repairBonus(tech, S_TECH_ELECTRONIC));
    }

    @Test
    void basicToolkitGrantsNoRepairBonus() {
        Person tech = personWithKit(KIT_BASIC_TOOLKIT);
        assertEquals(0, RepairKitCatalog.repairBonus(tech, S_TECH_WEAPONS));
    }

    @Test
    void repairBonusIsZeroForNullPerson() {
        assertEquals(0, RepairKitCatalog.repairBonus(null, S_TECH_WEAPONS));
    }
    // endregion repairBonus

    // region maintenanceBonus
    @Test
    void descartesScannersGiveGradedMaintenanceBonuses() {
        assertEquals(3, RepairKitCatalog.maintenanceBonus(personWithKit(KIT_DESCARTES_MK_XXV)));
        assertEquals(2, RepairKitCatalog.maintenanceBonus(personWithKit(KIT_DESCARTES_MK_XXI)));
    }

    @Test
    void aDeluxeToolkitHelpsMaintenance() {
        assertEquals(DELUXE_TOOLKIT_ROLL_BONUS, RepairKitCatalog.maintenanceBonus(personWithKit(KIT_DELUXE_TOOLKIT)));
    }

    @Test
    void maintenanceBonusIsZeroWithoutRelevantGearOrForNull() {
        assertEquals(0, RepairKitCatalog.maintenanceBonus(personWithKit(KIT_WEAPON)));
        assertEquals(0, RepairKitCatalog.maintenanceBonus(null));
    }
    // endregion maintenanceBonus

    // region generalSkillBonus
    @Test
    void generalSkillBonusReadsTheNonTechnicianSkillTables() {
        assertEquals(2, RepairKitCatalog.generalSkillBonus(personWithKit(KIT_ADVANCED_FIELD), S_SURVIVAL));
        assertEquals(1, RepairKitCatalog.generalSkillBonus(personWithKit(KIT_BASIC_FIELD), S_SURVIVAL));
        assertEquals(2, RepairKitCatalog.generalSkillBonus(personWithKit(KIT_PERSONAL_COMPUTER), S_ADMIN));
        assertEquals(2, RepairKitCatalog.generalSkillBonus(personWithKit(KIT_POCKET_TRANSCRIBER), S_NEGOTIATION));
    }

    @Test
    void medicalKitsRaiseBothMedTechAndSurgery() {
        assertEquals(2, RepairKitCatalog.generalSkillBonus(personWithKit(KIT_FIELD_SURGICAL), S_MEDTECH));
        assertEquals(2, RepairKitCatalog.generalSkillBonus(personWithKit(KIT_FIELD_SURGICAL), S_SURGERY));
        // The plain Medical Kit still grants the customized +1 surgery bonus even though the book lists none.
        assertEquals(1, RepairKitCatalog.generalSkillBonus(personWithKit(KIT_MEDICAL), S_SURGERY));
        assertEquals(1, RepairKitCatalog.generalSkillBonus(personWithKit(KIT_ADVANCED_MEDICAL), S_SURGERY));
    }

    @Test
    void generalSkillBonusIsZeroForAnUncoveredSkillOrNull() {
        assertEquals(0, RepairKitCatalog.generalSkillBonus(personWithKit(KIT_COMPAD), S_SURVIVAL));
        assertEquals(0, RepairKitCatalog.generalSkillBonus(null, S_ADMIN));
        assertEquals(0, RepairKitCatalog.generalSkillBonus(personWithKit(KIT_COMPAD), null));
    }

    @Test
    void noteputerHelpsBothAdministrationAndNegotiation() {
        Person clerk = personWithKit(KIT_NOTEPUTER);
        assertEquals(1, RepairKitCatalog.generalSkillBonus(clerk, S_ADMIN));
        assertEquals(1, RepairKitCatalog.generalSkillBonus(clerk, S_NEGOTIATION));
    }

    @Test
    void compassKitsAssistNavigation() {
        assertEquals(1, RepairKitCatalog.generalSkillBonus(personWithKit(RepairKitCatalog.KIT_COMPASS), S_NAVIGATION));
        assertEquals(2,
              RepairKitCatalog.generalSkillBonus(personWithKit(RepairKitCatalog.KIT_ELECTRONIC_COMPASS),
                    S_NAVIGATION));
    }
    // endregion generalSkillBonus

    // region generalSkillBonuses (map)
    @Test
    void generalSkillBonusesCollectsEverySkillTheOwnedKitImproves() {
        Map<String, Integer> bonuses = RepairKitCatalog.generalSkillBonuses(personWithKit(KIT_FIELD_SURGICAL));
        assertEquals(2, bonuses.get(S_MEDTECH));
        assertEquals(2, bonuses.get(S_SURGERY));
        assertEquals(2, bonuses.size());
    }

    @Test
    void aKitCoveringTwoSkillsMapsBoth() {
        Map<String, Integer> bonuses = RepairKitCatalog.generalSkillBonuses(personWithKit(KIT_NOTEPUTER));
        assertEquals(1, bonuses.get(S_ADMIN));
        assertEquals(1, bonuses.get(S_NEGOTIATION));
    }

    @Test
    void generalSkillBonusesIsEmptyWithNoRelevantKitOrForNull() {
        assertTrue(RepairKitCatalog.generalSkillBonuses(personWithKit(KIT_WEAPON)).isEmpty());
        assertTrue(RepairKitCatalog.generalSkillBonuses(null).isEmpty());
    }
    // endregion generalSkillBonuses

    // region hasToolKit
    @Test
    void hasToolKitIsTrueForBasicOrDeluxeToolkits() {
        assertTrue(RepairKitCatalog.hasToolKit(personWithKit(KIT_BASIC_TOOLKIT)));
        assertTrue(RepairKitCatalog.hasToolKit(personWithKit(KIT_DELUXE_TOOLKIT)));
    }

    @Test
    void hasToolKitIsFalseForSpecialistKitsAloneOrNoKitsOrNull() {
        // A specialist repair kit augments a tool kit but does not replace one.
        assertFalse(RepairKitCatalog.hasToolKit(personWithKit(KIT_WEAPON)));
        assertFalse(RepairKitCatalog.hasToolKit(personWithKit(null)));
        assertFalse(RepairKitCatalog.hasToolKit(null));
    }
    // endregion hasToolKit

    // region canBeIssuedKit
    @Test
    void anyCharacterMayBeIssuedAKitButNullMayNot() {
        assertTrue(RepairKitCatalog.canBeIssuedKit(mock(Person.class)));
        assertFalse(RepairKitCatalog.canBeIssuedKit(null));
    }
    // endregion canBeIssuedKit

    // region skill/kit lookups
    @Test
    void skillsBoostedByAndKitsBoostingSkillAreConsistent() {
        assertTrue(RepairKitCatalog.skillsBoostedBy(KIT_WEAPON).contains(S_TECH_WEAPONS));
        assertTrue(RepairKitCatalog.kitsBoostingSkill(S_TECH_WEAPONS).contains(KIT_WEAPON));
        assertTrue(RepairKitCatalog.isKitBoostedSkill(S_TECH_MECHANICAL));
        assertFalse(RepairKitCatalog.isKitBoostedSkill(S_ADMIN));
        assertTrue(RepairKitCatalog.skillsBoostedBy("Not A Kit").isEmpty());
    }

    @Test
    void ownsKitBoostingSkillTracksOwnership() {
        assertTrue(RepairKitCatalog.ownsKitBoostingSkill(personWithKit(KIT_ELECTRONICS), S_TECH_ELECTRONIC));
        assertFalse(RepairKitCatalog.ownsKitBoostingSkill(personWithKit(KIT_WEAPON), S_TECH_ELECTRONIC));
        assertFalse(RepairKitCatalog.ownsKitBoostingSkill(null, S_TECH_ELECTRONIC));
    }
    // endregion skill/kit lookups

    // region optionKitNames
    @Test
    void optionKitNamesLeadWithTheNoneSentinelThenEveryIssuableKit() {
        List<String> options = RepairKitCatalog.optionKitNames();
        assertEquals(NO_DEFAULT_KIT, options.get(0));
        assertTrue(options.containsAll(RepairKitCatalog.allKitNames()));
        assertEquals(RepairKitCatalog.allKitNames().size() + 1, options.size());
    }
    // endregion optionKitNames
}
