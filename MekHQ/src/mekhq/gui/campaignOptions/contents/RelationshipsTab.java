/*
 * Copyright (c) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.campaignOptions.contents;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.enums.BabySurnameStyle;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;
import mekhq.gui.campaignOptions.components.*;

import javax.swing.*;
import java.awt.*;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.*;

public class RelationshipsTab {
    private final CampaignOptions campaignOptions;

    //start Marriage Tab
    private JPanel pnlMarriageGeneralOptions;
    private JCheckBox chkUseManualMarriages;
    private JCheckBox chkUseClanPersonnelMarriages;
    private JCheckBox chkUsePrisonerMarriages;
    private JLabel lblNoInterestInMarriageDiceSize;
    private JSpinner spnNoInterestInMarriageDiceSize;
    private JLabel lblCheckMutualAncestorsDepth;
    private JSpinner spnCheckMutualAncestorsDepth;
    private JCheckBox chkLogMarriageNameChanges;

    private JPanel pnlRandomMarriage;
    private JLabel lblRandomMarriageMethod;
    private MMComboBox<RandomMarriageMethod> comboRandomMarriageMethod;
    private JCheckBox chkUseRandomClanPersonnelMarriages;
    private JCheckBox chkUseRandomPrisonerMarriages;
    private JLabel lblRandomMarriageAgeRange;
    private JSpinner spnRandomMarriageAgeRange;
    private JLabel lblRandomMarriageOppositeSexDiceSize;
    private JSpinner spnRandomMarriageDiceSize;
    private JLabel lblRandomSameSexMarriageDiceSize;
    private JSpinner spnRandomSameSexMarriageDiceSize;
    private JLabel lblRandomNewDependentMarriage;
    private JSpinner spnRandomNewDependentMarriage;
    //end Marriage Tab

    //start Divorce Tab
    private JCheckBox chkUseManualDivorce;
    private JCheckBox chkUseClanPersonnelDivorce;
    private JCheckBox chkUsePrisonerDivorce;

    private JPanel pnlRandomDivorce;
    private JLabel lblRandomDivorceMethod;
    private MMComboBox<RandomDivorceMethod> comboRandomDivorceMethod;
    private JCheckBox chkUseRandomOppositeSexDivorce;
    private JCheckBox chkUseRandomSameSexDivorce;
    private JCheckBox chkUseRandomClanPersonnelDivorce;
    private JCheckBox chkUseRandomPrisonerDivorce;
    private JLabel lblRandomDivorceDiceSize;
    private JSpinner spnRandomDivorceDiceSize;
    //end Divorce Tab

    //start Procreation Tab
    private JCheckBox chkUseManualProcreation;
    private JCheckBox chkUseClanPersonnelProcreation;
    private JCheckBox chkUsePrisonerProcreation;
    private JLabel lblMultiplePregnancyOccurrences;
    private JSpinner spnMultiplePregnancyOccurrences;
    private JLabel lblBabySurnameStyle;
    private MMComboBox<BabySurnameStyle> comboBabySurnameStyle;
    private JCheckBox chkAssignNonPrisonerBabiesFounderTag;
    private JCheckBox chkAssignChildrenOfFoundersFounderTag;
    private JCheckBox chkDetermineFatherAtBirth;
    private JCheckBox chkDisplayTrueDueDate;
    private JLabel lblNoInterestInChildrenDiceSize;
    private JSpinner spnNoInterestInChildrenDiceSize;
    private JCheckBox chkUseMaternityLeave;
    private JCheckBox chkLogProcreation;

    private JPanel pnlProcreationGeneralOptionsPanel;
    private JPanel pnlRandomProcreationPanel;
    private JLabel lblRandomProcreationMethod;
    private MMComboBox<RandomProcreationMethod> comboRandomProcreationMethod;
    private JCheckBox chkUseRelationshiplessRandomProcreation;
    private JCheckBox chkUseRandomClanPersonnelProcreation;
    private JCheckBox chkUseRandomPrisonerProcreation;
    private JLabel lblRandomProcreationRelationshipDiceSize;
    private JSpinner spnRandomProcreationRelationshipDiceSize;
    private JLabel lblRandomProcreationRelationshiplessDiceSize;
    private JSpinner spnRandomProcreationRelationshiplessDiceSize;
    //end Procreation Tab

    public RelationshipsTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
    }

    /**
     * Calls the initialization methods for all the tab panels.
     */
    private void initialize() {
        initializeMarriageTab();
        initializeDivorceTab();
        initializeProcreationTab();
    }

    /**
     * Initializes the components of the ProcreationTab.
     * The panel contains settings related to character procreation in the simulation.
     */
    private void initializeProcreationTab() {
        pnlProcreationGeneralOptionsPanel = new JPanel();
        chkUseManualProcreation = new JCheckBox();
        chkUseClanPersonnelProcreation = new JCheckBox();
        chkUsePrisonerProcreation = new JCheckBox();
        lblMultiplePregnancyOccurrences = new JLabel();
        spnMultiplePregnancyOccurrences = new JSpinner();
        lblBabySurnameStyle = new JLabel();
        comboBabySurnameStyle = new MMComboBox<>("comboBabySurnameStyle", BabySurnameStyle.values());
        chkAssignNonPrisonerBabiesFounderTag = new JCheckBox();
        chkAssignChildrenOfFoundersFounderTag = new JCheckBox();
        chkDetermineFatherAtBirth = new JCheckBox();
        chkDisplayTrueDueDate = new JCheckBox();
        lblNoInterestInChildrenDiceSize = new JLabel();
        spnNoInterestInChildrenDiceSize = new JSpinner();
        chkUseMaternityLeave = new JCheckBox();
        chkLogProcreation = new JCheckBox();

        pnlRandomProcreationPanel = new JPanel();
        lblRandomProcreationMethod = new JLabel();
        comboRandomProcreationMethod = new MMComboBox<>("comboRandomProcreationMethod",
            RandomProcreationMethod.values());
        chkUseRelationshiplessRandomProcreation = new JCheckBox();
        chkUseRandomClanPersonnelProcreation = new JCheckBox();
        chkUseRandomPrisonerProcreation = new JCheckBox();
        lblRandomProcreationRelationshipDiceSize = new JLabel();
        spnRandomProcreationRelationshipDiceSize = new JSpinner();
        lblRandomProcreationRelationshiplessDiceSize = new JLabel();
        spnRandomProcreationRelationshiplessDiceSize = new JSpinner();
    }

    /**
     * Initializes the components of the DivorceTab.
     * The panel contains settings related to divorce mechanics within the simulation.
     */
    private void initializeDivorceTab() {
        chkUseManualDivorce = new JCheckBox();
        chkUseClanPersonnelDivorce = new JCheckBox();
        chkUsePrisonerDivorce = new JCheckBox();

        pnlRandomDivorce = new JPanel();
        lblRandomDivorceMethod = new JLabel();
        comboRandomDivorceMethod = new MMComboBox<>("comboRandomDivorceMethod", RandomDivorceMethod.values());
        chkUseRandomOppositeSexDivorce = new JCheckBox();
        chkUseRandomSameSexDivorce = new JCheckBox();
        chkUseRandomClanPersonnelDivorce = new JCheckBox();
        chkUseRandomPrisonerDivorce = new JCheckBox();
        lblRandomDivorceDiceSize = new JLabel();
        spnRandomDivorceDiceSize = new JSpinner();
    }

    /**
     * Initializes the components of the MarriageTab.
     * The panel contains various settings related to marriage mechanics within the simulation.
     */
    private void initializeMarriageTab() {
        pnlMarriageGeneralOptions = new JPanel();
        chkUseManualMarriages = new JCheckBox();
        chkUseClanPersonnelMarriages = new JCheckBox();
        chkUsePrisonerMarriages = new JCheckBox();
        lblNoInterestInMarriageDiceSize = new JLabel();
        spnNoInterestInMarriageDiceSize = new JSpinner();
        lblCheckMutualAncestorsDepth = new JLabel();
        spnCheckMutualAncestorsDepth = new JSpinner();
        chkLogMarriageNameChanges = new JCheckBox();

        pnlRandomMarriage = new JPanel();
        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod",
            RandomMarriageMethod.values());

        pnlRandomMarriage = new JPanel();
        lblRandomMarriageMethod = new JLabel();
        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod",
            RandomMarriageMethod.values());
        chkUseRandomClanPersonnelMarriages = new JCheckBox();
        chkUseRandomPrisonerMarriages = new JCheckBox();
        lblRandomMarriageAgeRange = new JLabel();
        spnRandomMarriageAgeRange = new JSpinner();

        lblRandomMarriageOppositeSexDiceSize = new JLabel();
        spnRandomMarriageDiceSize = new JSpinner();
        lblRandomSameSexMarriageDiceSize = new JLabel();
        spnRandomSameSexMarriageDiceSize = new JSpinner();
        lblRandomNewDependentMarriage = new JLabel();
        spnRandomNewDependentMarriage = new JSpinner();
    }

    /**
     * Creates a panel for the Marriage tab with various input components and panels related to marriage settings.
     *
     * @return a {@link} representing the Marriage tab with checkboxes for manual, clan personnel,
     * prisoner marriages, options for marriage characteristics, logging marriage name changes, surname
     * weight settings, and random marriage generation.
     */
    public JPanel createMarriageTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("MarriageTab",
            getImageDirectory() + "logo_morgrains_valkyrate.png");

        // Contents
        pnlMarriageGeneralOptions = createMarriageGeneralOptionsPanel();
        pnlRandomMarriage = createRandomMarriagePanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("MarriageTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(pnlMarriageGeneralOptions, layoutParent);

        layoutParent.gridx++;
        panel.add(pnlRandomMarriage, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panel, "MarriageTab");
    }

    /**
     * Creates a panel for general marriage options with checkboxes and input components.
     *
     * @return a {@link JPanel} representing the general marriage options panel
     */
    private JPanel createMarriageGeneralOptionsPanel() {
        // Contents
        chkUseManualMarriages = new CampaignOptionsCheckBox("UseManualMarriages");
        chkUseClanPersonnelMarriages = new CampaignOptionsCheckBox("UseClanPersonnelMarriages");
        chkUsePrisonerMarriages = new CampaignOptionsCheckBox("UsePrisonerMarriages");

        lblNoInterestInMarriageDiceSize = new CampaignOptionsLabel("NoInterestInMarriageDiceSize");
        spnNoInterestInMarriageDiceSize = new CampaignOptionsSpinner("NoInterestInMarriageDiceSize",
            10, 1, 100000, 1);

        lblCheckMutualAncestorsDepth = new CampaignOptionsLabel("CheckMutualAncestorsDepth");
        spnCheckMutualAncestorsDepth = new CampaignOptionsSpinner("CheckMutualAncestorsDepth",
            4, 0, 20, 1);

        chkLogMarriageNameChanges = new CampaignOptionsCheckBox("LogMarriageNameChanges");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("MarriageGeneralOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(chkUseManualMarriages, layout);

        layout.gridy++;
        panel.add(chkUseClanPersonnelMarriages, layout);

        layout.gridy++;
        panel.add(chkUsePrisonerMarriages, layout);

        layout.gridy++;
        panel.add(lblNoInterestInMarriageDiceSize, layout);
        layout.gridx++;
        panel.add(spnNoInterestInMarriageDiceSize, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblCheckMutualAncestorsDepth, layout);
        layout.gridx++;
        panel.add(spnCheckMutualAncestorsDepth, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkLogMarriageNameChanges, layout);

        return panel;
    }

    /**
     * Creates a panel for random marriage settings, including options for different marriage methods,
     * using random clan personnel and prisoner marriages, setting age range for marriages, and
     * percentage settings.
     *
     * @return a {@link JPanel} representing the random marriage panel with various input components
     * and panels for configuring random marriage settings
     */
    private JPanel createRandomMarriagePanel() {
        // Contents
        lblRandomMarriageMethod = new CampaignOptionsLabel("RandomMarriageMethod");
        comboRandomMarriageMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomMarriageMethod) {
                    list.setToolTipText(((RandomMarriageMethod) value).getToolTipText());
                }
                return this;
            }
        });

        chkUseRandomClanPersonnelMarriages = new CampaignOptionsCheckBox("UseRandomClanPersonnelMarriages");
        chkUseRandomPrisonerMarriages = new CampaignOptionsCheckBox("UseRandomPrisonerMarriages");

        lblRandomMarriageAgeRange = new CampaignOptionsLabel("RandomMarriageAgeRange");
        spnRandomMarriageAgeRange = new CampaignOptionsSpinner("RandomMarriageAgeRange",
            10, 0, 100, 1);

        lblRandomMarriageOppositeSexDiceSize = new CampaignOptionsLabel("RandomMarriageOppositeSexDiceSize");
        spnRandomMarriageDiceSize = new CampaignOptionsSpinner("RandomMarriageOppositeSexDiceSize",
            5000, 0, 100000, 1);

        lblRandomSameSexMarriageDiceSize = new CampaignOptionsLabel("RandomSameSexMarriageDiceSize");
        spnRandomSameSexMarriageDiceSize = new CampaignOptionsSpinner("RandomSameSexMarriageDiceSize",
            14, 0, 100000, 1);

        lblRandomNewDependentMarriage = new CampaignOptionsLabel("RandomNewDependentMarriage");
        spnRandomNewDependentMarriage = new CampaignOptionsSpinner("RandomSameSexMarriageDiceSize",
            20, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("RandomMarriages", true,
            "RandomMarriages");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblRandomMarriageMethod, layout);
        layout.gridx++;
        panel.add(comboRandomMarriageMethod, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkUseRandomClanPersonnelMarriages, layout);

        layout.gridy++;
        panel.add(chkUseRandomPrisonerMarriages, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblRandomMarriageAgeRange, layout);
        layout.gridx++;
        panel.add(spnRandomMarriageAgeRange, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblRandomMarriageOppositeSexDiceSize, layout);
        layout.gridx++;
        panel.add(spnRandomMarriageDiceSize, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblRandomSameSexMarriageDiceSize, layout);
        layout.gridx++;
        panel.add(spnRandomSameSexMarriageDiceSize, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblRandomNewDependentMarriage, layout);
        layout.gridx++;
        panel.add(spnRandomNewDependentMarriage, layout);

        return panel;
    }

    /**
     * Creates a tab for divorce settings with various checkboxes and panels for manual, clan personnel,
     * and prisoner divorces.
     *
     * @return a {@link JPanel} representing the Divorce tab with checkboxes for manual divorce,
     * clan personnel divorce, prisoner divorce, and a panel for configuring random divorce settings.
     */
    public JPanel createDivorceTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("DivorceTab",
            getImageDirectory() + "logo_escorpion_imperio.png");

        // Contents
        chkUseManualDivorce = new CampaignOptionsCheckBox("UseManualDivorce");
        chkUseClanPersonnelDivorce = new CampaignOptionsCheckBox("UseClanPersonnelDivorce");
        chkUsePrisonerDivorce = new CampaignOptionsCheckBox("UsePrisonerDivorce");

        pnlRandomDivorce = createRandomDivorcePanel();

        // Layout the Panel
        final JPanel panelLeft = new CampaignOptionsStandardPanel("DivorceTabLeft");
        final GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridwidth = 1;
        layoutLeft.gridx = 0;
        layoutLeft.gridy = 0;
        panelLeft.add(chkUseManualDivorce, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(chkUseClanPersonnelDivorce, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(chkUsePrisonerDivorce, layoutLeft);

        final JPanel panelParent = new CampaignOptionsStandardPanel("DivorceTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);

        layoutParent.gridx++;
        panelParent.add(pnlRandomDivorce, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "DivorceTab");
    }

    /**
     * Creates a panel for the Divorce tab with checkboxes for manual divorce, clan personnel divorce,
     * prisoner divorce, and a panel for configuring random divorce settings.
     *
     * @return a {@link JPanel} representing the Divorce tab with various components for configuring divorce settings
     */
    private JPanel createRandomDivorcePanel() {
        // Contents
        lblRandomDivorceMethod = new CampaignOptionsLabel("RandomDivorceMethod");
        comboRandomDivorceMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomDivorceMethod) {
                    list.setToolTipText(((RandomDivorceMethod) value).getToolTipText());
                }
                return this;
            }
        });

        chkUseRandomOppositeSexDivorce = new CampaignOptionsCheckBox("UseRandomOppositeSexDivorce");
        chkUseRandomSameSexDivorce = new CampaignOptionsCheckBox("UseRandomSameSexDivorce");
        chkUseRandomClanPersonnelDivorce = new CampaignOptionsCheckBox("UseRandomClanPersonnelDivorce");
        chkUseRandomPrisonerDivorce = new CampaignOptionsCheckBox("UseRandomPrisonerDivorce");

        lblRandomDivorceDiceSize = new CampaignOptionsLabel("RandomDivorceDiceSize");
        spnRandomDivorceDiceSize = new CampaignOptionsSpinner("RandomDivorceDiceSize",
            900, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("RandomDivorcePanel", true,
            "RandomDivorcePanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblRandomDivorceMethod, layout);
        layout.gridx++;
        panel.add(comboRandomDivorceMethod, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkUseRandomOppositeSexDivorce, layout);

        layout.gridy++;
        panel.add(chkUseRandomSameSexDivorce, layout);

        layout.gridy++;
        panel.add(chkUseRandomClanPersonnelDivorce, layout);

        layout.gridy++;
        panel.add(chkUseRandomPrisonerDivorce, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblRandomDivorceDiceSize, layout);
        layout.gridx++;
        panel.add(spnRandomDivorceDiceSize, layout);

        return panel;
    }

    /**
     * Creates a panel for the Procreation tab with header, general options panel, and random procreation panel.
     *
     * @return a {@link JPanel} representing the Procreation tab with header, general options panel,
     * and random procreation panel
     */
    public JPanel createProcreationTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("ProcreationTab",
            getImageDirectory() + "logo_hanseatic_league.png");

        // Contents
        pnlProcreationGeneralOptionsPanel = createProcreationGeneralOptionsPanel();
        pnlRandomProcreationPanel = createRandomProcreationPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ProcreationTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(pnlProcreationGeneralOptionsPanel, layoutParent);

        layoutParent.gridx++;
        panel.add(pnlRandomProcreationPanel, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panel, "ProcreationTab");
    }

    /**
     * @return the {@link JPanel} containing the procreation settings components
     */
    private JPanel createProcreationGeneralOptionsPanel() {
        // Contents
        chkUseManualProcreation = new CampaignOptionsCheckBox("UseManualProcreation");
        chkUseClanPersonnelProcreation = new CampaignOptionsCheckBox("UseClanPersonnelProcreation");
        chkUsePrisonerProcreation = new CampaignOptionsCheckBox("UsePrisonerProcreation");

        lblMultiplePregnancyOccurrences = new CampaignOptionsLabel("MultiplePregnancyOccurrences");
        spnMultiplePregnancyOccurrences = new CampaignOptionsSpinner("MultiplePregnancyOccurrences",
            50, 1, 1000, 1);

        lblBabySurnameStyle = new CampaignOptionsLabel("BabySurnameStyle");
        comboBabySurnameStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BabySurnameStyle) {
                    list.setToolTipText(((BabySurnameStyle) value).getToolTipText());
                }
                return this;
            }
        });

        chkAssignNonPrisonerBabiesFounderTag = new CampaignOptionsCheckBox("AssignNonPrisonerBabiesFounderTag");
        chkAssignChildrenOfFoundersFounderTag = new CampaignOptionsCheckBox("AssignChildrenOfFoundersFounderTag");
        chkDetermineFatherAtBirth = new CampaignOptionsCheckBox("DetermineFatherAtBirth");
        chkDisplayTrueDueDate = new CampaignOptionsCheckBox("DisplayTrueDueDate");

        lblNoInterestInChildrenDiceSize = new CampaignOptionsLabel("NoInterestInChildrenDiceSize");
        spnNoInterestInChildrenDiceSize = new CampaignOptionsSpinner("NoInterestInChildrenDiceSize",
            3, 1, 100000, 1);

        chkUseMaternityLeave = new CampaignOptionsCheckBox("UseMaternityLeave");
        chkLogProcreation = new CampaignOptionsCheckBox("LogProcreation");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ProcreationGeneralOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(chkUseManualProcreation, layout);

        layout.gridy++;
        panel.add(chkUseClanPersonnelProcreation, layout);

        layout.gridy++;
        panel.add(chkUsePrisonerProcreation, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblMultiplePregnancyOccurrences, layout);
        layout.gridx++;
        panel.add(spnMultiplePregnancyOccurrences, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblBabySurnameStyle, layout);
        layout.gridx++;
        panel.add(comboBabySurnameStyle, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkAssignNonPrisonerBabiesFounderTag, layout);

        layout.gridy++;
        panel.add(chkAssignChildrenOfFoundersFounderTag, layout);

        layout.gridy++;
        panel.add(chkDetermineFatherAtBirth, layout);

        layout.gridy++;
        panel.add(chkDisplayTrueDueDate, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblNoInterestInChildrenDiceSize, layout);
        layout.gridx++;
        panel.add(spnNoInterestInChildrenDiceSize, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseMaternityLeave, layout);

        layout.gridy++;
        panel.add(chkLogProcreation, layout);

        return panel;
    }

    /**
     * @return a {@link JPanel} containing the configured components for random procreation settings
     */
    private JPanel createRandomProcreationPanel() {
        // Contents
        lblRandomProcreationMethod = new CampaignOptionsLabel("RandomProcreationMethod");
        comboRandomProcreationMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomProcreationMethod) {
                    list.setToolTipText(((RandomProcreationMethod) value).getToolTipText());
                }
                return this;
            }
        });

        chkUseRelationshiplessRandomProcreation =new CampaignOptionsCheckBox("UseRelationshiplessRandomProcreation");
        chkUseRandomClanPersonnelProcreation = new CampaignOptionsCheckBox("UseRandomClanPersonnelProcreation");
        chkUseRandomPrisonerProcreation = new CampaignOptionsCheckBox("UseRandomPrisonerProcreation");

        lblRandomProcreationRelationshipDiceSize = new CampaignOptionsLabel("RandomProcreationRelationshipDiceSize");
        spnRandomProcreationRelationshipDiceSize = new CampaignOptionsSpinner("RandomProcreationRelationshipDiceSize",
            621, 0, 100000, 1);

        lblRandomProcreationRelationshiplessDiceSize = new CampaignOptionsLabel("RandomProcreationRelationshiplessDiceSize");
        spnRandomProcreationRelationshiplessDiceSize = new CampaignOptionsSpinner("RandomProcreationRelationshiplessDiceSize",
            1861, 0, 100000, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("RandomProcreationPanel", true,
            "RandomProcreationPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblRandomProcreationMethod, layout);
        layout.gridx++;
        panel.add(comboRandomProcreationMethod, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkUseRelationshiplessRandomProcreation, layout);

        layout.gridy++;
        panel.add(chkUseRandomClanPersonnelProcreation, layout);

        layout.gridy++;
        panel.add(chkUseRandomPrisonerProcreation, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblRandomProcreationRelationshipDiceSize, layout);
        layout.gridx++;
        panel.add(spnRandomProcreationRelationshipDiceSize, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblRandomProcreationRelationshiplessDiceSize, layout);
        layout.gridx++;
        panel.add(spnRandomProcreationRelationshiplessDiceSize, layout);

        return panel;
    }

    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Marriage
        chkUseManualMarriages.setSelected(options.isUseManualMarriages());
        chkUseClanPersonnelMarriages.setSelected(options.isUseClanPersonnelMarriages());
        chkUsePrisonerMarriages.setSelected(options.isUsePrisonerMarriages());
        spnNoInterestInMarriageDiceSize.setValue(options.getNoInterestInMarriageDiceSize());
        spnCheckMutualAncestorsDepth.setValue(options.getCheckMutualAncestorsDepth());
        chkLogMarriageNameChanges.setSelected(options.isLogMarriageNameChanges());
        comboRandomMarriageMethod.setSelectedItem(options.getRandomMarriageMethod());
        chkUseRandomClanPersonnelMarriages.setSelected(options.isUseRandomClanPersonnelMarriages());
        chkUseRandomPrisonerMarriages.setSelected(options.isUsePrisonerMarriages());
        spnRandomMarriageAgeRange.setValue(options.getRandomMarriageAgeRange());
        spnRandomMarriageDiceSize.setValue(options.getRandomMarriageDiceSize());
        spnRandomSameSexMarriageDiceSize.setValue(options.getRandomSameSexMarriageDiceSize());
        spnRandomNewDependentMarriage.setValue(options.getRandomNewDependentMarriage());

        // Divorce
        chkUseManualDivorce.setSelected(options.isUseManualDivorce());
        chkUseClanPersonnelDivorce.setSelected(options.isUseClanPersonnelDivorce());
        chkUsePrisonerDivorce.setSelected(options.isUsePrisonerDivorce());
        comboRandomDivorceMethod.setSelectedItem(options.getRandomDivorceMethod());
        chkUseRandomOppositeSexDivorce.setSelected(options.isUseRandomOppositeSexDivorce());
        chkUseRandomSameSexDivorce.setSelected(options.isUseRandomSameSexDivorce());
        chkUseRandomClanPersonnelDivorce.setSelected(options.isUseClanPersonnelDivorce());
        chkUseRandomPrisonerDivorce.setSelected(options.isUsePrisonerDivorce());
        spnRandomDivorceDiceSize.setValue(options.getRandomDivorceDiceSize());

        // Procreation
        chkUseManualProcreation.setSelected(options.isUseManualProcreation());
        chkUseClanPersonnelProcreation.setSelected(options.isUseClanPersonnelProcreation());
        chkUsePrisonerProcreation.setSelected(options.isUsePrisonerProcreation());
        spnMultiplePregnancyOccurrences.setValue(options.getMultiplePregnancyOccurrences());
        comboBabySurnameStyle.setSelectedItem(options.getBabySurnameStyle());
        chkAssignNonPrisonerBabiesFounderTag.setSelected(options.isAssignNonPrisonerBabiesFounderTag());
        chkAssignChildrenOfFoundersFounderTag.setSelected(options.isAssignChildrenOfFoundersFounderTag());
        chkDetermineFatherAtBirth.setSelected(options.isDetermineFatherAtBirth());
        chkDisplayTrueDueDate.setSelected(options.isDisplayTrueDueDate());
        spnNoInterestInChildrenDiceSize.setValue(options.getNoInterestInChildrenDiceSize());
        chkUseMaternityLeave.setSelected(options.isUseMaternityLeave());
        chkLogProcreation.setSelected(options.isLogProcreation());
        comboRandomProcreationMethod.setSelectedItem(options.getRandomProcreationMethod());
        chkUseRelationshiplessRandomProcreation.setSelected(options.isUseRelationshiplessRandomProcreation());
        chkUseRandomClanPersonnelProcreation.setSelected(options.isUseRandomClanPersonnelProcreation());
        chkUseRandomPrisonerProcreation.setSelected(options.isUseRandomPrisonerProcreation());
        spnRandomProcreationRelationshipDiceSize.setValue(options.getRandomProcreationRelationshipDiceSize());
        spnRandomProcreationRelationshiplessDiceSize.setValue(options.getRandomProcreationRelationshiplessDiceSize());
    }

    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Marriage
        options.setUseManualMarriages(chkUseManualMarriages.isSelected());
        options.setUseClanPersonnelMarriages(chkUseClanPersonnelMarriages.isSelected());
        options.setUsePrisonerMarriages(chkUsePrisonerMarriages.isSelected());
        options.setNoInterestInMarriageDiceSize((int) spnNoInterestInMarriageDiceSize.getValue());
        options.setCheckMutualAncestorsDepth((int) spnCheckMutualAncestorsDepth.getValue());
        options.setLogMarriageNameChanges(chkLogMarriageNameChanges.isSelected());
        options.setRandomMarriageMethod(comboRandomMarriageMethod.getSelectedItem());
        options.setUseRandomClanPersonnelMarriages(chkUseRandomClanPersonnelMarriages.isSelected());
        options.setUseRandomPrisonerMarriages(chkUseRandomPrisonerMarriages.isSelected());
        options.setRandomMarriageAgeRange((int) spnRandomMarriageAgeRange.getValue());
        options.setRandomMarriageDiceSize((int) spnRandomMarriageDiceSize.getValue());
        options.setRandomSameSexMarriageDiceSize((int) spnRandomSameSexMarriageDiceSize.getValue());
        options.setRandomNewDependentMarriage((int) spnRandomNewDependentMarriage.getValue());

        // Divorce
        options.setUseManualDivorce(chkUseManualDivorce.isSelected());
        options.setUseClanPersonnelDivorce(chkUseClanPersonnelDivorce.isSelected());
        options.setUsePrisonerDivorce(chkUsePrisonerDivorce.isSelected());
        options.setRandomDivorceMethod(comboRandomDivorceMethod.getSelectedItem());
        options.setUseRandomOppositeSexDivorce(chkUseRandomOppositeSexDivorce.isSelected());
        options.setUseRandomSameSexDivorce(chkUseRandomSameSexDivorce.isSelected());
        options.setUseRandomClanPersonnelDivorce(chkUseRandomClanPersonnelDivorce.isSelected());
        options.setUseRandomPrisonerDivorce(chkUseRandomPrisonerDivorce.isSelected());
        options.setRandomDivorceDiceSize((int) spnRandomDivorceDiceSize.getValue());

        // Procreation
        options.setUseManualProcreation(chkUseManualProcreation.isSelected());
        options.setUseClanPersonnelProcreation(chkUseClanPersonnelProcreation.isSelected());
        options.setUsePrisonerProcreation(chkUsePrisonerProcreation.isSelected());
        options.setMultiplePregnancyOccurrences((int) spnMultiplePregnancyOccurrences.getValue());
        options.setBabySurnameStyle(comboBabySurnameStyle.getSelectedItem());
        options.setAssignNonPrisonerBabiesFounderTag(chkAssignNonPrisonerBabiesFounderTag.isSelected());
        options.setAssignChildrenOfFoundersFounderTag(chkAssignChildrenOfFoundersFounderTag.isSelected());
        options.setDetermineFatherAtBirth(chkDetermineFatherAtBirth.isSelected());
        options.setDisplayTrueDueDate(chkDisplayTrueDueDate.isSelected());
        options.setNoInterestInChildrenDiceSize((int) spnNoInterestInChildrenDiceSize.getValue());
        options.setUseMaternityLeave(chkUseMaternityLeave.isSelected());
        options.setLogProcreation(chkLogProcreation.isSelected());
        options.setRandomProcreationMethod(comboRandomProcreationMethod.getSelectedItem());
        options.setUseRelationshiplessRandomProcreation(chkUseRelationshiplessRandomProcreation.isSelected());
        options.setUseRandomClanPersonnelProcreation(chkUseRandomClanPersonnelProcreation.isSelected());
        options.setUseRandomPrisonerProcreation(chkUseRandomPrisonerProcreation.isSelected());
        options.setRandomProcreationRelationshipDiceSize((int) spnRandomProcreationRelationshipDiceSize.getValue());
        options.setRandomProcreationRelationshiplessDiceSize((int) spnRandomProcreationRelationshiplessDiceSize.getValue());
    }
}
