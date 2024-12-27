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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientMedicsNagLogic.hasMedicsNeeded;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class contains test cases for the {@link InsufficientMedicsNagLogicTest} class.
 * It tests the different combinations of Medic requirements and verifies the behavior of the
 * {@code checkMedicsNeededCount()} method.
 */
class InsufficientMedicsNagLogicTest {
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
    void noMedicsNeeded() {
        when(campaign.getMedicsNeed()).thenReturn(0);
        assertFalse(hasMedicsNeeded(campaign));
    }

    @Test
    void oneMedicNeeded() {
        when(campaign.getMedicsNeed()).thenReturn(1);
        assertTrue(hasMedicsNeeded(campaign));
    }

    @Test
    void negativeMedicsNeeded() {
        when(campaign.getMedicsNeed()).thenReturn(-1);
        assertFalse(hasMedicsNeeded(campaign));
    }
}
