/*
 * Copyright (C) 2021-2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.procreation;

import megamek.common.Compute;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;

public class RandomProcreation extends AbstractProcreation {
    //region Variable Declarations
    private int relationshipDieSize;
    private int relationshiplessDieSize;
    //endregion Variable Declarations

    //region Constructors
    public RandomProcreation(final CampaignOptions options) {
        super(RandomProcreationMethod.DICE_ROLL, options);
        setRelationshipDieSize(options.getRandomProcreationRelationshipDiceSize());
        setRelationshiplessDieSize(options.getRandomProcreationRelationshiplessDiceSize());
    }
    //endregion Constructors

    //region Getters/Setters
    @SuppressWarnings(value = "unused")
    public double getRelationshipDieSize() {
        return relationshipDieSize;
    }

    @SuppressWarnings(value = "unused")
    public void setRelationshipDieSize(final int relationshipDieSize) {
        this.relationshipDieSize = relationshipDieSize;
    }

    @SuppressWarnings(value = "unused")
    public double getRelationshiplessDieSize() {
        return relationshiplessDieSize;
    }

    @SuppressWarnings(value = "unused")
    public void setRelationshiplessDieSize(final int relationshiplessDieSize) {
        this.relationshiplessDieSize = relationshiplessDieSize;
    }
    //endregion Getters/Setters

    @Override
    protected boolean procreation(final Person person) {
        int diceSize = person.getGenealogy().hasSpouse() ? relationshipDieSize : relationshiplessDieSize;
        int multiplier = Math.max(1, person.getGenealogy().getChildren().size());

        return Compute.randomInt(diceSize * multiplier) == 0;
    }
}
