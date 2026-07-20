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
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.digitalGM.stratCon.AbstractStratConGM;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;

/**
 * Strategy for advancing scenarios through their lifecycle: expiring scenarios the player ignored, returning committed
 * forces once their deployment ends, and applying the consequences of a completed scenario.
 *
 * <p>The method signatures mirror the corresponding static entry points on
 * {@link mekhq.campaign.digitalGM.stratCon.StratConRulesManager StratConRulesManager}; the default StratCon
 * implementation delegates to them, so the rules themselves are unchanged &mdash; only the daily loop that invokes them
 * now lives in {@link AbstractStratConGM AbstractStratConGM}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface IScenarioLifecycleStrategy {

    /**
     * Processes the return of forces whose deployment window on a track has elapsed.
     *
     * @param track    the track whose deployed forces are checked
     * @param campaign the active campaign
     */
    void processForceReturnDates(StratConTrackState track, Campaign campaign);

    /**
     * Applies the consequences of a scenario the player allowed to expire without committing forces.
     *
     * @param scenario      the expired scenario
     * @param track         the track the scenario belongs to
     * @param campaignState the StratCon state for the contract
     */
    void processExpiredScenario(StratConScenario scenario, StratConTrackState track,
          StratConCampaignState campaignState);

    /**
     * Applies the results of a resolved scenario (rewards, casualties, facility changes, and so on).
     *
     * @param tracker the resolution tracker carrying the completed scenario's outcome
     */
    void processScenarioCompletion(ResolveScenarioTracker tracker);
}
