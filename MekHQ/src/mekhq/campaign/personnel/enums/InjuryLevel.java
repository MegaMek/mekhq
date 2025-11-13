/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

/**
 * Injury levels are simple categorization values used in the display and prioritization of injuries. The order is
 * important: For display purposes, "later" injury levels are considered more important and have preference over
 * "earlier" ones.
 */
public enum InjuryLevel {
    //region Enum Declarations
    /** Not actually a real injury */
    NONE,
    /**
     * Low-level chronic injuries and diseases, not threatening under normal circumstances. Examples: Scars, tinnitus,
     * diabetes, lost limbs
     */
    CHRONIC,
    /**
     * Simple injuries, expected to heal without complications by themselves in almost all cases.
     */
    MINOR,
    /**
     * Important injuries, professional medical attention required about weekly to ensure proper healing.
     */
    MAJOR,
    /**
     * Life-threatening injuries, professional medical attention required on a daily basis.
     */
    DEADLY;
    //endregion Enum Declarations

    //region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isChronic() {
        return this == CHRONIC;
    }

    public boolean isMinor() {
        return this == MINOR;
    }

    public boolean isMajor() {
        return this == MAJOR;
    }

    public boolean isDeadly() {
        return this == DEADLY;
    }

    public boolean isMajorOrDeadly() {
        return isMajor() || isDeadly();
    }
    //endregion Boolean Comparison Methods
}
