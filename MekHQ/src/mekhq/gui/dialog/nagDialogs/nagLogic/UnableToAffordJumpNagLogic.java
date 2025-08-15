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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import java.util.Objects;

import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Money;

public class UnableToAffordJumpNagLogic {
    /**
     * Determines whether the campaign's current funds are insufficient to cover the cost of the next jump.
     *
     * <p>
     * This method compares the campaign's available funds with the calculated cost of the next jump stored in the
     * {@code nextJumpCost} field. If the funds are less than the jump cost, it returns {@code true}, indicating that
     * the jump cannot be afforded; otherwise, it returns {@code false}.
     * </p>
     *
     * @return {@code true} if the campaign's funds are less than the cost of the next jump; {@code false} otherwise.
     */
    public static boolean unableToAffordNextJump(Campaign campaign) {
        Money nextJumpCost = getNextJumpCost(campaign);
        return campaign.getFunds().isLessThan(nextJumpCost);
    }

    /**
     * Calculates the cost of the next jump based on the campaign's location and financial settings.
     *
     * <p>
     * This method retrieves the {@link JumpPath} for the campaign's current location and only calculates the jump cost
     * if the next system on the path differs from the current system. The actual jump cost is determined by the
     * campaign's settings, particularly whether contracts base their costs on the value of units in the player's TOE
     * (Table of Equipment).
     * </p>
     */
    public static Money getNextJumpCost(Campaign campaign) {
        CurrentLocation location = campaign.getLocation();
        JumpPath jumpPath = location.getJumpPath();

        if (jumpPath == null) {
            return Money.zero();
        }

        if (Objects.equals(jumpPath.getLastSystem(), location.getCurrentSystem())) {
            return Money.zero();
        }

        boolean isContractPayBasedOnToeUnitsValue = campaign.getCampaignOptions().isEquipmentContractBase();

        return campaign.calculateCostPerJump(true, isContractPayBasedOnToeUnitsValue);
    }
}
