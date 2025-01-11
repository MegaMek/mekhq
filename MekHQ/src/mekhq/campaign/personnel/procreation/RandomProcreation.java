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

/**
 * Represents a random procreation method that is based on dice rolls.
 */
public class RandomProcreation extends AbstractProcreation {
    //region Variable Declarations
    private int relationshipDieSize;
    private int relationshiplessDieSize;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Constructor to create a {@link RandomProcreation} object. This object is used to manage
     * randomly determined procreation events within the game's campaign.
     *
     * @param options the campaign settings.
     */
    public RandomProcreation(final CampaignOptions options) {
        super(RandomProcreationMethod.DICE_ROLL, options);
        setRelationshipDieSize(options.getRandomProcreationRelationshipDiceSize());
        setRelationshiplessDieSize(options.getRandomProcreationRelationshiplessDiceSize());
    }
    //endregion Constructors

    //region Getters/Setters
    /**
     * Sets the size of the relationship die.
     * The relationship die size determines the probability of procreation for a person who has a spouse.
     *
     * @param relationshipDieSize the size of the relationship die
     */
    public void setRelationshipDieSize(final int relationshipDieSize) {
        this.relationshipDieSize = relationshipDieSize;
    }

    /**
     * Sets the size of the relationshipless die.
     * The relationship die size determines the probability of procreation for a person who does not
     * have a spouse.
     *
     * @param relationshiplessDieSize the size of the relationship die
     */
    public void setRelationshiplessDieSize(final int relationshiplessDieSize) {
        this.relationshiplessDieSize = relationshiplessDieSize;
    }
    //endregion Getters/Setters

    @Override
    protected boolean procreation(final Person person) {
        int diceSize = person.getGenealogy().hasSpouse() ? relationshipDieSize : relationshiplessDieSize;

        if (diceSize == 0) {
            return false;
        } else if (diceSize == 1) {
            return true;
        }

        return Compute.randomInt(diceSize) == 0;
    }
}
