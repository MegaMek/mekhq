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

import java.util.List;

import mekhq.campaign.mission.AtBContract;

/**
 * Provides nag logic related to the single-drop setup flow.
 *
 * <p>This helper exposes static predicates that inspect active contracts
 * to decide whether a nag dialog should be shown.</p>
 */
public class SingleDropSetUpNagLogic {
    /**
     * Determines whether any of the given active contracts has an associated
     * StratCon campaign state.
     *
     * @param activeContracts the list of currently active AtB contracts to inspect;
     *                        may be empty but not {@code null}
     * @return {@code true} if at least one contract has a non-{@code null}
     *         StratCon campaign state; otherwise {@code false}
     */
    public static boolean hasActiveStratConContract(List<AtBContract> activeContracts) {
        for (AtBContract atBContract : activeContracts) {
            if (atBContract.getStratconCampaignState() != null) {
                return true;
            }
        }

        return false;
    }
}
