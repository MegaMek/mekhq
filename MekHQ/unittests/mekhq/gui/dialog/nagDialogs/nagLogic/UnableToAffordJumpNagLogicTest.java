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

import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordJumpNagLogic.unableToAffordNextJump;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.dialog.nagDialogs.UnableToAffordJumpNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class is a test class for the {@link UnableToAffordJumpNagDialog} class. It contains tests for various scenarios
 * related to the {@code isUnableToAffordNextJump} method
 */
class UnableToAffordJumpNagLogicTest {
    // Mock objects for the tests
    private Campaign campaign;

    /**
     * Test setup for each test, runs before each test. Initializes the mock objects and sets up the necessary mock
     * behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        CurrentLocation location = mock(CurrentLocation.class);
        JumpPath jumpPath = mock(JumpPath.class);
        PlanetarySystem originSystem = mock(PlanetarySystem.class);
        PlanetarySystem destinationSystem = mock(PlanetarySystem.class);

        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getFunds()).thenReturn(Money.of(1));

        when(campaign.getLocation()).thenReturn(location);
        when(location.getCurrentSystem()).thenReturn(originSystem);
        when(location.getJumpPath()).thenReturn(jumpPath);
        when(jumpPath.getLastSystem()).thenReturn(destinationSystem);

        when(options.isEquipmentContractBase()).thenReturn(false);
    }

    @Test
    void canAffordNextJump() {
        when(campaign.calculateCostPerJump(true, false)).thenReturn(Money.of(0));

        assertFalse(unableToAffordNextJump(campaign));
    }

    @Test
    void cannotAffordNextJump() {
        when(campaign.calculateCostPerJump(true, false)).thenReturn(Money.of(2));

        assertTrue(unableToAffordNextJump(campaign));
    }
}
