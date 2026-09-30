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
package mekhq.campaign.digitalGM.stratCon.facility;

/**
 * The orders a player can give a deployed formation at a facility, or on an empty hex.
 *
 * <ul>
 *     <li>{@link #RECON}, {@link #RAID}, {@link #SABOTAGE} and {@link #ASSAULT} act on a facility the enemy holds,
 *     from its hex or next to it.</li>
 *     <li>{@link #FORTIFY} raises the tier of a facility the player's side holds, and {@link #REINFORCE} tops up its
 *     garrison, both from its hex.</li>
 *     <li>{@link #BUILD} raises a new player-held Outpost on an empty hex.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum FacilityOperation {
    RECON(false, true),
    RAID(true, true),
    SABOTAGE(false, true),
    ASSAULT(true, true),
    FORTIFY(false, false),
    REINFORCE(false, false),
    BUILD(false, false),
    /** Ambush the enemy's supply convoys on a road hex, cutting their supply line through it if won. */
    INTERDICT(true, false);

    private final boolean isCombat;
    private final boolean isAllowedFromAdjacentHex;

    FacilityOperation(boolean isCombat, boolean isAllowedFromAdjacentHex) {
        this.isCombat = isCombat;
        this.isAllowedFromAdjacentHex = isAllowedFromAdjacentHex;
    }

    /**
     * @return {@code true} if giving the order always starts a scenario on the facility
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isCombat() {
        return isCombat;
    }

    /**
     * @return {@code true} if a formation next to the facility may take the order, not only one on its hex
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isAllowedFromAdjacentHex() {
        return isAllowedFromAdjacentHex;
    }

    /**
     * @return {@code true} if the order improves a facility the player's side holds
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isOnOwnFacility() {
        return (this == FORTIFY) || (this == REINFORCE);
    }

    /**
     * @return {@code true} if the order targets a facility the enemy holds
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isAgainstEnemyFacility() {
        return (this == RECON) || (this == RAID) || (this == SABOTAGE) || (this == ASSAULT);
    }
}
