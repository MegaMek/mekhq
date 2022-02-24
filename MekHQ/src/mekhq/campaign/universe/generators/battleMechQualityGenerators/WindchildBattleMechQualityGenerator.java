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
package mekhq.campaign.universe.generators.battleMechQualityGenerators;

import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.enums.BattleMechQualityGenerationMethod;

/**
 * @author Justin "Windchild" Bowen
 */
public class WindchildBattleMechQualityGenerator extends AbstractBattleMechQualityGenerator {
    //region Constructors
    public WindchildBattleMechQualityGenerator() {
        super(BattleMechQualityGenerationMethod.WINDCHILD);
    }
    //endregion Constructors

    @Override
    public int generate(final int roll) {
        switch (roll) {
            case 2:
            case 3:
            case 4:
                return IUnitRating.DRAGOON_F;
            case 5:
            case 6:
                return IUnitRating.DRAGOON_D;
            case 7:
            case 8:
                return IUnitRating.DRAGOON_C;
            case 9:
            case 10:
                return IUnitRating.DRAGOON_B;
            case 11:
            case 12:
                return IUnitRating.DRAGOON_A;
            default:
                return IUnitRating.DRAGOON_ASTAR;
        }
    }
}
