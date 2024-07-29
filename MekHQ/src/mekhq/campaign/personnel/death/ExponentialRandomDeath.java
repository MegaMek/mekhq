/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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

public class ExponentialRandomDeath extends AbstractDeath {
    //region Variable Declarations
    private double[] male;
    private double[] female;
    //endregion Variable Declarations

    //region Constructors
    public ExponentialRandomDeath(final CampaignOptions options, final boolean initializeCauses) {
        super(RandomDeathMethod.EXPONENTIAL, options, initializeCauses);
        setMale(options.getExponentialRandomDeathMaleValues());
        setFemale(options.getExponentialRandomDeathFemaleValues());
    }
    //endregion Constructors

    //region Getters/Setters
    public double[] getMale() {
        return male;
    }

    public void setMale(final double... male) {
        this.male = male;
    }

    public double[] getFemale() {
        return female;
    }

    public void setFemale(final double... female) {
        this.female = female;
    }
    //endregion Getters/Setters

    /**
     * Determines if a person dies a random death based on gender-dependent exponential equations in
     * the format c * 10^n * e^(k * age).
     * @param age the person's age
     * @param gender the person's gender
     * @return true if the person is selected to randomly die
     */
    @Override
    public boolean randomlyDies(final int age, final Gender gender) {
        return Compute.randomInt() < (gender.isMale()
                ? (getMale()[0] * Math.pow(10, getMale()[1]) * Math.exp(getMale()[2] * age))
                : (getFemale()[0] * Math.pow(10, getFemale()[1]) * Math.exp(getFemale()[2] * age)));
    }
}
