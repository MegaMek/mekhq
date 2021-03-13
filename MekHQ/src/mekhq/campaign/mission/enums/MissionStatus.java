/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum MissionStatus {
    //region Enum Declarations
    ACTIVE("MissionStatus.ACTIVE.text"),
    SUCCESS("MissionStatus.SUCCESS.text"),
    FAILED("MissionStatus.FAILED.text"),
    BREACH("MissionStatus.BREACH.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    MissionStatus(String name) {
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparison Methods
    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public boolean isBreach() {
        return this == BREACH;
    }

    /**
     * This is used to determine whether a status means that the mission is completed.
     * This is purposefully not a check to see if it is active for future proofing reasons
     * @return true if the mission has been completed, otherwise false
     */
    public boolean isCompleted() {
        return isSuccess() || isFailed() || isBreach();
    }
    //endregion Boolean Comparison Methods

    //region File IO
    /**
     * @param text containing the MissionStatus
     * @return the saved MissionStatus
     */
    public static MissionStatus parseFromString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 1:
                    return SUCCESS;
                case 2:
                    return FAILED;
                case 3:
                    return BREACH;
                case 0:
                default:
                    return ACTIVE;
            }
        } catch (Exception ignored) {

        }

        MekHQ.getLogger().error("Failed to parse text " + text + " into a MissionStatus");

        return ACTIVE;
    }
    //endregion File IO

    @Override
    public String toString() {
        return name;
    }
}
