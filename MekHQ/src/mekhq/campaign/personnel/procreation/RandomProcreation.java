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
package mekhq.campaign.personnel.procreation;

import megamek.common.compute.Compute;
import mekhq.campaign.campaignOptions.CampaignOptions;
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
     * Constructor to create a {@link RandomProcreation} object. This object is used to manage randomly determined
     * procreation events within the game's campaign.
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
     * Sets the size of the relationship die. The relationship die size determines the probability of procreation for a
     * person who has a spouse.
     *
     * @param relationshipDieSize the size of the relationship die
     */
    public void setRelationshipDieSize(final int relationshipDieSize) {
        this.relationshipDieSize = relationshipDieSize;
    }

    /**
     * Sets the size of the relationshipless die. The relationship die size determines the probability of procreation
     * for a person who does not have a spouse.
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
