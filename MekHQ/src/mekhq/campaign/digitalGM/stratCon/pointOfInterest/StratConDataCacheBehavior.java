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
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a data cache: intelligence the player must recover, placed in place of every point of interest on an
 * Espionage contract. Every data cache is a strategic objective.
 *
 * <p>When a ground formation deploys onto the cache's hex, the usual scenario roll is made. If no scenario breaks out,
 * the cache is secured on the spot. If one does, it is a {@value #SCENARIO_TEMPLATE} scenario fought over the cache:
 * an overall victory secures it, and anything else - a defeat, a draw, or leaving the scenario unplayed - loses it.
 * Either way the cache then leaves the map, and its objective is met or failed. Securing a cache pays the contract's
 * combat bonus, standing in for the Essential scenarios such a contract does not get. (See
 * {@link StratConContestedPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>A cache that is not recovered in time expires and is lost (its lifespan comes from its definition), except while
 * a scenario over it is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConDataCacheBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the data cache definition names. */
    public static final String BEHAVIOR_ID = "dataCache";

    /** The type ID of the data cache definition. */
    public static final String TYPE_ID = "DataCache";

    /** The scenario template fought over a contested data cache. */
    static final String SCENARIO_TEMPLATE = "Recon Evasion.json";

    @Override
    protected String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConDataCacheBehavior";
    }

    /**
     * With no scenario breaking out, the cache is secured at once.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onNoScenario(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        securePointOfInterest(pointOfInterest, track, contract, campaign);
    }
}
