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

import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.CATEGORY_NONE_NOT_ALONE;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.CONTRADICTORY_REQUIREMENT_EXCLUSION;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.FLEXIBLE_PICKS_EQUAL_ITEMS;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.MATCHING_UUID_EXCLUSION;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.MATCHING_UUID_REQUIREMENT;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.MIN_YEAR_ABOVE_MAX_YEAR;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.MISSING_CATEGORIES;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.MISSING_FACTION;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.MISSING_LIFE_STAGE;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.MISSING_NAME;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.MISSING_SOURCE;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.NO_FLEXIBLE_PICKS;
import static mekhq.campaign.personnel.advancedCharacterBuilder.InvalidLifePathReason.TOO_MANY_FLEXIBLE_PICKS;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTestFixtures.validBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mekhq.campaign.personnel.ATOWTraits;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests {@link LifePathValidator}: one case per reason it can report, plus the cases that must stay valid.
 *
 * @since 0.50.11
 */
class LifePathValidatorTest {
    @Test
    void testValidLifePath_ReportsNothing() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder());

        assertTrue(reasons.isEmpty(), "A valid Life Path should report no reasons, but reported: " + reasons);
    }

    @Test
    void testValidate_AcceptsAFinishedRecord() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().build());

        assertTrue(reasons.isEmpty(), "Validating a finished record should behave the same as validating a builder.");
    }

    // region Basic information

    @Test
    void testMissingName_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().name("  "));

        assertTrue(reasons.contains(MISSING_NAME), "A blank name should be reported.");
    }

    @Test
    void testMissingSource_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().source(""));

        assertTrue(reasons.contains(MISSING_SOURCE), "A blank source should be reported.");
    }

    @Test
    void testMissingLifeStage_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().lifeStages(Set.of()));

        assertTrue(reasons.contains(MISSING_LIFE_STAGE), "No life stage should be reported.");
    }

    @Test
    void testMissingCategories_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().categories(Set.of()));

        assertTrue(reasons.contains(MISSING_CATEGORIES), "No category should be reported.");
    }

    // endregion Basic information

    // region Years

    @Test
    void testMinimumYearAboveMaximum_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().minimumYear(3100)
                                                                             .maximumYear(3000));

        assertTrue(reasons.contains(MIN_YEAR_ABOVE_MAX_YEAR), "A minimum year above the maximum should be reported.");
    }

    @Test
    void testEqualYears_AreNotReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().minimumYear(3050)
                                                                             .maximumYear(3050));

        assertFalse(reasons.contains(MIN_YEAR_ABOVE_MAX_YEAR),
              "A Life Path available in exactly one year is legitimate.");
    }

    // endregion Years

    // region Categories

    @Test
    void testCategoryNoneWithAnother_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().categories(Set.of(
              LifePathCategory.NONE, LifePathCategory.GENERAL_CLAN)));

        assertTrue(reasons.contains(CATEGORY_NONE_NOT_ALONE),
              "NONE alongside a real category should be reported.");
    }

    @Test
    void testCategoryNoneAlone_IsNotReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().categories(Set.of(
              LifePathCategory.NONE)));

        assertFalse(reasons.contains(CATEGORY_NONE_NOT_ALONE), "NONE on its own is legitimate.");
        assertFalse(reasons.contains(MISSING_CATEGORIES), "NONE on its own is not an empty category set.");
    }

    // endregion Categories

    // region Affiliation factions

    @Test
    void testAffiliationWithNoFaction_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().lifeStages(Set.of(
              ATOWLifeStage.AFFILIATION)));

        assertTrue(reasons.contains(MISSING_FACTION), "An affiliation with no faction should be reported.");
    }

    @Test
    void testSubAffiliationWithNoFaction_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().lifeStages(Set.of(
              ATOWLifeStage.SUB_AFFILIATION)));

        assertTrue(reasons.contains(MISSING_FACTION), "A sub-affiliation with no faction should be reported.");
    }

    @Test
    void testClanCasteWithNoFaction_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().lifeStages(Set.of(
              ATOWLifeStage.CLAN_CASTE)));

        assertTrue(reasons.contains(MISSING_FACTION),
              "A Clan caste only exists inside a Clan, so it needs a faction requirement.");
    }

    @Test
    void testAffiliationWithAnEmptyFactionGroup_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().lifeStages(Set.of(
              ATOWLifeStage.AFFILIATION)).requirementsFactions(Map.of(0, Set.of())));

        assertTrue(reasons.contains(MISSING_FACTION), "An empty faction group is no faction at all.");
    }

    @Test
    void testAffiliationWithAFaction_IsNotReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().lifeStages(Set.of(
              ATOWLifeStage.AFFILIATION)).requirementsFactions(Map.of(0, Set.of("FS"))));

        assertFalse(reasons.contains(MISSING_FACTION), "An affiliation naming a faction is valid.");
    }

    @Test
    void testNonAffiliationWithNoFaction_IsNotReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().lifeStages(Set.of(
              ATOWLifeStage.REAL_LIFE)));

        assertFalse(reasons.contains(MISSING_FACTION), "Only affiliation stages and castes need a faction.");
    }

    // endregion Affiliation factions

    // region Flexible picks

    @Test
    void testFlexibleSetWithNoPicks_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().flexibleXPSkills(Map.of(0,
              Map.of("Gunnery/Mek", 100, "Piloting/Mek", 100))).flexibleXPPickCounts(Map.of(0, 0)));

        assertTrue(reasons.contains(NO_FLEXIBLE_PICKS), "A set the player can never pick from should be reported.");
    }

    @Test
    void testFlexibleSetWithNoPickCountRecorded_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().flexibleXPSkills(Map.of(0,
              Map.of("Gunnery/Mek", 100, "Piloting/Mek", 100))));

        assertTrue(reasons.contains(NO_FLEXIBLE_PICKS), "A set with no pick count recorded allows no picks.");
    }

    @Test
    void testNoFlexibleSetsAndNoPicks_IsNotReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder());

        assertFalse(reasons.contains(NO_FLEXIBLE_PICKS), "A Life Path with no flexible section is valid.");
    }

    @Test
    void testMorePicksThanItems_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().flexibleXPSkills(Map.of(0,
              Map.of("Gunnery/Mek", 100))).flexibleXPPickCounts(Map.of(0, 3)));

        assertTrue(reasons.contains(TOO_MANY_FLEXIBLE_PICKS),
              "Picking three items from a one-item set should be reported.");
    }

    @Test
    void testPicksEqualToItems_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().flexibleXPSkills(Map.of(0,
              Map.of("Gunnery/Mek", 100, "Piloting/Mek", 100))).flexibleXPPickCounts(Map.of(0, 2)));

        assertTrue(reasons.contains(FLEXIBLE_PICKS_EQUAL_ITEMS),
              "Picking every item in a set is a fixed award, and should be reported.");
        assertFalse(reasons.contains(TOO_MANY_FLEXIBLE_PICKS), "Equal is not too many.");
    }

    @Test
    void testPicksBelowItems_IsNotReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().flexibleXPSkills(Map.of(0,
              Map.of("Gunnery/Mek", 100, "Piloting/Mek", 100))).flexibleXPPickCounts(Map.of(0, 1)));

        assertFalse(reasons.contains(NO_FLEXIBLE_PICKS), "One pick from two items is a real choice.");
        assertFalse(reasons.contains(TOO_MANY_FLEXIBLE_PICKS), "One pick from two items is a real choice.");
        assertFalse(reasons.contains(FLEXIBLE_PICKS_EQUAL_ITEMS), "One pick from two items is a real choice.");
    }

    @Test
    void testEachFlexibleSet_IsCheckedOnItsOwn() {
        // Set 0 is fine. Set 1 has items but no picks. The report is about set 1, whatever set 0 looks like.
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().flexibleXPSkills(Map.of(0,
                    Map.of("Gunnery/Mek", 100, "Piloting/Mek", 100), 1, Map.of("Tactics", 100, "Leadership", 100)))
                                                                    .flexibleXPPickCounts(Map.of(0, 1)));

        assertTrue(reasons.contains(NO_FLEXIBLE_PICKS), "A second set with no picks should be reported.");
    }

    @Test
    void testPicksCountItemsInEveryFlexibleMap() {
        // Meta skills and both natural aptitude maps used to be left out of the count, so a path whose only items
        // lived there was reported as having too many picks.
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().flexibleXPNaturalAptitudes(
              Map.of(0, Map.of("Gunnery/Mek", 100, "Piloting/Mek", 100))).flexibleXPPickCounts(Map.of(0, 1)));

        assertFalse(reasons.contains(TOO_MANY_FLEXIBLE_PICKS),
              "Items held only in the natural aptitudes map should count.");
    }

    // endregion Flexible picks

    // region Self reference

    @Test
    void testRequiringItself_IsReported() {
        UUID id = UUID.randomUUID();
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().id(id)
                                                                             .requirementsLifePath(Map.of(0,
                                                                                   Set.of(id))));

        assertTrue(reasons.contains(MATCHING_UUID_REQUIREMENT), "A Life Path cannot require itself.");
    }

    @Test
    void testExcludingItself_IsReported() {
        UUID id = UUID.randomUUID();
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().id(id)
                                                                             .exclusionsLifePath(Map.of(0,
                                                                                   Set.of(id))));

        assertTrue(reasons.contains(MATCHING_UUID_EXCLUSION), "A Life Path cannot exclude itself.");
    }

    @Test
    void testRequiringADifferentLifePath_IsNotReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().id(UUID.randomUUID())
                                                                             .requirementsLifePath(Map.of(0,
                                                                                   Set.of(UUID.randomUUID()))));

        assertFalse(reasons.contains(MATCHING_UUID_REQUIREMENT), "Requiring another Life Path is normal.");
    }

    // endregion Self reference

    // region Contradictions

    @Test
    void testFactionBothRequiredAndExcluded_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().requirementsFactions(Map.of(0,
              Set.of("FS"))).exclusionsFactions(Map.of(0, Set.of("FS"))));

        assertTrue(reasons.contains(CONTRADICTORY_REQUIREMENT_EXCLUSION),
              "A faction that is both required and excluded makes the path impossible.");
    }

    @Test
    void testContradictionAcrossDifferentGroups_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().requirementsFactions(Map.of(0,
              Set.of("FS"))).exclusionsFactions(Map.of(1, Set.of("FS"))));

        assertTrue(reasons.contains(CONTRADICTORY_REQUIREMENT_EXCLUSION),
              "The group index does not matter; the character still has to both be and not be Federated Suns.");
    }

    @Test
    void testSystemBothRequiredAndExcluded_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().requirementsSystems(Map.of(0,
              Set.of("Terra"))).exclusionsSystems(Map.of(0, Set.of("Terra"))));

        assertTrue(reasons.contains(CONTRADICTORY_REQUIREMENT_EXCLUSION),
              "A system that is both required and excluded makes the path impossible.");
    }

    @Test
    void testLifePathBothRequiredAndExcluded_IsReported() {
        UUID other = UUID.randomUUID();
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().requirementsLifePath(Map.of(0,
              Set.of(other))).exclusionsLifePath(Map.of(0, Set.of(other))));

        assertTrue(reasons.contains(CONTRADICTORY_REQUIREMENT_EXCLUSION),
              "A Life Path that is both required and excluded makes the path impossible.");
    }

    @Test
    void testCategoryBothRequiredAndExcluded_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().requirementsCategories(Map.of(0,
              Map.of(LifePathCategory.GENERAL_CLAN, 1)))
                                                                             .exclusionsCategories(Map.of(0,
                                                                                   Map.of(LifePathCategory.GENERAL_CLAN,
                                                                                         3))));

        assertTrue(reasons.contains(CONTRADICTORY_REQUIREMENT_EXCLUSION),
              "The counts differ but the category is still demanded and forbidden at once.");
    }

    @Test
    void testTraitBothRequiredAndExcluded_IsReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().requirementsTraits(Map.of(0,
              Map.of(ATOWTraits.WEALTH, 2))).exclusionsTraits(Map.of(0, Map.of(ATOWTraits.WEALTH, 8))));

        assertTrue(reasons.contains(CONTRADICTORY_REQUIREMENT_EXCLUSION),
              "A trait that is both required and excluded makes the path impossible.");
    }

    @Test
    void testDifferentFactionsRequiredAndExcluded_IsNotReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().requirementsFactions(Map.of(0,
              Set.of("FS"))).exclusionsFactions(Map.of(0, Set.of("DC"))));

        assertFalse(reasons.contains(CONTRADICTORY_REQUIREMENT_EXCLUSION),
              "Requiring one faction and excluding another is a normal rule.");
    }

    @Test
    void testDifferentTraitsRequiredAndExcluded_IsNotReported() {
        Set<InvalidLifePathReason> reasons = LifePathValidator.validate(validBuilder().requirementsTraits(Map.of(0,
              Map.of(ATOWTraits.WEALTH, 2))).exclusionsTraits(Map.of(0, Map.of(ATOWTraits.UNLUCKY, 3))));

        assertFalse(reasons.contains(CONTRADICTORY_REQUIREMENT_EXCLUSION),
              "Requiring one trait and excluding another is a normal rule.");
    }

    // endregion Contradictions

    @ParameterizedTest
    @EnumSource(InvalidLifePathReason.class)
    void testEveryReason_HasDisplayStrings(InvalidLifePathReason reason) {
        assertFalse(reason.getDisplayName().startsWith("!"),
              reason + " has no display name in InvalidLifePathReason.properties.");
        assertFalse(reason.getDescription().startsWith("!"),
              reason + " has no description in InvalidLifePathReason.properties.");
    }

    @Test
    void testEveryReason_IsProducibleByTheValidator() {
        // A reason nothing can produce is dead weight and misleads the next person to read the enum. Every value
        // should be covered by one of the cases above.
        Set<InvalidLifePathReason> produced = new java.util.HashSet<>();

        produced.addAll(LifePathValidator.validate(validBuilder().name("").source("")
                                                         .lifeStages(Set.of(ATOWLifeStage.AFFILIATION))
                                                         .categories(Set.of())
                                                         .minimumYear(3100)
                                                         .maximumYear(3000)));
        // A separate case: an empty life stage set cannot also be an affiliation with no faction.
        produced.addAll(LifePathValidator.validate(validBuilder().lifeStages(Set.of())));
        produced.addAll(LifePathValidator.validate(validBuilder().flexibleXPSkills(Map.of(0,
              Map.of("Gunnery/Mek", 100))).flexibleXPPickCounts(Map.of(0, 0))));
        produced.addAll(LifePathValidator.validate(validBuilder().flexibleXPSkills(Map.of(0,
              Map.of("Gunnery/Mek", 100))).flexibleXPPickCounts(Map.of(0, 3))));
        produced.addAll(LifePathValidator.validate(validBuilder().flexibleXPSkills(Map.of(0,
              Map.of("Gunnery/Mek", 100))).flexibleXPPickCounts(Map.of(0, 1))));
        produced.addAll(LifePathValidator.validate(validBuilder().categories(Set.of(LifePathCategory.NONE,
              LifePathCategory.GENERAL_CLAN))));
        produced.addAll(LifePathValidator.validate(validBuilder().requirementsFactions(Map.of(0, Set.of("FS")))
                                                         .exclusionsFactions(Map.of(0, Set.of("FS")))));

        UUID id = UUID.randomUUID();
        produced.addAll(LifePathValidator.validate(validBuilder().id(id)
                                                         .requirementsLifePath(Map.of(0, Set.of(id)))
                                                         .exclusionsLifePath(Map.of(0, Set.of(id)))));

        assertEquals(Set.of(InvalidLifePathReason.values()).size(), produced.size(),
              "Every InvalidLifePathReason should be reachable. Unreachable: "
                    + java.util.Arrays.stream(InvalidLifePathReason.values())
                            .filter(reason -> !produced.contains(reason))
                            .toList());
    }
}
