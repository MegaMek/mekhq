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
import mekhq.campaign.personnel.skills.enums.SkillSubType;

public class LifePathXPCostCalculator {
    public static int calculateXPCost(int discount,
          Map<Integer, Map<SkillAttribute, Integer>> fixedAttributes,
          Map<Integer, Integer> fixedFlexibleAttribute,
          Map<Integer, Integer> fixedXPEdge,
          Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> fixedTraits,
          Map<Integer, Map<String, Integer>> fixedSkills,
          Map<Integer, Map<SkillSubType, Integer>> fixedMetaSkills,
          Map<Integer, Map<String, Integer>> fixedNaturalAptitudes,
          Map<Integer, Map<String, Integer>> fixedAbilities,
          int flexibleTabCount,
          int flexiblePickCount,
          Map<Integer, Map<SkillAttribute, Integer>> flexibleAttributes,
          Map<Integer, Integer> flexibleXPEdge,
          Map<Integer, Integer> flexibleFlexibleAttribute,
          Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> flexibleTraits,
          Map<Integer, Map<String, Integer>> flexibleSkills,
          Map<Integer, Map<SkillSubType, Integer>> flexibleMetaSkills,
          Map<Integer, Map<String, Integer>> flexibleNaturalAptitudes,
          Map<Integer, Map<String, Integer>> flexibleAbilities) {
        // Basic Info
        int globalCost = -discount;

        // Fixed XP
        globalCost += getCost(fixedAttributes, fixedFlexibleAttribute, fixedXPEdge, fixedTraits, fixedSkills,
              fixedMetaSkills, fixedNaturalAptitudes, fixedAbilities);

        // Flexible XP
        if (flexiblePickCount > 0) {
            int divisor = max(1, flexibleTabCount); // Prevents divide by zero errors
            int baseCost = getCost(flexibleAttributes, flexibleFlexibleAttribute, flexibleXPEdge, flexibleTraits,
                  flexibleSkills, flexibleMetaSkills, flexibleNaturalAptitudes, flexibleAbilities);
            double costPerTab = ((double) baseCost) / divisor;
            globalCost += (int) Math.round(costPerTab * flexiblePickCount);
        }

        // We can have 0 cost Life Paths, but not negative
        return max(0, globalCost);
    }

    private static int getCost(Map<Integer, Map<SkillAttribute, Integer>> attributes,
          Map<Integer, Integer> flexibleAttribute, Map<Integer, Integer> edge,
          Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> traits, Map<Integer, Map<String, Integer>> skills,
          Map<Integer, Map<SkillSubType, Integer>> metaSkills, Map<Integer, Map<String, Integer>> naturalAptitudes,
          Map<Integer, Map<String, Integer>> abilities) {
        int cost = 0;

        for (Map.Entry<Integer, Map<SkillAttribute, Integer>> entry : attributes.entrySet()) {
            Map<SkillAttribute, Integer> storage = entry.getValue();
            for (Map.Entry<SkillAttribute, Integer> attributeEntry : storage.entrySet()) {
                cost += attributeEntry.getValue();
            }
        }

        for (Map.Entry<Integer, Integer> entry : flexibleAttribute.entrySet()) {
            Integer value = entry.getValue();
            if (value != null) {
                cost += value;
            }
        }

        for (Map.Entry<Integer, Integer> entry : edge.entrySet()) {
            Integer value = entry.getValue();
            if (value != null) {
                cost += value;
            }
        }

        for (Map.Entry<Integer, Map<LifePathEntryDataTraitLookup, Integer>> entry : traits.entrySet()) {
            Map<LifePathEntryDataTraitLookup, Integer> storage = entry.getValue();
            for (Map.Entry<LifePathEntryDataTraitLookup, Integer> traitEntry : storage.entrySet()) {
                cost += traitEntry.getValue();
            }
        }

        for (Map.Entry<Integer, Map<String, Integer>> entry : skills.entrySet()) {
            Map<String, Integer> storage = entry.getValue();
            for (Map.Entry<String, Integer> skillEntry : storage.entrySet()) {
                cost += skillEntry.getValue();
            }
        }

        for (Map.Entry<Integer, Map<SkillSubType, Integer>> entry : metaSkills.entrySet()) {
            Map<SkillSubType, Integer> storage = entry.getValue();
            for (Map.Entry<SkillSubType, Integer> metaSkillEntry : storage.entrySet()) {
                cost += metaSkillEntry.getValue();
            }
        }

        for (Map.Entry<Integer, Map<String, Integer>> entry : naturalAptitudes.entrySet()) {
            Map<String, Integer> storage = entry.getValue();
            for (Map.Entry<String, Integer> skillEntry : storage.entrySet()) {
                cost += skillEntry.getValue();
            }
        }

        for (Map.Entry<Integer, Map<String, Integer>> entry : abilities.entrySet()) {
            Map<String, Integer> storage = entry.getValue();
            for (Map.Entry<String, Integer> abilityEntry : storage.entrySet()) {
                cost += abilityEntry.getValue();
            }
        }

        return cost;
    }
}
