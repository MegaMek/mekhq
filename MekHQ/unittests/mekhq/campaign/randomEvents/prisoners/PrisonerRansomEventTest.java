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
package mekhq.campaign.randomEvents.prisoners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PrisonerRansomEvent}.
 *
 * <p>These tests exercise the stateless outcome helpers of the ransom event. The "GREGification" merge rerouted the
 * financial and prisoner operations through {@code getPlayerForce().getFinances()} and
 * {@code getPlayerForce().getHumanResources()}; the tests pin the transaction direction (credit vs debit) and the
 * prisoner disposition (return-to-active vs removal) so a reroute regression is caught.</p>
 */
class PrisonerRansomEventTest {

    @Test
    void testCalculateRansomCount_roundsUpToAtLeastOne() {
        // 10% of the prisoner pool, rounded up, and never fewer than one prisoner.
        assertEquals(1, PrisonerRansomEvent.calculateRansomCount(0));
        assertEquals(1, PrisonerRansomEvent.calculateRansomCount(1));
        assertEquals(1, PrisonerRansomEvent.calculateRansomCount(10));
        assertEquals(2, PrisonerRansomEvent.calculateRansomCount(11));
        assertEquals(3, PrisonerRansomEvent.calculateRansomCount(25));
    }

    @Test
    void testCalculateTotalRansom_friendly_multipliesSum() {
        // Setup
        Campaign campaign = mockCampaign();
        Person first = mock(Person.class);
        Person second = mock(Person.class);
        when(first.getRansomValue(campaign)).thenReturn(Money.of(100));
        when(second.getRansomValue(campaign)).thenReturn(Money.of(150));

        // Act
        Money result = PrisonerRansomEvent.calculateTotalRansom(List.of(first, second), campaign, true);

        // Assert — friendly POWs cost (sum * RANSOM_COST_MULTIPLIER) = (250 * 10)
        assertEquals(Money.of(2500), result);
    }

    @Test
    void testCalculateTotalRansom_enemy_dividesSum() {
        // Setup
        Campaign campaign = mockCampaign();
        Person first = mock(Person.class);
        Person second = mock(Person.class);
        when(first.getRansomValue(campaign)).thenReturn(Money.of(100));
        when(second.getRansomValue(campaign)).thenReturn(Money.of(150));

        // Act
        Money result = PrisonerRansomEvent.calculateTotalRansom(List.of(first, second), campaign, false);

        // Assert — enemy prisoners yield (sum / RANSOM_COST_DIVIDER) = (250 / 5)
        assertEquals(Money.of(50), result);
    }

    @Test
    void testCanAffordRansom_sufficientFunds_returnsTrue() {
        // Setup
        Campaign campaign = mockCampaign();
        when(campaign.getPlayerForce().getFinances().getBalance()).thenReturn(Money.of(1000));

        // Assert — equal funds are affordable, and so is anything cheaper
        assertTrue(PrisonerRansomEvent.canAffordRansom(campaign, Money.of(1000)));
        assertTrue(PrisonerRansomEvent.canAffordRansom(campaign, Money.of(500)));
    }

    @Test
    void testCanAffordRansom_insufficientFunds_returnsFalse() {
        // Setup
        Campaign campaign = mockCampaign();
        when(campaign.getPlayerForce().getFinances().getBalance()).thenReturn(Money.of(1000));

        // Assert
        assertFalse(PrisonerRansomEvent.canAffordRansom(campaign, Money.of(1500)));
    }

    @Test
    void testHandleRansomOutcome_friendly_debitsFundsAndReturnsToActiveDuty() {
        // Setup
        Campaign campaign = mockCampaign();
        LocalDate today = LocalDate.of(3151, 1, 1);
        when(campaign.getLocalDate()).thenReturn(today);
        Person pow = mock(Person.class);
        Money ransom = Money.of(500);

        // Act
        PrisonerRansomEvent.handleRansomOutcome(campaign, List.of(pow), ransom, true);

        // Assert — the player pays (debit) to recover their POWs, who return to active duty
        verify(campaign.getPlayerForce().getFinances())
              .debit(eq(TransactionType.RANSOM), eq(today), eq(ransom), anyString());
        verify(pow).changeStatus(campaign, today, PersonnelStatus.ACTIVE);
    }

    @Test
    void testHandleRansomOutcome_enemy_creditsFundsAndRemovesPrisoners() {
        // Setup
        Campaign campaign = mockCampaign();
        LocalDate today = LocalDate.of(3151, 1, 1);
        when(campaign.getLocalDate()).thenReturn(today);
        Person prisoner = mock(Person.class);
        Money ransom = Money.of(500);

        // Act
        PrisonerRansomEvent.handleRansomOutcome(campaign, List.of(prisoner), ransom, false);

        // Assert — the player is paid (credit) for enemy prisoners, who are removed from the campaign
        verify(campaign.getPlayerForce().getFinances())
              .credit(eq(TransactionType.RANSOM), eq(today), eq(ransom), anyString());
        verify(campaign.getPlayerForce().getHumanResources()).removePerson(campaign, prisoner);
    }
}
