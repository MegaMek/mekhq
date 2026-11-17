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
package mekhq.gui.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ShellContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

class MissionViewPanelTest {
    private static final LocalDate CAMPAIGN_DATE = LocalDate.of(3045, 12, 26);
    private static final PlanetarySystem CONTRACT_SYSTEM = new PlanetarySystem("Rastaban");
    private static final PlanetarySystem OTHER_SYSTEM = new PlanetarySystem("Somewhere Else");

    @Test
    void activeContractLandedAtDestinationShowsTimeline() {
        assertTrue(MissionViewPanel.shouldShowContractTimeline(
              campaignOnDate(CONTRACT_SYSTEM, true),
              contractAt(CONTRACT_SYSTEM, true)));
    }

    @Test
    void activeContractAtDestinationButNotLandedDoesNotShowTimeline() {
        assertFalse(MissionViewPanel.shouldShowContractTimeline(
              campaignOnDate(CONTRACT_SYSTEM, false),
              contractAt(CONTRACT_SYSTEM, true)));
    }

    @Test
    void activeContractLandedAtDifferentSystemDoesNotShowTimeline() {
        assertFalse(MissionViewPanel.shouldShowContractTimeline(
              campaignOnDate(OTHER_SYSTEM, true),
              contractAt(CONTRACT_SYSTEM, true)));
    }

    @Test
    void activeContractWithoutCurrentLocationDoesNotShowTimeline() {
        assertFalse(MissionViewPanel.shouldShowContractTimeline(
              campaignOnDateWithoutCurrentLocation(),
              contractAt(CONTRACT_SYSTEM, true)));
    }

    @Test
    void activeContractWithoutContractSystemDoesNotShowTimeline() {
        assertFalse(MissionViewPanel.shouldShowContractTimeline(
              campaignOnDate(CONTRACT_SYSTEM, true),
              contractAt(null, true)));
    }

    @Test
    void inactiveContractLandedAtDestinationDoesNotShowTimeline() {
        assertFalse(MissionViewPanel.shouldShowContractTimeline(
              campaignOnDate(CONTRACT_SYSTEM, true),
              contractAt(CONTRACT_SYSTEM, false)));
    }

    @Test
    void openEndedContractLandedAtDestinationDoesNotShowTimeline() {
        assertFalse(MissionViewPanel.shouldShowContractTimeline(
              campaignOnDate(CONTRACT_SYSTEM, true),
              openEndedContractAt(CONTRACT_SYSTEM, true)));
    }

    @Test
    void landedCurrentLocationWithoutPlanetUsesSystemName() {
        assertEquals("Somewhere Else (landed)",
              MissionViewPanel.currentLocationDescription(campaignOnDate(OTHER_SYSTEM, true)));
    }

    @Test
    void atJumpPointCurrentLocationUsesSystemName() {
        assertEquals("Somewhere Else (at jump point)",
              MissionViewPanel.currentLocationDescription(campaignInSpace(OTHER_SYSTEM, true)));
    }

    @Test
    void inTransitCurrentLocationUsesSystemName() {
        assertEquals("Somewhere Else (in transit)",
              MissionViewPanel.currentLocationDescription(campaignInSpace(OTHER_SYSTEM, false)));
    }

    // Battle record

    @Test
    void battleRecordIsShownForAnActiveContract() {
        assertTrue(MissionViewPanel.isBattleRecordShown(CAMPAIGN_DATE, contractAt(CONTRACT_SYSTEM, true)));
    }

    @Test
    void battleRecordIsHiddenForAContractThatIsNotActive() {
        assertFalse(MissionViewPanel.isBattleRecordShown(CAMPAIGN_DATE, contractAt(CONTRACT_SYSTEM, false)));
    }

    @Test
    void battleRecordIsHiddenWhileTheContractIsPeaceful() {
        final AbstractContract contract = contractAt(CONTRACT_SYSTEM, true);
        when(contract.isPeaceful()).thenReturn(true);

        assertFalse(MissionViewPanel.isBattleRecordShown(CAMPAIGN_DATE, contract));
    }

