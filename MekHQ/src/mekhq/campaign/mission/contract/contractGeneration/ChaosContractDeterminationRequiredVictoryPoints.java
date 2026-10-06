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
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.INDEPENDENT_COMMAND_RIGHTS_REQUIRED_VICTORY_POINTS;

import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractCommandRights;

public class ChaosContractDeterminationRequiredVictoryPoints {
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

        if (contract.getCommandRights().isIndependent()) {
            return INDEPENDENT_COMMAND_RIGHTS_REQUIRED_VICTORY_POINTS;
        }

        double baseRequirement = contract.getScale();

        int duration = contract.getLengthInMonths();
        if (contract.getObjectiveType().isGarrisonType()) {
            duration = (int) ceil(duration * 0.75); // We assume around 25% of the contract will be peaceful
        }

        double trackCount = 0;
        int totalScenarioOdds = 0;
        for (StratConTrackState trackState : contract.getStratConCampaignState().getTracks()) {
            trackCount++;
            totalScenarioOdds += trackState.getScenarioOdds();
        }

        double meanScenarioOdds = totalScenarioOdds / trackCount;
        double scenarioOdds = meanScenarioOdds / 100.0;
        double turningPointChance = (contract.getCommandRights() == ContractCommandRights.INTEGRATED ? 1.0 : 0.33);

        // This result gives us the average number of Turning Points expected for the contract
        return (int) ceil(baseRequirement * duration * scenarioOdds * turningPointChance);
    }
}
