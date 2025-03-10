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
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.dialog.nagDialogs.UnresolvedStratConContactsNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static mekhq.gui.dialog.nagDialogs.nagLogic.UnresolvedStratConContactsNagLogic.hasUnresolvedContacts;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link UnresolvedStratConContactsNagDialog} class.
 * It contains tests for various scenarios related to the {@code nagUnresolvedContacts} method
 */
public class UnresolvedStratConContactsNagLogicTest {
    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private AtBContract contract;
    private StratconCampaignState stratconCampaignState;
    private StratconTrackState track;
    private LocalDate today;
    private StratconScenario scenario1, scenario2;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        campaign = mock(Campaign.class);
        campaignOptions = mock(CampaignOptions.class);
        today = LocalDate.of(3025, 1, 1);
        contract = mock(AtBContract.class);
        stratconCampaignState = mock(StratconCampaignState.class);
        track = mock(StratconTrackState.class);

        StratconCoords mockCoordinates1 = mock(StratconCoords.class);
        StratconCoords mockCoordinates2 = mock(StratconCoords.class);

        when(mockCoordinates1.toBTString()).thenReturn("MockCoordinate1");
        when(mockCoordinates2.toBTString()).thenReturn("MockCoordinate2");

        scenario1 = mock(StratconScenario.class);
        scenario2 = mock(StratconScenario.class);

        when(scenario1.getCoords()).thenReturn(mockCoordinates1);
        when(scenario2.getCoords()).thenReturn(mockCoordinates2);

        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.isUseStratCon()).thenReturn(true);

        when(campaign.getActiveAtBContracts()).thenReturn(List.of(contract));
        when(contract.getStratconCampaignState()).thenReturn(stratconCampaignState);
        when(stratconCampaignState.getTracks()).thenReturn(List.of(track));

        when(track.getScenarios()).thenReturn(Map.of(mockCoordinates1, scenario1, mockCoordinates2, scenario2));
        when(scenario1.getCurrentState()).thenReturn(ScenarioState.UNRESOLVED);
        when(scenario2.getCurrentState()).thenReturn(ScenarioState.UNRESOLVED);
        when(campaign.getLocalDate()).thenReturn(today);
    }

    @Test
    public void noScenariosDue() {
        when(scenario1.getDeploymentDate()).thenReturn(today.plusDays(1));
        when(scenario2.getDeploymentDate()).thenReturn(today.plusDays(1));

        assertFalse(hasUnresolvedContacts(campaign));
    }

    @Test
    public void scenariosDue() {
        when(scenario1.getDeploymentDate()).thenReturn(today.plusDays(1));
        when(scenario2.getDeploymentDate()).thenReturn(today);

        assertTrue(hasUnresolvedContacts(campaign));
    }
}
