/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;

public class RandomMarriage extends AbstractMarriage {
    //region Variable Declarations
    private int oppositeSexDiceSize;
    private int sameSexDiceSize;
    //endregion Variable Declarations

    //region Constructors
    public RandomMarriage(final CampaignOptions options) {
        super(RandomMarriageMethod.DICE_ROLL, options);

        setOppositeSexDiceSize(options.getRandomMarriageOppositeSexDiceSize());

        setSameSexDiceSize(options.getRandomMarriageSameSexDiceSize());
    }
    //endregion Constructors

    //region Getters/Setters
    @SuppressWarnings(value = "unused")
    public int getOppositeSexDiceSize() {
        return oppositeSexDiceSize;
    }

    @SuppressWarnings(value = "unused")
    public void setOppositeSexDiceSize(final int oppositeSexDiceSize) {
        this.oppositeSexDiceSize = oppositeSexDiceSize;
    }

    @SuppressWarnings(value = "unused")
    public int getSameSexDiceSize() {
        return sameSexDiceSize;
    }

    @SuppressWarnings(value = "unused")
    public void setSameSexDiceSize(final int sameSexDiceSize) {
        this.sameSexDiceSize = sameSexDiceSize;
    }
    //endregion Getters/Setters

    @Override
    protected boolean randomOppositeSexMarriage(final Person person) {
        // this is used to simulate failed relationships.
        // it's very arbitrary but serves its purpose for now.
        // TODO: replace this with a proper relationship system
        if (Compute.randomInt(100) != 0) {
            return false;
        }

        int multiplier = Math.max(1, person.getGenealogy().getFormerSpouses().size());

        return Compute.randomInt(oppositeSexDiceSize * multiplier) == 0;
    }

    @Override
    protected boolean randomSameSexMarriage(final Person person) {
        // this is used to simulate failed relationships.
        // it's very arbitrary but serves its purpose for now.
        // TODO: replace this with a proper relationship system
        if (Compute.randomInt(100) != 0) {
            return false;
        }

        int multiplier = Math.max(1, person.getGenealogy().getFormerSpouses().size());

        return Compute.randomInt(sameSexDiceSize * multiplier) == 0;
    }
}
