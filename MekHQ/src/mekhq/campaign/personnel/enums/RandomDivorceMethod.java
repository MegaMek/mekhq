/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.divorce.AbstractDivorce;
import mekhq.campaign.personnel.divorce.DisabledRandomDivorce;
import mekhq.campaign.personnel.divorce.RandomDivorce;

import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;

public enum RandomDivorceMethod {
    //region Enum Declarations
    NONE("RandomDivorceMethod.NONE.text", "RandomDivorceMethod.NONE.toolTipText"),
    DICE_ROLL("RandomDivorceMethod.DICE_ROLL.text", "RandomDivorceMethod.DICE_ROLL.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    RandomDivorceMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = wordWrap(resources.getString(toolTipText));
    }
    //endregion Constructors

    //region Getters
    @SuppressWarnings(value = "unused")
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparisons
    @SuppressWarnings(value = "unused")
    public boolean isNone() {
        return this == NONE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDiceRoll() {
        return this == DICE_ROLL;
    }
    //endregion Boolean Comparisons

    public AbstractDivorce getMethod(final CampaignOptions options) {
        if (this == DICE_ROLL) {
            return new RandomDivorce(options);
        } else {
            return new DisabledRandomDivorce(options);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
