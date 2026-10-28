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
package mekhq.gui.dialog.markets.contractMarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.finances.ContractExpenseProjection;
import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.Test;

/**
 * Tests the per-month figures of {@link ContractProfitEstimate}, which drive board sorting and the Compare page.
 *
 * <p>The outbound transit is unpaid, so it must stretch the engagement: a contract far away earns less per month than
 * the same contract nearby. The records are built directly, since {@link ContractProfitEstimate#estimate} needs a full
 * campaign.</p>
 */
class ContractProfitEstimateTest {
    /** {@link ContractExpenseProjection#DAYS_PER_MONTH} is 487/16 days, so this transit is exactly 16 months. */
    private static final int SIXTEEN_MONTHS_OF_TRANSIT = 487;

    private static final double DELTA = 1e-9;
    private static final double MONEY_DELTA = 0.01;

    @Test
    void engagementWithoutTransitIsTheContractTerm() {
        ContractProfitEstimate estimate = estimate(0, 6, 600_000);

        assertEquals(6, estimate.contractMonths());
        assertEquals(6.0, estimate.getEngagementMonths(), DELTA);
    }

    @Test
    void engagementAddsTheTransitInMonths() {
        assertEquals(20.0, estimate(SIXTEEN_MONTHS_OF_TRANSIT, 4, 0).getEngagementMonths(), DELTA);
        assertEquals(3 + (45 / ContractExpenseProjection.DAYS_PER_MONTH),
              estimate(45, 3, 0).getEngagementMonths(),
              DELTA);
    }

    @Test
    void engagementOfTransitOnlyIsTheTransit() {
        assertEquals(16.0, estimate(SIXTEEN_MONTHS_OF_TRANSIT, 0, 0).getEngagementMonths(), DELTA);
    }

    @Test
    void netPerMonthSpreadsTheNetOverTheContractTerm() {
        assertEquals(100_000, estimate(0, 6, 600_000).getNetPerMonth().getAmount().doubleValue(), MONEY_DELTA);
    }

    @Test
    void netPerMonthSpreadsTheNetOverTheTransitToo() {
        ContractProfitEstimate estimate = estimate(SIXTEEN_MONTHS_OF_TRANSIT, 4, 2_000_000);

        assertEquals(100_000, estimate.getNetPerMonth().getAmount().doubleValue(), MONEY_DELTA);
    }

    @Test
    void distantContractEarnsLessPerMonthThanTheSameContractNearby() {
        Money nearby = estimate(0, 12, 1_200_000).getNetPerMonth();
        Money distant = estimate(SIXTEEN_MONTHS_OF_TRANSIT, 12, 1_200_000).getNetPerMonth();

        assertTrue(distant.isLessThan(nearby), "distant " + distant + " should be less than nearby " + nearby);
    }

    @Test
    void netPerMonthKeepsALossNegative() {
        assertEquals(-50_000, estimate(0, 6, -300_000).getNetPerMonth().getAmount().doubleValue(), MONEY_DELTA);
    }

    @Test
    void netPerMonthOfAZeroLengthEngagementIsTheNetUnchanged() {
        ContractProfitEstimate estimate = estimate(0, 0, 250_000);

        assertEquals(0.0, estimate.getEngagementMonths(), DELTA);
        assertEquals(estimate.estimatedNet(), estimate.getNetPerMonth());
    }

    @Test
    void engagementWithNoRouteIsTheContractTermAlone() {
        ContractProfitEstimate estimate = estimate(ContractProfitEstimate.NO_ROUTE, 6, 600_000);

        assertFalse(estimate.hasRoute());
        assertEquals(6.0, estimate.getEngagementMonths(), DELTA);
        assertEquals(100_000, estimate.getNetPerMonth().getAmount().doubleValue(), MONEY_DELTA);
    }

    @Test
    void aSameSystemTripHasARoute() {
        assertTrue(estimate(0, 6, 0).hasRoute());
    }

    @Test
    void netWithCombatPayAddsThePotentialCombatPay() {
        ContractProfitEstimate estimate = new ContractProfitEstimate(0, 6, Money.zero(), Money.zero(),
              Money.of(600_000), Money.zero(), Money.zero(), Money.zero(), Money.zero(), Money.zero(), Money.zero(), Money.zero(), Money.zero(), null, Money.of(150_000));

        assertEquals(750_000, estimate.getNetWithCombatPay().getAmount().doubleValue(), MONEY_DELTA);
    }

    /** An estimate with the given term, transit and net; the cost breakdown is irrelevant to the per-month figures. */
    private static ContractProfitEstimate estimate(int transitDays, int contractMonths, double estimatedNet) {
        return new ContractProfitEstimate(transitDays,
              contractMonths,
              Money.zero(),
              Money.zero(),
              Money.of(estimatedNet),
              Money.zero(),
              Money.zero(),
              Money.zero(),
              Money.zero(),
              Money.zero(),
              Money.zero(),
              Money.zero(),
              Money.zero(),
              null,
              Money.zero());
    }
}
