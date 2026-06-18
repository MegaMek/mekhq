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

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.universe.PlanetarySystem;

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
              campaignOnDateWithoutCurrentLocation(CONTRACT_SYSTEM),
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

    private static Campaign campaignOnDate(PlanetarySystem currentSystem, boolean onPlanet) {
        final Campaign campaign = mock(Campaign.class);
        final AbstractLocation currentLocation = mock(AbstractLocation.class);
        when(campaign.getLocalDate()).thenReturn(CAMPAIGN_DATE);
        when(campaign.getCurrentSystem()).thenReturn(currentSystem);
        when(campaign.getCurrentLocation()).thenReturn(currentLocation);
        when(currentLocation.isOnPlanet()).thenReturn(onPlanet);
        return campaign;
    }

    private static Campaign campaignOnDateWithoutCurrentLocation(PlanetarySystem currentSystem) {
        final Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(CAMPAIGN_DATE);
        when(campaign.getCurrentSystem()).thenReturn(currentSystem);
        when(campaign.getCurrentLocation()).thenReturn(null);
        return campaign;
    }

    private static Campaign campaignInSpace(PlanetarySystem currentSystem, boolean atJumpPoint) {
        final Campaign campaign = mock(Campaign.class);
        final AbstractLocation currentLocation = mock(AbstractLocation.class);
        when(campaign.getLocalDate()).thenReturn(CAMPAIGN_DATE);
        when(campaign.getCurrentSystem()).thenReturn(currentSystem);
        when(campaign.getCurrentLocation()).thenReturn(currentLocation);
        when(currentLocation.isOnPlanet()).thenReturn(false);
        when(currentLocation.isAtJumpPoint()).thenReturn(atJumpPoint);
        return campaign;
    }

    private static Contract contractAt(PlanetarySystem system, boolean activeOnCampaignDate) {
        final Contract contract = mock(Contract.class);
        when(contract.getSystem()).thenReturn(system);
        when(contract.isActiveOn(CAMPAIGN_DATE)).thenReturn(activeOnCampaignDate);
        return contract;
    }
}
