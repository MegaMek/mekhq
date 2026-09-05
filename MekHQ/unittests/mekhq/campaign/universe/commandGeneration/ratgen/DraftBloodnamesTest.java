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

import java.util.List;

import megamek.client.ratgenerator.CrewDescriptor;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers who gets a Bloodname in the draft and how it travels to the person built from the crew.
 */
class DraftBloodnamesTest {

    private static Campaign campaign;

    @BeforeAll
    static void loadSkillsAndCampaign() {
        SkillType.initializeTypes();
        campaign = MHQTestUtilities.getTestCampaign();
    }

    @Test
    void theBestCrewsAreNamedAndTheNameShowsInTheDraft() {
        CrewDescriptor elite = crew("Aidan", 2, 3);
        CrewDescriptor regular = crew("Marthe", 4, 5);
        CrewDescriptor green = crew("Diana", 5, 6);

        int awarded = DraftBloodnames.awardTo(List.of(green, regular, elite), 1, crew -> "Pryde");

        assertEquals(1, awarded);
        assertEquals("Pryde", elite.getBloodname(), "the best crew carries the Bloodname");
        assertEquals("Aidan Pryde", elite.getName(), "the draft shows it as part of the name");
        assertFalse(DraftBloodnames.hasBloodname(regular));
        assertFalse(DraftBloodnames.hasBloodname(green));
    }

    @Test
    void aCrewNoBloodnameCanBeFoundForIsPassedOver() {
        CrewDescriptor elite = crew("Aidan", 2, 3);
        CrewDescriptor regular = crew("Marthe", 4, 5);

        int awarded = DraftBloodnames.awardTo(List.of(elite, regular), 1,
              crew -> crew == elite ? null : "Ward");

        assertEquals(1, awarded);
        assertEquals("Ward", regular.getBloodname(), "the next crew takes the Bloodname the best could not");
    }

    @Test
    void theBuiltPersonCarriesTheBloodnameOnceNotTwice() {
        CrewDescriptor crew = crew("Aidan", 2, 3);
        DraftBloodnames.awardTo(List.of(crew), 1, descriptor -> "Pryde");
        Person person = new Person(campaign);

        CrewDescriptorAdapter.apply(crew, person, true);

        assertEquals("Pryde", person.getBloodname());
        assertEquals("Aidan", person.getGivenName());
        assertTrue(person.getSurname().isBlank(), "the Bloodname is not doubled into the surname");
    }

    private static CrewDescriptor crew(String name, int gunnery, int piloting) {
        return CrewDescriptorMocks.crew(name, gunnery, piloting);
    }
}
