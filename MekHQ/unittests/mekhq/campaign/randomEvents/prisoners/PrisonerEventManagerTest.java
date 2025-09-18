/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents.prisoners;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.TEMPORARY_CAPACITY_DEGRADE_RATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

/**
 * Test class for the {@link PrisonerEventManager} class.
 *
 * <p>This class contains unit tests to verify the behavior of prisoner management-related
 * functionality. It uses mock instances of dependent components to isolate and test the specific logic of the
 * {@link PrisonerEventManager} class. The tests primarily focus on scenarios involving degradation of temporary
 * prisoner capacity and events triggered by prisoner or morale conditions.</p>
 */
public class PrisonerEventManagerTest {
    @Test
    void testDegradeTemporaryCapacity_DegradeDownTowardsDefault() {
        final int INITIAL_TEMPORARY_CAPACITY = 150;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getTemporaryPrisonerCapacity()).thenReturn(INITIAL_TEMPORARY_CAPACITY);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        LocalDate today = LocalDate.of(3151, 1, 3);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        PrisonerEventManager eventManager = new PrisonerEventManager(mockCampaign);

        // Act
        int actualValue = eventManager.degradeTemporaryCapacity();
        int expectedValue = max(DEFAULT_TEMPORARY_CAPACITY,
              INITIAL_TEMPORARY_CAPACITY - TEMPORARY_CAPACITY_DEGRADE_RATE);

