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

import static java.lang.Math.round;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.chaosCampaign.ChaosCampaignUtilities;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import org.jspecify.annotations.NonNull;

/**
 * The default Chaos Campaign pay scheme: monthly and combat pay are derived from the contract's abstract scale and
 * fixed support-point multipliers, then optionally converted to C-bills. Transport pay is inherited unchanged from
 * {@link AbstractContractDeterminationPay}.
 *
 * @see AbstractContractDeterminationPay
 * @see CamOpsContractDeterminationPay
 */
public class ChaosContractDeterminationPay extends AbstractContractDeterminationPay {
    public final static int DEFAULT_MONTHLY_PAY_MULTIPLIER = 500; // Draconis Reach first printing pg 26
    public final static int DEFAULT_COMBAT_PAY_MULTIPLIER = 500; // Draconis Reach first printing pg 26

    @Override
    public @NonNull Money getCombatPay(Campaign campaign, AbstractContract contract) {
        int scale = contract.getScale();
        int combatPayInSupportPoints = DEFAULT_COMBAT_PAY_MULTIPLIER * scale;
        // When "Multiply Track Intensity by Scale" is set, a contract fields scale times as many scenarios. Divide
        // combat pay by scale so it stays flat across those extra scenarios rather than growing with their number.
        if ((scale > 0) && campaign.getCampaignOptions().get(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE)) {
            combatPayInSupportPoints /= scale;
        }
        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(combatPayInSupportPoints,
              shouldConvertSupportPoints(campaign));
    }

    @Override
    public @NonNull Money getMonthlyPay(Campaign campaign, AbstractContract contract) {
        int monthlyPayInSupportPoints = DEFAULT_MONTHLY_PAY_MULTIPLIER * contract.getScale();
        monthlyPayInSupportPoints = (int) round(monthlyPayInSupportPoints * contract.getBasePayMultiplier());
        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(monthlyPayInSupportPoints,
              shouldConvertSupportPoints(campaign));
    }
}
