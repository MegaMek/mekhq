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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.enums.TechAssignmentSortFactor;
import org.junit.jupiter.api.Test;

class TechAssignmentOrderTest {

    // --- The sort grid becomes a unit ordering ---

    @Test
    void unitOrderFor_everySlotUnset_returnsNullSoTheAssignerKeepsItsOwnOrder() {
        CommandGenerationOptions options = optionsWithSlots(TechAssignmentSortFactor.NONE, false,
              TechAssignmentSortFactor.NONE, false, TechAssignmentSortFactor.NONE, false);

        assertNull(TechAssignmentOrder.unitOrderFor(mock(Campaign.class), options));
    }

    @Test
    void unitOrderFor_nullArguments_returnNull() {
        assertNull(TechAssignmentOrder.unitOrderFor(null, mock(CommandGenerationOptions.class)));
        assertNull(TechAssignmentOrder.unitOrderFor(mock(Campaign.class), null));
    }

    @Test
    void unitOrderFor_unitWeightDescending_putsTheHeaviestFirst() {
        CommandGenerationOptions options = optionsWithSlots(TechAssignmentSortFactor.UNIT_WEIGHT, true,
              TechAssignmentSortFactor.NONE, false, TechAssignmentSortFactor.NONE, false);

        Unit light = unitWithWeightClassAndRank(1, 0);
        Unit assault = unitWithWeightClassAndRank(4, 0);
        Unit medium = unitWithWeightClassAndRank(2, 0);

        List<Unit> units = new ArrayList<>(List.of(light, assault, medium));
        Comparator<Unit> order = TechAssignmentOrder.unitOrderFor(mock(Campaign.class), options);
        assertNotNull(order);
        units.sort(order);

        assertEquals(List.of(assault, medium, light), units);
    }

    @Test
    void unitOrderFor_unitWeightAscending_putsTheLightestFirst() {
        CommandGenerationOptions options = optionsWithSlots(TechAssignmentSortFactor.UNIT_WEIGHT, false,
              TechAssignmentSortFactor.NONE, false, TechAssignmentSortFactor.NONE, false);

        Unit light = unitWithWeightClassAndRank(1, 0);
        Unit assault = unitWithWeightClassAndRank(4, 0);

        List<Unit> units = new ArrayList<>(List.of(assault, light));
        units.sort(TechAssignmentOrder.unitOrderFor(mock(Campaign.class), options));

        assertEquals(List.of(light, assault), units);
    }

    @Test
    void unitOrderFor_secondSlotBreaksTiesInTheFirst() {
        // Weight first, then pilot rank. Two assaults tie on weight and are split by their commander's rank.
        CommandGenerationOptions options = optionsWithSlots(TechAssignmentSortFactor.UNIT_WEIGHT, true,
              TechAssignmentSortFactor.PILOT_RANK, true, TechAssignmentSortFactor.NONE, false);

        Unit assaultLowRank = unitWithWeightClassAndRank(4, 2);
        Unit assaultHighRank = unitWithWeightClassAndRank(4, 9);
        Unit light = unitWithWeightClassAndRank(1, 20);

        List<Unit> units = new ArrayList<>(List.of(light, assaultLowRank, assaultHighRank));
        units.sort(TechAssignmentOrder.unitOrderFor(mock(Campaign.class), options));

        assertEquals(List.of(assaultHighRank, assaultLowRank, light), units,
              "Weight decides first; rank only splits the two assaults");
    }

    @Test
    void unitOrderFor_unsetFirstSlotDoesNotConsumeThePriority() {
        // The player left the primary slot unset but chose weight in the secondary; that must still order units.
        CommandGenerationOptions options = optionsWithSlots(TechAssignmentSortFactor.NONE, false,
              TechAssignmentSortFactor.UNIT_WEIGHT, true, TechAssignmentSortFactor.NONE, false);

        Unit light = unitWithWeightClassAndRank(1, 0);
        Unit assault = unitWithWeightClassAndRank(4, 0);

        List<Unit> units = new ArrayList<>(List.of(light, assault));
        Comparator<Unit> order = TechAssignmentOrder.unitOrderFor(mock(Campaign.class), options);
        assertNotNull(order, "A set secondary slot must still produce an ordering");
        units.sort(order);

        assertEquals(List.of(assault, light), units);
    }

