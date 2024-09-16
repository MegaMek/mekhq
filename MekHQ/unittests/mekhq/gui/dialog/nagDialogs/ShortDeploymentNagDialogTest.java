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
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.mission.AtBContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static mekhq.gui.dialog.nagDialogs.ShortDeploymentNagDialog.checkDeploymentRequirementsMet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link ShortDeploymentNagDialog} class.
 * It contains tests for various scenarios related to the {@code checkDeploymentRequirementsMet}
 * method
 */
public class ShortDeploymentNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    // I know 'location' can be converted to a local variable, but it makes sense to keep all the
    // mock objects in one place
    private CurrentLocation location;
    private LocalDate monday, sunday;
    private AtBContract contract;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
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

    // In the following tests the checkDeploymentRequirementsMet() method is called, and its
    // response is checked against expected behavior

    @Test
    void notOnPlanet() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(false);

        assertFalse(checkDeploymentRequirementsMet(campaign));
    }

    @Test
    void notSunday() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(monday);

        assertFalse(checkDeploymentRequirementsMet(campaign));
    }

    @Test
    void noContract() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(sunday);

        when(campaign.getActiveAtBContracts()).thenReturn(new ArrayList<>());

        assertFalse(checkDeploymentRequirementsMet(campaign));
    }

    @Test
    void noDeploymentDeficit() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(sunday);

        when(campaign.getActiveAtBContracts()).thenReturn(List.of(contract));
        when(campaign.getDeploymentDeficit(contract)).thenReturn(0);

        assertFalse(checkDeploymentRequirementsMet(campaign));
    }

    @Test
    void negativeDeploymentDeficit() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(sunday);

        when(campaign.getActiveAtBContracts()).thenReturn(List.of(contract));
        when(campaign.getDeploymentDeficit(contract)).thenReturn(-3);

        assertFalse(checkDeploymentRequirementsMet(campaign));
    }

    @Test
    void positiveDeploymentDeficit() {
        when(campaign.getLocation().isOnPlanet()).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(sunday);

        when(campaign.getActiveAtBContracts()).thenReturn(List.of(contract));
        when(campaign.getDeploymentDeficit(contract)).thenReturn(1);

        assertTrue(checkDeploymentRequirementsMet(campaign));
    }
}