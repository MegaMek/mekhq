/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.contents;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.formatBadges;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.gui.baseComponents.MHQCollapsiblePanel;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.CampaignOptionsMetadata;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * The {@code SystemsTab} class is responsible for managing and displaying the
 * "Reputation" tab within the campaign
 * options UI. It provides Swing components for configuring reputation-related
 * settings in a {@link Campaign}, including
 * unit rating methods, manual modifiers, and various campaign mechanics
 * toggles.
 *
 * <p>
 * The tab is constructed using helper panels and controls, grouped under
 * "General" and "Sanity" sub-panels,
 * facilitating a user-friendly way to view and adjust reputation options.
 * </p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class SystemsTab {
    private static final int FORM_LABEL_COLUMN_WIDTH = 220;
    private static final int FORM_CONTROL_COLUMN_WIDTH = 220;
    private static final int CHECKBOX_GRID_COLUMNS = 2;

    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final RandomSkillPreferences randomSkillPreferences;
    private SystemsOptionsModel model;
    private boolean reputationPageCreated;
    private boolean factionStandingPageCreated;
    private boolean atowPageCreated;

    // Reputation Tab
    private CampaignOptionsHeaderPanel reputationHeader;

    private JButton btnResetCriminalRecord;

    private JSpinner manualUnitRatingModifier;
    private JCheckBox chkClampReputationPayMultiplier;
    private JCheckBox chkReduceReputationPerformanceModifier;
    private JCheckBox chkReputationPerformanceModifierCutOff;

    // Faction Standing Tab
    private CampaignOptionsHeaderPanel factionStandingHeader;
    private JCheckBox chkTrackFactionStanding;
    private JCheckBox chkTrackClimateRegardChanges;
    private JSpinner spnRegardMultiplier;

    private JCheckBox chkUseFactionStandingNegotiation;
    private JCheckBox chkUseFactionStandingResupply;
    private JCheckBox chkUseFactionStandingCommandCircuit;
    private JCheckBox chkUseFactionStandingOutlawed;
    private JCheckBox chkUseFactionStandingBatchallRestrictions;
    private JCheckBox chkUseFactionStandingRecruitment;
    private JCheckBox chkUseFactionStandingBarracksCosts;
    private JCheckBox chkUseFactionStandingUnitMarket;
    private JCheckBox chkUseFactionStandingContractPay;
    private JCheckBox chkUseFactionStandingSupportPoints;

    // A Time of War Tab
    private CampaignOptionsHeaderPanel atowHeader;

    private JCheckBox chkUseAttributes;
    private JCheckBox chkRandomizeAttributes;
    private JCheckBox chkDisplayAllAttributes;
    private JCheckBox chkUseAgeEffects;
    private JCheckBox chkRandomizeTraits;
    private JCheckBox chkAllowMonthlyReinvestment;
    private JCheckBox chkAllowMonthlyConnections;
    private JCheckBox chkUseBetterExtraIncome;
    private JCheckBox chkUseSmallArmsOnly;

    /**
     * Constructs a new {@code SystemsTab} for the specified campaign.
     *
     * @param campaign the campaign associated with this tab
     *
     * @author Illiani
     * @since 0.50.07
     */
    public SystemsTab(Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();
        this.randomSkillPreferences = campaign.getRandomSkillPreferences();
        loadValuesFromCampaignOptions();
    }

    /**
     * Creates the Reputation tab panel, containing grouped UI elements for
     * reputation options and its header.
     *
     * @return a {@link JPanel} component representing the entire Reputation tab UI
     *
     * @author Illiani
     * @since 0.50.07
     */
    public JPanel createReputationTab() {
        // Header
        reputationHeader = new CampaignOptionsHeaderPanel("ReputationTab",
                getImageDirectory() + "logo_morgrains_valkyrate.png",
                8);

        // Contents
        JPanel pnlReputationGeneralOptions = createReputationGeneralPanel();
        JPanel pnlReputationSanityOptions = createReputationSanityPanel();
        MHQCollapsiblePanel generalSection = createSection("lblReputationGeneralOptionsPanel.text",
                "lblReputationGeneralOptionsPanel.summary",
                pnlReputationGeneralOptions);
        MHQCollapsiblePanel safeguardsSection = createSection("lblReputationSanityOptionsPanel.text",
                "lblReputationSanityOptionsPanel.summary",
                pnlReputationSanityOptions);

        reputationPageCreated = true;
        updateReputationControlsFromModel();

        // Layout the Panel
        final JPanel panel = createSectionedPanel("ReputationTab",
                reputationHeader,
                generalSection,
                safeguardsSection);

        // Create Parent Panel and return
        return createParentPanel(panel, "ReputationTab");
    }

    /**
     * Creates and lays out the general reputation options panel, including controls
     * for selecting unit rating method,
     * manual modifiers, and criminal record reset.
     *
     * @return a {@link JPanel} containing the general reputation controls
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createReputationGeneralPanel() {
        // Contents
        JLabel lblManualUnitRatingModifier = new CampaignOptionsLabel("ManualUnitRatingModifier");
        lblManualUnitRatingModifier.addMouseListener(createTipPanelUpdater(reputationHeader,
                "ManualUnitRatingModifier"));
        manualUnitRatingModifier = new CampaignOptionsSpinner("ManualUnitRatingModifier", 0, -1000, 1000, 1);
        manualUnitRatingModifier
                .addMouseListener(createTipPanelUpdater(reputationHeader, "ManualUnitRatingModifier"));

        JLabel lblResetCriminalRecord = new CampaignOptionsLabel("ResetCriminalRecord",
                getResetCriminalRecordMetadata());
        lblResetCriminalRecord.addMouseListener(createTipPanelUpdater(reputationHeader, "ResetCriminalRecord"));
        btnResetCriminalRecord = createResetCriminalRecordButton();

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ReputationGeneralOptionsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblManualUnitRatingModifier, manualUnitRatingModifier);
        panel.addRow(lblResetCriminalRecord, createLeftAlignedButtonPanel(btnResetCriminalRecord));

        return panel;
    }

    private JButton createResetCriminalRecordButton() {
        JButton button = new JButton(getTextAt(getCampaignOptionsResourceBundle(), "btnResetCriminalRecord.text"));
        button.setName("btnResetCriminalRecord");
        button.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(), "lblResetCriminalRecord.tooltip"));
        button.putClientProperty("JComponent.sizeVariant", "small");
        button.addMouseListener(createTipPanelUpdater(reputationHeader, "ResetCriminalRecord"));
        button.addActionListener(event -> {
            if (model != null) {
                model.resetCriminalRecord = true;
            }
            updateResetCriminalRecordButtonFromModel();
        });
        return button;
    }

    private JPanel createLeftAlignedButtonPanel(JButton button) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.add(button);
        return panel;
    }

    /**
     * Creates and lays out the reputation "sanity" options panel, which includes
     * various checkboxes for limiting or
     * modifying reputation calculations.
     *
     * @return a {@link JPanel} containing the reputation sanity option controls
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createReputationSanityPanel() {
        // Contents
        chkClampReputationPayMultiplier = new CampaignOptionsCheckBox("ClampReputationPayMultiplier",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.RECOMMENDED));
        chkClampReputationPayMultiplier.addMouseListener(createTipPanelUpdater(reputationHeader,
                "ClampReputationPayMultiplier"));

        chkReduceReputationPerformanceModifier = new CampaignOptionsCheckBox("ReduceReputationPerformanceModifier",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.RECOMMENDED));
        chkReduceReputationPerformanceModifier.addMouseListener(createTipPanelUpdater(reputationHeader,
                "ReduceReputationPerformanceModifier"));

        chkReputationPerformanceModifierCutOff = new CampaignOptionsCheckBox("ReputationPerformanceModifierCutOff",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.RECOMMENDED));
        chkReputationPerformanceModifierCutOff.addMouseListener(createTipPanelUpdater(reputationHeader,
                "ReputationPerformanceModifierCutOff"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ReputationSanityOptionsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkClampReputationPayMultiplier,
                chkReduceReputationPerformanceModifier,
                chkReputationPerformanceModifierCutOff);

        return panel;
    }

    /**
     * Creates the Faction Standing tab panel, containing grouped UI elements for
     * Faction Standing options and its
     * header.
     *
     * @return a {@link JPanel} component representing the entire Faction Standing
     *         tab UI
     *
     * @author Illiani
     * @since 0.50.07
     */
    public JPanel createFactionStandingTab() {
        // Header
        factionStandingHeader = new CampaignOptionsHeaderPanel("FactionStandingTab",
                getImageDirectory() + "logo_morgrains_valkyrate.png",
                3);

        // Contents
        JPanel pnlFactionStandingTrackingPanel = createFactionStandingTrackingPanel();
        JPanel pnlFactionStandingModifiersPanel = createFactionStandingModifiersPanel();
        MHQCollapsiblePanel trackingSection = createSection("lblFactionStandingTrackingPanel.text",
                "lblFactionStandingTrackingPanel.summary",
                pnlFactionStandingTrackingPanel);
        MHQCollapsiblePanel effectsSection = createSection("lblFactionStandingEffectsPanel.text",
                "lblFactionStandingEffectsPanel.summary",
                pnlFactionStandingModifiersPanel);

        factionStandingPageCreated = true;
        updateFactionStandingControlsFromModel();

        // Layout the Panel
        final JPanel panel = createSectionedPanel("FactionStandingTab",
                factionStandingHeader,
                trackingSection,
                effectsSection);

        // Create Parent Panel and return
        return createParentPanel(panel, "FactionStandingTab");
    }

    private JPanel createFactionStandingTrackingPanel() {
        // Contents
        chkTrackFactionStanding = new CampaignOptionsCheckBox("TrackFactionStanding",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.DOCUMENTED));
        chkTrackFactionStanding
                .addMouseListener(createTipPanelUpdater(factionStandingHeader, "TrackFactionStanding"));

        chkTrackClimateRegardChanges = new CampaignOptionsCheckBox("TrackClimateRegardChanges",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkTrackClimateRegardChanges.addMouseListener(createTipPanelUpdater(factionStandingHeader,
                "TrackClimateRegardChanges"));

        JLabel lblRegardMultiplier = new CampaignOptionsLabel("RegardMultiplier",
                getMetadata(MILESTONE_BEFORE_METADATA));
        lblRegardMultiplier.addMouseListener(createTipPanelUpdater(factionStandingHeader, "RegardMultiplier"));
        spnRegardMultiplier = new CampaignOptionsSpinner("RegardMultiplier", 1.0, 0.1, 3.0, 0.1);
        spnRegardMultiplier.addMouseListener(createTipPanelUpdater(factionStandingHeader, "RegardMultiplier"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("FactionStandingTrackingPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkTrackFactionStanding,
                chkTrackClimateRegardChanges);
        panel.addRow(lblRegardMultiplier, spnRegardMultiplier);

        return panel;
    }

    /**
     * Creates and lays out the Faction Standing modifiers panel, which includes
     * various checkboxes for limiting Faction
     * Standing modifiers.
     *
     * @return a {@link JPanel} containing the Faction Standing modifier controls
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createFactionStandingModifiersPanel() {
        // Contents
        chkUseFactionStandingNegotiation = new CampaignOptionsCheckBox("UseFactionStandingNegotiation",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingNegotiation.addMouseListener(createTipPanelUpdater(factionStandingHeader,
                "UseFactionStandingNegotiation"));

        chkUseFactionStandingResupply = new CampaignOptionsCheckBox("UseFactionStandingResupply",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingResupply.addMouseListener(createTipPanelUpdater(factionStandingHeader,
                "UseFactionStandingResupply"));

        chkUseFactionStandingCommandCircuit = new CampaignOptionsCheckBox("UseFactionStandingCommandCircuit",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingCommandCircuit.addMouseListener(createTipPanelUpdater(factionStandingHeader,
                "UseFactionStandingCommandCircuit"));

        chkUseFactionStandingOutlawed = new CampaignOptionsCheckBox("UseFactionStandingOutlawed",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingOutlawed.addMouseListener(createTipPanelUpdater(factionStandingHeader,
                "UseFactionStandingOutlawed"));

        chkUseFactionStandingBatchallRestrictions = new CampaignOptionsCheckBox(
                "UseFactionStandingBatchallRestrictions",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingBatchallRestrictions.addMouseListener(createTipPanelUpdater(factionStandingHeader,
                "UseFactionStandingBatchallRestrictions"));

        chkUseFactionStandingRecruitment = new CampaignOptionsCheckBox("UseFactionStandingRecruitment",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingRecruitment.addMouseListener(createTipPanelUpdater(factionStandingHeader,
                "UseFactionStandingRecruitment"));

        chkUseFactionStandingBarracksCosts = new CampaignOptionsCheckBox("UseFactionStandingBarracksCosts",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingBarracksCosts.addMouseListener(createTipPanelUpdater(factionStandingHeader,
                "UseFactionStandingBarracksCosts"));

        chkUseFactionStandingUnitMarket = new CampaignOptionsCheckBox("UseFactionStandingUnitMarket",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingUnitMarket.addMouseListener(createTipPanelUpdater(factionStandingHeader,
                "UseFactionStandingUnitMarket"));

        chkUseFactionStandingContractPay = new CampaignOptionsCheckBox("UseFactionStandingContractPay",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingContractPay.addMouseListener(createTipPanelUpdater(factionStandingHeader,
                "UseFactionStandingContractPay"));

        chkUseFactionStandingSupportPoints = new CampaignOptionsCheckBox("UseFactionStandingSupportPoints",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseFactionStandingSupportPoints.addMouseListener(createTipPanelUpdater(factionStandingHeader,
                "UseFactionStandingSupportPoints"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("FactionStandingEffectsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkUseFactionStandingNegotiation,
                chkUseFactionStandingResupply,
                chkUseFactionStandingCommandCircuit,
                chkUseFactionStandingOutlawed,
                chkUseFactionStandingBatchallRestrictions,
                chkUseFactionStandingRecruitment,
                chkUseFactionStandingBarracksCosts,
                chkUseFactionStandingUnitMarket,
                chkUseFactionStandingContractPay,
                chkUseFactionStandingSupportPoints);

        return panel;
    }

    /**
     * Creates the ATOW tab panel, containing grouped UI elements for configuring
     * ATOW-related options and its header.
     *
     * @return a {@link JPanel} component representing the entire ATOW tab UI
     *
     * @author Illiani
     * @since 0.50.07
     */
    public JPanel createATOWTab() {
        // Header
        atowHeader = new CampaignOptionsHeaderPanel("ATimeOfWarTab",
                getImageDirectory() + "logo_elysian_fields.png",
                8);

        // Contents
        JPanel pnlATOWAttributes = createATOWAttributesPanel();
        MHQCollapsiblePanel attributesSection = createSection("lblATOWAttributesPanel.text",
                "lblATOWAttributesPanel.summary",
                pnlATOWAttributes);

        atowPageCreated = true;
        updateATOWControlsFromModel();

        // Layout the Panel
        final JPanel panel = createSectionedPanel("ATimeOfWarTab",
                atowHeader,
                attributesSection);

        // Create Parent Panel and return
        return createParentPanel(panel, "ATimeOfWarTab");
    }

    /**
     * Creates and returns the ATOW panel, which allows users to configure settings
     * for attribute and traits
     * probabilities.
     *
     * @return A {@code JPanel} containing configuration options for phenotype
     *         probabilities.
     */
    private JPanel createATOWAttributesPanel() {
        // Contents
        chkUseAttributes = new CampaignOptionsCheckBox("UseAttributes",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkUseAttributes.addMouseListener(createTipPanelUpdater(atowHeader, "UseAttributes"));
        chkRandomizeAttributes = new CampaignOptionsCheckBox("RandomizeAttributes");
        chkRandomizeAttributes.addMouseListener(createTipPanelUpdater(atowHeader, "RandomizeAttributes"));
        chkDisplayAllAttributes = new CampaignOptionsCheckBox("DisplayAllAttributes",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkDisplayAllAttributes.addMouseListener(createTipPanelUpdater(atowHeader, "DisplayAllAttributes"));
        chkUseAgeEffects = new CampaignOptionsCheckBox("UseAgeEffects",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseAgeEffects.addMouseListener(createTipPanelUpdater(atowHeader, "UseAgeEffects"));
        chkRandomizeTraits = new CampaignOptionsCheckBox("RandomizeTraits");
        chkRandomizeTraits.addMouseListener(createTipPanelUpdater(atowHeader, "RandomizeTraits"));
        chkAllowMonthlyReinvestment = new CampaignOptionsCheckBox("AllowMonthlyReinvestment");
        chkAllowMonthlyReinvestment.addMouseListener(createTipPanelUpdater(atowHeader,
                "AllowMonthlyReinvestment"));
        chkAllowMonthlyConnections = new CampaignOptionsCheckBox("AllowMonthlyConnections",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkAllowMonthlyConnections.addMouseListener(createTipPanelUpdater(atowHeader,
                "AllowMonthlyConnections"));
        chkUseBetterExtraIncome = new CampaignOptionsCheckBox("UseBetterExtraIncome",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseBetterExtraIncome.addMouseListener(createTipPanelUpdater(atowHeader,
                "UseBetterExtraIncome"));
        chkUseSmallArmsOnly = new CampaignOptionsCheckBox("UseSmallArmsOnly",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseSmallArmsOnly.addMouseListener(createTipPanelUpdater(atowHeader,
                "UseSmallArmsOnly"));

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ATOWAttributesPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkUseAttributes,
                chkRandomizeAttributes,
                chkDisplayAllAttributes,
                chkUseAgeEffects,
                chkRandomizeTraits,
                chkAllowMonthlyReinvestment,
                chkAllowMonthlyConnections,
                chkUseBetterExtraIncome,
                chkUseSmallArmsOnly);

        return panel;
    }

    private JPanel createSectionedPanel(String name, CampaignOptionsHeaderPanel header,
            MHQCollapsiblePanel... sections) {
        JPanel sectionControls = createSectionControls(sections);

        final JPanel panel = new CampaignOptionsStandardPanel(name);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        layout.weightx = 1.0;
        panel.add(header, layout);

        layout.gridy++;
        layout.anchor = GridBagConstraints.EAST;
        panel.add(sectionControls, layout);

        layout.anchor = GridBagConstraints.NORTHWEST;
        for (MHQCollapsiblePanel section : sections) {
            layout.gridy++;
            panel.add(section, layout);
        }

        return panel;
    }

    private MHQCollapsiblePanel createSection(String titleKey, String summaryKey, JPanel content) {
        return createSection(titleKey, summaryKey, content, null);
    }

    private MHQCollapsiblePanel createSection(String titleKey, String summaryKey, JPanel content,
            @Nullable CampaignOptionsMetadata metadata) {
        MHQCollapsiblePanel section = new MHQCollapsiblePanel(getSectionTitle(titleKey, metadata), content);
        section.setSummary(getTextAt(getCampaignOptionsResourceBundle(), summaryKey));
        return section;
    }

    private String getSectionTitle(String titleKey, @Nullable CampaignOptionsMetadata metadata) {
        String title = getTextAt(getCampaignOptionsResourceBundle(), titleKey);
        String badges = formatBadges(metadata);
        if (badges.isBlank()) {
            return title;
        }
        return "<html>" + title + badges + "</html>";
    }

    private JPanel createSectionControls(MHQCollapsiblePanel... sections) {
        JButton expandAllButton = createSectionActionButton("btnExpandAll.text");
        expandAllButton.addActionListener(event -> setExpanded(true, sections));
        JButton collapseAllButton = createSectionActionButton("btnCollapseAll.text");
        collapseAllButton.addActionListener(event -> setExpanded(false, sections));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controls.setOpaque(false);
        controls.add(expandAllButton);
        controls.add(collapseAllButton);

        return controls;
    }

    private JButton createSectionActionButton(String resourceKey) {
        JButton button = new JButton(getTextAt(getCampaignOptionsResourceBundle(), resourceKey));
        button.putClientProperty("JComponent.sizeVariant", "small");
        return button;
    }

    private void setExpanded(boolean expanded, MHQCollapsiblePanel... sections) {
        for (MHQCollapsiblePanel section : sections) {
            section.setExpanded(expanded);
        }
    }

    /**
     * Loads values from the current campaign or an optional preset campaign options
     * into the UI components, updating
     * their states to match the data.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null);
    }

    /**
     * Loads values from the specified {@code presetCampaignOptions}, or the current
     * campaign's options if {@code null},
     * into the UI form fields and controls.
     *
     * @param presetCampaignOptions        an alternative {@link CampaignOptions},
     *                                     or {@code null} to use the current
     *                                     campaign's options
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences}
     *                                     object to load values from; if
     *                                     {@code null}, values are loaded from the
     *                                     current skill preferences.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions,
            @Nullable RandomSkillPreferences presetRandomSkillPreferences) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        RandomSkillPreferences skillPreferences = presetRandomSkillPreferences;
        if (skillPreferences == null) {
            skillPreferences = this.randomSkillPreferences;
        }

        model = new SystemsOptionsModel(options, skillPreferences);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the currently selected values in the UI controls to modify the
     * campaign's options. If a preset is
     * provided, that preset is updated instead of the campaign's default options.
     *
     * @param presetCampaignOptions        an alternative {@link CampaignOptions}
     *                                     object to update, or {@code null} to
     *                                     update the campaign's own options
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences}
     *                                     object to set values to; if
     *                                     {@code null}, values are applied to the
     *                                     current skill preferences.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions,
            @Nullable RandomSkillPreferences presetRandomSkillPreferences) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        RandomSkillPreferences skillPreferences = presetRandomSkillPreferences;
        if (skillPreferences == null) {
            skillPreferences = this.randomSkillPreferences;
        }

        updateModelFromCreatedControls();

        if (model.resetCriminalRecord) {
            campaign.setDateOfLastCrime(null);
            campaign.setCrimeRating(0);
            campaign.setCrimePirateModifier(0);
            model.resetCriminalRecord = false;
        }

        model.applyTo(options, skillPreferences);
        updateResetCriminalRecordButtonFromModel();
    }

    private void updateCreatedControlsFromModel() {
        updateReputationControlsFromModel();
        updateFactionStandingControlsFromModel();
        updateATOWControlsFromModel();
    }

    private void updateReputationControlsFromModel() {
        if (!reputationPageCreated || model == null) {
            return;
        }

        manualUnitRatingModifier.setValue(model.manualUnitRatingModifier);
        updateResetCriminalRecordButtonFromModel();
        chkClampReputationPayMultiplier.setSelected(model.clampReputationPayMultiplier);
        chkReduceReputationPerformanceModifier.setSelected(model.reduceReputationPerformanceModifier);
        chkReputationPerformanceModifierCutOff.setSelected(model.reputationPerformanceModifierCutOff);
    }

    private void updateResetCriminalRecordButtonFromModel() {
        if (btnResetCriminalRecord == null) {
            return;
        }

        boolean isResetPending = model != null && model.resetCriminalRecord;
        String resourceKey = isResetPending ? "btnResetCriminalRecord.pending.text" :
                "btnResetCriminalRecord.text";
        btnResetCriminalRecord.setText(getTextAt(getCampaignOptionsResourceBundle(), resourceKey));
        btnResetCriminalRecord.setEnabled(!isResetPending);
    }

    private void updateFactionStandingControlsFromModel() {
        if (!factionStandingPageCreated || model == null) {
            return;
        }

        chkTrackFactionStanding.setSelected(model.trackFactionStanding);
        chkTrackClimateRegardChanges.setSelected(model.trackClimateRegardChanges);
        spnRegardMultiplier.setValue(model.regardMultiplier);
        chkUseFactionStandingNegotiation.setSelected(model.useFactionStandingNegotiation);
        chkUseFactionStandingResupply.setSelected(model.useFactionStandingResupply);
        chkUseFactionStandingCommandCircuit.setSelected(model.useFactionStandingCommandCircuit);
        chkUseFactionStandingOutlawed.setSelected(model.useFactionStandingOutlawed);
        chkUseFactionStandingBatchallRestrictions.setSelected(model.useFactionStandingBatchallRestrictions);
        chkUseFactionStandingRecruitment.setSelected(model.useFactionStandingRecruitment);
        chkUseFactionStandingBarracksCosts.setSelected(model.useFactionStandingBarracksCosts);
        chkUseFactionStandingUnitMarket.setSelected(model.useFactionStandingUnitMarket);
        chkUseFactionStandingContractPay.setSelected(model.useFactionStandingContractPay);
        chkUseFactionStandingSupportPoints.setSelected(model.useFactionStandingSupportPoints);
    }

    private void updateATOWControlsFromModel() {
        if (!atowPageCreated || model == null) {
            return;
        }

        chkUseAttributes.setSelected(model.useAttributes);
        chkRandomizeAttributes.setSelected(model.randomizeAttributes);
        chkDisplayAllAttributes.setSelected(model.displayAllAttributes);
        chkUseAgeEffects.setSelected(model.useAgeEffects);
        chkRandomizeTraits.setSelected(model.randomizeTraits);
        chkAllowMonthlyReinvestment.setSelected(model.allowMonthlyReinvestment);
        chkAllowMonthlyConnections.setSelected(model.allowMonthlyConnections);
        chkUseBetterExtraIncome.setSelected(model.useBetterExtraIncome);
        chkUseSmallArmsOnly.setSelected(model.useSmallArmsOnly);
    }

    private void updateModelFromCreatedControls() {
        updateModelFromReputationControls();
        updateModelFromFactionStandingControls();
        updateModelFromATOWControls();
    }

    private void updateModelFromReputationControls() {
        if (!reputationPageCreated || model == null) {
            return;
        }

        model.manualUnitRatingModifier = (int) manualUnitRatingModifier.getValue();
        model.clampReputationPayMultiplier = chkClampReputationPayMultiplier.isSelected();
        model.reduceReputationPerformanceModifier = chkReduceReputationPerformanceModifier.isSelected();
        model.reputationPerformanceModifierCutOff = chkReputationPerformanceModifierCutOff.isSelected();
    }

    private CampaignOptionsMetadata getResetCriminalRecordMetadata() {
        return getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT);
    }

    private void updateModelFromFactionStandingControls() {
        if (!factionStandingPageCreated || model == null) {
            return;
        }

        model.trackFactionStanding = chkTrackFactionStanding.isSelected();
        model.trackClimateRegardChanges = chkTrackClimateRegardChanges.isSelected();
        model.regardMultiplier = (double) spnRegardMultiplier.getValue();
        model.useFactionStandingNegotiation = chkUseFactionStandingNegotiation.isSelected();
        model.useFactionStandingResupply = chkUseFactionStandingResupply.isSelected();
        model.useFactionStandingCommandCircuit = chkUseFactionStandingCommandCircuit.isSelected();
        model.useFactionStandingOutlawed = chkUseFactionStandingOutlawed.isSelected();
        model.useFactionStandingBatchallRestrictions = chkUseFactionStandingBatchallRestrictions.isSelected();
        model.useFactionStandingRecruitment = chkUseFactionStandingRecruitment.isSelected();
        model.useFactionStandingBarracksCosts = chkUseFactionStandingBarracksCosts.isSelected();
        model.useFactionStandingUnitMarket = chkUseFactionStandingUnitMarket.isSelected();
        model.useFactionStandingContractPay = chkUseFactionStandingContractPay.isSelected();
        model.useFactionStandingSupportPoints = chkUseFactionStandingSupportPoints.isSelected();
    }

    private void updateModelFromATOWControls() {
        if (!atowPageCreated || model == null) {
            return;
        }

        model.useAttributes = chkUseAttributes.isSelected();
        model.randomizeAttributes = chkRandomizeAttributes.isSelected();
        model.displayAllAttributes = chkDisplayAllAttributes.isSelected();
        model.useAgeEffects = chkUseAgeEffects.isSelected();
        model.randomizeTraits = chkRandomizeTraits.isSelected();
        model.allowMonthlyReinvestment = chkAllowMonthlyReinvestment.isSelected();
        model.allowMonthlyConnections = chkAllowMonthlyConnections.isSelected();
        model.useBetterExtraIncome = chkUseBetterExtraIncome.isSelected();
        model.useSmallArmsOnly = chkUseSmallArmsOnly.isSelected();
    }

}
