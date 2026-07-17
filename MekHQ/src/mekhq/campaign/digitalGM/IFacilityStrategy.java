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

import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;

/**
 * Strategy for map facilities &mdash; applying their periodic effects, updating them when a scenario resolves, and
 * transferring ownership. Mapless (and, by extension, Singles) play has no facility map at all, so its implementation
 * is a no-op; the legacy engine gated the periodic call behind {@code if (!isUseStratConMapless)} and skipped facility
 * placement entirely, so the remaining operations are already inert there.
 *
 * <p>The method signatures mirror the corresponding static entry points on
 * {@link mekhq.campaign.digitalGM.stratCon.StratConRulesManager StratConRulesManager}; the default StratCon
 * implementation delegates to them, so the rules themselves are unchanged.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface IFacilityStrategy {

    /**
     * Applies the periodic (daily, and where relevant monthly) effects of the facilities on a track.
     *
     * @param track          the track whose facilities are processed
     * @param campaignState  the StratCon state for the contract
     * @param isStartOfMonth {@code true} on the first day of the month, when monthly effects also apply
     */
    void applyPeriodicEffects(StratConTrackState track, StratConCampaignState campaignState, boolean isStartOfMonth);

    /**
     * Updates the facility associated with a resolved scenario, destroying or capturing it as required.
     *
     * @param scenario the resolved scenario
     * @param contract the contract owning the facility
     * @param destroy  {@code true} if the facility should be destroyed
     * @param capture  {@code true} if the facility should change hands
     */
    void updateFacilityForScenario(AtBScenario scenario, AtBContract contract, boolean destroy, boolean capture);

    /**
     * Transfers ownership of a facility to the opposing side (or its captured definition).
     *
     * @param facility the facility whose owner changes
     */
    void switchFacilityOwner(StratConFacility facility);
}
