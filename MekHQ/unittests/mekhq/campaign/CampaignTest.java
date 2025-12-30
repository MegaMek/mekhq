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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.TEST_CANON_SYSTEMS_DIR;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Stream;

import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.icons.Portrait;
import megamek.common.enums.Gender;
import megamek.common.units.Crew;
import megamek.common.units.Dropship;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.AbstractTransportedUnitsSummary;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.TestSystems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
        when(mockTechActive.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechActive);
        testActivePersonList.add(mockTechActive);

        Person mockTechActiveTwo = mock(Person.class);
        when(mockTechActiveTwo.isTech()).thenReturn(true);
        when(mockTechActiveTwo.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechActiveTwo.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechActiveTwo).getStatus();
        when(mockTechActiveTwo.getMinutesLeft()).thenReturn(1);
        when(mockTechActiveTwo.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechActiveTwo);
        testActivePersonList.add(mockTechActiveTwo);

        Person mockTechInactive = mock(Person.class);
        when(mockTechInactive.isTech()).thenReturn(true);
        when(mockTechInactive.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechInactive.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.RETIRED).when(mockTechInactive).getStatus();
        when(mockTechInactive.getMinutesLeft()).thenReturn(240);
        when(mockTechInactive.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechInactive);

        Person mockTechNoTime = mock(Person.class);
        when(mockTechNoTime.isTech()).thenReturn(true);
        when(mockTechNoTime.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechNoTime.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechNoTime).getStatus();
        when(mockTechNoTime.getMinutesLeft()).thenReturn(0);
        when(mockTechNoTime.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechNoTime);
        testActivePersonList.add(mockTechNoTime);

        Person mockNonTechOne = mock(Person.class);
        when(mockNonTechOne.isTech()).thenReturn(false);
        when(mockNonTechOne.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(mockNonTechOne.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockNonTechOne).getStatus();
        when(mockNonTechOne.getMinutesLeft()).thenReturn(240);
        when(mockNonTechOne.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockNonTechOne);
        testActivePersonList.add(mockNonTechOne);

        Person mockNonTechTwo = mock(Person.class);
        when(mockNonTechTwo.isTech()).thenReturn(false);
        when(mockNonTechTwo.getPrimaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR_COMMAND);
        when(mockNonTechTwo.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockNonTechTwo).getStatus();
        when(mockNonTechTwo.getMinutesLeft()).thenReturn(240);
        when(mockNonTechTwo.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockNonTechTwo);
        testActivePersonList.add(mockNonTechTwo);

        Campaign testCampaign = mock(Campaign.class);
        when(testCampaign.getPersonnel()).thenReturn(testPersonList);
        when(testCampaign.getActivePersonnel(false, false)).thenReturn(testActivePersonList);
        when(testCampaign.getTechs()).thenCallRealMethod();
        when(testCampaign.getTechs(anyBoolean())).thenCallRealMethod();
        when(testCampaign.getTechs(anyBoolean(), anyBoolean())).thenCallRealMethod();
        when(testCampaign.getTechsExpanded(anyBoolean(), anyBoolean(), anyBoolean())).thenCallRealMethod();

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(testCampaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.isTechsUseAdministration()).thenReturn(false);

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

    private static Person[] invokeFindTopCommanders(Campaign campaign) throws Exception {
        Method findTopCommanders = Campaign.class.getDeclaredMethod("findTopCommanders");
        findTopCommanders.setAccessible(true);
        return (Person[]) findTopCommanders.invoke(campaign);
    }

    @Test
    void findTopCommanders_whenBothFlagged_returnsThoseAndDoesNotScanPersonnel() throws Exception {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());

        Person flaggedCommander = mock(Person.class);
        Person flaggedSecond = mock(Person.class);

        doReturn(flaggedCommander).when(campaign).getFlaggedCommander();
        doReturn(flaggedSecond).when(campaign).getFlaggedSecondInCommand();

        Person[] result = invokeFindTopCommanders(campaign);

        assertSame(flaggedCommander, result[0]);
        assertSame(flaggedSecond, result[1]);

        verify(campaign, never()).getActivePersonnel(false, false);
    }

    @Test
    void findTopCommanders_whenOnlyCommanderFlagged_commanderLocked_secondChosenFromOthers() throws Exception {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());

        Person flaggedCommander = mock(Person.class);
        doReturn(flaggedCommander).when(campaign).getFlaggedCommander();
        doReturn(null).when(campaign).getFlaggedSecondInCommand();

        Person aPerson = mock(Person.class);
        Person bPerson = mock(Person.class);

        when(aPerson.outRanksUsingSkillTiebreaker(eq(campaign), eq(bPerson))).thenReturn(true);
        when(bPerson.outRanksUsingSkillTiebreaker(eq(campaign), eq(aPerson))).thenReturn(false);

        doReturn(List.of(flaggedCommander, bPerson, aPerson)).when(campaign).getActivePersonnel(false, false);

        Person[] result = invokeFindTopCommanders(campaign);

        assertSame(flaggedCommander, result[0], "Flagged commander must remain commander");
        assertSame(aPerson, result[1], "Second-in-command should be best among remaining personnel");
    }

    @Test
    void findTopCommanders_whenOnlySecondFlagged_secondLocked_commanderChosenFromOthersExcludingSecond()
          throws Exception {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());

        Person flaggedSecond = mock(Person.class);
        doReturn(null).when(campaign).getFlaggedCommander();
        doReturn(flaggedSecond).when(campaign).getFlaggedSecondInCommand();

        Person aPerson = mock(Person.class);
        Person bPerson = mock(Person.class);

        when(bPerson.outRanksUsingSkillTiebreaker(eq(campaign), eq(aPerson))).thenReturn(true);

        doReturn(List.of(aPerson, flaggedSecond, bPerson)).when(campaign).getActivePersonnel(false, false);

        Person[] result = invokeFindTopCommanders(campaign);

        assertSame(bPerson, result[0], "Commander should be the top-ranked among non-flagged-second personnel");
        assertSame(flaggedSecond, result[1], "Flagged second-in-command must remain second");
    }

    @Test
    void findTopCommanders_whenUnflagged_selectsTopTwo_andPromotesPreviousCommanderToSecondIfAppropriate()
          throws Exception {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());

        doReturn(null).when(campaign).getFlaggedCommander();
        doReturn(null).when(campaign).getFlaggedSecondInCommand();

        Person person1 = mock(Person.class);
        Person person2 = mock(Person.class);
        Person person3 = mock(Person.class);

        when(person2.outRanksUsingSkillTiebreaker(eq(campaign), eq(person1))).thenReturn(true);
        when(person3.outRanksUsingSkillTiebreaker(eq(campaign), eq(person2))).thenReturn(false);
        when(person3.outRanksUsingSkillTiebreaker(eq(campaign), eq(person1))).thenReturn(true);

        doReturn(List.of(person1, person2, person3)).when(campaign).getActivePersonnel(false, false);

        Person[] result = invokeFindTopCommanders(campaign);

        assertSame(person2, result[0], "Commander should be the best overall");
        assertSame(person3, result[1], "Second should be the best excluding commander");
    }

    @Test
    void findTopCommanders_neverReturnsSamePersonForBothSlots() throws Exception {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());

        doReturn(null).when(campaign).getFlaggedCommander();
        doReturn(null).when(campaign).getFlaggedSecondInCommand();

        Person only = mock(Person.class);
        doReturn(List.of(only)).when(campaign).getActivePersonnel(false, false);

        Person[] result = invokeFindTopCommanders(campaign);

        assertSame(only, result[0]);
        assertNull(result[1], "Second-in-command must be null when only one eligible person exists");
        assertArrayEquals(new Person[] { only, null }, result);
    }

    // region Nested Test Classes for Temp Crew
    /**
     * Parent nested test class for all temp crew tests.
     * Contains shared setup and helper methods used across all temp crew role tests.
     */
    @Nested
    class TempCrewTests {
        private Campaign testCampaign;
        private CampaignOptions campaignOptions;

        @BeforeEach
        void setupTempCrewTests() {
            testCampaign = MHQTestUtilities.getTestCampaign();
            campaignOptions = testCampaign.getCampaignOptions();
        }

        /**
         * Provides all temp crew roles for parameterized tests
         */
        private static Stream<PersonnelRole> getTempCrewRoles() {
            return Stream.of(
                PersonnelRole.SOLDIER,
                PersonnelRole.BATTLE_ARMOUR,
                PersonnelRole.VEHICLE_CREW_GROUND,
                PersonnelRole.VEHICLE_CREW_VTOL,
                PersonnelRole.VEHICLE_CREW_NAVAL,
                PersonnelRole.VESSEL_PILOT,
                PersonnelRole.VESSEL_GUNNER,
                PersonnelRole.VESSEL_CREW
            );
        }

        /**
         * Creates a mock Unit with a mock Entity configured for the specified personnel role.
         * The unit will be properly configured to use the specified temp crew type.
         *
         * @param role The personnel role this unit should be configured for
         * @param withCrew If true, unit will have 1 active crew member; if false, no crew
         */
        private Unit createMockUnitForRole(PersonnelRole role, boolean withCrew) {
            // Create mock entity based on role
            megamek.common.units.Entity mockEntity;

            // Configure entity type based on role
            switch (role) {
                case SOLDIER -> {
                    megamek.common.units.Infantry mockInfantry = mock(megamek.common.units.Infantry.class);
                    when(mockInfantry.isInfantry()).thenReturn(true);
                    when(mockInfantry.isBattleArmor()).thenReturn(false);
                    when(mockInfantry.getUnitType()).thenReturn(UnitType.INFANTRY);
                    when(mockInfantry.isConventionalInfantry()).thenReturn(true);
                    when(mockInfantry.getSquadSize()).thenReturn(5);
                    when(mockInfantry.getSquadCount()).thenReturn(5);
                    mockEntity = mockInfantry;
                }
                case BATTLE_ARMOUR -> {
                    Mounted<?> mockMounted = mock(Mounted.class);
                    when(mockMounted.isMissingForTrooper(anyInt())).thenReturn(false);

                    megamek.common.battleArmor.BattleArmor mockBattleArmor =
                          mock(megamek.common.battleArmor.BattleArmor.class);
                    when(mockBattleArmor.isInfantry()).thenReturn(true);
                    when(mockBattleArmor.isBattleArmor()).thenReturn(true);
                    when(mockBattleArmor.getUnitType()).thenReturn(UnitType.BATTLE_ARMOR);
                    when(mockBattleArmor.isConventionalInfantry()).thenReturn(false);
                    when(mockBattleArmor.locations()).thenReturn(4);
                    when(mockBattleArmor.getInternal(anyInt())).thenReturn(1);
                    when(mockBattleArmor.getEquipment()).thenReturn(List.of(mockMounted));
                    mockEntity = mockBattleArmor;
                }
                case VEHICLE_CREW_GROUND -> {
                    mockEntity = mock(megamek.common.units.Tank.class);
                    when(mockEntity.isVehicle()).thenReturn(true);
                    when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.TRACKED);
                    when(mockEntity.getUnitType()).thenReturn(UnitType.TANK);
                    when(mockEntity.getWeight()).thenReturn(60.0);
                }
                case VEHICLE_CREW_VTOL -> {
                    mockEntity = mock(megamek.common.units.VTOL.class);
                    when(mockEntity.isVehicle()).thenReturn(true);
                    when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.VTOL);
                    when(mockEntity.getUnitType()).thenReturn(UnitType.VTOL);
                    when(mockEntity.getWeight()).thenReturn(20.0);
                }
                case VEHICLE_CREW_NAVAL -> {
                    mockEntity = mock(megamek.common.units.Tank.class);
                    when(mockEntity.isVehicle()).thenReturn(true);
                    when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.NAVAL);
                    when(mockEntity.getUnitType()).thenReturn(UnitType.NAVAL);
                    when(mockEntity.getWeight()).thenReturn(60.0);
                }
                case VESSEL_PILOT, VESSEL_GUNNER, VESSEL_CREW -> {
                    mockEntity = mock(megamek.common.units.Dropship.class);
                    when(mockEntity.isLargeCraft()).thenReturn(true);
                    when(mockEntity.getUnitType()).thenReturn(UnitType.DROPSHIP);
                }
                default -> mockEntity = mock(megamek.common.units.Entity.class);
            }

            // Set common entity properties
            when(mockEntity.getId()).thenReturn(1);

            // Mock Crew
            Crew mockCrew = mock(Crew.class);
            //when(mockCrew.getSlotCount()).thenReturn(1); // Single pilot/crew
            doNothing().when(mockCrew).resetGameState();
            doNothing().when(mockCrew).setCommandBonus(anyInt());
            doNothing().when(mockCrew).setMissing(anyBoolean(), anyInt());
            doNothing().when(mockCrew).setName(any(), anyInt());
            doNothing().when(mockCrew).setNickname(any(), anyInt());
            doNothing().when(mockCrew).setGender(any(), anyInt());
            doNothing().when(mockCrew).setClanPilot(anyBoolean(), anyInt());
            doNothing().when(mockCrew).setPortrait(any(), anyInt());
            doNothing().when(mockCrew).setExternalIdAsString(any(), anyInt());
            doNothing().when(mockCrew).setToughness(anyInt(), anyInt());
            when(mockCrew.isMissing(anyInt())).thenReturn(false);
            when(mockEntity.getCrew()).thenReturn(mockCrew);

            when(mockEntity.getTransports()).thenReturn(new Vector<>());
            when(mockEntity.getSensors()).thenReturn(new Vector<>()); // Prevent NPE in clearGameData
            when(mockEntity.hasBAP()).thenReturn(false);

            // Mock all the setter methods called by clearGameData and resetPilotAndEntity
            doNothing().when(mockEntity).setPassedThrough(any());
            doNothing().when(mockEntity).resetFiringArcs();
            doNothing().when(mockEntity).resetBays();
            doNothing().when(mockEntity).setEvading(anyBoolean());
            doNothing().when(mockEntity).setFacing(anyInt());
            doNothing().when(mockEntity).setPosition(any());
            doNothing().when(mockEntity).setProne(anyBoolean());
            doNothing().when(mockEntity).setHullDown(anyBoolean());
            doNothing().when(mockEntity).setTransportId(anyInt());
            doNothing().when(mockEntity).resetTransporter();
            doNothing().when(mockEntity).setDeployRound(anyInt());
            doNothing().when(mockEntity).setSwarmAttackerId(anyInt());
            doNothing().when(mockEntity).setSwarmTargetId(anyInt());
            doNothing().when(mockEntity).setUnloaded(anyBoolean());
            doNothing().when(mockEntity).setDone(anyBoolean());
            doNothing().when(mockEntity).setLastTarget(anyInt());
            doNothing().when(mockEntity).setNeverDeployed(anyBoolean());
            doNothing().when(mockEntity).setStuck(anyBoolean());
            doNothing().when(mockEntity).resetCoolantFailureAmount();
            doNothing().when(mockEntity).setConversionMode(anyInt());
            doNothing().when(mockEntity).setDoomed(anyBoolean());
            doNothing().when(mockEntity).setDestroyed(anyBoolean());
            doNothing().when(mockEntity).setHidden(anyBoolean());
            doNothing().when(mockEntity).clearNarcAndiNarcPods();
            doNothing().when(mockEntity).setShutDown(anyBoolean());
            doNothing().when(mockEntity).setSearchlightState(anyBoolean());
            doNothing().when(mockEntity).setNextSensor(any());
            doNothing().when(mockEntity).setCommander(anyBoolean());
            doNothing().when(mockEntity).resetPickedUpMekWarriors();
            doNothing().when(mockEntity).setStartingPos(anyInt());

            // Create Unit with the mock entity and spy it so we can stub methods
            Unit unit = spy(new Unit(mockEntity, testCampaign));

            // Set up commander based on withCrew parameter
            if (withCrew) {
                setupMockCommander(unit);
            } else {
                when(unit.getCommander()).thenReturn(null);
            }

            // Configure unit crew needs based on role
            int crewSize = switch (role) {
                case SOLDIER, BATTLE_ARMOUR -> 5; // Squad size
                case VEHICLE_CREW_GROUND, VEHICLE_CREW_VTOL, VEHICLE_CREW_NAVAL -> 3; // Vehicle crew
                case VESSEL_PILOT -> 2; // Pilot team (can have temp crew backup)
                case VESSEL_GUNNER -> 3; // Gunner crew (multiple gun positions)
                case VESSEL_CREW -> 10; // Large crew
                default -> 1;
            };

            // Mock the crew list
            List<Person> activeCrew = new ArrayList<>();
            if (withCrew) {
                Person mockPerson = mock(Person.class);
                activeCrew.add(mockPerson);
            }
            when(unit.getActiveCrew()).thenReturn(activeCrew);
            when(unit.getFullCrewSize()).thenReturn(crewSize);

            // Mock role methods so unitCanUseTempCrewRole returns true
            switch (role) {
                case SOLDIER, BATTLE_ARMOUR, VEHICLE_CREW_GROUND,
                     VEHICLE_CREW_VTOL, VEHICLE_CREW_NAVAL, VESSEL_PILOT -> {
                    when(unit.getDriverRole()).thenReturn(role);
                }
                case VESSEL_GUNNER -> {
                    when(unit.getGunnerRole()).thenReturn(role);
                }
                case VESSEL_CREW -> {
                    // Can take more crew if not fully crewed (activeCrew.size() < fullCrewSize)
                    when(unit.canTakeMoreVesselCrew()).thenReturn(activeCrew.size() < crewSize);
                }
                default -> throw new IllegalStateException("Unexpected value: " + role);
            }

            return unit;
        }

        /**
         * Convenience method to create a unit without crew
         */
        private Unit createMockUnitForRole(PersonnelRole role) {
            return createMockUnitForRole(role, false);
        }

        /**
         * Helper method to set up a mock commander for a unit.
         * Call this in the Arrange phase when you need a unit with a commander.
         */
        private void setupMockCommander(Unit unit) {
            Person mockCommander = mock(Person.class);
            when(mockCommander.getFullTitle()).thenReturn("Test Commander");
            when(mockCommander.getCallsign()).thenReturn("TestPilot");
            when(mockCommander.getGender()).thenReturn(Gender.MALE);
            when(mockCommander.isClanPersonnel()).thenReturn(false);

            // Mock Portrait and make it cloneable
            Portrait mockPortrait = mock(Portrait.class);
            when(mockPortrait.clone()).thenReturn(mockPortrait);
            when(mockCommander.getPortrait()).thenReturn(mockPortrait);

            when(mockCommander.getId()).thenReturn(UUID.randomUUID());
            when(mockCommander.getAdjustedToughness()).thenReturn(0);
            when(mockCommander.getHits()).thenReturn(0);

            when(unit.getCommander()).thenReturn(mockCommander);
        }

        /**
         * Helper method to enable blob crew for a specific role
         */
        private void enableBlobCrewForRole(PersonnelRole role) {
            switch (role) {
                case SOLDIER -> campaignOptions.setUseBlobInfantry(true);
                case BATTLE_ARMOUR -> campaignOptions.setUseBlobBattleArmor(true);
                case VEHICLE_CREW_GROUND -> campaignOptions.setUseBlobVehicleCrewGround(true);
                case VEHICLE_CREW_VTOL -> campaignOptions.setUseBlobVehicleCrewVTOL(true);
                case VEHICLE_CREW_NAVAL -> campaignOptions.setUseBlobVehicleCrewNaval(true);
                case VESSEL_PILOT -> campaignOptions.setUseBlobVesselPilot(true);
                case VESSEL_GUNNER -> campaignOptions.setUseBlobVesselGunner(true);
                case VESSEL_CREW -> campaignOptions.setUseBlobVesselCrew(true);
                default -> throw new IllegalStateException("Unexpected value: " + role);
            }
        }

        /**
         * Helper method to disable blob crew for a specific role
         */
        private void disableBlobCrewForRole(PersonnelRole role) {
            switch (role) {
                case SOLDIER -> campaignOptions.setUseBlobInfantry(false);
                case BATTLE_ARMOUR -> campaignOptions.setUseBlobBattleArmor(false);
                case VEHICLE_CREW_GROUND -> campaignOptions.setUseBlobVehicleCrewGround(false);
                case VEHICLE_CREW_VTOL -> campaignOptions.setUseBlobVehicleCrewVTOL(false);
                case VEHICLE_CREW_NAVAL -> campaignOptions.setUseBlobVehicleCrewNaval(false);
                case VESSEL_PILOT -> campaignOptions.setUseBlobVesselPilot(false);
                case VESSEL_GUNNER -> campaignOptions.setUseBlobVesselGunner(false);
                case VESSEL_CREW -> campaignOptions.setUseBlobVesselCrew(false);
                default -> throw new IllegalStateException("Unexpected value: " + role);
            }
        }

        /**
         * Tests that initial pool state is zero for each temp crew role.
         * Tests {@link Campaign#getTempCrewPool(PersonnelRole)}.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testInitialPoolStateIsZero(PersonnelRole role) {
            assertEquals(0, testCampaign.getTempCrewPool(role));
        }

        /**
         * Tests setting pool to a positive value.
         * Tests {@link Campaign#setTempCrewPool(PersonnelRole, int)}.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testSetPoolToPositiveValue(PersonnelRole role) {
            // Arrange
            testCampaign.setTempCrewPool(role, 0);

            // Act
            testCampaign.setTempCrewPool(role, 10);

            // Assert
            assertEquals(10, testCampaign.getTempCrewPool(role));
        }

        /**
         * Tests that setting pool to negative value results in zero.
         * Tests {@link Campaign#setTempCrewPool(PersonnelRole, int)}.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testSetPoolToNegativeValueResultsInZero(PersonnelRole role) {
            // Arrange
            testCampaign.setTempCrewPool(role, 5);

            // Act
            testCampaign.setTempCrewPool(role, -5);

            // Assert
            assertEquals(0, testCampaign.getTempCrewPool(role));
        }

        /**
         * Tests that disabling blob crew option disables it for the role.
         * Tests {@link Campaign#isBlobCrewEnabled(PersonnelRole)}.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testDisablingBlobCrewOptionDisablesRole(PersonnelRole role) {
            // Arrange
            enableBlobCrewForRole(role);

            // Act
            disableBlobCrewForRole(role);

            // Assert
            assertTrue(!testCampaign.isBlobCrewEnabled(role));
        }

        /**
         * Tests that enabling blob crew option enables it for the role.
         * Tests {@link Campaign#isBlobCrewEnabled(PersonnelRole)}.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testEnablingBlobCrewOptionEnablesRole(PersonnelRole role) {
            // Arrange
            disableBlobCrewForRole(role);

            // Act
            enableBlobCrewForRole(role);

            // Assert
            assertTrue(testCampaign.isBlobCrewEnabled(role));
        }

        /**
         * Tests that clearing blob crew for a specific role only affects that role.
         * Tests {@link Campaign#clearBlobCrewForRole(PersonnelRole)}.
         */
        @Test
        void testClearBlobCrewForRoleIsolation() {
            // Arrange
            testCampaign.setTempCrewPool(PersonnelRole.SOLDIER, 10);
            testCampaign.setTempCrewPool(PersonnelRole.BATTLE_ARMOUR, 20);
            testCampaign.setTempCrewPool(PersonnelRole.VEHICLE_CREW_GROUND, 30);

            // Act
            testCampaign.clearBlobCrewForRole(PersonnelRole.SOLDIER);

            // Assert
            assertEquals(0, testCampaign.getTempCrewPool(PersonnelRole.SOLDIER));
            assertEquals(20, testCampaign.getTempCrewPool(PersonnelRole.BATTLE_ARMOUR));
            assertEquals(30, testCampaign.getTempCrewPool(PersonnelRole.VEHICLE_CREW_GROUND));
        }

        /**
         * Tests {@link Campaign#increaseTempCrewPool(PersonnelRole, int)}.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testIncreaseTempCrewPool(PersonnelRole role) {
            // Arrange
            testCampaign.setTempCrewPool(role, 10);

            // Act
            testCampaign.increaseTempCrewPool(role, 5);

            // Assert
            assertEquals(15, testCampaign.getTempCrewPool(role));
        }

        /**
         * Tests {@link Campaign#decreaseTempCrewPool(PersonnelRole, int)}.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testDecreaseTempCrewPool(PersonnelRole role) {
            // Arrange
            testCampaign.setTempCrewPool(role, 10);

            // Act
            testCampaign.decreaseTempCrewPool(role, 3);

            // Assert
            assertEquals(7, testCampaign.getTempCrewPool(role));
        }

        /**
         * Tests that {@link Campaign#decreaseTempCrewPool(PersonnelRole, int)} never goes below zero.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testDecreasePoolMoreThanAvailableResultsInZero(PersonnelRole role) {
            // Arrange
            testCampaign.setTempCrewPool(role, 5);

            // Act
            testCampaign.decreaseTempCrewPool(role, 20);

            // Assert
            assertEquals(0, testCampaign.getTempCrewPool(role));
        }

        /**
         * Tests {@link Campaign#emptyTempCrewPoolForRole(PersonnelRole)}.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testEmptyTempCrewPool(PersonnelRole role) {
            // Arrange
            testCampaign.setTempCrewPool(role, 50);
            enableBlobCrewForRole(role);

            // Act
            testCampaign.emptyTempCrewPoolForRole(role);

            // Assert
            assertEquals(0, testCampaign.getTempCrewPool(role));
        }

        /**
         * Tests {@link Campaign#fillTempCrewPoolForRole(PersonnelRole)} correctly calculates crew needs
         * from units WITH at least 1 crew. Units need at least 1 real crew member to be able to use temp crew.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testFillTempCrewPoolCalculatesNeedsFromUnitsWithCrew(PersonnelRole role) {
            // Arrange - Enable blob crew for this role
            enableBlobCrewForRole(role);

            // Create a mock unit WITH 1 crew member (can use temp crew)
            Unit mockUnit = createMockUnitForRole(role, true);
            testCampaign.importUnit(mockUnit);

            // Start with empty pool
            testCampaign.setTempCrewPool(role, 0);

            // Act - Fill the pool
            testCampaign.fillTempCrewPoolForRole(role);

            // Assert - Pool should be filled to match unit needs (fullCrewSize - activeCrew)
            int fullCrewSize = mockUnit.getFullCrewSize();
            int activeCrew = mockUnit.getActiveCrew().size();
            int expectedNeed = fullCrewSize - activeCrew;

            assertTrue(expectedNeed > 0);
            assertEquals(expectedNeed, testCampaign.getTempCrewPool(role),
                "Pool should be filled to match unit crew needs for " + role +
                " (fullCrew=" + fullCrewSize + " - activeCrew=" + activeCrew + ")");
        }

        /**
         * Tests that {@link Campaign#fillTempCrewPoolForRole(PersonnelRole)} does NOT count units WITHOUT any crew.
         * Units must have at least 1 real crew member to be able to use temp crew.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testFillTempCrewPoolIgnoresUnitsWithoutCrew(PersonnelRole role) {
            // Arrange - Enable blob crew for this role
            enableBlobCrewForRole(role);

            // Create a mock unit with NO crew (cannot use temp crew)
            Unit mockUnitWithoutCrew = createMockUnitForRole(role, false);
            testCampaign.importUnit(mockUnitWithoutCrew);

            // Start with empty pool
            testCampaign.setTempCrewPool(role, 0);

            // Act - Fill the pool
            testCampaign.fillTempCrewPoolForRole(role);

            // Assert - Pool should remain 0 because unit has no crew
            assertEquals(0, testCampaign.getTempCrewPool(role),
                "Pool should not be filled for units without any crew for " + role);
        }

        /**
         * Tests {@link Campaign#getTempCrewInUse(PersonnelRole)}.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testGetTempCrewInUse(PersonnelRole role) {
            // Arrange
            Unit mockUnit = createMockUnitForRole(role);
            mockUnit.setTempCrew(role, 3);
            testCampaign.importUnit(mockUnit);

            // Act
            int inUse = testCampaign.getTempCrewInUse(role);

            // Assert
            assertEquals(3, inUse);
        }

        /**
         * Tests {@link Campaign#getAvailableTempCrewPool(PersonnelRole)}.
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testGetAvailableTempCrewPool(PersonnelRole role) {
            // Arrange
            testCampaign.setTempCrewPool(role, 10);
            Unit mockUnit = createMockUnitForRole(role);
            mockUnit.setTempCrew(role, 3);
            testCampaign.importUnit(mockUnit);

            // Act
            int available = testCampaign.getAvailableTempCrewPool(role);

            // Assert
            assertEquals(7, available);
        }

        /**
         * Tests that {@link Campaign#getAvailableTempCrewPool(PersonnelRole)} never returns negative values.
         */
        @Test
        void testAvailablePoolNeverGoesNegative() {
            // Arrange
            PersonnelRole testRole = PersonnelRole.SOLDIER;
            testCampaign.setTempCrewPool(testRole, 5);

            Unit mockUnit = createMockUnitForRole(testRole);
            mockUnit.setTempCrew(testRole, 10);
            testCampaign.importUnit(mockUnit);

            // Act
            int available = testCampaign.getAvailableTempCrewPool(testRole);

            // Assert - Available should never be negative
            assertEquals(0, available, "Available pool should not go negative");
        }

        /**
         * Tests that {@link Campaign#clearBlobCrewForRole(PersonnelRole)} only affects the specified role.
         */
        @Test
        void testClearBlobCrewForRoleDoesNotAffectOtherRoles() {
            // Arrange
            testCampaign.setTempCrewPool(PersonnelRole.SOLDIER, 10);
            testCampaign.setTempCrewPool(PersonnelRole.BATTLE_ARMOUR, 8);

            // Act
            testCampaign.clearBlobCrewForRole(PersonnelRole.SOLDIER);

            // Assert
            assertEquals(0, testCampaign.getTempCrewPool(PersonnelRole.SOLDIER));
            assertEquals(8, testCampaign.getTempCrewPool(PersonnelRole.BATTLE_ARMOUR));
        }
    }
    // endregion Nested Test Classes for Temp Crew
}
