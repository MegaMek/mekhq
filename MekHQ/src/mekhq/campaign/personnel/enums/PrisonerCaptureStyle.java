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
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

public enum PrisonerCaptureStyle {
    //region Enum Declarations
    NONE("PrisonerCaptureStyle.NONE.text", "PrisonerCaptureStyle.NONE.toolTipText"),
    ATB("PrisonerCaptureStyle.ATB.text", "PrisonerCaptureStyle.ATB.toolTipText"),
    TAHARQA("PrisonerCaptureStyle.TAHARQA.text", "PrisonerCaptureStyle.TAHARQA.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String styleName;
    private final String toolTip;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    PrisonerCaptureStyle(String styleName, String toolTip) {
        this.styleName = resources.getString(styleName);
        this.toolTip = resources.getString(toolTip);
    }
    //endregion Constructors

    //region Boolean Comparisons
    public boolean isEnabled() {
        return this != NONE;
    }

    public boolean isAtB() {
        return this == ATB;
    }

    public boolean isTaharqa() {
        return this == TAHARQA;
    }
    //endregion Boolean Comparisons

    @Override
    public String toString() {
        return styleName;
    }

    public String getToolTip() {
        return toolTip;
    }
}
