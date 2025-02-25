/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.enums;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

import java.util.ResourceBundle;

/**
 * The AtBMoraleLevel enum represents the different enemy morale conditions used by AtB systems.
 */
public enum AtBMoraleLevel {
    // region Enum Declarations
    ROUTED("AtBMoraleLevel.ROUTED.text", "AtBMoraleLevel.ROUTED.toolTipText"),
    CRITICAL("AtBMoraleLevel.CRITICAL.text", "AtBMoraleLevel.CRITICAL.toolTipText"),
    WEAKENED("AtBMoraleLevel.WEAKENED.text", "AtBMoraleLevel.WEAKENED.toolTipText"),
    STALEMATE("AtBMoraleLevel.STALEMATE.text", "AtBMoraleLevel.STALEMATE.toolTipText"),
    ADVANCING("AtBMoraleLevel.ADVANCING.text", "AtBMoraleLevel.ADVANCING.toolTipText"),
    DOMINATING("AtBMoraleLevel.DOMINATING.text", "AtBMoraleLevel.DOMINATING.toolTipText"),
    OVERWHELMING("AtBMoraleLevel.OVERWHELMING.text", "AtBMoraleLevel.OVERWHELMING.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    /**
     * Initializes a new {@link AtBMoraleLevel} object with the specified name and tooltip text.
     *
     * @param name         the resource key for the name of the Morale Level
     * @param toolTipText  the resource key for the tooltip text of the Morale Level
     */
    // region Constructors
    AtBMoraleLevel(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    // endregion Constructors

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
     * @return {@code true} if the current object is equal to {@code CRITICAL}, {@code false}
     * otherwise.
     */
    public boolean isCritical() {
        return this == CRITICAL;
    }

    /**
     * Checks if the current object is equal to the value of {@code WEAKENED}.
     *
     * @return {@code true} if the current object is equal to {@code WEAKENED}, {@code false}
     * otherwise.
     */
    public boolean isWeakened() {
        return this == WEAKENED;
    }

    /**
     * Checks if the current object is equal to the value of {@code STALEMATE}.
     *
     * @return {@code true} if the current object is equal to {@code STALEMATE}, {@code false}
     * otherwise.
     */
    public boolean isStalemate() {
        return this == STALEMATE;
    }

    /**
     * Checks if the current object is equal to the value of {@code ADVANCING}.
     *
     * @return {@code true} if the current object is equal to {@code ADVANCING}, {@code false}
     * otherwise.
     */
    public boolean isAdvancing() {
        return this == ADVANCING;
    }

    /**
     * Checks if the current object is equal to the value of {@code DOMINATING}.
     *
     * @return {@code true} if the current object is equal to {@code DOMINATING}, {@code false}
     * otherwise.
     */
    public boolean isDominating() {
        return this == DOMINATING;
    }

    /**
     * Checks if the current object is equal to the value of {@code OVERWHELMING}.
     *
     * @return {@code true} if the current object is equal to {@code OVERWHELMING}, {@code false}
     * otherwise.
     */
    public boolean isOverwhelming() {
        return this == OVERWHELMING;
    }
    // endregion Boolean Comparison Methods

    /**
     * Parses a string representation of a morale level and returns the corresponding
     * {@link AtBMoraleLevel} enum value.
     *
     * @param moraleLevel the string representation of a morale level
     * @return the {@link AtBMoraleLevel} enum value corresponding to the given morale level string,
     * or {@code STALEMATE} if the string cannot be parsed
     */
    // region File I/O
    public static AtBMoraleLevel parseFromString(final String moraleLevel) {
        try {
            return valueOf(moraleLevel);
        } catch (Exception ignored) {}

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
        } catch (Exception ignored) {}

        //start <50.01 compatibility handler, replace it after post-40.10.1 Milestone
        switch (moraleLevel) {
            case "BROKEN" -> {
                return ROUTED;
            }
            case "VERY_LOW" -> {
                return CRITICAL;
            }
            case "LOW" -> {
                return WEAKENED;
            }
            case "NORMAL" -> {
                return STALEMATE;
            }
            case "HIGH" -> {
                return ADVANCING;
            }
            case "VERY_HIGH" -> {
                return DOMINATING;
            }
            case "UNBREAKABLE" -> {
                return OVERWHELMING;
            }
            default -> {}
        }
        //end <50.01 compatibility handler

        MMLogger.create(AtBMoraleLevel.class)
                .error("Unable to parse " + moraleLevel + " into an AtBMoraleLevel. Returning NORMAL.");
        return STALEMATE;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
