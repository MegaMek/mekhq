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
package mekhq.gui.dialog.markets.personnelMarket;

import static mekhq.campaign.finances.enums.TransactionType.RECRUITMENT;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.NONE;
import static mekhq.gui.enums.PersonnelFilter.ACTIVE;
import static mekhq.gui.enums.PersonnelFilter.getStandardPersonnelFilters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.baseComponents.MMComboBox;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.market.personnelMarket.NewPersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.enums.PersonnelFilter;
import mekhq.gui.view.PersonViewPanel;

public class NewPersonnelMarketGUI {
    private static final int MAXIMUM_DAYS_IN_MONTH = 31;
    private static final int MAXIMUM_NUMBER_OF_SYSTEM_ROLLS = 4;

    private final NewPersonnelMarket market;
    private final JFrame parent;
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;

    private List<Person> currentApplicants;
    private MMComboBox<PersonnelFilter> roleComboBox = new MMComboBox<>("roleFilter");
    private JCheckBox goldenHelloCheckbox = new JCheckBox();
    private Person selectedPerson;
    private PersonnelTablePanel tablePanel;
    private PersonViewPanel personViewPanel;

    public NewPersonnelMarketGUI(NewPersonnelMarket market) {
        this.market = market;
        this.campaign = market.getCampaign();
        this.campaignOptions = campaign.getCampaignOptions();
        this.parent = campaign.getApp().getCampaigngui().getFrame();
        this.currentApplicants = market.getCurrentApplicants();

        initializeComponents();
    }

