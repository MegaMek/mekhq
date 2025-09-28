/*
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;


import static mekhq.campaign.unit.enums.TransporterType.ASF_BAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.TEST_CANON_SYSTEMS_DIR;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.mission.TestSystems;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Cubicle;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.meks.MekGyro;
import mekhq.campaign.parts.meks.MekLifeSupport;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.meks.MekSensor;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.AbstractTransportedUnitsSummary;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import testUtilities.MHQTestUtilities;

/**
 * @author Deric Page (dericdotpageatgmaildotcom)
 * @since 6/10/14 10:23 AM
 */
public class CampaignTest {

    private TestSystems systems;

    @BeforeAll
    public static void setup() {
        EquipmentType.initializeTypes();
        Ranks.initializeRankSystems();
    }

    @BeforeEach
    public void before() {
        // Reset TestSystems
        systems = TestSystems.getInstance();
    }

    @Test
    void testCampaignConstructorWithDependencyInjection() {
        // Example of using dependency injection to provide test data directly to a Campaign instance
        // without mocking or spying.

        // Create a test CampaignConfiguration with default values but using the above TestSystems instance
        CampaignConfiguration config = MHQTestUtilities.buildTestConfigWithSystems(systems);

        // Let's try switching the year up.
        config.setCurrentDay(LocalDate.ofYearDay(2875, 183));

        // Add a system to the systems instance; it must exist in the testresources dir
        config.getSystemsInstance().load(TEST_CANON_SYSTEMS_DIR + "Skye.yml");

        // Instantiate the campaign with the new info
        Campaign campaign = new Campaign(config);

        // Let's plot a trip from the starting location to Skye!  It should be about 6 days:
        int travelTime = campaign.getSimplifiedTravelTime(systems.getSystemByName("Skye", config.getDate()));
        assertEquals(6, travelTime);
    }

