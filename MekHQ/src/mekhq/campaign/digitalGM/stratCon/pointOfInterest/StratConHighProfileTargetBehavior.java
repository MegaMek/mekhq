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
import mekhq.campaign.digitalGM.stratCon.StratConEscalation;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a high profile target: a conspicuous enemy asset whose attack draws the enemy's eye, placed in place
 * of every point of interest on a Diversionary Raid contract. High profile targets are not strategic objectives
 * themselves: a Diversionary Raid's objective is to raise its Escalation (see {@link StratConEscalation}), and striking
 * them is the quickest way to do it.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the target is
 * struck on the spot, raising the contract's Escalation by 3d6. If one does, the enemy was waiting, and the deploying
 * formation is ambushed in a {@value #SCENARIO_TEMPLATE} scenario; an ambushed target is spent whatever the ambush's
 * result, and leaves the map. Striking one pays the combat bonus, standing in for the Essential scenarios a
 * Diversionary Raid does not get (see {@link #isCombatBonusPaid}). (See
 * {@link StratConAmbushPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>A high profile target not struck in time is gone: it expires (its lifespan comes from its definition), except
 * while an ambush there is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConHighProfileTargetBehavior extends StratConAmbushPointOfInterestBehavior {
    /** The behavior ID the high profile target definition names. */
    public static final String BEHAVIOR_ID = "highProfileTarget";

    /** The type ID of the high profile target definition. */
    public static final String TYPE_ID = "HighProfileTarget";

    /** The scenario template the ambush at a high profile target is fought as. */
    static final String SCENARIO_TEMPLATE = "Decoy Engagement.json";

    /**
     * The ambush at a high profile target is fought as a {@value #SCENARIO_TEMPLATE} scenario.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConHighProfileTargetBehavior() {
        super(BEHAVIOR_ID, SCENARIO_TEMPLATE);
    }

    /**
     * Striking a high profile target raises the contract's Escalation by 3d6.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void onSecured(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          AbstractContract contract, Campaign campaign) {
        StratConEscalation.onHighProfileTargetStruck(campaign, contract);
    }
}