    /**
     * Shows a modal dialog containing the personnel table.
     */
    public void initializeComponents() {
        JDialog dialog = new JDialog(parent, "Personnel Market", true);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                market.setOfferingGoldenHello(goldenHelloCheckbox.isSelected());
                market.setCurrentApplicants(currentApplicants);
                dialog.dispose();
            }
        });

        // This panel houses all components
        JPanel mainPanel = new JPanel(new BorderLayout());

        // This panel houses the tips, table, and header
        JPanel pnlLeft = new JPanel(new BorderLayout());
        mainPanel.add(pnlLeft, BorderLayout.CENTER);

        JPanel pnlHeader = initializeHeader();
        pnlLeft.add(pnlHeader, BorderLayout.NORTH);

        tablePanel = initializeTablePanel();
        tablePanel.addListSelectionListener(e -> {
            List<Person> applicants = tablePanel.getSelectedApplicants();
            selectedPerson = applicants.isEmpty() ? null : applicants.get(0);
            if (personViewPanel != null) {
                personViewPanel.setPerson(selectedPerson);
            }
        });
        pnlLeft.add(tablePanel, BorderLayout.CENTER);

        JPanel pnlTips = initializeTipPanel();
        pnlLeft.add(pnlTips, BorderLayout.SOUTH);

        AtomicReference<Person> selectedPerson = new AtomicReference<>();
        if (!currentApplicants.isEmpty()) {
            selectedPerson.set(tablePanel.getSelectedApplicants().get(0));
        }

        // This handles the initializing and display of the applicant panel
        JSplitPane splitPane = initializePersonView(selectedPerson, mainPanel);
        dialog.getContentPane().add(splitPane, BorderLayout.CENTER);

        // Finalize the dialog
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private JSplitPane initializePersonView(AtomicReference<Person> selectedPerson, JPanel mainPanel) {
        personViewPanel = new PersonViewPanel(selectedPerson.get(), campaign, campaign.getApp().getCampaigngui());
        JScrollPane viewScrollPane = new JScrollPane(personViewPanel);
        viewScrollPane.setPreferredSize(new Dimension(500, 500));
        SwingUtilities.invokeLater(() -> viewScrollPane.getVerticalScrollBar().setValue(0));

        JPanel buttonPanel = initializeButtonPanel();

        JPanel applicantPanel = new JPanel();
        applicantPanel.setLayout(new BorderLayout());
        applicantPanel.add(viewScrollPane, BorderLayout.CENTER);
        applicantPanel.add(buttonPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainPanel, applicantPanel);
        splitPane.setResizeWeight(1.0);
        splitPane.setDividerLocation(0.75);
        personViewPanel.setVisible(selectedPerson.get() != null);

        return splitPane;
    }

    private JPanel initializeButtonPanel() {
        boolean isGM = campaign.isGM();

        JPanel buttonPanel = new JPanel();
        JButton hireButton = new JButton("HIRE");
        hireButton.addActionListener(e -> hireActionListener(isGM));
        buttonPanel.add(hireButton);

        JButton addGMButton = new JButton("Add (GM)");
        addGMButton.addActionListener(e -> hireActionListener(isGM));
        addGMButton.setEnabled(isGM);
        buttonPanel.add(addGMButton);

        return buttonPanel;
    }

    private void hireActionListener(boolean isGM) {
        List<Person> recruitedPersons = new ArrayList<>(tablePanel.getSelectedApplicants());

        // Process recruitment and golden hello logic for all selected applicants
        for (Person applicant : recruitedPersons) {
            if (!isGM && market.isOfferingGoldenHello()) {
                campaign.getFinances()
                      .debit(RECRUITMENT,
                            campaign.getLocalDate(),
                            applicant.getSalary(campaign).multipliedBy(12),
                            "hiring " + applicant.getFullTitle());
            }
            campaign.recruitPerson(applicant, isGM);
        }

        // Remove all recruited persons from the applicant list
        currentApplicants.removeAll(recruitedPersons);
        if (currentApplicants.isEmpty()) {
            personViewPanel.setVisible(false);
        }

        // Refresh the table view (notify the model of data changes)
        AbstractTableModel model = (AbstractTableModel) tablePanel.getTable().getModel();
        model.fireTableDataChanged();

        // Clear selection in the table
        tablePanel.getTable().clearSelection();
    }

    private static JPanel initializeTipPanel() {
        JLabel infoLabel = new JLabel("Paying a 12-month Golden Hello increases applicant quality.");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(infoLabel, BorderLayout.CENTER);
        return bottomPanel;
    }

    private PersonnelTablePanel initializeTablePanel() {
        PersonnelTablePanel tablePanel = new PersonnelTablePanel(campaign, currentApplicants);

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
        return tablePanel;
    }

    private JPanel initializeHeader() {
        JPanel panel = new JPanel(new GridBagLayout());

        // === Left Column ===
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0;
        leftGbc.weightx = 1.0;
        leftGbc.fill = GridBagConstraints.NONE;
        leftGbc.anchor = GridBagConstraints.WEST;
        int leftRow = 0;

        // Golden Hello Checkbox
        leftGbc.gridy = leftRow++;
        leftGbc.insets = new Insets(0, 0, 8, 0);
        JCheckBox goldenHelloCheckbox = new JCheckBox("Offer Golden Hello");
        goldenHelloCheckbox.setSelected(market.isOfferingGoldenHello());
        leftPanel.add(goldenHelloCheckbox, leftGbc);

        // Role ComboBox (Label + ComboBox)
        leftGbc.gridy = leftRow++;
        leftGbc.insets = new Insets(0, 0, 0, 0);

        JPanel filterPanel = new JPanel(new GridBagLayout());
        GridBagConstraints fbc = new GridBagConstraints();
        fbc.gridx = 0;
        fbc.gridy = 0;
        fbc.anchor = GridBagConstraints.WEST;
        filterPanel.add(new JLabel("Role:"), fbc);

        List<PersonnelFilter> filters = getStandardPersonnelFilters();
        filters.remove(ACTIVE);
        DefaultComboBoxModel<PersonnelFilter> filterModel = new DefaultComboBoxModel<>(filters.toArray(new PersonnelFilter[0]));
        roleComboBox = new MMComboBox<>("roleFilter");
        roleComboBox.setModel(filterModel);
        fbc.gridx = 1;
        filterPanel.add(roleComboBox, fbc);

        leftPanel.add(filterPanel, leftGbc);

        // === Right Column ===
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 0;
        rightGbc.weightx = 1.0;
        rightGbc.fill = GridBagConstraints.HORIZONTAL;
        rightGbc.anchor = GridBagConstraints.CENTER;
        int rightRow = 0;

        // Personnel Availability Label (Centered)
        rightGbc.gridy = rightRow++;
        rightGbc.insets = new Insets(0, 0, 8, 0);
        JLabel availabilityLabel = new JLabel("Personnel Availability");
        availabilityLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rightPanel.add(availabilityLabel, rightGbc);

        // Slider
        rightGbc.gridy = rightRow++;
        int recruitmentSliderMaximum = campaignOptions.getPersonnelMarketStyle() != NONE ?
                                             MAXIMUM_DAYS_IN_MONTH * MAXIMUM_NUMBER_OF_SYSTEM_ROLLS :
                                             MAXIMUM_DAYS_IN_MONTH;
        int recruitmentSliderCurrent = market.getRecruitmentRolls();
        JSlider personnelAvailabilitySlider = new JSlider(0, recruitmentSliderMaximum, recruitmentSliderCurrent);
        personnelAvailabilitySlider.setEnabled(false);
        rightPanel.add(personnelAvailabilitySlider, rightGbc);

        // Experience Label
        rightGbc.gridy = rightRow++;
        rightGbc.insets = new Insets(0, 0, 0, 0);
        rightPanel.add(new JLabel(getAvailabilityModifierMessage()), rightGbc);

        // === Place Panels ===
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainGbc.anchor = GridBagConstraints.NORTHWEST;
        mainGbc.fill = GridBagConstraints.BOTH;
        mainGbc.weightx = 0.5;
        mainGbc.weighty = 1.0;
        panel.add(leftPanel, mainGbc);

        mainGbc.gridx = 1;
        panel.add(rightPanel, mainGbc);

        return panel;
    }

    private String getAvailabilityModifierMessage() {
        String noAvailabilityMessage = market.getAvailabilityMessage();

        if (noAvailabilityMessage.isBlank() && campaignOptions.getUnitRatingMethod().isCampaignOperations()) {
            if (campaign.getReputation().getReputationRating() < market.getUnitReputationRecruitmentCutoff()) {
                noAvailabilityMessage = "Nobody likes you.";
            }
        }
        PlanetarySystem currentSystem = campaign.getCurrentSystem();
        LocalDate today = campaign.getLocalDate();

        if (noAvailabilityMessage.isBlank() &&
                  currentSystem.getPopulation(today) < market.getLowPopulationRecruitmentDivider()) {
            noAvailabilityMessage = "Reduced recruitment due to low population.";
        }
        return noAvailabilityMessage;
    }
}
