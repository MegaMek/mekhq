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
package mekhq.campaign.mission;

import java.util.List;

/**
 * Utility class for calculating contract performance scores based on scenario outcomes.
 *
 * <p>This class provides scoring logic for military contracts by evaluating the results of individual scenarios and
 * aggregating them into an overall contract score. Positive scores indicate overall success, while negative scores
 * indicate failure.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class ContractScore {
    private static final int DECISIVE_VICTORY = 3;
    private static final int VICTORY = 2;
    private static final int MARGINAL_VICTORY = 1;
    private static final int PYRRHIC_VICTORY = 1;
    private static final int DRAW = 0;
    private static final int MARGINAL_DEFEAT = -1;
    private static final int DEFEAT = -2;
    private static final int DECISIVE_DEFEAT = -3;
    private static final int FLEET_IN_BEING = 2;
    private static final int REFUSED_ENGAGEMENT = 2;

    /**
     * Calculates the overall contract score based on the outcomes of all completed scenarios.
     *
     * <p>This method iterates through all provided scenarios and aggregates their individual scores based on their
     * completion status. Current (ongoing) scenarios are excluded from the calculation. Each scenario outcome
     * contributes to the total score.</p>
     *
     * @param scenarios the list of scenarios to evaluate for contract scoring
     *
     * @return the aggregate contract score, where positive values indicate overall success, negative values indicate
     *       overall failure, and zero indicates a balanced outcome
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static int getContractScore(List<Scenario> scenarios) {
        int contractScore = 0;
        for (Scenario scenario : scenarios) {
            if (scenario.getStatus().isCurrent()) {
                continue;
            }

            switch (scenario.getStatus()) {
                case DECISIVE_VICTORY -> contractScore += DECISIVE_VICTORY;
                case VICTORY -> contractScore += VICTORY;
                case MARGINAL_VICTORY -> contractScore += MARGINAL_VICTORY;
                case PYRRHIC_VICTORY -> contractScore += PYRRHIC_VICTORY;
                case DRAW -> contractScore += DRAW;
                case MARGINAL_DEFEAT -> contractScore += MARGINAL_DEFEAT;
                case DEFEAT -> contractScore += DEFEAT;
                case DECISIVE_DEFEAT -> contractScore += DECISIVE_DEFEAT;
                case FLEET_IN_BEING -> contractScore += FLEET_IN_BEING;
                case REFUSED_ENGAGEMENT -> contractScore += REFUSED_ENGAGEMENT;
            }
        }

        return contractScore;
    }
}
