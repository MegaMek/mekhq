/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.universe.generators.battleMekWeightClassGenerators;

import mekhq.campaign.universe.enums.BattleMekWeightClassGenerationMethod;

/**
 * This was designed to provide options for the method of weight generation for Company Generators,
 * and any use outside of them should take this specific design into consideration.
 * @author Justin "Windchild" Bowen
 */
public abstract class AbstractBattleMekWeightClassGenerator {
    //region Variable Declarations
    private final BattleMekWeightClassGenerationMethod method;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractBattleMekWeightClassGenerator(final BattleMekWeightClassGenerationMethod method) {
        this.method = method;
    }
    //endregion Constructors

    //region Getters
    public BattleMekWeightClassGenerationMethod getMethod() {
        return method;
    }
    //endregion Getters

    /**
     * @param roll the modified roll to use
     * @return the generated EntityWeightClass, returning EntityWeightClass.WEIGHT_ULTRA_LIGHT to
     * signify no generation and EntityWeightClass.WEIGHT_SUPER_HEAVY to signify Star League-era
     * generation.
    */
    public abstract int generate(int roll);
}
