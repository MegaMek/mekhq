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
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.stratcon.StratconTrackState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static mekhq.gui.dialog.nagDialogs.UnresolvedStratConContactsNagDialog.nagUnresolvedContacts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link UnresolvedStratConContactsNagDialog} class.
 * It contains tests for various scenarios related to the {@code nagUnresolvedContacts} method
 */
public class UnresolvedStratConContactsNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private LocalDate today;
    private CampaignOptions options;
    private AtBContract contract;
    private StratconCampaignState stratconCampaignState;
    private StratconTrackState track;

    private StratconCoords coordinates;
    private StratconScenario scenarioNotDue, scenarioDue;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        options = mock(CampaignOptions.class);
        today = LocalDate.now();

        contract = mock(AtBContract.class);

        stratconCampaignState = mock(StratconCampaignState.class);
        track = mock(StratconTrackState.class);

        coordinates = mock(StratconCoords.class);

        scenarioNotDue = mock(StratconScenario.class);
        scenarioDue = mock(StratconScenario.class);

        // Stubs
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(today);

        when(contract.getStratconCampaignState()).thenReturn(stratconCampaignState);
        when(stratconCampaignState.getTracks()).thenReturn(Collections.singletonList(track));

        when(options.isUseStratCon()).thenReturn(true);
        when(campaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        when(scenarioDue.getCurrentState()).thenReturn(ScenarioState.UNRESOLVED);
        when(scenarioDue.getDeploymentDate()).thenReturn(today);
        when(scenarioDue.getName()).thenReturn("Scenario Due");

        when(scenarioNotDue.getCurrentState()).thenReturn(ScenarioState.UNRESOLVED);
        when(scenarioNotDue.getDeploymentDate()).thenReturn(today.plusDays(1));
        when(scenarioNotDue.getName()).thenReturn("Scenario Not Due");
    }

    @Test
    void stratConDisabled() {
        when(options.isUseStratCon()).thenReturn(false);

        assertEquals("", nagUnresolvedContacts(campaign));
    }

    @Test
    void noActiveContract() {
        when(campaign.getActiveAtBContracts()).thenReturn(new ArrayList<>());

        assertEquals("", nagUnresolvedContacts(campaign));
    }

    @Test
    void nullStratConState() {
        when(contract.getStratconCampaignState()).thenReturn(null);

        assertEquals("", nagUnresolvedContacts(campaign));
    }

    @Test
    void noScenarios() {
        when(track.getScenarios()).thenReturn(new HashMap<>());

        assertEquals("", nagUnresolvedContacts(campaign));
    }

    @Test
    void noUnresolvedScenarios() {
        Map<StratconCoords, StratconScenario> mockMap = new HashMap<>();
        mockMap.put(coordinates, scenarioNotDue);

        when(track.getScenarios()).thenReturn(mockMap);
        when(scenarioNotDue.getCurrentState()).thenReturn(ScenarioState.COMPLETED);

        assertEquals("", nagUnresolvedContacts(campaign));
    }

    @Test
    void noScenariosDue() {
        when(track.getScenarios()).thenReturn(Collections.singletonMap(coordinates, scenarioNotDue));
        when(track.getDisplayableName()).thenReturn("Test Track");

        assertEquals("", nagUnresolvedContacts(campaign));
    }

    @Test
    void scenarioDue() {
        when(track.getScenarios()).thenReturn(Collections.singletonMap(coordinates, scenarioDue));
        when(track.getDisplayableName()).thenReturn("Test Track");

        assertEquals("Scenario Due, Test Track\n", nagUnresolvedContacts(campaign));
    }
}
