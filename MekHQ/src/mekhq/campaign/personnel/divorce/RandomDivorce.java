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
package mekhq.campaign.personnel.divorce;

import megamek.common.Compute;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;

/**
 * The {@link RandomDivorce} class is an implementation of the {@link AbstractDivorce} class that
 * represents a divorce method based on random chance.
 * The divorce outcome is determined by rolling a die with a specified number of sides.
 */
public class RandomDivorce extends AbstractDivorce {
    //region Variable Declarations
    private int divorceDiceSize;
    //endregion Variable Declarations

    //region Constructors
    /**
     * The {@link RandomDivorce} class is an implementation of the {@link AbstractDivorce} class that
     * represents a divorce method based on random chance.
     */
    public RandomDivorce(final CampaignOptions options) {
        super(RandomDivorceMethod.DICE_ROLL, options);
        setDivorceDiceSize(options.getRandomDivorceDiceSize());
    }
    //endregion Constructors

    /**
     * Retrieves the size of the divorce dice.
     *
     * @return The size of the divorce dice as an integer.
     */
    //region Getters/Setters
    public int getDivorceDiceSize() {
        return divorceDiceSize;
    }

    /**
     * Sets the size of the divorce dice.
     *
     * @param divorceDiceSize the size of the dice used to determine divorce outcomes
     */
    public void setDivorceDiceSize(final int divorceDiceSize) {
        this.divorceDiceSize = divorceDiceSize;
    }
    //endregion Getters/Setters

    @Override
    protected boolean randomDivorce() {
        if (divorceDiceSize == 0) {
            return false;
        } else if (divorceDiceSize == 1) {
            return true;
        }

        return Compute.randomInt(divorceDiceSize) == 0;
    }
}
