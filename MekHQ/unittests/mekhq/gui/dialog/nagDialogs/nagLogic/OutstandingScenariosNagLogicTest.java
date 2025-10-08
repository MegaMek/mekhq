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

import static mekhq.gui.dialog.nagDialogs.nagLogic.OutstandingScenariosNagLogic.hasOutStandingScenarios;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.gui.dialog.nagDialogs.OutstandingScenariosNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class is a test class for the {@link OutstandingScenariosNagDialog} class. It contains tests for various
 * scenarios related to the {@code checkForOutstandingScenarios} method
 */
class OutstandingScenariosNagLogicTest {
    private Campaign campaign;
    private AtBScenario scenario1, scenario2;
    private LocalDate today;

    protected final transient ResourceBundle resources = ResourceBundle.getBundle(
          "mekhq.resources.GUI", MekHQ.getMHQOptions().getLocale());

    /**
     * Test setup for each test, runs before each test. Initializes the mock objects and sets up the necessary mock
     * behaviors.
     */
    @BeforeEach
    void init() {
        campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        AtBContract contract = mock(AtBContract.class);
        scenario1 = mock(AtBScenario.class);
        scenario2 = mock(AtBScenario.class);
        today = LocalDate.of(3025, 1, 1);

        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.isUseStratCon()).thenReturn(false);

        when(campaign.getActiveAtBContracts(true)).thenReturn(List.of(contract));
        when(campaign.getLocalDate()).thenReturn(today);
        when(contract.getCurrentAtBScenarios()).thenReturn(List.of(scenario1, scenario2));
    }

    @Test
    public void twoScenariosDueToday() {
        when(scenario1.getDate()).thenReturn(today);
        when(scenario2.getDate()).thenReturn(today);
        when(scenario1.getHasTrack()).thenReturn(false);
        when(scenario2.getHasTrack()).thenReturn(false);

        assertTrue(hasOutStandingScenarios(campaign));
    }

    @Test
    public void oneScenariosDueToday() {
        when(scenario1.getDate()).thenReturn(today);
        when(scenario2.getDate()).thenReturn(today.plusDays(1));
        when(scenario1.getHasTrack()).thenReturn(false);
        when(scenario2.getHasTrack()).thenReturn(false);

        assertTrue(hasOutStandingScenarios(campaign));
    }

    @Test
    public void noScenariosDueToday() {
        when(scenario1.getDate()).thenReturn(today.plusDays(1));
        when(scenario2.getDate()).thenReturn(today.plusDays(1));
        when(scenario1.getHasTrack()).thenReturn(false);
        when(scenario2.getHasTrack()).thenReturn(false);

        assertFalse(hasOutStandingScenarios(campaign));
    }
}
