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
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mekhq.gui.dialog.nagDialogs.UnableToAffordJumpNagDialog.getNextJumpCost;
import static mekhq.gui.dialog.nagDialogs.UnableToAffordJumpNagDialog.isUnableToAffordNextJump;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link UnableToAffordJumpNagDialog} class.
 * It contains tests for various scenarios related to the {@code isUnableToAffordNextJump} method
 */
class UnableToAffordJumpNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private CampaignOptions options;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        options = mock(CampaignOptions.class);

        // Stubs
        when(campaign.getCampaignOptions()).thenReturn(options);
    }

    // In the following tests the canAffordNextJump() method is called, and its response is checked
    // against expected behavior

    @Test
    void canAffordNextJump() {
        when(campaign.getFunds()).thenReturn(Money.of(5));
        when(getNextJumpCost(campaign)).thenReturn(Money.of(1));

        assertFalse(isUnableToAffordNextJump(campaign));
    }

    @Test
    void cannotAffordNextJump() {
        when(campaign.getFunds()).thenReturn(Money.of(1));
        when(getNextJumpCost(campaign)).thenReturn(Money.of(5));

        assertTrue(isUnableToAffordNextJump(campaign));
    }
}
