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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import testUtilities.MHQTestUtilities;

/**
 * Covers the build's roster-wide flags: they reach everyone the build hired, and only them.
 */
class CommandGeneratorRosterFlagsTest {

    @BeforeAll
    static void loadTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @Test
    void personsAddedSince_findsOnlyThePeopleHiredAfterTheSnapshot() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Person veteran = person(campaign, PersonnelRole.MEKWARRIOR);
        Set<UUID> before = CommandGenerator.snapshotPersonnelIds(campaign);
        Person tech = person(campaign, PersonnelRole.MEK_TECH);
        Person driver = person(campaign, PersonnelRole.VEHICLE_CREW_GROUND);

        List<Person> hired = CommandGenerator.personsAddedSince(campaign, before);

        assertEquals(2, hired.size());
        assertTrue(hired.contains(tech));
        assertTrue(hired.contains(driver));
        assertFalse(hired.contains(veteran), "someone already on the books was not hired by this build");
    }

    @Test
    void founderFlagReachesSupportStaffAndCrewsAlike() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Person tech = person(campaign, PersonnelRole.MEK_TECH);
        Person driver = person(campaign, PersonnelRole.VEHICLE_CREW_GROUND);
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setAssignFounderFlag(true);
        options.setAssignMekWarriorsCallSigns(false);

        CommandGenerator.applyRosterFlags(campaign, options, List.of(tech, driver));

        assertTrue(tech.isFounder(), "support staff are founders of the command too");
        assertTrue(driver.isFounder(), "support-vehicle crews are founders of the command too");
    }

    @Test
    void callsignsSkipClanWarriorsWhateverTheCampaign() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Person clanWarrior = person(campaign, PersonnelRole.MEKWARRIOR);
        clanWarrior.setClanPersonnel(true);
        String clanCallsign = clanWarrior.getCallsign();
        Person innerSphereWarrior = person(campaign, PersonnelRole.MEKWARRIOR);
        innerSphereWarrior.setClanPersonnel(false);
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setAssignFounderFlag(false);
        options.setAssignMekWarriorsCallSigns(true);

        RandomCallsignGenerator generator = mock(RandomCallsignGenerator.class);
        when(generator.generate()).thenReturn("Hammer");
        try (MockedStatic<RandomCallsignGenerator> callsigns = mockStatic(RandomCallsignGenerator.class)) {
            callsigns.when(RandomCallsignGenerator::getInstance).thenReturn(generator);
            CommandGenerator.applyRosterFlags(campaign, options, List.of(clanWarrior, innerSphereWarrior));
        }

        assertEquals("Hammer", innerSphereWarrior.getCallsign());
        assertEquals(clanCallsign, clanWarrior.getCallsign(), "a Clan warrior carries a bloodname, not a callsign");
    }

    private static Person person(Campaign campaign, PersonnelRole role) {
        Person person = new Person(campaign);
        person.setPrimaryRole(campaign.getLocalDate(), role);
        campaign.importPerson(person);
        return person;
    }
}
