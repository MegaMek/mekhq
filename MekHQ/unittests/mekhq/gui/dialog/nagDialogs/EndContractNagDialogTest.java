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
import mekhq.campaign.mission.Contract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link EndContractNagDialog} class.
 * It contains test methods for various scenarios related to contract expiration.
 */
public class EndContractNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private LocalDate today;
    private Contract contract1, contract2;

    private EndContractNagDialog endContractNagDialog;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        System.setProperty("java.awt.headless", "true");

        // Initialize the mock objects
        campaign = mock(Campaign.class);
        today = LocalDate.now();
        contract1 = mock(Contract.class);
        contract2 = mock(Contract.class);

        endContractNagDialog = new EndContractNagDialog(campaign);

        when(campaign.getLocalDate()).thenReturn(today);
    }

    // In the following tests the isContractEnded() method is called, and its response is
    // checked against expected behavior

    @Test
    void noActiveContracts() {
        when(campaign.getActiveContracts()).thenReturn(new ArrayList<>());
        assertFalse(endContractNagDialog.isContractEnded());
    }

    @Test
    void oneActiveContractEndsTomorrow() {
        when(campaign.getActiveContracts()).thenReturn(List.of(contract1));
        when(contract1.getEndingDate()).thenReturn(today.plusDays(1));
        assertFalse(endContractNagDialog.isContractEnded());
    }

    @Test
    void oneActiveContractEndsToday() {
        when(campaign.getActiveContracts()).thenReturn(List.of(contract1));
        when(contract1.getEndingDate()).thenReturn(today);
        assertTrue(endContractNagDialog.isContractEnded());
    }

    @Test
    void twoActiveContractsOneEndsTomorrowOneEndsToday() {
        when(campaign.getActiveContracts()).thenReturn(List.of(contract1, contract2));
        when(contract1.getEndingDate()).thenReturn(today.plusDays(1));
        when(contract2.getEndingDate()).thenReturn(today);
        assertTrue(endContractNagDialog.isContractEnded());
    }

    @Test
    void twoActiveContractsBothEndToday() {
        when(campaign.getActiveContracts()).thenReturn(List.of(contract1, contract2));
        when(contract1.getEndingDate()).thenReturn(today);
        assertTrue(endContractNagDialog.isContractEnded());
    }
}
