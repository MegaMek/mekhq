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
package mekhq.gui.enums;

import megamek.common.util.EncodeControl;

import java.util.List;
import java.util.ResourceBundle;

public enum PersonnelFilterStyle {
    //region Enum Declarations
    STANDARD("PersonnelFilterStyle.STANDARD.text", "PersonnelFilterStyle.STANDARD.toolTipText"),
    INDIVIDUAL_ROLE("PersonnelFilterStyle.INDIVIDUAL_ROLE.text", "PersonnelFilterStyle.INDIVIDUAL_ROLE.toolTipText"),
    ALL("PersonnelFilterStyle.ALL.text", "PersonnelFilterStyle.ALL.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUIEnums", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    PersonnelFilterStyle(String name, String toolTipText) {
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    public List<PersonnelFilter> getFilters(boolean standard) {
        switch (this) {
            case INDIVIDUAL_ROLE:
                return standard ? PersonnelFilter.getIndividualRolesStandardPersonnelFilters()
                        : PersonnelFilter.getIndividualRolesExpandedPersonnelFilters();
            case ALL:
                return standard ? PersonnelFilter.getAllStandardFilters()
                        : PersonnelFilter.getAllIndividualRoleFilters();
            default:
            case STANDARD:
                return standard ? PersonnelFilter.getStandardPersonnelFilters()
                        : PersonnelFilter.getExpandedPersonnelFilters();
        }
    }

    public String getToolTipText() {
        return toolTipText;
    }

    @Override
    public String toString() {
        return name;
    }
}
