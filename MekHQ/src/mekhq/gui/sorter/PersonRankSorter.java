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

public class PersonRankSorter implements Comparator<Person> {
    //region Variable Declarations
    private final PrisonerStatusSorter prisonerStatusSorter = new PrisonerStatusSorter();
    private final NaturalOrderComparator naturalOrderComparator;
    //endregion Variable Declarations

    //region Constructors
    public PersonRankSorter(final NaturalOrderComparator naturalOrderComparator) {
        this.naturalOrderComparator = naturalOrderComparator;
    }
    //endregion Constructors

    //region Getters
    public PrisonerStatusSorter getPrisonerStatusSorter() {
        return prisonerStatusSorter;
    }

    public NaturalOrderComparator getNaturalOrderComparator() {
        return naturalOrderComparator;
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

        // First we sort based on prisoner status
        final int prisonerStatusComparison = getPrisonerStatusSorter().compare(
              p0.getPrisonerStatus(), p1.getPrisonerStatus());
        if (prisonerStatusComparison != 0) {
            return prisonerStatusComparison;
        }

        // Both have the same prisoner status, so now we sort based on the ranks
        // This is done in the following way:
        // 1. Rank Numeric
        // 2. Rank Level
        // 3. Manei Domini Rank
        // 4. Rank Name (natural order)
        if (p0.getRankNumeric() == p1.getRankNumeric()) {
            if (p0.getRankLevel() == p1.getRankLevel()) {
                if (p0.getManeiDominiRank() == p1.getManeiDominiRank()) {
                    return getNaturalOrderComparator().compare(p1.getRankName(), p0.getRankName());
                } else {
                    return Integer.compare(p0.getManeiDominiRank().ordinal(), p1.getManeiDominiRank().ordinal());
                }
            } else {
                return Integer.compare(p0.getRankLevel(), p1.getRankLevel());
            }
        } else {
            return Integer.compare(p0.getRankNumeric(), p1.getRankNumeric());
        }
    }
}
