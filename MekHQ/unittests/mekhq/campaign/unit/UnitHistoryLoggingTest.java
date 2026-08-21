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
package mekhq.campaign.unit;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.log.AssignmentLogEntry;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.UnitLogger;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests the automatic unit history entries appended by {@link UnitLogger}, covering which events do and do not become
 * part of a unit's permanent record.
 */
class UnitHistoryLoggingTest {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LogEntries";
    private static final LocalDate DATE = LocalDate.of(3067, 1, 1);
    private static final String UNIT_NAME = "Locust LCT-1V";

    private Campaign campaign;
    private Unit unit;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        when(campaign.getLocalDate()).thenReturn(DATE);

        unit = spy(new Unit(mock(Entity.class), campaign));
        when(unit.getId()).thenReturn(UUID.randomUUID());
        doNothing().when(unit).resetPilotAndEntity();
        doReturn(UNIT_NAME).when(unit).getName();

        // with no TOE formation, a removal writes only the unit-level entry to the person's assignment log
        when(campaign.getPlayerForce().getFormationFor(unit)).thenReturn(null);
    }

    private Person mockPerson(String fullName) {
        Person person = mock(Person.class);
        UUID id = UUID.randomUUID();
        when(person.getId()).thenReturn(id);
        when(person.getFullName()).thenReturn(fullName);
        when(campaign.getPlayerForce().getHumanResources().getPerson(eq(id))).thenReturn(person);
        return person;
    }

    private static List<String> descriptions(List<LogEntry> log) {
        return log.stream().map(LogEntry::getDesc).toList();
    }

    @Test
    void assigningAUnitToAScenarioDoesNotRecordADeployment() {
        unit.setScenarioId(42);

        assertTrue(unit.getDeploymentLog().isEmpty(),
              "assigning a unit to a scenario is only a plan and must not be recorded as a deployment");
    }

    @Test
    void cancelingAScenarioAssignmentLeavesNoDeploymentBehind() {
        unit.setScenarioId(42);
        unit.setScenarioId(Scenario.S_DEFAULT_ID);

        assertTrue(unit.getDeploymentLog().isEmpty(),
              "an assignment that was undone before play must leave no deployment history");
    }

    @Test
    void deploymentIsRecordedWhenTheUnitActuallyParticipates() {
        UnitLogger.deployed(unit, DATE, "Battle of Tukayyid");

        assertEquals(List.of(getFormattedTextAt(RESOURCE_BUNDLE, "unitDeployed.text", "Battle of Tukayyid")),
              descriptions(unit.getDeploymentLog()));
    }

    @Test
    void crewDepartureIsRecordedAlongsideTheAssignment() {
        Person driver = mockPerson("Natasha Kerensky");
        when(driver.getUnit()).thenReturn(unit);

        unit.addDriver(driver);
        // log = false covers a transfer, where the person's own log records the move but the unit still lost its crew
        unit.remove(driver, false);

        assertEquals(List.of(getFormattedTextAt(RESOURCE_BUNDLE, "unitAssignedCrew.text", "Natasha Kerensky"),
                    getFormattedTextAt(RESOURCE_BUNDLE, "unitRemovedCrew.text", "Natasha Kerensky")),
              descriptions(unit.getCrewLog()));
    }

    @Test
    void techsAreRecordedAsTechniciansRatherThanAsCrew() {
        Person tech = mockPerson("Tyra Miraborg");

        unit.setTech(tech);
        unit.removeTech();

        assertEquals(List.of(getFormattedTextAt(RESOURCE_BUNDLE, "unitAssignedTech.text", "Tyra Miraborg"),
                    getFormattedTextAt(RESOURCE_BUNDLE, "unitRemovedTech.text", "Tyra Miraborg")),
              descriptions(unit.getCrewLog()),
              "a technician is not crew, so must not be logged as a crew assignment");
    }

    /**
     * Returns the descriptions of every assignment log entry added to the given person.
     */
    private static List<String> assignmentLog(Person person) {
        ArgumentCaptor<AssignmentLogEntry> captor = ArgumentCaptor.forClass(AssignmentLogEntry.class);
        verify(person, atLeast(0)).addAssignmentLogEntry(captor.capture());
        return captor.getAllValues().stream().map(LogEntry::getDesc).toList();
    }

    @Test
    void aTechsOwnLogRecordsThatTheyStoppedMaintainingTheUnit() {
        Person tech = mockPerson("Tyra Miraborg");

        unit.setTech(tech);
        unit.removeTech();

        assertEquals(List.of(getFormattedTextAt(RESOURCE_BUNDLE, "assignedTo.text", UNIT_NAME),
                    getFormattedTextAt(RESOURCE_BUNDLE, "removedFrom.text", UNIT_NAME)),
              assignmentLog(tech));
    }

    @Test
    void removingATechAsAPersonDoesNotLogTheirDepartureTwice() {
        Person tech = mockPerson("Tyra Miraborg");

        unit.setTech(tech);
        // Unit.remove already logs the person's side of the move, so removeTech must not log it as well
        unit.remove(tech, true);

        assertEquals(List.of(getFormattedTextAt(RESOURCE_BUNDLE, "assignedTo.text", UNIT_NAME),
                    getFormattedTextAt(RESOURCE_BUNDLE, "removedFrom.text", UNIT_NAME)),
              assignmentLog(tech));
    }

    @Test
    void transferringATechAwayLeavesTheirAssignmentLogToTheReceivingUnit() {
        Person tech = mockPerson("Tyra Miraborg");

        unit.setTech(tech);
        unit.remove(tech, false);

        assertEquals(List.of(getFormattedTextAt(RESOURCE_BUNDLE, "assignedTo.text", UNIT_NAME)),
              assignmentLog(tech),
              "a transfer is recorded by the receiving unit, so no removal belongs in the person's log");
        assertTrue(descriptions(unit.getCrewLog())
                         .contains(getFormattedTextAt(RESOURCE_BUNDLE, "unitRemovedTech.text", "Tyra Miraborg")),
              "the unit still lost its technician, so its own log must record the departure");
    }

    @Test
    void aKillWithoutANamedCommanderIsStillRecorded() {
        UnitLogger.scoredKill(unit, DATE, "Atlas AS7-D", null);

        assertEquals(List.of(getFormattedTextAt(RESOURCE_BUNDLE, "unitScoredKill.text", "Atlas AS7-D")),
              descriptions(unit.getKillLog()));
    }

    @Test
    void aKillWithANamedCommanderNamesThem() {
        UnitLogger.scoredKill(unit, DATE, "Atlas AS7-D", "Natasha Kerensky");

        assertEquals(List.of(getFormattedTextAt(RESOURCE_BUNDLE,
              "unitScoredKillCommandedBy.text",
              "Atlas AS7-D",
              "Natasha Kerensky")), descriptions(unit.getKillLog()));
    }

    @Test
    void everyAcquisitionTypeResolvesToItsOwnMessage() {
        Set<String> messages = new HashSet<>();

        for (UnitAcquisitionType acquisitionType : UnitAcquisitionType.values()) {
            Unit acquired = new Unit(mock(Entity.class), campaign);
            UnitLogger.acquired(acquired, DATE, acquisitionType);

            assertEquals(1, acquired.getUnitLog().size(), acquisitionType + " must record exactly one entry");

            String message = acquired.getUnitLog().get(0).getDesc();
            assertTrue(isResourceKeyValid(message), acquisitionType + " has no message for key " +
                                                          acquisitionType.getResourceKey());
            assertTrue(messages.add(message), acquisitionType + " must not reuse another acquisition type's message");
        }
    }
}
