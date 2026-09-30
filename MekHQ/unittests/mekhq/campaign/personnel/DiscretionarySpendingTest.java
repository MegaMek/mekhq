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
package mekhq.campaign.personnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/**
 * Tests the Wealth trait's monthly reinvestment and Extreme Expenditure mechanics in {@link DiscretionarySpending}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class DiscretionarySpendingTest {
    private static final LocalDate TODAY = LocalDate.of(3151, 1, 1);

    private Person person;
    private Finances finances;

    @BeforeEach
    void beforeEach() {
        Campaign campaign = mockCampaign();
        Faction faction = mock(Faction.class);
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);
        when(faction.getShortName()).thenReturn("MERC");

        person = new Person(campaign);
        finances = mock(Finances.class);
    }

    private String performMonthlySpending(int roll) {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(roll);
            return DiscretionarySpending.performDiscretionarySpending(person, finances, TODAY);
        }
    }

    private void verifyCredited(int amount) {
        verify(finances).credit(eq(TransactionType.WEALTH), eq(TODAY), eq(Money.of(amount)), anyString());
    }

    @ParameterizedTest
    @CsvSource({
          // wealth, roll, expected total
          // Every purchase succeeds: 1 major + (wealth) moderate + (wealth x 3) minor
          "0, 12, 200",
          "2, 12, 2403", // 875 + 2 x 350 + 6 x 138
          "10, 12, 1200000", // 150,000 + 10 x 60,000 + 30 x 15,000
          // Only the major purchase (target 6) succeeds
          "2, 7, 875",
          // Major and moderate (target 8) succeed, minor (target 10) fails
          "2, 9, 1575",
          // Negative Wealth has no moderate or minor purchases
          "-1, 12, 21"
    })
    void monthlySpendingCreditsSuccessfulPurchases(int wealth, int roll, int expected) {
        person.setWealth(wealth);

        String report = performMonthlySpending(roll);

        assertFalse(report.isBlank());
        verifyCredited(expected);
    }

    @Test
    void monthlySpendingWithEveryRollFailedCreditsNothing() {
        person.setWealth(5);

        String report = performMonthlySpending(2);

        assertFalse(report.isBlank());
        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    @Test
    void monthlySpendingDoesNotChangeWealth() {
        person.setWealth(5);
        performMonthlySpending(12);
        assertEquals(5, person.getWealth());
    }

    @ParameterizedTest
    @CsvSource({ "1, -1, 21", "5, 0, 1000", "4, 3, 6500", "5, 10, 750000" })
    void expenditureIsMajorLimitTimesWillpower(int willpower, int wealth, int expected) {
        assertEquals(expected, DiscretionarySpending.getExpenditure(willpower, wealth));
    }

    @Test
    void extremeExpenditureCostsOneWealthAndCreditsFunds() {
        person.setWealth(3);
        person.setAttributeScore(SkillAttribute.WILLPOWER, 4);

        String report = DiscretionarySpending.performExtremeExpenditure(person, finances, TODAY);

        assertFalse(report.isBlank());
        assertEquals(2, person.getWealth());
        assertTrue(person.isHasPerformedExtremeExpenditure());
        verifyCredited(DiscretionarySpending.getExpenditure(4, 3));
    }

    @Test
    void extremeExpenditureAtMinimumWealthDoesNothing() {
        person.setWealth(ATOWTraits.WEALTH.getMinimum());

        String report = DiscretionarySpending.performExtremeExpenditure(person, finances, TODAY);

        assertTrue(report.isEmpty());
        assertEquals(ATOWTraits.WEALTH.getMinimum(), person.getWealth());
        assertFalse(person.isHasPerformedExtremeExpenditure());
        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    @Test
    void extremeExpenditureCanReachMinimumWealth() {
        person.setWealth(0);
        person.setAttributeScore(SkillAttribute.WILLPOWER, 1);

        DiscretionarySpending.performExtremeExpenditure(person, finances, TODAY);

        assertEquals(ATOWTraits.WEALTH.getMinimum(), person.getWealth());
        verifyCredited(200);
    }
}
