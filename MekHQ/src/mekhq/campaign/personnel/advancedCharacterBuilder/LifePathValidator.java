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

import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.FLEXIBLE_PICKS_EQUAL_ITEMS;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.MISSING_FACTION;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.NO_FLEXIBLE_PICKS;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.TOO_MANY_FLEXIBLE_PICKS;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.codeUtilities.StringUtility;

/**
 * Checks a Life Path for the mistakes an author can make that the record's own constructor does not catch.
 *
 * <p>The record constructor rejects values that make a Life Path unloadable, such as a null map or a negative XP
 * cost. This class covers the next layer up: a path that loads perfectly well but cannot work, such as an
 * affiliation stage with no faction attached, or more flexible picks than there are groups to pick from.</p>
 *
 * <p>There are two entry points because validation happens at two moments. {@link #validate(LifePath)} checks a
 * finished record and is used when files are read from disk. {@link #validate(LifePathBuilder)} checks the wizard's
 * working state, which has to be validated <em>before</em> a record is built, because the record's constructor would
 * throw first and the author would see a stack trace instead of a list of problems.</p>
 *
 * @since 0.50.11
 */
public class LifePathValidator {
    private final LifePathBuilder lifePath;
    private final Set<InvalidLifePathReason> invalidReasons = new HashSet<>();

    /**
     * Returns every problem found with the Life Path that was validated.
     *
     * @return the reasons this Life Path is invalid, empty when it is valid
     *
     * @since 0.50.11
     */
    public Set<InvalidLifePathReason> getInvalidReasons() {
        return invalidReasons;
    }

    /**
     * Validates a finished {@link LifePath}.
     *
     * <p>Use this for records that already exist, such as the files read at campaign load. A record that reaches
     * this method has already satisfied its own constructor, so anything reported here is an authoring mistake
     * rather than a corrupt file.</p>
     *
     * @param lifePath the Life Path to check
     *
     * @return the reasons it is invalid, empty when it is valid
     *
     * @since 0.50.11
     */
    public static Set<InvalidLifePathReason> validate(LifePath lifePath) {
        return validate(LifePathBuilder.from(lifePath));
    }

    /**
     * Validates a Life Path that is still being assembled.
     *
     * <p>This is what the wizard uses. It cannot hand over a {@link LifePath}, because the record's constructor
     * throws on the very values this check exists to report, and the author would see a stack trace instead of a
     * list of things to fix.</p>
     *
     * @param lifePath the Life Path under construction
     *
     * @return the reasons it is invalid, empty when it is valid
     *
     * @since 0.50.11
     */
    public static Set<InvalidLifePathReason> validate(LifePathBuilder lifePath) {
        return new LifePathValidator(lifePath).getInvalidReasons();
    }

    /**
     * Collapses a group-keyed map of Life Path id sets into one set of ids.
     *
     * @param groupedIds Life Path ids keyed by group index
     *
     * @return every id in every group
     *
     * @since 0.50.11
     */
    private static Set<UUID> flattenIds(Map<Integer, Set<UUID>> groupedIds) {
        Set<UUID> allIds = new HashSet<>();

        for (Set<UUID> group : groupedIds.values()) {
            if (group != null) {
                allIds.addAll(group);
            }
        }

        return allIds;
    }

    private LifePathValidator(LifePathBuilder lifePath) {
        this.lifePath = lifePath;

        // Tests
        checkYears();
        checkFlexiblePicks();
        checkAffiliationFactionRequirement();
        checkSource();
        checkName();
        checkLifeStages();
        checkCategories();
        checkLifePathRequirements();
        checkLifePathExclusions();
        checkContradictions();
    }

    /**
     * Flags a Life Path whose earliest year is later than its latest year.
     *
     * <p>Equal years are allowed: a Life Path available in exactly one year is legitimate.</p>
     *
     * @since 0.50.11
     */
    private void checkYears() {
        if (lifePath.minimumYear() > lifePath.maximumYear()) {
            invalidReasons.add(InvalidLifePathReason.MIN_YEAR_ABOVE_MAX_YEAR);
        }
    }

