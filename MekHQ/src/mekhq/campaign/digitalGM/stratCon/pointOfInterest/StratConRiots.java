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

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConScenarioFactory;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.randomEvents.other.RiotScenario;

/**
 * Sets up the riots that break out at points of interest - a Scheduled Parade disrupted, or Civil Disobedience turned
 * violent - as "Crowd Control" scenarios, the way riots have always been set up (see {@link RiotScenario}).
 *
 * @author Illiani
 * @since 0.51.01
 */
final class StratConRiots {
    /** The scenario template a riot is fought as. */
    static final String SCENARIO_TEMPLATE = "Crowd Control.json";

    private StratConRiots() {
    }

    /**
     * Places a riot on a point of interest's hex and links the point of interest to it. The scenario is generated and
     * finalized with the deploying formation present, so the opposition is sized against it; the riot's civilian mobs
     * are then added to its "Civilians" force. The formation is then handed back, to be assigned to the riot like any
     * scenario found on the hex.
     *
     * @param pointOfInterest the point of interest the riot breaks out at
     * @param track           the sector it sits in
     * @param formationId     the ID of the deploying formation
     * @param contract        the contract whose map holds the sector
     * @param campaign        the current campaign
     *
     * @return the riot, or {@code null} if none could be generated
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable StratConScenario placeRiot(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          int formationId, AbstractContract contract, Campaign campaign) {
        // A missing template is logged by the factory. Without it there is no riot to fight.
        ScenarioTemplate template = StratConScenarioFactory.getSpecificScenario(SCENARIO_TEMPLATE);
        if (template == null) {
            return null;
        }

        // Facilities are ignored: these points of interest occupy their hex, so none can share it. The riot breaks out
        // at once.
        StratConScenario riot = StratConRulesManager.setupScenario(pointOfInterest.getCoords(),
              formationId,
              campaign,
              contract,
              track,
              template,
              true,
              0);
        if (riot == null) {
            return null;
        }

        // Finalized as a riot's scenario always has been, which also generates its "Civilians" force for the mobs.
        StratConRulesManager.finalizeBackingScenario(campaign, contract, track, false, riot);
        RiotScenario.addRiotingMobs(campaign, contract.getEnemyFaction(), riot.getBackingScenario());

        // Finalizing without auto-assignment has already taken the formation out of the backing scenario; clear it from
        // the primary forces too, so that assigning it to the riot on the hex does not list it twice.
        riot.getPrimaryForceIDs().remove(Integer.valueOf(formationId));

        StratConPointOfInterestRules.linkScenario(pointOfInterest, riot);
        return riot;
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
