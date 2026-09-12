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
    void testFlexibleSet_IsNotChargedWithoutPicks() {
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 500)))
                                        .flexibleXPPickCounts(Map.of(0, 0));

        assertEquals(0, LifePathXPCostCalculator.calculateXPCost(builder),
              "A set the player cannot pick from should not be charged for.");
    }

    @Test
    void testFlexibleSet_IsNotChargedWhenNoPickCountIsRecorded() {
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 500)));

        assertEquals(0, LifePathXPCostCalculator.calculateXPCost(builder),
              "A set with no pick count recorded allows no picks, so it costs nothing.");
    }

    @Test
    void testSingleItemSet_CostsItsOwnValue() {
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 120)))
                                        .flexibleXPPickCounts(Map.of(0, 1));

        assertEquals(120, LifePathXPCostCalculator.calculateXPCost(builder),
              "With one item there is nothing to average, so the price is that item.");
    }

    @Test
    void testIdenticalItems_CostTheirSharedValue() {
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 100,
              "Piloting/Mek", 100, "Tactics", 100))).flexibleXPPickCounts(Map.of(0, 2));

        assertEquals(200, LifePathXPCostCalculator.calculateXPCost(builder),
              "Three equal items should price at that value however they are weighted.");
    }

    @Test
    void testWeightedAverage_FavoursTheMiddleItem() {
        // Items worth 60, 90 and 150 in one set, two picks. A flat average would charge 200. The weighted average
        // leans on the middle item, so it charges less.
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 60,
              "Piloting/Mek", 90, "Tactics", 150))).flexibleXPPickCounts(Map.of(0, 2));

        int cost = LifePathXPCostCalculator.calculateXPCost(builder);

        assertEquals(186, cost, "Two picks of 60/90/150 should cost 186, not the flat-average 200.");
    }

    @Test
    void testWeightedAverage_AnOutlierBarelyMovesThePrice() {
        Map<Integer, Map<String, Integer>> tightSet = Map.of(0, Map.of("A", 100, "B", 100, "C", 100));
        Map<Integer, Map<String, Integer>> withOutlier = Map.of(0, Map.of("A", 100, "B", 100, "C", 1000));

        int tightCost = LifePathXPCostCalculator.calculateXPCost(validBuilder().flexibleXPSkills(tightSet)
                                                                      .flexibleXPPickCounts(Map.of(0, 1)));
        int outlierCost = LifePathXPCostCalculator.calculateXPCost(validBuilder().flexibleXPSkills(withOutlier)
                                                                        .flexibleXPPickCounts(Map.of(0, 1)));

        // A flat average would have jumped from 100 to 400.
        assertEquals(100, tightCost, "Three equal items should price at their shared value.");
        assertEquals(196, outlierCost, "A tenfold outlier should move the price from 100 to 196, not to 400.");
        assertTrue(outlierCost > tightCost, "The outlier should still raise the price a little.");
    }

    @Test
    void testWeightedAverage_IgnoresWhichMapAnItemLivesIn() {
        // The same three values, once all as skills and once spread across skills, an ability and Edge. The average
        // is over item values, so where the items live must not matter.
        LifePathBuilder allSkills = validBuilder().flexibleXPSkills(Map.of(0, Map.of("A", 60, "B", 90, "C", 150)))
                                          .flexibleXPPickCounts(Map.of(0, 2));
        LifePathBuilder spread = validBuilder().flexibleXPSkills(Map.of(0, Map.of("A", 60)))
                                       .flexibleXPAbilities(Map.of(0, Map.of("Melee Specialist", 90)))
                                       .flexibleXPEdge(Map.of(0, 150))
                                       .flexibleXPPickCounts(Map.of(0, 2));

        assertEquals(LifePathXPCostCalculator.calculateXPCost(allSkills),
              LifePathXPCostCalculator.calculateXPCost(spread),
              "Items are priced by value, whichever award map they come from.");
    }

    @Test
    void testFlexibleSetCost_ScalesWithItsPickCount() {
        Map<Integer, Map<String, Integer>> set = Map.of(0, Map.of("A", 100, "B", 100, "C", 100, "D", 100));

        int onePick = LifePathXPCostCalculator.calculateXPCost(validBuilder().flexibleXPSkills(set)
                                                                    .flexibleXPPickCounts(Map.of(0, 1)));
        int threePicks = LifePathXPCostCalculator.calculateXPCost(validBuilder().flexibleXPSkills(set)
                                                                       .flexibleXPPickCounts(Map.of(0, 3)));

        assertEquals(onePick * 3, threePicks, "Each pick from a set should cost that set's weighted average.");
    }

    @Test
    void testFlexibleSets_ArePricedSeparatelyAndAdded() {
        // The plan's worked example. Set A: 30, 30 and 200, one pick, weighted average 48. Set B: 20 and 20, two
        // picks, 40. Pricing the sets together used to give 150; pricing them apart gives 88.
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("Gunnery/Mek", 30,
                    "Piloting/Mek", 30), 1, Map.of("Small Arms", 20, "Melee", 20)))
                                        .flexibleXPAbilities(Map.of(0, Map.of("Pain Resistance", 200)))
                                        .flexibleXPPickCounts(Map.of(0, 1, 1, 2));

        assertEquals(48, LifePathXPCostCalculator.calculateFlexibleSetCost(builder, 0),
              "Set A should price at its own weighted average times one pick.");
        assertEquals(40, LifePathXPCostCalculator.calculateFlexibleSetCost(builder, 1),
              "Set B should price at 20 times two picks.");
        assertEquals(88, LifePathXPCostCalculator.calculateXPCost(builder),
              "The Life Path's flexible cost is the sum of the set prices.");
    }

    @Test
    void testOneExpensiveSet_DoesNotChangeAnotherSetsPrice() {
        LifePathBuilder cheapOnly = validBuilder().flexibleXPSkills(Map.of(1, Map.of("Small Arms", 20, "Melee", 20)))
                                          .flexibleXPPickCounts(Map.of(1, 2));
        LifePathBuilder withExpensive = validBuilder().flexibleXPSkills(Map.of(1, Map.of("Small Arms", 20,
                    "Melee", 20)))
                                              .flexibleXPAbilities(Map.of(0, Map.of("Pain Resistance", 1000)))
                                              .flexibleXPPickCounts(Map.of(0, 1, 1, 2));

        assertEquals(LifePathXPCostCalculator.calculateFlexibleSetCost(cheapOnly, 1),
              LifePathXPCostCalculator.calculateFlexibleSetCost(withExpensive, 1),
              "A set is priced on its own items; an expensive neighbouring set cannot touch it.");
    }

    @Test
    void testFlexibleSets_CountSparseIndexes() {
        // Empty sets are stripped on save, so indexes need not run from zero.
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(4, Map.of("A", 100, "B", 100),
              9, Map.of("C", 100, "D", 100))).flexibleXPPickCounts(Map.of(4, 1, 9, 1));

        assertEquals(200, LifePathXPCostCalculator.calculateXPCost(builder),
              "Sparse set indexes should be priced the same as consecutive ones.");
    }

    @Test
    void testFlexibleSet_PricesEveryMapForThatSet() {
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("A", 100)))
                                        .flexibleXPEdge(Map.of(0, 100))
                                        .flexibleXPAbilities(Map.of(0, Map.of("Melee Specialist", 100)))
                                        .flexibleXPPickCounts(Map.of(0, 2));

        assertEquals(200, LifePathXPCostCalculator.calculateXPCost(builder),
              "Items in every award map belong to the set and share its pick count.");
    }

    @Test
    void testFlexibleSetCost_IsRoundedPerSet() {
        // Two sets whose exact prices are 47.5 each. Rounded once at the end they would come to 95; rounded per set
        // they come to 96, and the per-set figures shown in the wizard add up to what the file records.
        LifePathBuilder builder = validBuilder().flexibleXPSkills(Map.of(0, Map.of("A", 45, "B", 50),
                    1, Map.of("C", 45, "D", 50)))
                                        .flexibleXPPickCounts(Map.of(0, 1, 1, 1));

        int setZero = LifePathXPCostCalculator.calculateFlexibleSetCost(builder, 0);
        int setOne = LifePathXPCostCalculator.calculateFlexibleSetCost(builder, 1);

        assertEquals(setZero + setOne, LifePathXPCostCalculator.calculateFlexibleXPCost(builder),
              "The flexible total should be the sum of the rounded set prices.");
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
                                  .flexibleXPSkills(Map.of(0, Map.of("A", 60, "B", 90)))
                                  .flexibleXPPickCounts(Map.of(0, 1))
                                  .xpCost(0)
                                  .build();

        assertEquals(LifePathXPCostCalculator.calculateXPCost(LifePathBuilder.from(lifePath)),
              LifePathXPCostCalculator.calculateXPCost(lifePath),
              "Pricing a record and pricing the builder it came from should agree.");
    }
}
