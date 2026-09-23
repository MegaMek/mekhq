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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.randomEvents.other.RiotScenario;

/**
 * The pieces of the riots that break out at points of interest - a Scheduled Parade disrupted, or Civil Disobedience
 * turned violent - fought as "Crowd Control" scenarios, the way riots have always been (see {@link RiotScenario}). The
 * scenario itself is placed as any point of interest's is (see
 * {@link AbstractStratConRolledPointOfInterestBehavior}); these add what makes it a riot.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class StratConRiots {
    /** The scenario template a riot is fought as. */
    static final String SCENARIO_TEMPLATE = "Crowd Control.json";

    /** A riot breaks out at once, rather than after the sector's usual deployment time. */
    static final int DAYS_UNTIL_DEPLOYMENT = 0;

    private StratConRiots() {
    }

    /**
     * Adds a riot's civilian mobs to its "Civilians" force, which exists once the scenario has been finalized.
     *
     * @param riot     the finalized riot
     * @param contract the contract the riot belongs to
     * @param campaign the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void addRiotingMobs(StratConScenario riot, AbstractContract contract, Campaign campaign) {
        RiotScenario.addRiotingMobs(campaign, contract.getEnemyFaction(), riot.getBackingScenario());
    }

    /**
     * Announces a riot to the player, as riots have always been announced (see {@link RiotScenario#reportRiot}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void announceRiot(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        RiotScenario.reportRiot(campaign, contract, track, pointOfInterest.getCoords());
    }
}
