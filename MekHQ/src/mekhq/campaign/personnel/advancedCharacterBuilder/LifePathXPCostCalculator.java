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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;

/**
 * Works out what a Life Path costs a character in XP.
 *
 * <p>The total is everything the path awards from its Fixed XP section, plus a share of its Flexible XP section,
 * less the path's own discount. A path can cost nothing, but never less than nothing.</p>
 *
 * <p><b>How the flexible share is charged.</b> The player picks some number of the flexible groups, and which ones
 * they pick is up to them, so the path has to carry one price that covers any choice. That price is a weighted
 * average of the groups, charged once per pick. The groups are sorted by cost and weighted with a bell curve
 * centred on the middle of that order, so the middle group sets the price and an unusually cheap or unusually
 * expensive group barely moves it. The weight of the group at position {@code p} is
 * {@code exp(-0.5 * ((p - middle) / 0.5) ^ 2)}, where {@code middle} is {@code (groupCount - 1) / 2}.</p>
 *
 * <p>Example: a Life Path has three flexible groups worth 60, 90 and 150 XP and lets the player pick two. The
 * middle group is the 90, and the 60 and the 150 each carry about an eighth of its weight, so the weighted average
 * is about 93 rather than the flat average of 100. Two picks therefore cost about 186. Add 200 XP of fixed skills
 * and take off a 50 XP discount and the path costs 336 XP, whichever two groups the player chooses.</p>
 *
 * <p>A flat average would let one expensive group drag the whole path's price up even for a player who never takes
 * it, and would make the price jump every time an author added an outlier group.</p>
 *
 * @since 0.50.11
 */
public class LifePathXPCostCalculator {
    /**
     * How quickly a flexible group's weight falls away as it moves from the middle of the cost order, measured in
     * positions.
     *
     * <p>At 0.5 a group one position from the middle carries about an eighth of the middle group's weight, which is
     * what keeps a single outlier group from setting the price.</p>
     */
    private static final double GAUSSIAN_SPREAD = 0.5;

    /**
     * Returns the XP a Life Path costs.
     *
     * @param lifePath the Life Path being priced, finished or still under construction
     *
     * @return the XP cost, never below zero
     *
     * @since 0.50.11
     */
    public static int calculateXPCost(LifePathBuilder lifePath) {
        int discount = lifePath.xpDiscount() == null ? 0 : lifePath.xpDiscount();
        int flexiblePickCount = lifePath.flexibleXPPickCount() == null ? 0 : lifePath.flexibleXPPickCount();

        // Basic Info
        int globalCost = -discount;

        // Fixed XP
        globalCost += getCost(lifePath.fixedXPAttributes(),
              lifePath.fixedXPFlexibleAttribute(),
              lifePath.fixedXPEdge(),
              lifePath.fixedXPTraits(),
              lifePath.fixedXPSkills(),
              lifePath.fixedXPMetaSkills(),
              lifePath.fixedXPNaturalAptitudes(),
              lifePath.fixedXPNaturalAptitudesMetaSkills(),
              lifePath.fixedXPAbilities());

        // Flexible XP
        if (flexiblePickCount > 0) {
            double costPerPick = getWeightedFlexibleCost(lifePath);
            globalCost += (int) Math.round(costPerPick * flexiblePickCount);
        }

        // We can have 0 cost Life Paths, but not negative
        return max(0, globalCost);
    }

    /**
     * Returns the XP a finished Life Path costs.
     *
     * @param lifePath the Life Path to price
     *
     * @return the XP cost, never below zero
     *
     * @since 0.50.11
     */
    public static int calculateXPCost(LifePath lifePath) {
        return calculateXPCost(LifePathBuilder.from(lifePath));
    }

    /**
     * Returns the weighted average cost of one flexible XP pick.
     *
     * <p>See the class documentation for why the average is weighted rather than flat.</p>
     *
     * @param lifePath the Life Path whose flexible groups are being priced
     *
     * @return the XP one pick costs, or zero when there are no flexible groups
     *
     * @since 0.50.11
     */
    private static double getWeightedFlexibleCost(LifePathBuilder lifePath) {
        SortedSet<Integer> groupKeys = LifePath.flexibleXPGroupKeys(lifePath.flexibleXPAttributes(),
              lifePath.flexibleXPEdge(),
              lifePath.flexibleXPFlexibleAttribute(),
              lifePath.flexibleXPTraits(),
              lifePath.flexibleXPSkills(),
              lifePath.flexibleXPMetaSkills(),
              lifePath.flexibleXPNaturalAptitudes(),
              lifePath.flexibleXPNaturalAptitudesMetaSkills(),
              lifePath.flexibleXPAbilities());

        if (groupKeys.isEmpty()) {
            return 0;
        }

        List<Integer> groupCosts = new ArrayList<>();
        for (int groupKey : groupKeys) {
            groupCosts.add(getGroupCost(lifePath, groupKey));
        }

        // Sorted so that "the middle group" means the middle by cost, not whichever index the author happened to
        // create first.
        Collections.sort(groupCosts);

        double middlePosition = (groupCosts.size() - 1) / 2.0;
        double weightedTotal = 0;
        double totalWeight = 0;

        for (int position = 0; position < groupCosts.size(); position++) {
            double offset = (position - middlePosition) / GAUSSIAN_SPREAD;
            double weight = Math.exp(-0.5 * offset * offset);

            weightedTotal += weight * groupCosts.get(position);
            totalWeight += weight;
        }

        return weightedTotal / totalWeight;
    }

