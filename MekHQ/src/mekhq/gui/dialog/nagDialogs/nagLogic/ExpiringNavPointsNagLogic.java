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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * Finds StratCon Nav Points (points of interest) that will expire when the day next advances without having been
 * dealt with.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ExpiringNavPointsNagLogic {
    final static String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

    /**
     * @param activeContracts the active contracts to check
     * @param today           the current campaign date
     *
     * @return {@code true} if any Nav Point the player can see will expire tomorrow
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean hasExpiringNavPoints(List<AbstractContract> activeContracts, LocalDate today) {
        return !determineExpiringNavPoints(activeContracts, today).isEmpty();
    }

    /**
     * Builds a report of every Nav Point, visible to the player, that will expire when the day advances. A Nav Point
     * expires on the new day once that day reaches its expiry date, so long as it is still active and not waiting on
     * a linked scenario.
     *
     * @param activeContracts the active contracts to check
     * @param today           the current campaign date
     *
     * @return an HTML report listing each expiring Nav Point, or an empty string if there are none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String determineExpiringNavPoints(List<AbstractContract> activeContracts, LocalDate today) {
        StringBuilder expiringNavPoints = new StringBuilder();
        LocalDate tomorrow = today.plusDays(1);

        for (AbstractContract contract : activeContracts) {
            if (contract.getStratConCampaignState() == null) {
                continue;
            }

            for (StratConTrackState track : contract.getStratConCampaignState().getTracks()) {
                for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest()) {
                    if (!isExpiringTomorrow(pointOfInterest, track, tomorrow)) {
                        continue;
                    }

                    String coordinates = (pointOfInterest.getCoords() == null) ?
                                               "" :
                                               pointOfInterest.getCoords().toBTString();

                    expiringNavPoints.append(getFormattedTextAt(RESOURCE_BUNDLE,
                          "ExpiringNavPointsNagDialog.report",
                          pointOfInterest.getDisplayableName(),
                          contract.getHyperlinkedName(),
                          track.getDisplayableName(),
                          coordinates));
                }
            }
        }

        return expiringNavPoints.toString();
    }

    private static boolean isExpiringTomorrow(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          LocalDate tomorrow) {
        return pointOfInterest.isActive() &&
                     !pointOfInterest.hasLinkedScenario() &&
                     pointOfInterest.hasReachedExpiryDate(tomorrow) &&
                     pointOfInterest.isVisibleToPlayer(track);
    }
}
