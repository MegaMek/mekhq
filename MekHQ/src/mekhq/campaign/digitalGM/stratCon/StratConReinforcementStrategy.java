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

import megamek.common.annotations.Nullable;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.IReinforcementStrategy;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementResultsType;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;

/**
 * Default StratCon implementation of {@link IReinforcementStrategy}. Every method delegates to the existing static
 * logic on {@link StratConRulesManager}, so this class introduces the overridable seam without moving any behaviour.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConReinforcementStrategy implements IReinforcementStrategy {

    @Override
    public ReinforcementEligibilityType getReinforcementType(int forceID, StratConTrackState trackState,
          Campaign campaign, StratConCampaignState campaignState) {
        return StratConRulesManager.getReinforcementType(forceID, trackState, campaign, campaignState);
    }

    @Override
    public TargetRoll calculateReinforcementTargetNumber(@Nullable Person commandLiaison, AtBContract contract,
          int baseTargetNumber) {
        return StratConRulesManager.calculateReinforcementTargetNumber(commandLiaison, contract, baseTargetNumber);
    }

    @Override
    public ReinforcementResultsType processReinforcementDeployment(Formation formation,
          ReinforcementEligibilityType reinforcementType, StratConCampaignState campaignState,
          StratConScenario scenario, Campaign campaign, int reinforcementTargetNumber, boolean isGMReinforcement) {
        return processReinforcementDeployment(formation,
              reinforcementType,
              campaignState,
              scenario,
              campaign,
              reinforcementTargetNumber,
              isGMReinforcement,
              false);
    }

    @Override
    public ReinforcementResultsType processReinforcementDeployment(Formation formation,
          ReinforcementEligibilityType reinforcementType, StratConCampaignState campaignState,
          StratConScenario scenario, Campaign campaign, int reinforcementTargetNumber, boolean isGMReinforcement,
          boolean isInstantlyDeployed) {
        return StratConRulesManager.processReinforcementDeployment(formation, reinforcementType, campaignState,
              scenario, campaign, reinforcementTargetNumber, isGMReinforcement, isInstantlyDeployed);
    }
}
