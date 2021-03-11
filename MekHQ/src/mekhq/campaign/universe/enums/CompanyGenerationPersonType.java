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

public enum CompanyGenerationPersonType {
    //region Enum Declarations
    COMPANY_COMMANDER,
    CAPTAIN,
    LIEUTENANT,
    MECHWARRIOR,
    SUPPORT,
    ASSISTANT;
    //endregion Enum Declarations

    //region Boolean Comparisons
    public boolean isCompanyCommander() {
        return this == COMPANY_COMMANDER;
    }

    public boolean isCaptain() {
        return this == CAPTAIN;
    }

    public boolean isLieutenant() {
        return this == LIEUTENANT;
    }

    public boolean isMechWarrior() {
        return this == MECHWARRIOR;
    }

    public boolean isSupport() {
        return this == SUPPORT;
    }

    public boolean isOfficer() {
        return isCaptain() || isLieutenant();
    }

    public boolean isCombat() {
        return isCompanyCommander() || isCaptain() || isLieutenant() || isMechWarrior();
    }
    //endregion Boolean Comparisons
}
