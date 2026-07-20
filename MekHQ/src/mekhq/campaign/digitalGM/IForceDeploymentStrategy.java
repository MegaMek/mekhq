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

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.AbstractStratConGM;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.AtBContract;

/**
 * Strategy for placing player forces into scenarios &mdash; direct deployment to coordinates, assignment to a scenario,
 * and committing primary forces. This is the seam that a mapless GM diverges on most, since it bypasses the
 * map/coordinate model.
 *
 * <p>The method signatures mirror the corresponding static entry points on
 * {@link mekhq.campaign.digitalGM.stratCon.StratConRulesManager StratConRulesManager}; the default StratCon
 * implementation delegates to them, so the rules themselves are unchanged. The accessor lives on
 * {@link AbstractStratConGM AbstractStratConGM}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface IForceDeploymentStrategy {

    /**
     * Deploys a force to a set of coordinates, potentially revealing terrain and spawning a scenario.
     *
     * @param coords   the coordinates to deploy to
     * @param forceID  the force being deployed
     * @param campaign the active campaign
     * @param contract the contract owning the track
     * @param track    the track being deployed on
     * @param sticky   {@code true} if the deployment should persist rather than being cleared each cycle
     */
    void deployForceToCoords(StratConCoords coords, int forceID, Campaign campaign, AtBContract contract,
          StratConTrackState track, boolean sticky);

    /**
     * Assigns a force to the scenario located at the given coordinates.
     *
     * @param coords   the coordinates of the scenario
     * @param forceID  the force being assigned
     * @param campaign the active campaign
     * @param contract the contract owning the track
     * @param track    the track containing the scenario
     * @param sticky   {@code true} if the assignment should persist rather than being cleared each cycle
     */
    void assignForceToScenario(StratConCoords coords, int forceID, Campaign campaign, AtBContract contract,
          StratConTrackState track, boolean sticky);

    /**
     * Processes the mechanical consequences of deploying a force to a set of coordinates (scouting neighbours, clearing
     * prior placements, and so on).
     *
     * @param coords   the coordinates deployed to
     * @param forceID  the deployed force
     * @param campaign the active campaign
     * @param track    the track being deployed on
     * @param sticky   {@code true} if the deployment should persist rather than being cleared each cycle
     */
    void processForceDeployment(StratConCoords coords, int forceID, Campaign campaign, StratConTrackState track,
          boolean sticky);

    /**
     * Commits the primary forces to a scenario, adding it to the track and finalising its deployment dates.
     *
     * @param campaign   the active campaign
     * @param scenario   the scenario receiving the committed forces
     * @param trackState the track the scenario belongs to
     */
    void commitPrimaryForces(Campaign campaign, StratConScenario scenario, StratConTrackState trackState);
}
