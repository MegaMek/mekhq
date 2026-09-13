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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;

/**
 * Works out what a Life Path costs a character in XP.
 *
 * <p>The total is everything the path awards from its Fixed XP section, plus a share of its Flexible XP section,
 * less the path's own discount. The total can be negative: a path whose drawbacks, such as negative trait awards,
 * outweigh what it gives pays the character to take it.</p>
 *
 * <p><b>How the flexible share is charged.</b> The flexible section is a number of sets. Each set is a list of
 * individual awards and says how many of them the player takes, and the player picks from every set. Which items
 * they pick is up to them, so each set has to carry one price that covers any choice. That price is a weighted
 * average of the set's item values, charged once per pick from that set, and the Life Path pays the sum of its set
 * prices. Within a set the items are sorted by value and weighted with a bell curve centred on the middle of that
 * order, so the middle item sets the price and an unusually cheap or unusually expensive item barely moves it. The
 * weight of the item at position {@code p} is {@code exp(-0.5 * ((p - middle) / 0.5) ^ 2)}, where {@code middle}
 * is {@code (itemCount - 1) / 2}.</p>
 *
 * <p>Example: set A holds Gunnery/Mek 30 XP, Piloting/Mek 30 XP and the SPA Pain Resistance at 200 XP, and the
 * player takes one. The middle item is a 30, and the other 30 and the 200 each carry about an eighth of its weight,
 * so the set prices at 48 XP rather than the flat average of 87. Set B holds Small Arms 20 XP and Melee 20 XP, and
 * the player takes both, so it prices at 40 XP. The flexible section costs 88 XP, whichever items the player
 * chooses. Pricing the two sets together, as the calculator once did, would have charged 150.</p>
 *
 * <p>A flat average would let one expensive item drag a set's price up even for a player who never takes it, and
 * would make the price jump every time an author added an outlier. Pricing sets separately keeps an expensive set
 * from touching a cheap one.</p>
 *
 * @since 0.50.11
 */
public class LifePathXPCostCalculator {
    /**
     * How quickly an item's weight falls away as it moves from the middle of its set's value order, measured in
     * positions.
     *
     * <p>At 0.5 an item one position from the middle carries about an eighth of the middle item's weight, which is
     * what keeps a single outlier from setting a set's price.</p>
     */
    private static final double GAUSSIAN_SPREAD = 0.5;

    /**
     * Returns the XP a Life Path costs.
     *
     * @param lifePath the Life Path being priced, finished or still under construction
     *
     * @return the XP cost, which is negative when the path's drawbacks outweigh its awards
     *
     * @since 0.50.11
     */
    public static int calculateXPCost(LifePathBuilder lifePath) {
        int discount = lifePath.xpDiscount() == null ? 0 : lifePath.xpDiscount();

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
        globalCost += calculateFlexibleXPCost(lifePath);

        // Not floored at zero: a path whose drawbacks outweigh its awards pays the character to take it.
        return globalCost;
    }

    /**
     * Returns the XP a finished Life Path costs.
     *
     * @param lifePath the Life Path to price
     *
     * @return the XP cost, which is negative when the path's drawbacks outweigh its awards
     *
     * @since 0.50.11
     */
    public static int calculateXPCost(LifePath lifePath) {
        return calculateXPCost(LifePathBuilder.from(lifePath));
    }

    /**
     * Returns the XP the whole flexible section costs: every set's price added together.
     *
     * @param lifePath the Life Path whose flexible sets are being priced
     *
     * @return the flexible XP cost, zero when there are no sets or none allow a pick
     *
     * @since 0.50.11
     */
    public static int calculateFlexibleXPCost(LifePathBuilder lifePath) {
        int flexibleCost = 0;

        for (int setIndex : LifePath.flexibleXPGroupKeys(lifePath)) {
            flexibleCost += calculateFlexibleSetCost(lifePath, setIndex);
        }

        return flexibleCost;
    }

    /**
     * Returns the XP one flexible set costs: the weighted average of its item values, multiplied by the number of
     * items the player picks from it, rounded to the nearest XP.
     *
     * <p>Rounded here, per set, so that the per-set prices the wizard shows add up to the total the file records.
     * See the class documentation for why the average is weighted rather than flat.</p>
     *
     * @param lifePath the Life Path the set belongs to
     * @param setIndex the set's index
     *
     * @return the set's price, zero when it has no items or allows no picks
     *
     * @since 0.50.11
     */
    public static int calculateFlexibleSetCost(LifePathBuilder lifePath, int setIndex) {
        Map<Integer, Integer> pickCounts = lifePath.flexibleXPPickCounts();
        Integer storedPickCount = pickCounts == null ? null : pickCounts.get(setIndex);
        int pickCount = storedPickCount == null ? 0 : storedPickCount;

        if (pickCount <= 0) {
            return 0;
        }

        List<Integer> itemValues = LifePath.flexibleXPItemValues(lifePath, setIndex);
        if (itemValues.isEmpty()) {
            return 0;
        }

        return (int) Math.round(weightedMiddleAverage(itemValues) * pickCount);
    }

    /**
     * Returns the average of the values with a bell curve centred on the middle one.
     *
     * @param values the values to average; at least one
     *
     * @return the weighted average
     *
     * @since 0.50.11
     */
    private static double weightedMiddleAverage(List<Integer> values) {
        // Sorted so that "the middle item" means the middle by value, not whichever the author happened to add first.
        List<Integer> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);

        double middlePosition = (sortedValues.size() - 1) / 2.0;
        double weightedTotal = 0;
        double totalWeight = 0;

        for (int position = 0; position < sortedValues.size(); position++) {
            double offset = (position - middlePosition) / GAUSSIAN_SPREAD;
            double weight = Math.exp(-0.5 * offset * offset);

            weightedTotal += weight * sortedValues.get(position);
            totalWeight += weight;
        }

        return weightedTotal / totalWeight;
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
