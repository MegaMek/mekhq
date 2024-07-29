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
import mekhq.campaign.personnel.enums.TenYearAgeRange;

import java.util.HashMap;
import java.util.Map;

public class AgeRangeRandomDeath extends AbstractDeath {
    //region Variable Declarations
    private Map<TenYearAgeRange, Double> male;
    private Map<TenYearAgeRange, Double> female;
    //endregion Variable Declarations

    //region Constructors
    public AgeRangeRandomDeath(final CampaignOptions options, final boolean initializeCauses) {
        super(RandomDeathMethod.AGE_RANGE, options, initializeCauses);
        adjustRangeValues(options);
    }
    //endregion Constructors

    //region Getters/Setters
    public Map<TenYearAgeRange, Double> getMale() {
        return male;
    }

    public void setMale(final Map<TenYearAgeRange, Double> male) {
        this.male = male;
    }

    public Map<TenYearAgeRange, Double> getFemale() {
        return female;
    }

    public void setFemale(final Map<TenYearAgeRange, Double> female) {
        this.female = female;
    }
    //endregion Getters/Setters

    /**
     * Odds are over an entire year per 100,000 people, so we need to adjust the numbers to be
     * the individual odds for a day. We do this now, so it only occurs once per option set.
     * @param options the options to set the ranges based on
     */
    public void adjustRangeValues(final CampaignOptions options) {
        setMale(new HashMap<>());
        setFemale(new HashMap<>());
        final double adjustment = 365.25 * 100000.0;
        for (final TenYearAgeRange ageRange : TenYearAgeRange.values()) {
            getMale().put(ageRange, options.getAgeRangeRandomDeathMaleValues().get(ageRange) / adjustment);
            getFemale().put(ageRange, options.getAgeRangeRandomDeathFemaleValues().get(ageRange) / adjustment);
        }
    }

    /**
     * @param age the person's age
     * @param gender the person's gender
     * @return true if the person is selected to randomly die
     */
    @Override
    public boolean randomlyDies(final int age, final Gender gender) {
        final TenYearAgeRange ageRange = TenYearAgeRange.determineAgeRange(age);
        return Compute.randomInt() < ((gender.isMale() ? getMale() : getFemale()).get(ageRange));
    }
}
