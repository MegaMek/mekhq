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

import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTestFixtures.ANCIENT_VERSION;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTestFixtures.FUTURE_VERSION;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTestFixtures.validBuilder;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTestFixtures.validLifePath;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.function.Consumer;

import mekhq.MHQConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * Tests the {@link LifePath} record: what its constructor accepts, what it rejects, and the group counting that bounds
 * the flexible pick count.
 *
 * @since 0.50.11
 */
class LifePathTest {
    @Test
    void testValidLifePath_IsAccepted() {
        assertDoesNotThrow(LifePathTestFixtures::validLifePath, "A fully populated Life Path should be accepted.");
    }

    // region Version gate

    @Test
    void testVersion_BelowLastMilestone_IsRejected() {
        LifePathBuilder builder = validBuilder().version(ANCIENT_VERSION);

        assertThrows(IllegalArgumentException.class, builder::build,
              "A Life Path older than the last milestone should be rejected.");
    }

    @Test
    void testVersion_AboveCurrent_IsRejected() {
        LifePathBuilder builder = validBuilder().version(FUTURE_VERSION);

        assertThrows(IllegalArgumentException.class, builder::build,
              "A Life Path from a newer version than this one should be rejected.");
    }

    @Test
    void testVersion_CurrentVersion_IsAccepted() {
        LifePathBuilder builder = validBuilder().version(MHQConstants.VERSION);

        assertDoesNotThrow(builder::build, "A Life Path saved by this version should be accepted.");
    }

    @Test
    void testVersion_LastMilestone_IsAccepted() {
        LifePathBuilder builder = validBuilder().version(MHQConstants.LAST_MILESTONE);

        assertDoesNotThrow(builder::build, "A Life Path saved by the last milestone should be accepted.");
    }

    // endregion Version gate

    // region Required components

