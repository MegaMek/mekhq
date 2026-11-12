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
import java.util.UUID;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.Assignment;
import org.junit.jupiter.api.Test;

/**
 * Tests what {@link CrewAssignmentState} lists, and counts as assigned, on the Techs page as against the Assign page.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CrewAssignmentStateTechsPageTest {
    private static Person person(boolean isTech) {
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        when(person.isTech()).thenReturn(isTech);
        return person;
    }

    private static Unit unit() {
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(UUID.randomUUID());
        return unit;
    }

    @Test
    void theAssignPageListsEveryoneAndTheTechsPageOnlyTechs() {
        CrewAssignmentState state = new CrewAssignmentState();
        Person tech = person(true);
        Person pilot = person(false);

        assertTrue(state.lists(tech));
        assertTrue(state.lists(pilot));

        state.page = CrewAssignmentPage.TECHS;

        assertTrue(state.lists(tech));
        assertFalse(state.lists(pilot));
    }

    @Test
    void onTheTechsPageATechIsAssignedOnlyWhileTheyMaintainAUnit() {
        CrewAssignmentState state = new CrewAssignmentState();
        AssignmentPlan plan = new AssignmentPlan(false, false);
        // A tech who crews a unit but maintains none
        Person tech = person(true);
        when(tech.getUnit()).thenReturn(unit());

        assertTrue(state.countsAsAssigned(tech, plan));

        state.page = CrewAssignmentPage.TECHS;
        assertFalse(state.countsAsAssigned(tech, plan));

        when(tech.getTechUnits()).thenReturn(List.of(unit()));
        assertTrue(state.countsAsAssigned(tech, plan));
    }

    @Test
    void eachPageStartsOnItsOwnAssignmentFilter() {
        assertEquals(Assignment.UNASSIGNED, CrewAssignmentState.defaultAssignment(CrewAssignmentPage.ASSIGN));
        assertEquals(Assignment.EVERYONE, CrewAssignmentState.defaultAssignment(CrewAssignmentPage.TECHS));
    }

    @Test
    void aPagesOwnStartingAssignmentFilterIsNotCountedAsSet() {
        CrewAssignmentState state = new CrewAssignmentState();
        state.assignment = Assignment.EVERYONE;

        assertEquals(1, state.activeRailFilters(CrewAssignmentPage.ASSIGN));
        assertEquals(0, state.activeRailFilters(CrewAssignmentPage.TECHS));
    }
}
