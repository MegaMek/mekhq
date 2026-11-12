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

import java.nio.file.Files;
import java.nio.file.Path;

import megamek.client.ui.preferences.SuitePreferences;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.Assignment;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.Condition;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.FamiliarityBand;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.SingleRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests hiding the crew assignment dialog's filter rail: the saved setting, and the count of filters left set while
 * it is hidden.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CrewAssignmentFiltersHiddenTest {
    @Test
    void filtersAreShownByDefault() {
        CrewAssignmentGuidePreferences preferences = new CrewAssignmentGuidePreferences(
              new SuitePreferences().forClass(CrewAssignmentGuidePreferences.class));
        assertFalse(preferences.isFiltersHidden());
    }

    @Test
    void hiddenFiltersSurviveSavingAndLoadingThePreferencesFile(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("mhq.preferences");
        SuitePreferences saved = new SuitePreferences();
        CrewAssignmentGuidePreferences before = new CrewAssignmentGuidePreferences(
              saved.forClass(CrewAssignmentGuidePreferences.class));
        before.setFiltersHidden(true);
        saved.saveToFile(file.toString());

        String json = Files.readString(file);
        assertTrue(json.contains(CrewAssignmentGuidePreferences.FILTERS_HIDDEN), json);

        SuitePreferences loaded = new SuitePreferences();
        loaded.loadFromFile(file.toString());
        CrewAssignmentGuidePreferences after = new CrewAssignmentGuidePreferences(
              loaded.forClass(CrewAssignmentGuidePreferences.class));
        assertTrue(after.isFiltersHidden());
    }

    @Test
    void startingFiltersCountAsNoneSet() {
        CrewAssignmentState state = new CrewAssignmentState();
        assertEquals(0, state.activeRailFilters(CrewAssignmentPage.UNITS));
        assertEquals(0, state.activeRailFilters(CrewAssignmentPage.PERSONNEL));
        assertEquals(0, state.activeRailFilters(CrewAssignmentPage.REVIEW));
    }

    @Test
    void eachPageCountsOnlyItsOwnRailFilters() {
        CrewAssignmentState state = new CrewAssignmentState();
        state.unitType = "Mek";
        state.formation = "D-1";
        state.assignment = Assignment.EVERYONE;
        state.role = new SingleRole(PersonnelRole.MEKWARRIOR);
        state.condition = Condition.FIT;
        state.familiarity = FamiliarityBand.AT_LEAST_100;
        // The search box is above the list, not on the rail, so it stays in sight and is not counted
        state.peopleQuery = "Smith";

        assertEquals(2, state.activeRailFilters(CrewAssignmentPage.UNITS));
        assertEquals(4, state.activeRailFilters(CrewAssignmentPage.PERSONNEL));
        assertEquals(0, state.activeRailFilters(CrewAssignmentPage.REVIEW));
    }
}
