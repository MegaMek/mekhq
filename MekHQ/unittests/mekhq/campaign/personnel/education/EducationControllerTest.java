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
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.Hangar;
import mekhq.campaign.JumpPath;
import mekhq.campaign.Warehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.location.AcademyCampusLocation;
import mekhq.campaign.location.LocationDispatch;
import mekhq.campaign.personnel.Person;
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
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));

        CampaignOptions options = mock(CampaignOptions.class);
        when(options.getNaturalHealingWaitingPeriod()).thenReturn(0);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Hangar hangar = mock(Hangar.class);
        when(campaign.getAllHangar()).thenReturn(hangar);

        Warehouse warehouse = mock(Warehouse.class);
        when(warehouse.getParts()).thenReturn(Collections.emptyList());
        when(campaign.getAllWarehouse()).thenReturn(warehouse);

        when(campaign.getAllFormations()).thenReturn(Collections.emptyList());

        PlanetarySystem currentSystem = mock(PlanetarySystem.class);
        when(currentSystem.getId()).thenReturn("CurrentSystem");
        when(campaign.getCurrentSystem()).thenReturn(currentSystem);

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
            campaign = mock(Campaign.class);
            PlanetarySystem destSystem = mock(PlanetarySystem.class);
            when(campaign.getSystemById("TestSystem")).thenReturn(destSystem);
            when(campaign.getSimplifiedTravelTime(destSystem)).thenReturn(2);
            when(campaign.getOrCreateCampusLocation(any(), any(), any()))
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
            campaign = mock(Campaign.class);
            when(campaign.getSystemById("TestSystem")).thenReturn(destSystem);
            when(campaign.getSimplifiedTravelTime(destSystem)).thenReturn(5);
            when(campaign.getOrCreateCampusLocation(any(), any(), any()))
                  .thenReturn(new AcademyCampusLocation(ACADEMY_SET, ACADEMY_NAME));

            PlanetarySystem currentSystem = mock(PlanetarySystem.class);
            when(currentSystem.getId()).thenReturn("CurrentSystem");
            when(campaign.getCurrentSystem()).thenReturn(currentSystem);
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

    /** Tests for {@link EducationController#enrollPerson} stage assignment. */
    @Nested
    class EnrollPerson {

        Campaign campaign;

        @BeforeEach
        void setUp() {
            campaign = buildMinimalCampaignMock();
            when(campaign.getOrCreateCampusLocation(any(), any(), any()))
                  .thenReturn(new AcademyCampusLocation(ACADEMY_SET, ACADEMY_NAME));
            when(campaign.getOrCreateLocalCampusLocation(any(), any()))
                  .thenReturn(new AcademyCampusLocation(ACADEMY_SET, ACADEMY_NAME));
        }

        @Test
        void homeSchool_setsStageToEducation() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getName()).thenReturn("TestCampaign");

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

            try (MockedStatic<LocationDispatch> mockDispatch = mockStatic(LocationDispatch.class, CALLS_REAL_METHODS)) {
                JumpPath mockPath = mock(JumpPath.class);
                when(mockPath.getTotalTime(any(), anyDouble(), anyBoolean())).thenReturn(10.0);
                CurrentLocation travelLoc = new CurrentLocation(mock(PlanetarySystem.class), 0.5);
                travelLoc.setJumpPath(mockPath);

                mockDispatch.when(() -> LocationDispatch.dispatchToLocation(any(), any(), any()))
                      .thenAnswer(inv -> {
                          Collection<Person> people = inv.getArgument(0);
                          people.forEach(p -> p.setParent(travelLoc));
                          return null;
                      });

                EducationController.enrollPerson(campaign, person, academy, "Galatea", "MERC", 0);
            }

            assertEquals(EducationStage.JOURNEY_TO_CAMPUS, person.getEduEducationStage());
        }

        @Test
        void homeSchool_setsAcademyNameToCampaignName() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getName()).thenReturn("My Campaign");

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

            try (MockedStatic<LocationDispatch> mockDispatch = mockStatic(LocationDispatch.class, CALLS_REAL_METHODS)) {
                JumpPath mockPath = mock(JumpPath.class);
                when(mockPath.getTotalTime(any(), anyDouble(), anyBoolean())).thenReturn(10.0);
                CurrentLocation travelLoc = new CurrentLocation(mock(PlanetarySystem.class), 0.5);
                travelLoc.setJumpPath(mockPath);

                mockDispatch.when(() -> LocationDispatch.dispatchToLocation(any(), any(), any()))
                      .thenAnswer(inv -> {
                          Collection<Person> people = inv.getArgument(0);
                          people.forEach(p -> p.setParent(travelLoc));
                          return null;
                      });

                EducationController.enrollPerson(campaign, person, academy, "Galatea", "MERC", 0);
            }

            assertEquals(10, person.getEduJourneyTime());
        }

        @Test
        void anyAcademy_setsAcademySetOnPerson() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getName()).thenReturn("TestCampaign");

            Person person = buildStudentPerson();
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertEquals(ACADEMY_SET, person.getEduAcademySet());
        }

        @Test
        void anyAcademy_setsAcademyNameInSetOnPerson() {
            Academy academy = buildAcademy(true, false);
            when(campaign.getName()).thenReturn("TestCampaign");

            Person person = buildStudentPerson();
            EducationController.enrollPerson(campaign, person, academy, null, "MERC", 0);

            assertEquals(ACADEMY_NAME, person.getEduAcademyNameInSet());
        }
    }
}
