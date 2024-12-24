/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.icons.enums;

import megamek.common.Entity;
import mekhq.MHQConstants;
import mekhq.campaign.unit.Unit;

import static megamek.codeUtilities.MathUtility.clamp;

/**
 * This is the Operational Status of a force or unit, as part of automatically assigning and
 * updating the force's LayeredForceIcon on a new day. It is also used to determine the Operation
 * Status for a unit.
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
     * This is used to determine the operational status of the specified unit as part of determining
     * the overall operational status of a Force.
     * @param unit the specified unit
     * @return the determined operational status
     */
    public static OperationalStatus determineLayeredForceIconOperationalStatus(final Unit unit) {
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
     * Retrieves the {@code LayeredForceIconOperationalStatus} corresponding to the given ordinal value.
     * <p>
     * If the specified ordinal is out of range, it will be clamped to ensure it lies within the valid range
     * of the available enumeration values.
     *
     * @param ordinal the ordinal value to map to a {@code LayeredForceIconOperationalStatus}.
     *                If the value is less than 0, it will be clamped to 0. If it exceeds the
     *                maximum ordinal value, it will be clamped to the last index.
     * @return the corresponding {@code LayeredForceIconOperationalStatus} enum value for the adjusted ordinal.
     */
    public static OperationalStatus fromInt(int ordinal) {
        ordinal = clamp(ordinal, 0, values().length);

        return values()[ordinal];
    }
}
