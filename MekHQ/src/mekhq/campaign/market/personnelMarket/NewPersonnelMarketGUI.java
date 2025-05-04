/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.personnelMarket;

import static mekhq.campaign.finances.enums.TransactionType.RECRUITMENT;
import static mekhq.gui.enums.PersonnelFilter.ACTIVE;
import static mekhq.gui.enums.PersonnelFilter.getStandardPersonnelFilters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.baseComponents.MMComboBox;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.enums.PersonnelFilter;
import mekhq.gui.view.PersonViewPanel;
// ---- END ROLE FILTER ADDITION ----

public class NewPersonnelMarketGUI {
    private static final int MAXIMUM_DAYS_IN_MONTH = 31;
    private static final int MAXIMUM_NUMBER_OF_SYSTEM_ROLLS = 4;

    /**
     * Shows a modal dialog containing the personnel table.
     */
    public static void showPersonnelTableDialog(Frame parent, Campaign campaign, List<Person> people,
          int recruitmentRolls, boolean systemHasNoPopulation, boolean noInterestedApplicants) {
        JDialog dialog = new JDialog(parent, "Personnel Table", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JCheckBox goldenHelloCheckbox = new JCheckBox("Offer Golden Hello");
        goldenHelloCheckbox.setSelected(campaign.isOfferingGoldenHello());
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                campaign.setIsOfferingGoldenHello(goldenHelloCheckbox.isSelected());
                dialog.dispose();
            }
        });
        leftPanel.add(goldenHelloCheckbox);

        JPanel availabilityPanel = new JPanel();
        availabilityPanel.setLayout(new BoxLayout(availabilityPanel, BoxLayout.Y_AXIS));
        availabilityPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (systemHasNoPopulation) {
            availabilityPanel.add(new JLabel("System is Unpopulated"));
        } else if (noInterestedApplicants) {
            availabilityPanel.add(new JLabel("Nobody is Interested in Joining your Faction"));
        } else {
            JLabel sliderLabel = new JLabel("Personnel Availability");
            int maxSliderValue = MAXIMUM_DAYS_IN_MONTH * MAXIMUM_NUMBER_OF_SYSTEM_ROLLS;
            JSlider personnelAvailabilitySlider = new JSlider(0, maxSliderValue, recruitmentRolls);
            personnelAvailabilitySlider.setEnabled(false);
            availabilityPanel.add(sliderLabel);
            availabilityPanel.add(personnelAvailabilitySlider);
        }

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(availabilityPanel, BorderLayout.EAST);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(topPanel, BorderLayout.NORTH);

        List<PersonnelFilter> filters = getStandardPersonnelFilters();
        filters.remove(ACTIVE);
        DefaultComboBoxModel<PersonnelFilter> filterModel = new DefaultComboBoxModel<>(filters.toArray(new PersonnelFilter[0]));
        MMComboBox<PersonnelFilter> roleComboBox = new MMComboBox<>("roleFilter");
        roleComboBox.setModel(filterModel);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Role:"));
        filterPanel.add(roleComboBox);

        PersonnelTablePanel tablePanel = new PersonnelTablePanel(campaign, people);

        JPanel tableAndFilterPanel = new JPanel(new BorderLayout());
        tableAndFilterPanel.add(filterPanel, BorderLayout.NORTH);
        tableAndFilterPanel.add(tablePanel, BorderLayout.CENTER);

        contentPanel.add(tableAndFilterPanel, BorderLayout.CENTER);

        JTable personnelTable = tablePanel.getTable();
        if (personnelTable.getRowSorter() instanceof TableRowSorter<?> sorter) {
            roleComboBox.addActionListener(ev -> {
                PersonnelFilter selectedFilter = roleComboBox.getSelectedItem();
                if (selectedFilter == null) {
                    selectedFilter = PersonnelFilter.ALL;
                }
                PersonnelFilter finalSelectedFilter = selectedFilter;
                sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                        int modelRow = entry.getIdentifier();
                        TableModel model = entry.getModel();
                        if (model instanceof PersonTableModel) {
                            Person person = ((PersonTableModel) model).getPerson(modelRow);
                            return finalSelectedFilter.getFilteredInformation(person, campaign.getLocalDate());
                        }
                        return true;
                    }
                });
            });
        }

        JLabel infoLabel = new JLabel("Paying a 12-month Golden Hello increases applicant quality.");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(infoLabel, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        Person initialPerson = tablePanel.getSelectedPerson();
        if (initialPerson != null) {
            PersonViewPanel personViewPanel = new PersonViewPanel(initialPerson,
                  campaign,
                  campaign.getApp().getCampaigngui());
            JScrollPane viewScrollPane = new JScrollPane(personViewPanel);
            viewScrollPane.setPreferredSize(new Dimension(500, 500));
            SwingUtilities.invokeLater(() -> viewScrollPane.getVerticalScrollBar().setValue(0));

            JPanel buttonPanel = new JPanel();
            JButton hireButton = new JButton("HIRE");
            hireButton.addActionListener(e -> {
                if (campaign.isOfferingGoldenHello()) {
                    campaign.getFinances()
                          .debit(RECRUITMENT,
                                campaign.getLocalDate(),
                                tablePanel.getSelectedPerson().getSalary(campaign).multipliedBy(12),
                                "hiring " + tablePanel.getSelectedPerson().getFullTitle());
                }
                campaign.recruitPerson(tablePanel.getSelectedPerson());
            });

            buttonPanel.add(hireButton);
            if (campaign.isGM()) {
                JButton addGMButton = new JButton("Add (GM)");
                addGMButton.addActionListener(e -> campaign.recruitPerson(tablePanel.getSelectedPerson(), true));
                buttonPanel.add(addGMButton);
            }

            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BorderLayout());
            rightPanel.add(viewScrollPane, BorderLayout.CENTER);
            rightPanel.add(buttonPanel, BorderLayout.SOUTH);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainPanel, rightPanel);
            splitPane.setResizeWeight(1.0);
            splitPane.setDividerLocation(0.75);
            dialog.getContentPane().add(splitPane, BorderLayout.CENTER);

            tablePanel.addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting()) {
                    Person selectedPerson = tablePanel.getSelectedPerson();
                    personViewPanel.setPerson(selectedPerson);
                }
            });
        } else {
            dialog.getContentPane().add(mainPanel, BorderLayout.CENTER);
        }

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
