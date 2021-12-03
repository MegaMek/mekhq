/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
    ACTIVE("MissionStatus.ACTIVE.text", "MissionStatus.ACTIVE.toolTipText"),
    SUCCESS("MissionStatus.SUCCESS.text", "MissionStatus.SUCCESS.toolTipText"),
    FAILED("MissionStatus.FAILED.text", "MissionStatus.FAILED.toolTipText"),
    BREACH("MissionStatus.BREACH.text", "MissionStatus.BREACH.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    MissionStatus(final String name, final String toolTipText) {
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

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

    //region File I/O
    /**
     * @param text containing the MissionStatus
     * @return the saved MissionStatus
     */
    public static MissionStatus parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return ACTIVE;
                case 1:
                    return SUCCESS;
                case 2:
                    return FAILED;
                case 3:
                    return BREACH;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MekHQ.getLogger().error("Failed to parse text " + text + " into a MissionStatus, returning ACTIVE.");

        return ACTIVE;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
