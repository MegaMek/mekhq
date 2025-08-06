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

import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_0;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_5;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_6;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_7;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_8;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

/**
 * Represents the various accolade recognitions that can be awarded by a faction.
 *
 * <p>Accolade recognitions indicate achievements or recognition that a faction grants, ranging from no accolade to
 * increasingly significant honors (such as money or units).</p>
 *
 * <p>Each enum constant corresponds to a specific accolade recognition, represented by an integer value. Provides
 * utility methods for retrieving an accolade recognition from its numeric value or string name, as well as for
 * comparison.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum FactionAccoladeLevel {
    /**
     * No accolade awarded.
     */
    NO_ACCOLADE("NO_ACCOLADE", 0, STANDING_LEVEL_0.getStandingLevel(), true, true),

    /**
     * Typically signifies the first recognition by a faction.
     */
    TAKING_NOTICE_0("TAKING_NOTICE", 1, STANDING_LEVEL_5.getStandingLevel(), true, true),

    /**
     * Represents later recognition by a faction at higher standing.
     */
    TAKING_NOTICE_1("TAKING_NOTICE", 2, STANDING_LEVEL_5.getStandingLevel(), true, true),

    /**
     * The unit or individual becomes notable enough to be indexed by faction or public searches.
     */
    APPEARING_IN_SEARCHES("APPEARING_IN_SEARCHES", 3, STANDING_LEVEL_6.getStandingLevel(), true, true),

    /**
     * The recipient is awarded a monetary reward based on their standing.
     */
    CASH_BONUS_0("CASH_BONUS", 4, STANDING_LEVEL_6.getStandingLevel(), true, true),

    /**
     * The unit or individual receives media attention for their achievements.
     */
    PRESS_RECOGNITION("PRESS_RECOGNITION", 5, STANDING_LEVEL_6.getStandingLevel(), true, true),

    /**
     * An additional or higher monetary reward recognizing continued accomplishments.
     */
    CASH_BONUS_1("CASH_BONUS", 6, STANDING_LEVEL_6.getStandingLevel(), true, true),

    /**
     * The unit or individual is featured in promotional or morale-boosting media.
     */
    PROPAGANDA_REEL("PROPAGANDA_REEL", 7, STANDING_LEVEL_6.getStandingLevel(), true, true),

    /**
     * Recognizes significant honor through factional adoption or Mek gift.
     */
    ADOPTION_OR_MEKS("ADOPTION_OR_MEKS", 8, STANDING_LEVEL_6.getStandingLevel(), false, false),

    /**
     * Reflects further increased monetary rewards at higher standing.
     */
    CASH_BONUS_2("CASH_BONUS", 9, STANDING_LEVEL_7.getStandingLevel(), false, false),

    /**
     * Granted in recognition of major victories or in memorial of distinguished service.
     */
    TRIUMPH_OR_REMEMBRANCE("TRIUMPH_OR_REMEMBRANCE", 10, STANDING_LEVEL_7.getStandingLevel(), false, false),

    /**
     * An even more significant monetary reward corresponding to greater achievements.
     */
    CASH_BONUS_3("CASH_BONUS", 11, STANDING_LEVEL_7.getStandingLevel(), false, false),

    /**
     * Represents one of the highest honors, signifying legendary status.
     */
    STATUE_OR_SIBKO("STATUE_OR_SIBKO", 12, STANDING_LEVEL_8.getStandingLevel(), false, false),

    /**
     * The highest level of monetary recognition afforded by the awarding faction.
     */
    CASH_BONUS_4("CASH_BONUS", 13, STANDING_LEVEL_8.getStandingLevel(), false, false),

    /**
     * A highly prestigious honor indicating direct recognition from the faction’s leader.
     */
    LETTER_FROM_HEAD_OF_STATE("LETTER_FROM_HEAD_OF_STATE", 14, STANDING_LEVEL_8.getStandingLevel(), false, false);

    private final String lookupName;
    private final int recognition;
    private final int requiredStandingLevel;
    private final boolean mercenarySuitable;
    private final boolean pirateSuitable;

    /**
     * Constructs a new {@link FactionAccoladeLevel} constant.
     *
     * @param lookupName            the string value used to fetch information about this accolade
     * @param recognition           the integer value associated with this accolade recognition
     * @param requiredStandingLevel the minimum Faction Standing required for the accolade
     * @param mercenarySuitable     whether the accolade level is suitable for mercenary factions
     *
     * @author Illiani
     * @since 0.50.07
     */
    FactionAccoladeLevel(String lookupName, int recognition, int requiredStandingLevel, boolean mercenarySuitable,
          boolean pirateSuitable) {
        this.lookupName = lookupName;
        this.recognition = recognition;
        this.requiredStandingLevel = requiredStandingLevel;
        this.mercenarySuitable = mercenarySuitable;
        this.pirateSuitable = pirateSuitable;
    }

    /**
     * Retrieves the string value used to fetch information about this accolade.
     *
     * @return lookup name associated with this accolade recognition
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getLookupName() {
        return lookupName;
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
    public int getRequiredStandingLevel() {
        return requiredStandingLevel;
    }

    /**
     * Determines whether this accolade is suitable for a mercenary faction.
     *
     * @return {@code true} if the accolade is deemed suitable for mercenaries, {@code false} otherwise.
     */
    public boolean isMercenarySuitable() {
        return mercenarySuitable;
    }

    /**
     * Determines whether this accolade is suitable for a pirate faction.
     *
     * @return {@code true} if the accolade is deemed suitable for pirates, {@code false} otherwise.
     */
    public boolean isPirateSuitable() {
        return pirateSuitable;
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

    @Override
    public String toString() {
        return this.getLookupName().replace("_", " ") + " (" + this.getRecognition() + ")";
    }
}
