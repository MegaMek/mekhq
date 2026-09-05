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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers Assign Most Skilled to Primary Lances: the leading lance ends up with the best pilots.
 */
class PilotSkillSorterTest {

    @BeforeAll
    static void loadTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @Test
    void theBestPilotMovesToTheFirstLance() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Formation firstLance = lance(campaign, "First Lance");
        Formation secondLance = lance(campaign, "Second Lance");
        Unit firstSeat = mekIn(campaign, firstLance);
        Unit secondSeat = mekIn(campaign, firstLance);
        Unit thirdSeat = mekIn(campaign, secondLance);
        Person green = pilot(campaign, firstSeat, 1);
        Person regular = pilot(campaign, secondSeat, 3);
        Person veteran = pilot(campaign, thirdSeat, 5);
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setAssignMostSkilledToPrimaryLances(true);

        int moved = PilotSkillSorter.apply(campaign, options);

        assertEquals(2, moved, "the veteran and the green pilot swap seats");
        assertSame(veteran, firstSeat.getCommander());
        assertSame(regular, secondSeat.getCommander());
        assertSame(green, thirdSeat.getCommander());
        assertSame(firstSeat, veteran.getUnit());
        assertSame(thirdSeat, green.getUnit());
    }

    @Test
    void nothingMovesWhenTheOptionIsOff() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Formation lance = lance(campaign, "Lance");
        Unit firstSeat = mekIn(campaign, lance);
        Unit secondSeat = mekIn(campaign, lance);
        Person green = pilot(campaign, firstSeat, 1);
        pilot(campaign, secondSeat, 5);

        int moved = PilotSkillSorter.apply(campaign, new CommandGenerationOptions());

        assertEquals(0, moved);
        assertSame(green, firstSeat.getCommander());
    }

    private static Formation lance(Campaign campaign, String name) {
        Formation lance = new Formation(name);
        campaign.getPlayerForce().addFormation(lance, campaign.getPlayerForce().getFormations(), campaign);
        return lance;
    }

    private static Unit mekIn(Campaign campaign, Formation lance) {
        Unit unit = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        campaign.getPlayerForce().addUnitToFormation(unit, lance.getId(), campaign);
        return unit;
    }

    /** A MekWarrior seated in the unit, with the given gunnery and piloting skill level; higher is better. */
    private static Person pilot(Campaign campaign, Unit unit, int skillLevel) {
        Person person = new Person(campaign);
        person.setPrimaryRole(campaign.getLocalDate(), PersonnelRole.MEKWARRIOR);
        person.addSkill(SkillType.S_GUN_MEK, skillLevel, 0);
        person.addSkill(SkillType.S_PILOT_MEK, skillLevel, 0);
        campaign.importPerson(person);
        unit.addPilotOrSoldier(person);
        return person;
    }
}
