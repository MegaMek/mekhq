/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;

/**
 * Holds data for Faction Standing ultimatum event. Each ultimatum is stored in its own JSON file.
 *
 * @param name                 the name of the ultimatum. Also used to build its resource keys and its file name
 * @param date                 the date of the ultimatum, as {@code yyyy-MM-dd}. Stored as a {@link String} for ease of
 *                             loading
 * @param notes                developer notes explaining the ultimatum, such as how its date was chosen. Never shown
 *                             to the player; may be {@code null}
 * @param affectedFactionCodes the codes of every faction whose campaigns receive this ultimatum. Never {@code null}
 * @param sides                the sides the player can choose between, in the order the picker shows them. The first
 *                             side delivers the opening offer. Never {@code null}
 * @param dissenterPreference  the {@link FactionStandingUltimatumSide#id() ID} of the side the dissenting officer
 *                             wants the campaign to choose, or {@link #ROGUE_PREFERENCE} if they want it to go rogue.
 *                             They leave the campaign if the player chooses anything else
 * @param isViolentTransition  {@code true} if the transition is violent
 * @param divisiveness         how divisive the ultimatum is. Added to the target number of the loyalty check each
 *                             character makes to follow the player's decision, so positive values make more characters
 *                             refuse. Between {@link #MINIMUM_DIVISIVENESS} and {@link #MAXIMUM_DIVISIVENESS}; absent
 *                             from a file means 0
 *
 * @author Illiani
 * @since 0.50.07
 */
@JsonPropertyOrder({ "name", "date", "notes", "affectedFactionCodes", "sides", "dissenterPreference",
                     "isViolentTransition", "divisiveness" })
public record FactionStandingUltimatumData(
      @JsonProperty("name") String name,
      @JsonProperty("date") String date,
      @JsonProperty("notes") @Nullable String notes,
      @JsonProperty("affectedFactionCodes") List<String> affectedFactionCodes,
      @JsonProperty("sides") List<FactionStandingUltimatumSide> sides,
      @JsonProperty("dissenterPreference") String dissenterPreference,
      @JsonProperty("isViolentTransition") boolean isViolentTransition,
      @JsonProperty("divisiveness") int divisiveness
) {
    private static final MMLogger LOGGER = MMLogger.create(FactionStandingUltimatumData.class);

    /**
     * The lowest divisiveness an ultimatum may have. The loyalty check is 2d6 against a target number of 6, so at -6
     * only characters with a poor loyalty modifier can fail it.
     */
    public static final int MINIMUM_DIVISIVENESS = -6;

    /**
     * The highest divisiveness an ultimatum may have. At +6 only characters with a strong loyalty modifier can pass the
     * loyalty check.
     */
    public static final int MAXIMUM_DIVISIVENESS = 6;

    /** The {@link #dissenterPreference()} meaning the dissenting officer wants the campaign to go rogue. */
    public static final String ROGUE_PREFERENCE = "ROGUE";

    /**
     * Normalizes {@code affectedFactionCodes} and {@code sides} into immutable, non-null lists.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public FactionStandingUltimatumData {
        affectedFactionCodes = affectedFactionCodes == null ? List.of() : List.copyOf(affectedFactionCodes);
        sides = sides == null ? List.of() : List.copyOf(sides);
    }

    /**
     * Finds one of this ultimatum's sides by its ID.
     *
     * @param sideId the side's ID
     *
     * @return the side, or {@code null} if this ultimatum has no side with that ID
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable FactionStandingUltimatumSide getSide(@Nullable String sideId) {
        if (sideId == null) {
            return null;
        }
        for (FactionStandingUltimatumSide side : sides) {
            // Sides from a hand-edited file may be missing their ID
            if (sideId.equals(side.id())) {
                return side;
            }
        }
        return null;
    }

    /**
     * Returns the date as a {@link LocalDate}, parsed from the date string.
     *
     * @return {@link LocalDate} representation of the date.
     *
     * @throws DateTimeParseException if the date format is invalid
     * @author Illiani
     * @since 0.50.07
     */
    @JsonIgnore
    public LocalDate getDate() {
        return LocalDate.parse(date);
    }

    /**
     * Returns the divisiveness to use in play, limited to {@link #MINIMUM_DIVISIVENESS} through
     * {@link #MAXIMUM_DIVISIVENESS}. A hand-edited file can hold any value, and an extreme one would make every loyalty
     * check pass or fail regardless of the character.
     *
     * @return the clamped divisiveness
     *
     * @author Illiani
     * @since 0.51.01
     */
    @JsonIgnore
    public int getEffectiveDivisiveness() {
        return Math.clamp(divisiveness, MINIMUM_DIVISIVENESS, MAXIMUM_DIVISIVENESS);
    }

    /**
     * Reads an ultimatum from its JSON file.
     *
     * @param ultimatumFile the file to read
     *
     * @return the ultimatum, or {@code null} if the file does not exist or could not be read (logged)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable FactionStandingUltimatumData deserialize(File ultimatumFile) {
        if (!ultimatumFile.exists()) {
            LOGGER.warn("Specified file {} does not exist", ultimatumFile.getPath());
            return null;
        }

        try {
            return FactionStandingUltimatumJson.fromFile(ultimatumFile, FactionStandingUltimatumData.class);
        } catch (Exception exception) {
            LOGGER.error("Error deserializing ultimatum {}", ultimatumFile.getPath(), exception);
            return null;
        }
    }

    /**
     * Writes this ultimatum to the given JSON file.
     *
     * @param outputFile the destination file
     *
     * @return {@code true} if the file was written, {@code false} if an error occurred (logged)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean serialize(File outputFile) {
        try {
            FactionStandingUltimatumJson.toFile(this, outputFile);
            return true;
        } catch (Exception exception) {
            LOGGER.error("Error serializing ultimatum {}", outputFile.getPath(), exception);
            return false;
        }
    }
}
