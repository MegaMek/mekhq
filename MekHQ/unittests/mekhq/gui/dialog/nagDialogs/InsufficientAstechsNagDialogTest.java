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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class contains test cases for the {@link InsufficientAstechsNagDialog} class.
 * It tests the different combinations of Astech requirements and verifies the behavior of the
 * {@code checkAstechsNeededCount()} method.
 */
class InsufficientAstechsNagDialogTest {
    private Campaign campaign;
    private InsufficientAstechsNagDialog insufficientAstechsNagDialog;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        campaign = mock(Campaign.class);
        insufficientAstechsNagDialog = new InsufficientAstechsNagDialog(campaign);
    }

    @Test
    void noAsTechsNeeded() {
        when(campaign.getAstechNeed()).thenReturn(0);
        insufficientAstechsNagDialog.checkAsTechsNeededCount();
        assertFalse(insufficientAstechsNagDialog.hasAsTechsNeeded());
    }

    @Test
    void oneAsTechNeeded() {
        when(campaign.getAstechNeed()).thenReturn(1);
        insufficientAstechsNagDialog.checkAsTechsNeededCount();
        assertTrue(insufficientAstechsNagDialog.hasAsTechsNeeded());
    }

    @Test
    void negativeAsTechsNeeded() {
        when(campaign.getAstechNeed()).thenReturn(-1);
        insufficientAstechsNagDialog.checkAsTechsNeededCount();
        assertFalse(insufficientAstechsNagDialog.hasAsTechsNeeded());
    }
}
