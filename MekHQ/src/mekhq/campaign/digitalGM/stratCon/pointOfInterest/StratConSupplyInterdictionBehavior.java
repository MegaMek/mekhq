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

import static mekhq.campaign.enums.DailyReportType.GENERAL;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * The behavior of a supply interdiction: an enemy raiding party lying in wait across one of the player's supply routes
 * (see {@code StratConFacilitySupply}), sent out by {@code StratConEnemyFacilityActivity}.
 *
 * <p>While it is on the map it blocks the route through its hex, so every facility of the player's side beyond it is
 * cut off. Deploying a formation onto its hex makes the usual scenario roll. With no scenario, the raiders scatter.
 * With one, the formation is ambushed: winning breaks them up, and anything else leaves them holding the route until
 * they leave of their own accord. Either way, once they are gone the route is open again; they leave no lasting cut.</p>
 *
 * <p>Breaking them up pays no combat bonus.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConSupplyInterdictionBehavior extends StratConContestedPointOfInterestBehavior {
    /** The behavior ID the supply interdiction definition names. */
    public static final String BEHAVIOR_ID = "supplyInterdiction";

    /** The type ID of the supply interdiction definition. */
    public static final String TYPE_ID = "SupplyInterdiction";

    /**
     * The raiders ambush the deploying formation in a template suited to its unit type.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConSupplyInterdictionBehavior() {
        super(BEHAVIOR_ID, null, NoScenarioOutcome.SECURE);
    }

    @Override
    protected boolean isCombatBonusPaid(AbstractContract contract) {
        return false;
    }

    /**
     * The raiders are always an ambush: a Crisis, and never a Turning Point.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected boolean isScenarioAnAmbush() {
        return true;
    }

    /**
     * Winning breaks the raiders up; anything else leaves them where they are, still holding the route, until they
     * leave.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
        if (isVictory) {
            super.onLinkedScenarioEnded(pointOfInterest, track, true, campaign);
            return;
        }

        addReport(GENERAL, "held.report", pointOfInterest, track, campaign);
    }

    /**
     * @param track  a sector
     * @param coords a hex in it
     *
     * @return {@code true} if a supply interdiction still in play sits on the hex, blocking the player's supply through
     *       it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isInterdicted(StratConTrackState track, StratConCoords coords) {
        for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest(coords)) {
            if (pointOfInterest.isActive() && TYPE_ID.equals(pointOfInterest.getTypeId())) {
                return true;
            }
        }
        return false;
    }
}
