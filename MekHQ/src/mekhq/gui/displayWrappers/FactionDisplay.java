/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.universe.Faction;

import java.time.LocalDate;

public class FactionDisplay {
    //region Variable Declarations
    private final Faction faction;
    private final String displayName;
    //endregion Variable Declarations

    //region Constructors
    public FactionDisplay(final Faction faction, final int year) {
        this(faction, LocalDate.ofYearDay(year, 1));
    }

    public FactionDisplay(final Faction faction, final LocalDate year) {
        this.faction = faction;
        this.displayName = String.format("%s [%s]", faction.getFullName(year.getYear()), faction.getId());
    }
    //endregion Constructors

    //region Getters
    public Faction getFaction() {
        return faction;
    }
    //endregion Getters

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(final @Nullable Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof FactionDisplay)) {
            return false;
        } else {
            return getFaction().equals(((FactionDisplay) object).getFaction());
        }
    }

    @Override
    public int hashCode() {
        return getFaction().hashCode();
    }
}

