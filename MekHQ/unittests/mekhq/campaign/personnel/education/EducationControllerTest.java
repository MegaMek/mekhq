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
package mekhq.campaign.personnel.education;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignLocationManager;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.location.AcademyCampusLocation;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.enums.education.EducationStage;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for the stage-transition logic in {@link EducationController}. These tests verify the
 * pre-existing behavior that must be preserved when adding location tracking to the education
 * pipeline.
 */
class EducationControllerTest {

    static final String ACADEMY_SET = "TestSet";
    static final String ACADEMY_NAME = "Test Academy";

    static Academy buildAcademy(boolean isHomeSchool, boolean isLocal) {
        return new Academy(ACADEMY_SET, ACADEMY_NAME, "College",
              false, false, false,
              "Test Academy Description",
              0, false,
              List.of("TestSystem"),
              isLocal, isHomeSchool,
              3025, null, null,
              1000, 365, 5,
              EducationLevel.HIGH_SCHOOL, EducationLevel.COLLEGE,
              18, 35,
              List.of("Test Course"),
              List.of("Test Curriculum"),
              List.of(3025),
              0, 0);
    }

    Campaign buildMinimalCampaignMock() {
        Campaign campaign = mockCampaign();
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));

        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.NATURAL_HEALING_WAITING_PERIOD)).thenReturn(0);
        when(campaign.getCampaignOptions()).thenReturn(options);

        mekhq.campaign.LocalHangar hangar = mock(mekhq.campaign.LocalHangar.class);
        when(campaign.getPlayerForce().getHangar()).thenReturn(hangar);

        LocalWarehouse warehouse = mock(LocalWarehouse.class);
        when(warehouse.getParts()).thenReturn(Collections.emptyList());
        //TODO: This won't work once we support multiple warehouse. Method separated from getWarehouse() for future
        when(campaign.getPlayerForce().getWarehouse()).thenReturn(warehouse);

        when(campaign.getPlayerForce().getAllFormations()).thenReturn(Collections.emptyList());

        PlanetarySystem currentSystem = mock(PlanetarySystem.class);
        when(currentSystem.getId()).thenReturn("CurrentSystem");
        when(campaign.getCurrentSystem()).thenReturn(currentSystem);

        when(campaign.getCampaignLocationManager()).thenReturn(mock(CampaignLocationManager.class));

        PlayerForce playerForce = mock(PlayerForce.class, RETURNS_DEEP_STUBS);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getForceDetachment()).thenReturn(mock(Detachment.class));

        return campaign;
    }

    Person buildStudentPerson() {
        return new Person("Test", "Student", null, "MERC");
    }

    /** Tests for {@link EducationController#processNewDay} with JOURNEY_TO_CAMPUS stage. */
    @Nested
    class JourneyToCampus {

        Academy academy;
        Person person;
        Campaign campaign;

        @BeforeEach
        void setUp() {
            academy = buildAcademy(false, false);
            person = buildStudentPerson();
            person.setEduAcademySet(ACADEMY_SET);
            person.setEduAcademyNameInSet(ACADEMY_NAME);
            person.setEduAcademySystem("TestSystem");
            person.setEduEducationStage(EducationStage.JOURNEY_TO_CAMPUS);
            campaign = mockCampaign();
            PlayerForce playerForce = mock(PlayerForce.class, RETURNS_DEEP_STUBS);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getForceDetachment()).thenReturn(mock(Detachment.class));
            when(campaign.getCampaignLocationManager()).thenReturn(mock(CampaignLocationManager.class));
            PlanetarySystem destSystem = mock(PlanetarySystem.class);
            when(campaign.getSystemById("TestSystem")).thenReturn(destSystem);
            when(campaign.getSimplifiedTravelTime(destSystem)).thenReturn(2);
            when(campaign.getCampaignLocationManager().getOrCreateCampusLocation(any(), any(), any(), any()))
                  .thenReturn(new AcademyCampusLocation(ACADEMY_SET, ACADEMY_NAME));
        }

        @Test
        void notYetArrived_stageRemainsJourneyToCampus() {
            person.setEduJourneyTime(5);
            person.setEduDaysOfTravel(0);

            try (MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class)) {
                AcademyFactory factory = mock(AcademyFactory.class);
                mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
                when(factory.getAllSetNames()).thenReturn(List.of(ACADEMY_SET));
                when(factory.getAllAcademiesForSet(ACADEMY_SET)).thenReturn(List.of(academy));

                EducationController.processNewDay(campaign, person, false);
            }

            assertEquals(EducationStage.JOURNEY_TO_CAMPUS, person.getEduEducationStage());
        }

        @Test
        void notYetArrived_daysOfTravelIncremented() {
            person.setEduJourneyTime(5);
            person.setEduDaysOfTravel(0);

            try (MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class)) {
                AcademyFactory factory = mock(AcademyFactory.class);
                mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
                when(factory.getAllSetNames()).thenReturn(List.of(ACADEMY_SET));
                when(factory.getAllAcademiesForSet(ACADEMY_SET)).thenReturn(List.of(academy));

                EducationController.processNewDay(campaign, person, false);
            }

            assertEquals(1, person.getEduDaysOfTravel());
        }

        @Test
        void onArrival_stageTransitionsToEducation() {
            person.setEduJourneyTime(2);
            person.setEduDaysOfTravel(1);

            try (MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class)) {
                AcademyFactory factory = mock(AcademyFactory.class);
                mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
                when(factory.getAllSetNames()).thenReturn(List.of(ACADEMY_SET));
                when(factory.getAllAcademiesForSet(ACADEMY_SET)).thenReturn(List.of(academy));

                EducationController.processNewDay(campaign, person, false);
            }

            assertEquals(EducationStage.EDUCATION, person.getEduEducationStage());
        }

        @Test
        void alreadyArrived_stageTransitionsToEducation() {
            person.setEduJourneyTime(3);
            person.setEduDaysOfTravel(3);

            try (MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class)) {
                AcademyFactory factory = mock(AcademyFactory.class);
                mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
                when(factory.getAllSetNames()).thenReturn(List.of(ACADEMY_SET));
                when(factory.getAllAcademiesForSet(ACADEMY_SET)).thenReturn(List.of(academy));

                EducationController.processNewDay(campaign, person, false);
            }

            assertEquals(EducationStage.EDUCATION, person.getEduEducationStage());
        }

        private void runNewDay() {
            try (MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class)) {
                AcademyFactory factory = mock(AcademyFactory.class);
                mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
                when(factory.getAllSetNames()).thenReturn(List.of(ACADEMY_SET));
                when(factory.getAllAcademiesForSet(ACADEMY_SET)).thenReturn(List.of(academy));

                EducationController.processNewDay(campaign, person, false);
            }
        }

        private Detachment stubHomeDetachment() {
            when(campaign.getLocalDate()).thenReturn(LocalDate.of(3053, 6, 26));
            Detachment homeDetachment = new Detachment();
            homeDetachment.setParent(new CurrentLocation(mock(PlanetarySystem.class), 0));
            when(campaign.getPlayerForce().getForceDetachment()).thenReturn(homeDetachment);
            return homeDetachment;
        }

        @Test
        void missingAcademySystem_landsAtAcademyLocationSystem() {
            person.setEduAcademySystem(null);
            person.setEduAcademyName("Test Academy (TestSystem)");
            person.setEduJourneyTime(73);
            person.setEduDaysOfTravel(947);

            runNewDay();

            verify(campaign.getCampaignLocationManager())
                  .getOrCreateCampusLocation(campaign, ACADEMY_SET, ACADEMY_NAME, "TestSystem");
            assertEquals(EducationStage.EDUCATION, person.getEduEducationStage());
        }

        @Test
        void missingAcademySystem_localAcademy_landsAtCampaignSystem() {
            academy = buildAcademy(false, true);
            person.setEduAcademySystem(null);
            person.setEduAcademyName("Test Academy");
            person.setEduJourneyTime(2);
            person.setEduDaysOfTravel(482);
            PlanetarySystem currentSystem = mock(PlanetarySystem.class);
            when(currentSystem.getId()).thenReturn("CurrentSystem");
            when(campaign.getCurrentSystem()).thenReturn(currentSystem);

            runNewDay();

            verify(campaign.getCampaignLocationManager())
                  .getOrCreateCampusLocation(campaign, ACADEMY_SET, ACADEMY_NAME, "CurrentSystem");
            assertEquals(EducationStage.EDUCATION, person.getEduEducationStage());
        }

        @Test
        void unresolvableAcademySystem_returnsHomeInsteadOfThrowing() {
            person.setEduAcademySystem(null);
            person.setEduJourneyTime(73);
            person.setEduDaysOfTravel(947);
            person.setStatus(PersonnelStatus.STUDENT);
            Detachment homeDetachment = stubHomeDetachment();

            runNewDay();

            verify(campaign.getCampaignLocationManager(), never())
                  .getOrCreateCampusLocation(any(), any(), any(), any());
            assertSame(homeDetachment.getPersonnel(), person.getParentLocation());
            assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
        }

        @Test
        void unresolvableCampus_returnsHomeInsteadOfThrowing() {
            person.setEduJourneyTime(2);
            person.setEduDaysOfTravel(2);
            when(campaign.getCampaignLocationManager().getOrCreateCampusLocation(any(), any(), any(), any()))
                  .thenReturn(null);
            person.setStatus(PersonnelStatus.STUDENT);
            Detachment homeDetachment = stubHomeDetachment();

            runNewDay();

            assertSame(homeDetachment.getPersonnel(), person.getParentLocation());
            assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
        }
    }

    /** Tests for {@link EducationController#processNewDay} with GRADUATING stage. */
    @Nested
    class BeginJourneyHome {

        Academy academy;
        Person person;
        Campaign campaign;
        PlanetarySystem destSystem;

        @BeforeEach
        void setUp() {
            academy = buildAcademy(false, false);
            person = buildStudentPerson();
            person.setEduAcademySet(ACADEMY_SET);
            person.setEduAcademyNameInSet(ACADEMY_NAME);
            person.setEduAcademySystem("TestSystem");

            destSystem = mock(PlanetarySystem.class);
            campaign = mockCampaign();
            PlayerForce playerForce = mock(PlayerForce.class, RETURNS_DEEP_STUBS);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            when(playerForce.getForceDetachment()).thenReturn(mock(Detachment.class));
            when(campaign.getCampaignLocationManager()).thenReturn(mock(CampaignLocationManager.class));
            when(campaign.getSystemById("TestSystem")).thenReturn(destSystem);
            when(campaign.getCampaignLocationManager().getOrCreateCampusLocation(any(), any(), any(), any()))
                  .thenReturn(new AcademyCampusLocation(ACADEMY_SET, ACADEMY_NAME));

            PlanetarySystem currentSystem = mock(PlanetarySystem.class);
            when(currentSystem.getId()).thenReturn("CurrentSystem");
            when(campaign.getCurrentSystem()).thenReturn(currentSystem);
            when(campaign.getSimplifiedTravelTime(destSystem, currentSystem)).thenReturn(5);
        }

        @Test
        void graduating_nonHomeSchool_stageTransitionsToJourneyFromCampus() {
            person.setEduEducationStage(EducationStage.GRADUATING);

            try (MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class)) {
                AcademyFactory factory = mock(AcademyFactory.class);
                mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
                when(factory.getAllSetNames()).thenReturn(List.of(ACADEMY_SET));
                when(factory.getAllAcademiesForSet(ACADEMY_SET)).thenReturn(List.of(academy));

                EducationController.processNewDay(campaign, person, false);
            }

            assertEquals(EducationStage.JOURNEY_FROM_CAMPUS, person.getEduEducationStage());
        }

        @Test
        void droppingOut_nonHomeSchool_stageTransitionsToJourneyFromCampus() {
            person.setEduEducationStage(EducationStage.DROPPING_OUT);

            try (MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class)) {
                AcademyFactory factory = mock(AcademyFactory.class);
                mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
                when(factory.getAllSetNames()).thenReturn(List.of(ACADEMY_SET));
                when(factory.getAllAcademiesForSet(ACADEMY_SET)).thenReturn(List.of(academy));

                EducationController.processNewDay(campaign, person, false);
            }

            assertEquals(EducationStage.JOURNEY_FROM_CAMPUS, person.getEduEducationStage());
        }

        @Test
        void graduating_nonHomeSchool_journeyTimeSetFromTravelTime() {
            person.setEduEducationStage(EducationStage.GRADUATING);

            try (MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class)) {
                AcademyFactory factory = mock(AcademyFactory.class);
                mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
                when(factory.getAllSetNames()).thenReturn(List.of(ACADEMY_SET));
                when(factory.getAllAcademiesForSet(ACADEMY_SET)).thenReturn(List.of(academy));

                EducationController.processNewDay(campaign, person, false);
            }

            assertEquals(5, person.getEduJourneyTime());
        }

        @Test
        void graduating_nonHomeSchool_daysOfTravelResetToZero() {
            person.setEduEducationStage(EducationStage.GRADUATING);
            person.setEduDaysOfTravel(10);

            try (MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class)) {
                AcademyFactory factory = mock(AcademyFactory.class);
                mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
                when(factory.getAllSetNames()).thenReturn(List.of(ACADEMY_SET));
                when(factory.getAllAcademiesForSet(ACADEMY_SET)).thenReturn(List.of(academy));

                EducationController.processNewDay(campaign, person, false);
            }

            assertEquals(0, person.getEduDaysOfTravel());
        }
    }

    /** Tests for resuming {@link EducationStage#JOURNEY_FROM_CAMPUS} travel stranded without a jump path. */
    @Nested
    class StrandedJourneyHome {

        Academy academy;
        Person person;
        Campaign campaign;
        Detachment forceDetachment;
        PlanetarySystem academySystem;
        PlanetarySystem campaignSystem;

        @BeforeEach
        void setUp() {
            academy = buildAcademy(false, false);
            person = buildStudentPerson();
            person.setEduAcademySet(ACADEMY_SET);
            person.setEduAcademyNameInSet(ACADEMY_NAME);
            person.setEduAcademySystem("TestSystem");
            person.setEduEducationStage(EducationStage.JOURNEY_FROM_CAMPUS);
            person.setEduJourneyTime(92);
            person.setEduDaysOfTravel(2817);

            campaign = mockCampaign();
            PlayerForce playerForce = mock(PlayerForce.class, RETURNS_DEEP_STUBS);
            when(campaign.getPlayerForce()).thenReturn(playerForce);
            forceDetachment = mock(Detachment.class);
            when(playerForce.getForceDetachment()).thenReturn(forceDetachment);
            when(campaign.getCampaignLocationManager()).thenReturn(mock(CampaignLocationManager.class));
            when(campaign.getLocalDate()).thenReturn(LocalDate.of(3050, 12, 16));

            academySystem = mock(PlanetarySystem.class);
            when(academySystem.getId()).thenReturn("TestSystem");
            when(campaign.getSystemById("TestSystem")).thenReturn(academySystem);
            campaignSystem = mock(PlanetarySystem.class);
            when(campaignSystem.getId()).thenReturn("CurrentSystem");
        }

        private CurrentLocation strandTravelerAt(PlanetarySystem system) {
            CurrentLocation travelNode = new CurrentLocation(system, 28.0);
            person.setParent(travelNode);
            return travelNode;
        }

        private void runNewDay() {
            try (MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class)) {
                AcademyFactory factory = mock(AcademyFactory.class);
                mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
                when(factory.getAllSetNames()).thenReturn(List.of(ACADEMY_SET));
                when(factory.getAllAcademiesForSet(ACADEMY_SET)).thenReturn(List.of(academy));

                EducationController.processNewDay(campaign, person, false);
            }
        }

        @Test
        void strandedInTargetSystem_resumesInSystemTransit() {
            when(campaign.getCurrentSystem()).thenReturn(academySystem);
            CurrentLocation travelNode = strandTravelerAt(academySystem);

            runNewDay();

            assertNotNull(travelNode.getJumpPath());
            assertEquals(List.of(academySystem), travelNode.getJumpPath().getSystems());
            assertEquals(28, person.getEduJourneyTime());
            assertEquals(0, person.getEduDaysOfTravel());
        }

        @Test
        void strandedInOtherSystem_resumesAlongPlannedRoute() {
            when(campaign.getCurrentSystem()).thenReturn(campaignSystem);
            CurrentLocation travelNode = strandTravelerAt(academySystem);
            JumpPath route = new JumpPath();
            route.addSystem(academySystem);
            route.addSystem(campaignSystem);

            try (MockedStatic<LocationUtils> mockUtils = mockStatic(LocationUtils.class, CALLS_REAL_METHODS)) {
                mockUtils.when(() -> LocationUtils.planJumpPath(academySystem, campaignSystem, campaign))
                      .thenReturn(route);
                mockUtils.when(() -> LocationUtils.computeJourneyDays(any(JumpPath.class), any(LocalDate.class),
                      anyDouble())).thenReturn(40);

                runNewDay();
            }

            assertSame(route, travelNode.getJumpPath());
            assertEquals(40, person.getEduJourneyTime());
            assertEquals(0, person.getEduDaysOfTravel());
        }

        @Test
        void strandedWithNoRoute_arrivesHomeImmediately() {
            when(campaign.getCurrentSystem()).thenReturn(campaignSystem);
            CurrentLocation travelNode = strandTravelerAt(academySystem);
            Detachment homeDetachment = new Detachment();
            homeDetachment.setParent(new CurrentLocation(campaignSystem, 0));
            when(forceDetachment.getPersonnel()).thenReturn(homeDetachment.getPersonnel());
            CampaignLocationManager locationManager = campaign.getCampaignLocationManager();

            try (MockedStatic<LocationUtils> mockUtils = mockStatic(LocationUtils.class, CALLS_REAL_METHODS)) {
                mockUtils.when(() -> LocationUtils.planJumpPath(academySystem, campaignSystem, campaign))
                      .thenReturn(null);

                runNewDay();
            }

            assertSame(homeDetachment.getPersonnel(), person.getParentLocation());
            assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
            assertNull(travelNode.getJumpPath());
            assertNull(travelNode.getParentLocation());
            verify(locationManager).removeLocation(travelNode);
        }

        @Test
        void mainForceLocationWithoutPath_isLeftUntouched() {
            when(campaign.getCurrentSystem()).thenReturn(campaignSystem);
            CurrentLocation mainForceLocation = strandTravelerAt(academySystem);
            when(forceDetachment.getCurrentLocation()).thenReturn(mainForceLocation);

            runNewDay();

            assertNull(mainForceLocation.getJumpPath());
        }

        private Person addTagAlong(PersonnelStatus status) {
            Person tagAlong = new Person("Test", "TagAlong", null, "MERC");
            tagAlong.setStatus(status);
            person.addEduTagAlong(tagAlong.getId());
            when(campaign.getPlayerForce().getHumanResources().getPerson(tagAlong.getId())).thenReturn(tagAlong);
            return tagAlong;
        }

        @Test
        void arrivingHome_campFollowerTagAlongIsNotEmployed() {
            when(campaign.getCurrentSystem()).thenReturn(campaignSystem);
            strandTravelerAt(academySystem);
            Detachment homeDetachment = new Detachment();
            homeDetachment.setParent(new CurrentLocation(campaignSystem, 0));
            when(forceDetachment.getPersonnel()).thenReturn(homeDetachment.getPersonnel());

            person.setStatus(PersonnelStatus.STUDENT);
            Person campFollower = addTagAlong(PersonnelStatus.CAMP_FOLLOWER);
            Person onLeave = addTagAlong(PersonnelStatus.ON_LEAVE);

            try (MockedStatic<LocationUtils> mockUtils = mockStatic(LocationUtils.class, CALLS_REAL_METHODS)) {
                mockUtils.when(() -> LocationUtils.planJumpPath(academySystem, campaignSystem, campaign))
                      .thenReturn(null);

                runNewDay();
            }

            assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
            assertEquals(PersonnelStatus.CAMP_FOLLOWER, campFollower.getStatus());
            assertEquals(PersonnelStatus.ACTIVE, onLeave.getStatus());
            assertSame(homeDetachment.getPersonnel(), campFollower.getParentLocation());
            assertSame(homeDetachment.getPersonnel(), onLeave.getParentLocation());
            assertTrue(person.getEduTagAlongs().isEmpty());
        }

        private PlayerBase addOriginBase(PlanetarySystem baseSystem) {
            PlayerBase originBase = mock(PlayerBase.class);
            UUID originBaseId = UUID.randomUUID();
            when(originBase.getId()).thenReturn(originBaseId);
            when(originBase.getCurrentSystem()).thenReturn(baseSystem);
            Detachment baseDetachment = new Detachment();
            baseDetachment.setParent(new CurrentLocation(baseSystem, 0));
            when(originBase.getPersonnel()).thenReturn(baseDetachment.getPersonnel());
            person.setEduOriginBaseId(originBaseId);
            return originBase;
        }

        @Test
        void arrivingHome_returnsToOriginBase() {
            when(campaign.getCurrentSystem()).thenReturn(campaignSystem);
            strandTravelerAt(academySystem);
            PlanetarySystem baseSystem = mock(PlanetarySystem.class);
            when(baseSystem.getId()).thenReturn("BaseSystem");
            PlayerBase originBase = addOriginBase(baseSystem);
            when(campaign.getCampaignLocationManager().getPlayerBaseById(originBase.getId())).thenReturn(originBase);
            Detachment homeDetachment = new Detachment();
            homeDetachment.setParent(new CurrentLocation(campaignSystem, 0));
            when(forceDetachment.getPersonnel()).thenReturn(homeDetachment.getPersonnel());

            person.setStatus(PersonnelStatus.STUDENT);
            Person campFollower = addTagAlong(PersonnelStatus.CAMP_FOLLOWER);

            try (MockedStatic<LocationUtils> mockUtils = mockStatic(LocationUtils.class, CALLS_REAL_METHODS)) {
                mockUtils.when(() -> LocationUtils.planJumpPath(academySystem, baseSystem, campaign))
                      .thenReturn(null);

                runNewDay();
            }

            assertSame(originBase.getPersonnel(), person.getParentLocation());
            assertSame(originBase.getPersonnel(), campFollower.getParentLocation());
            assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
            assertNull(person.getEduOriginBaseId());
        }

        @Test
        void arrivingHome_originBaseGone_returnsToMainForce() {
            when(campaign.getCurrentSystem()).thenReturn(campaignSystem);
            strandTravelerAt(academySystem);
            PlanetarySystem baseSystem = mock(PlanetarySystem.class);
            when(baseSystem.getId()).thenReturn("BaseSystem");
            PlayerBase originBase = addOriginBase(baseSystem);
            when(campaign.getCampaignLocationManager().getPlayerBaseById(originBase.getId())).thenReturn(null);
            Detachment homeDetachment = new Detachment();
            homeDetachment.setParent(new CurrentLocation(campaignSystem, 0));
            when(forceDetachment.getPersonnel()).thenReturn(homeDetachment.getPersonnel());

            person.setStatus(PersonnelStatus.STUDENT);

            try (MockedStatic<LocationUtils> mockUtils = mockStatic(LocationUtils.class, CALLS_REAL_METHODS)) {
                mockUtils.when(() -> LocationUtils.planJumpPath(academySystem, campaignSystem, campaign))
                      .thenReturn(null);

                runNewDay();
            }

            assertSame(homeDetachment.getPersonnel(), person.getParentLocation());
            assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
            assertNull(person.getEduOriginBaseId());
        }
    }

    /** Tests for {@link EducationController#enrollPerson} stage assignment. */
    @Nested
    class EnrollPerson {

        Campaign campaign;

        @BeforeEach
        void setUp() {
            campaign = buildMinimalCampaignMock();
            when(campaign.getCampaignLocationManager().getOrCreateCampusLocation(any(), any(), any(), any()))
                  .thenReturn(new AcademyCampusLocation(ACADEMY_SET, ACADEMY_NAME));
            when(campaign.getCampaignLocationManager().getOrCreateLocalCampusLocation(any(), any(), any()))
                  .thenReturn(new AcademyCampusLocation(ACADEMY_SET, ACADEMY_NAME));
            when(campaign.getCampaignLocationManager().getOrCreateCampusUnderLocation(any(), any(), any()))
                  .thenReturn(new AcademyCampusLocation(ACADEMY_SET, ACADEMY_NAME));
        }

        @Test
        void homeSchool_setsStageToEducation() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getPlayerForce().getName()).thenReturn("TestCampaign");

            Person person = buildStudentPerson();
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertEquals(EducationStage.EDUCATION, person.getEduEducationStage());
        }

        @Test
        void nonHomeSchoolLocal_setsStageToJourneyToCampus() {
            Academy academy = buildAcademy(false, true);
            PlanetarySystem currentSystem = mock(PlanetarySystem.class);
            when(currentSystem.getId()).thenReturn("CurrentSystem");
            when(campaign.getCurrentSystem()).thenReturn(currentSystem);

            Person person = buildStudentPerson();
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertEquals(EducationStage.JOURNEY_TO_CAMPUS, person.getEduEducationStage());
        }

        @Test
        void nonHomeSchoolRemote_setsStageToJourneyToCampus() {
            Academy academy = buildAcademy(false, false);
            PlanetarySystem destSystem = mock(PlanetarySystem.class);
            when(destSystem.getName(any())).thenReturn("Galatea");
            when(campaign.getSystemById("Galatea")).thenReturn(destSystem);

            Person person = buildStudentPerson();

            // Travel is now queued rather than dispatched; the stage is set regardless of the planned route.
            EducationController.enrollPerson(campaign, person, academy, "Galatea", "MERC", 0);

            assertEquals(EducationStage.JOURNEY_TO_CAMPUS, person.getEduEducationStage());
        }

        @Test
        void homeSchool_setsAcademyNameToCampaignName() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getPlayerForce().getName()).thenReturn("My Campaign");

            Person person = buildStudentPerson();
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertEquals("My Campaign", person.getEduAcademyName());
        }

        @Test
        void nonHomeSchoolLocal_setsJourneyTimeToTwo() {
            Academy academy = buildAcademy(false, true);
            PlanetarySystem currentSystem = mock(PlanetarySystem.class);
            when(currentSystem.getId()).thenReturn("CurrentSystem");
            when(campaign.getCurrentSystem()).thenReturn(currentSystem);

            Person person = buildStudentPerson();
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertEquals(2, person.getEduJourneyTime());
        }

        @Test
        void nonHomeSchoolRemote_setsJourneyTimeFromTravelTime() {
            Academy academy = buildAcademy(false, false);
            PlanetarySystem destSystem = mock(PlanetarySystem.class);
            when(destSystem.getName(any())).thenReturn("Galatea");
            when(campaign.getSystemById("Galatea")).thenReturn(destSystem);

            Person person = buildStudentPerson();

            try (MockedStatic<LocationUtils> mockUtils = mockStatic(LocationUtils.class, CALLS_REAL_METHODS)) {
                JumpPath mockPath = mock(JumpPath.class);
                when(mockPath.getTotalTime(any(), anyDouble(), anyBoolean())).thenReturn(10.0);
                mockUtils.when(() -> LocationUtils.planJumpPath(any(), any(), any()))
                      .thenReturn(mockPath);

                EducationController.enrollPerson(campaign, person, academy, "Galatea", "MERC", 0);
            }

            assertEquals(10, person.getEduJourneyTime());
        }

        @Test
        void nonHomeSchoolRemote_unresolvableCampus_abortsEnrollment() {
            Academy academy = buildAcademy(false, false);
            when(campaign.getCampaignLocationManager().getOrCreateCampusLocation(any(), any(), any(), any()))
                  .thenReturn(null);

            Person person = buildStudentPerson();
            EducationController.enrollPerson(campaign, person, academy, "Galatea", "MERC", 0);

            assertNotEquals(EducationStage.JOURNEY_TO_CAMPUS, person.getEduEducationStage());
            verify(campaign.getCampaignLocationManager(), never()).queueTravel(any(), any());
        }

        @Test
        void anyAcademy_setsAcademySetOnPerson() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getPlayerForce().getName()).thenReturn("TestCampaign");

            Person person = buildStudentPerson();
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertEquals(ACADEMY_SET, person.getEduAcademySet());
        }

        @Test
        void anyAcademy_setsAcademyNameInSetOnPerson() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getPlayerForce().getName()).thenReturn("TestCampaign");

            Person person = buildStudentPerson();
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertEquals(ACADEMY_NAME, person.getEduAcademyNameInSet());
        }

        @Test
        void enrollingFromPlayerBase_remembersOriginBase() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getPlayerForce().getName()).thenReturn("TestCampaign");
            PlayerBase originBase = mock(PlayerBase.class);
            UUID originBaseId = UUID.randomUUID();
            when(originBase.getId()).thenReturn(originBaseId);

            Person person = buildStudentPerson();
            try (MockedStatic<LocationUtils> mockUtils = mockStatic(LocationUtils.class, CALLS_REAL_METHODS)) {
                mockUtils.when(() -> LocationUtils.findEffectiveBase(person)).thenReturn(originBase);

                EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);
            }

            assertEquals(originBaseId, person.getEduOriginBaseId());
        }

        @Test
        void enrollingFromMainForce_hasNoOriginBase() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getPlayerForce().getName()).thenReturn("TestCampaign");

            Person person = buildStudentPerson();
            person.setEduOriginBaseId(UUID.randomUUID());
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertNull(person.getEduOriginBaseId());
        }

        private Person addSpouse(Person person, PersonnelStatus status) {
            Person spouse = new Person("Test", "Spouse", null, "MERC");
            spouse.setStatus(status);
            person.getGenealogy().setSpouse(spouse);
            spouse.getGenealogy().setSpouse(person);
            return spouse;
        }

        private Person addChild(Person person, PersonnelStatus status) {
            Person child = new Person("Test", "Child", null, "MERC");
            child.setDateOfBirth(LocalDate.of(3020, 1, 1));
            child.setStatus(status);
            person.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, child);
            return child;
        }

        @Test
        void campFollowerFamily_tagsAlongAndRemainsCampFollowers() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getPlayerForce().getName()).thenReturn("TestCampaign");

            Person person = buildStudentPerson();
            Person spouse = addSpouse(person, PersonnelStatus.CAMP_FOLLOWER);
            Person child = addChild(person, PersonnelStatus.CAMP_FOLLOWER);
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertEquals(List.of(spouse.getId(), child.getId()), person.getEduTagAlongs());
            assertEquals(PersonnelStatus.CAMP_FOLLOWER, spouse.getStatus());
            assertEquals(PersonnelStatus.CAMP_FOLLOWER, child.getStatus());
        }

        @Test
        void dependentFamily_tagsAlongAndGoesOnLeave() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getPlayerForce().getName()).thenReturn("TestCampaign");

            Person person = buildStudentPerson();
            Person spouse = addSpouse(person, PersonnelStatus.ACTIVE);
            spouse.setPrimaryRoleDirect(PersonnelRole.DEPENDENT);
            Person child = addChild(person, PersonnelStatus.ACTIVE);
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertEquals(List.of(spouse.getId(), child.getId()), person.getEduTagAlongs());
            assertEquals(PersonnelStatus.ON_LEAVE, spouse.getStatus());
            assertEquals(PersonnelStatus.ON_LEAVE, child.getStatus());
        }

        @Test
        void employedSpouse_staysBehindWithChildren() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getPlayerForce().getName()).thenReturn("TestCampaign");

            Person person = buildStudentPerson();
            Person spouse = addSpouse(person, PersonnelStatus.ACTIVE);
            Person child = addChild(person, PersonnelStatus.ACTIVE);
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertTrue(person.getEduTagAlongs().isEmpty());
            assertEquals(PersonnelStatus.ACTIVE, spouse.getStatus());
            assertEquals(PersonnelStatus.ACTIVE, child.getStatus());
        }
    }
}
