/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.logging.MMLogger;
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum AtBLanceRole {
    // region Enum Declarations
    FIGHTING("AtBLanceRole.FIGHTING.text", "AtBLanceRole.FIGHTING.toolTipText"),
    DEFENCE("AtBLanceRole.DEFENCE.text", "AtBLanceRole.DEFENCE.toolTipText"),
    SCOUTING("AtBLanceRole.SCOUTING.text", "AtBLanceRole.SCOUTING.toolTipText"),
    TRAINING("AtBLanceRole.TRAINING.text", "AtBLanceRole.TRAINING.toolTipText"),
    IN_RESERVE("AtBLanceRole.IN_RESERVE.text", "AtBLanceRole.IN_RESERVE.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    AtBLanceRole(final String name, final String toolTipText) {
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
    public boolean isFighting() {
        return this == FIGHTING;
    }

    public boolean isDefence() {
        return this == DEFENCE;
    }

    public boolean isScouting() {
        return this == SCOUTING;
    }

    public boolean isTraining() {
        return this == TRAINING;
    }

    public boolean isInReserve() {
        return this == IN_RESERVE;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    public static AtBLanceRole parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return IN_RESERVE;
                case 1:
                    return FIGHTING;
                case 2:
                    return DEFENCE;
                case 3:
                    return SCOUTING;
                case 4:
                    return TRAINING;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(AtBLanceRole.class)
                .error("Unable to parse " + text + " into an AtBLanceRole. Returning FIGHTING.");

        return FIGHTING;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
