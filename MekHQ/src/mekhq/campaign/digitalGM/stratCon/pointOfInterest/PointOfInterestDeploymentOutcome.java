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

/**
 * What a point of interest does to the random scenario roll when a player formation deploys onto its hex (see
 * {@link IStratConPointOfInterestBehavior#onFormationDeployed}).
 *
 * <p>When several points of interest share the hex, their outcomes are combined with {@link #combineWith}:
 * suppressing beats forcing, and forcing beats no effect.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum PointOfInterestDeploymentOutcome {
    /** The usual random scenario roll goes ahead unchanged. */
    NO_EFFECT(0),

    /**
     * The roll is certain to spawn a scenario. The usual limits still apply: nothing spawns under the "Essential
     * Scenarios Only" option, on a facility's hex (where the facility's own rules decide), or against a routed enemy.
     */
    FORCE_SCENARIO(1),

    /** No random scenario spawns from this deployment. */
    SUPPRESS_SCENARIO(2);

    // Which outcome wins when several points of interest share a hex: the higher priority. Kept explicit so that
    // reordering or adding constants cannot quietly change the rule.
    private final int priority;

    PointOfInterestDeploymentOutcome(int priority) {
        this.priority = priority;
    }

    /**
     * @param other another point of interest's outcome for the same deployment; {@code null} is treated as no outcome
     *
     * @return the outcome that wins: {@link #SUPPRESS_SCENARIO} over {@link #FORCE_SCENARIO} over {@link #NO_EFFECT}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public PointOfInterestDeploymentOutcome combineWith(@Nullable PointOfInterestDeploymentOutcome other) {
        if ((other == null) || (priority >= other.priority)) {
            return this;
        }

        return other;
    }
}
