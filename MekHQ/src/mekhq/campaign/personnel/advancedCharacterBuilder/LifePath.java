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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.Version;
import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;

/**
 * Represents a single Life Path definition for the advanced character builder system, including identifying metadata,
 * requirements, awards, and exclusion logic.
 *
 * @author Illiani
 * @since 0.50.07
 */
public record LifePath(UUID id,
      String source,
      Version version,
      String name,
      String flavorText,
      int age,
      int xpDiscount,
      int xpCost,
      List<ATOWLifeStage> lifeStages,
      List<LifePathCategory> categories,
      Map<Integer, List<LifePathEntryData>> requirements,
      List<LifePathEntryData> exclusions,
      List<LifePathEntryData> fixedXpAwards,
      Map<Integer, List<LifePathEntryData>> flexibleXpAwards,
      int pickCount
) {
    private static final MMLogger LOGGER = MMLogger.create(LifePath.class);

    /**
     * Constructs a {@link LifePath} from raw string and list inputs, translating all fields into their strong typed
     * representations.
     *
     * <p>This factory method validates all inputs and converts string-based data into the proper types required by
     * the record, throwing exceptions on error.</p>
     *
     * @param rawID                 The unique Life Path ID as a string (must be a valid UUID).
     * @param source                The source of the Life Path (I.e., what manual it's from).
     * @param rawVersion            The version string (must be a valid {@link Version}).
     * @param name                  The user-facing name of the Life Path.
     * @param flavorText            The user-facing narrative for the Life Path.
     * @param rawAge                Years added to character's age for this Life Path.
     * @param rawXPDiscount         Discount applied to XP gains.
     * @param rawXPCost             XP cost of taking this Life Path.
     * @param rawLifeStages         List of life stage names.
     * @param rawCategories         List of category names.
     * @param rawRequirements       Map of integer keys to lists of requirement strings.
     * @param rawExclusions         List of exclusion entry strings.
     * @param rawFixedXpAwards      List of fixed XP award entry strings.
     * @param rawSelectableXPAwards Map of integer keys to lists of selectable XP award strings.
     * @param rawPickCount          How many times flexible XP awards can be selected.
     *
     * @return a new {@link LifePath} parsed and validated from the provided inputs.
     *
     * @throws IllegalArgumentException if any input is invalid or cannot be converted.
     * @author Illiani
     * @since 0.50.07
     */
    public static LifePath fromRawEntry(@Nullable String rawID, @Nullable String source,
          @Nullable String rawVersion, @Nullable String name, @Nullable String flavorText, @Nullable String rawAge,
          @Nullable String rawXPDiscount, @Nullable String rawXPCost, @Nullable List<String> rawLifeStages,
          @Nullable List<String> rawCategories, @Nullable Map<Integer, List<String>> rawRequirements,
          @Nullable List<String> rawExclusions, @Nullable List<String> rawFixedXpAwards,
          @Nullable Map<Integer, List<String>> rawSelectableXPAwards, @Nullable String rawPickCount) {
        // ID
        UUID id;
        if (rawID != null) {
            try {
                id = UUID.fromString(rawID);
            } catch (IllegalArgumentException e) {
                id = UUID.randomUUID();
                LOGGER.warn("Invalid ID provided for LifePathRecord, generating random UUID");
            }
        } else {
            id = UUID.randomUUID();
            LOGGER.info("No ID provided for LifePathRecord, generating random UUID");
        }

        // Source
        if (source == null) {
            source = "";
            LOGGER.info("No source provided for LifePathRecord, using empty string");
        }

        // Version
        Version version;
        if (rawVersion != null) {
            try {
                version = new Version(rawVersion);
            } catch (IllegalArgumentException e) {
                version = MHQConstants.VERSION;
                LOGGER.warn("Invalid version provided for {}, using default version: {}", id.toString(), version);
            }
        } else {
            version = MHQConstants.VERSION;
            LOGGER.info("No version provided for {}, using default version: {}", id.toString(), version);
        }

        // Name
        if (name == null) {
            name = "";
            LOGGER.info("No name provided for {}, using empty string", id.toString());
        }

        // Flavor Text
        if (flavorText == null) {
            flavorText = "";
            LOGGER.info("No flavor text provided for {}, using empty string", id.toString());
        }

        // Age
        int age;
        if (rawAge != null) {
            age = MathUtility.parseInt(rawAge);
            if (age < 0) {
                LOGGER.warn("Age provided for {} is negative, using 0", id.toString());
                age = 0;
            }
        } else {
            LOGGER.info("No age provided for {}, using 0", id.toString());
            age = 0;
        }

        // XP Discount
        int xpDiscount;
        if (rawXPDiscount != null) {
            xpDiscount = MathUtility.parseInt(rawXPDiscount);
            if (xpDiscount < 0) {
                LOGGER.warn("XP discount provided for {} is negative, using 0", id.toString());
                xpDiscount = 0;
            }
        } else {
            LOGGER.info("No XP discount provided for {}, using 0", id.toString());
            xpDiscount = 0;
        }

        // XP Cost
        int xpCost;
        if (rawXPCost != null) {
            xpCost = MathUtility.parseInt(rawXPCost);
            if (xpCost < 0) {
                LOGGER.warn("XP cost provided for {} is negative, using 0", id.toString());
                xpCost = 0;
            }
        } else {
            LOGGER.info("No XP cost provided for {}, using 0", id.toString());
            xpCost = 0;
        }

        // Life Stages
        List<ATOWLifeStage> lifeStages = new ArrayList<>();
        if (rawLifeStages != null) {
            for (String rawLifeStage : rawLifeStages) {
                ATOWLifeStage stage = ATOWLifeStage.fromLookupName(rawLifeStage);
                if (stage == null) {
                    LOGGER.warn("Unknown life stage: {}", rawLifeStage);
                    continue;
                }

                lifeStages.add(stage);
            }
        } else {
            LOGGER.info("No life stages provided for {}", id.toString());
        }

        // Categories
        List<LifePathCategory> categories = new ArrayList<>();
        if (rawCategories != null) {
            for (String rawCategory : rawCategories) {
                LifePathCategory category = LifePathCategory.fromLookupName(rawCategory);
                if (category == null) {
                    LOGGER.warn("Unknown life path category: {}", rawCategory);
                } else {
                    categories.add(category);
                }
            }
        } else {
            LOGGER.info("No life path categories provided for {}", id.toString());
        }

        // Requirements
        Map<Integer, List<LifePathEntryData>> requirements = new HashMap<>();
        if (rawRequirements != null) {
            requirements = translateLifePathEntryMap(rawRequirements);
        } else {
            LOGGER.info("No requirements provided for {}", id.toString());
        }

        // Exclusions
        List<LifePathEntryData> exclusions = new ArrayList<>();
        if (rawExclusions != null) {
            for (String exclusion : rawExclusions) {
                exclusions.add(LifePathEntryData.fromRawEntry(exclusion));
            }
        } else {
            LOGGER.info("No exclusions provided for {}", id.toString());
        }

        // Fixed XP Awards
        List<LifePathEntryData> fixedXPAwards = new ArrayList<>();
        if (rawFixedXpAwards != null) {
            for (String xpAward : rawFixedXpAwards) {
                fixedXPAwards.add(LifePathEntryData.fromRawEntry(xpAward));
            }
        } else {
            LOGGER.info("No fixed XP awards provided for {}", id.toString());
        }

        // Selectable XP Awards
        Map<Integer, List<LifePathEntryData>> selectableXPAwards = new HashMap<>();
        if (rawSelectableXPAwards != null) {
            selectableXPAwards = translateLifePathEntryMap(rawSelectableXPAwards);
        } else {
            LOGGER.info("No selectable XP awards provided for {}", id.toString());
        }

        // Pick Count
        int pickCount;
        if (rawPickCount != null) {
            pickCount = MathUtility.parseInt(rawPickCount);
            if (pickCount < 0) {
                LOGGER.warn("Pick count provided for {} is negative, using 0", id.toString());
                pickCount = 0;
            }

            int selectableXPAwardSize = selectableXPAwards.size();
            if (pickCount > selectableXPAwardSize) {
                LOGGER.warn("Pick count provided for {} is greater than the number of selectable XP awards, using {}",
                      id.toString(), selectableXPAwardSize);
                pickCount = selectableXPAwardSize;
            }
        } else {
            LOGGER.info("No pick count provided for {}, using 0", id.toString());
            pickCount = 0;
        }

        return new LifePath(id, source, version, name, flavorText, age, xpDiscount, xpCost, lifeStages,
              categories, requirements, exclusions, fixedXPAwards, selectableXPAwards, pickCount);
    }

    /**
     * Helper method that translates a map of integer keys and string list values into a map of integer keys and
     * {@link LifePathEntryData} list values.
     *
     * @param rawMap Map with integer keys and a list of raw entry strings as values.
     *
     * @return Map with integer keys and list of {@link LifePathEntryData} values.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static Map<Integer, List<LifePathEntryData>> translateLifePathEntryMap(Map<Integer, List<String>> rawMap) {
        Map<Integer, List<LifePathEntryData>> translatedMap = new HashMap<>();

        for (Map.Entry<Integer, List<String>> entry : rawMap.entrySet()) {
            List<LifePathEntryData> data = new ArrayList<>();
            for (String rawData : entry.getValue()) {
                data.add(LifePathEntryData.fromRawEntry(rawData));
            }
            translatedMap.put(entry.getKey(), data);
        }

        return translatedMap;
    }

    /**
     * Canonical constructor for {@link LifePath}.
     *
     * <p>Validates that no reference field is {@code null} when constructing this record; throws an
     * {@link IllegalArgumentException} if any are null.</p>
     *
     * @param id               Unique identifier for this Life Path.
     * @param source           Source/manual reference.
     * @param version          Last updated version.
     * @param name             User-visible name.
     * @param flavorText       Description/fluff text.
     * @param age              Number of years added by this path.
     * @param xpDiscount       XP discount value.
     * @param xpCost           XP cost value.
     * @param lifeStages       List of valid life stages.
     * @param categories       Life path categories.
     * @param requirements     Requirements map.
     * @param exclusions       Exclusions list.
     * @param fixedXpAwards    Fixed XP awards.
     * @param flexibleXpAwards Selectable XP award options.
     *
     * @throws IllegalArgumentException if any argument except primitives is null
     * @author Illiani
     * @since 0.50.07
     */
    public LifePath {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("source cannot be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("version cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (flavorText == null) {
            throw new IllegalArgumentException("flavorText cannot be null");
        }
        if (lifeStages == null) {
            throw new IllegalArgumentException("lifeStages cannot be null");
        }
        if (categories == null) {
            throw new IllegalArgumentException("categories cannot be null");
        }
        if (requirements == null) {
            throw new IllegalArgumentException("requirements cannot be null");
        }
        if (exclusions == null) {
            throw new IllegalArgumentException("exclusions cannot be null");
        }
        if (fixedXpAwards == null) {
            throw new IllegalArgumentException("fixedXpAwards cannot be null");
        }
        if (flexibleXpAwards == null) {
            throw new IllegalArgumentException("flexibleXpAwards cannot be null");
        }
        if (age < 0) {
            throw new IllegalArgumentException("age must be a non-negative integer");
        }
        if (xpDiscount < 0) {
            throw new IllegalArgumentException("xpDiscount must be a non-negative integer");
        }
        if (xpCost < 0) {
            throw new IllegalArgumentException("xpCost must be a non-negative integer");
        }
        if (pickCount < 0) {
            throw new IllegalArgumentException("pickCount must be a non-negative integer");
        }
        if (pickCount > flexibleXpAwards.size()) {
            throw new IllegalArgumentException(
                  "pickCount must be less than or equal to the number of selectable XP awards");
        }
    }
}
