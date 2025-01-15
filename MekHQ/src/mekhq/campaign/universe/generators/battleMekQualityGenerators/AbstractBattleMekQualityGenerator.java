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
package mekhq.campaign.universe.generators.battleMekQualityGenerators;

import mekhq.campaign.universe.enums.BattleMekQualityGenerationMethod;

/**
 * This was designed to provide options for the method of quality generation for Company Generators,
 * and any use outside of them should take this specific design into consideration.
 * @author Justin "Windchild" Bowen
 */
public abstract class AbstractBattleMekQualityGenerator {
    //region Variable Declarations
    private final BattleMekQualityGenerationMethod method;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractBattleMekQualityGenerator(final BattleMekQualityGenerationMethod method) {
        this.method = method;
    }
    //endregion Constructors

    //region Getters
    public BattleMekQualityGenerationMethod getMethod() {
        return method;
    }
    //endregion Getters

    /**
     * @param roll the modified roll to use
     * @return the generated IUnitRating magic int for Dragoon Quality
     */
    public abstract int generate(int roll);
}
