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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Reason;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StageResult;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedAssignment;
import org.junit.jupiter.api.Test;

class AssignmentPlanTest {
    private final CrewFixture fixture = new CrewFixture();
    private final AssignmentPlan plan = new AssignmentPlan(false, false);

    private Unit platoon(String name, int seats) {
        return fixture.infantry(mock(Infantry.class), name, true, seats).unit;
    }

    private Unit mek(String name) {
        return fixture.soloPilotUnit(mock(Mek.class), name).unit;
    }

    private List<Person> soldiers(int count) {
        return java.util.stream.IntStream.range(0, count)
                     .mapToObj(i -> fixture.person("Soldier " + i, PersonnelRole.SOLDIER))
                     .toList();
    }

    @Test
    void bulkStageFillsUnitsInOrderWithoutTouchingThem() {
        Unit first = platoon("First", 2);
        Unit second = platoon("Second", 2);
        List<Person> people = soldiers(3);

        StageResult result = plan.stage(people, List.of(first, second));

        assertEquals(3, result.staged().size());
        assertTrue(result.leftOver().isEmpty());
        assertEquals(0, plan.openSeats(first, SeatType.SOLDIER));
        assertEquals(1, plan.openSeats(second, SeatType.SOLDIER));
        assertEquals(second, plan.plannedUnitOf(people.get(2)));
        // Nothing real has changed yet
        assertNull(people.get(0).getUnit());
        assertTrue(first.getGunners().isEmpty());
    }

    @Test
    void peopleWithoutAFreeSeatAreLeftOver() {
        Unit squad = platoon("Squad", 1);
        Person trooper = fixture.person("Trooper", PersonnelRole.BATTLE_ARMOUR);
        List<Person> people = soldiers(2);

        StageResult result = plan.stage(List.of(people.get(0), trooper, people.get(1)), List.of(squad));

        assertEquals(1, result.staged().size());
        assertEquals(List.of(trooper, people.get(1)), result.leftOver());
    }

    @Test
    void aSeatAlreadyOverCapacityStaysFullUntilEnoughPeopleLeave() {
        List<Person> crew = soldiers(3);
        Unit unit = fixture.infantry(mock(Infantry.class), "Platoon", true, 2)
                          .with(crew.get(0), SeatType.SOLDIER)
                          .with(crew.get(1), SeatType.SOLDIER)
                          .with(crew.get(2), SeatType.SOLDIER).unit;

        plan.unassign(List.of(crew.get(0)));

        assertEquals(0, plan.openSeats(unit, SeatType.SOLDIER));
        assertFalse(plan.canStage(fixture.person("Newcomer", PersonnelRole.SOLDIER), unit, SeatType.SOLDIER));
    }

    @Test
    void undoRevertsAWholeBulkStage() {
        Unit unit = platoon("Platoon", 5);
        plan.stage(soldiers(2), List.of(unit));
        plan.stage(soldiers(2), List.of(unit));

        assertTrue(plan.undo());

        assertEquals(2, plan.size());
        assertEquals(3, plan.openSeats(unit, SeatType.SOLDIER));
    }

    @Test
    void stagingSomeoneAgainReplacesTheirChange() {
        Unit first = mek("First");
        Unit second = mek("Second");
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);

        plan.stage(pilot, first, SeatType.PILOT);
        plan.stage(pilot, second, SeatType.PILOT);

