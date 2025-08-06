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

import static mekhq.gui.dialog.nagDialogs.nagLogic.DeploymentShortfallNagLogic.hasDeploymentShortfall;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.mission.AtBContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class is a test class for the {@link ShortDeploymentNagDialog} class. It contains tests for various scenarios
 * related to the {@code checkDeploymentRequirementsMet} method
 */
public class ShortDeploymentNagLogicTest {
    // Mock objects for the tests
    private Campaign campaign;
    private CurrentLocation location;
    private LocalDate monday, sunday;
    private AtBContract contract;

    /**
     * Test setup for each test, runs before each test. Initializes the mock objects and sets up the necessary mock
     * behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);

        location = mock(CurrentLocation.class);

        monday = LocalDate.of(2024, 10, 7);
        sunday = LocalDate.of(2024, 10, 6);

        contract = mock(AtBContract.class);

        // Stubs
        when(campaign.getLocation()).thenReturn(location);
    }

    @Test
    void notOnPlanet() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(false);

        assertFalse(hasDeploymentShortfall(campaign));
    }

    @Test
    void notSunday() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(monday);

        assertFalse(hasDeploymentShortfall(campaign));
    }

    @Test
    void noContract() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(sunday);

        when(campaign.getActiveAtBContracts()).thenReturn(new ArrayList<>());

        assertFalse(hasDeploymentShortfall(campaign));
    }

    @Test
    void noDeploymentDeficit() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(sunday);

        when(campaign.getActiveAtBContracts()).thenReturn(List.of(contract));
        when(campaign.getDeploymentDeficit(contract)).thenReturn(0);

        assertFalse(hasDeploymentShortfall(campaign));
    }

    @Test
    void negativeDeploymentDeficit() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(sunday);

        when(campaign.getActiveAtBContracts()).thenReturn(List.of(contract));
        when(campaign.getDeploymentDeficit(contract)).thenReturn(-3);

        assertFalse(hasDeploymentShortfall(campaign));
    }

    @Test
    void positiveDeploymentDeficit() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(sunday);

        when(campaign.getActiveAtBContracts()).thenReturn(List.of(contract));
        when(campaign.getDeploymentDeficit(contract)).thenReturn(1);

        assertTrue(hasDeploymentShortfall(campaign));
    }
}
