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
 * Represents the various accolade recognitions that can be awarded by a faction.
 *
 * <p>Accolade recognitions indicate achievements or recognition that a faction grants, ranging from no accolade to
 * increasingly significant honors (such as money or units).</p>
 *
 * <p>Each enum constant corresponds to a specific accolade recognition, represented by an integer value. Provides
 * utility
 * methods for retrieving an accolade recognition from its numeric value or string name, as well as for comparison.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum FactionAccoladeLevel {
    /** No accolade awarded. */
    NO_ACCOLADE(0, FactionStandingLevel.STANDING_LEVEL_0),
    /** The faction has taken an interest in the campaign. */
    TAKING_NOTICE(1, FactionStandingLevel.STANDING_LEVEL_5),
    /** Quiet acknowledgment for services rendered. */
    FIELD_COMMENDATION(1, FactionStandingLevel.STANDING_LEVEL_5),
    /** Minor achievement noted in internal faction records. */
    OFFICIAL_COMMENDATION(2, FactionStandingLevel.STANDING_LEVEL_5),
    /** Public acknowledgement via a faction-wide news outlet. */
    PRESS_RECOGNITION(3, FactionStandingLevel.STANDING_LEVEL_6),
    /** Personal thanks issued from a senior commander or noble. */
    LETTER_OF_DISTINCTION(4, FactionStandingLevel.STANDING_LEVEL_6),
    /** Offer of adoption into the faction (or a lance of units) */
    ADOPTION_OR_LANCE(5, FactionStandingLevel.STANDING_LEVEL_6),
    /** Formal recognition from the faction head of state. */
    LETTER_FROM_HEAD_OF_STATE(6, FactionStandingLevel.STANDING_LEVEL_7),
    /** High-profile celebration or parade in the unit's honor or entered into the remembrance. */
    TRIUMPH_OR_REMEMBRANCE(7, FactionStandingLevel.STANDING_LEVEL_7),
    /** Statue made in the unit’s honor or a sibko founded from their bloodline */
    STATUE_OR_SIBKO(8, FactionStandingLevel.STANDING_LEVEL_8);

    /** The minimum possible accolade recognition (inclusive). */
    public static final int MIN_ACCOLADE_RECOGNITION = NO_ACCOLADE.getRecognition();
    /** The maximum possible accolade recognition (inclusive). */
    public static final int MAX_ACCOLADE_RECOGNITION = STATUE_OR_SIBKO.getRecognition();

    /** The integer representation of the accolade recognition. */
    private final int recognition;
    private final FactionStandingLevel requiredStanding;

    /**
     * Constructs a new {@link FactionAccoladeLevel} constant.
     *
     * @param recognition      the integer value associated with this accolade recognition
     * @param requiredStanding the minimum Faction Standing required for the accolade
     *
     * @author Illiani
     * @since 0.50.07
     */
    FactionAccoladeLevel(int recognition, FactionStandingLevel requiredStanding) {
        this.recognition = recognition;
        this.requiredStanding = requiredStanding;
    }

    /**
     * Gets the integer value associated with this accolade recognition.
     *
     * @return the accolade recognition as an integer
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getRecognition() {
        return recognition;
    }

    /**
     * Retrieves the required faction standing level for this accolade.
     *
     * @return the required Faction Standing
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingLevel getRequiredStanding() {
        return requiredStanding;
    }

    /**
     * Checks if this accolade recognition is equal to the given one.
     *
     * @param other the other {@link FactionAccoladeLevel} to compare with
     *
     * @return {@code true} if both are the same accolade recognition, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean is(FactionAccoladeLevel other) {
        return this == other;
    }

    /**
     * Retrieves the {@link FactionAccoladeLevel} corresponding to the given integer value. If no matching accolade
     * recognition is found, {@link #NO_ACCOLADE} is returned.
     *
     * @param recognition the integer value for which to get the accolade recognition
     *
     * @return the matching {@link FactionAccoladeLevel}, or {@link #NO_ACCOLADE} if not found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionAccoladeLevel getAccoladeRecognitionFromRecognition(int recognition) {
        for (FactionAccoladeLevel accoladeRecognition : FactionAccoladeLevel.values()) {
            if (accoladeRecognition.getRecognition() == recognition) {
                return accoladeRecognition;
            }
        }
        return NO_ACCOLADE;
    }

    /**
     * Retrieves the {@link FactionAccoladeLevel} from a string representation.
     *
     * <p>The string can be either the name of the enum constant or an integer value. If parsing fails,
     * {@link #NO_ACCOLADE} is returned and a warning is logged.</p>
     *
     * @param text the input string to parse
     *
     * @return the corresponding {@link FactionAccoladeLevel}, or {@link #NO_ACCOLADE} if not recognized
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionAccoladeLevel getAccoladeRecognitionFromString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {
        }

        try {
            int recognition = MathUtility.parseInt(text, NO_ACCOLADE.getRecognition());
            return getAccoladeRecognitionFromRecognition(recognition);
        } catch (Exception ignored) {
        }

        MMLogger.create(FactionAccoladeLevel.class)
              .warn("Unable to parse {} into an FactionAccoladeLevel. Returning NO_ACCOLADE.",
                    text);
        return NO_ACCOLADE;
    }
}
