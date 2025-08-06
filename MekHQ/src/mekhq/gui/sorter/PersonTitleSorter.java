/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.sorter;

import java.util.Comparator;

import megamek.common.annotations.Nullable;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.campaign.personnel.Person;

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
