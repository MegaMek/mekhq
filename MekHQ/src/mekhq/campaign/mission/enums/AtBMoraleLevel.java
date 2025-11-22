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
package mekhq.campaign.mission.enums;

import java.util.ResourceBundle;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;

/**
 * The AtBMoraleLevel enum represents the different enemy morale conditions used by AtB systems.
 */
public enum AtBMoraleLevel {
    // region Enum Declarations
    ROUTED(-3, 7, "AtBMoraleLevel.ROUTED.text", "AtBMoraleLevel.ROUTED.toolTipText"),
    CRITICAL(-2, 6, "AtBMoraleLevel.CRITICAL.text", "AtBMoraleLevel.CRITICAL.toolTipText"),
    WEAKENED(-1, 5, "AtBMoraleLevel.WEAKENED.text", "AtBMoraleLevel.WEAKENED.toolTipText"),
    STALEMATE(0, 4, "AtBMoraleLevel.STALEMATE.text", "AtBMoraleLevel.STALEMATE.toolTipText"),
    ADVANCING(1, 3, "AtBMoraleLevel.ADVANCING.text", "AtBMoraleLevel.ADVANCING.toolTipText"),
    DOMINATING(2, 2, "AtBMoraleLevel.DOMINATING.text", "AtBMoraleLevel.DOMINATING.toolTipText"),
    OVERWHELMING(3, 1, "AtBMoraleLevel.OVERWHELMING.text", "AtBMoraleLevel.OVERWHELMING.toolTipText");

    public final static int MINIMUM_MORALE_LEVEL = ROUTED.getLevel();
    public final static int MAXIMUM_MORALE_LEVEL = OVERWHELMING.getLevel();
    // endregion Enum Declarations

    // region Variable Declarations
    private final int level;
    private final int crisisDieSize;
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    /**
     * Initializes a new {@link AtBMoraleLevel} object with the specified name and tooltip text.
     *
     * @param level         the severity of the morale level
     * @param crisisDieSize the number of sides on the die rolled to determine if a scenario is classified as a
     *                      'crisis'
     * @param name          the resource key for the name of the Morale Level
     * @param toolTipText   the resource key for the tooltip text of the Morale Level
     */
    // region Constructors
    AtBMoraleLevel(final int level, final int crisisDieSize, final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission",
              MekHQ.getMHQOptions().getLocale());
        this.level = level;
        this.crisisDieSize = crisisDieSize;
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    // endregion Constructors

    public int getLevel() {
        return level;
    }

    public int getCrisisDieSize() {
        return crisisDieSize;
    }

    /**
     * Retrieves the tooltip text associated with this object.
     *
     * @return the tooltip text
     */
    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    // endregion Getters

    // region Boolean Comparison Methods

    /**
     * Checks if the current object is equal to the value of {@code ROUTED}.
     *
     * @return {@code true} if the current object is equal to {@code ROUTED}, {@code false} otherwise.
     */
    public boolean isRouted() {
        return this == ROUTED;
    }

    /**
     * Checks if the current object is equal to the value of {@code CRITICAL}.
     *
     * @return {@code true} if the current object is equal to {@code CRITICAL}, {@code false} otherwise.
     */
    public boolean isCritical() {
        return this == CRITICAL;
    }

    /**
     * Checks if the current object is equal to the value of {@code WEAKENED}.
     *
     * @return {@code true} if the current object is equal to {@code WEAKENED}, {@code false} otherwise.
     */
    public boolean isWeakened() {
        return this == WEAKENED;
    }

    /**
     * Checks if the current object is equal to the value of {@code STALEMATE}.
     *
     * @return {@code true} if the current object is equal to {@code STALEMATE}, {@code false} otherwise.
     */
    public boolean isStalemate() {
        return this == STALEMATE;
    }

    /**
     * Checks if the current object is equal to the value of {@code ADVANCING}.
     *
     * @return {@code true} if the current object is equal to {@code ADVANCING}, {@code false} otherwise.
     */
    public boolean isAdvancing() {
        return this == ADVANCING;
    }

    /**
     * Checks if the current object is equal to the value of {@code DOMINATING}.
     *
     * @return {@code true} if the current object is equal to {@code DOMINATING}, {@code false} otherwise.
     */
    public boolean isDominating() {
        return this == DOMINATING;
    }

    /**
     * Checks if the current object is equal to the value of {@code OVERWHELMING}.
     *
     * @return {@code true} if the current object is equal to {@code OVERWHELMING}, {@code false} otherwise.
     */
    public boolean isOverwhelming() {
        return this == OVERWHELMING;
    }
    // endregion Boolean Comparison Methods

    /**
     * Returns the {@link AtBMoraleLevel} that corresponds to the specified integer level.
     *
     * <p>This method iterates over all defined {@link AtBMoraleLevel} values and returns the one whose
     * {@link #getLevel()} value matches the provided {@code level}. If no matching morale level exists, {@code null} is
     * returned.</p>
     *
     * @param level the integer morale level to parse
     *
     * @return the matching {@link AtBMoraleLevel}, or {@code null} if no defined morale level corresponds to the given
     *       value
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static @Nullable AtBMoraleLevel parseFromLevel(final int level) {
        for (AtBMoraleLevel atBMoraleLevel : AtBMoraleLevel.values()) {
            if (atBMoraleLevel.getLevel() == level) {
                return atBMoraleLevel;
            }
        }

        return null;
    }

    /**
     * Parses a string representation of a morale level and returns the corresponding {@link AtBMoraleLevel} enum
     * value.
     *
     * @param moraleLevel the string representation of a morale level
     *
     * @return the {@link AtBMoraleLevel} enum value corresponding to the given morale level string, or
     *       {@code STALEMATE} if the string cannot be parsed
     */
    // region File I/O
    public static AtBMoraleLevel parseFromString(final String moraleLevel) {
        try {
            return valueOf(moraleLevel);
        } catch (Exception ignored) {
        }

        try {
            switch (Integer.parseInt(moraleLevel)) {
                case 0:
                    return ROUTED;
                case 1:
                    return CRITICAL;
                case 2:
                    return WEAKENED;
                case 3:
                    return STALEMATE;
                case 4:
                    return ADVANCING;
                case 5:
                    return DOMINATING;
                case 6:
                    return OVERWHELMING;
                default:
                    break;
            }
        } catch (Exception ignored) {
        }

        MMLogger.create(AtBMoraleLevel.class)
              .error("Unable to parse {} into an AtBMoraleLevel. Returning STALEMATE.", moraleLevel);
        return STALEMATE;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
