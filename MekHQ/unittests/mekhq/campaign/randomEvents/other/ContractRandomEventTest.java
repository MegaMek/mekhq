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
package mekhq.campaign.randomEvents.other;

import static mekhq.campaign.randomEvents.other.ContractRandomEvent.getEventType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.randomEvents.other.ContractRandomEvent.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the monthly contract random event roll.
 */
class ContractRandomEventTest {

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4 })
    @DisplayName("A 1 to 4 grants bonus Employer Support")
    void oneToFourGrantsEmployerSupport(int roll) {
        assertEquals(EventType.EMPLOYER_SUPPORT, getEventType(roll));
    }

    @Test
    @DisplayName("A 5 offers a Ronin")
    void fiveOffersRonin() {
        assertEquals(EventType.RONIN, getEventType(5));
    }

    @Test
    @DisplayName("A 6 opens a mercenary auction")
    void sixOpensAuction() {
        assertEquals(EventType.AUCTION, getEventType(6));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 7 })
    @DisplayName("A roll outside 1d6 is rejected")
    void rollOutsideOneDieIsRejected(int roll) {
        assertThrows(IllegalArgumentException.class, () -> getEventType(roll));
    }

    @Test
    @DisplayName("A contract without StratCon never rolls for an event")
    void contractWithoutStratConNeverRolls() {
        Campaign campaign = mock(Campaign.class);
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStratConCampaignState()).thenReturn(null);

        ContractRandomEvent.processMonthlyEvent(campaign, contract);

        verifyNoInteractions(campaign);
    }
}
