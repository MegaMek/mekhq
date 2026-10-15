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
import mekhq.campaign.mission.contract.contractGeneration.TrackIntensityTable;

/**
 * How many times a contract offer can pay its combat bonus, if the player wins every chance at it.
 *
 * <p>Ordinarily the bonus is paid for each Essential scenario won. Their schedule is rolled when the contract is
 * generated, so their number is already known. When the contract's special Nav Points replace its Essential scenarios
 * (under "Contracts Use Special Mechanics"), the bonus is paid for each Nav Point secured instead. Those are rolled the
 * same way, but only when the contract is accepted, so their average number is used. An Objective Raid's target
 * intelligence pays nothing itself: the bonus comes from the Essential scenarios fought at the facilities it leads to,
 * of which there is at most one per point of Scale. Without StratCon, or in mapless play, there are no Essential
 * scenarios or Nav Points, so no combat bonus is paid.</p>
 *
 * <p>Other one-off bonuses, such as handing a captured facility over to the employer or the fights at a contract's
 * objective facilities, depend on how the contract is played and are not counted.</p>
 *
 * @param bonusCount     how many times the bonus can be paid; an average, so not always whole
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

        StratConContractMechanics mechanics = StratConContractMechanics.forContract(contract);
        boolean isReplacingEssentialScenarios = campaignOptions.get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS)
                                                      && mechanics.isReplacingEssentialScenarios();
        if (!isReplacingEssentialScenarios) {
            int essentialScenarioCount = 0;
            for (int monthlyCount : contract.getScenarioSchedule()) {
                essentialScenarioCount += monthlyCount;
            }
            return new StratConCombatBonusProjection(essentialScenarioCount, false);
        }

        // Rolled as StratConContractInitializer.scheduleSpecialPointsOfInterest does, which rolls at least once
        int rollCount = campaignOptions.get(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE)
                              ? max(1, contract.getScale())
                              : 1;
        double navPointCount = TrackIntensityTable.getExpectedScheduleTotal(contract.getLengthInMonths(),
              contract.getTrackCount(),
              rollCount,
              campaignOptions.get(CampaignOption.MINIMUM_ONE_TRACK_PER_ROLL),
              campaignOptions.get(CampaignOption.ROLL_TRACKS_WEEKLY));

        if (StratConTargetIntelligenceBehavior.TYPE_ID.equals(mechanics.pointOfInterestTypeId())) {
            // Only one piece of intelligence per point of Scale leads to a facility (see its onScheduled)
            navPointCount = Math.min(navPointCount, max(1, contract.getScale()));
        }

        return new StratConCombatBonusProjection(navPointCount, true);
    }
}
