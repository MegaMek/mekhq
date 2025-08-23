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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.Version;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

/**
 * Represents a single Life Path definition for the advanced character builder system, including identifying metadata,
 * requirements, awards, and exclusion logic.
 *
 * @author Illiani
 * @since 0.50.07
 */
public record LifePath(
      // Dynamic
      UUID id,
      Version version,
      int xpCost,
      // Basic Info
      String source,
      String name,
      String flavorText,
      int age,
      int xpDiscount,
      List<ATOWLifeStage> lifeStages,
      List<LifePathCategory> categories,
      // Requirements
      Map<Integer, List<String>> requirementsFactions,
      Map<Integer, List<UUID>> requirementsLifePath,
      Map<Integer, Map<LifePathCategory, Integer>> requirementsCategories,
      Map<Integer, Map<SkillAttribute, Integer>> requirementsAttributes,
      Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> requirementsTraits,
      Map<Integer, Map<String, Integer>> requirementsSkills,
      Map<Integer, Map<String, Integer>> requirementsAbilities,
      // Exclusions
      Map<Integer, List<String>> exclusionsFactions,
      Map<Integer, List<UUID>> exclusionsLifePath,
      Map<Integer, Map<LifePathCategory, Integer>> exclusionsCategories,
      Map<Integer, Map<SkillAttribute, Integer>> exclusionsAttributes,
      Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> exclusionsTraits,
      Map<Integer, Map<String, Integer>> exclusionsSkills,
      Map<Integer, Map<String, Integer>> exclusionsAbilities,
      // Fixed XP
      Map<Integer, Map<SkillAttribute, Integer>> fixedXPAttributes,
      Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> fixedXPTraits,
      Map<Integer, Map<String, Integer>> fixedXPSkills,
      Map<Integer, Map<String, Integer>> fixedXPAbilities,
      // Flexible XP
      Map<Integer, Map<SkillAttribute, Integer>> flexibleXPAttributes,
      Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> flexibleXPTraits,
      Map<Integer, Map<String, Integer>> flexibleXPSkills,
      Map<Integer, Map<String, Integer>> flexibleXPAbilities,
      int flexibleXPPickCount
) {
    public LifePath {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("version cannot be null");
        }
        if (xpCost < 0) {
            throw new IllegalArgumentException("xpCost must be a non-negative integer");
        }
        if (source == null) {
            throw new IllegalArgumentException("source cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (flavorText == null) {
            throw new IllegalArgumentException("flavorText cannot be null");
        }
        if (age < 0) {
            throw new IllegalArgumentException("age must be a non-negative integer");
        }
        if (xpDiscount < 0) {
            throw new IllegalArgumentException("xpDiscount must be a non-negative integer");
        }
        if (lifeStages == null) {
            throw new IllegalArgumentException("lifeStages cannot be null");
        }
        if (categories == null) {
            throw new IllegalArgumentException("categories cannot be null");
        }
        if (requirementsFactions == null) {
            throw new IllegalArgumentException("requirementsFactions cannot be null");
        }
        if (requirementsLifePath == null) {
            throw new IllegalArgumentException("requirementsLifePath cannot be null");
        }
        if (requirementsCategories == null) {
            throw new IllegalArgumentException("requirementsCategories cannot be null");
        }
        if (requirementsAttributes == null) {
            throw new IllegalArgumentException("requirementsAttributes cannot be null");
        }
        if (requirementsTraits == null) {
            throw new IllegalArgumentException("requirementsTraits cannot be null");
        }
        if (requirementsSkills == null) {
            throw new IllegalArgumentException("requirementsSkills cannot be null");
        }
        if (requirementsAbilities == null) {
            throw new IllegalArgumentException("requirementsAbilities cannot be null");
        }
        if (exclusionsFactions == null) {
            throw new IllegalArgumentException("exclusionsFactions cannot be null");
        }
        if (exclusionsLifePath == null) {
            throw new IllegalArgumentException("exclusionsLifePath cannot be null");
        }
        if (exclusionsCategories == null) {
            throw new IllegalArgumentException("exclusionsCategories cannot be null");
        }
        if (exclusionsAttributes == null) {
            throw new IllegalArgumentException("exclusionsAttributes cannot be null");
        }
        if (exclusionsTraits == null) {
            throw new IllegalArgumentException("exclusionsTraits cannot be null");
        }
        if (exclusionsSkills == null) {
            throw new IllegalArgumentException("exclusionsSkills cannot be null");
        }
        if (exclusionsAbilities == null) {
            throw new IllegalArgumentException("exclusionsAbilities cannot be null");
        }
        if (fixedXPAttributes == null) {
            throw new IllegalArgumentException("fixedXPAttributes cannot be null");
        }
        if (fixedXPTraits == null) {
            throw new IllegalArgumentException("fixedXPTraits cannot be null");
        }
        if (fixedXPSkills == null) {
            throw new IllegalArgumentException("fixedXPSkills cannot be null");
        }
        if (fixedXPAbilities == null) {
            throw new IllegalArgumentException("fixedXPAbilities cannot be null");
        }
        if (flexibleXPAttributes == null) {
            throw new IllegalArgumentException("flexibleXPAttributes cannot be null");
        }
        if (flexibleXPTraits == null) {
            throw new IllegalArgumentException("flexibleXPTraits cannot be null");
        }
        if (flexibleXPSkills == null) {
            throw new IllegalArgumentException("flexibleXPSkills cannot be null");
        }
        if (flexibleXPAbilities == null) {
            throw new IllegalArgumentException("flexibleXPAbilities cannot be null");
        }
        if (flexibleXPPickCount < 0) {
            throw new IllegalArgumentException("flexibleXPPickCount must be a non-negative integer");
        }

        int groupCount = 0;
        groupCount += flexibleXPAttributes.size();
        groupCount += flexibleXPTraits.size();
        groupCount += flexibleXPSkills.size();
        groupCount += flexibleXPAbilities.size();
        if (flexibleXPPickCount > groupCount) {
            throw new IllegalArgumentException(
                  "flexibleXPPickCount must be less than or equal to the total number of flexible XP award groups");
        }

    }

}
