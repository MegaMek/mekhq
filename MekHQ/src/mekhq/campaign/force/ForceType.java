/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.force;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.logging.MMLogger;

/**
 * Represents the various types of forces available.
 * <p>
 * It is used to classify and manipulate forces within the game.
 * </p>
 */
public enum ForceType {
    // region Enum Declarations
    /**
     * Standard force type, typically used for combat.
     */
    STANDARD(true, false),

    /**
     * Support force type, used by forces that should be deployed in StratCon but not involved in combat.
     */
    SUPPORT(false, false),

    /**
     * Convoy force type, typically used by the Resupply module.
     */
    CONVOY(true, true),

    /**
     * Salvage force type, typically used in post-scenario salvage operations
     */
    SALVAGE(true, true),

    /**
     * Security force type, typically used by the Prisoner Events module.
     */
    SECURITY(true, true);

    // region Fields
    private final boolean standardizeParents;
    private final boolean childrenInherit;

    // region Constructor

    /**
     * Constructor for the {@code ForceType} enum.
     *
     * @param standardizeParents Whether changing to this ForceType changes the ForceType in all parent forces to
     *                           STANDARD
     * @param childrenInherit    Whether changing to this ForceType changes the ForceType in all child forces to this
     *                           ForceType.
     */
    ForceType(boolean standardizeParents, boolean childrenInherit) {
        this.standardizeParents = standardizeParents;
        this.childrenInherit = childrenInherit;
    }
    // endregion Constructor


    // region Getters

    /**
     * Retrieves the display name for the ForceType by fetching a localized label from the relevant resource bundle.
     *
     * <p>The method uses the {@code name} of the current instance to construct a resource
     * key in the format {@code [name].label}. This key is used to look up a localized string from the {@code ForceType}
     * resource bundle located in the {@code mekhq.resources} package. The formatted text at the specified key is
     * returned as the display name.</p>
     *
     * @return The localized display name for the current instance.
     */
    public String getDisplayName() {
        final String RESOURCE_BUNDLE = "mekhq.resources.ForceType";
        final String RESOURCE_KEY = name() + ".label";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the symbol associated with this ForceType.
     *
     * <p>The method determines the symbol to display for the current instance by looking up
     * a localization resource key in the {@code ForceType} resource bundle, with keys formatted as
     * {@code [enumName].symbol}.</p>
     *
     * <p>If the current instance is {@code STANDARD}, an empty string is returned as the symbol.</p>
     *
     * @return The localized symbol associated with the current instance, or an empty string if the instance is
     *       {@code STANDARD}.
     */
    public String getSymbol() {
        if (this == STANDARD) {
            return "";
        }

        final String RESOURCE_BUNDLE = "mekhq.resources.ForceType";
        final String RESOURCE_KEY = name() + ".symbol";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * This flag indicates whether, when changing to this ForceType, whether all parent forces should be changed to
     * STANDARD.
     *
     * @return {@code true} if parent relationships should be standardized; {@code false} otherwise.
     */
    public boolean shouldStandardizeParents() {
        return standardizeParents;
    }

    /**
     * This flag indicates whether, when changing to this ForceType, whether all child forces should be changed to the
     * same ForceType.
     *
     * @return {@code true} if children should inherit from parents; {@code false} otherwise.
     */
    public boolean shouldChildrenInherit() {
        return childrenInherit;
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
     * Checks if this force type is {@code SALVAGE}.
     *
     * @return {@code true} if this is the {@code SALVAGE} type; otherwise, {@code false}.
     */
    public boolean isSalvage() {
        return this == SALVAGE;
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
     *
     * @return the corresponding {@code ForceType} if the ordinal is valid; otherwise, defaults to {@code STANDARD}.
     */
    public static ForceType fromOrdinal(int ordinal) {
        if ((ordinal >= 0) && (ordinal < values().length)) {
            return values()[ordinal];
        }

        MMLogger logger = MMLogger.create(ForceType.class);
        logger.error("Unknown ForceType ordinal: {} - returning STANDARD.", ordinal);

        return STANDARD;
    }
}
