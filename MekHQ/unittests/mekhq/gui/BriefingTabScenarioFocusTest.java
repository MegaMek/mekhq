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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.swing.JTable;

import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;
import mekhq.gui.BriefingTab.ScenarioQueueFilter;
import mekhq.gui.model.ScenarioTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

class BriefingTabScenarioFocusTest {
    private static final LocalDate CAMPAIGN_DATE = LocalDate.of(3079, 1, 15);

    private MockedStatic<MekHQ> mekHQ;
    private BriefingTab tab;
    private AbstractContract mission;
    private List<Scenario> scenarios;
    private MMComboBox<AbstractContract> missions;
    private MMComboBox<ScenarioQueueFilter> filter;
    private ScenarioTableModel model;
    private JTable table;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        MHQOptions mhqOptions = mock(MHQOptions.class);
        when(mhqOptions.getLocale()).thenReturn(Locale.ENGLISH);
        when(mhqOptions.getDisplayFormattedDate(any())).thenAnswer(invocation -> {
            LocalDate date = invocation.getArgument(0);
            return (date == null) ? "" : date.toString();
        });
        mekHQ = mockStatic(MekHQ.class);
        mekHQ.when(MekHQ::getMHQOptions).thenReturn(mhqOptions);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(CAMPAIGN_DATE);
        when(campaign.getCampaignOptions()).thenReturn(mock(CampaignOptions.class));
        mission = mock(AbstractContract.class);
        scenarios = new ArrayList<>();
        when(mission.getScenarios()).thenReturn(scenarios);
        when(mission.getVisibleScenarios()).thenReturn(scenarios);
        LinkedHashMap<UUID, AbstractContract> contractHistory = new LinkedHashMap<>();
        contractHistory.put(UUID.randomUUID(), mission);
        when(campaign.getContractHistoryAsMap()).thenReturn(contractHistory);

        // Exercise navigation and filtering without constructing the unrelated detail panels.
        tab = mock(BriefingTab.class);
        when(tab.getCampaign()).thenReturn(campaign);
        doCallRealMethod().when(tab).focusOnScenario(anyInt());
        doCallRealMethod().when(tab).refreshScenarioTableData();

