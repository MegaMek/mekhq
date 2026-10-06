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

import megamek.client.ratgenerator.CrewDescriptor;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers where a generated warrior's Bloodname comes from: the roll's descriptor when it carries one, and
 * otherwise nowhere until the force's single roll, so the one MekHQ rolls at creation is cleared.
 */
class CrewDescriptorAdapterBloodnameTest {

    private static Campaign campaign;

    @BeforeAll
    static void loadSkillsAndCampaign() {
        SkillType.initializeTypes();
        campaign = MHQTestUtilities.getTestCampaign();
    }

    @Test
    void aBloodnameRolledAtCreationIsClearedWhenTheDescriptorHasNone() {
        Person person = new Person(campaign);
        person.setBloodname("Kerensky");
        CrewDescriptor descriptor = descriptorNamed("Aidan", "");

        CrewDescriptorAdapter.apply(descriptor, person, true);

        assertFalse(OfficerSelector.hasBloodname(person), "the force rolls once, later; the creation roll does not count");
    }

    @Test
    void aBloodnameOnTheDescriptorIsKept() {
        Person person = new Person(campaign);
        CrewDescriptor descriptor = descriptorNamed("Aidan", "Pryde");

        CrewDescriptorAdapter.apply(descriptor, person, true);

        assertEquals("Pryde", person.getBloodname());
    }

    private static CrewDescriptor descriptorNamed(String name, String bloodname) {
        return CrewDescriptorMocks.crew(name, bloodname, 4, 5);
    }
}
