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

import megamek.client.ratgenerator.CrewDescriptor;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers issue 10082: the Force Generator engine names a crew for the gender it rolled, while MekHQ rolls a second
 * gender for the same seat. A Person that takes the descriptor's name must take its gender too, or half of all unit
 * commanders end up as a female "Bill Schindler" or a male "Teresa Corbucca".
 */
class CrewDescriptorAdapterGenderTest {
    private static Campaign campaign;

    @BeforeAll
    static void loadSkillsAndCampaign() {
        SkillType.initializeTypes();
        campaign = MHQTestUtilities.getTestCampaign();
    }

    @Test
    void aFemalePersonTakingAMaleNameBecomesMale() {
        Person person = personOfGender(Gender.FEMALE);
        CrewDescriptor descriptor = descriptorNamed("Bill Schindler", Gender.MALE);

        CrewDescriptorAdapter.apply(descriptor, person, true);

        assertEquals(Gender.MALE, person.getGender());
        assertEquals("Bill", person.getGivenName());
        assertEquals("Schindler", person.getSurname());
    }

    @Test
    void aMalePersonTakingAFemaleNameBecomesFemale() {
        Person person = personOfGender(Gender.MALE);
        CrewDescriptor descriptor = descriptorNamed("Teresa Corbucca", Gender.FEMALE);

        CrewDescriptorAdapter.apply(descriptor, person, true);

        assertEquals(Gender.FEMALE, person.getGender());
        assertEquals("Teresa", person.getGivenName());
    }

    @Test
    void aMatchingGenderIsLeftAlone() {
        Person person = personOfGender(Gender.FEMALE);
        CrewDescriptor descriptor = descriptorNamed("Teresa Corbucca", Gender.FEMALE);

        CrewDescriptorAdapter.apply(descriptor, person, true);

        assertEquals(Gender.FEMALE, person.getGender());
    }

    @Test
    void aNonBinaryPersonKeepsThatFlavourButFollowsTheName() {
        Person person = personOfGender(Gender.OTHER_MALE);
        CrewDescriptor descriptor = descriptorNamed("Teresa Corbucca", Gender.FEMALE);

        CrewDescriptorAdapter.apply(descriptor, person, true);

        assertEquals(Gender.OTHER_FEMALE, person.getGender());
    }

    @Test
    void aDescriptorWithoutAGenderLeavesThePersonsGenderAlone() {
        Person person = personOfGender(Gender.FEMALE);
        CrewDescriptor descriptor = descriptorNamed("Bill Schindler", null);

        CrewDescriptorAdapter.apply(descriptor, person, true);

        assertEquals(Gender.FEMALE, person.getGender());
        assertEquals("Bill", person.getGivenName());
    }

    @Test
    void aPersonKeepingItsOwnNameKeepsItsOwnGender() {
        Person person = personOfGender(Gender.FEMALE);
        person.setGivenName("Teresa");
        CrewDescriptor descriptor = descriptorNamed("Bill Schindler", Gender.MALE);

        CrewDescriptorAdapter.apply(descriptor, person, false);

        assertEquals(Gender.FEMALE, person.getGender());
        assertEquals("Teresa", person.getGivenName());
    }

    private static Person personOfGender(Gender gender) {
        Person person = new Person(campaign);
        person.setGender(gender);
        return person;
    }

    private static CrewDescriptor descriptorNamed(String name, Gender gender) {
        CrewDescriptor descriptor = CrewDescriptorMocks.crew(name, 4, 5);
        descriptor.setGender(gender);
        return descriptor;
    }
}
