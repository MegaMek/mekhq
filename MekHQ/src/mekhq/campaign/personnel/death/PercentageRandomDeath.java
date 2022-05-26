/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.death;

import megamek.common.Compute;
import megamek.common.enums.Gender;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.enums.RandomDeathMethod;

public class PercentageRandomDeath extends AbstractDeath {
    //region Variable Declarations
    private double percentage;
    //endregion Variable Declarations

    //region Constructors
    public PercentageRandomDeath(final CampaignOptions options) {
        super(RandomDeathMethod.PERCENTAGE, options);
        setPercentage(options.getPercentageRandomDeathChance());
    }
    //endregion Constructors

    //region Getters/Setters
    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(final double percentage) {
        this.percentage = percentage;
    }
    //endregion Getters/Setters

    @Override
    public boolean randomlyDies(final int age, final Gender gender) {
        return Compute.randomFloat() < getPercentage();
    }
}
