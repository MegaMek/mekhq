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
package mekhq.campaign.unit.crewAssignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;

import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Reason;
import mekhq.campaign.unit.crewAssignment.CrewAssignmentService.CommitReport;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Applying unassignments, tech changes and temporary crew changes with {@link CrewAssignmentService}. */
class CrewAssignmentServiceTechTempCrewTest {
    private static final LocalDate TODAY = LocalDate.of(3067, 4, 12);

    private final CrewFixture fixture = new CrewFixture();
    private final AssignmentPlan plan = new AssignmentPlan(false, false);

    private CrewFixture.CrewUnit mek(String name) {
        return fixture.soloPilotUnit(mock(Mek.class), name);
    }

    @Test
    void unassigningIsAlwaysLoggedAsARemoval() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        CrewFixture.CrewUnit home = mek("Home").with(pilot, SeatType.PILOT);
        plan.unassign(List.of(pilot));

        // Even with transfers on, leaving a unit altogether is not a transfer
        CommitReport report = CrewAssignmentService.commit(plan, true, TODAY);

        assertEquals(1, report.applied().size());
        assertNull(pilot.getUnit());
        assertTrue(home.drivers.isEmpty());
        verify(home.unit).remove(pilot, true);
    }

    @Test
    void aNewTechReplacesTheOldOne() {
        Person oldTech = fixture.tech("Old", 480);
        Person newTech = fixture.tech("New", 480);
        CrewFixture.CrewUnit unit = mek("Mek").maintainedBy(oldTech);
        plan.stageTech(newTech, unit.unit);

        CommitReport report = CrewAssignmentService.commit(plan, false, TODAY);

        assertEquals(1, report.techsApplied().size());
        assertEquals(newTech, unit.tech);
        verify(unit.unit).remove(oldTech, true);
        verify(unit.unit).setTech(newTech);
    }

    @Test
    void aRefusedTechLeavesTheUnitWithItsOldTech() {
        Person oldTech = fixture.tech("Old", 480);
        Person newTech = fixture.tech("New", 480);
        CrewFixture.CrewUnit unit = mek("Mek").maintainedBy(oldTech);
        plan.stageTech(newTech, unit.unit);
        unit.refuse = newTech;

        CommitReport report = CrewAssignmentService.commit(plan, false, TODAY);

        assertEquals(Reason.REFUSED, report.techsSkipped().get(0).reason());
        assertEquals(oldTech, unit.tech);
    }

    @Test
    void leavingAUnitWithoutATechOnlyRemovesTheOldOne() {
        Person tech = fixture.tech("Tech", 480);
        CrewFixture.CrewUnit unit = mek("Mek").maintainedBy(tech);
        plan.unassignTechs(List.of(unit.unit));

        CrewAssignmentService.commit(plan, false, TODAY);

        assertNull(unit.tech);
        assertTrue(tech.getTechUnits().isEmpty());
    }

    @Test
    void tempCrewAreSetAfterDeparturesAndBeforeArrivals() {
        Person leaving = fixture.person("Leaving", PersonnelRole.SOLDIER);
        Person arriving = fixture.person("Arriving", PersonnelRole.SOLDIER);
        CrewFixture.CrewUnit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 10)
                                             .with(leaving, SeatType.SOLDIER)
                                             .withTempCrew(PersonnelRole.SOLDIER, 0);
        Unit other = fixture.infantry(mock(Infantry.class), "Other", true, 10).unit;
        plan.stage(List.of(leaving), List.of(other));
        plan.stage(List.of(arriving), List.of(platoon.unit));
        plan.fillTempCrew(List.of(platoon.unit), role -> 20);

        CommitReport report = CrewAssignmentService.commit(plan, false, TODAY);

        assertEquals(1, report.tempCrewApplied().size());
        assertEquals(9, platoon.tempCrew.get(PersonnelRole.SOLDIER));
        InOrder order = inOrder(platoon.unit);
        order.verify(platoon.unit).remove(leaving, true);
        order.verify(platoon.unit).setTempCrew(PersonnelRole.SOLDIER, 9);
        order.verify(platoon.unit).addPilotOrSoldier(arriving, null, false);
    }

    @Test
    void removingTempCrewClearsTheirRoles() {
        CrewFixture.CrewUnit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 10)
                                             .withTempCrew(PersonnelRole.SOLDIER, 6);
        plan.removeTempCrew(List.of(platoon.unit));

        CrewAssignmentService.commit(plan, false, TODAY);

        assertTrue(platoon.tempCrew.isEmpty());
    }

    @Test
    void tempCrewChangedSinceStagingAreSkipped() {
        CrewFixture.CrewUnit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 10)
                                             .withTempCrew(PersonnelRole.SOLDIER, 6);
        plan.removeTempCrew(List.of(platoon.unit));
        platoon.tempCrew.put(PersonnelRole.SOLDIER, 4);

        CommitReport report = CrewAssignmentService.commit(plan, false, TODAY);

        assertEquals(Reason.OUT_OF_DATE, report.tempCrewSkipped().get(0).reason());
        assertEquals(4, platoon.tempCrew.get(PersonnelRole.SOLDIER));
    }

    @Test
    void theReportCountsEveryKindOfChange() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        Person tech = fixture.tech("Tech", 480);
        Unit mekUnit = mek("Mek").unit;
        Unit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 10)
                             .withTempCrew(PersonnelRole.SOLDIER, 3).unit;
        plan.stage(pilot, mekUnit, SeatType.PILOT);
        plan.stageTech(tech, mekUnit);
        plan.removeTempCrew(List.of(platoon));

        CommitReport report = CrewAssignmentService.commit(plan, false, TODAY);

        assertEquals(3, report.appliedCount());
        assertEquals(0, report.skippedCount());
        assertTrue(plan.isEmpty());
    }
}
