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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.divorce;

import megamek.common.Compute;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;

/**
 * The {@link RandomDivorce} class is an implementation of the {@link AbstractDivorce} class that represents a divorce
 * method based on random chance. The divorce outcome is determined by rolling a die with a specified number of sides.
 */
public class RandomDivorce extends AbstractDivorce {
    //region Variable Declarations
    private int divorceDiceSize;
    //endregion Variable Declarations

    //region Constructors

    /**
     * The {@link RandomDivorce} class is an implementation of the {@link AbstractDivorce} class that represents a
     * divorce method based on random chance.
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
