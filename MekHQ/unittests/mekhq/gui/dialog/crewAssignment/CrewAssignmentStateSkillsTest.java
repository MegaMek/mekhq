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
package mekhq.gui.dialog.crewAssignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.enums.SkillLevel;
import megamek.common.units.AeroSpaceFighter;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.PersonEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.SkillEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.AssignSort;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.SkillGroup;
import org.junit.jupiter.api.Test;

/**
 * Tests the Personnel page's Skills filter, skill readout and skill orders in {@link CrewAssignmentState}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CrewAssignmentStateSkillsTest {
    /**
     * @param skills skill names and levels, alternating; each skill's value is 10 less its level, as a skill that
     *               counts down has
     */
    private static PersonEntry entry(PersonnelRole primary, PersonnelRole secondary, Object... skills) {
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(primary);
        when(person.getSecondaryRole()).thenReturn(secondary);
        Map<String, SkillEntry> byName = new HashMap<>();

        for (int index = 0; (index + 1) < skills.length; index += 2) {
            String name = (String) skills[index];
            int level = (Integer) skills[index + 1];
            byName.put(name, new SkillEntry(name, 10 - level, level));
        }
        return new PersonEntry(person, SkillLevel.REGULAR, List.of(), Map.copyOf(byName), 0, null, "",
              CrewAssignmentData.FamiliarityEntry.NONE);
    }

    private static PersonEntry mekWarrior(Object... skills) {
        return entry(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE, skills);
    }

    private static List<PersonEntry> sorted(CrewAssignmentState state, PersonEntry... entries) {
        List<PersonEntry> list = new ArrayList<>(List.of(entries));
        list.sort(state.assignOrder());
        return list;
    }

    // region Skills filter

    @Test
    void noSkillsFilterMatchesEveryone() {
        assertTrue(CrewAssignmentState.hasSkill(mekWarrior(), null));
    }

    @Test
    void anyScoutingSkillMatchesScouting() {
        assertTrue(CrewAssignmentState.hasSkill(mekWarrior(SkillType.S_TRACKING, 1), SkillGroup.SCOUTING));
        assertTrue(CrewAssignmentState.hasSkill(mekWarrior(SkillType.S_SENSOR_OPERATIONS, 0), SkillGroup.SCOUTING));
    }

    @Test
    void otherSkillsDoNotMatchScouting() {
        PersonEntry entry = mekWarrior(SkillType.S_GUN_MEK, 4, SkillType.S_LEADER, 2);
        assertFalse(CrewAssignmentState.hasSkill(entry, SkillGroup.SCOUTING));
        assertTrue(CrewAssignmentState.hasSkill(entry, SkillGroup.LEADERSHIP));
    }

    @Test
    void bestSkillInAGroupIsTheHighestLevel() {
        PersonEntry entry = mekWarrior(SkillType.S_PERCEPTION, 2, SkillType.S_SENSOR_OPERATIONS, 4,
              SkillType.S_STEALTH, 1);
        SkillEntry best = CrewAssignmentState.best(entry.skills(), SkillGroup.SCOUTING);
        assertEquals(SkillType.S_SENSOR_OPERATIONS, best.name());
        assertNull(CrewAssignmentState.best(entry.skills(), SkillGroup.ARTILLERY));
    }

    // endregion Skills filter

    // region Combat skills

    @Test
    void combatSkillsFollowTheCombatRoleWithNoUnit() {
        assertEquals(List.of(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK),
              CrewAssignmentState.combatSkills(mekWarrior().person(), null));
    }

    @Test
    void combatSkillsFallBackToTheSecondaryCombatRole() {
        PersonEntry entry = entry(PersonnelRole.DOCTOR, PersonnelRole.VEHICLE_CREW_VTOL);
        assertEquals(List.of(SkillType.S_GUN_VEE, SkillType.S_PILOT_VTOL),
              CrewAssignmentState.combatSkills(entry.person(), null));
    }

    @Test
    void noCombatRoleAndNoUnitHasNoCombatSkills() {
        PersonEntry entry = entry(PersonnelRole.DOCTOR, PersonnelRole.NONE);
        assertTrue(CrewAssignmentState.combatSkills(entry.person(), null).isEmpty());
    }

    @Test
    void combatSkillsFollowTheSelectedUnitOverTheRole() {
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(mock(AeroSpaceFighter.class));
        assertEquals(List.of(SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO),
              CrewAssignmentState.combatSkills(mekWarrior().person(), unit));
    }

    // endregion Combat skills

    // region Orders

    @Test
    void bestFirstKeepsTheOrderTheListArrivesIn() {
        CrewAssignmentState state = new CrewAssignmentState();
        assertEquals(AssignSort.BEST, state.effectiveAssignSort());
        assertNull(state.assignOrder());
    }

    @Test
    void combatOrderPutsBothSkillsAheadOfOneThenTheHigherTotal() {
        PersonEntry gunneryOnly = mekWarrior(SkillType.S_GUN_MEK, 9);
        PersonEntry weaker = mekWarrior(SkillType.S_GUN_MEK, 2, SkillType.S_PILOT_MEK, 2);
        PersonEntry stronger = mekWarrior(SkillType.S_GUN_MEK, 4, SkillType.S_PILOT_MEK, 3);
        PersonEntry neither = mekWarrior();
        CrewAssignmentState state = new CrewAssignmentState();
        state.assignSort = AssignSort.COMBAT;

        assertEquals(List.of(stronger, weaker, gunneryOnly, neither),
              sorted(state, neither, gunneryOnly, weaker, stronger));
    }

    @Test
    void combatOrderMeasuresAgainstTheSelectedUnit() {
        PersonEntry mekPilot = mekWarrior(SkillType.S_GUN_MEK, 5, SkillType.S_PILOT_MEK, 5);
        PersonEntry aeroPilot = mekWarrior(SkillType.S_GUN_AERO, 2, SkillType.S_PILOT_AERO, 2);
        Unit fighter = mock(Unit.class);
        when(fighter.getEntity()).thenReturn(mock(AeroSpaceFighter.class));
        CrewAssignmentState state = new CrewAssignmentState();
        state.assignSort = AssignSort.COMBAT;
        state.selectedUnit = fighter;

        assertEquals(List.of(aeroPilot, mekPilot), sorted(state, mekPilot, aeroPilot));
    }

    @Test
    void skillOrderNeedsTheSkillsFilter() {
        CrewAssignmentState state = new CrewAssignmentState();
        state.assignSort = AssignSort.SKILL;
        assertEquals(AssignSort.BEST, state.effectiveAssignSort());
        assertNull(state.assignOrder());
    }

    @Test
    void skillOrderPutsTheBestInTheGroupFirstAndThoseWithoutLast() {
        PersonEntry none = mekWarrior(SkillType.S_GUN_MEK, 5);
        PersonEntry tracker = mekWarrior(SkillType.S_TRACKING, 2);
        PersonEntry sensors = mekWarrior(SkillType.S_PERCEPTION, 1, SkillType.S_SENSOR_OPERATIONS, 4);
        CrewAssignmentState state = new CrewAssignmentState();
        state.skill = SkillGroup.SCOUTING;
        state.assignSort = AssignSort.SKILL;

        assertSame(AssignSort.SKILL, state.effectiveAssignSort());
        assertEquals(List.of(sensors, tracker, none), sorted(state, none, tracker, sensors));
    }

    // endregion Orders
}
