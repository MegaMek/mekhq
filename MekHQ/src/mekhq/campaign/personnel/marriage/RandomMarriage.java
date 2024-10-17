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
package mekhq.campaign.personnel.marriage;

import megamek.common.Compute;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;

/**
 * {@link RandomMarriage} class represents a type of marriage where the result is determined by
 * rolling a die.
 * It extends the {@link AbstractMarriage} class.
 */
public class RandomMarriage extends AbstractMarriage {
    //region Variable Declarations
    private int marriageDiceSize;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Constructs a {@link RandomMarriage} object. This object is used to manage randomly determined
     * marriages.
     *
     * @param options  the {@link CampaignOptions} object that contains current game campaign settings.
     */
    public RandomMarriage(final CampaignOptions options) {
        super(RandomMarriageMethod.DICE_ROLL, options);

        setMarriageDiceSize(options.getRandomMarriageDiceSize());
    }
    //endregion Constructors

    //region Getters/Setters
    /**
     * Sets the size of the marriage dice used in a random marriage.
     *
     * @param marriageDiceSize the size of the marriage dice to set
     */
    public void setMarriageDiceSize(final int marriageDiceSize) {
        this.marriageDiceSize = marriageDiceSize;
    }
    //endregion Getters/Setters

    @Override
    protected boolean randomMarriage() {
        if (marriageDiceSize == 0) {
            return false;
        } else if (marriageDiceSize == 1) {
            return true;
        }

        return Compute.randomInt(marriageDiceSize) == 0;
    }
}
