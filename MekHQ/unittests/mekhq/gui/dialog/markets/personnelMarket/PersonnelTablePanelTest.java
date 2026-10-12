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
package mekhq.gui.dialog.markets.personnelMarket;

import static mekhq.gui.dialog.markets.personnelMarket.ApplicantTableColumns.HIRING_COST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class PersonnelTablePanelTest {
    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private NewPersonnelMarket market;
    private List<Person> applicants;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3050, 4, 6));
        market = campaign.getPlayerForce().getHumanResources().getNewPersonnelMarket();
        applicants = new ArrayList<>();
    }

    private Person addApplicant(double hiringCost) {
        Person person = mock(Person.class);
        when(person.getFullName()).thenReturn("Applicant " + applicants.size());
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.SOLDIER);
        when(person.getOptions(PersonnelOptions.LVL3_ADVANTAGES))
              .thenAnswer(invocation -> Collections.emptyEnumeration());
        when(market.getHiringCost(person)).thenReturn(Money.of(hiringCost));
        applicants.add(person);
        return person;
    }

    private int hiringCostColumn() {
        return ApplicantTableColumns.getVisibleColumns(campaignOptions).indexOf(HIRING_COST);
    }

    private void assertSortedCosts(JTable table, SortOrder order) {
        int column = hiringCostColumn();
        for (int row = 1; row < table.getRowCount(); row++) {
            Money previous = assertInstanceOf(Money.class, table.getValueAt(row - 1, column));
            Money current = assertInstanceOf(Money.class, table.getValueAt(row, column));
            int comparison = previous.compareTo(current);
            assertTrue(order == SortOrder.ASCENDING ? comparison <= 0 : comparison >= 0);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void hiringCostsUseMoneyValuesAndKeepFormattedDisplay(boolean showReputation) throws Exception {
        campaignOptions.set(CampaignOption.USE_CHAOS_REPUTATION, showReputation);
        campaignOptions.set(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION, false);
        Person applicant = addApplicant(12345.75);

        SwingUtilities.invokeAndWait(() -> {
            PersonnelTablePanel panel = new PersonnelTablePanel(campaign, applicants);
            JTable table = panel.getTable();
            int column = hiringCostColumn();
            Money expectedCost = market.getHiringCost(applicant);

            assertEquals(Money.class, table.getModel().getColumnClass(column));
            assertEquals(expectedCost, table.getValueAt(0, column));
            JLabel renderer = assertInstanceOf(JLabel.class,
                  table.prepareRenderer(table.getCellRenderer(0, column), 0, column));
            assertEquals(expectedCost.toAmountString(), renderer.getText());
        });
    }

    @ParameterizedTest
    @EnumSource(value = SortOrder.class, names = {"ASCENDING", "DESCENDING"})
    void hiringCostsSortNumericallyAndSelectTheCorrectApplicant(SortOrder order) throws Exception {
        for (double cost : new double[] {2000, 0, 500, 10000, 1000, 1000, 3000000000.0, 2500.75}) {
            addApplicant(cost);
        }

        SwingUtilities.invokeAndWait(() -> {
            PersonnelTablePanel panel = new PersonnelTablePanel(campaign, applicants);
            JTable table = panel.getTable();
            table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(hiringCostColumn(), order)));

            assertSortedCosts(table, order);
            Person expected = applicants.get(table.convertRowIndexToModel(0));
            table.setRowSelectionInterval(0, 0);
            assertEquals(List.of(expected), panel.getSelectedApplicants());
            assertEquals(Money.of(order == SortOrder.ASCENDING ? 0 : 3000000000.0),
                  table.getValueAt(0, hiringCostColumn()));
        });
    }

    @ParameterizedTest
    @CsvSource({"false, ASCENDING", "false, DESCENDING", "true, ASCENDING", "true, DESCENDING"})
    void repeatedHiringRefreshesKeepTheTableSorted(boolean showReputation, SortOrder order) throws Exception {
        campaignOptions.set(CampaignOption.USE_CHAOS_REPUTATION, showReputation);
        campaignOptions.set(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION, false);
        double[] costs = {0, 500, 1000, 2000, 5000, 10000, 25000};
        Random random = new Random(2);
        for (int row = 0; row < 100; row++) {
            addApplicant(costs[random.nextInt(costs.length)]);
        }

        SwingUtilities.invokeAndWait(() -> {
            PersonnelTablePanel panel = new PersonnelTablePanel(campaign, applicants);
            JTable table = panel.getTable();
            PersonTableModel model = (PersonTableModel) table.getModel();
            List<RowSorter.SortKey> sortKeys = List.of(
                  new RowSorter.SortKey(ApplicantTableColumns.PROFESSION.columnIndex, SortOrder.ASCENDING),
                  new RowSorter.SortKey(hiringCostColumn(), order));
            table.getRowSorter().setSortKeys(sortKeys);

            while (!applicants.isEmpty()) {
                table.setRowSelectionInterval(0, 0);
                Person hired = panel.getSelectedApplicants().getFirst();
                assertSame(applicants.get(table.convertRowIndexToModel(0)), hired);

                applicants.remove(hired);
                model.fireTableDataChanged();
                table.clearSelection();

                assertSortedCosts(table, order);
                assertEquals(applicants.size(), table.getRowCount());
                assertTrue(panel.getSelectedApplicants().isEmpty());
                assertEquals(sortKeys, table.getRowSorter().getSortKeys());
            }
        });
    }

    @Test
    void addingAnApplicantPreservesHiringCostSorting() throws Exception {
        addApplicant(10000);
        addApplicant(500);

        SwingUtilities.invokeAndWait(() -> {
            PersonnelTablePanel panel = new PersonnelTablePanel(campaign, applicants);
            JTable table = panel.getTable();
            table.getRowSorter().setSortKeys(List.of(
                  new RowSorter.SortKey(hiringCostColumn(), SortOrder.ASCENDING)));
            table.setRowSelectionInterval(0, 0);

            addApplicant(2000);
            ((PersonTableModel) table.getModel()).fireTableDataChanged();
            table.clearSelection();

            assertEquals(3, table.getRowCount());
            assertSortedCosts(table, SortOrder.ASCENDING);
            assertTrue(panel.getSelectedApplicants().isEmpty());
        });
    }
}
