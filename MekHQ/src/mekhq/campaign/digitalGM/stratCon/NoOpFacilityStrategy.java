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
 * {@link IFacilityStrategy} for play types with no facility map (Mapless and Singles). Every facility operation is a
 * deliberate no-op: Mapless/Singles skip facility placement entirely (see {@link StratConContractInitializer}), so
 * there are never any facilities to affect. The periodic no-op also mirrors the legacy engine's
 * {@code if (!isUseStratConMapless)} guard.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class NoOpFacilityStrategy implements IFacilityStrategy {

    @Override
    public void applyPeriodicEffects(StratConTrackState track, StratConCampaignState campaignState,
          boolean isStartOfMonth) {
        // Intentionally empty: Mapless and Singles play have no facility map.
    }

    @Override
    public void updateFacilityForScenario(AtBScenario scenario, AtBContract contract, boolean destroy,
          boolean capture) {
        // Intentionally empty: Mapless and Singles play have no facilities to update.
    }

    @Override
    public void switchFacilityOwner(StratConFacility facility) {
        // Intentionally empty: Mapless and Singles play have no facilities to transfer.
    }
}
