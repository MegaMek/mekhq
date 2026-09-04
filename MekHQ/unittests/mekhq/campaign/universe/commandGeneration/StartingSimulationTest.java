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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers Run Starting Simulation: how far back it walks, that the campaign's own date stays put, and that
 * whatever the run left the people with is healed.
 */
class StartingSimulationTest {

    @BeforeAll
    static void loadSkills() {
        SkillType.initializeTypes();
    }

    @Test
    void walksBackTheYearsAskedForWeekByWeekAndLeavesTheDateAlone() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        LocalDate today = campaign.getLocalDate();
        Person person = mekWarrior(campaign);
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setRunStartingSimulation(true);
        options.setSimulationDuration(2);
        options.setSimulateRandomMarriages(true);
        options.setSimulateRandomProcreation(true);

        StartingSimulation.Result result = StartingSimulation.run(campaign, options, List.of(person), null);

        assertTrue((result.weeksSimulated() >= 104) && (result.weeksSimulated() <= 105),
              "two years is 104 or 105 weeks, was " + result.weeksSimulated());
        assertEquals(today, campaign.getLocalDate(), "the campaign's own date does not move");
    }

    @Test
    void healsWhatTheRunLeftBehind() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Person person = mekWarrior(campaign);
        person.setHits(2);
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setRunStartingSimulation(true);
        options.setSimulationDuration(1);
        options.setSimulateRandomProcreation(true);

        StartingSimulation.Result result = StartingSimulation.run(campaign, options, List.of(person), null);

        assertEquals(1, result.peopleHealed());
        assertFalse(person.needsFixing());
    }

    @Test
    void doesNothingWhenOffOrWhenNothingIsAskedToHappen() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Person person = mekWarrior(campaign);
        CommandGenerationOptions off = new CommandGenerationOptions();
        CommandGenerationOptions nothingToDo = new CommandGenerationOptions();
        nothingToDo.setRunStartingSimulation(true);
        nothingToDo.setSimulationDuration(12);

        assertEquals(StartingSimulation.Result.none(), StartingSimulation.run(campaign, off, List.of(person), null));
        assertEquals(StartingSimulation.Result.none(),
              StartingSimulation.run(campaign, nothingToDo, List.of(person), null),
              "neither marriages nor births were asked for");
    }

    private static Person mekWarrior(Campaign campaign) {
        Person person = new Person(campaign);
        person.setPrimaryRole(campaign.getLocalDate(), PersonnelRole.MEKWARRIOR);
        person.addSkill(SkillType.S_GUN_MEK, 4, 0);
        person.addSkill(SkillType.S_PILOT_MEK, 5, 0);
        campaign.importPerson(person);
        return person;
    }
}
