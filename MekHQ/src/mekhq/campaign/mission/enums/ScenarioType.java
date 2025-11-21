/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.logging.MMLogger;

/**
 * Represents the type of scenario within MekHQ.
 *
 * <p>This enum defines specific scenario types that can occur in the game. It provides utility
 * methods to distinguish between various types and supports parsing from string representations, with graceful error
 * handling.</p>
 *
 * <p>Currently available scenario types:</p>
 * <ul>
 *   <li>{@code NONE} - Default scenario type.</li>
 *   <li>{@code SPECIAL_LOS_TECH} - Indicates a special LosTech-related scenario.</li>
 *   <li>{@code SPECIAL_RESUPPLY} - Indicates a resupply-related scenario.</li>
 * </ul>
 *
 * <p>This enum also supports utility methods to determine if a scenario is of a specific
 * type, such as {@link #isLosTech()} and {@link #isResupply()}.</p>
 */
public enum ScenarioType {
    NONE,
    SPECIAL_LOS_TECH,
    SPECIAL_RESUPPLY,
    SPECIAL_JAIL_BREAK,
    CONVOY,
    RIOT,
    OFFICIAL_CHALLENGE,
    HOSTILE_FACILITY;

    /**
     * @return {@code true} if the scenario is considered a LosTech scenario, {@code false} otherwise.
     */
    public boolean isLosTech() {
        return this == SPECIAL_LOS_TECH;
    }

    /**
     * @return {@code true} if the scenario is considered a convoy scenario, {@code false} otherwise. Convoy scenarios
     *       involve the defense or interception of a convoys with supplies, VIPs, or resupplies.
     */
    public boolean isConvoy() {
        return this == SPECIAL_RESUPPLY || this == CONVOY;
    }

    /**
     * @return {@code true} if the scenario is considered a Resupply scenario, {@code false} otherwise.
     */
    public boolean isResupply() {
        return this == SPECIAL_RESUPPLY;
    }

    /**
     * @return {@code true} if the scenario is considered a Jail Break scenario, {@code false} otherwise.
     */
    public boolean isJailBreak() {
        return this == SPECIAL_JAIL_BREAK;
    }

    /**
     * @return {@code true} if the scenario is considered an Official Challenge scenario, {@code false} otherwise.
     */
    public boolean isOfficialChallenge() {
        return this == OFFICIAL_CHALLENGE;
    }

    /**
     * @return {@code true} if the scenario is considered a Riot scenario, {@code false} otherwise.
     */
    public boolean isRiot() {
        return this == RIOT;
    }


    /**
     * @return {@code true} if the scenario is considered a hostile facility scenario, {@code false} otherwise.
     */
    public boolean isHostileFacility() {
        return this == HOSTILE_FACILITY;
    }

    /**
     * @return {@code true} if the instance is one of the special types; {@code false} otherwise.
     */
    public boolean isSpecial() {
        return this == SPECIAL_LOS_TECH ||
                     this == SPECIAL_RESUPPLY ||
                     this == SPECIAL_JAIL_BREAK ||
                     this == RIOT;
        // Official Challenge is purposefully left off here
    }

    /**
     * Parses a {@code ScenarioType} from a string input.
     *
     * <p>This method attempts to interpret the given string as either:</p>
     * <ol>
     *   <li>An integer index corresponding to the scenario type values, retrieved using {@link #values()}.</li>
     *   <li>A string matching the name of a specific {@code ScenarioType} constant, case-sensitive.</li>
     * </ol>
     *
     * <p>If parsing fails for both cases, an error is logged and the method returns the default
     * {@code NONE} value.</p>
     *
     * <p><b>Parsing Strategy:</b></p>
     * <ol>
     *   <li>First, it tries to parse the input as an integer and use it as an index for {@link #values()}.</li>
     *   <li>If that fails, it tries to match the input to a constant name using {@link #valueOf(String)}.</li>
     *   <li>If both attempts fail, it logs an error and returns {@code NONE}.</li>
     * </ol>
     *
     * <p>Note: If the input is invalid (e.g., a non-integer string or out-of-bounds index), the error
     * is logged via {@link MMLogger} and the fallback {@code NONE} is returned.</p>
     *
     * @param text the string to be parsed into a {@code ScenarioType}, representing either an integer index or the enum
     *             constant name.
     *
     * @return the parsed {@code ScenarioType}, or {@code NONE} if parsing fails.
     */
    public static ScenarioType parseFromString(final String text) {
        try {
            int value = Integer.parseInt(text.trim());
            return values()[value];
        } catch (Exception ignored) {}

        try {
            return valueOf(text.trim().toUpperCase());
        } catch (Exception ignored) {}

        MMLogger.create(ScenarioType.class)
              .warn("Unable to parse {} into an ScenarioType. Returning NONE.", text);

        return NONE;
    }
}
