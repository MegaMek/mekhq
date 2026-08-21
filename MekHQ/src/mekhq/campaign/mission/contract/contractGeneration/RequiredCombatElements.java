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
package mekhq.campaign.mission.contract.contractGeneration;

import static java.lang.Math.max;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.utilities.ContractUtilities;

public class RequiredCombatElements {


    /**
     * Calculates the required number of combat elements for a contract based on campaign options, contract details, and
     * variance factors.
     *
     * <p>
     * This method determines the number of combat elements needed to deploy, taking into account factors such as:
     * <ul>
     *   <li>Whether the contract is a subcontract (returns 1 as a base case).</li>
     *   <li>The effective unit forces.</li>
     *   <li>Whether variance bypass is enabled, applying a flat reduction to available forces.</li>
     *   <li>Variance adjustments applied through a die roll, affecting the availability of forces.</li>
     * </ul>
     * The method ensures values are clamped to maintain a minimum deployment of at least 1 combat
     * element while not exceeding the maximum deployable combat elements.
     *
     * @param campaign       the campaign containing relevant options and faction information
     * @param bypassVariance a flag indicating whether variance adjustments should be bypassed
     * @param varianceFactor the degree of variance to apply to required combat elements
     *
     * @return the calculated number of required units in combat teams, ensuring it meets game rules and constraints
     */
    public static int calculateRequiredCombatElements(Campaign campaign, boolean bypassVariance,
          double varianceFactor) {
        // Calculate base formation size and effective unit force
        int effectiveForces = ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(campaign);

        // If bypassing variance, apply flat reduction (reduce force by 1/3)
        if (bypassVariance) {
            return max(effectiveForces - calculateBypassVarianceReduction(effectiveForces), 1);
        }

        // Adjust available forces based on variance, ensuring minimum clamping
        int adjustedForces = (int) Math.floor((double) effectiveForces * varianceFactor);

        if (adjustedForces < 1) {
            adjustedForces = 1;
        }

        // Return the clamped value, ensuring it does not exceed max-deployable forces
        return Math.min(adjustedForces, effectiveForces);
    }

    /**
     * Calculates the bypass variance reduction based on the available forces.
     *
     * <p>
     * The reduction is calculated by dividing the available forces by a fixed factor of 3 and rounding down to the
     * nearest whole number. This value is used in scenarios where variance adjustments are bypassed.
     * </p>
     *
     * @param availableForces the total number of forces available
     *
     * @return the bypass variance reduction as an integer
     */
    private static int calculateBypassVarianceReduction(int availableForces) {
        return (int) Math.floor((double) availableForces / 3);
    }

}
