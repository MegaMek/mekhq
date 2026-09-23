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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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
     * @param today the current campaign date
     *
     * @return {@code true} if the point of interest's day has come - today or earlier, so a skipped day still catches
     *       up
     *
     * @author Illiani
     * @since 0.51.01
     */
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

    public boolean isDue(LocalDate today) {
        return (spawnDate != null) && !spawnDate.isAfter(today);
    }

    @Override
    public String toString() {
        return String.format("%s on %s%s", typeId, spawnDate, strategicObjective ? " (objective)" : "");
    }
}
