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
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FactionDisplay is a display wrapper around a Faction, primarily to be used in ComboBoxes.
 * This removes the need to track based on the index, thus simplifying the dev work.
 */
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
        return getSortedFactionDisplays(factions.stream()
                .filter(faction -> faction.validIn(today))
                .collect(Collectors.toList()), today);
    }

    public static List<FactionDisplay> getSortedFactionDisplays(
            final Collection<Faction> factions, final LocalDate today) {
        final NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
        return factions.stream()
                .map(faction -> new FactionDisplay(faction, today))
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
        } else if (other instanceof FactionDisplay) {
            return getFaction().equals(((FactionDisplay) other).getFaction());
        } else if (other instanceof Faction) {
            return getFaction().equals(other);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getFaction().hashCode();
    }
}