    /**
     * Flags every flexible XP set whose pick count does not fit the items it holds.
     *
     * <p>Each set is checked on its own. A set with items but no picks is unreachable; more picks than items is
     * impossible; and picks equal to the items means the player chooses nothing, which is a fixed award wearing the
     * wrong label. Sets with no items are skipped, because the wizard drops their pick count on save.</p>
     *
     * @since 0.50.11
     */
    private void checkFlexiblePicks() {
        Map<Integer, Integer> pickCounts = lifePath.flexibleXPPickCounts() == null
                                                 ? Map.of()
                                                 : lifePath.flexibleXPPickCounts();

        for (int setIndex : LifePath.flexibleXPGroupKeys(lifePath)) {
            int itemCount = LifePath.flexibleXPItemValues(lifePath, setIndex).size();
            if (itemCount == 0) {
                continue;
            }

            Integer storedPickCount = pickCounts.get(setIndex);
            int pickCount = storedPickCount == null ? 0 : storedPickCount;

            if (pickCount <= 0) {
                invalidReasons.add(NO_FLEXIBLE_PICKS);
            } else if (pickCount > itemCount) {
                invalidReasons.add(TOO_MANY_FLEXIBLE_PICKS);
            } else if (pickCount == itemCount) {
                invalidReasons.add(FLEXIBLE_PICKS_EQUAL_ITEMS);
            }
        }
    }

    /**
     * Flags a Life Path that places a character in a faction without saying which faction.
     *
     * <p>Clan caste is included alongside the two affiliation stages: a caste only exists inside a Clan, so a caste
     * path with no faction requirement cannot be qualified for.</p>
     *
     * @since 0.50.11
     */
    private void checkAffiliationFactionRequirement() {
        // If the Life Path has an affiliation stage, but no factions are selected, then the Life Path is invalid
        Set<ATOWLifeStage> lifeStages = lifePath.lifeStages();

        if (lifeStages.contains(ATOWLifeStage.AFFILIATION) ||
                  lifeStages.contains(ATOWLifeStage.SUB_AFFILIATION) ||
                  lifeStages.contains(ATOWLifeStage.CLAN_CASTE)) {
            Collection<Set<String>> requirements = lifePath.requirementsFactions().values();
            if (requirements.isEmpty()) {
                invalidReasons.add(MISSING_FACTION);
                return;
            }

            for (Set<String> group : requirements) {
                if (group.isEmpty()) {
                    invalidReasons.add(MISSING_FACTION);
                    return;
                }
            }
        }
    }

    /**
     * Flags a Life Path with no source recorded.
     *
     * @since 0.50.11
     */
    private void checkSource() {
        if (StringUtility.isNullOrBlank(lifePath.source())) {
            invalidReasons.add(InvalidLifePathReason.MISSING_SOURCE);
        }
    }

    /**
     * Flags a Life Path with no name.
     *
     * @since 0.50.11
     */
    private void checkName() {
        if (StringUtility.isNullOrBlank(lifePath.name())) {
            invalidReasons.add(InvalidLifePathReason.MISSING_NAME);
        }
    }

    /**
     * Flags a Life Path belonging to no life stage, which nothing would ever offer.
     *
     * @since 0.50.11
     */
    private void checkLifeStages() {
        if (lifePath.lifeStages().isEmpty()) {
            invalidReasons.add(InvalidLifePathReason.MISSING_LIFE_STAGE);
        }
    }

    /**
     * Flags a Life Path with no categories, or one that combines {@link LifePathCategory#NONE} with a real category.
     *
     * <p>{@code NONE} means "belongs to no category". Pairing it with a real one says both at once, so one of the
     * two is always wrong.</p>
     *
     * @since 0.50.11
     */
    private void checkCategories() {
        Set<LifePathCategory> categories = lifePath.categories();

        if (categories.isEmpty()) {
            invalidReasons.add(InvalidLifePathReason.MISSING_CATEGORIES);
            return;
        }

        if (categories.contains(LifePathCategory.NONE) && categories.size() > 1) {
            invalidReasons.add(InvalidLifePathReason.CATEGORY_NONE_NOT_ALONE);
        }
    }