    @Test
    void testGetTechs() {
        List<Person> testPersonList = new ArrayList<>(5);
        List<Person> testActivePersonList = new ArrayList<>(5);

        Person mockTechActive = mock(Person.class);
        when(mockTechActive.isTech()).thenReturn(true);
        when(mockTechActive.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechActive.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechActive).getStatus();
        when(mockTechActive.getMinutesLeft()).thenReturn(240);
        when(mockTechActive.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechActive);
        testActivePersonList.add(mockTechActive);

        Person mockTechActiveTwo = mock(Person.class);
        when(mockTechActiveTwo.isTech()).thenReturn(true);
        when(mockTechActiveTwo.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechActiveTwo.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechActiveTwo).getStatus();
        when(mockTechActiveTwo.getMinutesLeft()).thenReturn(1);
        when(mockTechActiveTwo.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechActiveTwo);
        testActivePersonList.add(mockTechActiveTwo);

        Person mockTechInactive = mock(Person.class);
        when(mockTechInactive.isTech()).thenReturn(true);
        when(mockTechInactive.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechInactive.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.RETIRED).when(mockTechInactive).getStatus();
        when(mockTechInactive.getMinutesLeft()).thenReturn(240);
        when(mockTechInactive.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechInactive);

        Person mockTechNoTime = mock(Person.class);
        when(mockTechNoTime.isTech()).thenReturn(true);
        when(mockTechNoTime.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechNoTime.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechNoTime).getStatus();
        when(mockTechNoTime.getMinutesLeft()).thenReturn(0);
        when(mockTechNoTime.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechNoTime);
        testActivePersonList.add(mockTechNoTime);

        Person mockNonTechOne = mock(Person.class);
        when(mockNonTechOne.isTech()).thenReturn(false);
        when(mockNonTechOne.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(mockNonTechOne.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockNonTechOne).getStatus();
        when(mockNonTechOne.getMinutesLeft()).thenReturn(240);
        when(mockNonTechOne.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockNonTechOne);
        testActivePersonList.add(mockNonTechOne);

        Person mockNonTechTwo = mock(Person.class);
        when(mockNonTechTwo.isTech()).thenReturn(false);
        when(mockNonTechTwo.getPrimaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR_COMMAND);
        when(mockNonTechTwo.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockNonTechTwo).getStatus();
        when(mockNonTechTwo.getMinutesLeft()).thenReturn(240);
        when(mockNonTechTwo.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockNonTechTwo);
        testActivePersonList.add(mockNonTechTwo);

        Campaign testCampaign = mock(Campaign.class);
        when(testCampaign.getPersonnel()).thenReturn(testPersonList);
        when(testCampaign.getActivePersonnel(true)).thenReturn(testActivePersonList);
        when(testCampaign.getTechs()).thenCallRealMethod();
        when(testCampaign.getTechs(anyBoolean())).thenCallRealMethod();
        when(testCampaign.getTechs(anyBoolean(), anyBoolean())).thenCallRealMethod();
        when(testCampaign.getTechsExpanded(anyBoolean(), anyBoolean(), anyBoolean())).thenCallRealMethod();

        // Test just getting the list of active techs.
        List<Person> expected = new ArrayList<>(3);
        expected.add(mockTechActive);
        expected.add(mockTechActiveTwo);
        expected.add(mockTechNoTime);
        assertEquals(expected, testCampaign.getTechs());

        // Test getting active techs with time remaining.
        expected = new ArrayList<>(2);
        expected.add(mockTechActive);
        expected.add(mockTechActiveTwo);
        assertEquals(expected, testCampaign.getTechs(true));
    }

    @ParameterizedTest
    @EnumSource(value = CampaignTransportType.class)
    void testTransportShips(CampaignTransportType campaignTransportType) {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());

        // New campaigns have no transports
        assertTrue(campaign.getTransports(campaignTransportType).isEmpty());
        campaign.hasTransports(campaignTransportType);

        // Create a mock transport
        Dropship mockTransport = mock(Dropship.class);

        UUID mockId = UUID.randomUUID();
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(mockId);
        when(mockUnit.getEntity()).thenReturn(mockTransport);

        // Create mock transport capacity info for transport
        AbstractTransportedUnitsSummary mockTransportedUnitsSummary = mock(campaignTransportType.getTransportedUnitsSummaryType());
        when(mockTransportedUnitsSummary.getTransportCapabilities()).thenReturn(new HashSet<>(List.of(ASF_BAY)));

        when(mockUnit.getTransportedUnitsSummary(campaignTransportType)).thenReturn(mockTransportedUnitsSummary);

        // Add our mock transport
        campaign.importUnit(mockUnit);
        campaign.addCampaignTransport(campaignTransportType, mockUnit);

        // Ensure our mock transport exists
        assertEquals(1, campaign.getTransports(campaignTransportType).size());
        assertTrue(campaign.getTransportsByType(campaignTransportType, ASF_BAY).contains(mockUnit));

        // Add our mock transport a second time
        campaign.addCampaignTransport(campaignTransportType, mockUnit);

        // Ensure our mock transport exists only once
        assertEquals(1, campaign.getTransports(campaignTransportType).size());
        assertTrue(campaign.getTransportsByType(campaignTransportType, ASF_BAY).contains(mockUnit));

        // Remove the mock transport
        campaign.removeCampaignTransporter(campaignTransportType, mockUnit);

        // Ensure it was removed
        campaign.hasTransports(campaignTransportType);
        assertTrue(campaign.getTransports(campaignTransportType).isEmpty());
    }

    @Test
    void testInitiative() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();

