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
package mekhq.campaign.universe.factionStanding;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

/**
 * Enumerates the possible types of disciplinary actions (censures) that can be imposed by a faction due to low Faction
 * Standing or disciplinary issues.
 *
 * <p>These censures range from fines and forced retirements to more severe actions such as forced retirement or
 * replacement. This enumeration is used to represent outcomes resulting from faction standing events or penalties.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum FactionCensureLevel {
    /** The absence of any censure or disciplinary action. */
    NONE(0),
    /** A warning imposed as a form of censure. */
    WARNING(1),
    /** Mandatory retirement imposed on the campaign commander as censure. */
    COMMANDER_RETIREMENT(2),
    /** Imprisonment of the campaign commander as a disciplinary action. */
    COMMANDER_IMPRISONMENT(3),
    /** Replacement of all officers as a punitive measure. */
    LEADERSHIP_REPLACEMENT(4),
    /** Forcible disbanding of the campaign as a disciplinary action. */
    DISBAND(5);

    public static final int MIN_CENSURE_SEVERITY = NONE.getSeverity();
    public static final int MAX_CENSURE_SEVERITY = DISBAND.getSeverity();

    /** The severity level of this censure. Higher values indicate more severe censures. */
    private final int severity;

    /**
     * Constructs a FactionStandingCensure with the specified severity.
     *
     * @param severity the numeric severity level of this censure
     *
     * @author Illiani
     * @since 0.50.07
     */
    FactionCensureLevel(int severity) {
        this.severity = severity;
    }

    /**
     * Returns the severity level associated with this censure.
     *
     * @return the severity as an integer
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getSeverity() {
        return severity;
    }

    /**
     * Determines if this censure is the same as the provided censure.
     *
     * @param other the censure to compare with
     *
     * @return {@code true} if this censure and the provided censure are the same; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean is(FactionCensureLevel other) {
        return this == other;
    }

    /**
     * Retrieves the {@link FactionCensureLevel} corresponding to the specified severity value.
     * <p>
     * Iterates through all available censure levels and returns the one whose severity matches the provided value. If
     * no match is found, returns {@code NONE}.
     * </p>
     *
     * @param severity the severity level to search for
     *
     * @return the matching {@link FactionCensureLevel}, or {@code NONE} if not found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionCensureLevel getCensureLevelFromSeverity(int severity) {
        for (FactionCensureLevel censureLevel : FactionCensureLevel.values()) {
            if (censureLevel.getSeverity() == severity) {
                return censureLevel;
            }
        }
        return NONE;
    }

    /**
     * Parses the specified censure {@link String} into a {@link FactionCensureLevel} value.
     *
     * <p>The method first attempts to parse the text as an {@link Integer}, returning the corresponding ordinal
     * value from the {@link FactionCensureLevel} enum. If that fails, it then attempts to parse the text by its
     * name.</p>
     *
     * <p>If neither parsing attempt succeeds, it logs a warning and returns {@link FactionCensureLevel#NONE}.</p>
     *
     * @param text the {@link String} to parse, representing either an enum ordinal or name
     *
     * @return the matching {@link FactionCensureLevel}, or {@code FactionCensureLevel#NONE} if parsing fails
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionCensureLevel getCensureLevelFromCensureString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {
        }

        try {
            int severity = MathUtility.parseInt(text, NONE.getSeverity());
            return getCensureLevelFromSeverity(severity);
        } catch (Exception ignored) {
        }

        MMLogger.create(FactionCensureLevel.class)
              .warn("Unable to parse {} into an FactionCensureLevel. Returning NONE.",
                    text);
        return NONE;
    }
}
