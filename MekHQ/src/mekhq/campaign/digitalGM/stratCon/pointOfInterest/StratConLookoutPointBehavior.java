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

/**
 * The behavior of a lookout point: a vantage point from which the player can observe enemy movements, placed in place
 * of every point of interest on an Observation Raid contract. Every lookout point is a strategic objective.
 *
 * <p>When any formation deploys onto its hex, the usual scenario roll is made. If no scenario breaks out, the
 * observation is made: its objective is met and the contract's combat bonus is paid, standing in for the Essential
 * scenarios such a contract does not get. If one does, the deploying formation is ambushed. An ambushed lookout point
 * is compromised whatever the ambush's result: it leaves the map and its objective is removed, neither met nor failed.
 * (See {@link StratConAmbushPointOfInterestBehavior} for the rules it shares.)</p>
 *
 * <p>A lookout point that is not used in time expires and its objective fails (its lifespan comes from its
 * definition), except while an ambush there is still to be fought.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConLookoutPointBehavior extends StratConAmbushPointOfInterestBehavior {
    /** The behavior ID the lookout point definition names. */
    public static final String BEHAVIOR_ID = "lookoutPoint";

    /** The type ID of the lookout point definition. */
    public static final String TYPE_ID = "LookoutPoint";

    @Override
    protected String getResourceKeyPrefix() {
        return "StratConLookoutPointBehavior";
    }
}
