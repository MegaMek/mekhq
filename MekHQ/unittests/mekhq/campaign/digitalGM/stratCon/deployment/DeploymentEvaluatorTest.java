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
package mekhq.campaign.digitalGM.stratCon.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DeploymentEvaluator}, the pure deployment arithmetic behind the StratCon deployment wizard.
 *
 * @author Illiani
 * @since 0.51.01
 */
class DeploymentEvaluatorTest {
    private static final double PROBABILITY_TOLERANCE = 1.0e-9;

    @Test
    void naturalTwoStillFailsWhenTargetAtOrBelowTwo() {
        assertEquals(35.0 / 36.0, DeploymentEvaluator.successProbabilityTwoD6(2), PROBABILITY_TOLERANCE);
        assertEquals(35.0 / 36.0, DeploymentEvaluator.successProbabilityTwoD6(-3), PROBABILITY_TOLERANCE);
    }

    @Test
    void successProbabilityImpossibleWhenTargetAboveTwelve() {
        assertEquals(0.0, DeploymentEvaluator.successProbabilityTwoD6(13), PROBABILITY_TOLERANCE);
    }

    @Test
    void successProbabilityMatchesTwoD6Distribution() {
        assertEquals(26.0 / 36.0, DeploymentEvaluator.successProbabilityTwoD6(6), PROBABILITY_TOLERANCE);
        assertEquals(21.0 / 36.0, DeploymentEvaluator.successProbabilityTwoD6(7), PROBABILITY_TOLERANCE);
        assertEquals(1.0 / 36.0, DeploymentEvaluator.successProbabilityTwoD6(12), PROBABILITY_TOLERANCE);
    }

    @Test
    void leadershipBudgetScalesWithSkillAndCaps() {
        assertEquals(0, DeploymentEvaluator.leadershipBudget(0));
        assertEquals(1500, DeploymentEvaluator.leadershipBudget(3));
        assertEquals(2500, DeploymentEvaluator.leadershipBudget(5));
        assertEquals(2500, DeploymentEvaluator.leadershipBudget(9));
    }

    @Test
    void leadershipBudgetNeverNegative() {
        assertEquals(0, DeploymentEvaluator.leadershipBudget(-2));
    }

    @Test
    void leadershipPointsRemainingSubtractsUsageAndFloorsAtZero() {
        assertEquals(1100, DeploymentEvaluator.leadershipPointsRemaining(3, 400));
        assertEquals(0, DeploymentEvaluator.leadershipPointsRemaining(3, 2000));
    }

    @Test
    void minefieldsRemainingTradesOffAgainstStagedUnits() {
        assertEquals(2, DeploymentEvaluator.minefieldsRemaining(4, 2));
        assertEquals(0, DeploymentEvaluator.minefieldsRemaining(4, 6));
    }

    @Test
    void reinforcementCostNormalArrival() {
        ReinforcementCost cost = DeploymentEvaluator.reinforcementCost(2, false, 2);
        assertEquals(3, cost.perForceSupportPoints());
        assertEquals(6, cost.totalSupportPoints());
        assertEquals(-2, cost.targetNumberModifier());
    }

    @Test
    void reinforcementCostInstantArrivalDoublesBaseCost() {
        ReinforcementCost cost = DeploymentEvaluator.reinforcementCost(0, true, 1);
        assertEquals(2, cost.perForceSupportPoints());
        assertEquals(2, cost.totalSupportPoints());
        assertEquals(0, cost.targetNumberModifier());
    }

    @Test
    void reinforcementCostWithNoForcesIsFree() {
        ReinforcementCost cost = DeploymentEvaluator.reinforcementCost(3, false, 0);
        assertEquals(0, cost.totalSupportPoints());
        assertEquals(0, cost.targetNumberModifier());
    }

    @Test
    void reinforcementRollAppliesModifierAndComputesOdds() {
        ReinforcementRoll roll = DeploymentEvaluator.reinforcementRoll(10, -4, false);
        assertEquals(6, roll.finalTargetNumber());
        assertEquals(26.0 / 36.0, roll.successProbability(), PROBABILITY_TOLERANCE);
    }

    @Test
    void reinforcementRollWithoutModifierUsesBaseTarget() {
        ReinforcementRoll roll = DeploymentEvaluator.reinforcementRoll(8, 0, false);
        assertEquals(8, roll.finalTargetNumber());
        assertEquals(15.0 / 36.0, roll.successProbability(), PROBABILITY_TOLERANCE);
    }
}
