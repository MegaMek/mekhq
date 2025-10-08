/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordLoanPaymentNag.unableToAffordLoans;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.gui.dialog.nagDialogs.UnableToAffordLoanPaymentNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class is a test class for the {@link UnableToAffordLoanPaymentNagDialog} class. It contains tests for various
 * scenarios related to the {@code getTotalPaymentsDue} and {@code isUnableToAffordLoanPayment} methods
 */
class UnableToAffordLoanPaymentNagLogicTest {
    private LocalDate today;
    private Finances finances;
    private Loan firstLoan, secondLoan;

    /**
     * Test setup for each test, runs before each test. Initializes the mock objects and sets up the necessary mock
     * behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        //  for the tests
        Campaign campaign = mock(Campaign.class);
        today = LocalDate.now();
        finances = mock(Finances.class);
        firstLoan = mock(Loan.class);
        secondLoan = mock(Loan.class);

        // Stubs
        when(campaign.getFinances()).thenReturn(finances);
        when(campaign.getLocalDate()).thenReturn(today);

        when(firstLoan.getPaymentAmount()).thenReturn(Money.of(5));
        when(secondLoan.getPaymentAmount()).thenReturn(Money.of(5));
    }

    /**
     * Initializes the loans with the specified number of days till the next payment.
     *
     * @param daysTillFirstLoan The number of days till the next payment for the first loan.
     */
    private void initializeLoans(int daysTillFirstLoan) {
        when(finances.getLoans()).thenReturn(List.of(firstLoan, secondLoan));

        when(firstLoan.getNextPayment()).thenReturn(today.plusDays(daysTillFirstLoan));
        when(secondLoan.getNextPayment()).thenReturn(today.plusDays(1));
    }

    // In the following tests the getTotalPaymentsDue() method is called, and its response
    // is checked against expected behavior

    @Test
    void canAffordLoans() {
        initializeLoans(2);

        Money currentFunds = Money.of(10);

        assertFalse(unableToAffordLoans(List.of(firstLoan, secondLoan), today, currentFunds));
    }

    @Test
    void cannotAffordLoans() {
        initializeLoans(1);

        Money currentFunds = Money.of(5);

        assertTrue(unableToAffordLoans(List.of(firstLoan, secondLoan), today, currentFunds));
    }
}
