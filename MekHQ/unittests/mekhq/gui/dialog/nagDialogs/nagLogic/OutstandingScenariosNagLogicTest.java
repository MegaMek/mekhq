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

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.gui.dialog.nagDialogs.OutstandingScenariosNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

import static mekhq.gui.dialog.nagDialogs.nagLogic.OutstandingScenariosNagLogic.hasOutStandingScenarios;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link OutstandingScenariosNagDialog} class.
 * It contains tests for various scenarios related to the {@code checkForOutstandingScenarios} method
 */
class OutstandingScenariosNagLogicTest {
    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private AtBContract contract;
    private AtBScenario scenario1, scenario2;
    private LocalDate today;

    protected final transient ResourceBundle resources = ResourceBundle.getBundle(
        "mekhq.resources.GUI", MekHQ.getMHQOptions().getLocale());

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        campaign = mock(Campaign.class);
        campaignOptions = mock(CampaignOptions.class);
        contract = mock(AtBContract.class);
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
