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
package mekhq.gui.campaignOptions.contents;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * The {@code SystemsTab} class is responsible for managing and displaying the "Reputation" tab within the campaign
 * options UI. It provides Swing components for configuring reputation-related settings in a {@link Campaign}, including
 * unit rating methods, manual modifiers, and various campaign mechanics toggles.
 *
 * <p>The tab is constructed using helper panels and controls, grouped under "General" and "Sanity" sub-panels,
 * facilitating a user-friendly way to view and adjust reputation options.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class SystemsTab {
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final RandomSkillPreferences randomSkillPreferences;

    // Reputation Tab
    private CampaignOptionsHeaderPanel reputationHeader;

    private MMComboBox<UnitRatingMethod> unitRatingMethodCombo;
    private JCheckBox chkResetCriminalRecord;

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
    private JCheckBox chkRandomizeTraits;
    private JCheckBox chkAllowMonthlyReinvestment;
    private JCheckBox chkAllowMonthlyConnections;

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
    }

    /**
     * Creates the Reputation tab panel, containing grouped UI elements for reputation options and its header.
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
              10);

        // Contents
        JPanel pnlReputationGeneralOptions = createReputationGeneralPanel();
        JPanel pnlReputationSanityOptions = createReputationSanityPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ReputationTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(reputationHeader, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(pnlReputationGeneralOptions, layoutParent);

        layoutParent.gridx++;
        panel.add(pnlReputationSanityOptions, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panel, "ReputationTab");
    }

    /**
     * Creates and lays out the general reputation options panel, including controls for selecting unit rating method,
     * manual modifiers, and criminal record reset.
     *
     * @return a {@link JPanel} containing the general reputation controls
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createReputationGeneralPanel() {
        // Contents
        JLabel lblReputation = new CampaignOptionsLabel("Reputation");
        lblReputation.addMouseListener(createTipPanelUpdater(reputationHeader, "Reputation"));
        unitRatingMethodCombo = new MMComboBox<>("unitRatingMethodCombo", UnitRatingMethod.values());
        unitRatingMethodCombo.setToolTipText(String.format("<html>%s</html>",
              getTextAt(getCampaignOptionsResourceBundle(), "lblReputation.tooltip")));
        unitRatingMethodCombo.addMouseListener(createTipPanelUpdater(reputationHeader, "Reputation"));

        JLabel lblManualUnitRatingModifier = new CampaignOptionsLabel("ManualUnitRatingModifier");
        lblManualUnitRatingModifier.addMouseListener(createTipPanelUpdater(reputationHeader,
              "ManualUnitRatingModifier"));
        manualUnitRatingModifier = new CampaignOptionsSpinner("ManualUnitRatingModifier", 0, -1000, 1000, 1);
        manualUnitRatingModifier.addMouseListener(createTipPanelUpdater(reputationHeader, "ManualUnitRatingModifier"));

        chkResetCriminalRecord = new CampaignOptionsCheckBox("ResetCriminalRecord");
        chkResetCriminalRecord.addMouseListener(createTipPanelUpdater(reputationHeader, "ResetCriminalRecord"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ReputationGeneralOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblReputation, layout);
        layout.gridx++;
        panel.add(unitRatingMethodCombo, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblManualUnitRatingModifier, layout);
        layout.gridx++;
        panel.add(manualUnitRatingModifier, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkResetCriminalRecord, layout);

        return panel;
    }

    /**
     * Creates and lays out the reputation "sanity" options panel, which includes various checkboxes for limiting or
     * modifying reputation calculations.
     *
     * @return a {@link JPanel} containing the reputation sanity option controls
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createReputationSanityPanel() {
        // Contents
        chkClampReputationPayMultiplier = new CampaignOptionsCheckBox("ClampReputationPayMultiplier");
        chkClampReputationPayMultiplier.addMouseListener(createTipPanelUpdater(reputationHeader,
              "ClampReputationPayMultiplier"));

        chkReduceReputationPerformanceModifier = new CampaignOptionsCheckBox("ReduceReputationPerformanceModifier");
        chkReduceReputationPerformanceModifier.addMouseListener(createTipPanelUpdater(reputationHeader,
              "ReduceReputationPerformanceModifier"));

        chkReputationPerformanceModifierCutOff = new CampaignOptionsCheckBox("ReputationPerformanceModifierCutOff");
        chkReputationPerformanceModifierCutOff.addMouseListener(createTipPanelUpdater(reputationHeader,
              "ReputationPerformanceModifierCutOff"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ReputationSanityOptionsPanel", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(chkClampReputationPayMultiplier, layout);

        layout.gridy++;
        panel.add(chkReduceReputationPerformanceModifier, layout);

        layout.gridy++;
        panel.add(chkReputationPerformanceModifierCutOff, layout);

        return panel;
    }

    /**
     * Creates the Faction Standing tab panel, containing grouped UI elements for Faction Standing options and its
     * header.
     *
     * @return a {@link JPanel} component representing the entire Faction Standing tab UI
     *
     * @author Illiani
     * @since 0.50.07
     */
    public JPanel createFactionStandingTab() {
        // Header
        factionStandingHeader = new CampaignOptionsHeaderPanel("FactionStandingTab",
              getImageDirectory() + "logo_morgrains_valkyrate.png",
              4);

        // Contents
        chkTrackFactionStanding = new CampaignOptionsCheckBox("TrackFactionStanding");
        chkTrackFactionStanding.addMouseListener(createTipPanelUpdater(factionStandingHeader, "TrackFactionStanding"));

        chkTrackClimateRegardChanges = new CampaignOptionsCheckBox("TrackClimateRegardChanges");
        chkTrackClimateRegardChanges.addMouseListener(createTipPanelUpdater(factionStandingHeader,
              "TrackClimateRegardChanges"));

        JLabel lblRegardMultiplier = new CampaignOptionsLabel("RegardMultiplier");
        lblRegardMultiplier.addMouseListener(createTipPanelUpdater(factionStandingHeader, "RegardMultiplier"));
        spnRegardMultiplier = new CampaignOptionsSpinner("RegardMultiplier", 1.0, 0.1, 3.0, 0.1);
        spnRegardMultiplier.addMouseListener(createTipPanelUpdater(factionStandingHeader, "RegardMultiplier"));

        JPanel pnlFactionStandingModifiersPanel = createFactionStandingModifiersPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("FactionStandingTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(factionStandingHeader, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(chkTrackFactionStanding, layoutParent);
        layoutParent.gridx++;
        panel.add(chkTrackClimateRegardChanges, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        panel.add(lblRegardMultiplier, layoutParent);
        layoutParent.gridx++;
        panel.add(spnRegardMultiplier, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 2;
        panel.add(pnlFactionStandingModifiersPanel, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panel, "FactionStandingTab");
    }

    /**
     * Creates and lays out the Faction Standing modifiers panel, which includes various checkboxes for limiting Faction
     * Standing modifiers.
     *
     * @return a {@link JPanel} containing the Faction Standing modifier controls
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createFactionStandingModifiersPanel() {
        // Contents
        chkUseFactionStandingNegotiation = new CampaignOptionsCheckBox("UseFactionStandingNegotiation");
        chkUseFactionStandingNegotiation.addMouseListener(createTipPanelUpdater(factionStandingHeader,
              "UseFactionStandingNegotiation"));

        chkUseFactionStandingResupply = new CampaignOptionsCheckBox("UseFactionStandingResupply");
        chkUseFactionStandingResupply.addMouseListener(createTipPanelUpdater(factionStandingHeader,
              "UseFactionStandingResupply"));

        chkUseFactionStandingCommandCircuit = new CampaignOptionsCheckBox("UseFactionStandingCommandCircuit");
        chkUseFactionStandingCommandCircuit.addMouseListener(createTipPanelUpdater(factionStandingHeader,
              "UseFactionStandingCommandCircuit"));

        chkUseFactionStandingOutlawed = new CampaignOptionsCheckBox("UseFactionStandingOutlawed");
        chkUseFactionStandingOutlawed.addMouseListener(createTipPanelUpdater(factionStandingHeader,
              "UseFactionStandingOutlawed"));

        chkUseFactionStandingBatchallRestrictions = new CampaignOptionsCheckBox("UseFactionStandingBatchallRestrictions");
        chkUseFactionStandingBatchallRestrictions.addMouseListener(createTipPanelUpdater(factionStandingHeader,
              "UseFactionStandingBatchallRestrictions"));

        chkUseFactionStandingRecruitment = new CampaignOptionsCheckBox("UseFactionStandingRecruitment");
        chkUseFactionStandingRecruitment.addMouseListener(createTipPanelUpdater(factionStandingHeader,
              "UseFactionStandingRecruitment"));

        chkUseFactionStandingBarracksCosts = new CampaignOptionsCheckBox("UseFactionStandingBarracksCosts");
        chkUseFactionStandingBarracksCosts.addMouseListener(createTipPanelUpdater(factionStandingHeader,
              "UseFactionStandingBarracksCosts"));

        chkUseFactionStandingUnitMarket = new CampaignOptionsCheckBox("UseFactionStandingUnitMarket");
        chkUseFactionStandingUnitMarket.addMouseListener(createTipPanelUpdater(factionStandingHeader,
              "UseFactionStandingUnitMarket"));

        chkUseFactionStandingContractPay = new CampaignOptionsCheckBox("UseFactionStandingContractPay");
        chkUseFactionStandingContractPay.addMouseListener(createTipPanelUpdater(factionStandingHeader,
              "UseFactionStandingContractPay"));

        chkUseFactionStandingSupportPoints = new CampaignOptionsCheckBox("UseFactionStandingSupportPoints");
        chkUseFactionStandingSupportPoints.addMouseListener(createTipPanelUpdater(factionStandingHeader,
              "UseFactionStandingSupportPoints"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("FactionStandingModifiersPanel", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(chkUseFactionStandingNegotiation, layout);
        layout.gridx++;
        panel.add(chkUseFactionStandingResupply, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseFactionStandingCommandCircuit, layout);
        layout.gridx++;
        panel.add(chkUseFactionStandingOutlawed, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseFactionStandingBatchallRestrictions, layout);
        layout.gridx++;
        panel.add(chkUseFactionStandingRecruitment, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseFactionStandingBarracksCosts, layout);
        layout.gridx++;
        panel.add(chkUseFactionStandingUnitMarket, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseFactionStandingContractPay, layout);
        layout.gridx++;
        panel.add(chkUseFactionStandingSupportPoints, layout);

        return panel;
    }

    /**
     * Creates the ATOW tab panel, containing grouped UI elements for configuring ATOW-related options and its header.
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
              9);

        // Contents
        JPanel pnlATOWAttributes = createATOWAttributesPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ATimeOfWarTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(atowHeader, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(pnlATOWAttributes, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panel, "ATimeOfWarTab");
    }

    /**
     * Creates and returns the ATOW panel, which allows users to configure settings for attribute and traits
     * probabilities.
     *
     * @return A {@code JPanel} containing configuration options for phenotype probabilities.
     */
    private JPanel createATOWAttributesPanel() {
        // Contents
        chkUseAttributes = new CampaignOptionsCheckBox("UseAttributes");
        chkUseAttributes.addMouseListener(createTipPanelUpdater(atowHeader, "UseAttributes"));
        chkRandomizeAttributes = new CampaignOptionsCheckBox("RandomizeAttributes");
        chkRandomizeAttributes.addMouseListener(createTipPanelUpdater(atowHeader, "RandomizeAttributes"));
        chkRandomizeTraits = new CampaignOptionsCheckBox("RandomizeTraits");
        chkRandomizeTraits.addMouseListener(createTipPanelUpdater(atowHeader, "RandomizeTraits"));
        chkAllowMonthlyReinvestment = new CampaignOptionsCheckBox("AllowMonthlyReinvestment");
        chkAllowMonthlyReinvestment.addMouseListener(createTipPanelUpdater(atowHeader,
              "AllowMonthlyReinvestment"));
        chkAllowMonthlyConnections = new CampaignOptionsCheckBox("AllowMonthlyConnections");
        chkAllowMonthlyConnections.addMouseListener(createTipPanelUpdater(atowHeader,
              "AllowMonthlyConnections"));

        final JPanel panel = new CampaignOptionsStandardPanel("ATOWAttributesPanel", true, "ATOWAttributesPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;

        layout.gridy++;
        panel.add(chkUseAttributes, layout);
        layout.gridx++;
        panel.add(chkRandomizeAttributes, layout);
        layout.gridx++;
        panel.add(chkRandomizeTraits, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkAllowMonthlyReinvestment, layout);
        layout.gridx++;
        panel.add(chkAllowMonthlyConnections, layout);

        return panel;
    }

    /**
     * Loads values from the current campaign or an optional preset campaign options into the UI components, updating
     * their states to match the data.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null);
    }

    /**
     * Loads values from the specified {@code presetCampaignOptions}, or the current campaign's options if {@code null},
     * into the UI form fields and controls.
     *
     * @param presetCampaignOptions        an alternative {@link CampaignOptions}, or {@code null} to use the current
     *                                     campaign's options
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences} object to load values from; if
     *                                     {@code null}, values are loaded from the current skill preferences.
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

        // Reputation
        unitRatingMethodCombo.setSelectedItem(options.getUnitRatingMethod());
        manualUnitRatingModifier.setValue(options.getManualUnitRatingModifier());

        chkClampReputationPayMultiplier.setSelected(options.isClampReputationPayMultiplier());
        chkReduceReputationPerformanceModifier.setSelected(options.isReduceReputationPerformanceModifier());
        chkReputationPerformanceModifierCutOff.setSelected(options.isReputationPerformanceModifierCutOff());

        // Faction Standing
        chkTrackFactionStanding.setSelected(options.isTrackFactionStanding());
        chkTrackClimateRegardChanges.setSelected(options.isTrackClimateRegardChanges());
        spnRegardMultiplier.setValue(options.getRegardMultiplier());
        chkUseFactionStandingNegotiation.setSelected(options.isUseFactionStandingNegotiation());
        chkUseFactionStandingResupply.setSelected(options.isUseFactionStandingResupply());
        chkUseFactionStandingCommandCircuit.setSelected(options.isUseFactionStandingCommandCircuit());
        chkUseFactionStandingOutlawed.setSelected(options.isUseFactionStandingOutlawed());
        chkUseFactionStandingBatchallRestrictions.setSelected(options.isUseFactionStandingBatchallRestrictions());
        chkUseFactionStandingRecruitment.setSelected(options.isUseFactionStandingRecruitment());
        chkUseFactionStandingBarracksCosts.setSelected(options.isUseFactionStandingBarracksCosts());
        chkUseFactionStandingUnitMarket.setSelected(options.isUseFactionStandingUnitMarket());
        chkUseFactionStandingContractPay.setSelected(options.isUseFactionStandingContractPay());
        chkUseFactionStandingSupportPoints.setSelected(options.isUseFactionStandingSupportPoints());

        // A Time of War
        chkUseAttributes.setSelected(skillPreferences.isUseAttributes());
        chkRandomizeAttributes.setSelected(skillPreferences.isRandomizeAttributes());
        chkRandomizeTraits.setSelected(skillPreferences.isRandomizeTraits());
        chkAllowMonthlyReinvestment.setSelected(options.isAllowMonthlyReinvestment());
        chkAllowMonthlyConnections.setSelected(options.isAllowMonthlyConnections());
    }

    /**
     * Applies the currently selected values in the UI controls to modify the campaign's options. If a preset is
     * provided, that preset is updated instead of the campaign's default options.
     *
     * @param presetCampaignOptions        an alternative {@link CampaignOptions} object to update, or {@code null} to
     *                                     update the campaign's own options
     * @param presetRandomSkillPreferences Optional {@code RandomSkillPreferences} object to set values to; if
     *                                     {@code null}, values are applied to the current skill preferences.
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

        // Reputation
        options.setUnitRatingMethod(unitRatingMethodCombo.getSelectedItem());
        options.setManualUnitRatingModifier((int) manualUnitRatingModifier.getValue());

        if (chkResetCriminalRecord.isSelected()) {
            campaign.setDateOfLastCrime(null);
            campaign.setCrimeRating(0);
            campaign.setCrimePirateModifier(0);
        }

        options.setClampReputationPayMultiplier(chkClampReputationPayMultiplier.isSelected());
        options.setReduceReputationPerformanceModifier(chkReduceReputationPerformanceModifier.isSelected());
        options.setReputationPerformanceModifierCutOff(chkReputationPerformanceModifierCutOff.isSelected());

        // Faction Standing
        options.setTrackFactionStanding(chkTrackFactionStanding.isSelected());
        options.setTrackClimateRegardChanges(chkTrackClimateRegardChanges.isSelected());
        options.setRegardMultiplier((double) spnRegardMultiplier.getValue());
        options.setUseFactionStandingNegotiation(chkUseFactionStandingNegotiation.isSelected());
        options.setUseFactionStandingResupply(chkUseFactionStandingResupply.isSelected());
        options.setUseFactionStandingCommandCircuit(chkUseFactionStandingCommandCircuit.isSelected());
        options.setUseFactionStandingOutlawed(chkUseFactionStandingOutlawed.isSelected());
        options.setUseFactionStandingBatchallRestrictions(chkUseFactionStandingBatchallRestrictions.isSelected());
        options.setUseFactionStandingRecruitment(chkUseFactionStandingRecruitment.isSelected());
        options.setUseFactionStandingBarracksCosts(chkUseFactionStandingBarracksCosts.isSelected());
        options.setUseFactionStandingUnitMarket(chkUseFactionStandingUnitMarket.isSelected());
        options.setUseFactionStandingContractPay(chkUseFactionStandingContractPay.isSelected());
        options.setUseFactionStandingSupportPoints(chkUseFactionStandingSupportPoints.isSelected());

        // A Time of War
        skillPreferences.setUseAttributes(chkUseAttributes.isSelected());
        skillPreferences.setRandomizeAttributes(chkRandomizeAttributes.isSelected());
        skillPreferences.setRandomizeTraits(chkRandomizeTraits.isSelected());
        options.setAllowMonthlyReinvestment(chkAllowMonthlyReinvestment.isSelected());
        options.setAllowMonthlyConnections(chkAllowMonthlyConnections.isSelected());
    }
}
