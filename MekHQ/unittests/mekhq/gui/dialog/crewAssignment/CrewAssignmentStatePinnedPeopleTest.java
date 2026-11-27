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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.PersonEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.Assignment;
import org.junit.jupiter.api.Test;

/**
 * Tests how {@link CrewAssignmentState} holds the people list to the group the dialog was opened for.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CrewAssignmentStatePinnedPeopleTest {
    private static PersonEntry entry(Person person) {
        return new PersonEntry(person, SkillLevel.REGULAR, List.of(), Map.of(), 0, null, "",
              CrewAssignmentData.FamiliarityEntry.NONE);
    }

    private static Person person() {
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        return person;
    }

    @Test
    void withNoOnePinnedEveryoneIsPinned() {
        CrewAssignmentState state = new CrewAssignmentState();

        assertTrue(state.isPinned(person()));
    }

    @Test
    void onlyThePinnedPeopleAreListed() {
        CrewAssignmentState state = new CrewAssignmentState();
        state.assignment = Assignment.EVERYONE;
        AssignmentPlan plan = new AssignmentPlan(false, false);
        Person pinned = person();
        Person other = person();
        state.pinnedPeople.add(pinned);

        assertTrue(state.shows(entry(pinned), plan));
        assertFalse(state.shows(entry(other), plan));
    }

    @Test
    void pinnedPeopleAreListedWhetherOrNotTheyCrewAUnit() {
        CrewAssignmentState state = new CrewAssignmentState();
        AssignmentPlan plan = new AssignmentPlan(false, false);
        // The unit is made first: a mock cannot be stubbed inside another stubbing's thenReturn
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(UUID.randomUUID());
        Person crew = person();
        when(crew.getUnit()).thenReturn(unit);
        state.pinnedPeople.add(crew);
        state.assignment = state.startingAssignment(CrewAssignmentPage.ASSIGN);

        assertEquals(Assignment.EVERYONE, state.assignment);
        assertTrue(state.shows(entry(crew), plan));
    }

    @Test
    void listingEveryoneForAPinnedGroupIsNotCountedAsAFilterSet() {
        CrewAssignmentState state = new CrewAssignmentState();
        state.pinnedPeople.add(person());
        state.assignment = Assignment.EVERYONE;

        assertEquals(0, state.activeRailFilters(CrewAssignmentPage.ASSIGN));
    }

    @Test
    void theStartingAssignmentFilterIsThePagesOwnOnceTheGroupIsLetGo() {
        CrewAssignmentState state = new CrewAssignmentState();
        state.pinnedPeople.add(person());

        assertEquals(Assignment.EVERYONE, state.startingAssignment(CrewAssignmentPage.ASSIGN));

        state.pinnedPeople.clear();

        assertEquals(Assignment.UNASSIGNED, state.startingAssignment(CrewAssignmentPage.ASSIGN));
        assertEquals(Assignment.EVERYONE, state.startingAssignment(CrewAssignmentPage.TECHS));
    }
}