    /**
     * Returns the XP held in a single flexible XP group.
     *
     * @param lifePath the Life Path the group belongs to
     * @param groupKey the group's index
     *
     * @return the group's total XP
     *
     * @since 0.50.11
     */
    private static int getGroupCost(LifePathBuilder lifePath, int groupKey) {
        int cost = 0;

        cost += singleValue(lifePath.flexibleXPEdge(), groupKey);
        cost += singleValue(lifePath.flexibleXPFlexibleAttribute(), groupKey);

        cost += nestedValues(lifePath.flexibleXPAttributes(), groupKey);
        cost += nestedValues(lifePath.flexibleXPTraits(), groupKey);
        cost += nestedValues(lifePath.flexibleXPSkills(), groupKey);
        cost += nestedValues(lifePath.flexibleXPMetaSkills(), groupKey);
        cost += nestedValues(lifePath.flexibleXPNaturalAptitudes(), groupKey);
        cost += nestedValues(lifePath.flexibleXPNaturalAptitudesMetaSkills(), groupKey);
        cost += nestedValues(lifePath.flexibleXPAbilities(), groupKey);

        return cost;
    }

    /**
     * Returns one group's single value, treating a missing or null entry as zero.
     *
     * @param groups   values keyed by group index
     * @param groupKey the group to read
     *
     * @return the value, or zero
     *
     * @since 0.50.11
     */
    private static int singleValue(Map<Integer, Integer> groups, int groupKey) {
        Integer value = groups.get(groupKey);

        return value == null ? 0 : value;
    }

    /**
     * Returns the sum of one group's scored entries, treating missing or null entries as zero.
     *
     * @param groups   scored entries keyed by group index
     * @param groupKey the group to read
     * @param <K>      the inner key type, which the total does not depend on
     *
     * @return the group's total
     *
     * @since 0.50.11
     */
    private static <K> int nestedValues(Map<Integer, Map<K, Integer>> groups, int groupKey) {
        Map<K, Integer> group = groups.get(groupKey);

        if (group == null) {
            return 0;
        }

        int total = 0;
        for (Integer value : group.values()) {
            if (value != null) {
                total += value;
            }
        }

        return total;
    }

    /**
     * Adds up the XP held in one section's nine award maps.
     *
     * @param attributes                 attribute awards, keyed by group then attribute
     * @param flexibleAttribute          "any attribute" awards, keyed by group
     * @param edge                       Edge awards, keyed by group
     * @param traits                     trait awards, keyed by group then trait
     * @param skills                     skill awards, keyed by group then skill name
     * @param metaSkills                 meta skill awards, keyed by group then meta skill
     * @param naturalAptitudes           natural aptitude awards, keyed by group then skill name
     * @param naturalAptitudesMetaSkills natural aptitude meta skill awards, keyed by group
     * @param abilities                  special ability awards, keyed by group then ability name
     *
     * @return the section's total XP
     *
     * @since 0.50.11
     */
    private static int getCost(Map<Integer, Map<SkillAttribute, Integer>> attributes,
          Map<Integer, Integer> flexibleAttribute, Map<Integer, Integer> edge,
          Map<Integer, Map<ATOWTraits, Integer>> traits, Map<Integer, Map<String, Integer>> skills,
          Map<Integer, Map<SkillSubType, Integer>> metaSkills, Map<Integer, Map<String, Integer>> naturalAptitudes,
          Map<Integer, Map<SkillSubType, Integer>> naturalAptitudesMetaSkills,
          Map<Integer, Map<String, Integer>> abilities) {
        int cost = 0;

        cost += sumGroups(flexibleAttribute);
        cost += sumGroups(edge);

        cost += sumNestedGroups(attributes);
        cost += sumNestedGroups(traits);
        cost += sumNestedGroups(skills);
        cost += sumNestedGroups(metaSkills);
        cost += sumNestedGroups(naturalAptitudes);
        cost += sumNestedGroups(naturalAptitudesMetaSkills);
        cost += sumNestedGroups(abilities);

        return cost;
    }

    /**
     * Adds up one value per group.
     *
     * @param groups values keyed by group index
     *
     * @return the total, treating a missing value as zero
     *
     * @since 0.50.11
     */
    private static int sumGroups(Map<Integer, Integer> groups) {
        int total = 0;

        for (Integer value : groups.values()) {
            if (value != null) {
                total += value;
            }
        }

        return total;
    }

    /**
     * Adds up every value in a group-keyed map of maps.
     *
     * <p>Nulls are skipped at both levels. A hand-edited file can leave either a whole group or a single entry
     * null, and unboxing one would take the wizard down with a {@link NullPointerException} rather than reporting
     * the bad file.</p>
     *
     * @param groups values keyed by group index, then by whatever the section awards
     * @param <K>    the inner key type, which the total does not depend on
     *
     * @return the total across every group
     *
     * @since 0.50.11
     */
    private static <K> int sumNestedGroups(Map<Integer, Map<K, Integer>> groups) {
        int total = 0;

        for (Map<K, Integer> group : groups.values()) {
            if (group == null) {
                continue;
            }

            for (Integer value : group.values()) {
                if (value != null) {
                    total += value;
                }
            }
        }

        return total;
    }
}
