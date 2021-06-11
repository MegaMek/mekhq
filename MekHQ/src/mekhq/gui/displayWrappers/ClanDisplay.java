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
package mekhq.gui.displayWrappers;

import megamek.common.annotations.Nullable;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.campaign.personnel.Clan;
import mekhq.campaign.universe.Faction;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * I only exist because of our current Clan/Faction split, and need to be removed alongside Clan.
 */
@Deprecated
public class ClanDisplay {
    //region Variable Declarations
    private final Clan clan;
    private final String displayName;
    //endregion Variable Declarations

    //region Constructors
    public ClanDisplay(final Clan clan, final LocalDate today) {
        this.clan = clan;
        this.displayName = String.format("%s [%s]", getClan().getFullName(today.getYear()),
                getClan().getCode());
    }
    //endregion Constructors

    //region Getters/Setters
    public Clan getClan() {
        return clan;
    }
    //endregion Getters/Setters

    public static List<ClanDisplay> getSortedClanDisplays(
            final Collection<Clan> clans, final LocalDate today) {
        final NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
        return clans.stream()
                .map(clan -> new ClanDisplay(clan, today))
                .sorted((a, b) -> naturalOrderComparator.compare(a.toString(), b.toString()))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (other == null) {
            return false;
        } else if (this == other) {
            return true;
        } else if (other instanceof ClanDisplay) {
            return getClan().equals(((ClanDisplay) other).getClan());
        } else if (other instanceof Faction) {
            return getClan().equals(other);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getClan().hashCode();
    }
}
