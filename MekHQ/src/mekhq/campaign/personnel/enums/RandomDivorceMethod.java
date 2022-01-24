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
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.divorce.AbstractDivorce;
import mekhq.campaign.personnel.divorce.DisabledRandomDivorce;
import mekhq.campaign.personnel.divorce.PercentageRandomDivorce;

import java.util.ResourceBundle;

public enum RandomDivorceMethod {
    //region Enum Declarations
    NONE("RandomDivorceMethod.NONE.text", "RandomDivorceMethod.NONE.toolTipText"),
    PERCENTAGE("RandomDivorceMethod.PERCENTAGE.text", "RandomDivorceMethod.PERCENTAGE.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    RandomDivorceMethod(final String name, final String toolTipText) {
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

    public boolean isPercentage() {
        return this == PERCENTAGE;
    }
    //endregion Boolean Comparisons

    public AbstractDivorce getMethod(final CampaignOptions options) {
        switch (this) {
            case PERCENTAGE:
                return new PercentageRandomDivorce(options);
            case NONE:
            default:
                return new DisabledRandomDivorce(options);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
