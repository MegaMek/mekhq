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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.FormationLevel;
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
 * Covers Generate Captains and Apply Officer Stat Bonus to Weakest Skill: what each officer gains.
 */
class OfficerSkillBoosterTest {

    private static Campaign campaign;

    @BeforeAll
    static void loadTypesAndCampaign() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
        campaign = MHQTestUtilities.getTestCampaign();
    }

    @Test
    void theCommandingOfficerGainsBothCombatSkillsAndTwoCommandIncreases() {
        Person commandingOfficer = seatedMekWarrior(4, 4);
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setGenerateCaptains(true);
        RulesetRankAssigner.Result ranks = ranks(commandingOfficer, Map.of(commandingOfficer, FormationLevel.COMPANY));

        int improved = OfficerSkillBooster.apply(options, ranks, bound -> 0);

        assertEquals(1, improved);
        assertEquals(5, commandingOfficer.getSkill(SkillType.S_GUN_MEK).getLevel());
        assertEquals(5, commandingOfficer.getSkill(SkillType.S_PILOT_MEK).getLevel());
        assertEquals(2, commandingOfficer.getSkill(SkillType.S_LEADER).getLevel(),
              "two increases in Leadership, the first opening the skill at level one");
    }

    @Test
    void aLanceLeaderGainsTheirBetterCombatSkillAndOneCommandIncrease() {
        Person commandingOfficer = seatedMekWarrior(4, 4);
        Person lanceLeader = seatedMekWarrior(5, 3);
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setGenerateCaptains(true);
        Map<Person, FormationLevel> officers = new LinkedHashMap<>();
        officers.put(commandingOfficer, FormationLevel.COMPANY);
        officers.put(lanceLeader, FormationLevel.LANCE);

        OfficerSkillBooster.apply(options, ranks(commandingOfficer, officers), bound -> 2);

        assertEquals(6, lanceLeader.getSkill(SkillType.S_GUN_MEK).getLevel(), "gunnery was the better skill");
        assertEquals(3, lanceLeader.getSkill(SkillType.S_PILOT_MEK).getLevel());
        assertEquals(1, lanceLeader.getSkill(SkillType.S_TACTICS).getLevel(), "one increase, opening Tactics");
        assertNull(lanceLeader.getSkill(SkillType.S_LEADER));
    }

    @Test
    void theBonusGoesToTheWeakestSkillWhenAskedAndACaptainGetsTwoIncreases() {
        Person commandingOfficer = seatedMekWarrior(4, 4);
        Person captain = seatedMekWarrior(5, 3);
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setGenerateCaptains(true);
        options.setApplyOfficerStatBonusToWorstSkill(true);
        Map<Person, FormationLevel> officers = new LinkedHashMap<>();
        officers.put(commandingOfficer, FormationLevel.BATTALION);
        officers.put(captain, FormationLevel.COMPANY);

        OfficerSkillBooster.apply(options, ranks(commandingOfficer, officers), bound -> 1);

        assertEquals(5, captain.getSkill(SkillType.S_GUN_MEK).getLevel());
        assertEquals(4, captain.getSkill(SkillType.S_PILOT_MEK).getLevel(), "piloting was the weaker skill");
        assertEquals(2, captain.getSkill(SkillType.S_STRATEGY).getLevel(), "a captain gets two increases");
    }

    @Test
    void nothingChangesWhenGenerateCaptainsIsOff() {
        Person commandingOfficer = seatedMekWarrior(4, 4);
        RulesetRankAssigner.Result ranks = ranks(commandingOfficer, Map.of(commandingOfficer, FormationLevel.COMPANY));

        int improved = OfficerSkillBooster.apply(new CommandGenerationOptions(), ranks, bound -> 0);

        assertEquals(0, improved);
        assertEquals(4, commandingOfficer.getSkill(SkillType.S_GUN_MEK).getLevel());
        assertFalse(commandingOfficer.hasSkill(SkillType.S_LEADER));
    }

    @Test
    void captainsAndAboveAreToldApartFromLanceLeaders() {
        assertTrue(OfficerSkillBooster.isCaptainOrAbove(FormationLevel.COMPANY));
        assertTrue(OfficerSkillBooster.isCaptainOrAbove(FormationLevel.BATTALION));
        assertFalse(OfficerSkillBooster.isCaptainOrAbove(FormationLevel.LANCE));
    }

    private static RulesetRankAssigner.Result ranks(Person rootCommander, Map<Person, FormationLevel> officers) {
        return new RulesetRankAssigner.Result(rootCommander, officers);
    }

    /** A MekWarrior in a Locust, with the given gunnery and piloting skill levels. */
    private static Person seatedMekWarrior(int gunneryLevel, int pilotingLevel) {
        Unit unit = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Person person = new Person(campaign);
        person.setPrimaryRole(campaign.getLocalDate(), PersonnelRole.MEKWARRIOR);
        person.addSkill(SkillType.S_GUN_MEK, gunneryLevel, 0);
        person.addSkill(SkillType.S_PILOT_MEK, pilotingLevel, 0);
        campaign.importPerson(person);
        unit.addPilotOrSoldier(person);
        return person;
    }
}
