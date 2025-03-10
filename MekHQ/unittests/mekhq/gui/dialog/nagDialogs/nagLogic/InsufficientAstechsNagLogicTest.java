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
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.gui.dialog.nagDialogs.InsufficientAstechsNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientAstechsNagLogic.hasAsTechsNeeded;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class contains test cases for the {@link InsufficientAstechsNagDialog} class.
 * It tests the different combinations of Astech requirements and verifies the behavior of the
 * {@code checkAstechsNeededCount()} method.
 */
class InsufficientAstechsNagLogicTest {
    private Campaign campaign;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        campaign = mock(Campaign.class);
    }

    @Test
    void noAsTechsNeeded() {
        when(campaign.getAstechNeed()).thenReturn(0);
        assertFalse(hasAsTechsNeeded(campaign));
    }

    @Test
    void oneAsTechNeeded() {
        when(campaign.getAstechNeed()).thenReturn(1);
        assertTrue(hasAsTechsNeeded(campaign)); // Updated assertion
    }

    @Test
    void negativeAsTechsNeeded() {
        when(campaign.getAstechNeed()).thenReturn(-1);
        assertFalse(hasAsTechsNeeded(campaign));
    }
}
