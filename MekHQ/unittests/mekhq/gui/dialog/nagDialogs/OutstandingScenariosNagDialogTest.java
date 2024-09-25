/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog.nagDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static mekhq.gui.dialog.nagDialogs.OutstandingScenariosNagDialog.checkForOutstandingScenarios;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link OutstandingScenariosNagDialog} class.
 * It contains tests for various scenarios related to the {@code checkForOutstandingScenarios} method
 */
class OutstandingScenariosNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private AtBContract contract;
    private AtBScenario scenario1, scenario2;
    private LocalDate today;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        contract = mock(AtBContract.class);
        scenario1 = mock(AtBScenario.class);
        scenario2 = mock(AtBScenario.class);
        today = LocalDate.now();

        // When the Campaign mock calls 'getLocalDate()' return today's date
        when(campaign.getLocalDate()).thenReturn(today);
    }

    /**
     * Initializes an {@link AtBContract} containing two instances of {@link AtBScenario}.
     */
    private void initializeContractWithTwoScenarios() {
        when(campaign.getActiveAtBContracts(true)).thenReturn(List.of(contract));
        when(contract.getCurrentAtBScenarios()).thenReturn(List.of(scenario1, scenario2));
    }

    // In the following tests the checkForOutstandingScenarios() method is called, and its response
    // is checked against expected behavior

    @Test
    void noContracts() {
        when(campaign.getActiveAtBContracts(true)).thenReturn(new ArrayList<>());

        assertFalse(checkForOutstandingScenarios(campaign));
    }

    @Test
    void noScenarios() {
        when(campaign.getActiveAtBContracts(true)).thenReturn(List.of(contract));
        when(contract.getCurrentAtBScenarios()).thenReturn(new ArrayList<>());

        assertFalse(checkForOutstandingScenarios(campaign));
    }

    @Test
    void noOutstandingScenarios() {
        initializeContractWithTwoScenarios();

        when(scenario1.getDate()).thenReturn(today.plusDays(1));
        when(scenario2.getDate()).thenReturn(today.plusDays(1));

        assertFalse(checkForOutstandingScenarios(campaign));
    }

    @Test
    void oneOutstandingScenarioFirst() {
        initializeContractWithTwoScenarios();

        when(scenario1.getDate()).thenReturn(today);
        when(scenario2.getDate()).thenReturn(today.plusDays(1));

        assertTrue(checkForOutstandingScenarios(campaign));
    }

    @Test
    void oneOutstandingScenarioSecond() {
        initializeContractWithTwoScenarios();

        when(scenario1.getDate()).thenReturn(today.plusDays(1));
        when(scenario2.getDate()).thenReturn(today);

        assertTrue(checkForOutstandingScenarios(campaign));
    }

    @Test
    void twoOutstandingScenarios() {
        initializeContractWithTwoScenarios();

        when(scenario1.getDate()).thenReturn(today);
        when(scenario2.getDate()).thenReturn(today);

        assertTrue(checkForOutstandingScenarios(campaign));
    }
}