        // Assert
        assertEquals(expectedValue, actualValue);
        assertNotEquals(INITIAL_TEMPORARY_CAPACITY, actualValue);
    }

    @Test
    void testDegradeTemporaryCapacity_DegradeDownTowardsDefault_ResultBelowDefault() {
        final int INITIAL_TEMPORARY_CAPACITY = 101;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getTemporaryPrisonerCapacity()).thenReturn(INITIAL_TEMPORARY_CAPACITY);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        LocalDate today = LocalDate.of(3151, 1, 3);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        PrisonerEventManager eventManager = new PrisonerEventManager(mockCampaign);

        // Act
        int actualValue = eventManager.degradeTemporaryCapacity();

        // Assert
        assertEquals(DEFAULT_TEMPORARY_CAPACITY, actualValue);
        assertNotEquals(INITIAL_TEMPORARY_CAPACITY, actualValue);
    }

    @Test
    void testDegradeTemporaryCapacity_DegradeUpTowardsDefault() {
        final int INITIAL_TEMPORARY_CAPACITY = 50;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getTemporaryPrisonerCapacity()).thenReturn(INITIAL_TEMPORARY_CAPACITY);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        LocalDate today = LocalDate.of(3151, 1, 3);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        PrisonerEventManager eventManager = new PrisonerEventManager(mockCampaign);

        // Act
        int actualValue = eventManager.degradeTemporaryCapacity();

        int expectedValue = min(DEFAULT_TEMPORARY_CAPACITY,
              INITIAL_TEMPORARY_CAPACITY + TEMPORARY_CAPACITY_DEGRADE_RATE);

        // Assert
        assertEquals(expectedValue, actualValue);
        assertNotEquals(INITIAL_TEMPORARY_CAPACITY, actualValue);
    }

    @Test
    void testDegradeTemporaryCapacity_DegradeUpTowardsDefault_ResultAboveDefault() {
        final int INITIAL_TEMPORARY_CAPACITY = 99;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getTemporaryPrisonerCapacity()).thenReturn(INITIAL_TEMPORARY_CAPACITY);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        LocalDate today = LocalDate.of(3151, 1, 3);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        PrisonerEventManager eventManager = new PrisonerEventManager(mockCampaign);

        // Act
        int actualValue = eventManager.degradeTemporaryCapacity();

        // Assert
        assertEquals(DEFAULT_TEMPORARY_CAPACITY, actualValue);
        assertNotEquals(INITIAL_TEMPORARY_CAPACITY, actualValue);
    }

    @Test
    void testCheckForRansomEvents_NoEvent() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        LocalDate today = LocalDate.of(3151, 1, 3);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        AtBContract contract = new AtBContract("TEST");
        contract.setMoraleLevel(STALEMATE);
        when(mockCampaign.hasActiveContract()).thenReturn(true);
        when(mockCampaign.getActiveContracts()).thenReturn(List.of(contract));

        PrisonerEventManager realEventManager = new PrisonerEventManager(mockCampaign) {
            @Override
            protected int d6(int dice) {
                return STALEMATE.ordinal() + 1;
            }
        };

        // Act
        PrisonerEventManager eventManager = spy(realEventManager);

        // We're deliberately not triggering this when initializing eventManager, as that allows us
        // to effectively skip the rest of the logic and create a more predictable test environment.
        boolean eventTriggered = eventManager.checkForRansomEvents().get(0);

        // Assert
        assertFalse(eventTriggered);
    }

    @Test
    void testCheckForRansomEvents_EnemyEvent() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.hasActiveContract()).thenReturn(true);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        LocalDate today = LocalDate.of(3151, 1, 3);
        when(mockCampaign.getLocalDate()).thenReturn(today);


        Person friendlyPrisonerOfWar = new Person(mockCampaign);
        when(mockCampaign.getFriendlyPrisoners()).thenReturn(List.of(friendlyPrisonerOfWar));

        PrisonerEventManager realEventManager = new PrisonerEventManager(mockCampaign) {
            @Override
            protected int d6(int dice) {
                return RANSOM_EVENT_CHANCE;
            }

            @Override
            protected int randomInt(int maxValue) {
                return 2;
            }
        };

        // Act
        PrisonerEventManager eventManager = spy(realEventManager);

        // We're deliberately not triggering this when initializing eventManager, as that allows us
        // to effectively skip the rest of the logic and create a more predictable test environment.
        List<Boolean> results = eventManager.checkForRansomEvents();
        boolean eventTriggered = results.get(0);
        boolean isAllied = results.get(1);

        // Assert
        assertTrue(eventTriggered);
        assertFalse(isAllied);
    }

    @Test
    void testCheckForRansomEvents_FriendlyEvent() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.hasActiveContract()).thenReturn(true);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        LocalDate today = LocalDate.of(3151, 1, 3);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        Person friendlyPrisonerOfWar = new Person(mockCampaign);
        when(mockCampaign.getFriendlyPrisoners()).thenReturn(List.of(friendlyPrisonerOfWar));

        Finances finances = new Finances();
        when(mockCampaign.getFinances()).thenReturn(finances);

        PrisonerEventManager realEventManager = new PrisonerEventManager(mockCampaign) {
            @Override
            protected int d6(int dice) {
                return RANSOM_EVENT_CHANCE;
            }

            @Override
            protected int randomInt(int maxValue) {
                return 1;
            }
        };

        // Act
        PrisonerEventManager eventManager = spy(realEventManager);

        // We're deliberately not triggering this when initializing eventManager, as that allows us
        // to effectively skip the rest of the logic and create a more predictable test environment.
        List<Boolean> results = eventManager.checkForRansomEvents();
        boolean eventTriggered = results.get(0);
        boolean isAllied = results.get(1);

        // Assert
        assertTrue(eventTriggered);
        assertTrue(isAllied);
    }

    @Test
    void testCheckForPrisonerEvents_NoEvent() {
        // Setup
        int totalPrisoners = 1;
        int prisonerCapacity = 10;

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));

        PrisonerEventManager realEventManager = new PrisonerEventManager(campaign) {
            @Override
            protected int randomInt(int maxValue) {
                return 1;
            }
        };

        // Act
        List<Boolean> results = realEventManager.checkForPrisonerEvents(true,
              totalPrisoners,
              totalPrisoners,
              prisonerCapacity);
        boolean minorEvent = results.get(0);
        boolean majorEvent = results.get(1);

        // Assert
        assertFalse(minorEvent);
        assertFalse(majorEvent);
    }

    @Test
    void testCheckForPrisonerEvents_MinorEvent() {
        // Setup
        int totalPrisoners = 1;
        int prisonerCapacity = 0;

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));

        PrisonerEventManager realEventManager = new PrisonerEventManager(campaign) {
            @Override
            protected int randomInt(int maxValue) {
                return 1;
            }
        };

        // Act
        List<Boolean> results = realEventManager.checkForPrisonerEvents(true,
              totalPrisoners,
              totalPrisoners,
              prisonerCapacity);
        boolean minorEvent = results.get(0);
        boolean majorEvent = results.get(1);

        // Assert
        assertTrue(minorEvent);
        assertFalse(majorEvent);
    }

    @Test
    void testCheckForPrisonerEvents_MinorEvent_Automatic_BelowThreshold() {
        // Setup
        int totalPrisoners = 1;
        int prisonerCapacity = 1;

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));

        PrisonerEventManager realEventManager = new PrisonerEventManager(campaign) {
            @Override
            protected int randomInt(int maxValue) {
                return 0;
            }
        };

        // Act
        List<Boolean> results = realEventManager.checkForPrisonerEvents(true,
              totalPrisoners,
              totalPrisoners,
              prisonerCapacity);
        boolean minorEvent = results.get(0);
        boolean majorEvent = results.get(1);

        // Assert
        assertFalse(minorEvent);
        assertFalse(majorEvent);
    }

    @Test
    void testCheckForPrisonerEvents_MinorEvent_Automatic_AboveThreshold() {
        // Setup
        int totalPrisoners = 25;
        int prisonerCapacity = 25;

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));

        PrisonerEventManager realEventManager = new PrisonerEventManager(campaign) {
            @Override
            protected int randomInt(int maxValue) {
                return 0;
            }
        };

        // Act
        List<Boolean> results = realEventManager.checkForPrisonerEvents(true,
              totalPrisoners,
              totalPrisoners,
              prisonerCapacity);
        boolean minorEvent = results.get(0);
        boolean majorEvent = results.get(1);

        // Assert
        assertTrue(minorEvent);
        assertFalse(majorEvent);
    }

    @Test
    void testCheckForPrisonerEvents_MajorEvent() {
        // Setup
        int totalPrisoners = 100;
        int prisonerCapacity = 0;

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));

        PrisonerEventManager realEventManager = new PrisonerEventManager(campaign) {
            @Override
            protected int randomInt(int maxValue) {
                return 1;
            }
        };

        // Act
        List<Boolean> results = realEventManager.checkForPrisonerEvents(true,
              totalPrisoners,
              totalPrisoners,
              prisonerCapacity);
        boolean minorEvent = results.get(0);
        boolean majorEvent = results.get(1);

        // Assert
        assertTrue(minorEvent);
        assertTrue(majorEvent);
    }

    @Test
    void testCheckForPrisonerEvents_MajorEvent_NotEnoughPrisoners() {
        // Setup
        int totalPrisoners = 1;
        int prisonerCapacity = 0;

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));

        PrisonerEventManager realEventManager = new PrisonerEventManager(campaign) {
            @Override
            protected int randomInt(int maxValue) {
                return 1;
            }
        };

        // Act
        List<Boolean> results = realEventManager.checkForPrisonerEvents(true,
              totalPrisoners,
              totalPrisoners,
              prisonerCapacity);
        boolean minorEvent = results.get(0);
        boolean majorEvent = results.get(1);

        // Assert
        assertTrue(minorEvent);
        assertFalse(majorEvent);
    }

    @Test
    void testCheckForPrisonerEvents_MajorEvent_Automatic() {
        // Setup
        int totalPrisoners = 100;
        int prisonerCapacity = 100;

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));

        PrisonerEventManager realEventManager = new PrisonerEventManager(campaign) {
            @Override
            protected int randomInt(int maxValue) {
                return 0;
            }
        };

        // Act
        List<Boolean> results = realEventManager.checkForPrisonerEvents(true,
              totalPrisoners,
              totalPrisoners,
              prisonerCapacity);
        boolean minorEvent = results.get(0);
        boolean majorEvent = results.get(1);

        // Assert
        assertTrue(minorEvent);
        assertTrue(majorEvent);
    }
}