        assertEquals(1, plan.size());
        assertEquals(second, plan.changeFor(pilot).toUnit());
        assertNull(plan.changeFor(pilot).fromUnit());
        assertEquals(1, plan.openSeats(first, SeatType.PILOT));
    }

    @Test
    void stagingSomeoneBackIntoTheirOwnSeatCancelsTheirChange() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        Unit home = fixture.soloPilotUnit(mock(Mek.class), "Home").with(pilot, SeatType.PILOT).unit;
        Unit away = mek("Away");

        plan.stage(pilot, away, SeatType.PILOT);
        StageResult result = plan.stage(List.of(pilot), List.of(home));

        assertTrue(plan.isEmpty());
        assertTrue(result.leftOver().isEmpty());
        assertEquals(0, plan.openSeats(home, SeatType.PILOT));
    }

    @Test
    void aReassignmentFreesTheOldSeatForSomeoneElse() {
        Person veteran = fixture.person("Veteran", PersonnelRole.MEKWARRIOR);
        Unit oldMek = fixture.soloPilotUnit(mock(Mek.class), "Old").with(veteran, SeatType.PILOT).unit;
        Unit newMek = mek("New");
        Person rookie = fixture.person("Rookie", PersonnelRole.MEKWARRIOR);

        assertFalse(plan.canStage(rookie, oldMek, SeatType.PILOT));
        plan.stage(veteran, newMek, SeatType.PILOT);

        assertTrue(plan.changeFor(veteran).isReassignment());
        assertTrue(plan.stage(rookie, oldMek, SeatType.PILOT));
    }

    @Test
    void removalIsRefusedWhenTheOldSeatHasBeenGivenAway() {
        Person veteran = fixture.person("Veteran", PersonnelRole.MEKWARRIOR);
        Unit oldMek = fixture.soloPilotUnit(mock(Mek.class), "Old").with(veteran, SeatType.PILOT).unit;
        Person rookie = fixture.person("Rookie", PersonnelRole.MEKWARRIOR);
        plan.stage(veteran, mek("New"), SeatType.PILOT);
        plan.stage(rookie, oldMek, SeatType.PILOT);

        assertFalse(plan.remove(veteran));

        assertTrue(plan.remove(rookie));
        assertTrue(plan.remove(veteran));
        assertTrue(plan.isEmpty());
    }

    @Test
    void removalCanBeUndone() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        Unit unit = mek("Mek");
        plan.stage(pilot, unit, SeatType.PILOT);

        plan.remove(pilot);
        plan.undo();

        assertEquals(unit, plan.plannedUnitOf(pilot));
    }

    @Test
    void unqualifiedPeopleAndUnavailableUnitsAreNotStaged() {
        Unit mek = mek("Mek");
        Unit refitting = mek("Refitting");
        when(refitting.isAvailable()).thenReturn(false);

        assertFalse(plan.stage(fixture.person("Soldier", PersonnelRole.SOLDIER), mek, SeatType.PILOT));
        assertFalse(plan.stage(fixture.person("Pilot", PersonnelRole.MEKWARRIOR), refitting, SeatType.PILOT));
        assertTrue(plan.isEmpty());
    }

    @Test
    void validationFindsChangesTheCampaignHasOvertaken() {
        Unit mek = mek("Mek");
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        Person injured = fixture.person("Injured", PersonnelRole.SOLDIER);
        Unit unit = platoon("Platoon", 3);
        plan.stage(pilot, mek, SeatType.PILOT);
        plan.stage(List.of(injured), List.of(unit));

        when(mek.isAvailable()).thenReturn(false);
        when(injured.isDeployed()).thenReturn(true);

        List<AssignmentPlan.Problem> problems = plan.validate();
        assertEquals(2, problems.size());
        assertEquals(Reason.UNIT_UNAVAILABLE, reasonFor(problems, pilot));
        assertEquals(Reason.NOT_ASSIGNABLE, reasonFor(problems, injured));
    }

    private static Reason reasonFor(List<AssignmentPlan.Problem> problems, Person person) {
        return problems.stream()
                     .filter(problem -> problem.change().person() == person)
                     .map(AssignmentPlan.Problem::reason)
                     .findFirst()
                     .orElse(null);
    }

    @Test
    void changesKeepTheOrderTheyWereStaged() {
        Unit unit = platoon("Platoon", 3);
        List<Person> people = soldiers(3);
        plan.stage(people, List.of(unit));

        assertEquals(people, plan.changes().stream().map(StagedAssignment::person).toList());
    }
}
