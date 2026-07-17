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

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.IForceDeploymentStrategy;
import mekhq.campaign.mission.AtBContract;

/**
 * Default StratCon implementation of {@link IForceDeploymentStrategy}. Every method delegates to the existing static
 * logic on {@link StratConRulesManager}, so this class introduces the overridable seam without moving any behaviour.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConForceDeploymentStrategy implements IForceDeploymentStrategy {

    @Override
    public void deployForceToCoords(StratConCoords coords, int forceID, Campaign campaign, AtBContract contract,
          StratConTrackState track, boolean sticky) {
        StratConRulesManager.deployForceToCoords(coords, forceID, campaign, contract, track, sticky);
    }

    @Override
    public void assignForceToScenario(StratConCoords coords, int forceID, Campaign campaign, AtBContract contract,
          StratConTrackState track, boolean sticky) {
        StratConRulesManager.assignForceToScenario(coords, forceID, campaign, contract, track, sticky);
    }

    @Override
    public void processForceDeployment(StratConCoords coords, int forceID, Campaign campaign, StratConTrackState track,
          boolean sticky) {
        StratConRulesManager.processForceDeployment(coords, forceID, campaign, track, sticky);
    }

    @Override
    public void commitPrimaryForces(Campaign campaign, StratConScenario scenario, StratConTrackState trackState) {
        StratConRulesManager.commitPrimaryForces(campaign, scenario, trackState);
    }
}
