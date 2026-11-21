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
package mekhq.campaign.personnel.autoAwards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests kill awards: lifetime kill awards, and picking the best award when only the best award is issued.
 */
class KillAwardsTest {
    private static Award killAward(String name, int kills, String size, String range) {
        return new Award(name, "test", "", "", List.of(), List.of(), List.of(), 0, 0, false, kills, "Kill", size,
              range, 0);
    }

    @ParameterizedTest
    @CsvSource({ "4, 0", "5, 1", "9, 1" })
    void testLifetimeKillAwardIsIssuedOnceEnoughKillsAreScored(int kills, int expectedAwards) {
        UUID personId = UUID.randomUUID();
        Person person = mock(Person.class, RETURNS_DEEP_STUBS);

        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        when(campaign.getPlayerForce().getHumanResources().getPerson(personId)).thenReturn(person);
        when(campaign.getCampaignOptions()).thenReturn(new CampaignOptions());
        when(campaign.getKillsFor(personId)).thenReturn(Collections.nCopies(kills, mock(Kill.class)));

        Award award = killAward("Ace", 5, "Individual", "Lifetime");

        // Lifetime awards are checked monthly, with no mission
        Map<Integer, List<Object>> awardData = KillAwards.KillAwardProcessor(campaign, null, personId,
              List.of(award), Map.of());

        assertEquals(expectedAwards, awardData.size());
    }

    @Test
    void testBestAwardIsTheHighestWhateverTheOrder() {
        Award low = killAward("Low", 24, "Lance", "Mission");
        Award high = killAward("High", 48, "Lance", "Mission");
        Award middle = killAward("Middle", 36, "Lance", "Mission");

        assertEquals(high, KillAwards.getBestAward(List.of(low, high, middle)));
        assertEquals(high, KillAwards.getBestAward(List.of(high, middle, low)));
    }

    @Test
    void testBestAwardKeepsTheFirstOfEqualAwards() {
        Award first = killAward("First", 10, "Individual", "Mission");
        Award second = killAward("Second", 10, "Individual", "Mission");

        assertEquals(first, KillAwards.getBestAward(List.of(first, second)));
    }
}
