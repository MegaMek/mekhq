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
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.marriage.DisabledRandomMarriage;
import mekhq.campaign.personnel.marriage.RandomMarriage;

import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;

/**
 * The {@link RandomMarriageMethod} enum represents different methods of getting random marriages.
 * <p>
 * The available methods are:
 * - {@code NONE}: No random marriage method.
 * - {@code DICE_ROLL}: Random marriage method using dice roll.
 */
public enum RandomMarriageMethod {
    //region Enum Declarations
    NONE("RandomMarriageMethod.NONE.text", "RandomMarriageMethod.NONE.toolTipText"),
    DICE_ROLL("RandomMarriageMethod.DICE_ROLL.text", "RandomMarriageMethod.DICE_ROLL.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    /**
     * Constructor for the {@link RandomMarriageMethod} class.
     * Initializes the name and toolTipText variables using the specified name and toolTipText resources.
     *
     * @param name          the name resource key used to retrieve the name from the resource bundle
     * @param toolTipText   the tooltip text resource key used to retrieve the tool tip text from the resource bundle
     */
    //region Constructors
    RandomMarriageMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = wordWrap(resources.getString(toolTipText));
    }
    //endregion Constructors

    /**
     * @return The tooltip text associated with the current instance of the class.
     */
    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    /**
     * Checks if the current {@link RandomMarriageMethod} is {@code NONE}.
     *
     * @return {@code true} if the current {@link RandomMarriageMethod} is {@code NONE},
     * {@code false} otherwise.
     */
    public boolean isNone() {
        return this == NONE;
    }

    /**
     * Checks if the current {@link RandomMarriageMethod} is {@code DICE_ROLL}.
     *
     * @return {@code true} if the current {@link RandomMarriageMethod} is {@code DICE_ROLL},
     * {@code false} otherwise.
     */
    public boolean isDiceRoll() {
        return this == DICE_ROLL;
    }
    //endregion Boolean Comparison Methods

    /**
     * @param options the {@link CampaignOptions} object used to initialize the {@link AbstractMarriage} instance
     * @return an instance of {@link AbstractMarriage} based on the {@link RandomMarriageMethod}
     */
    public AbstractMarriage getMethod(final CampaignOptions options) {
        if (this == DICE_ROLL) {
            return new RandomMarriage(options);
        } else {
            return new DisabledRandomMarriage(options);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
