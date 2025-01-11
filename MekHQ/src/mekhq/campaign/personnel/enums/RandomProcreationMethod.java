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
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.procreation.DisabledRandomProcreation;
import mekhq.campaign.personnel.procreation.RandomProcreation;

import java.util.ResourceBundle;

/**
 * The {@link RandomProcreationMethod} enum represents different methods of getting random procreation.
 * <p>
 * The available methods are:
 * - {@code NONE}: No random procreation method.
 * - {@code DICE_ROLL}: Random procreation method using dice roll.
 */
public enum RandomProcreationMethod {
    //region Enum Declarations
    NONE("RandomProcreationMethod.NONE.text", "RandomProcreationMethod.NONE.toolTipText"),
    DICE_ROLL("RandomProcreationMethod.DICE_ROLL.text", "RandomProcreationMethod.DICE_ROLL.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Constructor for the {@link RandomProcreationMethod} class.
     * Initializes the name and toolTipText variables using the specified name and toolTipText resources.
     *
     * @param name          the name resource key used to retrieve the name from the resource bundle
     * @param toolTipText   the tooltip text resource key used to retrieve the tool tip text from the resource bundle
     */
    RandomProcreationMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    /**
     * @return The tooltip text associated with the current instance of the class.
     */
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    /**
     * Checks if the current {@link RandomProcreationMethod} is {@code NONE}.
     *
     * @return {@code true} if the current {@link RandomProcreationMethod} is {@code NONE},
     * {@code false} otherwise.
     */
    public boolean isNone() {
        return this == NONE;
    }

    /**
     * Checks if the current {@link RandomProcreationMethod} is {@code DICE_ROLL}.
     *
     * @return {@code true} if the current {@link RandomProcreationMethod} is {@code DICE_ROLL},
     * {@code false} otherwise.
     */
    public boolean isDiceRoll() {
        return this == DICE_ROLL;
    }
    //endregion Boolean Comparison Methods

    /**
     * @param options the {@link CampaignOptions} object used to initialize the {@link AbstractProcreation} instance
     * @return an instance of {@link AbstractProcreation} based on the {@link RandomProcreationMethod}
     */
    public AbstractProcreation getMethod(final CampaignOptions options) {
        if (this == DICE_ROLL) {
            return new RandomProcreation(options);
        } else {
            return new DisabledRandomProcreation(options);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
