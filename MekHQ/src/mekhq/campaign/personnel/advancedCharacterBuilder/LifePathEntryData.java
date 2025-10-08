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

import static megamek.codeUtilities.MathUtility.clamp;
import static mekhq.campaign.personnel.Person.*;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.ATOW_TRAIT;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.FACTION_CODE;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.LIFE_PATH;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.LIFE_PATH_CATEGORY;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.SKILL;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.SKILL_ATTRIBUTE;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathDataClassLookup.SPA;
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_ATTRIBUTE_SCORE;

import java.util.UUID;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

/**
 * Represents a single piece of life path data for use in the advanced character builder, encapsulating an identifying
 * class name, object name, and value.
 *
 * <p>This record is used for the flexible storage and retrieval of various life path data points, such as traits,
 * skills, skill attributes, special abilities, and categories, as part of Advanced Character Builder operations.</p>
 *
 * @param classLookupName  The lookup name of the data's class/category (e.g., skill, trait, etc.).
 * @param objectLookupName The specific lookup name for the object (e.g., name of skill or trait).
 * @param value            The data value (may represent a rating, amount, or boolean value as int).
 *
 * @author Illiani
 * @since 0.50.07
 */
public record LifePathEntryData(
      String classLookupName,
      String objectLookupName,
      int value
) {
    private static final MMLogger LOGGER = MMLogger.create(LifePathEntryData.class);

    /**
     * Creates a new {@link LifePathEntryData} instance from a raw string entry.
     *
     * <p>The input string is expected to be in the format {@code "classLookupName::objectLookupName::value"}.</p>
     *
     * @param rawLifePathEntry the raw life path entry string to parse
     *
     * @return a new {@link LifePathEntryData} instance representing the parsed values
     *
     * @throws ArrayIndexOutOfBoundsException if the input string does not contain three parts
     * @throws NumberFormatException          if the value segment cannot be parsed as an integer
     * @author Illiani
     * @since 0.50.07
     */
    public static LifePathEntryData fromRawEntry(String rawLifePathEntry) {
        String[] parts = rawLifePathEntry.split("::", 3);
        return new LifePathEntryData(parts[0], parts[1], MathUtility.parseInt(parts[2]));
    }

    /**
     * Canonical constructor for {@link LifePathEntryData} with null checks.
     *
     * <p>Ensures that {@code classLookupName} and {@code objectLookupName} are not null when creating a record
     * instance.</p>
     *
     * @param classLookupName  the lookup name of the data's class/category
     * @param objectLookupName the specific lookup name for the object
     * @param value            the value for this entry
     *
     * @throws IllegalArgumentException if either {@code classLookupName} or {@code objectLookupName} is null
     * @author Illiani
     * @since 0.50.07
     */
    public LifePathEntryData {
        if (classLookupName == null) {
            throw new IllegalArgumentException("classLookupName cannot be null");
        }

        if (objectLookupName == null) {
            throw new IllegalArgumentException("objectLookupName cannot be null");
        }
    }

    /**
     * Retrieves the value for a specific trait if this instance represents that trait.
     *
     * @param trait The {@link LifePathEntryDataTraitLookup} to fetch.
     *
     * @return The trait value (possibly clamped or minimum), or 0 if not matching.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getTrait(LifePathEntryDataTraitLookup trait) {
        if (!ATOW_TRAIT.getLookupName().equalsIgnoreCase(classLookupName)) {
            return 0;
        }

        return getTraitValue(trait, !trait.getLookupName().equalsIgnoreCase(objectLookupName));
    }

    /**
     * Helper for trait value lookups, with minimum value handling.
     *
     * @param trait     The trait to fetch.
     * @param isMinimum Whether to return the minimum allowed value.
     *
     * @return The value clamped/minimum or 0 for unknown traits.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private int getTraitValue(LifePathEntryDataTraitLookup trait, boolean isMinimum) {
        return switch (trait) {
            case BLOODMARK -> isMinimum ? MINIMUM_BLOODMARK : clamp(value, MINIMUM_BLOODMARK, MAXIMUM_BLOODMARK);
            case CONNECTIONS ->
                  isMinimum ? MINIMUM_CONNECTIONS : clamp(value, MINIMUM_CONNECTIONS, MAXIMUM_CONNECTIONS);
            //            case ENEMY -> isMinimum ? MINIMUM_ENEMY : clamp(value, MINIMUM_ENEMY, MAXIMUM_ENEMY);
            //            case EXTRA_INCOME -> isMinimum ? MINIMUM_EXTRA_INCOME : clamp(value, MINIMUM_EXTRA_INCOME,
            //                  MAXIMUM_EXTRA_INCOME);
            //            case PROPERTY -> isMinimum ? MINIMUM_PROPERTY : max(value, MINIMUM_PROPERTY); // Has no maximum value
            case REPUTATION -> isMinimum ? MINIMUM_REPUTATION : clamp(value, MINIMUM_REPUTATION, MAXIMUM_REPUTATION);
            // case TITLE -> isMinimum ? MINIMUM_TITLE : clamp(value, MINIMUM_TITLE, MAXIMUM_TITLE);
            case UNLUCKY -> isMinimum ? MINIMUM_UNLUCKY : clamp(value, MINIMUM_UNLUCKY, MAXIMUM_UNLUCKY);
            case WEALTH -> isMinimum ? MINIMUM_WEALTH : clamp(value, MINIMUM_WEALTH, MAXIMUM_WEALTH);
        };
    }

    /**
     * Retrieves the faction code associated with the current instance if it matches the specified lookup name
     * criteria.
     *
     * @return the faction code (objectLookupName) if the classLookupName matches FACTION_CODE.getLookupName() ignoring
     *       case, or {@code null} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable String getFactionCode() {
        if (!FACTION_CODE.getLookupName().equalsIgnoreCase(classLookupName)) {
            return null;
        }

        return objectLookupName;
    }

    /**
     * Returns the UUID for a specific life path if this instance represents a life path.
     *
     * @return The {@link UUID} if valid and present, or {@code null} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable UUID getLifePathUUID() {
        if (!LIFE_PATH.getLookupName().equalsIgnoreCase(classLookupName)) {
            return null;
        }

        try {
            // We return the specific UUID, not boolean for whether it's present, as this is better for checking
            // against a map of Life Path UUIDs. I.e., we can fetch the UUID and then cross-reference it in the map.
            return UUID.fromString(objectLookupName);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid life path UUID provided: {}", objectLookupName);
            return null;
        }
    }

    /**
     * Retrieves a value for a life path category if this instance represents that category and matches.
     *
     * @param category The {@link LifePathCategory} to fetch.
     *
     * @return The value stored or 0 if unmatched.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getLifePathCategory(LifePathCategory category) {
        if (!LIFE_PATH_CATEGORY.getLookupName().equalsIgnoreCase(classLookupName)) {
            return 0;
        }

        if (!category.getLookupName().equalsIgnoreCase(objectLookupName)) {
            return 0;
        }

        return value;
    }

    /**
     * Retrieves the value for a given skill name if this instance represents that skill.
     *
     * @param skillName The name of the skill to fetch.
     *
     * @return The value associated with the skill, or 0 if no match.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getSkill(String skillName) {
        if (!SKILL.getLookupName().equalsIgnoreCase(classLookupName)) {
            return 0;
        }

        if (!skillName.equalsIgnoreCase(objectLookupName)) {
            return 0;
        }

        // Does the skillName correspond to an actual skill?
        SkillType skillType = SkillType.getSkillHash().get(skillName);
        if (skillType == null) {
            return 0;
        }

        return value;
    }

    /**
     * Gets a skill attribute modifier if this instance represents that attribute.
     *
     * @param attribute The {@link SkillAttribute} to fetch.
     *
     * @return The attributed value clamped to allowed range, or minimum if unmatched.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getSkillAttribute(SkillAttribute attribute) {
        if (!SKILL_ATTRIBUTE.getLookupName().equalsIgnoreCase(classLookupName)) {
            return MINIMUM_ATTRIBUTE_SCORE;
        }

        if (attribute == SkillAttribute.NONE) {
            return 0;
        }

        if (!attribute.getLookupName().equalsIgnoreCase(objectLookupName)) {
            return MINIMUM_ATTRIBUTE_SCORE;
        }

        return clamp(value, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
    }

    /**
     * Retrieves a special ability (SPA) option if this instance represents an SPA.
     *
     * @param lookupName The binary name of the SPA to look up.
     *
     * @return The {@link IOption} instance, or null if not an SPA.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable IOption getSPA(String lookupName) {
        if (!SPA.getLookupName().equalsIgnoreCase(classLookupName)) {
            return null;
        }

        PersonnelOptions options = new PersonnelOptions();
        return options.getOption(lookupName);
    }
}
