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
 * The behavior of beleaguered forces: friendly troops under attack and calling for relief, placed in place of every
 * point of interest on a Relief Duty contract. Every group of beleaguered forces is a strategic objective, alongside
 * the contract's Essential scenarios rather than in place of them.
 *
 * <p>When any formation deploys onto their hex, the usual scenario roll is made. If no scenario breaks out, they are
 * relieved on the spot. If one does, a {@value #SCENARIO_TEMPLATE} scenario is fought to reach them: an overall
 * victory relieves them, and anything else - a defeat, a draw, or leaving the scenario unplayed - leaves them to be
 * overrun. Either way they then leave the map, and their objective is met or failed. Relieving them pays no combat
 * bonus: the contract's Essential scenarios still pay it. (See {@link StratConContestedPointOfInterestBehavior} for the
 * rules it shares.)</p>
 *
 * <p>Beleaguered forces not relieved in time are overrun: they expire and their objective fails (their lifespan comes
 * from their definition), except while a scenario to reach them is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConBeleagueredForcesBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the beleaguered forces definition names. */
    public static final String BEHAVIOR_ID = "beleagueredForces";

    /** The type ID of the beleaguered forces definition. */
    public static final String TYPE_ID = "BeleagueredForces";

    /** The scenario template fought to reach beleaguered forces. */
    static final String SCENARIO_TEMPLATE = "Relief Column.json";

    @Override
    protected String getScenarioTemplateName() {
        return SCENARIO_TEMPLATE;
    }

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConBeleagueredForcesBehavior";
    }

    /**
     * Any formation can relieve beleaguered forces.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean canFollowUp(int formationId, Campaign campaign) {
        return true;
    }

    /**
     * Relieving beleaguered forces pays no combat bonus: a Relief Duty contract keeps its Essential scenarios, which
     * still pay it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isCombatBonusPaid() {
        return false;
    }

    /**
     * With no scenario breaking out, the beleaguered forces are relieved at once.
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
