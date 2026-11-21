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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests Theatre of War awards for wars that list both sides.
 */
class TheatreOfWarAwardsTest {
    @ParameterizedTest
    @CsvSource({ "true, FS, DC, 1", "true, DC, FS, 1", "true, FS, CC, 0", "true, CC, DC, 0", "false, FS, DC, 0" })
    void testTwoSidedWar(boolean isUseStratCon, String employer, String enemy, int expectedAwards) {
        UUID personId = UUID.randomUUID();

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.isUseStratCon()).thenReturn(isUseStratCon);

        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        when(campaign.getPlayerForce().getHumanResources().getPerson(personId))
              .thenReturn(mock(Person.class, RETURNS_DEEP_STUBS));
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getGameYear()).thenReturn(3039);

        AbstractContract mission = mock(AbstractContract.class);
        when(mission.getEmployerFactionCode()).thenReturn(employer);
        when(mission.getEnemyFactionCode()).thenReturn(enemy);
        when(mission.getStartDate()).thenReturn(LocalDate.of(3039, 6, 1));

        // The Davion and Steiner side against the Draconis Combine. Without StratCon, a war that lists both sides
        // is skipped, as the contract's side cannot be told reliably.
        Award award = new Award("War of 3039", "test", "", "", List.of(), List.of(), List.of(), 0, 0, false, 0,
              "Theatre of War", "3039, 3040", "FS(1),LA(1),DC(2)", 0);

        assertEquals(expectedAwards,
              TheatreOfWarAwards.TheatreOfWarAwardsProcessor(campaign, mission, personId, List.of(award)).size());
    }
}
