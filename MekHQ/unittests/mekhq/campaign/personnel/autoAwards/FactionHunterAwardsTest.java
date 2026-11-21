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
import java.util.Map;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

/**
 * Tests Faction Hunter awards: the ComStar category, faction lists, and that each award is judged on its own.
 */
class FactionHunterAwardsTest {
    private final UUID personId = UUID.randomUUID();
    private final Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
    private final AbstractContract mission = mock(AbstractContract.class);
    private final Faction comStar = mock(Faction.class);

    private static Award hunterAward(String name, String factions) {
        return new Award(name, "test", "", "", List.of(), List.of(), List.of(), 0, 0, false, 0, "Faction Hunter",
              null, factions, 0);
    }

    private List<Award> eligibleAwards(Award... awards) {
        when(campaign.getPlayerForce().getHumanResources().getPerson(personId))
              .thenReturn(mock(Person.class, RETURNS_DEEP_STUBS));
        when(comStar.getShortName()).thenReturn("CS");
        when(comStar.isComStar()).thenReturn(true);
        when(comStar.isComStarOrWoB()).thenReturn(true);
        when(mission.getEnemyFaction()).thenReturn(comStar);

        Map<Integer, List<Object>> awardData = FactionHunterAwards.FactionHunterAwardsProcessor(campaign, mission,
              personId, List.of(awards));

        return awardData.values().stream().map(row -> (Award) row.get(1)).toList();
    }

    @Test
    void testComStarCategory() {
        Award award = hunterAward("ComStar Hunter", "ComStar");

        assertEquals(List.of(award), eligibleAwards(award));
    }

    @Test
    void testFactionListAllowsSpacesAfterCommas() {
        Award award = hunterAward("Listed Hunter", "PIR, CS");

        assertEquals(List.of(award), eligibleAwards(award));
    }

    @Test
    void testEachAwardIsJudgedOnItsOwnFactions() {
        // Matching the first award must not make the second award eligible too
        Award matching = hunterAward("ComStar Hunter", "ComStar");
        Award other = hunterAward("Clan Hunter", "Clans");

        assertEquals(List.of(matching), eligibleAwards(matching, other));
    }
}