    @Test
    void battleRecordIsHiddenForAShellContract() {
        final ShellContract contract = mock(ShellContract.class);
        when(contract.isActiveOn(CAMPAIGN_DATE)).thenReturn(true);

        assertFalse(MissionViewPanel.isBattleRecordShown(CAMPAIGN_DATE, contract));
    }

    @Test
    void battleRecordTextShowsTheRatingAndThePointsBehindIt() {
        final AbstractContract contract = contractAt(CONTRACT_SYSTEM, true);
        // 2 + 1 victory points against 2 defeat points for an unanswered scenario: a victory, but not a decisive one
        // Built before stubbing, as building it stubs the scenarios' own mocks
        final List<Scenario> scenarios = scenariosWithStatuses(ScenarioStatus.DECISIVE_VICTORY,
              ScenarioStatus.VICTORY, ScenarioStatus.REFUSED_ENGAGEMENT);
        when(contract.getScenarios()).thenReturn(scenarios);

        final String text = MissionViewPanel.getBattleRecordText(CAMPAIGN_DATE, contract);

        assertTrue(text.contains("<b>Victory</b>"), text);
        assertTrue(text.contains("(3 \u2013 2)"), text);
    }

    private static List<Scenario> scenariosWithStatuses(ScenarioStatus... statuses) {
        final List<Scenario> scenarios = new ArrayList<>();
        for (ScenarioStatus status : statuses) {
            final Scenario scenario = mock(Scenario.class);
            when(scenario.getDate()).thenReturn(CAMPAIGN_DATE.minusDays(7));
            when(scenario.getStatus()).thenReturn(status);
            scenarios.add(scenario);
        }
        return scenarios;
    }

    private static Campaign campaignOnDate(PlanetarySystem currentSystem, boolean onPlanet) {
        final Campaign campaign = mockCampaign();
        final AbstractLocation currentLocation = mock(AbstractLocation.class);
        when(campaign.getLocalDate()).thenReturn(CAMPAIGN_DATE);
        when(campaign.getPlayerForce().getForceDetachment().getCurrentLocation()).thenReturn(currentLocation);
        when(currentLocation.getCurrentSystem()).thenReturn(currentSystem);
        when(currentLocation.isOnPlanet()).thenReturn(onPlanet);
        return campaign;
    }

    private static Campaign campaignOnDateWithoutCurrentLocation() {
        final Campaign campaign = mockCampaign();
        when(campaign.getLocalDate()).thenReturn(CAMPAIGN_DATE);
        when(campaign.getPlayerForce().getForceDetachment().getCurrentLocation()).thenReturn(null);
        return campaign;
    }

    private static Campaign campaignInSpace(PlanetarySystem currentSystem, boolean atJumpPoint) {
        final Campaign campaign = mockCampaign();
        final AbstractLocation currentLocation = mock(AbstractLocation.class);
        when(campaign.getLocalDate()).thenReturn(CAMPAIGN_DATE);
        when(campaign.getPlayerForce().getForceDetachment().getCurrentLocation()).thenReturn(currentLocation);
        when(currentLocation.getCurrentSystem()).thenReturn(currentSystem);
        when(currentLocation.isOnPlanet()).thenReturn(false);
        when(currentLocation.isAtJumpPoint()).thenReturn(atJumpPoint);
        return campaign;
    }

    private static AbstractContract contractAt(PlanetarySystem system, boolean activeOnCampaignDate) {
        final AbstractContract contract = openEndedContractAt(system, activeOnCampaignDate);
        when(contract.getEndingDate()).thenReturn(CAMPAIGN_DATE.plusMonths(6));
        return contract;
    }

    private static AbstractContract openEndedContractAt(PlanetarySystem system, boolean activeOnCampaignDate) {
        final AbstractContract contract = mock(AbstractContract.class);
        when(contract.getTargetSystem()).thenReturn(system);
        when(contract.isActiveOn(CAMPAIGN_DATE)).thenReturn(activeOnCampaignDate);
        return contract;
    }
}
