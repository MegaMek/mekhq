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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConDeploymentOrderBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinition;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinitions;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConScheduledPointOfInterest;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractCommandRights;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for Deployment Orders: the command step a contract's command rights and employer give it, how many orders
 * that brings each month (the command step per point of Scale), and scheduling the orders over a contract's run.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConDeploymentOrdersTest {
    private static final LocalDate START_DATE = LocalDate.of(3025, 1, 1);
    private static final int LENGTH_IN_MONTHS = 4;
    private static final int SCALE = 3;

    @BeforeAll
    static void registerDefinition() {
        // Registered rather than read from the data files, so the tests do not depend on them.
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(StratConDeploymentOrderBehavior.TYPE_ID);
        definition.setBehaviorId(StratConDeploymentOrderBehavior.BEHAVIOR_ID);
        StratConPointOfInterestDefinitions.registerDefinition(definition);
    }

    @AfterAll
    static void unregisterDefinition() {
        StratConPointOfInterestDefinitions.unregisterDefinition(StratConDeploymentOrderBehavior.TYPE_ID);
    }

    private static Faction faction(boolean isControlling, boolean isLenient) {
        Faction faction = mock(Faction.class);
        when(faction.isControlling()).thenReturn(isControlling);
        when(faction.isLenient()).thenReturn(isLenient);
        return faction;
    }

    private static AbstractContract contract(ContractCommandRights commandRights, Faction employer) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getCommandRights()).thenReturn(commandRights);
        when(contract.getEmployerFaction()).thenReturn(employer);
        when(contract.getStartDate()).thenReturn(START_DATE);
        when(contract.getEndingDate()).thenReturn(START_DATE.plusMonths(LENGTH_IN_MONTHS));
        when(contract.getLengthInMonths()).thenReturn(LENGTH_IN_MONTHS);
        when(contract.getScale()).thenReturn(SCALE);
        when(contract.getExactScale()).thenReturn((double) SCALE);
        return contract;
    }

    private static List<StratConScheduledPointOfInterest> schedule(AbstractContract contract, LocalDate fromDate) {
        StratConCampaignState campaignState = new StratConCampaignState();
        StratConDeploymentOrders.scheduleDeploymentOrders(contract, campaignState, fromDate);
        return campaignState.getScheduledPointsOfInterest();
    }

    @ParameterizedTest
    @CsvSource({
          // command rights, controlling, lenient, expected step
          "INDEPENDENT, false, false, 0",
          "INDEPENDENT, true, false, 0",
          "LIAISON, false, false, 1",
          "LIAISON, true, false, 2",
          "LIAISON, false, true, 0",
          "HOUSE, false, false, 2",
          "HOUSE, true, false, 3",
          "HOUSE, false, true, 1",
          "INTEGRATED, false, false, 3",
          "INTEGRATED, true, false, 4",
          "INTEGRATED, false, true, 2",
          "INTEGRATED, true, true, 3"
    })
    void theCommandStepFollowsCommandRightsAndTheEmployer(ContractCommandRights commandRights, boolean isControlling,
          boolean isLenient, int expectedStep) {
        assertEquals(expectedStep,
              StratConDeploymentOrders.getCommandStep(commandRights, faction(isControlling, isLenient)));
    }

    @Test
    void aMissingEmployerLeavesTheCommandRightsStep() {
        assertEquals(2, StratConDeploymentOrders.getCommandStep(ContractCommandRights.HOUSE, null));
    }

    @Test
    void missingCommandRightsGiveNoOrders() {
        assertEquals(0, StratConDeploymentOrders.getCommandStep(null, faction(true, false)));
    }

    @ParameterizedTest
    @CsvSource({
          // command step, exact scale, expected orders a month
          "0, 3.0, 0",
          "1, 3.0, 3",
          "2, 3.0, 6",
          "3, 3.0, 9",
          "4, 3.0, 12",
          "1, 1.4, 1",
          "3, 1.4, 4",
          "2, 1.25, 3",
          "1, 0.0, 1"
    })
    void eachMonthBringsTheCommandStepPerPointOfScaleRoundedNormally(int commandStep, double exactScale,
          int expectedOrders) {
        assertEquals(expectedOrders, StratConDeploymentOrders.getMonthlyOrderCount(commandStep, exactScale));
    }

    @Test
    void anIndependentContractReceivesNoOrders() {
        AbstractContract contract = contract(ContractCommandRights.INDEPENDENT, faction(true, false));

        assertTrue(schedule(contract, START_DATE).isEmpty());
    }

    @Test
    void ordersAreStrategicObjectivesWithinTheContract() {
        AbstractContract contract = contract(ContractCommandRights.INTEGRATED, faction(false, false));

        List<StratConScheduledPointOfInterest> scheduled = schedule(contract, START_DATE);

        assertEquals(LENGTH_IN_MONTHS * 3 * SCALE, scheduled.size(), "three orders a month per point of Scale");
        LocalDate endDate = START_DATE.plusMonths(LENGTH_IN_MONTHS);
        for (StratConScheduledPointOfInterest scheduledOrder : scheduled) {
            assertEquals(StratConDeploymentOrderBehavior.TYPE_ID, scheduledOrder.getTypeId());
            assertTrue(scheduledOrder.isStrategicObjective());
            assertFalse(scheduledOrder.getSpawnDate().isBefore(START_DATE));
            assertTrue(scheduledOrder.getSpawnDate().isBefore(endDate));
        }
    }

    @Test
    void aControllingEmployerAddsAnOrderPerPointOfScale() {
        AbstractContract contract = contract(ContractCommandRights.LIAISON, faction(true, false));

        assertEquals(LENGTH_IN_MONTHS * 2 * SCALE, schedule(contract, START_DATE).size());
    }

    @Test
    void aLenientEmployerUnderLiaisonCommandRightsIssuesNoOrders() {
        AbstractContract contract = contract(ContractCommandRights.LIAISON, faction(false, true));

        assertTrue(schedule(contract, START_DATE).isEmpty());
    }

    @Test
    void aScheduleRolledAgainKeepsOnlyDaysFromItsStart() {
        AbstractContract contract = contract(ContractCommandRights.INTEGRATED, faction(false, false));
        LocalDate fromDate = START_DATE.plusMonths(2);

        List<StratConScheduledPointOfInterest> scheduled = schedule(contract, fromDate);

        assertEquals(2 * 3 * SCALE, scheduled.size(), "only the last two months are still to come");
        for (StratConScheduledPointOfInterest scheduledOrder : scheduled) {
            assertFalse(scheduledOrder.getSpawnDate().isBefore(fromDate));
        }
    }

    @Test
    void outstandingOrdersDoNotHoldTheContractOpen() {
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addScheduledPointOfInterest(new StratConScheduledPointOfInterest(START_DATE,
              StratConDeploymentOrderBehavior.TYPE_ID,
              true));

        assertFalse(campaignState.hasScheduledStrategicObjectivePointsOfInterest());
    }
}
