/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon;

import megamek.common.annotations.Nullable;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;

/**
 * This class is a data structure storing data relating to StratCon strategic objectives and also handles some small
 * amount of "business logic"
 */
public class StratConStrategicObjective {
    public static final int OBJECTIVE_FAILED = -1;

    private StratConCoords objectiveCoords;
    private StrategicObjectiveType objectiveType;
    private int currentObjectiveCount;
    private int desiredObjectiveCount;

    // Point of interest objectives follow their point of interest by ID rather than by hex: several points of interest
    // can share a hex (and a facility), and one can be moved, so these objectives leave objectiveCoords unset.
    private String pointOfInterestId;

    public StratConCoords getObjectiveCoords() {
        return objectiveCoords;
    }

    public void setObjectiveCoords(StratConCoords objectiveCoords) {
        this.objectiveCoords = objectiveCoords;
    }

    public StrategicObjectiveType getObjectiveType() {
        return objectiveType;
    }

    public void setObjectiveType(StrategicObjectiveType objectiveType) {
        this.objectiveType = objectiveType;
    }

    public int getCurrentObjectiveCount() {
        return currentObjectiveCount;
    }

    public void setCurrentObjectiveCount(int currentObjectiveCount) {
        this.currentObjectiveCount = currentObjectiveCount;
    }

    public void incrementCurrentObjectiveCount() {
        currentObjectiveCount++;
    }

    /**
     * @return the ID of the point of interest a {@link StrategicObjectiveType#PointOfInterest} objective is tied to, or
     *       {@code null} for any other kind of objective
     */
    public @Nullable String getPointOfInterestId() {
        return pointOfInterestId;
    }

    public void setPointOfInterestId(@Nullable String pointOfInterestId) {
        this.pointOfInterestId = pointOfInterestId;
    }

    public int getDesiredObjectiveCount() {
        return desiredObjectiveCount;
    }

    public void setDesiredObjectiveCount(int desiredObjectiveCount) {
        this.desiredObjectiveCount = desiredObjectiveCount;
    }

    public boolean isObjectiveFailed(StratConTrackState trackState) {
        return switch (getObjectiveType()) {
            case AnyScenarioVictory, SpecificScenarioVictory, Escalation ->
                // you can fail this if the scenario goes away somehow
                  getCurrentObjectiveCount() == OBJECTIVE_FAILED;
            case AlliedFacilityControl, HostileFacilityControl -> {
                // you can fail this by having the facility destroyed
                StratConFacility alliedFacility = trackState.getFacility(getObjectiveCoords());
                yield alliedFacility == null;
            }
            case FacilityDestruction ->
                // you can't really permanently fail this
                  false;
            case PointOfInterest -> isPointOfInterestObjectiveFailed(trackState);
            default ->
                // we shouldn't be here, but just in case
                  false;
        };
    }

    /**
     * Given the track that this objective is on, is it complete?
     */
    public boolean isObjectiveCompleted(StratConTrackState trackState) {
        return switch (getObjectiveType()) {
            case AnyScenarioVictory, SpecificScenarioVictory, Escalation ->
                // this is set once qualifying scenarios are completed (or, for Escalation, follows the contract's)
                  getCurrentObjectiveCount() >= getDesiredObjectiveCount();
            case AlliedFacilityControl -> {
                // this is "ok" if the facility exists and is under allied control
                StratConFacility alliedFacility = trackState.getFacility(getObjectiveCoords());
                yield (alliedFacility != null) && (alliedFacility.getOwner() == ForceAlignment.Allied);
            }
            case HostileFacilityControl, FacilityDestruction -> {
                // these are "ok" if the facility no longer exists or is under allied control
                // we assume that we can slag a facility at any time if we control it
                StratConFacility hostileFacility = trackState.getFacility(getObjectiveCoords());
                yield (hostileFacility == null) || (hostileFacility.getOwner() == ForceAlignment.Allied);
            }
            case PointOfInterest -> isPointOfInterestObjectiveCompleted(trackState);
            default ->
                // we shouldn't be here, but just in case
                  false;
        };
    }

    /**
     * A point of interest objective is met - unless it has been marked failed - once it has been latched as met (see
     * {@link mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestRules#resolvePointOfInterest}),
     * or while its point of interest is on the map and that point of interest's behavior says it is met.
     *
     * <p>The latch is what lets a type remove its point of interest from the map once it has been dealt with, without
     * the objective then reading as failed.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    private boolean isPointOfInterestObjectiveCompleted(StratConTrackState trackState) {
        // An objective explicitly marked failed stays failed, whatever becomes of its point of interest afterward.
        if (getCurrentObjectiveCount() == OBJECTIVE_FAILED) {
            return false;
        }

        if ((getDesiredObjectiveCount() > 0) && (getCurrentObjectiveCount() >= getDesiredObjectiveCount())) {
            return true;
        }

        StratConPointOfInterest pointOfInterest = getPointOfInterest(trackState);
        return (pointOfInterest != null) &&
                     pointOfInterest.getBehavior().isObjectiveCompleted(pointOfInterest, trackState);
    }

    /**
     * A point of interest objective fails when it has not been met and either its point of interest is gone from the
     * map, or that point of interest's behavior says it has failed (by default, once it expires).
     *
     * @author Illiani
     * @since 0.51.01
     */
    private boolean isPointOfInterestObjectiveFailed(StratConTrackState trackState) {
        if (getCurrentObjectiveCount() == OBJECTIVE_FAILED) {
            return true;
        }

        if (isPointOfInterestObjectiveCompleted(trackState)) {
            return false;
        }

        StratConPointOfInterest pointOfInterest = getPointOfInterest(trackState);
        return (pointOfInterest == null) ||
                     pointOfInterest.getBehavior().isObjectiveFailed(pointOfInterest, trackState);
    }

    /**
     * @param trackState the sector this objective belongs to
     *
     * @return the point of interest this objective is tied to, or {@code null} if it has none or it is gone
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable StratConPointOfInterest getPointOfInterest(StratConTrackState trackState) {
        return (pointOfInterestId == null) ? null : trackState.getPointOfInterest(pointOfInterestId);
    }

    /**
     * Determines whether a StratCon objective has been resolved (either completed or failed).
     *
     * <p>An objective is considered resolved if it has reached a terminal state, meaning it is no longer active and
     * requires no further player action.</p>
     *
     * @param trackState the current state of the StratCon track containing the objective
     *
     * @return {@code true} if the objective is completed or failed, {@code false} if it is still active
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean isObjectiveResolved(StratConTrackState trackState) {
        return isObjectiveCompleted(trackState) || isObjectiveFailed(trackState);
    }
}
