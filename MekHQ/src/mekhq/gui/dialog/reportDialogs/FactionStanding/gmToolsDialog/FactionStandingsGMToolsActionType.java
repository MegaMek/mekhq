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
package mekhq.gui.dialog.reportDialogs.FactionStanding.gmToolsDialog;

/**
 * Enum representing the types of actions that can be performed on faction standings through GM (Game Master) tools.
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum FactionStandingsGMToolsActionType {
    /**
     * Resets all faction regard values to their default state, as defined by the campaign faction and game date.
     */
    RESET_ALL_REGARD,
    /**
     * Sets all faction regard values to zero.
     */
    ZERO_ALL_REGARD,
    /**
     * Updates the regard value for a specific faction. This action is typically performed to adjust the standings of a
     * particular faction to a desired value.
     */
    SET_SPECIFIC_REGARD,
    /**
     * Updates faction standings based on historical contract data (i.e., completed
     * {@link mekhq.campaign.mission.Mission} objects).
     */
    UPDATE_HISTORIC_CONTRACTS
}