    @Test
    void unitOrderFor_unitWithoutCommanderOrEntity_doesNotThrow() {
        CommandGenerationOptions options = optionsWithSlots(TechAssignmentSortFactor.PILOT_RANK, true,
              TechAssignmentSortFactor.UNIT_WEIGHT, true, TechAssignmentSortFactor.NONE, false);

        Unit bare = mock(Unit.class);
        when(bare.getCommander()).thenReturn(null);
        when(bare.getEntity()).thenReturn(null);

        List<Unit> units = new ArrayList<>(List.of(bare, unitWithWeightClassAndRank(4, 9)));
        units.sort(TechAssignmentOrder.unitOrderFor(mock(Campaign.class), options));

        assertEquals(2, units.size());
    }

    // --- Only the generated techs are offered ---

    @Test
    void techsAmong_keepsOnlyTheFourMaintenanceRoles() {
        List<Person> generated = List.of(
              personWithRole(PersonnelRole.MEK_TECH),
              personWithRole(PersonnelRole.MECHANIC),
              personWithRole(PersonnelRole.AERO_TEK),
              personWithRole(PersonnelRole.BA_TECH),
              personWithRole(PersonnelRole.DOCTOR),
              personWithRole(PersonnelRole.ADMINISTRATOR_HR),
              personWithRole(PersonnelRole.ASTECH));

        List<Person> techs = TechAssignmentOrder.techsAmong(generated);

        assertEquals(4, techs.size());
        for (Person tech : techs) {
            assertTrue(tech.getPrimaryRole().isTech(), "Only tech roles belong in the pool");
        }
    }

    @Test
    void techsAmong_nullOrEmpty_returnsEmptyList() {
        assertTrue(TechAssignmentOrder.techsAmong(null).isEmpty());
        assertTrue(TechAssignmentOrder.techsAmong(List.of()).isEmpty());
    }

    @Test
    void techsAmong_skipsNullEntries() {
        List<Person> generated = new ArrayList<>();
        generated.add(personWithRole(PersonnelRole.MEK_TECH));
        generated.add(null);

        assertEquals(1, TechAssignmentOrder.techsAmong(generated).size());
    }

    // --- Helpers ---

    private static CommandGenerationOptions optionsWithSlots(TechAssignmentSortFactor primary,
          boolean primaryDescending, TechAssignmentSortFactor secondary, boolean secondaryDescending,
          TechAssignmentSortFactor tertiary, boolean tertiaryDescending) {
        CommandGenerationOptions options = mock(CommandGenerationOptions.class);
        when(options.getTechAssignmentPrimarySort()).thenReturn(primary);
        when(options.isTechAssignmentPrimaryDescending()).thenReturn(primaryDescending);
        when(options.getTechAssignmentSecondarySort()).thenReturn(secondary);
        when(options.isTechAssignmentSecondaryDescending()).thenReturn(secondaryDescending);
        when(options.getTechAssignmentTertiarySort()).thenReturn(tertiary);
        when(options.isTechAssignmentTertiaryDescending()).thenReturn(tertiaryDescending);
        return options;
    }

    private static Unit unitWithWeightClassAndRank(int weightClass, int commanderRank) {
        Entity entity = mock(Entity.class);
        when(entity.getWeightClass()).thenReturn(weightClass);

        Person commander = mock(Person.class);
        when(commander.getRankNumeric()).thenReturn(commanderRank);

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.getCommander()).thenReturn(commander);
        return unit;
    }

    private static Person personWithRole(PersonnelRole role) {
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(role);
        return person;
    }
}
