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
package mekhq.campaign.mission.contract.contractData;

import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.Test;

/**
 * Checks the pay arithmetic on {@link ContractFinanceData}: monthly pay scales with contract length, and the total adds
 * the one-off transport cost on top.
 */
class ContractFinanceDataTest {

    private static final Money TRANSPORT = Money.of(1_000);
    private static final Money MONTHLY_PAY = Money.of(500);
    private static final Money COMBAT_PAY = Money.of(250);

    private static final ContractFinanceData FINANCE = new ContractFinanceData(TRANSPORT, MONTHLY_PAY, COMBAT_PAY);

    @Test
    void totalMonthlyPayScalesWithLength() {
        assertEquals(Money.of(1_500), FINANCE.getTotalMonthlyPay(3));
    }

    @Test
    void totalMonthlyPayOverZeroMonthsIsZero() {
        assertEquals(Money.zero(), FINANCE.getTotalMonthlyPay(0));
    }

    @Test
    void totalPayAddsTransportOnTopOfTheMonthlyTotal() {
        // 500 * 3 monthly + 1000 transport
        assertEquals(Money.of(2_500), FINANCE.getTotalPay(3));
    }

    @Test
    void totalPayOverZeroMonthsIsJustTransport() {
        assertEquals(TRANSPORT, FINANCE.getTotalPay(0));
    }
}
