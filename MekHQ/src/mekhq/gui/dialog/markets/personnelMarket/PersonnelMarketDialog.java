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

import static java.lang.Math.min;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.finances.enums.TransactionType.RECRUITMENT;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.MEKHQ;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED;
import static mekhq.gui.enums.PersonnelFilter.ACTIVE;
import static mekhq.gui.enums.PersonnelFilter.ALL;
import static mekhq.gui.enums.PersonnelFilter.getStandardPersonnelFilters;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.utilities.glossary.DocumentationEntry;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.AdvanceDaysDialog;
import mekhq.gui.dialog.glossary.NewDocumentationEntryDialog;
import mekhq.gui.enums.PersonnelFilter;
import mekhq.gui.view.PersonViewPanel;

/**
 * Provides the main dialog window for interacting with the personnel market.
 *
 * <p>This class handles the GUI elements and core logic for displaying available applicants, filtering options, and
 * handling personnel hiring activities. It integrates with campaign data and market configurations and supports a
 * variety of customization and localization features.
 *
 * <p>Features:</p>
 * <ul>
 *     <li>Displays applicant data in a table with sorting/filtering</li>
 *     <li>Provides detail views for each applicant</li>
 *     <li>Integrates hiring functionality, including "Golden Hello" options</li>
 *     <li>Shows dynamic messages and tooltips related to personnel market states</li>
 *     <li>Supports filtering by personnel role and custom campaign-defined settings</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <ol>
 *     <li>Instantiate with the relevant market model, campaign, and parent frame.</li>
 *     <li>Call {@code initializeComponents()} before use to construct and arrange UI elements.</li>
 *     <li>Handles user actions for hiring or inspecting applicants directly in the GUI.</li>
 * </ol>
 *
 * <p>Notable Constants:</p>
 * <ul>
 *     <li>{@link #MAXIMUM_DAYS_IN_MONTH}: Used for calendar-related UI logic.</li>
 *     <li>{@link #MAXIMUM_NUMBER_OF_SYSTEM_ROLLS}: Defines maximum rolls that can be gained from system status.</li>
 * </ul>
 *
 * <p>Private implementation includes utility and helper methods for constructing the dialog's layout, handling
 * events, and providing dynamic UI feedback.</p>
 *
 * @author Illiani
 * @since 0.50.06
 */