    /**
     * Every component that must not be null, paired with the setter that nulls it.
     *
     * @return one argument set per component
     */
    static Stream<Arguments> requiredComponents() {
        return Stream.of(
              nulled("id", builder -> builder.id(null)),
              nulled("version", builder -> builder.version(null)),
              nulled("xpCost", builder -> builder.xpCost(null)),
              nulled("source", builder -> builder.source(null)),
              nulled("name", builder -> builder.name(null)),
              nulled("flavorText", builder -> builder.flavorText(null)),
              nulled("age", builder -> builder.age(null)),
              nulled("xpDiscount", builder -> builder.xpDiscount(null)),
              nulled("minimumYear", builder -> builder.minimumYear(null)),
              nulled("maximumYear", builder -> builder.maximumYear(null)),
              nulled("randomWeight", builder -> builder.randomWeight(null)),
              nulled("lifeStages", builder -> builder.lifeStages(null)),
              nulled("categories", builder -> builder.categories(null)),
              nulled("isPlayerRestricted", builder -> builder.isPlayerRestricted(null)),
              nulled("requirementsFactions", builder -> builder.requirementsFactions(null)),
              nulled("requirementsSystems", builder -> builder.requirementsSystems(null)),
              nulled("requirementsLifePath", builder -> builder.requirementsLifePath(null)),
              nulled("requirementsCategories", builder -> builder.requirementsCategories(null)),
              nulled("requirementsAttributes", builder -> builder.requirementsAttributes(null)),
              nulled("requirementsEdge", builder -> builder.requirementsEdge(null)),
              nulled("requirementsFlexibleAttribute", builder -> builder.requirementsFlexibleAttribute(null)),
              nulled("requirementsTraits", builder -> builder.requirementsTraits(null)),
              nulled("requirementsSkills", builder -> builder.requirementsSkills(null)),
              nulled("requirementsMetaSkills", builder -> builder.requirementsMetaSkills(null)),
              nulled("requirementsAbilities", builder -> builder.requirementsAbilities(null)),
              nulled("exclusionsFactions", builder -> builder.exclusionsFactions(null)),
              nulled("exclusionsSystems", builder -> builder.exclusionsSystems(null)),
              nulled("exclusionsLifePath", builder -> builder.exclusionsLifePath(null)),
              nulled("exclusionsCategories", builder -> builder.exclusionsCategories(null)),
              nulled("exclusionsAttributes", builder -> builder.exclusionsAttributes(null)),
              nulled("exclusionsEdge", builder -> builder.exclusionsEdge(null)),
              nulled("exclusionsFlexibleAttribute", builder -> builder.exclusionsFlexibleAttribute(null)),
              nulled("exclusionsTraits", builder -> builder.exclusionsTraits(null)),
              nulled("exclusionsSkills", builder -> builder.exclusionsSkills(null)),
              nulled("exclusionsMetaSkills", builder -> builder.exclusionsMetaSkills(null)),
              nulled("exclusionsAbilities", builder -> builder.exclusionsAbilities(null)),
              nulled("fixedXPAttributes", builder -> builder.fixedXPAttributes(null)),
              nulled("fixedXPEdge", builder -> builder.fixedXPEdge(null)),
              nulled("fixedXPFlexibleAttribute", builder -> builder.fixedXPFlexibleAttribute(null)),
              nulled("fixedXPTraits", builder -> builder.fixedXPTraits(null)),
              nulled("fixedXPSkills", builder -> builder.fixedXPSkills(null)),
              nulled("fixedXPMetaSkills", builder -> builder.fixedXPMetaSkills(null)),
              nulled("fixedXPNaturalAptitudes", builder -> builder.fixedXPNaturalAptitudes(null)),
              nulled("fixedXPNaturalAptitudesMetaSkills",
                    builder -> builder.fixedXPNaturalAptitudesMetaSkills(null)),
              nulled("fixedXPAbilities", builder -> builder.fixedXPAbilities(null)),
              nulled("flexibleXPAttributes", builder -> builder.flexibleXPAttributes(null)),
              nulled("flexibleXPEdge", builder -> builder.flexibleXPEdge(null)),
              nulled("flexibleXPFlexibleAttribute", builder -> builder.flexibleXPFlexibleAttribute(null)),
              nulled("flexibleXPTraits", builder -> builder.flexibleXPTraits(null)),
              nulled("flexibleXPSkills", builder -> builder.flexibleXPSkills(null)),
              nulled("flexibleXPMetaSkills", builder -> builder.flexibleXPMetaSkills(null)),
              nulled("flexibleXPNaturalAptitudes", builder -> builder.flexibleXPNaturalAptitudes(null)),
              nulled("flexibleXPNaturalAptitudesMetaSkills",
                    builder -> builder.flexibleXPNaturalAptitudesMetaSkills(null)),
              nulled("flexibleXPAbilities", builder -> builder.flexibleXPAbilities(null)),
              nulled("flexibleXPPickCount", builder -> builder.flexibleXPPickCount(null)));
    }

    private static Arguments nulled(String componentName, Consumer<LifePathBuilder> setToNull) {
        return Arguments.of(componentName, setToNull);
    }

    @ParameterizedTest(name = "{0} cannot be null")
    @MethodSource("requiredComponents")
    void testRequiredComponent_NullIsRejected(String componentName, Consumer<LifePathBuilder> setToNull) {
        LifePathBuilder builder = validBuilder();
        setToNull.accept(builder);

        assertThrows(IllegalArgumentException.class, builder::build,
              componentName + " should not be accepted as null.");
    }

    @Test
    void testRequiredComponents_CoversEveryRecordComponent() {
        int componentCount = LifePath.class.getRecordComponents().length;

        assertEquals(componentCount, requiredComponents().count(),
              "Every record component should have a null-rejection test. Add the new one to requiredComponents().");
    }

    // endregion Required components

    // region Value ranges

    @Test
    void testName_BlankIsRejected() {
        LifePathBuilder builder = validBuilder().name("   ");

        assertThrows(IllegalArgumentException.class, builder::build, "A blank name should be rejected.");
    }

    @Test
    void testXpCost_NegativeIsRejected() {
        LifePathBuilder builder = validBuilder().xpCost(-1);

        assertThrows(IllegalArgumentException.class, builder::build, "A negative XP cost should be rejected.");
    }

