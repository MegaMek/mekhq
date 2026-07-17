/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.enums;

/**
 * The kind of starting location a mercenary or pirate campaign requests when a new campaign is created.
 *
 * <p>Aligned campaigns (Great Houses, Periphery states, Clans, and so on) always begin with their own faction and
 * therefore do not use these modes; they only choose between their faction capital and a random hiring hall in their
 * own territory.</p>
 *
 * @since 0.51.0
 */
public enum StartingLocationMode {
    /** Start on the mercenary capital (or the pirate haven) for the current campaign date. */
    MERCENARY_CAPITAL,
    /** Start on the capital of a random, eligible Inner Sphere faction (the historical semi-random behaviour). */
    RANDOM,
    /** Start on the capital of a random Inner Sphere major or super power. */
    RANDOM_GREAT_HOUSE,
    /** Start on the capital of a random Periphery faction. */
    RANDOM_PERIPHERY,
    /** Start on the capital of a specific faction chosen by the player. */
    SPECIFIC_FACTION;

    /**
     * @return {@code true} if this mode requires the player to pick a specific faction from a dropdown
     */
    public boolean isSpecificFaction() {
        return this == SPECIFIC_FACTION;
    }

    /**
     * @return {@code true} if this mode should offer the "include Deep Periphery" option
     */
    public boolean isRandomPeriphery() {
        return this == RANDOM_PERIPHERY;
    }
}
