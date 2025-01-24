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

/**
 * Represents the various types of forces available.
 *
 * It is used to classify and manipulate forces within the game.
 */
public enum ForceType {
    // region Enum Declarations
    /**
     * Standard force type, typically used for combat and general operations.
     */
    STANDARD("Standard"),

    /**
     * Support force type, generally ignored by MekHQ.
     */
    SUPPORT("Support"),

    /**
     * Convoy force type, typically used for transport and supply operations.
     */
    CONVOY("Convoy"),

    /**
     * Security force type, typically used for protection and guarding operations.
     */
    SECURITY("Security");


    // Fields
    private final String name;

    // Constructor
    /**
     * Constructs a {@code ForceType} with a specified name.
     *
     * @param name the name of the force type, used for displaying or referencing.
     */
    ForceType(String name) {
        this.name = name;
    }


    // region Getters
    /**
     * Returns the name of this force type.
     *
     * @return a string representing the name of the force type.
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this force type is {@code STANDARD}.
     *
     * @return {@code true} if this is the {@code STANDARD} type; otherwise, {@code false}.
     */
    public boolean isStandard() {
        return this == STANDARD;
    }

    /**
     * Checks if this force type is {@code SUPPORT}.
     *
     * @return {@code true} if this is the {@code SUPPORT} type; otherwise, {@code false}.
     */
    public boolean isSupport() {
        return this == SUPPORT;
    }

    /**
     * Checks if this force type is {@code CONVOY}.
     *
     * @return {@code true} if this is the {@code CONVOY} type; otherwise, {@code false}.
     */
    public boolean isConvoy() {
        return this == CONVOY;
    }


    /**
     * Checks if this force type is {@code SECURITY}.
     *
     * @return {@code true} if this is the {@code SECURITY} type; otherwise, {@code false}.
     */
    public boolean isSecurity() {
        return this == SECURITY;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    /**
     * Retrieves a {@code ForceType} based on its ordinal value.
     *
     * @param ordinal the ordinal index of the force type.
     * @return the corresponding {@code ForceType} if the ordinal is valid;
     *         otherwise, defaults to {@code STANDARD}.
     */
    public static ForceType fromOrdinal(int ordinal) {
        if ((ordinal >= 0) && (ordinal < values().length)) {
            return values()[ordinal];
        }

        MMLogger logger = MMLogger.create(ForceType.class);
        logger.error(String.format("Unknown ForceType ordinal: %s - returning STANDARD.", ordinal));

        return STANDARD;
    }
}
