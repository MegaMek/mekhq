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
package mekhq.campaign.market.personnelMarket.markets;

import static mekhq.campaign.personnel.ATOWTraits.CONNECTIONS_TARGET_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.ConnectionsLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

/**
 * Tests the Connections trait's extra recruit rolls in {@link NewPersonnelMarket#performConnectionsRecruitsCheck()}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class NewPersonnelMarketConnectionsTest {
    private static final LocalDate TODAY = LocalDate.of(3151, 1, 1);

    private Campaign campaign;
    private Person commander;
    private NewPersonnelMarket market;

    @BeforeEach
    void beforeEach() {
        campaign = mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        commander = mock(Person.class);
        when(commander.getFullTitle()).thenReturn("Commander");
        when(commander.getHyperlinkedFullTitle()).thenReturn("Commander");
        when(campaign.getPlayerForce().getHumanResources().getCommander(any(), anyBoolean(), any()))
              .thenReturn(commander);

        market = new NewPersonnelMarket();
        market.setCampaign(campaign);
    }

    private int check(int connections, int roll) {
        when(commander.getAdjustedConnections(false)).thenReturn(connections);
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(roll);
            return market.performConnectionsRecruitsCheck();
        }
    }

    @Test
    void noCommanderGrantsNoRecruits() {
        when(campaign.getPlayerForce().getHumanResources().getCommander(any(), anyBoolean(), any()))
              .thenReturn(null);
        assertEquals(0, check(10, 12));
    }

    @Test
    void burnedConnectionsGrantNoRecruits() {
        when(commander.getBurnedConnectionsEndDate()).thenReturn(TODAY.plusMonths(1));
        assertEquals(0, check(10, 12));
    }

    @Test
    void failedRollGrantsNoRecruits() {
        assertEquals(0, check(10, CONNECTIONS_TARGET_NUMBER - 1));
    }

    @ParameterizedTest
    @EnumSource(ConnectionsLevel.class)
    void successfulRollGrantsLevelRecruits(ConnectionsLevel level) {
        assertEquals(level.getRecruits(), check(level.getLevel(), CONNECTIONS_TARGET_NUMBER));
    }
}
