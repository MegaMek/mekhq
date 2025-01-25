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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

/**
 * Represents a tab in the campaign options UI for configuring relationship-related options,
 * such as marriage, divorce, and procreation settings.
 * <p>
 * This tab allows users to manage manual and random settings for the relationships
 * between personnel in a campaign, applying user-defined rules and configurations.
 * The class generates UI components for the respective configurations and interacts
 * with {@link CampaignOptions} to store and apply these settings.
 * </p>
 * <p>
 * The tab is divided into three main sections:
 * </p>
 * <ul>
 *     <li>Marriage Tab: Manages configurations for manual and random marriage settings.</li>
 *     <li>Divorce Tab: Manages configurations for manual and random divorce settings.</li>
 *     <li>Procreation Tab: Manages configurations for manual and random procreation settings.</li>
 * </ul>
 */
public class RelationshipsTab {
    private final CampaignOptions campaignOptions;

    //start Marriage Tab
    private JPanel pnlMarriageGeneralOptions;
    private JLabel lblNoInterestInMarriageDiceSize;
    private JSpinner spnNoInterestInMarriageDiceSize;

    private JPanel pnlRandomMarriage;
    private JLabel lblRandomMarriageMethod;
    private MMComboBox<RandomMarriageMethod> comboRandomMarriageMethod;
    private JCheckBox chkUseRandomClanPersonnelMarriages;
    private JLabel lblRandomMarriageOppositeSexDiceSize;
    private JSpinner spnRandomMarriageDiceSize;
    private JLabel lblRandomSameSexMarriageDiceSize;
    private JSpinner spnRandomSameSexMarriageDiceSize;
    private JLabel lblRandomNewDependentMarriage;
    private JSpinner spnRandomNewDependentMarriage;
    //end Marriage Tab

    //start Divorce Tab
    private JPanel pnlRandomDivorce;
    private JLabel lblRandomDivorceMethod;
    private MMComboBox<RandomDivorceMethod> comboRandomDivorceMethod;
    private JLabel lblRandomDivorceDiceSize;
    private JSpinner spnRandomDivorceDiceSize;
    //end Divorce Tab

    //start Procreation Tab
    private JLabel lblMultiplePregnancyOccurrences;
    private JSpinner spnMultiplePregnancyOccurrences;
    private JLabel lblBabySurnameStyle;
    private MMComboBox<BabySurnameStyle> comboBabySurnameStyle;
    private JCheckBox chkAssignNonPrisonerBabiesFounderTag;
    private JCheckBox chkAssignChildrenOfFoundersFounderTag;
    private JLabel lblNoInterestInChildrenDiceSize;
    private JSpinner spnNoInterestInChildrenDiceSize;
    private JCheckBox chkUseMaternityLeave;

    private JPanel pnlProcreationGeneralOptionsPanel;
    private JPanel pnlRandomProcreationPanel;
    private JLabel lblRandomProcreationMethod;
    private MMComboBox<RandomProcreationMethod> comboRandomProcreationMethod;
    private JCheckBox chkUseRandomClanPersonnelProcreation;
    private JCheckBox chkUseRandomPrisonerProcreation;
    private JLabel lblRandomProcreationRelationshipDiceSize;
    private JSpinner spnRandomProcreationRelationshipDiceSize;
    private JLabel lblRandomProcreationRelationshiplessDiceSize;
    private JSpinner spnRandomProcreationRelationshiplessDiceSize;
    //end Procreation Tab

