/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
import mekhq.MekHqConstants;

import java.util.ResourceBundle;

public enum ForceIconOperationalStatusStyle {
    //region Enum Declarations
    BORDER("ForceIconOperationalStatusStyle.BORDER.text", "ForceIconOperationalStatusStyle.BORDER.toolTipText", MekHqConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_BORDER_PATH),
    TAB("ForceIconOperationalStatusStyle.TAB.text", "ForceIconOperationalStatusStyle.TAB.toolTipText", MekHqConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_TAB_PATH);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String path;
    //endregion Variable Declarations

    //region Constructors
    ForceIconOperationalStatusStyle(final String name, final String toolTipText,
                                    final String path) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.path = path;
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public String getPath() {
        return path;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isBorder() {
        return this == BORDER;
    }

    public boolean isTab() {
        return this == TAB;
    }
    //endregion Boolean Comparison Methods

    @Override
    public String toString() {
        return name;
    }
}
