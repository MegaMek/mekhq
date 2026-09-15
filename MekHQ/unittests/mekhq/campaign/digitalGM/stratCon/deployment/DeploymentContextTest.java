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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DeploymentContext}: staging state and the live budget summaries it derives from
 * {@link DeploymentEvaluator}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class DeploymentContextTest {
    @Test
    void reinforcementCostReflectsStagedForcesAndSpend() {
        DeploymentContext context = new DeploymentContext(DeploymentMode.REINFORCE);
        context.setAvailableSupportPoints(7);
        context.setChosenSupportPoints(2);
        context.setInstantArrival(false);
        context.stageFormation(11);
        context.stageFormation(22);

        ReinforcementCost cost = context.getReinforcementCost();
        assertEquals(6, cost.totalSupportPoints());
        assertEquals(-2, cost.targetNumberModifier());
        assertEquals(1, context.getSupportPointsRemainingAfterCommit());
    }

    @Test
    void unstagingFormationReducesTheCount() {
        DeploymentContext context = new DeploymentContext(DeploymentMode.REINFORCE);
        context.stageFormation(11);
        context.stageFormation(22);
        context.unstageFormation(11);

        assertEquals(1, context.getStagedFormationCount());
        assertFalse(context.isFormationStaged(11));
        assertTrue(context.isFormationStaged(22));
    }

    @Test
    void utilityUnitsTradeOffAgainstMinefields() {
        DeploymentContext context = new DeploymentContext(DeploymentMode.UTILITY);
        context.setDefensivePoints(4);
        context.stageUnit(mock(Unit.class));
        context.stageUnit(mock(Unit.class));

        assertEquals(2, context.getMinefieldsRemaining());
    }

    @Test
    void leadershipBudgetRemainingUsesSkillAndPriorUsage() {
        DeploymentContext context = new DeploymentContext(DeploymentMode.AUXILIARIES);
        context.setLeadershipSkill(3);
        context.setLeadershipPointsUsed(400);

        assertEquals(1100, context.getLeadershipBattleValueRemaining());
        assertFalse(context.isOverLeadershipBudget());
    }

    @Test
    void stagedSelectionsSurviveModeSwitch() {
        DeploymentContext context = new DeploymentContext(DeploymentMode.PRIMARY);
        context.stageFormation(11);
        context.setMode(DeploymentMode.REINFORCE);

        assertTrue(context.isFormationStaged(11));
        assertEquals(DeploymentMode.REINFORCE, context.getMode());
    }
}
