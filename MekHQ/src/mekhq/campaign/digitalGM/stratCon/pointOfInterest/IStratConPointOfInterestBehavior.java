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
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;

/**
 * The rules that make one type of StratCon point of interest behave differently from another. A definition names its
 * behavior by ID (see {@link StratConPointOfInterestDefinition#getBehaviorId()}), and the behavior is looked up in
 * {@link StratConPointOfInterestBehaviors}.
 *
 * <p>Behaviors are code, never saved: a saved point of interest records only its type ID, so a new type needs no
 * change to how campaigns are saved. Every hook has a default, so an implementation overrides only what its type
 * cares about.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface IStratConPointOfInterestBehavior {
    /**
     * Called when a player force scouts the point of interest's hex for the first time.
     *
     * @param pointOfInterest the point of interest just revealed
     * @param track           the sector it sits in
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    default void onRevealed(StratConPointOfInterest pointOfInterest, StratConTrackState track, Campaign campaign) {
    }

    /**
     * Called when a player formation deploys onto the point of interest's hex from the map, after the hex has been
     * scouted and before any scenario is looked for or rolled. Only called while the point of interest is active, and
     * not when a formation is committed to a scenario that already exists.
     *
     * <p>The returned outcome shapes the usual random scenario roll. To replace that roll with a scenario of the
     * type's own, create the scenario on this hex here and return
     * {@link PointOfInterestDeploymentOutcome#SUPPRESS_SCENARIO}: the deploying formation is then assigned to it like
     * any scenario found on the hex.</p>
     *
     * @param pointOfInterest the point of interest on the hex
     * @param track           the sector it sits in
     * @param formationId     the ID of the deploying formation
     * @param campaign        the current campaign
     *
     * @return what this point of interest does to the random scenario roll; by default, nothing
     *
     * @author Illiani
     * @since 0.51.01
     */
    default PointOfInterestDeploymentOutcome onFormationDeployed(StratConPointOfInterest pointOfInterest,
          StratConTrackState track, int formationId, Campaign campaign) {
        return PointOfInterestDeploymentOutcome.NO_EFFECT;
    }

    /**
     * Called once per day for every active point of interest, after any whose expiry date has come have expired. A
     * point of interest waiting on a linked scenario does not expire, but still gets this hook.
     *
     * @param pointOfInterest the point of interest
     * @param track           the sector it sits in
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    default void onNewDay(StratConPointOfInterest pointOfInterest, StratConTrackState track, Campaign campaign) {
    }

    /**
     * Called when the point of interest reaches its expiry date, before it is marked as expired or removed. A behavior
     * may still resolve or remove it here, in which case it is neither marked as expired nor removed.
     *
     * @param pointOfInterest the point of interest expiring
     * @param track           the sector it sits in
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    default void onExpired(StratConPointOfInterest pointOfInterest, StratConTrackState track, Campaign campaign) {
    }

    /**
     * Called when the scenario this point of interest's fate is waiting on (see
     * {@link StratConPointOfInterestRules#linkScenario}) ends. The link is cleared before this is called.
     *
     * <p>A scenario that is ignored, or removed from the map without being played, ends in defeat - noticed on the
     * next day's point of interest step.</p>
     *
     * @param pointOfInterest the point of interest the scenario was linked to
     * @param track           the sector it sits in
     * @param isVictory       {@code true} if the scenario was an overall victory for the player; a draw is not
     * @param campaign        the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    default void onLinkedScenarioEnded(StratConPointOfInterest pointOfInterest, StratConTrackState track,
          boolean isVictory, Campaign campaign) {
    }

    /**
     * How much this point of interest widens the scan range of every player force scouting its sector, the way a
     * sensor facility does. By default, its definition's
     * {@link StratConPointOfInterestDefinition#getScanRangeIncrease() scan range increase}, but only while it is
     * active and held by the player or an ally - a sensor you do not hold scouts for someone else.
     *
     * @param pointOfInterest the point of interest
     * @param track           the sector it sits in
     *
     * @return the number of hexes to add to the sector's scan range
     *
     * @author Illiani
     * @since 0.51.01
     */
    default int getScanRangeIncrease(StratConPointOfInterest pointOfInterest, StratConTrackState track) {
        StratConPointOfInterestDefinition definition = pointOfInterest.getDefinition();
        if ((definition == null) || !pointOfInterest.isActive() || !pointOfInterest.isOwnerAlliedToPlayer()) {
            return 0;
        }

        return definition.getScanRangeIncrease();
    }

    /**
     * How much this point of interest shifts the odds of a scenario breaking out when a force deploys anywhere in its
     * sector. By default, its definition's {@link StratConPointOfInterestDefinition#getScenarioOddsModifier() scenario
     * odds modifier} while it is active, whoever holds it: it describes the ground, not the side.
     *
     * @param pointOfInterest the point of interest
     * @param track           the sector it sits in
     *
     * @return the modifier to add to the sector's scenario odds
     *
     * @author Illiani
     * @since 0.51.01
     */
    default int getScenarioOddsModifier(StratConPointOfInterest pointOfInterest, StratConTrackState track) {
        StratConPointOfInterestDefinition definition = pointOfInterest.getDefinition();
        if ((definition == null) || !pointOfInterest.isActive()) {
            return 0;
        }

        return definition.getScenarioOddsModifier();
    }

    /**
     * Decides whether a strategic objective tied to this point of interest has been met. By default, it is met once
     * the point of interest has been resolved.
     *
     * @param pointOfInterest the point of interest the objective is tied to
     * @param track           the sector it sits in
     *
     * @return {@code true} if the objective has been met
     *
     * @author Illiani
     * @since 0.51.01
     */
    default boolean isObjectiveCompleted(StratConPointOfInterest pointOfInterest, StratConTrackState track) {
        return pointOfInterest.getStatus() == PointOfInterestStatus.RESOLVED;
    }

    /**
     * Decides whether a strategic objective tied to this point of interest can no longer be met. By default, it fails
     * once the point of interest has expired unresolved. (An objective whose point of interest has been removed from
     * the map entirely is failed by the objective itself.)
     *
     * @param pointOfInterest the point of interest the objective is tied to
     * @param track           the sector it sits in
     *
     * @return {@code true} if the objective has failed
     *
     * @author Illiani
     * @since 0.51.01
     */
    default boolean isObjectiveFailed(StratConPointOfInterest pointOfInterest, StratConTrackState track) {
        return pointOfInterest.getStatus() == PointOfInterestStatus.EXPIRED;
    }

    /**
     * Describes what the player must do to meet a strategic objective tied to this point of interest, for the
     * objectives list - for example "Recover the flight recorder from the crash site". Shown only once the player can
     * see the point of interest; until then the list shows a generic "locate a point of interest".
     *
     * @param pointOfInterest the point of interest the objective is tied to
     * @param track           the sector it sits in
     *
     * @return the objective text, or {@code null} for the generic "investigate [name]"
     *
     * @author Illiani
     * @since 0.51.01
     */
    default @Nullable String getObjectiveDescription(StratConPointOfInterest pointOfInterest,
          StratConTrackState track) {
        return null;
    }

    /**
     * @param pointOfInterest the point of interest being described
     * @param track           the sector it sits in
     *
     * @return extra HTML-safe text for the selected-hex info panel, or {@code null} for none
     *
     * @author Illiani
     * @since 0.51.01
     */
    default @Nullable String getAdditionalInfo(StratConPointOfInterest pointOfInterest, StratConTrackState track) {
        return null;
    }
}
