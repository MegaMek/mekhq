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

import java.time.DayOfWeek;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;

public class DeploymentShortfallNagLogic {
    /**
     * Checks if the campaign's active contracts have deployment deficits that need to be addressed.
     *
     * <p>
     * The following conditions are evaluated to determine whether the requirements for short deployments are met:
     * <ul>
     *     <li>The campaign must currently be located on a planet. If it is not, the dialog is skipped.</li>
     *     <li>The check is performed weekly, only on Sundays, to avoid spamming the user daily.</li>
     *     <li>If any active AtB contract has a deployment deficit, the method returns {@code true}.</li>
     * </ul>
     * If none of these conditions are met, the method returns {@code false}.
     *
     * @return {@code true} if there are unmet deployment requirements; otherwise, {@code false}.
     */
    public static boolean hasDeploymentShortfall(Campaign campaign) {
        if (!campaign.getLocation().isOnPlanet()) {
            return false;
        }

        // this prevents the nag from spamming daily
        if (campaign.getLocalDate().getDayOfWeek() != DayOfWeek.SUNDAY) {
            return false;
        }

        // There is no need to use a stream here, as the number of iterations doesn't warrant it.
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            if (campaign.getDeploymentDeficit(contract) > 0) {
                return true;
            }
        }

        return false;
    }
}
