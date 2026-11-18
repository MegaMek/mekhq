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
package mekhq.campaign.digitalGM.stratCon;

import static java.lang.Math.max;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConTargetIntelligenceBehavior;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * How many times a contract offer can pay its combat bonus, if the player wins every chance at it.
 *
 * <p>Ordinarily the bonus is paid for each Essential scenario won: one a month per point of Scale (see
 * {@link StratConSpawnRates}), laid out when the contract is generated, so their number is known exactly. A contract
 * whose definition has no Essential scenarios to fight gets none (see
 * {@link StratConContractInitializer#usesEssentialScenarios}). When the contract's special Nav Points replace its
 * Essential scenarios (under "Contracts Use Special Mechanics"), the bonus is paid for each Nav Point secured instead:
 * {@value StratConSpawnRates#NAV_POINTS_PER_SCALE} a month per point of Scale, also known exactly. An Objective Raid's
 * target intelligence pays nothing itself: the bonus comes from the Essential scenarios fought at the facilities it
 * leads to, of which there is at most one per point of Scale. Without StratCon, or in mapless play, there are no
 * Essential scenarios or Nav Points, so no combat bonus is paid.</p>
 *
 * <p>Other one-off bonuses, such as handing a captured facility over to the employer or the fights at a contract's
 * objective facilities, depend on how the contract is played and are not counted. Counterattacks and assaults on the
 * player's side's facilities never pay it.</p>
 *
 * @param bonusCount     how many times the bonus can be paid
 * @param isFromNavPoint {@code true} if the bonus comes from the contract's Nav Points rather than its Essential
 *                       scenarios
 *
 * @author Illiani
 * @since 0.51.01
 */
public record StratConCombatBonusProjection(double bonusCount, boolean isFromNavPoint) {
    /**
     * Projects how many times the given offer can pay its combat bonus.
     *
     * @param campaign the campaign, for its StratCon options
     * @param contract the contract offer
     *
     * @return the projection
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static StratConCombatBonusProjection project(Campaign campaign, AbstractContract contract) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (!campaignOptions.isUseStratCon() || campaignOptions.isUseStratConMaplessMode()) {
            return new StratConCombatBonusProjection(0, false);
        }

        boolean isContractsUseSpecialMechanics = campaignOptions.get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS);
        if (StratConContractInitializer.usesEssentialScenarios(contract, isContractsUseSpecialMechanics)) {
            int essentialScenarioCount = 0;
            for (int monthlyCount : contract.getScenarioSchedule()) {
                essentialScenarioCount += monthlyCount;
            }
            return new StratConCombatBonusProjection(essentialScenarioCount, false);
        }

        if (!StratConContractInitializer.isReplacingEssentialScenarios(contract, isContractsUseSpecialMechanics)) {
            // Neither Essential scenarios nor Nav Points standing in for them
            return new StratConCombatBonusProjection(0, false);
        }

        StratConContractMechanics mechanics = StratConContractMechanics.forContract(contract);
        double navPointCount = (double) max(1, contract.getLengthInMonths())
                                     * StratConSpawnRates.getCount(contract,
              StratConSpawnRates.isFactorSupportPointsIntoScale(campaignOptions, contract),
              StratConSpawnRates.NAV_POINTS_PER_SCALE);

        if (StratConTargetIntelligenceBehavior.TYPE_ID.equals(mechanics.pointOfInterestTypeId())) {
            // Only one piece of intelligence per point of Scale leads to a facility (see its onScheduled)
            navPointCount = Math.min(navPointCount, max(1, contract.getScale()));
        }

        return new StratConCombatBonusProjection(navPointCount, true);
    }

    /**
     * Works out how many combat bonuses a month a contract's combat pay is spread across, so every contract type offers
     * the same total combat pay at the same Scale and length: combat pay per bonus is the contract's monthly combat pay
     * divided by this.
     *
     * <p>Ordinarily that is the contract's Essential scenarios, {@value StratConSpawnRates#ESSENTIAL_SCENARIOS_PER_SCALE}
     * a month per point of Scale. Nav Points that replace a contract's Essential scenarios come
     * {@value StratConSpawnRates#NAV_POINTS_PER_SCALE} a month per point of Scale, so each pays proportionately less.
     * Both counts are rounded as the contract's schedules round them (see {@link StratConSpawnRates#getCount}), so the
     * monthly total holds at any Scale, not only a whole one. Target intelligence is the exception: it pays nothing
     * itself, and the Essential scenarios at the facilities it leads to are paid as Essential scenarios are.</p>
     *
     * @param campaignOptions the campaign options, for "Contracts Use Special Mechanics" and how the contract's Scale
     *                        is counted
     * @param contract        the contract
     *
     * @return the combat bonuses a month, never below one
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getMonthlyCombatBonusCount(CampaignOptions campaignOptions, AbstractContract contract) {
        // Compared rather than unboxed, so a mocked options object (which returns null) reads as off
        boolean isContractsUseSpecialMechanics =
              Boolean.TRUE.equals(campaignOptions.get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS));
        boolean isFactorSupportPointsIntoScale = StratConSpawnRates.isFactorSupportPointsIntoScale(campaignOptions,
              contract);

        boolean isPaidThroughNavPoints =
              StratConContractInitializer.isReplacingEssentialScenarios(contract, isContractsUseSpecialMechanics)
                    && !StratConTargetIntelligenceBehavior.TYPE_ID.equals(
                    StratConContractMechanics.forContract(contract).pointOfInterestTypeId());

        return StratConSpawnRates.getCount(contract,
              isFactorSupportPointsIntoScale,
              isPaidThroughNavPoints
                    ? StratConSpawnRates.NAV_POINTS_PER_SCALE
                    : StratConSpawnRates.ESSENTIAL_SCENARIOS_PER_SCALE);
    }
}
