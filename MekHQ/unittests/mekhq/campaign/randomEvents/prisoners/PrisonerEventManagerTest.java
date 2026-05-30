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
import static java.lang.Math.round;
import static mekhq.campaign.force.FormationType.SECURITY;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.PRISONER_CAPACITY_BATTLE_ARMOR;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.PRISONER_CAPACITY_CAM_OPS_MULTIPLIER;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.PRISONER_CAPACITY_CONVENTIONAL_INFANTRY;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.PRISONER_CAPACITY_OTHER_UNIT_MAX_MULTIPLIER;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.PRISONER_CAPACITY_OTHER_UNIT_MULTIPLIER;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.TEMPORARY_CAPACITY_DEGRADE_RATE;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.calculatePrisonerCapacity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerCaptureStyle;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Nested;
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
        boolean eventTriggered = eventManager.checkForRansomEvents().getFirst();

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
        boolean eventTriggered = results.getFirst();
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
        boolean eventTriggered = results.getFirst();
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
        boolean minorEvent = results.getFirst();
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
        boolean minorEvent = results.getFirst();
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
        boolean minorEvent = results.getFirst();
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
        boolean minorEvent = results.getFirst();
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
        boolean minorEvent = results.getFirst();
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
        boolean minorEvent = results.getFirst();
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
        boolean minorEvent = results.getFirst();
        boolean majorEvent = results.get(1);

        // Assert
        assertTrue(minorEvent);
        assertTrue(majorEvent);
    }

    /**
     * Tests for {@link PrisonerEventManager#calculatePrisonerCapacity(Campaign)}.
     */
    @Nested
    class CalculatePrisonerCapacity {

        private Campaign buildCampaign(PrisonerCaptureStyle captureStyle, int temporaryCapacity) {
            CampaignOptions mockOptions = mock(CampaignOptions.class);
            when(mockOptions.getPrisonerCaptureStyle()).thenReturn(captureStyle);

            Campaign mockCampaign = mock(Campaign.class);
            when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
            when(mockCampaign.getTemporaryPrisonerCapacity()).thenReturn(temporaryCapacity);
            when(mockCampaign.getActiveContracts()).thenReturn(List.of());
            return mockCampaign;
        }

        private Formation securityFormation(Vector<UUID> unitIds) {
            Formation mockFormation = mock(Formation.class);
            when(mockFormation.isFormationType(SECURITY)).thenReturn(true);
            when(mockFormation.getUnits()).thenReturn(unitIds);
            return mockFormation;
        }

        @Test
        void noFormations_returnsZero() {
            // Arrange
            Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
            when(mockCampaign.getAllFormations()).thenReturn(List.of());

            // Act
            int result = calculatePrisonerCapacity(mockCampaign);

            // Assert
            assertEquals(0, result);
        }

        @Test
        void nonSecurityFormation_returnsZero() {
            // Arrange
            Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
            Formation mockFormation = mock(Formation.class);
            // isFormationType returns false by default — formation is not SECURITY
            when(mockCampaign.getAllFormations()).thenReturn(List.of(mockFormation));

            // Act
            int result = calculatePrisonerCapacity(mockCampaign);

            // Assert
            assertEquals(0, result);
        }

        @Test
        void unavailableUnit_returnsZero() {
            // Arrange
            UUID unitId = UUID.randomUUID();
            Vector<UUID> unitIds = new Vector<>(List.of(unitId));

            Unit mockUnit = mock(Unit.class);
            when(mockUnit.isAvailable()).thenReturn(false);

            Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
            Formation formation = securityFormation(unitIds);
            when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
            when(mockCampaign.getUnit(unitId)).thenReturn(mockUnit);

            // Act
            int result = calculatePrisonerCapacity(mockCampaign);

            // Assert
            assertEquals(0, result);
        }

        @Test
        void battleArmor_mekHQStyle_countsOperableSuits() {
            // Arrange
            UUID unitId = UUID.randomUUID();
            Vector<UUID> unitIds = new Vector<>(List.of(unitId));

            Unit mockUnit = mock(Unit.class);
            when(mockUnit.isAvailable()).thenReturn(true);
            when(mockUnit.isBattleArmor()).thenReturn(true);
            when(mockUnit.getCrew()).thenReturn(List.of(mock(Person.class), mock(Person.class)));
            when(mockUnit.isBattleArmorSuitOperable(0)).thenReturn(true);
            when(mockUnit.isBattleArmorSuitOperable(1)).thenReturn(true);

            Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
            Formation formation = securityFormation(unitIds);
            when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
            when(mockCampaign.getUnit(unitId)).thenReturn(mockUnit);

            // Act
            int result = calculatePrisonerCapacity(mockCampaign);

            // Assert
            assertEquals(2 * PRISONER_CAPACITY_BATTLE_ARMOR, result);
        }

        @Test
        void battleArmor_camOpsStyle_appliesCapacityMultiplier() {
            // Arrange
            UUID unitId = UUID.randomUUID();
            Vector<UUID> unitIds = new Vector<>(List.of(unitId));

            Unit mockUnit = mock(Unit.class);
            when(mockUnit.isAvailable()).thenReturn(true);
            when(mockUnit.isBattleArmor()).thenReturn(true);
            when(mockUnit.getCrew()).thenReturn(List.of(mock(Person.class), mock(Person.class)));
            when(mockUnit.isBattleArmorSuitOperable(0)).thenReturn(true);
            when(mockUnit.isBattleArmorSuitOperable(1)).thenReturn(true);

            Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.CAMPAIGN_OPERATIONS, 100);
            Formation formation = securityFormation(unitIds);
            when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
            when(mockCampaign.getUnit(unitId)).thenReturn(mockUnit);

            // Act
            int result = calculatePrisonerCapacity(mockCampaign);

            // Assert
            assertEquals(2 * PRISONER_CAPACITY_BATTLE_ARMOR * PRISONER_CAPACITY_CAM_OPS_MULTIPLIER, result);
        }

        @Test
        void conventionalInfantry_mekHQStyle_countsHealthySoldiers() {
            // Arrange
            UUID unitId = UUID.randomUUID();
            Vector<UUID> unitIds = new Vector<>(List.of(unitId));

            Person s1 = mock(Person.class);
            Person s2 = mock(Person.class);
            Person s3 = mock(Person.class);
            // needsFixing returns false by default

            Unit mockUnit = mock(Unit.class);
            when(mockUnit.isAvailable()).thenReturn(true);
            when(mockUnit.isBattleArmor()).thenReturn(false);
            when(mockUnit.isConventionalInfantry()).thenReturn(true);
            when(mockUnit.getCrew()).thenReturn(List.of(s1, s2, s3));

            Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
            Formation formation = securityFormation(unitIds);
            when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
            when(mockCampaign.getUnit(unitId)).thenReturn(mockUnit);

            // Act
            int result = calculatePrisonerCapacity(mockCampaign);

            // Assert
            assertEquals(3 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY, result);
        }

        @Test
        void conventionalInfantry_injuredSoldierSkipped_returnsReducedCapacity() {
            // Arrange
            UUID unitId = UUID.randomUUID();
            Vector<UUID> unitIds = new Vector<>(List.of(unitId));

            Person healthy1 = mock(Person.class);
            Person healthy2 = mock(Person.class);
            Person injured = mock(Person.class);
            when(injured.needsFixing()).thenReturn(true);

            Unit mockUnit = mock(Unit.class);
            when(mockUnit.isAvailable()).thenReturn(true);
            when(mockUnit.isBattleArmor()).thenReturn(false);
            when(mockUnit.isConventionalInfantry()).thenReturn(true);
            when(mockUnit.getCrew()).thenReturn(List.of(healthy1, healthy2, injured));

            Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
            Formation formation = securityFormation(unitIds);
            when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
            when(mockCampaign.getUnit(unitId)).thenReturn(mockUnit);

            // Act
            int result = calculatePrisonerCapacity(mockCampaign);

            // Assert
            assertEquals(2 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY, result);
        }

        @Test
        void otherUnit_undamaged_mekHQStyle_boostsCapacityMultiplier() {
            // Arrange — infantry unit provides base capacity; other unit raises the multiplier
            UUID infantryId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            Vector<UUID> unitIds = new Vector<>(List.of(infantryId, otherId));

            Person s1 = mock(Person.class);
            Person s2 = mock(Person.class);
            Person s3 = mock(Person.class);
            Person s4 = mock(Person.class);

            Unit infantryUnit = mock(Unit.class);
            when(infantryUnit.isAvailable()).thenReturn(true);
            when(infantryUnit.isBattleArmor()).thenReturn(false);
            when(infantryUnit.isConventionalInfantry()).thenReturn(true);
            when(infantryUnit.getCrew()).thenReturn(List.of(s1, s2, s3, s4));

            Unit otherUnit = mock(Unit.class);
            when(otherUnit.isAvailable()).thenReturn(true);
            when(otherUnit.isBattleArmor()).thenReturn(false);
            when(otherUnit.isConventionalInfantry()).thenReturn(false);
            when(otherUnit.isDamaged()).thenReturn(false);

            Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
            Formation formation = securityFormation(unitIds);
            when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
            when(mockCampaign.getUnit(infantryId)).thenReturn(infantryUnit);
            when(mockCampaign.getUnit(otherId)).thenReturn(otherUnit);

            int baseCapacity = 4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY;
            int expected = (int) round(baseCapacity * (1.0 + PRISONER_CAPACITY_OTHER_UNIT_MULTIPLIER));

            // Act
            int result = calculatePrisonerCapacity(mockCampaign);

            // Assert
            assertEquals(expected, result);
        }

        @Test
        void otherUnit_damaged_mekHQStyle_doesNotBoostMultiplier() {
            // Arrange — damaged other unit leaves the multiplier at its 1.0 baseline
            UUID infantryId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            Vector<UUID> unitIds = new Vector<>(List.of(infantryId, otherId));

            Person s1 = mock(Person.class);
            Person s2 = mock(Person.class);
            Person s3 = mock(Person.class);
            Person s4 = mock(Person.class);

            Unit infantryUnit = mock(Unit.class);
            when(infantryUnit.isAvailable()).thenReturn(true);
            when(infantryUnit.isBattleArmor()).thenReturn(false);
            when(infantryUnit.isConventionalInfantry()).thenReturn(true);
            when(infantryUnit.getCrew()).thenReturn(List.of(s1, s2, s3, s4));

            Unit otherUnit = mock(Unit.class);
            when(otherUnit.isAvailable()).thenReturn(true);
            when(otherUnit.isBattleArmor()).thenReturn(false);
            when(otherUnit.isConventionalInfantry()).thenReturn(false);
            when(otherUnit.isDamaged()).thenReturn(true);

            Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
            Formation formation = securityFormation(unitIds);
            when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
            when(mockCampaign.getUnit(infantryId)).thenReturn(infantryUnit);
            when(mockCampaign.getUnit(otherId)).thenReturn(otherUnit);

            // Act
            int result = calculatePrisonerCapacity(mockCampaign);

            // Assert
            assertEquals(4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY, result);
        }

        @Test
        void manyOtherUnits_multiplierCappedAtMaximum() {
            // Arrange — 6 undamaged other units would push multiplier to 1.30, but cap is 1.25
            UUID infantryId = UUID.randomUUID();
            Vector<UUID> unitIds = new Vector<>(List.of(infantryId));

            Person s1 = mock(Person.class);
            Person s2 = mock(Person.class);
            Person s3 = mock(Person.class);
            Person s4 = mock(Person.class);

            Unit infantryUnit = mock(Unit.class);
            when(infantryUnit.isAvailable()).thenReturn(true);
            when(infantryUnit.isBattleArmor()).thenReturn(false);
            when(infantryUnit.isConventionalInfantry()).thenReturn(true);
            when(infantryUnit.getCrew()).thenReturn(List.of(s1, s2, s3, s4));

            Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
            when(mockCampaign.getUnit(infantryId)).thenReturn(infantryUnit);

            for (int i = 0; i < 6; i++) {
                UUID otherId = UUID.randomUUID();
                unitIds.add(otherId);
                Unit otherUnit = mock(Unit.class);
                when(otherUnit.isAvailable()).thenReturn(true);
                when(otherUnit.isBattleArmor()).thenReturn(false);
                when(otherUnit.isConventionalInfantry()).thenReturn(false);
                when(otherUnit.isDamaged()).thenReturn(false);
                when(mockCampaign.getUnit(otherId)).thenReturn(otherUnit);
            }

            Formation formation = securityFormation(unitIds);
            when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));

            int baseCapacity = 4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY;
            int expected = (int) round(baseCapacity * PRISONER_CAPACITY_OTHER_UNIT_MAX_MULTIPLIER);

            // Act
            int result = calculatePrisonerCapacity(mockCampaign);

            // Assert
            assertEquals(expected, result);
        }

        @Test
        void temporaryCapacityModifier_scalesResult() {
            // Arrange — halved temporary capacity halves the final result
            UUID unitId = UUID.randomUUID();
            Vector<UUID> unitIds = new Vector<>(List.of(unitId));

            Person s1 = mock(Person.class);
            Person s2 = mock(Person.class);
            Person s3 = mock(Person.class);
            Person s4 = mock(Person.class);

            Unit mockUnit = mock(Unit.class);
            when(mockUnit.isAvailable()).thenReturn(true);
            when(mockUnit.isBattleArmor()).thenReturn(false);
            when(mockUnit.isConventionalInfantry()).thenReturn(true);
            when(mockUnit.getCrew()).thenReturn(List.of(s1, s2, s3, s4));

            Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 50);
            Formation formation = securityFormation(unitIds);
            when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
            when(mockCampaign.getUnit(unitId)).thenReturn(mockUnit);

            int expected = (int) round(4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY * 1.0 * 0.5);

            // Act
            int result = calculatePrisonerCapacity(mockCampaign);

            // Assert
            assertEquals(expected, result);
        }

        /**
         * Documents the intended behavior of {@link PrisonerEventManager#calculatePrisonerCapacity(Campaign)}
         * with respect to blob crew (temporary personnel stored as integers rather than {@link Person} objects).
         *
         * <p>Blob crew should count equally toward prisoner capacity alongside regular {@link Person} crew.
         * These tests are expected to <strong>fail</strong> until {@code calculatePrisonerCapacity} is updated
         * to include {@link mekhq.campaign.unit.Unit#getTotalTempCrew()} in its calculations.</p>
         *
         * <p>Note: tank capacity is derived from {@code otherUnitMultiplier} which does not examine crew
         * at all, so the tank blob-crew tests pass under the current implementation.</p>
         */
        @Nested
        class BlobCrewCounting {

            @Test
            void conventionalInfantry_partialBlobCrew_blobSoldiersCountedEquallyToRegular() {
                // Arrange
                int blobCount = 2;
                UUID unitId = UUID.randomUUID();
                Vector<UUID> unitIds = new Vector<>(List.of(unitId));

                Unit mockUnit = mock(Unit.class);
                when(mockUnit.isAvailable()).thenReturn(true);
                when(mockUnit.isBattleArmor()).thenReturn(false);
                when(mockUnit.isConventionalInfantry()).thenReturn(true);
                when(mockUnit.getCrew()).thenReturn(List.of(mock(Person.class)));
                when(mockUnit.getTotalTempCrew()).thenReturn(blobCount);

                Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
                Formation formation = securityFormation(unitIds);
                when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
                when(mockCampaign.getUnit(unitId)).thenReturn(mockUnit);

                // Act
                int result = calculatePrisonerCapacity(mockCampaign);

                // Assert
                assertEquals((1 + blobCount) * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY, result);
            }

            @Test
            void conventionalInfantry_fullyCrewedWithBlobCrew_blobSoldiersCountedEquallyToRegular() {
                // Arrange
                int blobCount = 3;
                UUID unitId = UUID.randomUUID();
                Vector<UUID> unitIds = new Vector<>(List.of(unitId));

                Unit mockUnit = mock(Unit.class);
                when(mockUnit.isAvailable()).thenReturn(true);
                when(mockUnit.isBattleArmor()).thenReturn(false);
                when(mockUnit.isConventionalInfantry()).thenReturn(true);
                when(mockUnit.getCrew()).thenReturn(List.of(mock(Person.class)));
                when(mockUnit.getTotalTempCrew()).thenReturn(blobCount);

                Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
                Formation formation = securityFormation(unitIds);
                when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
                when(mockCampaign.getUnit(unitId)).thenReturn(mockUnit);

                // Act
                int result = calculatePrisonerCapacity(mockCampaign);

                // Assert
                assertEquals((1 + blobCount) * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY, result);
            }

            @Test
            void battleArmor_partialBlobCrew_blobTroopersCountedEquallyToRegular() {
                // Arrange
                int blobCount = 2;
                UUID unitId = UUID.randomUUID();
                Vector<UUID> unitIds = new Vector<>(List.of(unitId));

                Unit mockUnit = mock(Unit.class);
                when(mockUnit.isAvailable()).thenReturn(true);
                when(mockUnit.isBattleArmor()).thenReturn(true);
                when(mockUnit.getCrew()).thenReturn(List.of(mock(Person.class)));
                when(mockUnit.isBattleArmorSuitOperable(0)).thenReturn(true);
                when(mockUnit.getTotalTempCrew()).thenReturn(blobCount);

                Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
                Formation formation = securityFormation(unitIds);
                when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
                when(mockCampaign.getUnit(unitId)).thenReturn(mockUnit);

                // Act
                int result = calculatePrisonerCapacity(mockCampaign);

                // Assert
                assertEquals((1 + blobCount) * PRISONER_CAPACITY_BATTLE_ARMOR, result);
            }

            @Test
            void battleArmor_fullyCrewedWithBlobCrew_blobTroopersCountedEquallyToRegular() {
                // Arrange
                int blobCount = 3;
                UUID unitId = UUID.randomUUID();
                Vector<UUID> unitIds = new Vector<>(List.of(unitId));

                Unit mockUnit = mock(Unit.class);
                when(mockUnit.isAvailable()).thenReturn(true);
                when(mockUnit.isBattleArmor()).thenReturn(true);
                when(mockUnit.getCrew()).thenReturn(List.of(mock(Person.class)));
                when(mockUnit.isBattleArmorSuitOperable(0)).thenReturn(true);
                when(mockUnit.getTotalTempCrew()).thenReturn(blobCount);

                Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
                Formation formation = securityFormation(unitIds);
                when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
                when(mockCampaign.getUnit(unitId)).thenReturn(mockUnit);

                // Act
                int result = calculatePrisonerCapacity(mockCampaign);

                // Assert
                assertEquals((1 + blobCount) * PRISONER_CAPACITY_BATTLE_ARMOR, result);
            }

            @Test
            void tank_partialBlobCrew_unitContributesToCapacity() {
                // Arrange — infantry provides base capacity; blob-crewed tank still raises the multiplier
                UUID infantryId = UUID.randomUUID();
                UUID tankId = UUID.randomUUID();
                Vector<UUID> unitIds = new Vector<>(List.of(infantryId, tankId));

                Unit infantryUnit = mock(Unit.class);
                when(infantryUnit.isAvailable()).thenReturn(true);
                when(infantryUnit.isBattleArmor()).thenReturn(false);
                when(infantryUnit.isConventionalInfantry()).thenReturn(true);
                when(infantryUnit.getCrew()).thenReturn(
                      List.of(mock(Person.class), mock(Person.class),
                              mock(Person.class), mock(Person.class)));

                Unit tankUnit = mock(Unit.class);
                when(tankUnit.isAvailable()).thenReturn(true);
                when(tankUnit.isBattleArmor()).thenReturn(false);
                when(tankUnit.isConventionalInfantry()).thenReturn(false);
                when(tankUnit.isDamaged()).thenReturn(false);
                when(tankUnit.getCrew()).thenReturn(List.of(mock(Person.class)));
                when(tankUnit.getTotalTempCrew()).thenReturn(2);

                Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
                Formation formation = securityFormation(unitIds);
                when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
                when(mockCampaign.getUnit(infantryId)).thenReturn(infantryUnit);
                when(mockCampaign.getUnit(tankId)).thenReturn(tankUnit);

                int baseCapacity = 4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY;
                int expected = (int) round(baseCapacity * (1.0 + PRISONER_CAPACITY_OTHER_UNIT_MULTIPLIER));

                // Act
                int result = calculatePrisonerCapacity(mockCampaign);

                // Assert
                assertEquals(expected, result);
            }

            @Test
            void tank_fullyCrewedWithBlobCrew_unitContributesToCapacity() {
                // Arrange — infantry provides base capacity; fully blob-crewed tank still raises the multiplier
                UUID infantryId = UUID.randomUUID();
                UUID tankId = UUID.randomUUID();
                Vector<UUID> unitIds = new Vector<>(List.of(infantryId, tankId));

                Unit infantryUnit = mock(Unit.class);
                when(infantryUnit.isAvailable()).thenReturn(true);
                when(infantryUnit.isBattleArmor()).thenReturn(false);
                when(infantryUnit.isConventionalInfantry()).thenReturn(true);
                when(infantryUnit.getCrew()).thenReturn(
                      List.of(mock(Person.class), mock(Person.class),
                              mock(Person.class), mock(Person.class)));

                Unit tankUnit = mock(Unit.class);
                when(tankUnit.isAvailable()).thenReturn(true);
                when(tankUnit.isBattleArmor()).thenReturn(false);
                when(tankUnit.isConventionalInfantry()).thenReturn(false);
                when(tankUnit.isDamaged()).thenReturn(false);
                when(tankUnit.getCrew()).thenReturn(List.of(mock(Person.class)));
                when(tankUnit.getTotalTempCrew()).thenReturn(3);

                Campaign mockCampaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
                Formation formation = securityFormation(unitIds);
                when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
                when(mockCampaign.getUnit(infantryId)).thenReturn(infantryUnit);
                when(mockCampaign.getUnit(tankId)).thenReturn(tankUnit);

                int baseCapacity = 4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY;
                int expected = (int) round(baseCapacity * (1.0 + PRISONER_CAPACITY_OTHER_UNIT_MULTIPLIER));

                // Act
                int result = calculatePrisonerCapacity(mockCampaign);

                // Assert
                assertEquals(expected, result);
            }
        }
    }
}
