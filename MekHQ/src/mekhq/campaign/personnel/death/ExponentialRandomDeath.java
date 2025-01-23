/*
 * Copyright (c) 2021-2025 - The MegaMek Team. All Rights Reserved.
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

/**
 * Implements a random death generator using gender-specific exponential equations.
 * These equations estimate the probability of death based on the person's age, gender,
 * and coefficients derived from real-world data (US death statistics in 2018).
 * The death rates are calculated weekly, compared with a randomly generated value,
 * and returned as a boolean indicating whether the person dies or not.
 */

public class ExponentialRandomDeath extends AbstractDeath {
    //region Variable Declarations
    /**
     * <p>An array of constants representing the male-specific coefficients (c, n, k)
     * used in the gender-dependent exponential death equation in the format:</p>
     *
     * <pre>c * 10^n * e^(k * age)</pre>
     */
    private final double[] MALE_DEATH_RATE = new double[]{5.4757, -7.0, 0.0709};

    /**
     * <p>An array of constants representing the female-specific coefficients (c, n, k)
     * used in the gender-dependent exponential death equation in the format:</p>
     *
     * <pre>c * 10^n * e^(k * age)</pre>
     */
    private final double[] FEMALE_DEATH_RATE = new double[]{2.4641, -7.0, 0.0752};
    //endregion Variable Declarations

    //region Constructors
    /**
     * Constructor for ExponentialRandomDeath.
     * Initializes the death method type, campaign options, and causes of death.
     *
     * @param options           The campaign options object that contains relevant settings.
     * @param initializeCauses  Whether to initialize random causes of death.
     */
    public ExponentialRandomDeath(final CampaignOptions options, final boolean initializeCauses) {
        super(RandomDeathMethod.RANDOM, options, initializeCauses);
    }
    //endregion Constructors

    /**
     * Determines if a person dies a random death based on gender-specific exponential equations.
     * The calculation uses weekly probabilities derived from gender-specific coefficients and age,
     * compared to a randomly generated value.
     *
     * <p>The exponential equation used is:</p>
     * <pre>c * 10^n * e^(k * age * 7)</pre>
     *
     * @param age    The person's age.
     * @param gender The person's gender.
     * @return {@code true} if the person is selected to randomly die, {@code false} otherwise.
     */
    @Override
    public boolean randomlyDies(final int age, final Gender gender) {
        double[] deathRateArray = gender.isMale() ? MALE_DEATH_RATE : FEMALE_DEATH_RATE;
        double chanceOfDeath = deathRateArray[0] * Math.pow(10, deathRateArray[1])
            * Math.exp(deathRateArray[2] * age * 7); // Multiply character age by 7 for weekly rate

        // Compare the random float with the calculated weekly death rate
        return Compute.randomFloat() < chanceOfDeath;
    }
}
