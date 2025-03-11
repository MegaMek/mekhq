/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.enums;

import mekhq.MekHQ;
import mekhq.MHQConstants;

import java.util.ResourceBundle;

/**
 * This is the style of Operational Status indicator to use for a LayeredForceIcon when
 * automatically assigning and updating the value based on the assigned units.
 */
public enum ForceIconOperationalStatusStyle {
    //region Enum Declarations
    BORDER("ForceIconOperationalStatusStyle.BORDER.text", "ForceIconOperationalStatusStyle.BORDER.toolTipText", MHQConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_BORDER_PATH),
    TAB("ForceIconOperationalStatusStyle.TAB.text", "ForceIconOperationalStatusStyle.TAB.toolTipText", MHQConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_TAB_PATH);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String path;
    //endregion Variable Declarations

    //region Constructors
    ForceIconOperationalStatusStyle(final String name, final String toolTipText,
                                    final String path) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
                MekHQ.getMHQOptions().getLocale());
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
