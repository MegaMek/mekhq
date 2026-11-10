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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import megamek.common.units.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.SeatType;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.FamiliarityBand;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.SeatFilter;
import org.junit.jupiter.api.Test;

/**
 * Tests the Personnel page's familiarity filter in {@link CrewAssignmentState}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CrewAssignmentStateFamiliarityTest {
    private static Unit unitWithChassis(String chassis) {
        Entity entity = mock(Entity.class);
        when(entity.getChassis()).thenReturn(chassis);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        return unit;
    }

    private static Person personWith(Map<String, Integer> familiarity) {
        Person person = mock(Person.class);
        when(person.getChassisFamiliarity()).thenReturn(familiarity);
        for (Map.Entry<String, Integer> entry : familiarity.entrySet()) {
            when(person.getChassisFamiliarity(entry.getKey())).thenReturn(entry.getValue());
        }
        return person;
    }

    @Test
    void bandsSplitAtTheBonusSteps() {
        assertTrue(CrewAssignmentState.matches(99, FamiliarityBand.UNDER_100));
        assertFalse(CrewAssignmentState.matches(100, FamiliarityBand.UNDER_100));
        assertTrue(CrewAssignmentState.matches(100, FamiliarityBand.AT_LEAST_100));
        assertFalse(CrewAssignmentState.matches(199, FamiliarityBand.AT_LEAST_200));
        assertTrue(CrewAssignmentState.matches(250, FamiliarityBand.AT_LEAST_200));
        assertTrue(CrewAssignmentState.matches(250, FamiliarityBand.AT_LEAST_100));
        assertFalse(CrewAssignmentState.matches(250, FamiliarityBand.AT_LEAST_300));
        assertTrue(CrewAssignmentState.matches(300, FamiliarityBand.AT_LEAST_300));
        assertTrue(CrewAssignmentState.matches(0, FamiliarityBand.ANY));
    }

    @Test
    void scoreUsesTheGivenChassis() {
        Person person = personWith(Map.of("Atlas", 150, "Locust", 250));
        assertEquals(150, CrewAssignmentState.familiarityScore(person, "Atlas"));
        assertEquals(0, CrewAssignmentState.familiarityScore(person, "Commando"));
    }

    @Test
    void scoreWithoutChassisIsTheBestFamiliarity() {
        assertEquals(250, CrewAssignmentState.familiarityScore(personWith(Map.of("Atlas", 150, "Locust", 250)), null));
        assertEquals(0, CrewAssignmentState.familiarityScore(personWith(Map.of()), null));
    }

    @Test
    void chassisComesFromTheSeatBeingFilledFirst() {
        CrewAssignmentState state = new CrewAssignmentState();
        state.selectedUnits.add(unitWithChassis("Locust"));
        state.qualifiedFor = new SeatFilter(unitWithChassis("Atlas"), SeatType.PILOT);
        assertEquals("Atlas", state.familiarityChassis());
    }

    @Test
    void chassisComesFromASingleSelectedUnit() {
        CrewAssignmentState state = new CrewAssignmentState();
        state.selectedUnits.add(unitWithChassis("Locust"));
        assertEquals("Locust", state.familiarityChassis());

        state.selectedUnits.add(unitWithChassis("Atlas"));
        assertNull(state.familiarityChassis());
    }
}
