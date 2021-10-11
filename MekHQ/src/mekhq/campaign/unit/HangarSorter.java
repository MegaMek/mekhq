/*
 * HangarSorter.java
 *
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.unit;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.campaign.Hangar;

/**
 * Creates sorted views of a hangar.
 */
public class HangarSorter {
    private final boolean weightClassSorted;
    private final boolean weightSorted;
    private final boolean unitTypeSorted;

    /**
     * Creates a new instance of the HangarSorter class.
     *
     * @param weightClassSorted True if the unit list is sorted by weight class in format heaviest to lightest, otherwise false
     * @param weightSorted      True if the unit list is sorted by weight descending, otherwise false
     * @param unitTypeSorted    True if the unit list is sorted by the unit type
     */
    public HangarSorter(boolean weightClassSorted, boolean weightSorted,
        boolean unitTypeSorted) {
        this.weightClassSorted = weightClassSorted;
        this.weightSorted = weightSorted;
        this.unitTypeSorted = unitTypeSorted;
    }

    /**
     * @return A new HangarSorter that sorts units by their default order.
     */
    public static HangarSorter defaultSorting() {
        return new HangarSorter(true, true, true);
    }

    /**
     * @return A new HangarSorter that sorts units by their weight, descending.
     */
    public static HangarSorter weightSorted() {
        return new HangarSorter(false, true, false);
    }

    /**
     * Gets a sorted list of units from the given hangar.
     * @param hangar The hangar to retrieve units from in sorted order.
     * @return A sorted list of units.
     */
    public List<Unit> getUnits(Hangar hangar) {
        return sort(hangar.getUnitsStream()).collect(Collectors.toList());
    }

    /**
     * Executes a consumer function on each unit in the hangar in sorted order.
     * @param hangar The hangar to retrieve units from in sorted order.
     * @param consumer A function to apply to each unit.
     */
    public void forEachUnit(Hangar hangar, Consumer<Unit> consumer) {
        sort(hangar.getUnitsStream()).forEach(consumer);
    }

    /**
     * This sorts a stream of units, sorted alphabetically and potentially by other methods
     * @return a stream with the applicable sort format
     */
    public Stream<Unit> sort(final Stream<Unit> units) {
        Stream<Unit> stream = units.sorted(Comparator.comparing(Unit::getName, new NaturalOrderComparator()));

        if (weightClassSorted || weightSorted || unitTypeSorted) {
            // We need to determine these by both the weight sorted and weight class sorted values,
            // as to properly sort by weight class and weight we should do both at the same time
            if (weightSorted && weightClassSorted) {
                stream = stream.sorted((lhs, rhs) -> {
                    int weightClass1 = lhs.getEntity().getWeightClass();
                    int weightClass2 = rhs.getEntity().getWeightClass();
                    if (weightClass1 == weightClass2) {
                        return Double.compare(rhs.getEntity().getWeight(), lhs.getEntity().getWeight());
                    } else {
                        return weightClass2 - weightClass1;
                    }
                });
            } else if (weightClassSorted) {
                stream = stream.sorted(Comparator.comparingInt(o -> o.getEntity().getWeightClass()));
            } else if (weightSorted) {
                // Sorted in descending order of weights
                stream = stream.sorted(Comparator.comparingDouble(o -> o.getEntity().getWeight()));
            }

            if (unitTypeSorted) {
                stream = stream.sorted(Comparator.comparingInt(e -> e.getEntity().getUnitType()));
            }
        }

        return stream;
    }
}
