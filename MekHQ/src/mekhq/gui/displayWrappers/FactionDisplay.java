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
package mekhq.gui.displayWrappers;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.campaign.universe.Faction;

/**
 * FactionDisplay is a display wrapper around a Faction, primarily to be used in ComboBoxes. This removes the need to
 * track based on the index, thus simplifying the dev work.
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
