/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import static mekhq.gui.dialog.nagDialogs.nagLogic.EndContractNagLogic.isContractEnded;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.gui.dialog.nagDialogs.EndContractNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class is a test class for the {@link EndContractNagDialog} class. It contains test methods for various scenarios
 * related to contract expiration.
 */
public class EndContractNagLogicTest {
    // Mock objects for the tests
    private Campaign campaign;
    private LocalDate today;
    private AtBContract contract1, contract2;

    /**
     * Test setup for each test, runs before each test. Initializes the mock objects and sets up the necessary mock
     * behaviors.
     */
    @BeforeEach
    void init() {
        System.setProperty("java.awt.headless", "true");

        // Initialize the mock objects
        campaign = mock(Campaign.class);
        today = LocalDate.now();
        contract1 = mock(AtBContract.class);
        contract2 = mock(AtBContract.class);
    }

    // In the following tests the isContractEnded() method is called, and its response is
    // checked against expected behavior

    @Test
    void noActiveContracts() {
        assertFalse(isContractEnded(today, new ArrayList<>()));
    }

    @Test
    void oneActiveContractEndsTomorrow() {
        when(contract1.getEndingDate()).thenReturn(today.plusDays(1));

        assertFalse(isContractEnded(today, List.of(contract1)));
    }

    @Test
    void oneActiveContractEndsToday() {
        when(contract1.getEndingDate()).thenReturn(today);

        assertTrue(isContractEnded(today, List.of(contract1)));
    }

    @Test
    void twoActiveContractsOneEndsTomorrowOneEndsToday() {
        when(contract1.getEndingDate()).thenReturn(today.plusDays(1));
        when(contract2.getEndingDate()).thenReturn(today);

        assertTrue(isContractEnded(today, List.of(contract1, contract2)));
    }

    @Test
    void twoActiveContractsBothEndToday() {
        when(contract1.getEndingDate()).thenReturn(today);
        when(contract2.getEndingDate()).thenReturn(today);

        assertTrue(isContractEnded(today, List.of(contract1, contract2)));
    }
}
