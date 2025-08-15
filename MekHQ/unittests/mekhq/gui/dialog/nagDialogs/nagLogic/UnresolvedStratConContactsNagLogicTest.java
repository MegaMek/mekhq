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

import static mekhq.gui.dialog.nagDialogs.nagLogic.UnresolvedStratConContactsNagLogic.hasUnresolvedContacts;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.dialog.nagDialogs.UnresolvedStratConContactsNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class is a test class for the {@link UnresolvedStratConContactsNagDialog} class. It contains tests for various
 * scenarios related to the {@code nagUnresolvedContacts} method
 */
public class UnresolvedStratConContactsNagLogicTest {
    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private AtBContract contract;
    private StratconCampaignState stratconCampaignState;
    private StratconTrackState track;
    private LocalDate today;
    private StratconScenario stratconScenario1, stratconScenario2;

    /**
     * Test setup for each test, runs before each test. Initializes the mock objects and sets up the necessary mock
     * behaviors.
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

        stratconScenario1 = mock(StratconScenario.class);
        stratconScenario2 = mock(StratconScenario.class);

        when(stratconScenario1.getCoords()).thenReturn(mockCoordinates1);
        when(stratconScenario2.getCoords()).thenReturn(mockCoordinates2);

        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.isUseStratCon()).thenReturn(true);

        when(campaign.getActiveAtBContracts()).thenReturn(List.of(contract));
        when(contract.getStratconCampaignState()).thenReturn(stratconCampaignState);
        when(stratconCampaignState.getTracks()).thenReturn(List.of(track));

        when(track.getScenarios()).thenReturn(Map.of(mockCoordinates1,
              stratconScenario1,
              mockCoordinates2,
              stratconScenario2));
        when(stratconScenario1.getCurrentState()).thenReturn(ScenarioState.UNRESOLVED);
        when(stratconScenario2.getCurrentState()).thenReturn(ScenarioState.UNRESOLVED);
        when(campaign.getLocalDate()).thenReturn(today);
    }

    @Test
    public void noScenariosDue() {
        when(stratconScenario1.getDeploymentDate()).thenReturn(today.plusDays(1));
        when(stratconScenario2.getDeploymentDate()).thenReturn(today.plusDays(1));

        assertFalse(hasUnresolvedContacts(List.of(contract), today));
    }

    @Test
    public void scenariosDue() {
        when(stratconScenario1.getDeploymentDate()).thenReturn(today.plusDays(1));
        when(stratconScenario2.getDeploymentDate()).thenReturn(today);

        assertTrue(hasUnresolvedContacts(List.of(contract), today));
    }
}
