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
package mekhq.campaign.mission.contract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import org.junit.jupiter.api.Test;

/**
 * Tests the CamOps pay scheme: {@link CamOpsContractDeterminationPay#getMonthlyPay} grounds the retainer in the
 * campaign's CamOps contract-base force value scaled by the negotiated base-pay multiplier, while
 * {@link CamOpsContractDeterminationPay#getCombatPay} is always zero because CamOps folds combat compensation into the
 * monthly retainer.
 */
class CamOpsContractDeterminationPayTest {
    private final CamOpsContractDeterminationPay payScheme = new CamOpsContractDeterminationPay();

    private static Campaign campaignWithContractBase(Money contractBase) {
        Accountant accountant = mock(Accountant.class);
        when(accountant.getContractBase()).thenReturn(contractBase);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getAccountant()).thenReturn(accountant);
        return campaign;
    }

    private static AbstractContract contractWithBasePayMultiplier(double basePayMultiplier) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getBasePayMultiplier()).thenReturn(basePayMultiplier);
        return contract;
    }

    @Test
    void monthlyPayIsTheContractBaseUnchangedAtAUnitMultiplier() {
        assertEquals(Money.of(5_000_000),
              payScheme.getMonthlyPay(campaignWithContractBase(Money.of(5_000_000)),
                    contractWithBasePayMultiplier(1.0)));
    }

    @Test
    void monthlyPayScalesTheContractBaseByTheNegotiatedMultiplier() {
        // 5,000,000 force value * 1.5 negotiated base-pay multiplier.
        assertEquals(Money.of(7_500_000),
              payScheme.getMonthlyPay(campaignWithContractBase(Money.of(5_000_000)),
                    contractWithBasePayMultiplier(1.5)));
    }

    @Test
    void combatPayIsAlwaysZero() {
        assertEquals(Money.zero(),
              payScheme.getCombatPay(mock(Campaign.class), mock(AbstractContract.class)));
    }
}
