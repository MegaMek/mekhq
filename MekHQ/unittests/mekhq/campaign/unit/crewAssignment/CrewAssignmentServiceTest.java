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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Reason;
import mekhq.campaign.unit.crewAssignment.CrewAssignmentService.CommitReport;
import org.junit.jupiter.api.Test;

class CrewAssignmentServiceTest {
    private static final LocalDate TODAY = LocalDate.of(3067, 4, 12);

    private final CrewFixture fixture = new CrewFixture();
    private final AssignmentPlan plan = new AssignmentPlan(false, false);

    private CrewFixture.CrewUnit mek(String name) {
        return fixture.soloPilotUnit(mock(Mek.class), name);
    }

    @Test
    void commitSeatsEveryoneAndEmptiesThePlan() {
        CrewFixture.CrewUnit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 3);
        Person first = fixture.person("First", PersonnelRole.SOLDIER);
        Person second = fixture.person("Second", PersonnelRole.SOLDIER);
        plan.stage(List.of(first, second), List.of(platoon.unit));

        CommitReport report = CrewAssignmentService.commit(plan, false, TODAY);

        assertEquals(2, report.applied().size());
        assertTrue(report.skipped().isEmpty());
        assertEquals(platoon.unit, first.getUnit());
        assertEquals(platoon.unit, second.getUnit());
        assertTrue(plan.isEmpty());
        verify(first).setRecruitment(TODAY);
    }

    @Test
    void aChainOfMovesWorksBecauseEveryoneLeavesBeforeAnyoneSits() {
        Person alpha = fixture.person("Alpha", PersonnelRole.MEKWARRIOR);
        Person bravo = fixture.person("Bravo", PersonnelRole.MEKWARRIOR);
        CrewFixture.CrewUnit first = mek("First").with(alpha, SeatType.PILOT);
        CrewFixture.CrewUnit second = mek("Second").with(bravo, SeatType.PILOT);
        CrewFixture.CrewUnit spare = mek("Spare");
        // Bravo moves out to the spare, which lets Alpha take Bravo's old seat
        plan.stage(bravo, spare.unit, SeatType.PILOT);
        plan.stage(alpha, second.unit, SeatType.PILOT);

        CommitReport report = CrewAssignmentService.commit(plan, false, TODAY);

        assertEquals(2, report.applied().size());
        assertEquals(second.unit, alpha.getUnit());
        assertEquals(spare.unit, bravo.getUnit());
        assertTrue(first.drivers.isEmpty());
    }

    @Test
    void aRefusedAssignmentPutsThePersonBackInTheirOldSeat() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        CrewFixture.CrewUnit home = mek("Home").with(pilot, SeatType.PILOT);
        CrewFixture.CrewUnit away = mek("Away");
        plan.stage(pilot, away.unit, SeatType.PILOT);
        away.refuse = pilot;

        CommitReport report = CrewAssignmentService.commit(plan, false, TODAY);

        assertTrue(report.applied().isEmpty());
        assertEquals(Reason.REFUSED, report.skipped().get(0).reason());
        assertEquals(home.unit, pilot.getUnit());
        assertEquals(List.of(pilot), home.drivers);
    }

    @Test
    void aChangeTheCampaignHasOvertakenIsSkippedAndLeavesThePersonAlone() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        CrewFixture.CrewUnit home = mek("Home").with(pilot, SeatType.PILOT);
        CrewFixture.CrewUnit away = mek("Away");
        plan.stage(pilot, away.unit, SeatType.PILOT);
        when(away.unit.isAvailable()).thenReturn(false);

        CommitReport report = CrewAssignmentService.commit(plan, false, TODAY);

        assertEquals(Reason.UNIT_UNAVAILABLE, report.skipped().get(0).reason());
        assertEquals(home.unit, pilot.getUnit());
        verify(home.unit, never()).remove(any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void withTransfersAMoveIsLoggedAsOneReassignment() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        CrewFixture.CrewUnit home = mek("Home").with(pilot, SeatType.PILOT);
        CrewFixture.CrewUnit away = mek("Away");
        plan.stage(pilot, away.unit, SeatType.PILOT);

        CrewAssignmentService.commit(plan, true, TODAY);

        // No separate removal entry, and the arrival is logged as a reassignment from the old unit
        verify(home.unit).remove(pilot, false);
        verify(away.unit).addPilotOrSoldier(pilot, home.unit, true);
    }

    @Test
    void withoutTransfersAMoveIsLoggedAsARemovalAndANewAssignment() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        CrewFixture.CrewUnit home = mek("Home").with(pilot, SeatType.PILOT);
        CrewFixture.CrewUnit away = mek("Away");
        plan.stage(pilot, away.unit, SeatType.PILOT);

        CrewAssignmentService.commit(plan, false, TODAY);

        verify(home.unit).remove(pilot, true);
        verify(away.unit).addPilotOrSoldier(pilot, home.unit, false);
    }

    @Test
    void eachSeatTypeUsesItsOwnUnitMethod() {
        CrewFixture.CrewUnit dropShip = fixture.vessel(mock(megamek.common.units.Dropship.class), "DropShip", 1, 1, 1);
        Person pilot = fixture.person("Pilot", PersonnelRole.VESSEL_PILOT);
        Person gunner = fixture.person("Gunner", PersonnelRole.VESSEL_GUNNER);
        Person crew = fixture.person("Crew", PersonnelRole.VESSEL_CREW);
        plan.stage(List.of(pilot, gunner, crew), List.of(dropShip.unit));

        CrewAssignmentService.commit(plan, false, TODAY);

        verify(dropShip.unit).addDriver(pilot, false);
        verify(dropShip.unit).addGunner(gunner, false);
        verify(dropShip.unit).addVesselCrew(crew, false);
    }
}
