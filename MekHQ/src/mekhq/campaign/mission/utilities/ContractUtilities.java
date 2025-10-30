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

package mekhq.campaign.mission.utilities;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static mekhq.campaign.force.ForceType.STANDARD;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.enums.CombatRole;

public class ContractUtilities {
    /**
     * Calculates the number of lances used for this contract, based on [campaign].
     *
     * @param campaign    The campaign to reference.
     * @param isCadreDuty {@code true} if {@link CombatRole#CADRE} should be considered a combat role
     *
     * @return The number of lances required.
     */
    public static int calculateBaseNumberOfRequiredLances(Campaign campaign, boolean isCadreDuty) {

        int combatForceCount = 0;
        for (CombatTeam combatTeam : campaign.getCombatTeamsAsList()) {
            if (0 >= combatTeam.getSize(campaign)) { // Don't count empty combat teams (or warship-only)
                continue;
            }

            Force force = combatTeam.getForce(campaign);
            if (force == null) {
                continue;
            }

            CombatRole roleInMemory = force.getCombatRoleInMemory();
            boolean hasCombatRole = roleInMemory.isCombatRole() || (isCadreDuty && roleInMemory.isCadre());
            if (force.isForceType(STANDARD) && hasCombatRole) {
                combatForceCount++;
            }
        }

        return max(combatForceCount, 1);
    }

    /**
     * Calculates the number of units required for this contract, based on [campaign].
     *
     * @param campaign The campaign to reference.
     *
     * @return The number of combat units present.
     */
    public static int calculateBaseNumberOfUnitsRequiredInCombatTeams(Campaign campaign) {
        return max(getEffectiveNumUnits(campaign), 1);
    }

    /**
     * Calculates the effective number of units available in the given campaign based on unit types and roles.
     *
     * <p>
     * This method iterates through all combat teams in the specified campaign, ignoring combat teams with the auxiliary
     * role. For each valid combat team, it retrieves the associated force and evaluates all units within that force.
     * The unit contribution to the total is determined based on its type. See {@link CombatTeam#getSize(Campaign)}
     *
     * <p>
     * Units that aren’t associated with a valid combat team or can’t be fetched due to missing data are ignored. The
     * final result is returned as an integer by flooring the calculated total.
     * </p>
     *
     * @param campaign the campaign containing the combat teams and units to evaluate
     *
     * @return the effective number of units as an integer
     */
    public static int getEffectiveNumUnits(Campaign campaign) {
        double numUnits = 0;
        for (CombatTeam combatTeam : campaign.getCombatTeamsAsList()) {
            Force force = combatTeam.getForce(campaign);

            if (force == null) {
                continue;
            }

            if (!force.isForceType(STANDARD)) {
                continue;
            }

            numUnits += combatTeam.getSize(campaign);
        }

        return (int) floor(numUnits);
    }
}