    /**
     * Constructs a {@code RelationshipsTab} instance for configuring relationships-related campaign options.
     *
     * @param campaignOptions the {@link CampaignOptions} instance to be used for managing relationship settings.
     */
    public RelationshipsTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
    }

    /**
     * Initializes the various tabs within the RelationshipsTab, including Marriage, Divorce, and Procreation Tabs.
     */
    private void initialize() {
        initializeMarriageTab();
        initializeDivorceTab();
        initializeProcreationTab();
    }

    /**
     * Initializes the Procreation Tab and its components.
     * This tab controls general procreation settings and allows configuring random procreation options.
     */
    private void initializeProcreationTab() {
        pnlProcreationGeneralOptionsPanel = new JPanel();
        lblMultiplePregnancyOccurrences = new JLabel();
        spnMultiplePregnancyOccurrences = new JSpinner();
        lblBabySurnameStyle = new JLabel();
        comboBabySurnameStyle = new MMComboBox<>("comboBabySurnameStyle", BabySurnameStyle.values());
        chkAssignNonPrisonerBabiesFounderTag = new JCheckBox();
        chkAssignChildrenOfFoundersFounderTag = new JCheckBox();
        lblNoInterestInChildrenDiceSize = new JLabel();
        spnNoInterestInChildrenDiceSize = new JSpinner();
        chkUseMaternityLeave = new JCheckBox();

        pnlRandomProcreationPanel = new JPanel();
        lblRandomProcreationMethod = new JLabel();
        comboRandomProcreationMethod = new MMComboBox<>("comboRandomProcreationMethod",
            RandomProcreationMethod.values());
        chkUseRandomClanPersonnelProcreation = new JCheckBox();
        chkUseRandomPrisonerProcreation = new JCheckBox();
        lblRandomProcreationRelationshipDiceSize = new JLabel();
        spnRandomProcreationRelationshipDiceSize = new JSpinner();
        lblRandomProcreationRelationshiplessDiceSize = new JLabel();
        spnRandomProcreationRelationshiplessDiceSize = new JSpinner();
    }

    /**
     * Initializes the Divorce Tab and its components.
     * This tab controls general divorce settings and allows configuring random divorce options.
     */
    private void initializeDivorceTab() {
        pnlRandomDivorce = new JPanel();
        lblRandomDivorceMethod = new JLabel();
        comboRandomDivorceMethod = new MMComboBox<>("comboRandomDivorceMethod", RandomDivorceMethod.values());
        lblRandomDivorceDiceSize = new JLabel();
        spnRandomDivorceDiceSize = new JSpinner();
    }

    /**
     * Initializes the Marriage Tab and its components.
     * This tab controls general marriage settings and allows configuring random marriage options.
     */
    private void initializeMarriageTab() {
        pnlMarriageGeneralOptions = new JPanel();
        lblNoInterestInMarriageDiceSize = new JLabel();
        spnNoInterestInMarriageDiceSize = new JSpinner();

        pnlRandomMarriage = new JPanel();
        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod",
            RandomMarriageMethod.values());

        pnlRandomMarriage = new JPanel();
        lblRandomMarriageMethod = new JLabel();
        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod",
            RandomMarriageMethod.values());
        chkUseRandomClanPersonnelMarriages = new JCheckBox();

        lblRandomMarriageOppositeSexDiceSize = new JLabel();
        spnRandomMarriageDiceSize = new JSpinner();
        lblRandomSameSexMarriageDiceSize = new JLabel();
        spnRandomSameSexMarriageDiceSize = new JSpinner();
        lblRandomNewDependentMarriage = new JLabel();
        spnRandomNewDependentMarriage = new JSpinner();
    }

    /**
     * Creates the UI for the Marriage Tab, including components for managing manual and random marriage options.
     *
     * @return a {@link JPanel} representing the Marriage Tab.
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
     * Creates the panel for general marriage settings.
     * This panel includes controls like manual marriage toggles and ancestor checks.
     *
     * @return a {@link JPanel} containing general marriage options.
     */
    private JPanel createMarriageGeneralOptionsPanel() {
        // Contents
        lblNoInterestInMarriageDiceSize = new CampaignOptionsLabel("NoInterestInMarriageDiceSize");
        spnNoInterestInMarriageDiceSize = new CampaignOptionsSpinner("NoInterestInMarriageDiceSize",
            10, 1, 100000, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("MarriageGeneralOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblNoInterestInMarriageDiceSize, layout);
        layout.gridx++;
        panel.add(spnNoInterestInMarriageDiceSize, layout);

        return panel;
    }

    /**
     * Creates the panel for configuring random marriage settings.
     * Options include random clan marriages, prisoner marriages, and other random marriage rules.
     *
     * @return a {@link JPanel} containing random marriage settings.
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

        lblRandomMarriageOppositeSexDiceSize = new CampaignOptionsLabel("RandomMarriageOppositeSexDiceSize");
        spnRandomMarriageDiceSize = new CampaignOptionsSpinner("RandomMarriageOppositeSexDiceSize",
            5000, 0, 100000, 1);

        lblRandomSameSexMarriageDiceSize = new CampaignOptionsLabel("RandomSameSexMarriageDiceSize");
        spnRandomSameSexMarriageDiceSize = new CampaignOptionsSpinner("RandomSameSexMarriageDiceSize",
            14, 0, 100000, 1);

        lblRandomNewDependentMarriage = new CampaignOptionsLabel("RandomNewDependentMarriage");
        spnRandomNewDependentMarriage = new CampaignOptionsSpinner("RandomNewDependentMarriage",
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

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
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
     * Creates the UI for the Divorce Tab, including components for managing manual and random divorce options.
     *
     * @return a {@link JPanel} representing the Divorce Tab.
     */
    public JPanel createDivorceTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("DivorceTab",
            getImageDirectory() + "logo_escorpion_imperio.png");

        // Contents
        pnlRandomDivorce = createRandomDivorcePanel();

        // Layout the Panel
        final JPanel panelParent = new CampaignOptionsStandardPanel("DivorceTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(pnlRandomDivorce, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "DivorceTab");
    }

    /**
     * Creates the panel for configuring random divorce settings.
     * Options include toggles for random same-sex and opposite-sex divorces.
     *
     * @return a {@link JPanel} containing random divorce settings.
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
        panel.add(lblRandomDivorceDiceSize, layout);
        layout.gridx++;
        panel.add(spnRandomDivorceDiceSize, layout);

        return panel;
    }

    /**
     * Creates the UI for the Procreation Tab, including components for managing manual and random procreation options.
     *
     * @return a {@link JPanel} representing the Procreation Tab.
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
     * Creates the panel for general procreation settings.
     * This panel includes controls for determining maternity leave, surname styles, and logging options.
     *
     * @return a {@link JPanel} containing general procreation options.
     */
    private JPanel createProcreationGeneralOptionsPanel() {
        // Contents
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

        lblNoInterestInChildrenDiceSize = new CampaignOptionsLabel("NoInterestInChildrenDiceSize");
        spnNoInterestInChildrenDiceSize = new CampaignOptionsSpinner("NoInterestInChildrenDiceSize",
            3, 1, 100000, 1);

        chkUseMaternityLeave = new CampaignOptionsCheckBox("UseMaternityLeave");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ProcreationGeneralOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
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

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblNoInterestInChildrenDiceSize, layout);
        layout.gridx++;
        panel.add(spnNoInterestInChildrenDiceSize, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseMaternityLeave, layout);

        return panel;
    }

    /**
     * Creates the panel for configuring random procreation options.
     * Options include toggles for relationshipless procreation and dice settings.
     *
     * @return a {@link JPanel} containing random procreation settings.
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

    /**
     * Loads the default {@link CampaignOptions} values into the RelationshipsTab components.
     * This is a shortcut for calling {@link #loadValuesFromCampaignOptions(CampaignOptions)} with {@code null}.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads values from the specified {@link CampaignOptions} instance into the RelationshipsTab components.
     * If no custom options are provided, the current {@link CampaignOptions} instance is used.
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} to load. If {@code null}, default options are used.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Marriage
        spnNoInterestInMarriageDiceSize.setValue(options.getNoInterestInMarriageDiceSize());
        comboRandomMarriageMethod.setSelectedItem(options.getRandomMarriageMethod());
        chkUseRandomClanPersonnelMarriages.setSelected(options.isUseRandomClanPersonnelMarriages());
        spnRandomMarriageDiceSize.setValue(options.getRandomMarriageDiceSize());
        spnRandomSameSexMarriageDiceSize.setValue(options.getRandomSameSexMarriageDiceSize());
        spnRandomNewDependentMarriage.setValue(options.getRandomNewDependentMarriage());

        // Divorce
        comboRandomDivorceMethod.setSelectedItem(options.getRandomDivorceMethod());
        spnRandomDivorceDiceSize.setValue(options.getRandomDivorceDiceSize());

        // Procreation
        spnMultiplePregnancyOccurrences.setValue(options.getMultiplePregnancyOccurrences());
        comboBabySurnameStyle.setSelectedItem(options.getBabySurnameStyle());
        chkAssignNonPrisonerBabiesFounderTag.setSelected(options.isAssignNonPrisonerBabiesFounderTag());
        chkAssignChildrenOfFoundersFounderTag.setSelected(options.isAssignChildrenOfFoundersFounderTag());
        spnNoInterestInChildrenDiceSize.setValue(options.getNoInterestInChildrenDiceSize());
        chkUseMaternityLeave.setSelected(options.isUseMaternityLeave());
        comboRandomProcreationMethod.setSelectedItem(options.getRandomProcreationMethod());
        chkUseRandomClanPersonnelProcreation.setSelected(options.isUseRandomClanPersonnelProcreation());
        chkUseRandomPrisonerProcreation.setSelected(options.isUseRandomPrisonerProcreation());
        spnRandomProcreationRelationshipDiceSize.setValue(options.getRandomProcreationRelationshipDiceSize());
        spnRandomProcreationRelationshiplessDiceSize.setValue(options.getRandomProcreationRelationshiplessDiceSize());
    }

    /**
     * Applies the current settings from the RelationshipsTab components to the specified {@link CampaignOptions}.
     * If no custom options are provided, changes are applied to the current {@link CampaignOptions} instance.
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} to apply changes to. If {@code null}, default options are used.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Marriage
        options.setNoInterestInMarriageDiceSize((int) spnNoInterestInMarriageDiceSize.getValue());
        options.setRandomMarriageMethod(comboRandomMarriageMethod.getSelectedItem());
        options.setUseRandomClanPersonnelMarriages(chkUseRandomClanPersonnelMarriages.isSelected());
        options.setRandomMarriageDiceSize((int) spnRandomMarriageDiceSize.getValue());
        options.setRandomSameSexMarriageDiceSize((int) spnRandomSameSexMarriageDiceSize.getValue());
        options.setRandomNewDependentMarriage((int) spnRandomNewDependentMarriage.getValue());

        // Divorce
        options.setRandomDivorceMethod(comboRandomDivorceMethod.getSelectedItem());
        options.setRandomDivorceDiceSize((int) spnRandomDivorceDiceSize.getValue());

        // Procreation
        options.setMultiplePregnancyOccurrences((int) spnMultiplePregnancyOccurrences.getValue());
        options.setBabySurnameStyle(comboBabySurnameStyle.getSelectedItem());
        options.setAssignNonPrisonerBabiesFounderTag(chkAssignNonPrisonerBabiesFounderTag.isSelected());
        options.setAssignChildrenOfFoundersFounderTag(chkAssignChildrenOfFoundersFounderTag.isSelected());
        options.setNoInterestInChildrenDiceSize((int) spnNoInterestInChildrenDiceSize.getValue());
        options.setUseMaternityLeave(chkUseMaternityLeave.isSelected());
        options.setRandomProcreationMethod(comboRandomProcreationMethod.getSelectedItem());
        options.setUseRandomClanPersonnelProcreation(chkUseRandomClanPersonnelProcreation.isSelected());
        options.setUseRandomPrisonerProcreation(chkUseRandomPrisonerProcreation.isSelected());
        options.setRandomProcreationRelationshipDiceSize((int) spnRandomProcreationRelationshipDiceSize.getValue());
        options.setRandomProcreationRelationshiplessDiceSize((int) spnRandomProcreationRelationshiplessDiceSize.getValue());
    }
}
