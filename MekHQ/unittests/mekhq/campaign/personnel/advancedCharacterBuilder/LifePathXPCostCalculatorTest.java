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

import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathTestFixtures.validBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link LifePathXPCostCalculator}, including the weighted average that prices the flexible section.
 *
 * @since 0.50.11
 */
class LifePathXPCostCalculatorTest {
    @Test
    void testEmptyLifePath_CostsNothing() {
        assertEquals(0, LifePathXPCostCalculator.calculateXPCost(validBuilder()),
              "A Life Path that awards nothing should cost nothing.");
    }

    // region Fixed XP

    @Test
    void testFixedSkills_AreSummed() {
        LifePathBuilder builder = validBuilder().fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 150,
              "Piloting/Mek", 50)));

        assertEquals(200, LifePathXPCostCalculator.calculateXPCost(builder),
              "Fixed skill awards should add up.");
    }

    @Test
    void testFixedAwards_AreSummedAcrossEveryMap() {
        LifePathBuilder builder = validBuilder().fixedXPAttributes(Map.of(0, Map.of(SkillAttribute.STRENGTH, 10)))
                                        .fixedXPEdge(Map.of(0, 20))
                                        .fixedXPFlexibleAttribute(Map.of(0, 30))
                                        .fixedXPTraits(Map.of(0, Map.of(ATOWTraits.WEALTH, 40)))
                                        .fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 50)))
                                        .fixedXPMetaSkills(Map.of(0, Map.of(SkillSubType.COMBAT_GUNNERY, 60)))
                                        .fixedXPNaturalAptitudes(Map.of(0, Map.of("Piloting/Mek", 70)))
                                        .fixedXPNaturalAptitudesMetaSkills(Map.of(0, Map.of(SkillSubType.SUPPORT, 80)))
                                        .fixedXPAbilities(Map.of(0, Map.of("Melee Specialist", 90)));

        assertEquals(450, LifePathXPCostCalculator.calculateXPCost(builder),
              "Every fixed XP map should contribute to the total.");
    }

    @Test
    void testFixedAwards_AreSummedAcrossGroups() {
        LifePathBuilder builder = validBuilder().fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 100),
              1, Map.of("Piloting/Mek", 100)));

        assertEquals(200, LifePathXPCostCalculator.calculateXPCost(builder),
              "Fixed XP groups are all awarded, so they all count.");
    }

    // endregion Fixed XP

    // region Discount and floor

    @Test
    void testDiscount_ReducesTheCost() {
        LifePathBuilder builder = validBuilder().xpDiscount(50)
                                        .fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 200)));

        assertEquals(150, LifePathXPCostCalculator.calculateXPCost(builder),
              "The discount should come off the total.");
    }

    @Test
    void testDiscountLargerThanTheAwards_FloorsAtZero() {
        LifePathBuilder builder = validBuilder().xpDiscount(500)
                                        .fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 100)));

        assertEquals(0, LifePathXPCostCalculator.calculateXPCost(builder),
              "A Life Path can cost nothing but never less than nothing.");
    }

    @Test
    void testNegativeTraitAward_ReducesTheCost() {
        // A negative trait is a drawback the character takes on, so it makes the Life Path cheaper.
        LifePathBuilder builder = validBuilder().fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 300)))
                                        .fixedXPTraits(Map.of(0, Map.of(ATOWTraits.UNLUCKY, -100)));

        assertEquals(200, LifePathXPCostCalculator.calculateXPCost(builder),
              "A negative award should reduce the cost.");
    }

    // endregion Discount and floor

    // region Flexible XP weighting

    @Test
    void testFlexibleSection_IsNotChargedWithoutPicks() {
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 500)))
                                        .flexibleXPPickCount(0);

        assertEquals(0, LifePathXPCostCalculator.calculateXPCost(builder),
              "Groups the player cannot pick should not be charged for.");
    }

    @Test
    void testSingleFlexibleGroup_CostsItsOwnValue() {
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 120)))
                                        .flexibleXPPickCount(1);

        assertEquals(120, LifePathXPCostCalculator.calculateXPCost(builder),
              "With one group there is nothing to average, so the price is that group.");
    }

    @Test
    void testIdenticalFlexibleGroups_CostTheirSharedValue() {
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 100),
              1, Map.of("Piloting/Mek", 100), 2, Map.of("Tactics", 100))).flexibleXPPickCount(2);

        assertEquals(200, LifePathXPCostCalculator.calculateXPCost(builder),
              "Three equal groups should price at that value however they are weighted.");
    }

    @Test
    void testWeightedAverage_FavoursTheMiddleGroup() {
        // The documented example: groups worth 60, 90 and 150, two picks. A flat average would charge 200. The
        // weighted average leans on the middle group, so it charges less.
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 60),
              1, Map.of("Piloting/Mek", 90), 2, Map.of("Tactics", 150))).flexibleXPPickCount(2);

        int cost = LifePathXPCostCalculator.calculateXPCost(builder);

        assertEquals(186, cost, "Two picks of 60/90/150 should cost 186, not the flat-average 200.");
    }

    @Test
    void testWeightedAverage_AnOutlierBarelyMovesThePrice() {
        Map<Integer, Map<String, Integer>> tightGroups = Map.of(0, Map.of("A", 100),
              1, Map.of("B", 100),
              2, Map.of("C", 100));
        Map<Integer, Map<String, Integer>> withOutlier = Map.of(0, Map.of("A", 100),
              1, Map.of("B", 100),
              2, Map.of("C", 1000));

        int tightCost = LifePathXPCostCalculator.calculateXPCost(validBuilder().flexibleXPSkills(tightGroups)
                                                                      .flexibleXPPickCount(1));
        int outlierCost = LifePathXPCostCalculator.calculateXPCost(validBuilder().flexibleXPSkills(withOutlier)
                                                                        .flexibleXPPickCount(1));

        // A flat average would have jumped from 100 to 400.
        assertEquals(100, tightCost, "Three equal groups should price at their shared value.");
        assertEquals(196, outlierCost, "A tenfold outlier should move the price from 100 to 196, not to 400.");
        assertTrue(outlierCost > tightCost, "The outlier should still raise the price a little.");
    }

    @Test
    void testWeightedAverage_IgnoresGroupOrder() {
        LifePathBuilder ascending = validBuilder().flexibleXPSkills(Map.of(0, Map.of("A", 60),
              1, Map.of("B", 90), 2, Map.of("C", 150))).flexibleXPPickCount(2);
        LifePathBuilder descending = validBuilder().flexibleXPSkills(Map.of(0, Map.of("A", 150),
              1, Map.of("B", 90), 2, Map.of("C", 60))).flexibleXPPickCount(2);

        assertEquals(LifePathXPCostCalculator.calculateXPCost(ascending),
              LifePathXPCostCalculator.calculateXPCost(descending),
              "The groups are sorted by cost, so the order the author created them in must not matter.");
    }

    @Test
    void testFlexibleCost_ScalesWithPickCount() {
        Map<Integer, Map<String, Integer>> groups = Map.of(0, Map.of("A", 100),
              1, Map.of("B", 100),
              2, Map.of("C", 100));

        int onePick = LifePathXPCostCalculator.calculateXPCost(validBuilder().flexibleXPSkills(groups)
                                                                    .flexibleXPPickCount(1));
        int threePicks = LifePathXPCostCalculator.calculateXPCost(validBuilder().flexibleXPSkills(groups)
                                                                       .flexibleXPPickCount(3));

        assertEquals(onePick * 3, threePicks, "Each pick should cost the same weighted average.");
    }

    @Test
    void testFlexibleGroups_CountSparseIndexes() {
        // Empty groups are stripped on save, so indexes need not run from zero.
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(4, Map.of("A", 100),
              9, Map.of("B", 100))).flexibleXPPickCount(2);

        assertEquals(200, LifePathXPCostCalculator.calculateXPCost(builder),
              "Sparse group indexes should be priced the same as consecutive ones.");
    }

    @Test
    void testFlexibleGroup_SumsEveryMapForThatGroup() {
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("A", 50)))
                                        .flexibleXPEdge(Map.of(0, 30))
                                        .flexibleXPAbilities(Map.of(0, Map.of("Melee Specialist", 20)))
                                        .flexibleXPPickCount(1);

        assertEquals(100, LifePathXPCostCalculator.calculateXPCost(builder),
              "A group's cost is everything that group awards, across every map.");
    }

    // endregion Flexible XP weighting

    // region Tolerance of bad data

    @Test
    void testNullValuesInAGroup_AreTreatedAsZero() {
        // A hand-edited file can leave a value null. Unboxing one used to take the wizard down.
        Map<String, Integer> skills = new HashMap<>();
        skills.put("Gunnery/Mek", 100);
        skills.put("Piloting/Mek", null);

        LifePathBuilder builder = validBuilder().fixedXPSkills(Map.of(0, skills));

        assertEquals(100, LifePathXPCostCalculator.calculateXPCost(builder),
              "A null award should count as nothing rather than throwing.");
    }

    @Test
    void testNullGroup_IsTreatedAsEmpty() {
        Map<Integer, Map<String, Integer>> groups = new HashMap<>();
        groups.put(0, Map.of("Gunnery/Mek", 100));
        groups.put(1, null);

        LifePathBuilder builder = validBuilder().fixedXPSkills(groups);

        assertEquals(100, LifePathXPCostCalculator.calculateXPCost(builder),
              "A null group should count as nothing rather than throwing.");
    }

    @Test
    void testNullEdgeValue_IsTreatedAsZero() {
        Map<Integer, Integer> edge = new HashMap<>();
        edge.put(0, null);

        LifePathBuilder builder = validBuilder().fixedXPEdge(edge)
                                        .fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 75)));

        assertEquals(75, LifePathXPCostCalculator.calculateXPCost(builder),
              "An unset Edge award should count as nothing.");
    }

    // endregion Tolerance of bad data

    @Test
    void testRecordAndBuilder_PriceIdentically() {
        LifePath lifePath = validBuilder().fixedXPSkills(Map.of(0, Map.of("Gunnery/Mek", 150)))
                                  .flexibleXPSkills(Map.of(0, Map.of("A", 60), 1, Map.of("B", 90)))
                                  .flexibleXPPickCount(1)
                                  .xpCost(0)
                                  .build();

        assertEquals(LifePathXPCostCalculator.calculateXPCost(LifePathBuilder.from(lifePath)),
              LifePathXPCostCalculator.calculateXPCost(lifePath),
              "Pricing a record and pricing the builder it came from should agree.");
    }
}
