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

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractFinanceData;
import org.jspecify.annotations.NonNull;

/**
 * Determines contract pay the Campaign Operations (CamOps) way, as an alternative to the default Chaos Campaign scheme
 * in {@link ChaosContractDeterminationPay}.
 *
 * <p>Where the Chaos scheme derives pay from the contract's abstract scale and support-point multipliers, the CamOps
 * scheme grounds the monthly retainer in the force-value calculation configured on the Contract Market campaign options
 * page ({@link mekhq.campaign.finances.Accountant#getContractBase()}): the total value of the units the player commits,
 * honoring the TOE-percent / sale-value / diminishing-returns / alternate-payment-model settings, or the theoretical
 * payroll when the "Payroll Influences Pay" basis is chosen.</p>
 *
 * <p>The mapping into the contract's {@link ContractFinanceData} is:</p>
 * <ul>
 *     <li><b>Monthly pay</b> &mdash; the CamOps base value, scaled by the contract's negotiated base-pay multiplier so
 *     negotiation terms still matter under CamOps.</li>
 *     <li><b>Combat pay</b> &mdash; zero; CamOps folds combat compensation into the monthly retainer rather than a
 *     separate battle bonus.</li>
 *     <li><b>Transport pay</b> &mdash; inherited from {@link AbstractContractDeterminationPay} (scale, transport terms,
 *     and the journey to the target), which already applies the negotiated transport multiplier.</li>
 * </ul>
 *
 * @see AbstractContractDeterminationPay
 * @see ChaosContractDeterminationPay
 */
public class CamOpsContractDeterminationPay extends AbstractContractDeterminationPay {
    /**
     * The CamOps monthly retainer: the campaign's CamOps contract-base force value, scaled by the contract's negotiated
     * base-pay multiplier.
     */
    @Override
    public @NonNull Money getMonthlyPay(Campaign campaign, AbstractContract contract) {
        Money base = campaign.getAccountant().getContractBase();
        return base.multipliedBy(contract.getBasePayMultiplier());
    }

    /** CamOps folds combat compensation into the monthly retainer, so there is no separate combat bonus. */
    @Override
    public @NonNull Money getCombatPay(Campaign campaign, AbstractContract contract) {
        return Money.zero();
    }
}
