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
package mekhq.campaign.digitalGM.stratCon.deployment;

import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.calculateReinforcementTargetNumber;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.getReinforcementType;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.utilities.CombatRole;
import mekhq.campaign.personnel.Person;

/**
 * Campaign-context helper that answers the reinforcement questions the inspector needs for a focused force: how it is
 * eligible to reinforce, and what its roll and odds are at a given support-point spend. It gathers the campaign inputs
 * (command liaison, contract, base target number) once and defers the arithmetic to {@link DeploymentEvaluator}.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ReinforcementAdvisor {
    private final Campaign campaign;
    private final StratConCampaignState campaignState;
    private final StratConTrackState track;
    private final TargetRoll baseTargetRoll;

    public ReinforcementAdvisor(Campaign campaign, StratConCampaignState campaignState, StratConTrackState track) {
        this.campaign = campaign;
        this.campaignState = campaignState;
        this.track = track;
        this.baseTargetRoll = computeBaseTargetRoll();
    }

    /**
     * @return the contract-modified base target roll shared by every reinforcement attempt on this contract, before any
     *       support-point spend, with its individual modifiers preserved for display in the confirmation dialog
     *
     * @author Illiani
     * @since 0.51.01
     */
    public TargetRoll getBaseTargetRoll() {
        return baseTargetRoll;
    }

    /**
     * @return the contract-modified base target number shared by every reinforcement attempt on this contract, before
     *       any support-point spend
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getBaseTargetNumber() {
        return baseTargetRoll.getValue();
    }

    /**
     * @param forceId the force being evaluated
     *
     * @return how that force is eligible to reinforce this scenario (regular roll, improved roll, already deployed, or
     *       none)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ReinforcementEligibilityType getEligibility(int forceId) {
        return getReinforcementType(forceId, track, campaign, campaignState);
    }

    /**
     * @param chosenSupportPoints the support points the player would spend per force to improve the roll
     * @param instant             whether the reinforcements would arrive instantly
     * @param isManeuver if the formation is set to {@link CombatRole#MANEUVER}} and therefore enjoys improved reinforcement rolls
     *
     * @return the final target number and success odds for a single force at that spend
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ReinforcementRoll getRoll(int chosenSupportPoints, boolean instant, boolean isManeuver) {
        int modifier = DeploymentEvaluator.reinforcementCost(chosenSupportPoints, instant, 1).targetNumberModifier();
        return DeploymentEvaluator.reinforcementRoll(baseTargetRoll.getValue(), modifier, isManeuver);
    }

    private TargetRoll computeBaseTargetRoll() {
        Person commandLiaison = campaign.getPlayerForce()
                                      .getHumanResources()
                                      .getSeniorAdminPerson(campaign.getCampaignOptions(),
                                            campaign.getPlayerForce().isClanForce(),
                                            campaign.getLocalDate());
        int base = campaign.getCampaignOptions().get(CampaignOption.REINFORCEMENT_BASE_TARGET_NUMBER);
        AbstractContract contract = campaignState.getContract();
        return calculateReinforcementTargetNumber(commandLiaison, contract, base);
    }
}