    @Test
    void testAge_NegativeIsRejected() {
        LifePathBuilder builder = validBuilder().age(-1);

        assertThrows(IllegalArgumentException.class, builder::build, "A negative age should be rejected.");
    }

    @Test
    void testXpDiscount_NegativeIsRejected() {
        LifePathBuilder builder = validBuilder().xpDiscount(-1);

        assertThrows(IllegalArgumentException.class, builder::build, "A negative XP discount should be rejected.");
    }

    @Test
    void testRandomWeight_NegativeIsRejected() {
        LifePathBuilder builder = validBuilder().randomWeight(-0.1);

        assertThrows(IllegalArgumentException.class, builder::build, "A negative random weight should be rejected.");
    }

    @Test
    void testRandomWeight_ZeroIsAccepted() {
        LifePathBuilder builder = validBuilder().randomWeight(0.0);

        assertDoesNotThrow(builder::build, "A random weight of zero should be accepted.");
    }

    @Test
    void testMaximumYear_BelowMinimumIsRejected() {
        LifePathBuilder builder = validBuilder().minimumYear(3100).maximumYear(3000);

        assertThrows(IllegalArgumentException.class, builder::build,
              "A maximum year below the minimum year should be rejected.");
    }

    @Test
    void testYears_EqualIsAccepted() {
        LifePathBuilder builder = validBuilder().minimumYear(3050).maximumYear(3050);

        assertDoesNotThrow(builder::build, "A Life Path available in exactly one year should be accepted.");
    }

    @Test
    void testFlexiblePickCount_NegativeIsRejected() {
        LifePathBuilder builder = validBuilder().flexibleXPPickCount(-1);

        assertThrows(IllegalArgumentException.class, builder::build, "A negative pick count should be rejected.");
    }

    // endregion Value ranges

    // region Flexible group counting

