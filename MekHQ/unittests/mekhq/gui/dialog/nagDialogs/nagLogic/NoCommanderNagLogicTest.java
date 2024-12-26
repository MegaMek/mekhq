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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.dialog.nagDialogs.NoCommanderNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mekhq.gui.dialog.nagDialogs.nagLogic.NoCommanderNagLogic.hasNoCommander;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link NoCommanderNagDialog} class.
 * It contains test methods for various scenarios related to commander assignment.
 */
class NoCommanderNagLogicTest {
    // Mock objects for the tests
    private Campaign campaign;
    private Person commander;
    private Person commanderNull;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        commander = mock(Person.class);
        commanderNull = null;
    }

    // In the following tests the isCommanderMissing() method is called, and its response is checked
    // against expected behavior

    @Test
    void commanderPresent() {
        when(campaign.getFlaggedCommander()).thenReturn(commander);
        assertFalse(hasNoCommander(campaign));
    }

    @Test
    void commanderMissing() {
        when(campaign.getFlaggedCommander()).thenReturn(commanderNull);
        assertTrue(hasNoCommander(campaign));
    }
}
