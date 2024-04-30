/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import org.apache.logging.log4j.LogManager;

import java.util.ResourceBundle;

public enum AtBMoraleLevel {
    //region Enum Declarations
    ROUT("AtBMoraleLevel.ROUT.text", "AtBMoraleLevel.ROUT.toolTipText"),
    VERY_LOW("AtBMoraleLevel.VERY_LOW.text", "AtBMoraleLevel.VERY_LOW.toolTipText"),
    LOW("AtBMoraleLevel.LOW.text", "AtBMoraleLevel.LOW.toolTipText"),
    NORMAL("AtBMoraleLevel.NORMAL.text", "AtBMoraleLevel.NORMAL.toolTipText"),
    HIGH("AtBMoraleLevel.HIGH.text", "AtBMoraleLevel.HIGH.toolTipText"),
    VERY_HIGH("AtBMoraleLevel.VERY_HIGH.text", "AtBMoraleLevel.VERY_HIGH.toolTipText"),
    INVINCIBLE("AtBMoraleLevel.INVINCIBLE.text", "AtBMoraleLevel.INVINCIBLE.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    AtBMoraleLevel(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission",
                MekHQ.getMHQOptions().getLocale());
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
    public boolean isRout() {
        return this == ROUT;
    }

    public boolean isVeryLow() {
        return this == VERY_LOW;
    }

    public boolean isLow() {
        return this == LOW;
    }

    public boolean isNormal() {
        return this == NORMAL;
    }

    public boolean isHigh() {
        return this == HIGH;
    }

    public boolean isVeryHigh() {
        return this == VERY_HIGH;
    }

    public boolean isInvincible() {
        return this == INVINCIBLE;
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    /**
     * @param text containing the AtBMoraleLevel
     * @return the saved AtBMoraleLevel
     */
    public static AtBMoraleLevel parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return ROUT;
                case 1:
                    return VERY_LOW;
                case 2:
                    return LOW;
                case 3:
                    return NORMAL;
                case 4:
                    return HIGH;
                case 5:
                    return VERY_HIGH;
                case 6:
                    return INVINCIBLE;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        LogManager.getLogger().error("Unable to parse " + text + " into an AtBMoraleLevel. Returning NORMAL.");
        return NORMAL;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
