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

/**
 * An enumeration representing the available random divorce methods.
 * <p>
 * The {@link RandomDivorceMethod} enum is used to specify the method of randomly generating a divorce.
 * It supports two methods: {@code NONE} and {@code DICE_ROLL}.
 * <p>
 * The {@code NONE} method represents no random divorce generation, and the {@code DICE_ROLL} method
 * represents randomly generated divorce using dice rolls.
 */
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
    /**
     * Constructor for the {@link RandomDivorceMethod} class.
     * Initializes the name and toolTipText variables using the specified name and toolTipText resources.
     *
     * @param name          the name resource key used to retrieve the name from the resource bundle
     * @param toolTipText   the tooltip text resource key used to retrieve the tool tip text from the resource bundle
     */
    RandomDivorceMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    /**
     * @return the tooltip text for the current object
     */
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparisons
    /**
     * Checks if the current {@link RandomDivorceMethod} is {@code NONE}.
     *
     * @return {@code true} if the current {@link RandomDivorceMethod} is {@code NONE},
     * {@code false} otherwise.
     */
    public boolean isNone() {
        return this == NONE;
    }

    /**
     * Checks if the current {@link RandomDivorceMethod} is {@code DICE_ROLL}.
     *
     * @return {@code true} if the current {@link RandomDivorceMethod} is {@code DICE_ROLL},
     * {@code false} otherwise.
     */
    public boolean isDiceRoll() {
        return this == DICE_ROLL;
    }
    //endregion Boolean Comparisons

    /**
     * @param options the {@link CampaignOptions} object used to initialize the {@link AbstractDivorce} instance
     * @return an instance of {@link AbstractDivorce} based on the {@link RandomDivorceMethod}
     */
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
