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

import mekhq.campaign.digitalGM.IFacilityStrategy;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;

/**
 * Default (map-based) StratCon implementation of {@link IFacilityStrategy}. Every method delegates to the existing
 * facility logic on {@link StratConRulesManager}, so behaviour is unchanged from the legacy engine.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConFacilityStrategy implements IFacilityStrategy {

    @Override
    public void applyPeriodicEffects(StratConTrackState track, StratConCampaignState campaignState,
          boolean isStartOfMonth) {
        StratConRulesManager.processFacilityEffects(track, campaignState, isStartOfMonth);
    }

    @Override
    public void updateFacilityForScenario(AtBScenario scenario, AtBContract contract, boolean destroy,
          boolean capture) {
        StratConRulesManager.updateFacilityForScenario(scenario, contract, destroy, capture);
    }

    @Override
    public void switchFacilityOwner(StratConFacility facility) {
        StratConRulesManager.switchFacilityOwner(facility);
    }
}
