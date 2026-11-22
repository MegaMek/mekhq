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

import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests rank awards: rank groups for exclusive awards, and that held awards are not offered again.
 */
class RankAwardsTest {
    private final UUID personId = UUID.randomUUID();
    private final Person person = mock(Person.class, RETURNS_DEEP_STUBS);
    private final Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);

    private static Award rankAward(int rankNumeric, String range) {
        return new Award("Rank Award", "test", "", "", List.of(), List.of(), List.of(), 0, 0, false, rankNumeric,
              "Rank", null, range, 0);
    }

    private int countAwards(Award award, int personRankNumeric) {
        when(campaign.getPlayerForce().getHumanResources().getPerson(personId)).thenReturn(person);
        when(person.getRankNumeric()).thenReturn(personRankNumeric);

        return RankAwards.RankAwardsProcessor(campaign, personId, List.of(award)).size();
    }

    @ParameterizedTest
    @CsvSource({ "0, 20, true", "21, 30, true", "31, 40, true", "20, 21, false", "30, 31, false", "5, 35, false" })
    void testRankGroups(int firstRankNumeric, int secondRankNumeric, boolean expected) {
        assertEquals(expected, RankAwards.isSameRankGroup(firstRankNumeric, secondRankNumeric));
    }

    @ParameterizedTest
    @CsvSource({ "8, 1", "4, 0", "25, 0", "35, 0" })
    void testExclusiveEnlistedAward(int personRankNumeric, int expectedAwards) {
        // An enlisted award at rank 5 goes to enlisted ranks from 5 to 20, never to warrant officers or officers
        assertEquals(expectedAwards, countAwards(rankAward(5, "Exclusive"), personRankNumeric));
    }

    @Test
    void testInclusiveAwardIncludesHigherGroups() {
        assertEquals(1, countAwards(rankAward(5, "Inclusive"), 35));
    }

    @Test
    void testHeldAwardIsNotOfferedAgain() {
        Award award = rankAward(5, "Inclusive");
        when(person.getAwardController().hasAward(award)).thenReturn(true);

        assertEquals(0, countAwards(award, 10));
    }
}
