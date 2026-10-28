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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConDeploymentOrders;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;

/**
 * A Deployment Order: a position the employer has ordered the player to move a formation to, issued more or less often
 * depending on the contract's command rights and how controlling the employer is (see
 * {@link StratConDeploymentOrders}). Each is a strategic objective, so letting one lapse costs the player a Complete
 * Success.
 *
 * <p>Any formation deploying onto its hex carries out the order. Unlike most points of interest, the order does not
 * stand in for the usual scenario roll: the formation has gone where it was told, and whatever it finds there is rolled
 * for as normal.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConDeploymentOrderBehavior implements IStratConPointOfInterestBehavior {
    public static final String BEHAVIOR_ID = "deploymentOrder";

    public static final String TYPE_ID = "DeploymentOrder";

    /**
     * @param typeId a point of interest type ID
     *
     * @return {@code true} if the type is a Deployment Order
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isDeploymentOrder(@Nullable String typeId) {
        return TYPE_ID.equals(typeId);
    }

    @Override
    public PointOfInterestDeploymentOutcome onFormationDeployed(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, Campaign campaign) {
        StratConPointOfInterestRules.resolvePointOfInterest(track, pointOfInterest);
        track.removePointOfInterest(pointOfInterest.getId());

        StratConPointOfInterestRules.reportToPlayer(campaign, GENERAL, getFormattedTextAt(
              StratConPointOfInterestRules.RESOURCE_BUNDLE,
              BEHAVIOR_ID + ".completed.report",
              pointOfInterest.getDisplayableName(),
              track.getDisplayableName()));

        return PointOfInterestDeploymentOutcome.NO_EFFECT;
    }

    @Override
    public @Nullable String getObjectiveDescription(StratConPointOfInterest pointOfInterest,
          StratConTrackState track) {
        return getTextAt(StratConPointOfInterestRules.RESOURCE_BUNDLE, BEHAVIOR_ID + ".objective");
    }
}
