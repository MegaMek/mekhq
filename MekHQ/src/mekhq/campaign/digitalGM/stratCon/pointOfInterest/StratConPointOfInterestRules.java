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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;

/**
 * The StratCon rules that act on points of interest during play: the points where the rest of StratCon hands control
 * to a point of interest's {@link IStratConPointOfInterestBehavior}.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConPointOfInterestRules {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConRulesManager";

    private StratConPointOfInterestRules() {
    }

    /**
     * Marks a point of interest as resolved - dealt with by the player - and records every strategic objective tied to
     * it as met. Recording the objectives means they stay met even if the point of interest is removed from the map
     * afterward, so a type may clear away a point of interest once it has been dealt with.
     *
     * <p>Behaviors should resolve points of interest through this rather than by setting their status directly.</p>
     *
     * @param track           the sector the point of interest sits in
     * @param pointOfInterest the point of interest to resolve
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void resolvePointOfInterest(StratConTrackState track, StratConPointOfInterest pointOfInterest) {
        pointOfInterest.setStatus(PointOfInterestStatus.RESOLVED);

        for (StratConStrategicObjective objective : getStrategicObjectives(track, pointOfInterest)) {
            objective.setCurrentObjectiveCount(objective.getDesiredObjectiveCount());
        }
    }

    /**
     * @param track           the sector the point of interest sits in
     * @param pointOfInterest the point of interest
     *
     * @return the strategic objectives of the sector tied to that point of interest; usually none or one
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<StratConStrategicObjective> getStrategicObjectives(StratConTrackState track,
          StratConPointOfInterest pointOfInterest) {
        List<StratConStrategicObjective> objectives = new ArrayList<>();
        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            if ((objective.getObjectiveType() == StrategicObjectiveType.PointOfInterest) &&
                      pointOfInterest.getId().equals(objective.getPointOfInterestId())) {
                objectives.add(objective);
            }
        }
        return objectives;
    }

    /**
     * Runs the daily point of interest step for one sector. Every active point of interest whose expiry date has come
     * expires (see {@link #expirePointOfInterest}); every other active one gets its
     * {@link IStratConPointOfInterestBehavior#onNewDay} hook. Resolved and expired points of interest are left alone.
     *
     * @param track    the sector to process
     * @param campaign the current campaign, whose date is today
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processNewDay(StratConTrackState track, Campaign campaign) {
        LocalDate today = campaign.getLocalDate();

        // Copied, so a hook that adds, moves, or removes points of interest is safe.
        for (StratConPointOfInterest pointOfInterest : new ArrayList<>(track.getPointsOfInterest())) {
            // A hook earlier in this pass may have removed it, or ended it some other way.
            if ((track.getPointOfInterest(pointOfInterest.getId()) == null) || !pointOfInterest.isActive()) {
                continue;
            }

            if (pointOfInterest.hasReachedExpiryDate(today)) {
                expirePointOfInterest(track, pointOfInterest, campaign);
            } else {
                pointOfInterest.getBehavior().onNewDay(pointOfInterest, track, campaign);
            }
        }
    }

    /**
     * Expires a point of interest: calls its {@link IStratConPointOfInterestBehavior#onExpired} hook, then - unless the
     * hook resolved or removed it - marks it as expired, and removes it from the map if its definition says so. If the
     * player could see it, the daily report says it has gone.
     *
     * @param track           the sector it sits in
     * @param pointOfInterest the point of interest expiring
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void expirePointOfInterest(StratConTrackState track, StratConPointOfInterest pointOfInterest,
          Campaign campaign) {
        // Decided before anything changes: once it is removed, it can no longer tell whether the player saw it.
        boolean visibleToPlayer = pointOfInterest.isVisibleToPlayer(track);

        pointOfInterest.getBehavior().onExpired(pointOfInterest, track, campaign);

        if ((track.getPointOfInterest(pointOfInterest.getId()) == null) || !pointOfInterest.isActive()) {
            return;
        }

        pointOfInterest.setStatus(PointOfInterestStatus.EXPIRED);

        StratConPointOfInterestDefinition definition = pointOfInterest.getDefinition();
        if ((definition != null) && definition.isRemoveOnExpiry()) {
            track.removePointOfInterest(pointOfInterest.getId());
        }

        if (visibleToPlayer) {
            campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "StratConPointOfInterestRules.expired.report",
                  pointOfInterest.getDisplayableName(),
                  track.getDisplayableName()));
        }
    }

    /**
     * Tells every active point of interest on a hex that a player formation has just deployed onto it (see
     * {@link IStratConPointOfInterestBehavior#onFormationDeployed}), and combines what they want done to the random
     * scenario roll.
     *
     * @param track       the sector deployed into
     * @param coords      the hex deployed onto
     * @param formationId the ID of the deploying formation
     * @param campaign    the current campaign
     *
     * @return the combined outcome (see {@link PointOfInterestDeploymentOutcome#combineWith}); no effect if the hex has
     *       no active point of interest
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static PointOfInterestDeploymentOutcome processFormationDeployment(StratConTrackState track,
          StratConCoords coords, int formationId, Campaign campaign) {
        PointOfInterestDeploymentOutcome combinedOutcome = PointOfInterestDeploymentOutcome.NO_EFFECT;

        // Copied, so a hook that removes its point of interest or places a scenario on the hex is safe.
        for (StratConPointOfInterest pointOfInterest : new ArrayList<>(track.getPointsOfInterest(coords))) {
            if (!pointOfInterest.isActive()) {
                continue;
            }

            PointOfInterestDeploymentOutcome outcome = pointOfInterest.getBehavior()
                                                             .onFormationDeployed(pointOfInterest,
                                                                   track,
                                                                   formationId,
                                                                   campaign);
            combinedOutcome = combinedOutcome.combineWith(outcome);
        }

        return combinedOutcome;
    }

    /**
     * Reveals every point of interest on the given hex that has not been revealed yet, and calls each one's
     * {@link IStratConPointOfInterestBehavior#onRevealed} hook. A point of interest already revealed is left alone, so
     * the hook fires once per point of interest however often its hex is scouted.
     *
     * <p>Call this whenever a player force scouts a hex.</p>
     *
     * @param track    the sector being scouted
     * @param coords   the hex being scouted
     * @param campaign the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void revealPointsOfInterest(StratConTrackState track, StratConCoords coords, Campaign campaign) {
        // Copied, so a behavior that removes or moves its point of interest from inside the hook is safe.
        List<StratConPointOfInterest> pointsOfInterestOnHex = new ArrayList<>(track.getPointsOfInterest(coords));

        for (StratConPointOfInterest pointOfInterest : pointsOfInterestOnHex) {
            if (!pointOfInterest.isRevealed()) {
                pointOfInterest.setRevealed(true);
                pointOfInterest.getBehavior().onRevealed(pointOfInterest, track, campaign);
            }
        }
    }

    /**
     * Hides every point of interest in the sector again, without calling any hooks. Used when a GM resets the
     * sector's fog of war to re-test scouting, so points of interest can be found (and their hooks fire) a second time.
     *
     * @param track the sector whose points of interest to hide
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void hideAllPointsOfInterest(StratConTrackState track) {
        for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest()) {
            pointOfInterest.setRevealed(false);
        }
    }
}
