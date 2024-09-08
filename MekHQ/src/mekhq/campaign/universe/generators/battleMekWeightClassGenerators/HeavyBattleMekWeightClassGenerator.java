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
public class HeavyBattleMekWeightClassGenerator extends AbstractBattleMekWeightClassGenerator {
    //region Constructors
    public HeavyBattleMekWeightClassGenerator() {
        super(BattleMechWeightClassGenerationMethod.HEAVY);
    }
    //endregion Constructors

    @Override
    public int generate(final int roll) {
        return EntityWeightClass.WEIGHT_HEAVY;
    }
}
