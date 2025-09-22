/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.enums;

import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum MissionStatus {
    // region Enum Declarations
    ACTIVE("MissionStatus.ACTIVE.text", "MissionStatus.ACTIVE.toolTipText"),
    SUCCESS("MissionStatus.SUCCESS.text", "MissionStatus.SUCCESS.toolTipText"),
    PARTIAL("MissionStatus.PARTIAL.text", "MissionStatus.PARTIAL.toolTipText"),
    FAILED("MissionStatus.FAILED.text", "MissionStatus.FAILED.toolTipText"),
    BREACH("MissionStatus.BREACH.text", "MissionStatus.BREACH.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    MissionStatus(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isPartialSuccess() {
        return this == PARTIAL;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public boolean isBreach() {
        return this == BREACH;
    }

    /**
     * This is used to determine whether a status means that the mission is completed. This is purposefully not a check
     * to see if it is active for future proofing reasons
     *
     * @return true if the mission has been completed, otherwise false
     */
    public boolean isCompleted() {
        return isSuccess() || isPartialSuccess() || isFailed() || isBreach();
    }
    // endregion Boolean Comparison Methods

    // region File I/O

    /**
     * @param text containing the MissionStatus
     *
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
                    return PARTIAL;
                case 3:
                    return FAILED;
                case 4:
                    return BREACH;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(MissionStatus.class)
              .error("Unable to parse {} into a MissionStatus. Returning ACTIVE.", text);
        return ACTIVE;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
