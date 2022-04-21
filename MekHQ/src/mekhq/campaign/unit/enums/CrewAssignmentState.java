/*
 * Copyright (c) 2016-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package mekhq.campaign.unit.enums;

import megamek.common.util.EncodeControl;
import mekhq.campaign.unit.Unit;

import java.util.ResourceBundle;

/**
 * This is used to specify the current crew assignment state for a {@link Unit}.
 *
 * Unsupported = no tech, no pilot/gunner
 * Unmaintained = no tech, 1+ pilot/gunner
 * Uncrewed = tech, no crew
 * Partially Crewed = tech, less than full pilot/gunner
 * Fully Crewed = tech, full crew
 * @author Justin "Windchild" Bowen
 */
public enum CrewAssignmentState {
    //region Enum Declarations
    UNSUPPORTED("CrewAssignmentState.UNSUPPORTED.text", "CrewAssignmentState.UNSUPPORTED.toolTipText"),
    UNMAINTAINED("CrewAssignmentState.UNMAINTAINED.text", "CrewAssignmentState.UNMAINTAINED.toolTipText"),
    UNCREWED("CrewAssignmentState.UNCREWED.text", "CrewAssignmentState.UNCREWED.toolTipText"),
    PARTIALLY_CREWED("CrewAssignmentState.PARTIALLY_CREWED.text", "CrewAssignmentState.PARTIALLY_CREWED.toolTipText"),
    FULLY_CREWED("CrewAssignmentState.FULLY_CREWED.text", "CrewAssignmentState.FULLY_CREWED.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    CrewAssignmentState(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Unit", new EncodeControl());
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
    public boolean isUnsupported() {
        return this == UNSUPPORTED;
    }

    public boolean isUnmaintained() {
        return this == UNMAINTAINED;
    }

    public boolean isUncrewed() {
        return this == UNCREWED;
    }

    public boolean isPartiallyCrewed() {
        return this == PARTIALLY_CREWED;
    }

    public boolean isFullyCrewed() {
        return this == FULLY_CREWED;
    }
    //endregion Boolean Comparison Methods

    @Override
    public String toString() {
        return name;
    }
}
