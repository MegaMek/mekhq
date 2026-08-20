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

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * A back-of-the-envelope profit estimate for a single contract offer.
 *
 * <p>The estimate weighs the guaranteed contract income (monthly pay plus the transport lump sum) against the
 * campaign's operating costs over the whole engagement. Crucially, the outbound transit is treated as unpaid: the
 * player still pays their monthly expenses while jumping to the target system but earns no monthly contract pay during
 * that time, so those months are pure cost.</p>
 *
 * <p>The figure is deliberately conservative: it counts only guaranteed pay and excludes combat pay (which depends on
 * how many battles are fought) and battlefield losses (which depend on how they go).</p>
 *
 * @param transitDays      one-way travel time to the target system, in days
 * @param guaranteedIncome monthly pay across the contract term plus the transport payment
 * @param operatingCosts   peacetime operating cost across the contract term and the transit out
 * @param estimatedNet     {@code guaranteedIncome} minus {@code operatingCosts}
 *
 * @author Illiani
 * @since 0.51.01
 */
public record ContractProfitEstimate(int transitDays, Money guaranteedIncome, Money operatingCosts,
      Money estimatedNet) {
    private static final double DAYS_PER_MONTH = 30.0;

    /**
     * Computes the profit estimate for the given offer in the context of the supplied campaign.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static ContractProfitEstimate estimate(Campaign campaign, AbstractContract contract) {
        PlayerForce playerForce = campaign.getPlayerForce();
        Detachment detachment = playerForce.getForceDetachment();
        FactionStandings factionStandings = playerForce.getFactionStandings();

        int transitDays = ContractUtilities.getTravelDays(campaign,
              contract,
              detachment.getCurrentLocation(),
              false,
              factionStandings);

        int months = contract.getLengthInMonths();
        Money guaranteedIncome = contract.getTotalMonthlyPay().plus(contract.getTransportPayment());

        Money peacetimeMonthly = campaign.getAccountant().getPeacetimeCost();
        double engagementMonths = months + (transitDays / DAYS_PER_MONTH);
        Money operatingCosts = peacetimeMonthly.multipliedBy(engagementMonths);

        Money estimatedNet = guaranteedIncome.minus(operatingCosts);

        return new ContractProfitEstimate(transitDays, guaranteedIncome, operatingCosts, estimatedNet);
    }
}
