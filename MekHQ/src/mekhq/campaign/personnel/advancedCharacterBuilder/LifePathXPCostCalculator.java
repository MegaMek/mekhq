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

import java.util.Map;

import mekhq.campaign.personnel.skills.enums.SkillAttribute;

public class LifePathXPCostCalculator {
    public static int calculateXPCost(int discount,
          Map<Integer, Map<SkillAttribute, Integer>> fixedAttributes,
          Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> fixedTraits,
          Map<Integer, Map<String, Integer>> fixedSkills,
          Map<Integer, Map<String, Integer>> fixedAbilities,
          int flexibleTabCount, int flexiblePickCount,
          Map<Integer, Map<SkillAttribute, Integer>> flexibleAttributes,
          Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> flexibleTraits,
          Map<Integer, Map<String, Integer>> flexibleSkills,
          Map<Integer, Map<String, Integer>> flexibleAbilities) {
        // Basic Info
        int globalCost = -discount;

        // Fixed XP
        globalCost += getCost(fixedAttributes, fixedTraits, fixedSkills, fixedAbilities);

        // Flexible XP
        if (flexiblePickCount > 0) {
            int divisor = max(1, flexibleTabCount);
            int baseCost = getCost(flexibleAttributes, flexibleTraits, flexibleSkills, flexibleAbilities);
            double costPerTab = ((double) baseCost) / flexibleTabCount;
            globalCost += (int) Math.round(costPerTab * flexiblePickCount);

        }

        // We can have 0 cost Life Paths, but not negative
        return max(0, globalCost);
    }

    private static int getCost(Map<Integer, Map<SkillAttribute, Integer>> fixedAttributes,
          Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> fixedTraits,
          Map<Integer, Map<String, Integer>> fixedSkills, Map<Integer, Map<String, Integer>> fixedAbilities) {
        int cost = 0;

        for (Map.Entry<Integer, Map<SkillAttribute, Integer>> entry : fixedAttributes.entrySet()) {
            Map<SkillAttribute, Integer> storage = entry.getValue();
            for (Map.Entry<SkillAttribute, Integer> attributeEntry : storage.entrySet()) {
                cost += attributeEntry.getValue();
            }
        }

        for (Map.Entry<Integer, Map<LifePathEntryDataTraitLookup, Integer>> entry : fixedTraits.entrySet()) {
            Map<LifePathEntryDataTraitLookup, Integer> storage = entry.getValue();
            for (Map.Entry<LifePathEntryDataTraitLookup, Integer> traitEntry : storage.entrySet()) {
                cost += traitEntry.getValue();
            }
        }

        for (Map.Entry<Integer, Map<String, Integer>> entry : fixedSkills.entrySet()) {
            Map<String, Integer> storage = entry.getValue();
            for (Map.Entry<String, Integer> traitEntry : storage.entrySet()) {
                cost += traitEntry.getValue();
            }
        }

        for (Map.Entry<Integer, Map<String, Integer>> entry : fixedAbilities.entrySet()) {
            Map<String, Integer> storage = entry.getValue();
            for (Map.Entry<String, Integer> traitEntry : storage.entrySet()) {
                cost += traitEntry.getValue();
            }
        }

        return cost;
    }
}
