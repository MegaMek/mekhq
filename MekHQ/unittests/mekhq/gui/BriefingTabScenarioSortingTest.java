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
package mekhq.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableRowSorter;

import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.gui.model.ScenarioTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class BriefingTabScenarioSortingTest {
    private MockedStatic<MekHQ> mekHQ;

    /**
     * Supplies display date text, which would otherwise need the application's user preferences.
     */
    @BeforeEach
    void setUp() {
        MHQOptions mhqOptions = mock(MHQOptions.class);
        when(mhqOptions.getLocale()).thenReturn(Locale.ENGLISH);
        when(mhqOptions.getDisplayFormattedDate(any())).thenAnswer(invocation -> {
            LocalDate date = invocation.getArgument(0);
            return (date == null) ? "" : date.toString();
        });
        mekHQ = mockStatic(MekHQ.class);
        mekHQ.when(MekHQ::getMHQOptions).thenReturn(mhqOptions);
    }

    @AfterEach
    void tearDown() {
        mekHQ.close();
    }

    @Test
    void scenariosDefaultToNewestFirst() {
        Scenario past = scenario("Past", LocalDate.of(3078, 12, 31));
        Scenario future = scenario("Future", LocalDate.of(3079, 2, 1));
        Scenario current = scenario("Current", LocalDate.of(3079, 1, 1));
        Scenario undated = scenario("Undated", null);
        ScenarioTableModel model = scenarioModel();
        model.setData(List.of(past, undated, future, current));

        TableRowSorter<ScenarioTableModel> sorter = BriefingTab.createScenarioSorter(model);

        assertEquals(List.of(new RowSorter.SortKey(ScenarioTableModel.COL_DATE, SortOrder.DESCENDING)),
              sorter.getSortKeys());
        assertOrder(sorter, future, current, past, undated);
    }

    @Test
    void initiallyEmptyQueueSortsLoadedScenarios() {
        ScenarioTableModel model = scenarioModel();
        TableRowSorter<ScenarioTableModel> sorter = BriefingTab.createScenarioSorter(model);
        Scenario earlier = scenario("Earlier", LocalDate.of(3079, 1, 1));
        Scenario later = scenario("Later", LocalDate.of(3079, 2, 1));

        model.setData(List.of(earlier, later));
        sorter.allRowsChanged();

        assertOrder(sorter, later, earlier);
    }

    @Test
    void refreshingQueuePreservesManualSortOrder() {
        Scenario earlier = scenario("Earlier", LocalDate.of(3079, 1, 1));
        Scenario later = scenario("Later", LocalDate.of(3079, 2, 1));
        ScenarioTableModel model = scenarioModel();
        model.setData(List.of(earlier, later));
        TableRowSorter<ScenarioTableModel> sorter = BriefingTab.createScenarioSorter(model);

        sorter.toggleSortOrder(ScenarioTableModel.COL_DATE);
        assertOrder(sorter, earlier, later);

        model.setData(List.of(later, earlier));
        sorter.allRowsChanged();

        assertEquals(List.of(new RowSorter.SortKey(ScenarioTableModel.COL_DATE, SortOrder.ASCENDING)),
              sorter.getSortKeys());
        assertOrder(sorter, earlier, later);
    }

    @Test
    void manualNameSortingStillUsesNaturalOrder() {
        Scenario scenario10 = scenario("Scenario 10", LocalDate.of(3079, 2, 1));
        Scenario scenario2 = scenario("Scenario 2", LocalDate.of(3079, 1, 1));
        ScenarioTableModel model = scenarioModel();
        model.setData(List.of(scenario10, scenario2));
        TableRowSorter<ScenarioTableModel> sorter = BriefingTab.createScenarioSorter(model);

        sorter.toggleSortOrder(ScenarioTableModel.COL_NAME);

        assertOrder(sorter, scenario2, scenario10);
    }

    private static ScenarioTableModel scenarioModel() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(mock(CampaignOptions.class));
        return new ScenarioTableModel(campaign);
    }

    private static Scenario scenario(String name, LocalDate date) {
        Scenario scenario = new Scenario(name);
        scenario.setDate(date);
        return scenario;
    }

    private static void assertOrder(TableRowSorter<ScenarioTableModel> sorter, Scenario... expected) {
        assertEquals(expected.length, sorter.getViewRowCount());
        for (int row = 0; row < expected.length; row++) {
            assertEquals(expected[row], sorter.getModel().getScenario(sorter.convertRowIndexToModel(row)));
        }
    }
}
