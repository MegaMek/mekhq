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

import static java.lang.Math.ceil;
import static java.lang.Math.max;

import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.utilities.MHQMorale;

public class ChaosContractDeterminationRequiredVictoryPoints {
    // the share of scenarios that are Turning Points, whatever the contract's command rights
    private static final double TURNING_POINT_CHANCE = 0.33;

    /**
     * Calculates the number of required Victory Points (VP) needed to achieve overall success for this StratCon
     * contract.
     *
     * <p>The final result estimates the expected number of Turning Points the player must win for overall contract
     * success. If the player loses a handful of Turning Points, they should still be able to win the contract by being
     * proactive in the Area of Operations.</p>
     *
     * @return the required number of Victory Points, rounded up to the nearest integer
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static int getRequiredVictoryPoints(AbstractContract contract) {
        if (contract.getStratConCampaignState() == null) {
            return 0;
        }

        double baseRequirement = contract.getScale();

        double duration = getActiveMonths(contract);

        double trackCount = 0;
        int totalScenarioOdds = 0;
        for (StratConTrackState trackState : contract.getStratConCampaignState().getTracks()) {
            trackCount++;
            totalScenarioOdds += trackState.getScenarioOdds();
        }

        double meanScenarioOdds = totalScenarioOdds / trackCount;
        double scenarioOdds = meanScenarioOdds / 100.0;

        // This result gives us the average number of Turning Points expected for the contract
        return (int) ceil(baseRequirement * duration * scenarioOdds * TURNING_POINT_CHANCE);
    }

    /**
     * The number of months the enemy is expected to be active over the contract, and so able to offer Turning Points.
     *
     * <p>Most contracts are active throughout. A garrison-type contract follows the raid model instead
     * (see {@link mekhq.campaign.mission.contract.contractData.ContractObjectiveType#isGarrisonType()}): it begins
     * peaceful, and each month after the first the enemy raids with a 1 in {@link MHQMorale#RAID_DIE_SIZE} chance (see
     * {@link MHQMorale#checkMorale}). No scenarios spawn between raids, so only the expected raid months count, though
     * never less than one.</p>
     *
     * @param contract the contract
     *
     * @return the expected number of active months
     *
     * @author Illiani
     * @since 0.51.01
     */
    static double getActiveMonths(AbstractContract contract) {
        int lengthInMonths = contract.getLengthInMonths();
        if (!contract.getObjectiveType().isGarrisonType()) {
            return lengthInMonths;
        }

        return max(1.0, (lengthInMonths - 1) / (double) MHQMorale.RAID_DIE_SIZE);
    }
}
