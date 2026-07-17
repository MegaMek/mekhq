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
package mekhq.campaign.digitalGM;

import megamek.common.annotations.Nullable;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.AbstractStratConGM;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementResultsType;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;

/**
 * Strategy for reinforcement handling &mdash; whether a force may reinforce an ongoing scenario, the target number it
 * must beat, and the outcome of the attempt.
 *
 * <p>The method signatures mirror the corresponding static entry points on
 * {@link mekhq.campaign.digitalGM.stratCon.StratConRulesManager StratConRulesManager}; the default StratCon
 * implementation delegates to them, so the rules themselves are unchanged. The accessor lives on
 * {@link AbstractStratConGM AbstractStratConGM}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface IReinforcementStrategy {

    /**
     * Determines a force's eligibility to reinforce, given its current commitments.
     *
     * @param forceID       the force in question
     * @param trackState    the track the reinforcement would occur on
     * @param campaign      the active campaign
     * @param campaignState the StratCon state for the contract
     *
     * @return the reinforcement eligibility category for the force
     */
    ReinforcementEligibilityType getReinforcementType(int forceID, StratConTrackState trackState, Campaign campaign,
          StratConCampaignState campaignState);

    /**
     * Calculates the target number a reinforcement attempt must beat.
     *
     * @param commandLiaison   the liaison influencing the roll, if any
     * @param contract         the active contract
     * @param baseTargetNumber the base target number before modifiers
     *
     * @return the assembled reinforcement target roll
     */
    TargetRoll calculateReinforcementTargetNumber(@Nullable Person commandLiaison, AtBContract contract,
          int baseTargetNumber);

    /**
     * Processes a reinforcement deployment attempt.
     *
     * @param formation                 the reinforcing formation
     * @param reinforcementType         the force's reinforcement eligibility
     * @param campaignState             the StratCon state for the contract
     * @param scenario                  the scenario being reinforced
     * @param campaign                  the active campaign
     * @param reinforcementTargetNumber the target number to beat
     * @param isGMReinforcement         {@code true} if the deployment is GM-forced
     *
     * @return the result of the reinforcement attempt
     */
    ReinforcementResultsType processReinforcementDeployment(Formation formation,
          ReinforcementEligibilityType reinforcementType, StratConCampaignState campaignState,
          StratConScenario scenario, Campaign campaign, int reinforcementTargetNumber, boolean isGMReinforcement);

    /**
     * Processes a reinforcement deployment attempt, optionally bypassing the roll for an instant deployment.
     *
     * @param formation                 the reinforcing formation
     * @param reinforcementType         the force's reinforcement eligibility
     * @param campaignState             the StratCon state for the contract
     * @param scenario                  the scenario being reinforced
     * @param campaign                  the active campaign
     * @param reinforcementTargetNumber the target number to beat
     * @param isGMReinforcement         {@code true} if the deployment is GM-forced
     * @param isInstantlyDeployed       {@code true} to deploy immediately, skipping the roll
     *
     * @return the result of the reinforcement attempt
     */
    ReinforcementResultsType processReinforcementDeployment(Formation formation,
          ReinforcementEligibilityType reinforcementType, StratConCampaignState campaignState,
          StratConScenario scenario, Campaign campaign, int reinforcementTargetNumber, boolean isGMReinforcement,
          boolean isInstantlyDeployed);
}