    @Test
    void testPickCount_AboveGroupCountIsRejected() {
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 100)))
                                        .flexibleXPPickCount(2);

        assertThrows(IllegalArgumentException.class, builder::build,
              "Picking two of one group should be rejected.");
    }

    @Test
    void testPickCount_EqualToGroupCountIsAccepted() {
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 100),
                    1, Map.of("Piloting/Mek", 100)))
                                        .flexibleXPPickCount(2);

        assertDoesNotThrow(builder::build, "Picking both of two groups should be accepted.");
    }

    @Test
    void testPickCount_CountsGroupsHeldOnlyInMetaSkills() {
        // The group count used to be taken from the largest of six maps, which left out meta skills and both natural
        // aptitude maps. A path whose only flexible groups lived in one of those passed the wizard and then failed to
        // load.
        LifePathBuilder builder = validBuilder().flexibleXPMetaSkills(Map.of(0,
                    Map.of(mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_GUNNERY, 100)))
                                        .flexibleXPPickCount(1);

        assertDoesNotThrow(builder::build, "A group held only in the meta skills map should count as a group.");
    }

    @Test
    void testPickCount_CountsGroupsHeldOnlyInNaturalAptitudes() {
        LifePathBuilder builder = validBuilder().flexibleXPNaturalAptitudes(Map.of(0, Map.of("Gunnery/Mek", 100)))
                                        .flexibleXPPickCount(1);

        assertDoesNotThrow(builder::build,
              "A group held only in the natural aptitudes map should count as a group.");
    }

    @Test
    void testCountFlexibleXPGroups_CountsTheUnionOfKeys() {
        Map<Integer, Map<String, Integer>> skills = Map.of(0, Map.of("Gunnery/Mek", 100));
        Map<Integer, Integer> edge = Map.of(1, 50);

        assertEquals(2, LifePath.countFlexibleXPGroups(skills, edge),
              "Two maps holding different group indexes describe two groups.");
    }

    @Test
    void testCountFlexibleXPGroups_DoesNotDoubleCountSharedKeys() {
        Map<Integer, Map<String, Integer>> skills = Map.of(0, Map.of("Gunnery/Mek", 100));
        Map<Integer, Integer> edge = Map.of(0, 50);

        assertEquals(1, LifePath.countFlexibleXPGroups(skills, edge),
              "Two maps holding the same group index describe one group.");
    }

    @Test
    void testCountFlexibleXPGroups_IgnoresNullMaps() {
        assertEquals(0, LifePath.countFlexibleXPGroups((Map<Integer, ?>) null),
              "A null map should contribute no groups.");
    }

    @Test
    void testFlexibleXPGroupKeys_AreSortedAndDeduplicated() {
        Map<Integer, Integer> edge = Map.of(5, 1, 1, 1);
        Map<Integer, Integer> flexibleAttribute = Map.of(3, 1, 1, 1);

        SortedSet<Integer> keys = LifePath.flexibleXPGroupKeys(edge, flexibleAttribute);

        assertEquals(Set.of(1, 3, 5), keys, "Group keys should be the union of every map's keys.");
        assertEquals(1, keys.first(), "Group keys should be sorted ascending.");
        assertEquals(5, keys.last(), "Group keys should be sorted ascending.");
    }

    @Test
    void testCountFlexibleXPGroups_CountsSparseIndexes() {
        // Indexes are sparse after empty groups are stripped on save, so the count cannot assume they run from zero.
        Map<Integer, Integer> edge = Map.of(4, 10, 9, 10);

        assertEquals(2, LifePath.countFlexibleXPGroups(edge), "Sparse group indexes should each count once.");
    }

    // endregion Flexible group counting

    // region Resaving

    @Test
    void testResaveWithUpdatedVersion_StampsTheCurrentVersion() {
        LifePath original = validBuilder().version(MHQConstants.LAST_MILESTONE).build();

        LifePath resaved = original.resaveWithUpdatedVersion();

        assertEquals(MHQConstants.VERSION, resaved.version(), "Resaving should stamp the current version.");
    }

    @Test
    void testResaveWithUpdatedVersion_ChangesNothingElse() {
        UUID id = UUID.randomUUID();
        LifePath original = validBuilder().id(id)
                                  .version(MHQConstants.LAST_MILESTONE)
                                  .name("Unchanged")
                                  .fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 150)))
                                  .build();

        LifePath resaved = original.resaveWithUpdatedVersion();

        assertEquals(id, resaved.id(), "Resaving should not change the id.");
        assertEquals("Unchanged", resaved.name(), "Resaving should not change the name.");
        assertEquals(original.fixedXPSkills(), resaved.fixedXPSkills(), "Resaving should not change the awards.");
        assertEquals(resaved, resaved.resaveWithUpdatedVersion(),
              "Resaving an already current Life Path should change nothing at all.");
    }

    @Test
    void testResaveWithUpdatedVersion_IsEqualToARebuildWithTheSameVersion() {
        LifePath original = validBuilder().version(MHQConstants.LAST_MILESTONE).build();

        LifePath resaved = original.resaveWithUpdatedVersion();
        LifePath rebuilt = LifePathBuilder.from(original).version(MHQConstants.VERSION).build();

        assertEquals(rebuilt, resaved, "Resaving should match rebuilding with the version replaced.");
    }

    // endregion Resaving

    @Test
    void testBlankSourceAndFlavorText_AreAccepted() {
        LifePathBuilder builder = validBuilder().source("").flavorText("");

        assertDoesNotThrow(builder::build, "A blank source and flavor text should be tolerated.");
    }

    @Test
    void testLifeStagesAndCategories_MayBeEmpty() {
        LifePathBuilder builder = validBuilder().lifeStages(Set.of()).categories(Set.of());

        assertDoesNotThrow(builder::build,
              "An empty life stage and category set should load; the validator reports them, not the constructor.");
    }

    @Test
    void testLifePath_WithEveryMapPopulated_IsAccepted() {
        LifePath lifePath = populatedBuilder().build();

        assertTrue(lifePath.requirementsFactions().containsKey(0), "Requirements should survive construction.");
        assertTrue(lifePath.flexibleXPAbilities().containsKey(0), "Flexible awards should survive construction.");
    }

    /**
     * Returns a builder with something in every group map, for round-trip and counting tests.
     *
     * @return the builder
     */
    static LifePathBuilder populatedBuilder() {
        return validBuilder().requirementsFactions(Map.of(0, Set.of("FS")))
                     .requirementsSystems(Map.of(0, Set.of("Terra")))
                     .requirementsLifePath(Map.of(0, Set.of(UUID.randomUUID())))
                     .requirementsCategories(Map.of(0, Map.of(LifePathCategory.GENERAL_INNER_SPHERE, 1)))
                     .requirementsAttributes(Map.of(0,
                           Map.of(mekhq.campaign.personnel.skills.enums.SkillAttribute.STRENGTH, 4)))
                     .requirementsEdge(Map.of(0, 2))
                     .requirementsFlexibleAttribute(Map.of(0, 3))
                     .requirementsTraits(Map.of(0, Map.of(mekhq.campaign.personnel.ATOWTraits.WEALTH, 2)))
                     .requirementsSkills(Map.of(0, Map.of("Gunnery/Mek", 3)))
                     .requirementsMetaSkills(Map.of(0,
                           Map.of(mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_GUNNERY, 2)))
                     .requirementsAbilities(Map.of(0, Map.of("Melee Specialist", 0)))
                     .exclusionsFactions(Map.of(0, Set.of("DC")))
                     .exclusionsSystems(Map.of(0, Set.of("Luthien")))
                     .exclusionsLifePath(Map.of(0, Set.of(UUID.randomUUID())))
                     .exclusionsCategories(Map.of(0, Map.of(LifePathCategory.GENERAL_CLAN, 1)))
                     .exclusionsAttributes(Map.of(0,
                           Map.of(mekhq.campaign.personnel.skills.enums.SkillAttribute.BODY, 9)))
                     .exclusionsEdge(Map.of(0, 9))
                     .exclusionsFlexibleAttribute(Map.of(0, 9))
                     .exclusionsTraits(Map.of(0, Map.of(mekhq.campaign.personnel.ATOWTraits.UNLUCKY, 3)))
                     .exclusionsSkills(Map.of(0, Map.of("Piloting/Mek", 8)))
                     .exclusionsMetaSkills(Map.of(0,
                           Map.of(mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_GENERAL, 8)))
                     .exclusionsAbilities(Map.of(0, Map.of("Dodge Maneuver", 0)))
                     .fixedXPAttributes(Map.of(0,
                           Map.of(mekhq.campaign.personnel.skills.enums.SkillAttribute.REFLEXES, 100)))
                     .fixedXPEdge(Map.of(0, 50))
                     .fixedXPFlexibleAttribute(Map.of(0, 25))
                     .fixedXPTraits(Map.of(0, Map.of(mekhq.campaign.personnel.ATOWTraits.CONNECTIONS, 100)))
                     .fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 150)))
                     .fixedXPMetaSkills(Map.of(0,
                           Map.of(mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_PILOTING, 75)))
                     .fixedXPNaturalAptitudes(Map.of(0, Map.of("Piloting/Mek", 60)))
                     .fixedXPNaturalAptitudesMetaSkills(Map.of(0,
                           Map.of(mekhq.campaign.personnel.skills.enums.SkillSubType.SUPPORT, 40)))
                     .fixedXPAbilities(Map.of(0, Map.of("Melee Specialist", 200)))
                     .flexibleXPAttributes(Map.of(0,
                           Map.of(mekhq.campaign.personnel.skills.enums.SkillAttribute.DEXTERITY, 100)))
                     .flexibleXPEdge(Map.of(0, 10))
                     .flexibleXPFlexibleAttribute(Map.of(0, 20))
                     .flexibleXPTraits(Map.of(0, Map.of(mekhq.campaign.personnel.ATOWTraits.FAME, 100)))
                     .flexibleXPSkills(Map.of(0, Map.of("Tactics", 120)))
                     .flexibleXPMetaSkills(Map.of(0,
                           Map.of(mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_GUNNERY, 30)))
                     .flexibleXPNaturalAptitudes(Map.of(0, Map.of("Tactics", 20)))
                     .flexibleXPNaturalAptitudesMetaSkills(Map.of(0,
                           Map.of(mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_GENERAL, 10)))
                     .flexibleXPAbilities(Map.of(0, Map.of("Dodge Maneuver", 200)))
                     .flexibleXPPickCount(1);
    }
}