        model = new ScenarioTableModel(campaign);
        table = new JTable(model);
        table.setRowSorter(BriefingTab.createScenarioSorter(model));
        missions = new MMComboBox<>("comboMission", new AbstractContract[] { mission });
        filter = new MMComboBox<>("scenarioFilter", ScenarioQueueFilter.values());
        setTabField("scenarioModel", model);
        setTabField("scenarioTable", table);
        setTabField("comboMission", missions);
        setTabField("scenarioFilter", filter);
        missions.addActionListener(event -> tab.refreshScenarioTableData());
        filter.addActionListener(event -> tab.refreshScenarioTableData());
    }

    @AfterEach
    void tearDown() {
        mekHQ.close();
    }

    @ParameterizedTest
    @EnumSource(value = ScenarioQueueFilter.class, names = { "ALL", "ALL_ACTIVE", "CURRENT_MONTH", "UNASSIGNED" })
    void focusingVisibleActiveScenarioPreservesFilter(ScenarioQueueFilter selectedFilter) {
        Scenario target = addScenario(1, CAMPAIGN_DATE, ScenarioStatus.CURRENT);
        filter.setSelectedItem(selectedFilter);

        tab.focusOnScenario(target.getId());

        assertEquals(selectedFilter, filter.getSelectedItem());
        assertSelectedScenario(target);
    }

    @ParameterizedTest
    @EnumSource(value = ScenarioQueueFilter.class, names = { "ALL", "ALL_RESOLVED", "CURRENT_MONTH" })
    void focusingVisibleResolvedScenarioPreservesFilter(ScenarioQueueFilter selectedFilter) {
        Scenario target = addScenario(1, CAMPAIGN_DATE, ScenarioStatus.VICTORY);
        filter.setSelectedItem(selectedFilter);

        tab.focusOnScenario(target.getId());

        assertEquals(selectedFilter, filter.getSelectedItem());
        assertSelectedScenario(target);
    }

    @Test
    void focusingActiveScenarioHiddenByResolvedFilterRevealsIt() {
        Scenario target = addScenario(1, CAMPAIGN_DATE, ScenarioStatus.CURRENT);
        filter.setSelectedItem(ScenarioQueueFilter.ALL_RESOLVED);
        assertEquals(0, table.getRowCount());

        tab.focusOnScenario(target.getId());

        assertEquals(ScenarioQueueFilter.ALL_ACTIVE, filter.getSelectedItem());
        assertSelectedScenario(target);
    }

    @Test
    void focusingResolvedScenarioHiddenByActiveFilterRevealsIt() {
        Scenario target = addScenario(1, CAMPAIGN_DATE, ScenarioStatus.VICTORY);
        filter.setSelectedItem(ScenarioQueueFilter.ALL_ACTIVE);
        assertEquals(0, table.getRowCount());

        tab.focusOnScenario(target.getId());

        assertEquals(ScenarioQueueFilter.ALL_RESOLVED, filter.getSelectedItem());
        assertSelectedScenario(target);
    }

    @Test
    void focusingScenarioOutsideCurrentMonthRevealsIt() {
        Scenario target = addScenario(1, CAMPAIGN_DATE.minusMonths(1), ScenarioStatus.CURRENT);
        filter.setSelectedItem(ScenarioQueueFilter.CURRENT_MONTH);
        assertEquals(0, table.getRowCount());

        tab.focusOnScenario(target.getId());

        assertEquals(ScenarioQueueFilter.ALL_ACTIVE, filter.getSelectedItem());
        assertSelectedScenario(target);
    }

    @Test
    void focusingScenarioInAnotherMissionPreservesFilter() {
        Scenario target = addScenario(1, CAMPAIGN_DATE, ScenarioStatus.CURRENT);
        AbstractContract otherMission = mock(AbstractContract.class);
        when(otherMission.getVisibleScenarios()).thenReturn(List.of());
        missions.addItem(otherMission);
        missions.setSelectedItem(otherMission);
        filter.setSelectedItem(ScenarioQueueFilter.CURRENT_MONTH);

        tab.focusOnScenario(target.getId());

        assertEquals(mission, missions.getSelectedItem());
        assertEquals(ScenarioQueueFilter.CURRENT_MONTH, filter.getSelectedItem());
        assertSelectedScenario(target);
    }

    @ParameterizedTest
    @EnumSource(value = ScenarioQueueFilter.class, names = { "ALL", "ALL_ACTIVE", "ALL_RESOLVED", "CURRENT_MONTH" })
    void refreshingAfterNewScenarioPreservesFilterAndSelection(ScenarioQueueFilter selectedFilter) {
        ScenarioStatus status = (selectedFilter == ScenarioQueueFilter.ALL_RESOLVED) ?
                                      ScenarioStatus.VICTORY : ScenarioStatus.CURRENT;
        Scenario selected = addScenario(1, CAMPAIGN_DATE.minusDays(1), status);
        filter.setSelectedItem(selectedFilter);
        tab.focusOnScenario(selected.getId());
        addScenario(2, CAMPAIGN_DATE, ScenarioStatus.CURRENT);

        tab.refreshScenarioTableData();

        assertEquals(selectedFilter, filter.getSelectedItem());
        assertSelectedScenario(selected);
        assertEquals((selectedFilter == ScenarioQueueFilter.ALL_RESOLVED) ? 1 : 2, table.getRowCount());
    }

    private Scenario addScenario(int id, LocalDate date, ScenarioStatus status) {
        Scenario scenario = new Scenario("Scenario " + id);
        scenario.setId(id);
        scenario.setDate(date);
        scenario.setStatus(status);
        scenarios.add(scenario);
        tab.refreshScenarioTableData();
        return scenario;
    }

    private void assertSelectedScenario(Scenario expected) {
        assertTrue(table.getSelectedRow() >= 0, "The linked scenario should be selected");
        assertEquals(expected, model.getScenario(table.convertRowIndexToModel(table.getSelectedRow())));
        assertEquals(expected.getId(), tab.selectedScenario);
    }

    private void setTabField(String name, Object value) throws ReflectiveOperationException {
        Field field = BriefingTab.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(tab, value);
    }
}
