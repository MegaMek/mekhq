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

import megamek.common.units.Entity;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.UnitEntry;
import org.junit.jupiter.api.Test;

/**
 * Tests that pinned units in {@link CrewAssignmentState} are listed whatever the filters, and listed first.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CrewAssignmentStatePinnedUnitsTest {
    private final AssignmentPlan plan = new AssignmentPlan(false, false);

    private static UnitEntry entry(String name, String typeName) {
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(UUID.randomUUID());
        when(unit.getName()).thenReturn(name);
        return new UnitEntry(unit, typeName, "", Entity.DMG_NONE, "", name.toLowerCase());
    }

    @Test
    void aPinnedUnitIsShownWhateverTheFilters() {
        UnitEntry pinned = entry("Atlas", "Mek");
        CrewAssignmentState state = new CrewAssignmentState();
        state.unitType = "Tank";
        state.formation = "Lance Two";
        state.unitQuery = "locust";

        assertFalse(state.shows(pinned, plan));

        state.pinnedUnits.add(pinned.unit());

        assertTrue(state.shows(pinned, plan));
    }

    @Test
    void anUnpinnedUnitStillFollowsTheFilters() {
        UnitEntry pinned = entry("Atlas", "Mek");
        UnitEntry other = entry("Locust", "Mek");
        CrewAssignmentState state = new CrewAssignmentState();
        state.pinnedUnits.add(pinned.unit());
        state.unitType = "Tank";

        assertFalse(state.shows(other, plan));
    }

    @Test
    void pinnedUnitsComeFirstInTheOrderTheyArrived() {
        UnitEntry first = entry("First", "Mek");
        UnitEntry second = entry("Second", "Mek");
        UnitEntry third = entry("Third", "Mek");
        UnitEntry fourth = entry("Fourth", "Mek");
        CrewAssignmentState state = new CrewAssignmentState();
        state.pinnedUnits.add(fourth.unit());
        state.pinnedUnits.add(second.unit());

        assertEquals(List.of(second, fourth, first, third),
              state.pinnedFirst(List.of(first, second, third, fourth)));
    }
}
