/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.icons.enums;

import static megamek.codeUtilities.MathUtility.clamp;

import megamek.common.units.Entity;
import mekhq.MHQConstants;
import mekhq.campaign.unit.Unit;

/**
 * This is the Operational Status of a force or unit, as part of automatically assigning and updating the force's
 * LayeredFormationIcon on a new day. It is also used to determine the Operation Status for a unit.
 *
 * @author Justin "Windchild" Bowen
 */
public enum OperationalStatus {
    //region Enum Declarations
    FULLY_OPERATIONAL(MHQConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_FULLY_OPERATIONAL_FILENAME),
    SUBSTANTIALLY_OPERATIONAL(MHQConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_SUBSTANTIALLY_OPERATIONAL_FILENAME),
    MARGINALLY_OPERATIONAL(MHQConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_MARGINALLY_OPERATIONAL_FILENAME),
    NOT_OPERATIONAL(MHQConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_NOT_OPERATIONAL_FILENAME),
    FACTORY_FRESH(MHQConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_FACTORY_FRESH_FILENAME);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String filename;
    //endregion Variable Declarations

    //region Constructors
    OperationalStatus(final String filename) {
        this.filename = filename;
    }
    //endregion Constructors

    //region Getters
    public String getFilename() {
        return filename;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isFullyOperational() {
        return this == FULLY_OPERATIONAL;
    }

    public boolean isSubstantiallyOperational() {
        return this == SUBSTANTIALLY_OPERATIONAL;
    }

    public boolean isMarginallyOperational() {
        return this == MARGINALLY_OPERATIONAL;
    }

    public boolean isNotOperational() {
        return this == NOT_OPERATIONAL;
    }

    public boolean isFactoryFresh() {
        return this == FACTORY_FRESH;
    }
    //endregion Boolean Comparison Methods

    /**
     * This is used to determine the operational status of the specified unit as part of determining the overall
     * operational status of a Force.
     *
     * @param unit the specified unit
     *
     * @return the determined operational status
     */
    public static OperationalStatus determineLayeredFormationIconOperationalStatus(final Unit unit) {
        if (unit.isMothballing() || unit.isMothballed() || !unit.isPresent() || unit.isRefitting()
                  || !unit.isRepairable() || !unit.isFunctional()) {
            return NOT_OPERATIONAL;
        }

        return switch (unit.getDamageState()) {
            case Entity.DMG_NONE -> FULLY_OPERATIONAL;
            case Entity.DMG_LIGHT, Entity.DMG_MODERATE -> SUBSTANTIALLY_OPERATIONAL;
            case Entity.DMG_HEAVY, Entity.DMG_CRIPPLED -> MARGINALLY_OPERATIONAL;
            default -> NOT_OPERATIONAL;
        };
    }

    /**
     * Retrieves the {@code LayeredFormationIconOperationalStatus} corresponding to the given ordinal value.
     * <p>
     * If the specified ordinal is out of range, it will be clamped to ensure it lies within the valid range of the
     * available enumeration values.
     *
     * @param ordinal the ordinal value to map to a {@code LayeredFormationIconOperationalStatus}. If the value is less than
     *                0, it will be clamped to 0. If it exceeds the maximum ordinal value, it will be clamped to the
     *                last index.
     *
     * @return the corresponding {@code LayeredFormationIconOperationalStatus} enum value for the adjusted ordinal.
     */
    public static OperationalStatus fromInt(int ordinal) {
        ordinal = clamp(ordinal, 0, values().length);

        return values()[ordinal];
    }
}
