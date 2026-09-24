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

import static java.lang.Math.min;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState.LocalDateAdapter;

/**
 * A point of interest a contract will place later: the day it appears, its type, and whether it is a strategic
 * objective. Rolled when the contract is accepted and drained by the daily StratCon lifecycle (see
 * {@link mekhq.campaign.digitalGM.stratCon.StratConCampaignState#getScheduledPointsOfInterest()}), the same way
 * strategic-objective scenarios are.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConScheduledPointOfInterest {
    /**
     * How many days past its day a point of interest that no sector has room for is still tried, before it is dropped.
     */
    public static final int MAXIMUM_PLACEMENT_DELAY_DAYS = 30;

    private LocalDate spawnDate;
    private String typeId;
    private boolean strategicObjective;
    // state copied onto the point of interest when it is placed, for a type that is told something at scheduling time
    private Map<String, String> initialState = new HashMap<>();

    /** Used when loading saved campaigns. */
    public StratConScheduledPointOfInterest() {
    }

    /**
     * @param spawnDate          the day the point of interest appears
     * @param typeId             the type ID of its definition
     * @param strategicObjective whether it is also made a strategic objective of the sector it lands in
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConScheduledPointOfInterest(LocalDate spawnDate, String typeId, boolean strategicObjective) {
        this.spawnDate = spawnDate;
        this.typeId = typeId;
        this.strategicObjective = strategicObjective;
    }

    /**
     * @return the day the point of interest appears
     */
    @XmlJavaTypeAdapter(value = LocalDateAdapter.class)
    public LocalDate getSpawnDate() {
        return spawnDate;
    }

    public void setSpawnDate(LocalDate spawnDate) {
        this.spawnDate = spawnDate;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    /**
     * @return whether the point of interest is also made a strategic objective of the sector it lands in
     */
    public boolean isStrategicObjective() {
        return strategicObjective;
    }

    public void setStrategicObjective(boolean strategicObjective) {
        this.strategicObjective = strategicObjective;
    }

    /**
     * @return the state the point of interest starts with when it is placed (see
     *       {@link StratConPointOfInterest#getState()}); empty for most types
     */
    public Map<String, String> getInitialState() {
        return initialState;
    }

    public void setInitialState(Map<String, String> initialState) {
        this.initialState = (initialState == null) ? new HashMap<>() : initialState;
    }

    /**
     * @param today the current campaign date
     *
     * @return {@code true} if the point of interest's day has come - today or earlier, so a skipped day still catches
     *       up
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isDue(LocalDate today) {
        return (spawnDate != null) && !spawnDate.isAfter(today);
    }

    /**
     * @param today the current campaign date
     *
     * @return {@code true} if the point of interest is more than {@link #MAXIMUM_PLACEMENT_DELAY_DAYS} days past its
     *       day, so the daily lifecycle should stop trying to place it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isPlacementAbandoned(LocalDate today) {
        return (spawnDate != null) && spawnDate.plusDays(MAXIMUM_PLACEMENT_DELAY_DAYS).isBefore(today);
    }

    /**
     * Marks, at random, as many of the given scheduled points of interest as the count asks for - or all of them, if
     * there are fewer - by storing {@code true} under the given key in each one's initial state.
     *
     * @param scheduledPointsOfInterest the scheduled points of interest to choose from
     * @param count                     how many to mark
     * @param stateKey                  the initial state key to mark them with
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void markAtRandom(List<StratConScheduledPointOfInterest> scheduledPointsOfInterest, int count,
          String stateKey) {
        List<StratConScheduledPointOfInterest> candidates = new ArrayList<>(scheduledPointsOfInterest);

        int markedCount = min(count, candidates.size());
        for (int index = 0; index < markedCount; index++) {
            StratConScheduledPointOfInterest candidate = candidates.remove(Compute.randomInt(candidates.size()));
            candidate.getInitialState().put(stateKey, Boolean.TRUE.toString());
        }
    }

    @Override
    public String toString() {
        return String.format("%s on %s%s", typeId, spawnDate, strategicObjective ? " (objective)" : "");
    }
}
