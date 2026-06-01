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
import java.util.ArrayList;
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
            when(mockCampaign.getAllFormations()).thenReturn(List.of());
            return mockCampaign;
        }

        private Formation securityFormation(Vector<UUID> unitIds) {
            Formation mockFormation = mock(Formation.class);
            when(mockFormation.isFormationType(SECURITY)).thenReturn(true);
            when(mockFormation.getUnits()).thenReturn(unitIds);
            return mockFormation;
        }

        private Person healthySoldier() {
            return mock(Person.class);
        }

        private Person injuredSoldier() {
            Person p = mock(Person.class);
            when(p.needsFixing()).thenReturn(true);
            return p;
        }

        private Unit infantryUnit(Person... crew) {
            Unit unit = mock(Unit.class);
            when(unit.isAvailable()).thenReturn(true);
            when(unit.isBattleArmor()).thenReturn(false);
            when(unit.isConventionalInfantry()).thenReturn(true);
            when(unit.getCrew()).thenReturn(List.of(crew));
            return unit;
        }

        private Unit infantryUnit(int soldierCount) {
            Person[] crew = new Person[soldierCount];
            for (int i = 0; i < soldierCount; i++) {
                crew[i] = healthySoldier();
            }
            return infantryUnit(crew);
        }

        private Unit battleArmorUnit(int operableSuits) {
            Unit unit = mock(Unit.class);
            when(unit.isAvailable()).thenReturn(true);
            when(unit.isBattleArmor()).thenReturn(true);
            List<Person> crew = new ArrayList<>();
            for (int i = 0; i < operableSuits; i++) {
                crew.add(mock(Person.class));
                when(unit.isBattleArmorSuitOperable(i)).thenReturn(true);
            }
            when(unit.getCrew()).thenReturn(crew);
            return unit;
        }

        private Unit otherUnit(boolean damaged) {
            Unit unit = mock(Unit.class);
            when(unit.isAvailable()).thenReturn(true);
            when(unit.isBattleArmor()).thenReturn(false);
            when(unit.isConventionalInfantry()).thenReturn(false);
            when(unit.isDamaged()).thenReturn(damaged);
            return unit;
        }

        private Campaign campaignWithUnits(PrisonerCaptureStyle style, int tempCapacity, Unit... units) {
            Vector<UUID> unitIds = new Vector<>();
            Campaign mockCampaign = buildCampaign(style, tempCapacity);
            for (Unit unit : units) {
                UUID id = UUID.randomUUID();
                unitIds.add(id);
                when(mockCampaign.getUnit(id)).thenReturn(unit);
            }
            Formation formation = securityFormation(unitIds);
            when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));
            return mockCampaign;
        }

        private Campaign campaignWithUnits(Unit... units) {
            return campaignWithUnits(PrisonerCaptureStyle.MEKHQ, 100, units);
        }

        private Campaign campaignWithUnit(Unit unit, PrisonerCaptureStyle style, int tempCapacity) {
            return campaignWithUnits(style, tempCapacity, unit);
        }

        private Campaign campaignWithUnit(Unit unit) {
            return campaignWithUnits(unit);
        }

        @Test
        void noFormations_returnsZero() {
            // Arrange
            Campaign campaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);

            // Act
            int result = calculatePrisonerCapacity(campaign);

            // Assert
            assertEquals(0, result);
        }

        @Test
        void nonSecurityFormation_returnsZero() {
            // Arrange
            // isFormationType returns false by default — formation is not SECURITY
            Campaign campaign = buildCampaign(PrisonerCaptureStyle.MEKHQ, 100);
            when(campaign.getAllFormations()).thenReturn(List.of(mock(Formation.class)));

            // Act
            int result = calculatePrisonerCapacity(campaign);

            // Assert
            assertEquals(0, result);
        }

        @Test
        void unavailableUnit_returnsZero() {
            // Arrange
            Unit mockUnit = mock(Unit.class);
            when(mockUnit.isAvailable()).thenReturn(false);
            Campaign campaign = campaignWithUnit(mockUnit);

            // Act
            int result = calculatePrisonerCapacity(campaign);

            // Assert
            assertEquals(0, result);
        }

        @Test
        void battleArmor_mekHQStyle_countsOperableSuits() {
            // Arrange
            Campaign campaign = campaignWithUnit(battleArmorUnit(2));

            // Act
            int result = calculatePrisonerCapacity(campaign);

            // Assert
            assertEquals(2 * PRISONER_CAPACITY_BATTLE_ARMOR, result);
        }

        @Test
        void battleArmor_camOpsStyle_appliesCapacityMultiplier() {
            // Arrange
            Campaign campaign = campaignWithUnit(battleArmorUnit(2), PrisonerCaptureStyle.CAMPAIGN_OPERATIONS, 100);

            // Act
            int result = calculatePrisonerCapacity(campaign);

            // Assert
            assertEquals(2 * PRISONER_CAPACITY_BATTLE_ARMOR * PRISONER_CAPACITY_CAM_OPS_MULTIPLIER, result);
        }

        @Test
        void conventionalInfantry_mekHQStyle_countsHealthySoldiers() {
            // Arrange
            Campaign campaign = campaignWithUnit(infantryUnit(3));

            // Act
            int result = calculatePrisonerCapacity(campaign);

            // Assert
            assertEquals(3 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY, result);
        }

        @Test
        void conventionalInfantry_injuredSoldierSkipped_returnsReducedCapacity() {
            // Arrange
            Campaign campaign = campaignWithUnit(infantryUnit(healthySoldier(), healthySoldier(), injuredSoldier()));

            // Act
            int result = calculatePrisonerCapacity(campaign);

            // Assert
            assertEquals(2 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY, result);
        }

        @Test
        void otherUnit_undamaged_mekHQStyle_boostsCapacityMultiplier() {
            // Arrange — infantry provides base capacity; other unit raises the multiplier
            Campaign campaign = campaignWithUnits(infantryUnit(4), otherUnit(false));
            int expected = (int) round(4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY *
                                             (1.0 + PRISONER_CAPACITY_OTHER_UNIT_MULTIPLIER));

            // Act
            int result = calculatePrisonerCapacity(campaign);

            // Assert
            assertEquals(expected, result);
        }

        @Test
        void otherUnit_damaged_mekHQStyle_doesNotBoostMultiplier() {
            // Arrange — damaged other unit leaves the multiplier at its 1.0 baseline
            Campaign campaign = campaignWithUnits(infantryUnit(4), otherUnit(true));

            // Act
            int result = calculatePrisonerCapacity(campaign);

            // Assert
            assertEquals(4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY, result);
        }

        @Test
        void manyOtherUnits_multiplierCappedAtMaximum() {
            // Arrange — 6 undamaged other units would push multiplier to 1.30, but cap is 1.25
            Unit[] units = new Unit[7];
            units[0] = infantryUnit(4);
            for (int i = 1; i < units.length; i++) {
                units[i] = otherUnit(false);
            }
            Campaign campaign = campaignWithUnits(units);
            int expected = (int) round(4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY *
                                             PRISONER_CAPACITY_OTHER_UNIT_MAX_MULTIPLIER);

            // Act
            int result = calculatePrisonerCapacity(campaign);

            // Assert
            assertEquals(expected, result);
        }

        @Test
        void temporaryCapacityModifier_scalesResult() {
            // Arrange — halved temporary capacity halves the final result
            Campaign campaign = campaignWithUnit(infantryUnit(4), PrisonerCaptureStyle.MEKHQ, 50);
            int expected = (int) round(4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY * 0.5);

            // Act
            int result = calculatePrisonerCapacity(campaign);

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
                Unit unit = infantryUnit(1);
                when(unit.getTotalTempCrew()).thenReturn(blobCount);
                Campaign campaign = campaignWithUnit(unit);

                // Act
                int result = calculatePrisonerCapacity(campaign);

                // Assert
                assertEquals((1 + blobCount) * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY, result);
            }

            @Test
            void conventionalInfantry_fullyCrewedWithBlobCrew_blobSoldiersCountedEquallyToRegular() {
                // Arrange
                int blobCount = 3;
                Unit unit = infantryUnit(1);
                when(unit.getTotalTempCrew()).thenReturn(blobCount);
                Campaign campaign = campaignWithUnit(unit);

                // Act
                int result = calculatePrisonerCapacity(campaign);

                // Assert
                assertEquals((1 + blobCount) * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY, result);
            }

            @Test
            void battleArmor_partialBlobCrew_blobTroopersCountedEquallyToRegular() {
                // Arrange
                int blobCount = 2;
                Unit unit = battleArmorUnit(1);
                when(unit.getTotalTempCrew()).thenReturn(blobCount);
                Campaign campaign = campaignWithUnit(unit);

                // Act
                int result = calculatePrisonerCapacity(campaign);

                // Assert
                assertEquals((1 + blobCount) * PRISONER_CAPACITY_BATTLE_ARMOR, result);
            }

            @Test
            void battleArmor_fullyCrewedWithBlobCrew_blobTroopersCountedEquallyToRegular() {
                // Arrange
                int blobCount = 3;
                Unit unit = battleArmorUnit(1);
                when(unit.getTotalTempCrew()).thenReturn(blobCount);
                Campaign campaign = campaignWithUnit(unit);

                // Act
                int result = calculatePrisonerCapacity(campaign);

                // Assert
                assertEquals((1 + blobCount) * PRISONER_CAPACITY_BATTLE_ARMOR, result);
            }

            @Test
            void tank_partialBlobCrew_unitContributesToCapacity() {
                // Arrange — infantry provides base capacity; blob-crewed tank still raises the multiplier
                Unit tank = otherUnit(false);
                when(tank.getTotalTempCrew()).thenReturn(2);
                Campaign campaign = campaignWithUnits(infantryUnit(4), tank);
                int expected = (int) round(4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY *
                                                 (1.0 + PRISONER_CAPACITY_OTHER_UNIT_MULTIPLIER));

                // Act
                int result = calculatePrisonerCapacity(campaign);

                // Assert
                assertEquals(expected, result);
            }

            @Test
            void tank_fullyCrewedWithBlobCrew_unitContributesToCapacity() {
                // Arrange — infantry provides base capacity; fully blob-crewed tank still raises the multiplier
                Unit tank = otherUnit(false);
                when(tank.getTotalTempCrew()).thenReturn(3);
                Campaign campaign = campaignWithUnits(infantryUnit(4), tank);
                int expected = (int) round(4 * PRISONER_CAPACITY_CONVENTIONAL_INFANTRY *
                                                 (1.0 + PRISONER_CAPACITY_OTHER_UNIT_MULTIPLIER));

                // Act
                int result = calculatePrisonerCapacity(campaign);

                // Assert
                assertEquals(expected, result);
            }
        }
    }
}
