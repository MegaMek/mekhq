/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.advancedCharacterBuilder;

import static java.lang.Math.max;

import java.util.Collection;
import java.util.Map;

public class LifePathXPCostCalculator {
    public static int calculateXPCost(int discount, LifePathTabStorage fixedXPStorage,
          Map<Integer, LifePathTabStorage> flexibleXPStorage) {
        // Basic Info
        int globalCost = -discount;

        // Fixed XP
        globalCost = getCost(fixedXPStorage, globalCost);

        // Flexible XP
        int runningCost = 0;
        int length = flexibleXPStorage.size();
        for (Map.Entry<Integer, LifePathTabStorage> entry : flexibleXPStorage.entrySet()) {
            LifePathTabStorage storage = entry.getValue();

            runningCost = getCost(storage, runningCost);
        }
        int meanCost = runningCost / length;

        // Total and return
        globalCost += meanCost;

        // We can have 0 cost Life Paths, but not negative
        return max(0, globalCost);
    }

    private static int getCost(LifePathTabStorage fixedXPStorage, int globalCost) {
        Collection<Integer> fixedAttributes = fixedXPStorage.attributes().values();
        globalCost += fixedAttributes.stream().mapToInt(Integer::intValue).sum();

        Collection<Integer> fixedTraits = fixedXPStorage.traits().values();
        globalCost += fixedTraits.stream().mapToInt(Integer::intValue).sum();

        Collection<Integer> fixedSkills = fixedXPStorage.skills().values();
        globalCost += fixedSkills.stream().mapToInt(Integer::intValue).sum();

        Collection<Integer> fixedAbilities = fixedXPStorage.abilities().values();
        globalCost += fixedAbilities.stream().mapToInt(Integer::intValue).sum();
        return globalCost;
    }
}
