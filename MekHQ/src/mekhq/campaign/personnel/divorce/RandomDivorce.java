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
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;

public class RandomDivorce extends AbstractDivorce {
    //region Variable Declarations
    private int divorceDiceSize;
    //endregion Variable Declarations

    //region Constructors
    public RandomDivorce(final CampaignOptions options) {
        super(RandomDivorceMethod.DICE_ROLL, options);
        setDivorceDiceSize(options.getRandomDivorceDiceSize());
    }
    //endregion Constructors

    //region Getters/Setters
    @SuppressWarnings(value = "unused")
    public int getDivorceDiceSize() {
        return divorceDiceSize;
    }

    @SuppressWarnings(value = "unused")
    public void setDivorceDiceSize(final int divorceDiceSize) {
        this.divorceDiceSize = divorceDiceSize;
    }
    //endregion Getters/Setters

    @Override
    protected boolean randomDivorce(final Person person) {
        return Compute.randomInt(divorceDiceSize) == 0;
    }
}
