/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.randomProcreation;

import megamek.common.Compute;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;

public class PercentageRandomProcreation extends AbstractProcreation {
    //region Variable Declarations
    private final double percentage;
    private final double relationshiplessPercentage;
    //endregion Variable Declarations

    //region Constructors
    public PercentageRandomProcreation(final CampaignOptions options) {
        super(RandomProcreationMethod.PERCENTAGE, options.isUseRelationshiplessRandomProcreation());
        this.percentage = options.getPercentageRandomProcreationRelationshipChance();
        this.relationshiplessPercentage = options.getPercentageRandomProcreationRelationshiplessChance();
    }
    //endregion Constructors

    //region Getters
    public double getPercentage() {
        return percentage;
    }

    public double getRelationshiplessPercentage() {
        return relationshiplessPercentage;
    }
    //endregion Getters

    @Override
    protected boolean relationshipProcreation(final Person person) {
        return Compute.randomFloat() < getPercentage();
    }

    @Override
    protected boolean relationshiplessProcreation(final Person person) {
        return Compute.randomFloat() < getRelationshiplessPercentage();
    }
}
