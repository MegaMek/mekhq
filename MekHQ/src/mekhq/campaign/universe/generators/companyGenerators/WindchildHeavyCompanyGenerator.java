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
package mekhq.campaign.universe.generators.companyGenerators;

import megamek.common.EntityWeightClass;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;

public class WindchildHeavyCompanyGenerator extends WindchildCompanyGenerator {
    //region Constructors
    public WindchildHeavyCompanyGenerator(final Campaign campaign, final CompanyGenerationOptions options) {
        super(CompanyGenerationMethod.WINDCHILD_HEAVY, campaign, options);
    }
    //endregion Constructors

    /**
     * This guarantees a BattleMech, and rolls a heavier lance
     *
     * @param roll the modified roll to use
     * @return the generated EntityWeightClass
     * EntityWeightClass.WEIGHT_ULTRA_LIGHT for none,
     * EntityWeightClass.WEIGHT_SUPER_HEAVY for SL tables
     */
    @Override
    protected int determineBattleMechWeight(final int roll) {
        switch (roll) {
            case 2:
            case 3:
            case 4:
                return EntityWeightClass.WEIGHT_LIGHT;
            case 6:
            case 7:
                return EntityWeightClass.WEIGHT_MEDIUM;
            case 8:
            case 9:
            case 10:
                return EntityWeightClass.WEIGHT_HEAVY;
            case 5:
            case 11:
            case 12:
                return EntityWeightClass.WEIGHT_ASSAULT;
            default:
                return EntityWeightClass.WEIGHT_SUPER_HEAVY;
        }
    }
}
