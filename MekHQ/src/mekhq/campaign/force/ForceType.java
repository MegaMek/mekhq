/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.force;

import megamek.logging.MMLogger;

public enum ForceType {
    // region Enum Declarations
    STANDARD("Standard"),
    SUPPORT("Support"),
    CONVOY("Convoy"),
    SECURITY("Security");

    // Fields
    private final String name;

    // Constructor
    ForceType(String name) {
        this.name = name;
    }


    // region Getters
    public String getName() {
        return name;
    }

    public boolean isStandard() {
        return this == STANDARD;
    }

    public boolean isSupport() {
        return this == SUPPORT;
    }

    public boolean isConvoy() {
        return this == CONVOY;
    }

    public boolean isSecurity() {
        return this == SECURITY;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    public static ForceType fromOrdinal(int ordinal) {
        if ((ordinal >= 0) && (ordinal < values().length)) {
            return values()[ordinal];
        }

        MMLogger logger = MMLogger.create(ForceType.class);
        logger.error(String.format("Unknown ForceType ordinal: %s - returning COMBAT.", ordinal));

        return STANDARD;
    }
}
