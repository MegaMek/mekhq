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
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
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
      Integer xpCost, // Always use full objects, not primitives, so we can use null checks in compatibility handlers
      // Basic Info
      String source,
      String name,
      String flavorText,
      Integer age,
      Integer xpDiscount,
      Integer minimumYear,
      Integer maximumYear,
      Double randomWeight,
      List<ATOWLifeStage> lifeStages,
      List<LifePathCategory> categories,
      Boolean isPlayerRestricted,
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
    static MMLogger LOGGER = MMLogger.create(LifePath.class);

    public LifePath {
        // Preliminary Checks
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("version cannot be null");
        }

        // Cross-Version Compatibility Handlers
        if (version.isLowerThan(MHQConstants.LAST_MILESTONE)) {
            throw new IllegalArgumentException("Unsupported version: " + version);
        }

        Version currentVersion = MHQConstants.VERSION;
        if (version.isHigherThan(currentVersion)) {
            throw new IllegalArgumentException("Unsupported version: " + version);
        }

        if (version.isLowerThan(currentVersion) || version.is(currentVersion)) {
            // Perform any necessary conversions here. Largely this will be adding new fields to the record. The is()
            // call is important, as it means these conversions will still occur for Nightly releases. Otherwise,
            // we'll end up in a situation where players on the Nightlies can't load their Life Paths any time this
            // class is changed. We only need to keep compatibility handlers until the next Milestone release, at
            // which point they can be removed.

            if (minimumYear == null) { // Added in 50.07
                LOGGER.warn("{} - {}: Minimum year is null, setting to 0", id, name);
                minimumYear = 0;
            }

            if (maximumYear == null) { // Added in 50.07
                LOGGER.warn("{} - {}: Maximum year is null, setting to 9999", id, name);
                maximumYear = 9999;
            }

            if (isPlayerRestricted == null) { // Added in 50.07
                LOGGER.warn("{} - {}: Player restriction check is null, setting to false", id, name);
                isPlayerRestricted = false;
            }

            if (randomWeight == null) { // Added in 50.07
                LOGGER.warn("{} - {}: Random weight is null, setting to 1.0", id, name);
                randomWeight = 1.0;
            }
        }

        // Data Validation Checks
        if (xpCost == null) {
            throw new IllegalArgumentException("xpCost cannot be null");
        }
        if (xpCost < 0) {
            throw new IllegalArgumentException("xpCost must be a non-negative integer");
        }
        if (source == null) {
            throw new IllegalArgumentException("source cannot be null");
        }
        if (source.isBlank()) {
            LOGGER.warn("{} - {}: Source is blank, setting to empty string", id, name);
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (flavorText == null) {
            throw new IllegalArgumentException("flavorText cannot be null");
        }
        if (flavorText.isBlank()) {
            LOGGER.warn("{} - {}: Flavor text is blank, setting to empty string", id, name);
        }
        if (age == null) {
            throw new IllegalArgumentException("age cannot be null");
        }
        if (age < 0) {
            throw new IllegalArgumentException("age must be a non-negative integer");
        }
        if (xpDiscount == null) {
            throw new IllegalArgumentException("xpDiscount cannot be null");
        }
        if (xpDiscount < 0) {
            throw new IllegalArgumentException("xpDiscount must be a non-negative integer");
        }
        if (minimumYear == null) {
            throw new IllegalArgumentException("minimumYear cannot be null");
        }
        if (maximumYear == null) {
            throw new IllegalArgumentException("maximumYear cannot be null");
        }
        if (maximumYear < minimumYear) {
            throw new IllegalArgumentException("maximumYear must be greater than or equal to minimumYear");
        }
        if (randomWeight == null) {
            throw new IllegalArgumentException("randomWeight cannot be null");
        }
        if (lifeStages == null) {
            throw new IllegalArgumentException("lifeStages cannot be null");
        }
        if (lifeStages.isEmpty()) {
            LOGGER.warn("{} - {}: Life stages is empty, setting to empty list", id, name);
        }
        if (categories == null) {
            throw new IllegalArgumentException("categories cannot be null");
        }
        if (categories.isEmpty()) {
            LOGGER.warn("{} - {}: Categories is empty, setting to empty list", id, name);
        }
        if (isPlayerRestricted == null) {
            throw new IllegalArgumentException("isPlayerRestricted cannot be null");
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
