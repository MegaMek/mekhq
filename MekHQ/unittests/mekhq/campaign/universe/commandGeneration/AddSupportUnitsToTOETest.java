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
package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Verifies that support-vehicle placement reuses an existing sub-formation of the same type instead of
 * creating a duplicate, so a top-up generation lands in the existing formation.
 */
class AddSupportUnitsToTOETest {

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @Test
    void reusesExistingSubFormationOfSameType() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Unit first = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1V());
        Unit second = UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getLocustLCT1E());

        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, List.of(first),
              SupportTOEFormationTypes.COMMISSARY_FORMATION);
        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, List.of(second),
              SupportTOEFormationTypes.COMMISSARY_FORMATION);

        String label = SupportTOEFormationTypes.COMMISSARY_FORMATION.getLabel();
        long matching = campaign.getPlayerForce().getAllFormations().stream()
              .filter(formation -> formation.getName().equalsIgnoreCase(label))
              .count();
        assertEquals(1, matching, "a second support batch of the same type must reuse the existing formation");
    }

    @Test
    void organize_collapsesASectionThatHoldsOneCompany() {
        // Twelve administrators pack into two squads, which become one Administrator company under the Command
        // section. That section then groups nothing, so it should hold the carriers itself rather than adding a layer
        // - every extra layer pushes the whole TOE up an echelon.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        List<Person> administrators = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            administrators.add(campaign.getPlayerForce().getHumanResources()
                                    .newPerson(campaign, PersonnelRole.ADMINISTRATOR, PersonnelRole.NONE));
        }

        SupportPersonnelToTOE.organize(campaign, administrators, false);

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        assertNotNull(supportCommand, "organize must record the Support Command formation");

        // Command is the only section, so it collapses into Support Command too, leaving the carriers one step down.
        assertTrue(supportCommand.getSubFormations().isEmpty(),
              "a Support Command holding a single section must not keep the section layer");
        assertEquals(2, supportCommand.getUnits().size(),
              "both administrator carriers should hang directly off Support Command");

        // Two squads is a platoon's worth, so that is what it says. Depth-based levels called this a Battalion.
        assertEquals(FormationLevel.LANCE, supportCommand.getFormationLevel(),
              "a support command holding two squads must be sized as a platoon, not by how deep the tree is");
    }

    @Test
    void organize_sizesEachSupportFormationByWhatItHolds() {
        // 30 technicians pack into a full 28-person platoon plus a 2-person squad: 4 + 1 = 5 squads, which is more
        // than one platoon and so reads as a company.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        List<Person> technicians = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            technicians.add(campaign.getPlayerForce().getHumanResources()
                                  .newPerson(campaign, PersonnelRole.MEK_TECH, PersonnelRole.NONE));
        }

        SupportPersonnelToTOE.organize(campaign, technicians, false);

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        assertNotNull(supportCommand);
        assertEquals(FormationLevel.COMPANY, supportCommand.getFormationLevel(),
              "a platoon plus a squad is five squads, which is a company's worth");
    }

    @Test
    void organize_keepsTheLayersThatSeparateProfessions() {
        // Two professions means the companies actually tell them apart, so nothing collapses.
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        List<Person> staff = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            staff.add(campaign.getPlayerForce().getHumanResources()
                              .newPerson(campaign, PersonnelRole.MEK_TECH, PersonnelRole.NONE));
        }
        for (int index = 0; index < 12; index++) {
            staff.add(campaign.getPlayerForce().getHumanResources()
                              .newPerson(campaign, PersonnelRole.MECHANIC, PersonnelRole.NONE));
        }

        SupportPersonnelToTOE.organize(campaign, staff, false);

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        assertNotNull(supportCommand);
        // Maintenance is still the only section, so it collapses into Support Command, but the two profession
        // companies below it survive: they are what separates the technicians from the mechanics.
        assertEquals(2, supportCommand.getSubFormations().size(),
              "the two profession companies must survive, one per profession");
    }
}
