/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
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
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum RandomRetirementMethod {
    //region Enum Declarations
    NONE("RandomRetirementMethod.NONE.text", "RandomRetirementMethod.NONE.toolTipText"),
    AGAINST_THE_BOT("RandomRetirementMethod.AGAINST_THE_BOT.text", "RandomRetirementMethod.AGAINST_THE_BOT.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    RandomRetirementMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparisons
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isAtB() {
        return this == AGAINST_THE_BOT;
    }
    //endregion Boolean Comparisons

/*
    public AbstractRetirement getMethod(final CampaignOptions options) {
        switch (this) {
            case AGAINST_THE_BOT:
                return new AtBRandomRetirement(options);
            case NONE:
            default:
                return new DisabledRandomRetirement(options);
        }
    }
*/

    @Override
    public String toString() {
        return name;
    }
}