    /**
     * Flags a Life Path that requires itself, which nothing can ever satisfy.
     *
     * @since 0.50.11
     */
    private void checkLifePathRequirements() {
        if (flattenIds(lifePath.requirementsLifePath()).contains(lifePath.id())) {
            invalidReasons.add(InvalidLifePathReason.MATCHING_UUID_REQUIREMENT);
        }
    }

    /**
     * Flags a Life Path that excludes itself.
     *
     * @since 0.50.11
     */
    private void checkLifePathExclusions() {
        if (flattenIds(lifePath.exclusionsLifePath()).contains(lifePath.id())) {
            invalidReasons.add(InvalidLifePathReason.MATCHING_UUID_EXCLUSION);
        }
    }

    /**
     * Flags a Life Path that both requires and excludes the same thing.
     *
     * <p>Anything named on both sides makes the path impossible to qualify for: the character would have to have it
     * and not have it at once. Checked for factions, planetary systems, other Life Paths, categories and traits.</p>
     *
     * @since 0.50.11
     */
    private void checkContradictions() {
        boolean hasContradiction =
              sharesAnyEntry(lifePath.requirementsFactions(), lifePath.exclusionsFactions()) ||
                    sharesAnyEntry(lifePath.requirementsSystems(), lifePath.exclusionsSystems()) ||
                    sharesAnyEntry(lifePath.requirementsLifePath(), lifePath.exclusionsLifePath()) ||
                    sharesAnyKey(lifePath.requirementsCategories(), lifePath.exclusionsCategories()) ||
                    sharesAnyKey(lifePath.requirementsTraits(), lifePath.exclusionsTraits());

        if (hasContradiction) {
            invalidReasons.add(InvalidLifePathReason.CONTRADICTORY_REQUIREMENT_EXCLUSION);
        }
    }

    /**
     * Reports whether any single entry appears in both sets of groups.
     *
     * <p>Group indexes are ignored: requiring a faction in group 0 and excluding it in group 1 is still a
     * contradiction.</p>
     *
     * @param requirementGroups entries required, keyed by group index
     * @param exclusionGroups   entries excluded, keyed by group index
     * @param <T>               the entry type
     *
     * @return {@code true} when at least one entry appears on both sides
     *
     * @since 0.50.11
     */
    private static <T> boolean sharesAnyEntry(Map<Integer, Set<T>> requirementGroups,
          Map<Integer, Set<T>> exclusionGroups) {
        Set<T> required = new HashSet<>();
        for (Set<T> group : requirementGroups.values()) {
            if (group != null) {
                required.addAll(group);
            }
        }

        if (required.isEmpty()) {
            return false;
        }

        for (Set<T> group : exclusionGroups.values()) {
            if (group == null) {
                continue;
            }

            for (T entry : group) {
                if (required.contains(entry)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Reports whether any key appears in both sets of scored groups.
     *
     * <p>Only the key matters. A requirement and an exclusion on the same trait contradict each other whatever
     * levels they name, because one demands at least a level and the other forbids it.</p>
     *
     * @param requirementGroups scored entries required, keyed by group index
     * @param exclusionGroups   scored entries excluded, keyed by group index
     * @param <K>               the scored key type
     *
     * @return {@code true} when at least one key appears on both sides
     *
     * @since 0.50.11
     */
    private static <K> boolean sharesAnyKey(Map<Integer, Map<K, Integer>> requirementGroups,
          Map<Integer, Map<K, Integer>> exclusionGroups) {
        Set<K> required = new HashSet<>();
        for (Map<K, Integer> group : requirementGroups.values()) {
            if (group != null) {
                required.addAll(group.keySet());
            }
        }

        if (required.isEmpty()) {
            return false;
        }

        for (Map<K, Integer> group : exclusionGroups.values()) {
            if (group == null) {
                continue;
            }

            for (K key : group.keySet()) {
                if (required.contains(key)) {
                    return true;
                }
            }
        }

        return false;
    }
}
