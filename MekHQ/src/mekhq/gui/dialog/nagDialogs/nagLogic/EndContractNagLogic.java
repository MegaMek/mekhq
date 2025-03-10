/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;

import java.time.LocalDate;

public class EndContractNagLogic {
    /**
     * Checks if any contract in the current campaign has its end date set to today.
     *
     * <p>
     * This method is used to detect whether there are any active contracts
     * ending on the campaign's current local date. It iterates over the active
     * contracts for the campaign and compares each contract's ending date to today's date.
     * </p>
     *
     * @return {@code true} if a contract's end date matches today's date, otherwise {@code false}.
     */
    public static boolean isContractEnded(Campaign campaign) {
        LocalDate today = campaign.getLocalDate();

        // we can't use 'is date y after x', as once the end date has been passed,
        // the contract is removed from the list of active contracts

        // there is no reason to use a stream here, as there won't be enough iterations to warrant it
        for (Contract contract : campaign.getActiveContracts()) {
            if (contract.getEndingDate().equals(today)) {
                return true;
            }
        }

        return false;
    }
}
