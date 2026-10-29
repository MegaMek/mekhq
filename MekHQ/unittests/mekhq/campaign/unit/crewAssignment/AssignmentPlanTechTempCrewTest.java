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
import java.util.Map;
import java.util.function.ToIntFunction;

import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Reason;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.TechStageResult;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.TempCrewResult;
import org.junit.jupiter.api.Test;

/** Unassigning, tech changes and temporary crew changes in an {@link AssignmentPlan}. */
class AssignmentPlanTechTempCrewTest {
    private final CrewFixture fixture = new CrewFixture();
    private final AssignmentPlan plan = new AssignmentPlan(false, false);

    private CrewFixture.CrewUnit mek(String name) {
        return fixture.soloPilotUnit(mock(Mek.class), name);
    }

    private static ToIntFunction<PersonnelRole> pool(int soldiers) {
        return role -> (role == PersonnelRole.SOLDIER) ? soldiers : 0;
    }

    // region Unassigning

    @Test
    void unassigningSomeoneFreesTheirSeat() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        Unit unit = mek("Mek").with(pilot, SeatType.PILOT).unit;

        plan.unassign(List.of(pilot));

        assertNull(plan.plannedUnitOf(pilot));
        assertTrue(plan.changeFor(pilot).isRemoval());
        assertEquals(1, plan.openSeats(unit, SeatType.PILOT));
    }

    @Test
    void unassigningSomeoneOnlyStagedIntoAUnitCancelsTheirChange() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        Unit unit = mek("Mek").unit;
        plan.stage(pilot, unit, SeatType.PILOT);

        plan.unassign(List.of(pilot));

        assertTrue(plan.isEmpty());
        assertEquals(1, plan.openSeats(unit, SeatType.PILOT));
    }

    @Test
    void someoneWithNoUnitCannotBeUnassigned() {
        Person free = fixture.person("Free", PersonnelRole.MEKWARRIOR);

        assertEquals(List.of(free), plan.unassign(List.of(free)).leftOver());
        assertTrue(plan.isEmpty());
    }

    @Test
    void clearingUnitsTakesEveryoneOutButKeepsTheTech() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        Person tech = fixture.tech("Tech", 480);
        Unit unit = mek("Mek").with(pilot, SeatType.PILOT).maintainedBy(tech).unit;

        plan.clearUnits(List.of(unit));

        assertNull(plan.plannedUnitOf(pilot));
        assertEquals(tech, plan.plannedTechOf(unit));
    }

    @Test
    void removingTechsKeepsTheCrew() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        Person tech = fixture.tech("Tech", 480);
        Unit unit = mek("Mek").with(pilot, SeatType.PILOT).maintainedBy(tech).unit;

        plan.unassignTechs(List.of(unit));

        assertNull(plan.plannedTechOf(unit));
        assertEquals(unit, plan.plannedUnitOf(pilot));
    }

    // endregion Unassigning

    // region Techs

    @Test
    void aTechTakesUnitsUntilTheirTimeRunsOut() {
        Person tech = fixture.tech("Tech", 150);
        List<Unit> units = List.of(mek("A").unit, mek("B").unit, mek("C").unit);

        TechStageResult result = plan.stageTechs(List.of(tech), units);

        // Each unit takes 60 minutes a day, so 150 minutes cover two of them
        assertEquals(2, result.staged().size());
        assertEquals(List.of(units.get(2)), result.leftOver());
        assertEquals(120, plan.plannedTechMinutes(tech));
        assertEquals(units.subList(0, 2), plan.plannedTechUnits(tech));
    }

    @Test
    void theNextTechTakesOverWhenTheFirstIsFull() {
        Person best = fixture.tech("Best", 60);
        Person next = fixture.tech("Next", 480);
        Unit first = mek("A").unit;
        Unit second = mek("B").unit;

        plan.stageTechs(List.of(best, next), List.of(first, second));

        assertEquals(best, plan.plannedTechOf(first));
        assertEquals(next, plan.plannedTechOf(second));
    }

    @Test
    void stagingANewTechRemembersTheOldOne() {
        Person oldTech = fixture.tech("Old", 480);
        Person newTech = fixture.tech("New", 480);
        Unit unit = mek("Mek").maintainedBy(oldTech).unit;

        assertTrue(plan.stageTech(newTech, unit));

        assertEquals(oldTech, plan.techChangeFor(unit).fromTech());
        assertEquals(newTech, plan.plannedTechOf(unit));
        assertTrue(plan.plannedTechUnits(oldTech).isEmpty());
        assertEquals(0, plan.plannedTechMinutes(oldTech));
    }

    @Test
    void someoneWhoIsNotATechCannotMaintainAUnit() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);

        assertFalse(plan.stageTech(pilot, mek("Mek").unit));
    }

    @Test
    void aBusyTechCannotTakeOnMoreUnits() {
        Person tech = fixture.tech("Tech", 480);
        plan.setTechBusy(person -> person == tech);

        assertFalse(plan.canStageTech(tech, mek("Mek").unit));
    }

    @Test
    void selfCrewedUnitsTakeNoTech() {
        Person tech = fixture.tech("Tech", 480);
        Unit dropShip = mek("DropShip").unit;
        when(dropShip.isSelfCrewed()).thenReturn(true);

        assertFalse(plan.canStageTech(tech, dropShip));
        assertTrue(plan.stageTechs(List.of(tech), List.of(dropShip)).staged().isEmpty());
    }

    @Test
    void unassigningEverythingTakesSomeoneOffTheirUnitAndTheirTechWorkInOneStep() {
        Person techWhoPilots = fixture.tech("Both", 480);
        when(techWhoPilots.getSecondaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        Unit piloted = mek("Piloted").with(techWhoPilots, SeatType.PILOT).unit;
        Unit maintained = mek("Maintained").maintainedBy(techWhoPilots).unit;

        plan.unassignEverything(List.of(techWhoPilots));

        assertNull(plan.plannedUnitOf(techWhoPilots));
        assertNull(plan.plannedTechOf(maintained));
        assertEquals(2, plan.size());

        assertTrue(plan.undo());
        assertTrue(plan.isEmpty());
        assertEquals(piloted, plan.plannedUnitOf(techWhoPilots));
    }

    @Test
    void validationNoticesATechWhoNoLongerHasTheTime() {
        Person tech = fixture.tech("Tech", 480);
        Unit unit = mek("Mek").unit;
        plan.stageTech(tech, unit);

        when(tech.getDailyAvailableTechTime(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(30);

        assertEquals(Reason.NO_TECH_TIME, plan.problemWith(plan.techChangeFor(unit)));
    }

    // endregion Techs

    // region Temporary crew

    @Test
    void fillingTopsAUnitUpToFullStrengthFromThePool() {
        Person soldier = fixture.person("Soldier", PersonnelRole.SOLDIER);
        Unit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 10)
                             .with(soldier, SeatType.SOLDIER)
                             .withTempCrew(PersonnelRole.SOLDIER, 0).unit;

        TempCrewResult result = plan.fillTempCrew(List.of(platoon), pool(20));

        assertEquals(1, result.staged().size());
        assertEquals(9, plan.plannedTempCrew(platoon, PersonnelRole.SOLDIER));
        assertEquals(11, plan.plannedTempCrewPool(PersonnelRole.SOLDIER, pool(20)));
    }

    @Test
    void fillingStopsWhenThePoolRunsOut() {
        Person first = fixture.person("First", PersonnelRole.SOLDIER);
        Person second = fixture.person("Second", PersonnelRole.SOLDIER);
        Unit platoonA = fixture.infantry(mock(Infantry.class), "A", true, 10).with(first, SeatType.SOLDIER)
                              .withTempCrew(PersonnelRole.SOLDIER, 0).unit;
        Unit platoonB = fixture.infantry(mock(Infantry.class), "B", true, 10).with(second, SeatType.SOLDIER)
                              .withTempCrew(PersonnelRole.SOLDIER, 0).unit;

        TempCrewResult result = plan.fillTempCrew(List.of(platoonA, platoonB), pool(12));

        assertEquals(9, plan.plannedTempCrew(platoonA, PersonnelRole.SOLDIER));
        assertEquals(3, plan.plannedTempCrew(platoonB, PersonnelRole.SOLDIER));
        assertTrue(result.leftOver().isEmpty());
        assertEquals(0, plan.plannedTempCrewPool(PersonnelRole.SOLDIER, pool(12)));
    }

    @Test
    void aUnitNeedsARealCrewMemberBeforeItCanBeFilled() {
        Unit empty = fixture.infantry(mock(Infantry.class), "Empty", true, 10)
                           .withTempCrew(PersonnelRole.SOLDIER, 0).unit;

        TempCrewResult result = plan.fillTempCrew(List.of(empty), pool(20));

        assertEquals(List.of(empty), result.leftOver());
        assertTrue(plan.isEmpty());
    }

    @Test
    void aStagedArrivalCountsAsTheRealCrewMemberAFillNeeds() {
        Person soldier = fixture.person("Soldier", PersonnelRole.SOLDIER);
        Unit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 10)
                             .withTempCrew(PersonnelRole.SOLDIER, 0).unit;
        plan.stage(List.of(soldier), List.of(platoon));

        plan.fillTempCrew(List.of(platoon), pool(20));

        assertEquals(9, plan.plannedTempCrew(platoon, PersonnelRole.SOLDIER));
    }

    @Test
    void removingTempCrewClearsThemAndReturnsThemToThePool() {
        Unit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 10)
                             .withTempCrew(PersonnelRole.SOLDIER, 7).unit;

        TempCrewResult result = plan.removeTempCrew(List.of(platoon));

        assertEquals(1, result.staged().size());
        assertEquals(-7, result.staged().get(0).change());
        assertEquals(0, plan.plannedTotalTempCrew(platoon));
        assertEquals(7, plan.plannedTempCrewPool(PersonnelRole.SOLDIER, pool(0)));
    }

    @Test
    void validationNoticesTempCrewThatChangedSinceTheyWereStaged() {
        CrewFixture.CrewUnit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 10)
                                             .withTempCrew(PersonnelRole.SOLDIER, 7);
        plan.removeTempCrew(List.of(platoon.unit));

        platoon.tempCrew.put(PersonnelRole.SOLDIER, 5);

        assertEquals(Reason.OUT_OF_DATE, plan.problemWith(plan.tempCrewChanges().get(0)));
    }

    @Test
    void sizeCountsEveryKindOfChange() {
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR);
        Person tech = fixture.tech("Tech", 480);
        Unit mekUnit = mek("Mek").unit;
        Unit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 10)
                             .withTempCrew(PersonnelRole.SOLDIER, 4).unit;

        plan.stage(pilot, mekUnit, SeatType.PILOT);
        plan.stageTech(tech, mekUnit);
        plan.removeTempCrew(List.of(platoon));

        assertEquals(3, plan.size());
        assertEquals(Map.of(PersonnelRole.SOLDIER, 0), plan.tempCrewChanges().get(0).after());
    }

    // endregion Temporary crew
}
