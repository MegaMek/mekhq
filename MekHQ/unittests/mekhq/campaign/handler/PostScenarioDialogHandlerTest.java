/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.scenarios.ScenarioResolvedEvent;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.gui.CampaignGUI;
import mekhq.service.IAutosaveService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

class PostScenarioDialogHandlerTest {
    private MockedStatic<MekHQ> mekHQ;
    private Campaign campaign;
    private IAutosaveService autosaveService;
    private RetirementDefectionTracker retirementDefectionTracker;

    @BeforeEach
    void beforeEach() {
        retirementDefectionTracker = mock(RetirementDefectionTracker.class);
        when(retirementDefectionTracker.getRetirees()).thenReturn(Set.of());
        ForceHumanResources humanResources = mock(ForceHumanResources.class);
        when(humanResources.getRetirementDefectionTracker()).thenReturn(retirementDefectionTracker);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(playerForce.getHumanResources()).thenReturn(humanResources);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.get(CampaignOption.ENABLE_AUTO_AWARDS)).thenReturn(false);

        autosaveService = mock(IAutosaveService.class);
        campaign = mock(Campaign.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getAutosaveService()).thenReturn(autosaveService);

        mekHQ = mockStatic(MekHQ.class);
    }

    @AfterEach
    void afterEach() {
        mekHQ.close();
    }

    @Test
    void autosavesOnceTheScenarioResolvedEventHasBeenHandled() {
        handle();

        InOrder inOrder = inOrder(MekHQ.class, autosaveService);
        inOrder.verify(mekHQ, () -> MekHQ.triggerEvent(any(ScenarioResolvedEvent.class)));
        inOrder.verify(autosaveService).requestAfterScenarioAutosave(campaign);
    }

    @Test
    void autosavesEvenWhenAnEarlierStepFails() {
        when(retirementDefectionTracker.getRetirees()).thenThrow(new IllegalStateException("post-battle failure"));

        assertThrows(IllegalStateException.class, this::handle);

        verify(autosaveService).requestAfterScenarioAutosave(campaign);
    }

    private void handle() {
        PostScenarioDialogHandler.handle(mock(CampaignGUI.class), campaign, mock(Scenario.class),
              mock(ResolveScenarioTracker.class));
    }
}
