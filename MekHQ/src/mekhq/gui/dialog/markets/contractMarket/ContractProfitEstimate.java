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
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCombatBonusProjection;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.ContractExpenseProjection;
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
 * campaign's expenses over the whole engagement. Crucially, the outbound transit is treated as unpaid: the player still
 * pays their monthly expenses while jumping to the target system but earns no monthly contract pay during that time, so
 * those months are pure cost.</p>
 *
 * <p>Expenses follow the way they are actually charged. Salaries, overhead, Hot Spots upkeep, and peacetime operating
 * costs are charged each month wherever the force is. Food is charged throughout, but housing only once the force is on
 * the target planet. Maintenance is projected per unit for each leg: off contract and travelling during the transit,
 * then on contract on the planet, where a combat contract maintains more often but at its own cost multiplier. Straight
 * support covers repairs, not maintenance (Hot Spots: Draconis Reach pg 27), so it is not counted.</p>
 *
 * <p>The estimated net is deliberately conservative: it counts only guaranteed pay, and excludes repairs and
 * battlefield losses (which depend on how the fighting goes). Combat pay is given separately, as the most the contract
 * can pay if every chance at the bonus is won (see {@link StratConCombatBonusProjection}).</p>
 *
 * @param transitDays        one-way travel time to the target system, in days
 * @param contractMonths     the contract term, in months
 * @param guaranteedIncome   monthly pay across the contract term plus the transport payment
 * @param operatingCosts     the total expenses across the contract term and the transit out
 * @param estimatedNet       {@code guaranteedIncome} minus {@code operatingCosts}
 * @param salaries           the payroll part of {@code operatingCosts}
 * @param maintenance        the unit maintenance part of {@code operatingCosts}
 * @param runningCosts       the overhead, Hot Spots upkeep, and peacetime operating cost part of
 *                           {@code operatingCosts}
 * @param foodAndHousing     the food and housing part of {@code operatingCosts}
 * @param combatBonus        how many times the combat bonus can be paid, and what pays it
 * @param potentialCombatPay the combat pay if every chance at the bonus is won
 *
 * @author Illiani
 * @since 0.51.01
 */
public record ContractProfitEstimate(int transitDays, int contractMonths, Money guaranteedIncome, Money operatingCosts,
      Money estimatedNet, Money salaries, Money maintenance, Money runningCosts, Money foodAndHousing,
      StratConCombatBonusProjection combatBonus, Money potentialCombatPay) {
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

        double transitMonths = transitDays / ContractExpenseProjection.DAYS_PER_MONTH;
        double engagementMonths = months + transitMonths;

        Accountant accountant = campaign.getAccountant();
        Money salaries = getMonthlySalaries(campaign, accountant).multipliedBy(engagementMonths);
        Money runningCosts = getMonthlyRunningCosts(campaign, accountant).multipliedBy(engagementMonths);
        Money foodAndHousing = getFoodAndHousing(campaign, contract, months, transitMonths);
        Money maintenance = getMaintenance(campaign, contract, months, transitDays);

        Money operatingCosts = salaries.plus(runningCosts).plus(foodAndHousing).plus(maintenance);
        Money estimatedNet = guaranteedIncome.minus(operatingCosts);

        StratConCombatBonusProjection combatBonus = StratConCombatBonusProjection.project(campaign, contract);
        Money potentialCombatPay = contract.getContractFinanceData().combatPay()
                                         .multipliedBy(combatBonus.bonusCount());

        return new ContractProfitEstimate(transitDays,
              months,
              guaranteedIncome,
              operatingCosts,
              estimatedNet,
              salaries,
              maintenance,
              runningCosts,
              foodAndHousing,
              combatBonus,
              potentialCombatPay);
    }

    /**
     * @return the estimated net plus the combat pay, if every chance at the combat bonus is won
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Money getNetWithCombatPay() {
        return estimatedNet.plus(potentialCombatPay);
    }

    /**
     * @return how long the force is away earning nothing else: the contract term plus the unpaid trip out, in months
     *
     * @author Illiani
     * @since 0.51.01
     */
    public double getEngagementMonths() {
        return contractMonths + (transitDays / ContractExpenseProjection.DAYS_PER_MONTH);
    }

    /**
     * The estimated net spread over the whole engagement. Because it counts the trip out, it lets a short contract far
     * away be weighed fairly against a long one nearby.
     *
     * @return the estimated net per month of the engagement
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Money getNetPerMonth() {
        double engagementMonths = getEngagementMonths();
        return engagementMonths > 0 ? estimatedNet.dividedBy(engagementMonths) : estimatedNet;
    }

    /**
     * @return the monthly payroll, or zero when salaries are not paid
     */
    private static Money getMonthlySalaries(Campaign campaign, Accountant accountant) {
        if (!campaign.getCampaignOptions().get(CampaignOption.PAY_FOR_SALARIES)) {
            return Money.zero();
        }
        return accountant.getPayRoll();
    }

    /**
     * The monthly costs charged wherever the force is, other than salaries and food and housing. Each is only included
     * while the campaign charges it; Hot Spots upkeep replaces overhead and peacetime costs.
     *
     * @return the monthly overhead, Hot Spots upkeep, and peacetime spare parts, fuel, and ammunition
     */
    private static Money getMonthlyRunningCosts(Campaign campaign, Accountant accountant) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        // Salaries are counted on their own, covering all salaried personnel rather than only unit crews
        Money runningCosts = campaignOptions.isChargingPeacetimeCost()
                                   ? accountant.getPeacetimeCost(false)
                                   : Money.zero();

        // These are zero unless their options are enabled
        runningCosts = runningCosts.plus(accountant.getOverheadExpenses());
        return runningCosts.plus(accountant.getHotSpotsUpkeepCosts());
    }

    /**
     * Food is charged for the whole engagement, housing only on the target planet. While travelling the force is not on
     * a planet, so only food is charged.
     *
     * @return the food and housing across the contract term and the transit out
     */
    private static Money getFoodAndHousing(Campaign campaign, AbstractContract contract, int months,
          double transitMonths) {
        Money transitFood = ContractExpenseProjection.getMonthlyTransitFood(campaign).multipliedBy(transitMonths);
        Money contractFoodAndHousing = ContractExpenseProjection.getMonthlyContractFoodAndHousing(campaign, contract)
                                             .multipliedBy(months);
        return transitFood.plus(contractFoodAndHousing);
    }

    /**
     * Unit maintenance off contract while travelling out, then on contract for the term. Straight support covers
     * repairs, not maintenance (Hot Spots: Draconis Reach pg 27), so none of it is reimbursed.
     *
     * @return the maintenance across the contract term and the transit out
     */
    private static Money getMaintenance(Campaign campaign, AbstractContract contract, int months, int transitDays) {
        Money transitMaintenance = ContractExpenseProjection.getTransitMaintenance(campaign, transitDays);
        Money contractMaintenance = ContractExpenseProjection.getMonthlyContractMaintenance(campaign, contract)
                                          .multipliedBy(months);
        return transitMaintenance.plus(contractMaintenance);
    }
}
