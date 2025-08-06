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

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;

public class EndContractNagLogic {
    /**
     * Determines if any active contract in the current campaign ends today.
     *
     * <p>This method checks all active contracts in the campaign to determine whether any
     * contract's end date matches the specified date. It iterates through the list of active contracts and compares
     * each contract's ending date with the given date.</p>
     *
     * <p>
     * Note that once a contract's end date has passed, it is removed from the list of active contracts. Therefore, this
     * method only checks contracts currently considered active.
     * </p>
     *
     * @param today           The current local date to check against the contracts' ending dates.
     * @param activeContracts A list of {@link AtBContract} objects representing the campaign's active contracts.
     *
     * @return {@code true} if any contract ends on the specified date; {@code false} otherwise.
     */
    public static boolean isContractEnded(LocalDate today, List<AtBContract> activeContracts) {
        // We can't use 'is date y after x', as once the end date has passed,
        // the contract is removed from the list of active contracts.

        // There is no reason to use a stream here, as there won't be enough iterations to warrant it.
        for (Contract contract : activeContracts) {
            if (contract.getEndingDate().equals(today)) {
                return true;
            }
        }

        return false;
    }
}
