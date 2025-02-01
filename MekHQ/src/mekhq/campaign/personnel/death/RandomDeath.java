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

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.enums.RandomDeathMethod;

import static megamek.common.Compute.randomInt;

public class RandomDeath extends AbstractDeath {
    //region Variable Declarations
    private double percentage;
    //endregion Variable Declarations

    //region Constructors
    public RandomDeath(final CampaignOptions options, final boolean initializeCauses) {
        super(RandomDeathMethod.RANDOM, options, initializeCauses);
        setPercentage(options.getRandomDeathChance());
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

    /**
     * Determines if a person randomly dies based on the campaign, age, and gender.
     *
     * <p>The probability of death increases as a person's age exceeds a specific
     * threshold, with the chance of death growing exponentially for extra years lived.</p>
     *
     * @param campaign The campaign that defines the base random death chance.
     * @param age The individual's age.
     * @param gender The individual's gender. Currently unused but supports future extensibility.
     * @return {@code true} if the person randomly dies; {@code false} otherwise.
     */
    @Override
    public boolean randomlyDies(Campaign campaign, final int age, final Gender gender) {
        final int AGE_THRESHOLD = 90;
        final double REDUCTION_MULTIPLIER = 0.90;

        int baseDieSize = campaign.getCampaignOptions().getRandomDeathChance();

        // Calculate adjusted die size if the age exceeds the threshold
        int adjustedDieSize = (age > AGE_THRESHOLD)
            ? (int) Math.round(baseDieSize * Math.pow(REDUCTION_MULTIPLIER, (age - AGE_THRESHOLD)))
            : baseDieSize;

        // Return random death outcome
        return randomInt(adjustedDieSize) < getPercentage();
    }
}
