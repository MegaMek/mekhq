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
package mekhq.campaign.universe.generators.battleMekWeightClassGenerators;

import megamek.common.EntityWeightClass;
import mekhq.campaign.universe.enums.BattleMechWeightClassGenerationMethod;

/**
 * @author Justin "Windchild" Bowen
 */
public class AtBBattleMekWeightClassGenerator extends AbstractBattleMekWeightClassGenerator {
    //region Constructors
    public AtBBattleMekWeightClassGenerator() {
        super(BattleMechWeightClassGenerationMethod.AGAINST_THE_BOT);
    }
    //endregion Constructors

    @Override
    public int generate(final int roll) {
        switch (roll) {
            case 2:
            case 3:
                return EntityWeightClass.WEIGHT_ULTRA_LIGHT;
            case 4:
            case 5:
            case 6:
                return EntityWeightClass.WEIGHT_LIGHT;
            case 7:
            case 8:
            case 9:
                return EntityWeightClass.WEIGHT_MEDIUM;
            case 10:
            case 11:
                return EntityWeightClass.WEIGHT_HEAVY;
            case 12:
                return EntityWeightClass.WEIGHT_ASSAULT;
            default:
                return EntityWeightClass.WEIGHT_SUPER_HEAVY;
        }
    }
}
