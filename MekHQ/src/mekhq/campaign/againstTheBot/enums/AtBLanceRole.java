/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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
package mekhq.campaign.againstTheBot.enums;

import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

public enum AtBLanceRole {
    //region Enum Declarations
    FIGHTING("AtBLanceRole.FIGHTING.text"),
    DEFENCE("AtBLanceRole.DEFENCE.text"),
    SCOUTING("AtBLanceRole.SCOUTING.text"),
    TRAINING("AtBLanceRole.TRAINING.text");
    //endregion Enum Declarations

    //region Variable Declarations
    final String roleName;
    //endregion Variable Declarations

    AtBLanceRole(String roleName) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AtB",
                new EncodeControl());
        this.roleName = resources.getString(roleName);
    }

    @Override
    public String toString() {
        return roleName;
    }
}
