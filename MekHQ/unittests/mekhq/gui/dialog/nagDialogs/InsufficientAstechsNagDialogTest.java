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

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class contains test cases for the {@link InsufficientAstechsNagDialog} class.
 * It tests the different combinations of Astech requirements and verifies the behavior of the local
 * {@code checkNag()} method.
 */
class InsufficientAstechsNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private InsufficientAstechsNagDialog dialog;

    /**
     * Sets up the necessary dependencies and configurations before running the test methods.
     * Runs once before all tests
     */
    @BeforeAll
    static void setup() {
        System.setProperty("java.awt.headless", "true");

        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception exception) {
            MMLogger.create(InsufficientAstechsNagDialogTest.class).error("", exception);
        }
    }

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        campaign = mock(Campaign.class);
        dialog = new InsufficientAstechsNagDialog(null, campaign);
    }

    // In the following tests,
    // Different combinations of unit states to set up desired behaviors in mock objects
    // Then the checkNag() method of InsufficientAstechsNagDialog class is called,
    // and its response is checked against expected behavior

    @Test
    void noAstechsNeeded() {
        when(campaign.getAstechNeed()).thenReturn(0);
        assertFalse(dialog.checkNag());
    }

    @Test
    void oneAstechNeeded() {
        when(campaign.getAstechNeed()).thenReturn(1);
        assertTrue(dialog.checkNag());
    }

    @Test
    void negativeAstechsNeeded() {
        when(campaign.getAstechNeed()).thenReturn(-1);
        assertFalse(dialog.checkNag());
    }
}