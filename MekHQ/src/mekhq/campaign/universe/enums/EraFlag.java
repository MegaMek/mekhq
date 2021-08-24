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
package mekhq.campaign.universe.enums;

public enum EraFlag {
    //region Enum Declarations
    PRE_SPACEFLIGHT,
    EARLY_SPACEFLIGHT,
    AGE_OF_WAR,
    STAR_LEAGUE,
    EARLY_SUCCESSION_WARS,
    LATE_SUCCESSION_WARS_LOSTECH,
    LATE_SUCCESSION_WARS_RENAISSANCE,
    CLAN_INVASION,
    CIVIL_WAR,
    JIHAD,
    EARLY_REPUBLIC,
    LATE_REPUBLIC,
    DARK_AGES,
    ILCLAN;
    //endregion Enum Declarations

    //region Boolean Comparison Methods
    public boolean isPreSpaceflight() {
        return this == PRE_SPACEFLIGHT;
    }

    public boolean isEarlySpaceflight() {
        return this == EARLY_SPACEFLIGHT;
    }

    public boolean isAgeOfWar() {
        return this == AGE_OF_WAR;
    }

    public boolean isStarLeague() {
        return this == STAR_LEAGUE;
    }

    public boolean isEarlySuccessionWars() {
        return this == EARLY_SUCCESSION_WARS;
    }

    public boolean isLateSuccessionWarsLosTech() {
        return this == LATE_SUCCESSION_WARS_LOSTECH;
    }

    public boolean isLateSuccessionWarsRenaissance() {
        return this == LATE_SUCCESSION_WARS_RENAISSANCE;
    }

    public boolean isClanInvasion() {
        return this == CLAN_INVASION;
    }

    public boolean isCivilWar() {
        return this == CIVIL_WAR;
    }

    public boolean isJihad() {
        return this == JIHAD;
    }

    public boolean isEarlyRepublic() {
        return this == EARLY_REPUBLIC;
    }

    public boolean isLateRepublic() {
        return this == LATE_REPUBLIC;
    }

    public boolean isDarkAges() {
        return this == DARK_AGES;
    }

    public boolean isIlClan() {
        return this == ILCLAN;
    }
    //endregion Boolean Comparison Methods
}
