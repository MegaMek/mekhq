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

import java.util.ArrayList;
import java.util.List;

import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers the order the Officer Selection options rank people in.
 */
class OfficerSelectorTest {

    private static Campaign campaign;

    @BeforeAll
    static void loadSkillsAndCampaign() {
        SkillType.initializeTypes();
        campaign = MHQTestUtilities.getTestCampaign();
    }

    @Test
    void tacticalGeniusOutranksSkillEitherWay() {
        Person crackShot = mekWarrior(6, 6);
        Person genius = mekWarrior(2, 2);
        genius.getOptions().getOption(OptionsConstants.MISC_TACTICAL_GENIUS).setValue(true);

        assertSame(genius, best(List.of(crackShot, genius), OfficerSelector.bestFirst(campaign, true)));
        assertSame(genius, best(List.of(crackShot, genius), OfficerSelector.bestFirst(campaign, false)));
    }

    @Test
    void commandSkillsDecideUnlessCombatIsAskedFor() {
        Person crackShot = mekWarrior(6, 6);
        Person leader = mekWarrior(2, 2);
        leader.addSkill(SkillType.S_LEADER, 3, 0);
        leader.addSkill(SkillType.S_TACTICS, 2, 0);

        assertSame(leader, best(List.of(crackShot, leader), OfficerSelector.bestFirst(campaign, false)),
              "command skills first: the leader wins");
        assertSame(crackShot, best(List.of(crackShot, leader), OfficerSelector.bestFirst(campaign, true)),
              "combat first: the crack shot wins");
    }

    @Test
    void theMostSkilledPilotComesFirstForALanceSeat() {
        Person green = mekWarrior(1, 1);
        Person veteran = mekWarrior(5, 5);

        assertSame(veteran, best(List.of(green, veteran), OfficerSelector.mostSkilledFirst(campaign)));
    }

    @Test
    void commandSkillTotalCountsOnlyTheSkillsThePersonHas() {
        Person nobody = mekWarrior(3, 3);
        Person person = mekWarrior(3, 3);
        person.addSkill(SkillType.S_LEADER, 2, 0);
        person.addSkill(SkillType.S_STRATEGY, 1, 0);
        java.time.LocalDate today = campaign.getLocalDate();
        int expected = person.getSkillLevel(SkillType.S_LEADER, false, false, today)
              + person.getSkillLevel(SkillType.S_STRATEGY, false, false, today);

        assertEquals(0, OfficerSelector.commandSkillTotal(nobody, false, false, today));
        assertEquals(expected, OfficerSelector.commandSkillTotal(person, false, false, today),
              "the total is of the skills' experience levels, the way the old generator summed them");
    }

    private static Person best(List<Person> people, java.util.Comparator<Person> bestFirst) {
        List<Person> sorted = new ArrayList<>(people);
        sorted.sort(bestFirst);
        return sorted.getFirst();
    }

    /** A MekWarrior with the given gunnery and piloting skill levels; higher is better. */
    private static Person mekWarrior(int gunneryLevel, int pilotingLevel) {
        Person person = new Person(campaign);
        person.setPrimaryRole(campaign.getLocalDate(), PersonnelRole.MEKWARRIOR);
        person.addSkill(SkillType.S_GUN_MEK, gunneryLevel, 0);
        person.addSkill(SkillType.S_PILOT_MEK, pilotingLevel, 0);
        return person;
    }
}
