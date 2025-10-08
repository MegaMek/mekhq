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
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.stratCon.StratConCoords;
import mekhq.campaign.stratCon.StratConScenario;
import mekhq.campaign.stratCon.StratConScenario.ScenarioState;
import mekhq.campaign.stratCon.StratConTrackState;
import mekhq.gui.dialog.nagDialogs.UnresolvedStratConContactsNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class is a test class for the {@link UnresolvedStratConContactsNagDialog} class. It contains tests for various
 * scenarios related to the {@code nagUnresolvedContacts} method
 */
public class UnresolvedStratConContactsNagLogicTest {
    private AtBContract contract;
    private LocalDate today;
    private StratConScenario stratConScenario1, stratConScenario2;

    /**
     * Test setup for each test, runs before each test. Initializes the mock objects and sets up the necessary mock
     * behaviors.
     */
    @BeforeEach
    void init() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        today = LocalDate.of(3025, 1, 1);
        contract = mock(AtBContract.class);
        StratConCampaignState stratconCampaignState = mock(StratConCampaignState.class);
        StratConTrackState track = mock(StratConTrackState.class);

        StratConCoords mockCoordinates1 = mock(StratConCoords.class);
        StratConCoords mockCoordinates2 = mock(StratConCoords.class);

        when(mockCoordinates1.toBTString()).thenReturn("MockCoordinate1");
        when(mockCoordinates2.toBTString()).thenReturn("MockCoordinate2");

        stratConScenario1 = mock(StratConScenario.class);
        stratConScenario2 = mock(StratConScenario.class);

        when(stratConScenario1.getCoords()).thenReturn(mockCoordinates1);
        when(stratConScenario2.getCoords()).thenReturn(mockCoordinates2);

        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.isUseStratCon()).thenReturn(true);

        when(campaign.getActiveAtBContracts()).thenReturn(List.of(contract));
        when(contract.getStratconCampaignState()).thenReturn(stratconCampaignState);
        when(stratconCampaignState.getTracks()).thenReturn(List.of(track));

        when(track.getScenarios()).thenReturn(Map.of(mockCoordinates1,
              stratConScenario1,
              mockCoordinates2,
              stratConScenario2));
        when(stratConScenario1.getCurrentState()).thenReturn(ScenarioState.UNRESOLVED);
        when(stratConScenario2.getCurrentState()).thenReturn(ScenarioState.UNRESOLVED);
        when(campaign.getLocalDate()).thenReturn(today);
    }

    @Test
    public void noScenariosDue() {
        when(stratConScenario1.getDeploymentDate()).thenReturn(today.plusDays(1));
        when(stratConScenario2.getDeploymentDate()).thenReturn(today.plusDays(1));

        assertFalse(hasUnresolvedContacts(List.of(contract), today));
    }

    @Test
    public void scenariosDue() {
        when(stratConScenario1.getDeploymentDate()).thenReturn(today.plusDays(1));
        when(stratConScenario2.getDeploymentDate()).thenReturn(today);

        assertTrue(hasUnresolvedContacts(List.of(contract), today));
    }
}
