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
package mekhq.campaign.mission.newContract;

import java.time.LocalDate;

import megamek.logging.MMLogger;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;

public class LandlessEmployerExclusion {
    private static final MMLogger LOGGER = MMLogger.create(LandlessEmployerExclusion.class);

    /**
     * A contract where the player defends is a defense of the employer's territory, so an employer with no planets
     * anywhere - neither its own nor a contained-faction host's worlds to stand on - has nothing to defend, and the
     * contract fails outright. Contracts where the player attacks stay valid for a landless employer.
     *
     * @return {@code true} if the contract should be rejected, {@code false} otherwise
     */
    public static boolean shouldRejectDefensiveObjectives(Faction employerFaction,
          boolean isPlayerAttacker, LocalDate currentDate) {
        boolean isLandless = employerFaction != null &&
                                   !RandomFactionGenerator.getInstance().hasAnyTerritory(employerFaction, currentDate);

        if (!isPlayerAttacker && isLandless) {
            String employerCode = employerFaction.getShortName();
            LOGGER.info("Defensive contract for landless employer {}; nothing to defend", employerCode);
            return true;
        }

        return false;
    }
}
