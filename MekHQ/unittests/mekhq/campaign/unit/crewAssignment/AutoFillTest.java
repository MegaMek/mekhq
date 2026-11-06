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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.SkillLevel;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import megamek.common.units.Tank;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StageResult;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedAssignment;
import org.junit.jupiter.api.Test;

class AutoFillTest {
    private final CrewFixture fixture = new CrewFixture();
    private final AssignmentPlan plan = new AssignmentPlan(false, false);

    private Person soldier(String name, SkillLevel level) {
        return person(name, PersonnelRole.SOLDIER, level);
    }

    private Person person(String name, PersonnelRole role, SkillLevel level) {
        Person person = fixture.person(name, role);
        when(person.getSkillLevel(any(), anyBoolean(), any(), anyBoolean(), anyBoolean())).thenReturn(level);
        return person;
    }

    private static Comparator<Person> ranking(Map<Person, Integer> combatSkill) {
        return AutoFill.byExperience(mock(CampaignOptions.class), false, LocalDate.of(3067, 4, 12),
              person -> combatSkill.getOrDefault(person, 8));
    }

    @Test
    void fillsOnlyTheOpenSeatsWithTheBestPeople() {
        Unit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 2).unit;
        Person green = soldier("Green", SkillLevel.GREEN);
        Person elite = soldier("Elite", SkillLevel.ELITE);
        Person veteran = soldier("Veteran", SkillLevel.VETERAN);

        StageResult result = AutoFill.fill(plan, List.of(platoon), SeatType.SOLDIER, List.of(green, elite, veteran),
              ranking(Map.of()));

        assertEquals(List.of(elite, veteran), result.staged().stream().map(StagedAssignment::person).toList());
        assertEquals(0, plan.openSeats(platoon, SeatType.SOLDIER));
    }

    @Test
    void tiesGoToTheBetterCombatSkill() {
        Unit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 1).unit;
        Person steady = soldier("Steady", SkillLevel.REGULAR);
        Person sharp = soldier("Sharp", SkillLevel.REGULAR);

        StageResult result = AutoFill.fill(plan, List.of(platoon), SeatType.SOLDIER, List.of(steady, sharp),
              ranking(Map.of(steady, 9, sharp, 7)));

        assertEquals(sharp, result.staged().get(0).person());
    }

    @Test
    void skipsPeopleWhoAlreadyCrewAUnitOrAreStaged() {
        Person crewing = soldier("Crewing", SkillLevel.ELITE);
        fixture.infantry(mock(Infantry.class), "Other", true, 5).with(crewing, SeatType.SOLDIER);
        Person staged = soldier("Staged", SkillLevel.ELITE);
        Unit elsewhere = fixture.infantry(mock(Infantry.class), "Elsewhere", true, 5).unit;
        plan.stage(List.of(staged), List.of(elsewhere));
        Person free = soldier("Free", SkillLevel.GREEN);
        Unit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 3).unit;

        StageResult result = AutoFill.fill(plan, List.of(platoon), SeatType.SOLDIER,
              List.of(crewing, staged, free), ranking(Map.of()));

        assertEquals(List.of(free), result.staged().stream().map(StagedAssignment::person).toList());
    }

    @Test
    void fillsUnitsInTheOrderGiven() {
        Unit first = fixture.infantry(mock(Infantry.class), "First", true, 1).unit;
        Unit second = fixture.infantry(mock(Infantry.class), "Second", true, 1).unit;
        Person best = soldier("Best", SkillLevel.ELITE);
        Person next = soldier("Next", SkillLevel.VETERAN);

        AutoFill.fill(plan, List.of(first, second), SeatType.SOLDIER, List.of(next, best), ranking(Map.of()));

        assertEquals(first, plan.plannedUnitOf(best));
        assertEquals(second, plan.plannedUnitOf(next));
    }

    @Test
    void peopleNoUnitCanTakeDoNotUseUpTheOpenSeats() {
        Unit squad = fixture.infantry(mock(BattleArmor.class), "Squad", false, 1).unit;
        Unit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 1).unit;
        Person eliteTrooper = person("Elite", PersonnelRole.BATTLE_ARMOUR, SkillLevel.ELITE);
        Person veteranTrooper = person("Veteran", PersonnelRole.BATTLE_ARMOUR, SkillLevel.VETERAN);
        Person soldier = soldier("Green", SkillLevel.GREEN);

        AutoFill.fill(plan, List.of(squad, platoon), SeatType.SOLDIER,
              List.of(eliteTrooper, veteranTrooper, soldier), ranking(Map.of()));

        assertEquals(squad, plan.plannedUnitOf(eliteTrooper));
        assertNull(plan.plannedUnitOf(veteranTrooper));
        assertEquals(platoon, plan.plannedUnitOf(soldier));
    }

    @Test
    void peopleGoIntoTheSeatBeingFilled() {
        Tank tank = mock(Tank.class);
        when(tank.getMovementMode()).thenReturn(EntityMovementMode.TRACKED);
        Unit unit = fixture.vehicle(tank, "Tank", 1).unit;
        Person crew = person("Crew", PersonnelRole.VEHICLE_CREW_GROUND, SkillLevel.REGULAR);

        AutoFill.fill(plan, List.of(unit), SeatType.GUNNER, List.of(crew), ranking(Map.of()));

        assertEquals(SeatType.GUNNER, plan.changeFor(crew).toSeat());
    }

    @Test
    void nothingIsStagedWhenEverySeatIsFull() {
        Unit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 0).unit;

        StageResult result = AutoFill.fill(plan, List.of(platoon), SeatType.SOLDIER,
              List.of(soldier("Spare", SkillLevel.REGULAR)), ranking(Map.of()));

        assertTrue(result.staged().isEmpty());
        assertTrue(plan.isEmpty());
    }
}
