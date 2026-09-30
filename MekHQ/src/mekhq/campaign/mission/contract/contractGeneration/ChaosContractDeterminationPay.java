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

import static java.lang.Math.ceil;
import static java.lang.Math.round;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.chaosCampaign.ChaosCampaignUtilities;
import mekhq.campaign.chaosCampaign.ChaosScaleLimits;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.PlayerForce;
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
        if (campaign.getCampaignOptions().get(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE)) {
            combatPayInSupportPoints = (int) round(DEFAULT_COMBAT_PAY_MULTIPLIER
                                                         * ChaosScaleLimits.getTaperedScale(scale));
        }
        // When "Multiply Track Intensity by Scale" is set, a contract fields scale times as many scenarios. Divide
        // combat pay by scale so it stays flat across those extra scenarios rather than growing with their number.
        if ((scale > 0) && campaign.getCampaignOptions().get(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE)) {
            combatPayInSupportPoints /= scale;
        }
        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(combatPayInSupportPoints,
              shouldConvertSupportPoints(campaign));
    }

    /**
     * The monthly retainer. Hot Spots: Draconis Reach (first printing pg 26) intends Base Pay to cover maintenance and
     * salaries, so by default it is the force's covered monthly running costs (see {@link #getCoveredMonthlyCosts}).
     * With {@link CampaignOption#BASE_PAY_ONLY_CONSIDERS_SCALE} it is instead a flat amount per Scale. Either way it is
     * scaled by the contract's base pay multiplier.
     */
    /**
     * Salvage rights taper off above Scale 3 when {@link CampaignOption#TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE} is set,
     * matching the combat pay taper.
     */
    @Override
    public double getSalvageTaperMultiplier(Campaign campaign, AbstractContract contract) {
        if (!campaign.getCampaignOptions().get(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE)) {
            return 1.0;
        }
        return ChaosScaleLimits.getTaperMultiplier(contract.getScale());
    }

    @Override
    public @NonNull Money getMonthlyPay(Campaign campaign, AbstractContract contract) {
        boolean isConvertSupportPoints = shouldConvertSupportPoints(campaign);

        if (campaign.getCampaignOptions().get(CampaignOption.BASE_PAY_ONLY_CONSIDERS_SCALE)) {
            int monthlyPayInSupportPoints = DEFAULT_MONTHLY_PAY_MULTIPLIER * contract.getScale();
            monthlyPayInSupportPoints = (int) round(monthlyPayInSupportPoints * contract.getBasePayMultiplier());
            return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(monthlyPayInSupportPoints,
                  isConvertSupportPoints);
        }

        Money monthlyPay = getCoveredMonthlyCosts(campaign)
                                 .multipliedBy(getJobShare(campaign, contract))
                                 .multipliedBy(contract.getBasePayMultiplier());
        if (isConvertSupportPoints) {
            return monthlyPay;
        }

        // Pay is expressed in raw support points, so convert the C-bill costs, rounding up
        double monthlyPayInSupportPoints = monthlyPay.getAmount().doubleValue()
                                                 / ChaosCampaignUtilities.SUPPORT_POINTS_TO_MONEY_CONVERSION;
        return Money.of(ceil(monthlyPayInSupportPoints));
    }

    /**
     * When contract Scale is capped by the Hiring Hall, the employer only covers the costs of the part of the force the
     * job needs: the capped Scale as a share of the committed force's Scale.
     *
     * @param campaign the campaign
     * @param contract the contract being paid
     *
     * @return the share of the force's costs the employer covers, {@code [0, 1]}
     *
     * @author Illiani
     * @since 0.51.01
     */
    static double getJobShare(Campaign campaign, AbstractContract contract) {
        if (!campaign.getCampaignOptions().get(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL)) {
            return 1.0;
        }

        PlayerForce playerForce = campaign.getPlayerForce();
        int uncappedScale = AbstractContractGeneration.determineUncappedScale(campaign, playerForce,
              playerForce.getHangar(), contract);
        int contractScale = contract.getScale();
        if (uncappedScale <= 0 || contractScale >= uncappedScale) {
            return 1.0;
        }

        return Math.max(0, contractScale) / (double) uncappedScale;
    }

    /**
     * The monthly running costs Base Pay covers: salaries, unit maintenance, Hot Spots upkeep, overhead, and food and
     * housing. Each is only included while the campaign actually charges it. The player force is a single detachment,
     * so its costs are the detachment's costs.
     *
     * @param campaign the campaign
     *
     * @return the covered monthly costs, in C-bills
     *
     * @author Illiani
     * @since 0.51.01
     */
    static Money getCoveredMonthlyCosts(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Accountant accountant = campaign.getAccountant();

        Money coveredCosts = Money.zero();
        if (campaignOptions.get(CampaignOption.PAY_FOR_SALARIES)) {
            coveredCosts = coveredCosts.plus(accountant.getPayRoll());
        }

        if (campaignOptions.isChargingMaintenance()) {
            // Maintenance is charged weekly; the planetary maintenance reduction never applies on contract
            Money weeklyMaintenance =
                  Accountant.getWeeklyMaintenanceTotal(campaign.getPlayerForce().getHangar().getUnits());
            coveredCosts = coveredCosts.plus(weeklyMaintenance.multipliedBy(4));
        }

        // These are zero unless their options are enabled
        coveredCosts = coveredCosts.plus(accountant.getHotSpotsUpkeepCosts());
        coveredCosts = coveredCosts.plus(accountant.getOverheadExpenses());
        coveredCosts = coveredCosts.plus(accountant.getMonthlyFoodAndHousingExpenses());

        return coveredCosts;
    }
}
