/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.enums;

import java.util.ResourceBundle;

import mekhq.campaign.unit.Unit;

/**
 * This is used to specify the current crew assignment state for a {@link Unit}. The states mean the following:
 * Unsupported = no tech, no pilot/gunner Unmaintained = no tech, 1+ pilot/gunner Uncrewed = tech, no crew Partially
 * Crewed = tech, less than full pilot/gunner Fully Crewed = tech, full crew
 *
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
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Unit");
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