        campaign.applyInitiativeBonus(6);
        // should increase bonus to 6 and max to 6
        assertEquals(6, campaign.getInitiativeBonus());
        assertEquals(6, campaign.getInitiativeMaxBonus());
        // Should not be able to increment over max of 6
        campaign.initiativeBonusIncrement(true);
        assertNotEquals(7, campaign.getInitiativeBonus());
        campaign.applyInitiativeBonus(2);
        assertEquals(6, campaign.getInitiativeBonus());
        // But should be able to decrease below max
        campaign.initiativeBonusIncrement(false);
        assertEquals(5, campaign.getInitiativeBonus());
        // After setting lower Max Bonus any applied bonus that's less than max should set
        // bonus to max
        campaign.setInitiativeMaxBonus(3);
        campaign.applyInitiativeBonus(2);
        assertEquals(3, campaign.getInitiativeBonus());

    }

    @Nested
    public class TestAutoLogistics {
        static Campaign campaign;

        @BeforeAll
        public static void beforeAll() {
            // beforeEach MUST refresh Campaign Options!
            // It is very time-consuming recreating Campaign for each test, let's try to reuse it
            campaign = MHQTestUtilities.getTestCampaign();
        }

        @Nested
        public class TestAutoLogisticsCampaignOptions {
            final static int FIRST_DESIRED_STOCK = 100;
            final static int SECOND_DESIRED_STOCK = 200;

            @BeforeEach
            public void beforeEach() {
                // beforeEach MUST refresh Campaign Options!
                campaign.setCampaignOptions(new CampaignOptions());
            }

            @Test
            public void testGetSetStockPercentHeatSink() {
                // Act
                campaign.getCampaignOptions().setAutoLogisticsHeatSink(FIRST_DESIRED_STOCK);
                int firstStockLevel = campaign.getCampaignOptions().getAutoLogisticsHeatSink();

                // Let's change the stock level to something else so we can make sure it properly changes
                campaign.getCampaignOptions().setAutoLogisticsHeatSink(SECOND_DESIRED_STOCK);
                int secondStockLevel = campaign.getCampaignOptions().getAutoLogisticsHeatSink();

                // Assert
                assertEquals(FIRST_DESIRED_STOCK, firstStockLevel);
                assertEquals(SECOND_DESIRED_STOCK, secondStockLevel);
            }

            @Test
            public void testGetSetStockPercentMekHead() {
                // Act
                campaign.getCampaignOptions().setAutoLogisticsMekHead(FIRST_DESIRED_STOCK);
                int firstStockLevel = campaign.getCampaignOptions().getAutoLogisticsMekHead();

                // Let's change the stock level to something else so we can make sure it properly changes
                campaign.getCampaignOptions().setAutoLogisticsMekHead(SECOND_DESIRED_STOCK);
                int secondStockLevel = campaign.getCampaignOptions().getAutoLogisticsMekHead();

                // Assert
                assertEquals(FIRST_DESIRED_STOCK, firstStockLevel);
                assertEquals(SECOND_DESIRED_STOCK, secondStockLevel);
            }

            @Test
            public void testGetSetStockPercentNonRepairable() {
                // Act
                campaign.getCampaignOptions().setAutoLogisticsNonRepairableLocation(FIRST_DESIRED_STOCK);
                int firstStockLevel = campaign.getCampaignOptions().getAutoLogisticsNonRepairableLocation();

                // Let's change the stock level to something else so we can make sure it properly changes
                campaign.getCampaignOptions().setAutoLogisticsNonRepairableLocation(SECOND_DESIRED_STOCK);
                int secondStockLevel = campaign.getCampaignOptions().getAutoLogisticsNonRepairableLocation();

                // Assert
                assertEquals(FIRST_DESIRED_STOCK, firstStockLevel);
                assertEquals(SECOND_DESIRED_STOCK, secondStockLevel);
            }

            @Test
            public void testGetSetStockPercentMekLocation() {
                // Act
                campaign.getCampaignOptions().setAutoLogisticsMekLocation(FIRST_DESIRED_STOCK);
                int firstStockLevel = campaign.getCampaignOptions().getAutoLogisticsMekLocation();

                // Let's change the stock level to something else so we can make sure it properly changes
                campaign.getCampaignOptions().setAutoLogisticsMekLocation(SECOND_DESIRED_STOCK);
                int secondStockLevel = campaign.getCampaignOptions().getAutoLogisticsMekLocation();

                // Assert
                assertEquals(FIRST_DESIRED_STOCK, firstStockLevel);
                assertEquals(SECOND_DESIRED_STOCK, secondStockLevel);
            }

            @Test
            public void testGetSetStockPercentAmmunition() {
                // Act
                campaign.getCampaignOptions().setAutoLogisticsAmmunition(FIRST_DESIRED_STOCK);
                int firstStockLevel = campaign.getCampaignOptions().getAutoLogisticsAmmunition();

                // Let's change the stock level to something else so we can make sure it properly changes
                campaign.getCampaignOptions().setAutoLogisticsAmmunition(SECOND_DESIRED_STOCK);
                int secondStockLevel = campaign.getCampaignOptions().getAutoLogisticsAmmunition();

                // Assert
                assertEquals(FIRST_DESIRED_STOCK, firstStockLevel);
                assertEquals(SECOND_DESIRED_STOCK, secondStockLevel);
            }

            @Test
            public void testGetSetStockPercentArmor() {
                // Act
                campaign.getCampaignOptions().setAutoLogisticsArmor(FIRST_DESIRED_STOCK);
                int firstStockLevel = campaign.getCampaignOptions().getAutoLogisticsArmor();

                // Let's change the stock level to something else so we can make sure it properly changes
                campaign.getCampaignOptions().setAutoLogisticsArmor(SECOND_DESIRED_STOCK);
                int secondStockLevel = campaign.getCampaignOptions().getAutoLogisticsArmor();

                // Assert
                assertEquals(FIRST_DESIRED_STOCK, firstStockLevel);
                assertEquals(SECOND_DESIRED_STOCK, secondStockLevel);
            }

            @Test
            public void testGetSetStockPercentActuators() {
                // Act
                campaign.getCampaignOptions().setAutoLogisticsActuators(FIRST_DESIRED_STOCK);
                int firstStockLevel = campaign.getCampaignOptions().getAutoLogisticsActuators();

                // Let's change the stock level to something else so we can make sure it properly changes
                campaign.getCampaignOptions().setAutoLogisticsActuators(SECOND_DESIRED_STOCK);
                int secondStockLevel = campaign.getCampaignOptions().getAutoLogisticsActuators();

                // Assert
                assertEquals(FIRST_DESIRED_STOCK, firstStockLevel);
                assertEquals(SECOND_DESIRED_STOCK, secondStockLevel);
            }

            @Test
            public void testGetSetStockPercentJumpJet() {
                // Act
                campaign.getCampaignOptions().setAutoLogisticsJumpJets(FIRST_DESIRED_STOCK);
                int firstStockLevel = campaign.getCampaignOptions().getAutoLogisticsJumpJets();

                // Let's change the stock level to something else so we can make sure it properly changes
                campaign.getCampaignOptions().setAutoLogisticsJumpJets(SECOND_DESIRED_STOCK);
                int secondStockLevel = campaign.getCampaignOptions().getAutoLogisticsJumpJets();

                // Assert
                assertEquals(FIRST_DESIRED_STOCK, firstStockLevel);
                assertEquals(SECOND_DESIRED_STOCK, secondStockLevel);
            }

            @Test
            public void testGetSetStockPercentEngines() {
                // Act
                campaign.getCampaignOptions().setAutoLogisticsEngines(FIRST_DESIRED_STOCK);
                int firstStockLevel = campaign.getCampaignOptions().getAutoLogisticsEngines();

                // Let's change the stock level to something else so we can make sure it properly changes
                campaign.getCampaignOptions().setAutoLogisticsEngines(SECOND_DESIRED_STOCK);
                int secondStockLevel = campaign.getCampaignOptions().getAutoLogisticsEngines();

                // Assert
                assertEquals(FIRST_DESIRED_STOCK, firstStockLevel);
                assertEquals(SECOND_DESIRED_STOCK, secondStockLevel);
            }

            @Test
            public void testGetSetStockPercentWeapons() {
                // Act
                campaign.getCampaignOptions().setAutoLogisticsWeapons(FIRST_DESIRED_STOCK);
                int firstStockLevel = campaign.getCampaignOptions().getAutoLogisticsWeapons();

                // Let's change the stock level to something else so we can make sure it properly changes
                campaign.getCampaignOptions().setAutoLogisticsWeapons(SECOND_DESIRED_STOCK);
                int secondStockLevel = campaign.getCampaignOptions().getAutoLogisticsWeapons();

                // Assert
                assertEquals(FIRST_DESIRED_STOCK, firstStockLevel);
                assertEquals(SECOND_DESIRED_STOCK, secondStockLevel);
            }

            @Test
            public void testGetSetStockPercentOther() {
                // Act
                campaign.getCampaignOptions().setAutoLogisticsOther(FIRST_DESIRED_STOCK);
                int firstStockLevel = campaign.getCampaignOptions().getAutoLogisticsOther();

                // Let's change the stock level to something else so we can make sure it properly changes
                campaign.getCampaignOptions().setAutoLogisticsOther(SECOND_DESIRED_STOCK);
                int secondStockLevel = campaign.getCampaignOptions().getAutoLogisticsOther();

                // Assert
                assertEquals(FIRST_DESIRED_STOCK, firstStockLevel);
                assertEquals(SECOND_DESIRED_STOCK, secondStockLevel);
            }
        }

        /**
         * {@link Campaign# getDefaultStockPercent} is private, so we'll need to use reflection to get the method for
         * testing
         */
        @Nested
        public class TestAutoLogisticsDefaultStockPercent {
            final int DESIRED_STOCK_LEVEL = 100;
            final int INCORRECT_STOCK_LEVEL = 15;
            static Set<Part> parts;

            static CampaignOptions mockCampaignOptions;
            static Method method;

            int initialStockPercent;
            int desiredStockPercent;
            List<Integer> initialAllPercents;
            List<Integer> afterChangeAllPercents;
            Part part;

            @BeforeAll
            static public void beforeAll() {
                mockCampaignOptions = mock(CampaignOptions.class);
                campaign.setCampaignOptions(mockCampaignOptions);

                parts = new HashSet<>(Arrays.asList(new HeatSink(),
                      new MekLocation(Mek.LOC_HEAD, 1, 0, false, false, false, false, false, campaign)));

                try {
                    method = campaign.getClass().getDeclaredMethod("getDefaultStockPercent", Part.class);
                    method.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            /**
             * @return parts that are not explicitly handled by {@link Campaign# getDefaultStockPercent(Part)}
             */
            public static Stream<Part> otherUnhandledDefaultStockPercentParts() {
                return Stream.of(new MekGyro(), new Cubicle(), new MekSensor(), new MekLifeSupport());
            }

            @BeforeEach
            void beforeEach() {
                when(mockCampaignOptions.getAutoLogisticsHeatSink()).thenReturn(INCORRECT_STOCK_LEVEL);
                when(mockCampaignOptions.getAutoLogisticsMekHead()).thenReturn(INCORRECT_STOCK_LEVEL);
                when(mockCampaignOptions.getAutoLogisticsNonRepairableLocation()).thenReturn(INCORRECT_STOCK_LEVEL);
                when(mockCampaignOptions.getAutoLogisticsMekLocation()).thenReturn(INCORRECT_STOCK_LEVEL);
                when(mockCampaignOptions.getAutoLogisticsAmmunition()).thenReturn(INCORRECT_STOCK_LEVEL);
                when(mockCampaignOptions.getAutoLogisticsArmor()).thenReturn(INCORRECT_STOCK_LEVEL);
                when(mockCampaignOptions.getAutoLogisticsActuators()).thenReturn(INCORRECT_STOCK_LEVEL);
                when(mockCampaignOptions.getAutoLogisticsJumpJets()).thenReturn(INCORRECT_STOCK_LEVEL);
                when(mockCampaignOptions.getAutoLogisticsEngines()).thenReturn(INCORRECT_STOCK_LEVEL);
                when(mockCampaignOptions.getAutoLogisticsWeapons()).thenReturn(INCORRECT_STOCK_LEVEL);
                when(mockCampaignOptions.getAutoLogisticsOther()).thenReturn(INCORRECT_STOCK_LEVEL);
            }

            @Test
            public void testGetDefaultStockPercentHeatSink() {
                // Arrange
                part = new HeatSink();

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsHeatSink()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @Test
            public void testGetDefaultStockPercentHead() {
                // Arrange
                part = new MekLocation(Mek.LOC_HEAD, 1, 0, false, false, false, false, false, campaign);

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsMekHead()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @Test
            public void testGetDefaultStockPercentCT() {
                // Arrange
                part = new MekLocation(Mek.LOC_CENTER_TORSO, 1, 0, false, false, false, false, false, campaign);

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsNonRepairableLocation()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @ParameterizedTest
            @ValueSource(ints = { Mek.LOC_LEFT_ARM, Mek.LOC_RIGHT_ARM, Mek.LOC_LEFT_TORSO, Mek.LOC_RIGHT_TORSO })
            public void testGetDefaultStockPercentOtherLocation(int location) {
                // Arrange
                part = new MekLocation(location, 1, 0, false, false, false, false, false, campaign);

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsMekLocation()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @Test
            public void testGetDefaultStockPercentTankLocation() {
                // Arrange
                part = new TankLocation();

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsNonRepairableLocation()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @Test
            public void testGetDefaultStockPercentAmmoBin() {
                // Arrange
                part = new AmmoBin();

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsAmmunition()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @Test
            public void testGetDefaultStockPercentAmmoStorage() {
                // Arrange
                part = new AmmoStorage();

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsAmmunition()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @Test
            public void testGetDefaultStockPercentArmor() {
                // Arrange
                part = new Armor();

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsArmor()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @Test
            public void testGetDefaultStockPercentActuator() {
                // Arrange
                part = new MekActuator();

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsActuators()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @Test
            public void testGetDefaultStockPercentJumpJets() {
                // Arrange
                part = new JumpJet();

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsJumpJets()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @Test
            public void testGetDefaultStockPercentEngines() {
                // Arrange
                part = new EnginePart();

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsEngines()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @Test
            public void testGetDefaultStockPercentWeapons() {
                // Arrange
                WeaponType mockWeaponType = mock(WeaponType.class);
                part = new EquipmentPart(1, mockWeaponType, Entity.LOC_NONE, 1.0, false, campaign);

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsWeapons()).thenReturn(DESIRED_STOCK_LEVEL); //TODO

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            @ParameterizedTest
            @MethodSource(value = "otherUnhandledDefaultStockPercentParts")
            public void testGetDefaultStockPercentOtherUnhandled(Part otherPart) {
                // Arrange
                part = otherPart;

                // Act
                try {
                    initialStockPercent = (int) method.invoke(campaign, part);
                    initialAllPercents = getAllDefaultStockPercents();

                    // Let's change it and make sure that it uses the new value
                    when(mockCampaignOptions.getAutoLogisticsOther()).thenReturn(DESIRED_STOCK_LEVEL);

                    desiredStockPercent = (int) method.invoke(campaign, part);
                    afterChangeAllPercents = getAllDefaultStockPercents();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Assert
                assertEquals(INCORRECT_STOCK_LEVEL, initialStockPercent);
                assertEquals(DESIRED_STOCK_LEVEL, desiredStockPercent);

                // None of the initial defaults should contain the desired stock percent
                assertFalse(initialAllPercents.contains(desiredStockPercent));

                // Only one of these should be the desired stock percent
                assertEquals(1, afterChangeAllPercents.stream().filter(i -> i == DESIRED_STOCK_LEVEL).toArray().length);
            }

            private List<Integer> getAllDefaultStockPercents() {
                List<Integer> stockPercents = new ArrayList<>();

                try {
                    stockPercents.add((int) method.invoke(campaign, part));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                return stockPercents;
            }
        }
    }
}
