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

import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.campaign.universe.Faction;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FactionDisplay {
    //region Variable Declarations
    private final Faction faction;
    private final String displayName;
    //endregion Variable Declarations

    //region Constructors
    public FactionDisplay(final Faction faction, final LocalDate today) {
        this.faction = faction;
        this.displayName = String.format("%s [%s]", getFaction().getFullName(today.getYear()),
                getFaction().getShortName());
    }
    //endregion Constructors

    //region Getters/Setters
    public Faction getFaction() {
        return faction;
    }
    //endregion Getters/Setters

    public static List<FactionDisplay> getSortedValidFactionDisplays(
            final Collection<Faction> factions, final LocalDate today) {
        final NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
        return factions.stream()
                .filter(faction -> faction.validIn(today))
                .map(faction -> new FactionDisplay(faction, today))
                .sorted((a, b) -> naturalOrderComparator.compare(a.toString(), b.toString()))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return displayName;
    }
}
