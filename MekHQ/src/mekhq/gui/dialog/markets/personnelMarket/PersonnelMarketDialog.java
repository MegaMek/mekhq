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

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.finances.enums.TransactionType.RECRUITMENT;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.MEKHQ;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.NONE;
import static mekhq.gui.enums.PersonnelFilter.ACTIVE;
import static mekhq.gui.enums.PersonnelFilter.ALL;
import static mekhq.gui.enums.PersonnelFilter.getStandardPersonnelFilters;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

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
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.enums.PersonnelFilter;
import mekhq.gui.view.PersonViewPanel;

public class PersonnelMarketDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PersonnelMarket";

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

    public PersonnelMarketDialog(NewPersonnelMarket market) {
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
        JDialog dialog = new JDialog(parent);
        setDialogTitle(dialog);
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
        dialog.setModal(true);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private void setDialogTitle(JDialog dialog) {
        Faction campaignFaction = campaign.getFaction();
        if (campaignFaction.isClan()) {
            dialog.setTitle(getTextAt(RESOURCE_BUNDLE, "title.personnelMarket.clan"));
        } else if (campaignFaction.isComStarOrWoB()) {
            Person commander = campaign.getFlaggedCommander();
            String address = commander != null ? commander.getTitleAndSurname() : campaign.getCommanderAddress(false);
            dialog.setTitle(getFormattedTextAt(RESOURCE_BUNDLE,
                  "title.personnelMarket.comStarOrWoB",
                  address.toUpperCase()));
        } else if (campaignFaction.isMercenary()) {
            dialog.setTitle(getTextAt(RESOURCE_BUNDLE, "title.personnelMarket.mercenary"));
        } else if (campaignFaction.isMercenary()) {
            dialog.setTitle(getTextAt(RESOURCE_BUNDLE, "title.personnelMarket.normal"));
        }
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
        JButton hireButton = new JButton(getTextAt(RESOURCE_BUNDLE, "button.personnelMarket.hire.normal"));
        hireButton.addActionListener(e -> hireActionListener(isGM));
        buttonPanel.add(hireButton);

        JButton addGMButton = new JButton(getTextAt(RESOURCE_BUNDLE, "button.personnelMarket.hire.gm"));
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
                campaign.getFinances().debit(RECRUITMENT,
                      campaign.getLocalDate(),
                            applicant.getSalary(campaign).multipliedBy(12),
                      getFormattedTextAt(RESOURCE_BUNDLE, "finances.personnelMarket.hire", applicant.getFullTitle()));
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

    private JPanel initializeTipPanel() {
        JLabel infoLabel = new JLabel(getTipMessage());
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(infoLabel, BorderLayout.CENTER);
        return bottomPanel;
    }

    private String getTipMessage() {
        if (market.getAssociatedPersonnelMarketStyle() == MEKHQ) {
            return getTextAt(RESOURCE_BUNDLE, "hint.personnelMarket." + randomInt(10));
        }

        return getTextAt(RESOURCE_BUNDLE, "hint.personnelMarket.0");
    }

    private PersonnelTablePanel initializeTablePanel() {
        PersonnelTablePanel tablePanel = new PersonnelTablePanel(campaign, currentApplicants);

        JTable personnelTable = tablePanel.getTable();
        if (personnelTable.getRowSorter() instanceof TableRowSorter<?> sorter) {
            roleComboBox.addActionListener(ev -> {
                PersonnelFilter selectedFilter = roleComboBox.getSelectedItem();
                if (selectedFilter == null) {
                    selectedFilter = ALL;
                } else {
                    market.setLastSelectedFilter(roleComboBox.getSelectedIndex());
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
        JCheckBox goldenHelloCheckbox = new JCheckBox(getTextAt(RESOURCE_BUNDLE,
              "checkbox.personnelMarket.goldenHello"));
        goldenHelloCheckbox.setSelected(market.isOfferingGoldenHello());
        goldenHelloCheckbox.setEnabled(market.getAssociatedPersonnelMarketStyle() == MEKHQ);
        leftPanel.add(goldenHelloCheckbox, leftGbc);

        // Role ComboBox (Label + ComboBox)
        leftGbc.gridy = leftRow++;
        leftGbc.insets = new Insets(0, 0, 0, 0);

        JPanel filterPanel = initializeFilter();
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
        JLabel availabilityLabel = new JLabel(getTextAt(RESOURCE_BUNDLE, "label.personnelMarket.availability"));
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

    private JPanel initializeFilter() {
        JPanel filterPanel = new JPanel(new GridBagLayout());
        GridBagConstraints fbc = new GridBagConstraints();
        fbc.gridx = 0;
        fbc.gridy = 0;
        fbc.anchor = GridBagConstraints.WEST;
        filterPanel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "label.personnelMarket.filter")), fbc);

        List<PersonnelFilter> filters = getStandardPersonnelFilters();
        filters.remove(ACTIVE);
        DefaultComboBoxModel<PersonnelFilter> filterModel = new DefaultComboBoxModel<>(filters.toArray(new PersonnelFilter[0]));
        roleComboBox = new MMComboBox<>("roleFilter");
        roleComboBox.setModel(filterModel);
        try {
            roleComboBox.setSelectedIndex(market.getLastSelectedFilter());
        } catch (Exception e) {
            // This will happen if we remove a filter choice from the combo, and that happens to be the filter the
            // player last chose.
            market.setLastSelectedFilter(0);
        }
        fbc.gridx = 1;
        filterPanel.add(roleComboBox, fbc);
        return filterPanel;
    }

    private String getAvailabilityModifierMessage() {
        String noAvailabilityMessage = market.getAvailabilityMessage();
        String color;
        String closingBrace = CLOSING_SPAN_TAG;

        if (noAvailabilityMessage.isBlank() && campaignOptions.getUnitRatingMethod().isCampaignOperations()) {
            if (campaign.getReputation().getReputationRating() < market.getUnitReputationRecruitmentCutoff()) {
                color = MekHQ.getMHQOptions().getFontColorWarningHexColor();

                noAvailabilityMessage = getFormattedTextAt(RESOURCE_BUNDLE,
                      "hint.personnelMarket.reputation",
                      spanOpeningWithCustomColor(color),
                      closingBrace);
            }
        }
        PlanetarySystem currentSystem = campaign.getCurrentSystem();
        LocalDate today = campaign.getLocalDate();

        if (noAvailabilityMessage.isBlank() &&
                  currentSystem.getPopulation(today) < market.getLowPopulationRecruitmentDivider()) {
            color = MekHQ.getMHQOptions().getFontColorWarningHexColor();

            noAvailabilityMessage = getFormattedTextAt(RESOURCE_BUNDLE,
                  "hint.personnelMarket.population",
                  spanOpeningWithCustomColor(color),
                  closingBrace);
        }
        return noAvailabilityMessage;
    }
}
