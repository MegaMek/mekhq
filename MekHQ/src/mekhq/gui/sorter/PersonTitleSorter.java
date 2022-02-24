/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.sorter;

import megamek.common.annotations.Nullable;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.campaign.personnel.Person;

import java.util.Comparator;

public class PersonTitleSorter implements Comparator<Person> {
    //region Variable Declarations
    private final NaturalOrderComparator naturalOrderComparator;
    private final PersonRankSorter personRankSorter;
    //endregion Variable Declarations

    //region Constructors
    public PersonTitleSorter() {
        this.naturalOrderComparator = new NaturalOrderComparator();
        this.personRankSorter = new PersonRankSorter(getNaturalOrderComparator());
    }
    //endregion Constructors

    //region Getters
    public NaturalOrderComparator getNaturalOrderComparator() {
        return naturalOrderComparator;
    }

    public PersonRankSorter getPersonRankSorter() {
        return personRankSorter;
    }
    //endregion Getters

    @Override
    public int compare(final @Nullable Person p0, final @Nullable Person p1) {
        // Initial Checks
        if (p0 == p1) { // Reference Equality Desired
            return 0;
        } else if (p0 == null) {
            return -1;
        } else if (p1 == null) {
            return 1;
        }

        // Initial comparison based on their rank
        final int personRankComparison = getPersonRankSorter().compare(p0, p1);
        if (personRankComparison != 0) {
            return personRankComparison;
        }

        // Now we can natural order compare the person's full name
        return getNaturalOrderComparator().compare(p1.getFullName(), p0.getFullName());
    }
}