public class PersonnelMarketDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(PersonnelMarketDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PersonnelMarket";

    private static final int MAXIMUM_DAYS_IN_MONTH = 31;
    private static final int MAXIMUM_NUMBER_OF_SYSTEM_ROLLS = 4;

    private final int PADDING = scaleForGUI(5);
    private final Dimension PERSON_VIEW_MINIMUM_SIZE = scaleForGUI(700, 500);

    private final NewPersonnelMarket market;
    private final JFrame parent;
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;

    private final List<Person> currentApplicants;
    private MMComboBox<PersonnelFilter> roleComboBox = new MMComboBox<>("roleFilter");
    private final JCheckBox goldenHelloCheckbox = new JCheckBox();
    private PersonnelTablePanel tablePanel;
    private PersonViewPanel personViewPanel;

    /**
     * Constructs a new PersonnelMarketDialog.
     *
     * @param market the personnel market logic backing this dialog
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonnelMarketDialog(NewPersonnelMarket market) {
        this.market = market;
        this.campaign = market.getCampaign();
        this.campaignOptions = campaign.getCampaignOptions();
        this.parent = campaign.getApp().getCampaigngui().getFrame();
        this.currentApplicants = market.getCurrentApplicants();

        initializeComponents();
    }

    /**
     * Initializes and arranges all GUI components for the dialog.
     *
     * <p>Should be called before displaying the dialog.</p>
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void initializeComponents() {
        setDialogTitle();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeAction();
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
        pnlLeft.add(tablePanel, BorderLayout.CENTER);

        JPanel pnlTips = initializeTipPanel();
        pnlLeft.add(pnlTips, BorderLayout.SOUTH);

        AtomicReference<Person> selectedPerson = new AtomicReference<>();
        if (!currentApplicants.isEmpty()) {
            selectedPerson.set(tablePanel.getSelectedApplicants().get(0));
        }

        // This handles the initializing and display of the applicant panel
        JSplitPane splitPane = initializePersonView(selectedPerson, mainPanel);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        // Finalize the dialog
        setModal(true);
        pack();
        setLocationRelativeTo(parent);
        setPreferences(this); // Must be before setVisible
        setVisible(true); // Should always be last
    }

    /**
     * Applies the current UI settings to the {@link NewPersonnelMarket} object and closes the dialog.
     *
     * <p>This method updates the market's "golden hello" offering status and the list of current applicants based on
     * the user selections, then disposes of the dialog.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void closeAction() {
        market.setOfferingGoldenHello(goldenHelloCheckbox.isSelected());
        market.setCurrentApplicants(currentApplicants);
        dispose();
    }

    private void documentationAction() {
        DocumentationEntry documentationEntry = DocumentationEntry.RECRUITMENT;

        try {
            new NewDocumentationEntryDialog(this, documentationEntry);
        } catch (Exception ex) {
            LOGGER.error("Failed to open PDF", ex);
        }
    }


    /**
     * Creates and returns the header panel for the personnel market dialog.
     *
     * @return a {@link JPanel} representing the dialog header
     *
     * @author Illiani
     * @since 0.50.06
     */
    private JPanel initializeHeader() {
        JPanel panel = new JPanel(new GridBagLayout());

        // === Left Column ===
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0;
        leftGbc.weightx = 1.0;
        leftGbc.fill = GridBagConstraints.NONE;
        leftGbc.anchor = GridBagConstraints.WEST;

        // Golden Hello Checkbox
        leftGbc.insets = new Insets(0, 0, PADDING, 0);
        goldenHelloCheckbox.setText(getTextAt(RESOURCE_BUNDLE,
              "checkbox.personnelMarket.goldenHello"));
        goldenHelloCheckbox.setSelected(market.isOfferingGoldenHello());
        goldenHelloCheckbox.setEnabled(market.getAssociatedPersonnelMarketStyle() == MEKHQ);
        leftPanel.add(goldenHelloCheckbox, leftGbc);

        // Role ComboBox (Label + ComboBox)
        leftGbc.insets = new Insets(0, 0, 0, 0);

        JPanel filterPanel = initializeFilter();
        leftPanel.add(filterPanel, leftGbc);

        // === Right Column ===
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 0;
        rightGbc.weightx = 1.0;
        rightGbc.fill = GridBagConstraints.HORIZONTAL;
        rightGbc.anchor = GridBagConstraints.CENTER;

        // Personnel Availability Label (Centered)
        rightGbc.insets = new Insets(0, 0, PADDING, 0);
        JLabel availabilityLabel = new JLabel(getTextAt(RESOURCE_BUNDLE, "label.personnelMarket.availability"));
        availabilityLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rightPanel.add(availabilityLabel, rightGbc);

        // Slider
        JSlider personnelAvailabilitySlider = getPersonnelAvailabilitySlider();
        rightPanel.add(personnelAvailabilitySlider, rightGbc);

        // Experience Label
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

    private JSlider getPersonnelAvailabilitySlider() {
        int recruitmentSliderMaximum = campaignOptions.getPersonnelMarketStyle() != PERSONNEL_MARKET_DISABLED ?
                                             MAXIMUM_DAYS_IN_MONTH * MAXIMUM_NUMBER_OF_SYSTEM_ROLLS :
                                             MAXIMUM_DAYS_IN_MONTH;
        int recruitmentSliderCurrent = min(market.getRecruitmentRolls(), recruitmentSliderMaximum);
        JSlider personnelAvailabilitySlider = new JSlider(0, recruitmentSliderMaximum, recruitmentSliderCurrent);
        personnelAvailabilitySlider.setEnabled(false);
        return personnelAvailabilitySlider;
    }


    /**
     * Creates and returns the filter panel containing controls for applicant filtering.
     *
     * @return a {@link JPanel} containing filter controls
     *
     * @author Illiani
     * @since 0.50.06
     */
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

    /**
     * Creates and returns the button panel for the dialog.
     *
     * @return a {@link JPanel} with action buttons
     *
     * @author Illiani
     * @since 0.50.06
     */
    private JPanel initializeButtonPanel() {
        boolean isGM = campaign.isGM();

        // Top row panel
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));
        RoundedJButton btnClose = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.personnelMarket.close"));
        btnClose.addActionListener(e -> closeAction());
        topRow.add(btnClose);

        RoundedJButton btnHire = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.personnelMarket.hire.normal"));
        btnHire.addActionListener(e -> hireActionListener(false));
        topRow.add(btnHire);

        RoundedJButton btnAdvanceMultipleDays = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "button.personnelMarket.advanceDays"));
        btnAdvanceMultipleDays.addActionListener(e -> {
            closeAction(); // Close old instance
            AdvanceDaysDialog advanceDaysDialog = new AdvanceDaysDialog(parent, campaign.getApp().getCampaigngui());
            advanceDaysDialog.setVisible(true);
            advanceDaysDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    new PersonnelMarketDialog(market); // Open a new instance (to ensure the market is refreshed)
                }
            });
        });
        topRow.add(btnAdvanceMultipleDays);

        // Bottom row panel
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));
        RoundedJButton btnDocumentation = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "button.personnelMarket.documentation"));
        btnDocumentation.addActionListener(e -> documentationAction());
        bottomRow.add(btnDocumentation);

        RoundedJButton btnGMHire = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.personnelMarket.hire.gm"));
        btnGMHire.addActionListener(e -> hireActionListener(true));
        btnGMHire.setEnabled(isGM);
        bottomRow.add(btnGMHire);

        RoundedJButton btnGMAdd = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "button.personnelMarket.add.gm"));
        btnGMAdd.addActionListener(e -> addApplicantActionListener());
        btnGMAdd.setEnabled(isGM);
        bottomRow.add(btnGMAdd);

        // Parent panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(topRow);
        buttonPanel.add(bottomRow);

        return buttonPanel;
    }

    /**
     * Creates and returns the tip panel, displaying context-sensitive help or advice.
     *
     * @return a {@link JPanel} showing dynamic tips
     *
     * @author Illiani
     * @since 0.50.06
     */
    private JPanel initializeTipPanel() {
        JLabel infoLabel = new JLabel(getTipMessage());
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        bottomPanel.add(infoLabel, BorderLayout.CENTER);
        return bottomPanel;
    }

    /**
     * Initializes and returns the personnel table panel displaying market applicants.
     *
     * @return a configured {@link PersonnelTablePanel} instance
     *
     * @author Illiani
     * @since 0.50.06
     */
    private PersonnelTablePanel initializeTablePanel() {
        PersonnelTablePanel tablePanel = new PersonnelTablePanel(campaign, currentApplicants);

        JTable personnelTable = tablePanel.getTable();
        personnelTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SwingUtilities.invokeLater(() -> {
                    List<Person> selected = tablePanel.getSelectedApplicants();
                    Person selectedPerson = selected.isEmpty() ? null : selected.get(0);
                    personViewPanel.setPerson(selectedPerson);
                });
            }
        });

        if (personnelTable.getRowSorter() instanceof TableRowSorter<?> sorter) {
            filterRoles(sorter);
            roleComboBox.addActionListener(ev -> filterRoles(sorter));
        }
        return tablePanel;
    }

    /**
     * Applies filtering logic to the given table row sorter based on the selected role filter.
     *
     * @param sorter the {@link TableRowSorter} to which the filtering logic is applied
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void filterRoles(TableRowSorter<?> sorter) {
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
    }

    /**
     * Initializes and returns the detail view pane for the selected person.
     *
     * @param selectedPerson reference to the selected person
     * @param mainPanel      main panel where the split pane is embedded
     *
     * @return a configured {@link JSplitPane} for applicant details
     *
     * @author Illiani
     * @since 0.50.06
     */
    private JSplitPane initializePersonView(AtomicReference<Person> selectedPerson, JPanel mainPanel) {
        personViewPanel = new PersonViewPanel(selectedPerson.get(), campaign, campaign.getApp().getCampaigngui());
        JScrollPane viewScrollPane = new JScrollPane(personViewPanel);
        viewScrollPane.setMinimumSize(PERSON_VIEW_MINIMUM_SIZE);
        viewScrollPane.setBorder(null);
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

    /**
     * Performs the hiring action for the selected applicant.
     *
     * @param isGMHire whether the hire action is performed as a GM Hire
     *
     * @author Illiani
     * @since 0.50.06
     */
    private void hireActionListener(boolean isGMHire) {
        List<Person> recruitedPersons = new ArrayList<>(tablePanel.getSelectedApplicants());

        // Process recruitment and golden hello logic for all selected applicants
        for (Person applicant : recruitedPersons) {
            if (!isGMHire && market.isWasOfferingGoldenHello()) {
                // Personnel are hired without rank, meaning they have a 0.5 salary multiplier. As a Golden Hello is
                // 12 months' salary, we double the multiplier from 12 to 24.
                Money cost = applicant.getSalary(campaign).multipliedBy(24);

                campaign.getFinances()
                      .debit(RECRUITMENT,
                            campaign.getLocalDate(),
                            cost,
                            getFormattedTextAt(RESOURCE_BUNDLE,
                                  "finances.personnelMarket.hire",
                                  applicant.getFullTitle()));
            }
            campaign.recruitPerson(applicant, isGMHire, true);
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

    /**
     * Handles the process of adding a fresh applicant to the applicant pool.
     *
     * <p>This method attempts to create a single applicant for the personnel market. If no applicant is available,
     * it reports an error message to the campaign log. Otherwise, the applicant is added to the list of current
     * applicants, the table view in the user interface is refreshed to reflect the change, and any existing table
     * selection is cleared.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void addApplicantActionListener() {
        Person applicant = market.getSingleApplicant();
        if (applicant == null) {
            campaign.addReport(getTextAt(RESOURCE_BUNDLE, "button.personnelMarket.add.gm.error"));
            return;
        }

        currentApplicants.add(applicant);

        // Refresh the table view (notify the model of data changes)
        AbstractTableModel model = (AbstractTableModel) tablePanel.getTable().getModel();
        model.fireTableDataChanged();

        // Clear selection in the table
        tablePanel.getTable().clearSelection();

        int rowCount = model.getRowCount();
        if (rowCount > 0) {
            if (rowCount == 1) { // Only 1 applicant in the table
                tablePanel.getTable().setRowSelectionInterval(0, 0); // Select the first (and only) row
            }
            personViewPanel.setVisible(true);
        }
    }

    /**
     * Sets the dialog's title based on market and campaign context.
     *
     * @author Illiani
     * @since 0.50.06
     */
    private void setDialogTitle() {
        Faction campaignFaction = campaign.getFaction();
        if (campaignFaction.isClan()) {
            setTitle(getTextAt(RESOURCE_BUNDLE, "title.personnelMarket.clan"));
        } else if (campaignFaction.isComStarOrWoB()) {
            Person commander = campaign.getCommander();
            String address = commander != null ? commander.getTitleAndSurname() : campaign.getCommanderAddress(false);
            setTitle(getFormattedTextAt(RESOURCE_BUNDLE,
                  "title.personnelMarket.comStarOrWoB",
                  address.toUpperCase()));
        } else if (campaignFaction.isMercenary()) {
            setTitle(getTextAt(RESOURCE_BUNDLE, "title.personnelMarket.mercenary"));
        } else {
            setTitle(getTextAt(RESOURCE_BUNDLE, "title.personnelMarket.normal"));
        }
    }


    /**
     * Returns a context-specific tip message for display.
     *
     * @return tip message as a {@link String}
     *
     * @author Illiani
     * @since 0.50.06
     */
    private String getTipMessage() {
        if (market.getAssociatedPersonnelMarketStyle() == MEKHQ) {
            return getTextAt(RESOURCE_BUNDLE, "hint.personnelMarket." + randomInt(11));
        }

        return getTextAt(RESOURCE_BUNDLE, "hint.personnelMarket.0");
    }

    /**
     * Computes and returns a message about applicant availability modifiers.
     *
     * @return availability modifier message as a {@link String}
     *
     * @author Illiani
     * @since 0.50.06
     */
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

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences(JDialog dialog) {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(PersonnelMarketDialog.class);
            dialog.setName("PersonnelMarketDialog");
            preferences.manage(new JWindowPreference(dialog));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
