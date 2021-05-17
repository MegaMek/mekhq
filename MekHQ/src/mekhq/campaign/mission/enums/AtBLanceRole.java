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

public enum AtBLanceRole {
    //region Enum Declarations
    FIGHTING("AtBLanceRole.FIGHTING.text"),
    DEFENCE("AtBLanceRole.DEFENCE.text"),
    SCOUTING("AtBLanceRole.SCOUTING.text"),
    TRAINING("AtBLanceRole.TRAINING.text"),
    UNASSIGNED("AtBLanceRole.UNASSIGNED.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    AtBLanceRole(final String name) {
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparison Methods
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

    public boolean isUnassigned() {
        return this == UNASSIGNED;
    }
    //endregion Boolean Comparison Methods

    public static AtBLanceRole parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        // Magic Number Save Format
        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return UNASSIGNED;
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

        MekHQ.getLogger().error("Unable to parse " + text + " into an AtBLanceRole. Returning FIGHTING.");

        return FIGHTING;
    }

    @Override
    public String toString() {
        return name;
    }
}
