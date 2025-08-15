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
package mekhq.campaign.personnel.marriage;

import megamek.common.Compute;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;

/**
 * {@link RandomMarriage} class represents a type of marriage where the result is determined by rolling a die. It
 * extends the {@link AbstractMarriage} class.
 */
public class RandomMarriage extends AbstractMarriage {
    //region Variable Declarations
    private int marriageDiceSize;
    //endregion Variable Declarations

    //region Constructors

    /**
     * Constructs a {@link RandomMarriage} object. This object is used to manage randomly determined marriages.
     *
     * @param options the {@link CampaignOptions} object that contains current game campaign settings.
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
