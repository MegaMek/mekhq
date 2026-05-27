/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import java.awt.Component;
import java.awt.GridBagConstraints;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.TurnoverFrequency;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * The {@code TurnoverAndRetentionTab} class represents a graphical user
 * interface (GUI) configuration tab in the
 * campaign options for managing unit turnover, retention, and fatigue settings.
 * <p>
 * This class provides functionality to define and customize gameplay-related
 * options such as:
 * </p>
 * <ul>
 * <li>Unit turnover settings, including retirement, contract durations,
 * payouts, and modifiers.</li>
 * <li>HR strain and management skills impacting unit cohesion.</li>
 * <li>Fatigue mechanics such as fatigue rates, leave thresholds, and injury
 * fatigue.</li>
 * </ul>
 * <p>
 * The class interacts with a {@link CampaignOptions} object, allowing the user
 * to load and save
 * configurations. It consists of two main panels:
 * </p>
 * <ul>
 * <li><strong>Turnover Tab:</strong> Controls unit turnover, payouts, and
 * related modifiers.</li>
 * <li><strong>Fatigue Tab:</strong> Manages fatigue-related options like
 * kitchen capacity
 * and fatigue rates.</li>
 * </ul>
 */
public class TurnoverAndRetentionTab {
    private final CampaignOptions campaignOptions;
    private TurnoverAndRetentionOptionsModel model;
    private boolean turnoverPageCreated;
    private boolean fatiguePageCreated;

    // start Turnover Tab
    private CampaignOptionsHeaderPanel turnoverHeader;
    private JCheckBox chkUseRandomRetirement;

    private JPanel pnlSettings;
    private JLabel lblTurnoverFixedTargetNumber;
    private JSpinner spnTurnoverFixedTargetNumber;
    private JLabel lblTurnoverFrequency;
    private MMComboBox<TurnoverFrequency> comboTurnoverFrequency;
    private JCheckBox chkUseContractCompletionRandomRetirement;
    private JCheckBox chkUseRandomFounderTurnover;
    private JCheckBox chkTrackOriginalUnit;
    private JCheckBox chkAeroRecruitsHaveUnits;
    private JCheckBox chkUseSubContractSoldiers;
    private JLabel lblServiceContractDuration;
    private JSpinner spnServiceContractDuration;
    private JLabel lblServiceContractModifier;
    private JSpinner spnServiceContractModifier;
    private JCheckBox chkPayBonusDefault;
    private JLabel lblPayBonusDefaultThreshold;
    private JSpinner spnPayBonusDefaultThreshold;
    private JCheckBox chkIncludeCivilians;

    private JPanel pnlModifiers;
    private JCheckBox chkUseCustomRetirementModifiers;
    private JCheckBox chkUseFatigueModifiers;
    private JCheckBox chkUseSkillModifiers;
    private JCheckBox chkUseAgeModifiers;
    private JCheckBox chkUseUnitRatingModifiers;
    private JCheckBox chkUseFactionModifiers;
    private JCheckBox chkUseMissionStatusModifiers;
    private JCheckBox chkUseHostileTerritoryModifiers;
    private JCheckBox chkUseFamilyModifiers;
    private JCheckBox chkUseLoyaltyModifiers;
    private JCheckBox chkUseHideLoyalty;

    private JPanel pnlPayout;
    private JLabel lblPayoutRateOfficer;
    private JSpinner spnPayoutRateOfficer;
    private JLabel lblPayoutRateEnlisted;
    private JSpinner spnPayoutRateEnlisted;
    private JLabel lblPayoutRetirementMultiplier;
    private JSpinner spnPayoutRetirementMultiplier;
    private JCheckBox chkUsePayoutServiceBonus;
    private JLabel lblPayoutServiceBonusRate;
    private JSpinner spnPayoutServiceBonusRate;

    private JPanel pnlUnitCohesion;

    private JPanel pnlHRStrainWrapper;
    private JCheckBox chkUseHRStrain;

    private JPanel pnlHRStrain;
    private JLabel lblHRCapacity;
    private JSpinner spnHRCapacity;

    private JPanel pnlManagementSkillWrapper;
    private JCheckBox chkUseManagementSkill;

    private JPanel pnlManagementSkill;
    private JCheckBox chkUseCommanderLeadershipOnly;
    private JLabel lblManagementSkillPenalty;
    private JSpinner spnManagementSkillPenalty;
    // end Turnover Tab

    private JCheckBox chkUseFatigue;
    private JLabel lblFatigueRate;
    private JSpinner spnFatigueRate;
    private JCheckBox chkUseInjuryFatigue;
    private JLabel lblFieldKitchenCapacity;
    private JSpinner spnFieldKitchenCapacity;
    private JCheckBox chkFieldKitchenIgnoreNonCombatants;
    private JLabel lblFatigueUndeploymentThreshold;
    private JSpinner spnFatigueUndeploymentThreshold;
    private JLabel lblFatigueLeaveThreshold;
    private JSpinner spnFatigueLeaveThreshold;
    // end Fatigue Tab

    /**
     * Constructs a {@code TurnoverAndRetentionTab} and initializes the tab with the
     * given {@link CampaignOptions}. This
     * sets up necessary UI components and their default configurations.
     *
     * @param campaignOptions the {@code CampaignOptions} instance that holds the
     *                        settings to be modified or displayed
     *                        in this tab.
     */
    public TurnoverAndRetentionTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
        loadValuesFromCampaignOptions();
    }

    /**
     * Initializes the content and configuration of the turnover and fatigue tabs.
     * This method sets up their respective
     * panels and components.
     */
    private void initialize() {
        initializeTurnoverTab();
        initializeFatigueTab();
    }

    /**
     * Initializes the content of the fatigue configuration tab. Includes settings
     * such as fatigue rate, injury fatigue,
     * field kitchen capacity, and fatigue leave thresholds.
     */
    private void initializeFatigueTab() {
        chkUseFatigue = new JCheckBox();
        lblFatigueRate = new JLabel();
        spnFatigueRate = new JSpinner();
        chkUseInjuryFatigue = new JCheckBox();
        lblFieldKitchenCapacity = new JLabel();
        spnFieldKitchenCapacity = new JSpinner();
        chkFieldKitchenIgnoreNonCombatants = new JCheckBox();
        lblFatigueUndeploymentThreshold = new JLabel();
        spnFatigueUndeploymentThreshold = new JSpinner();
        lblFatigueLeaveThreshold = new JLabel();
        spnFatigueLeaveThreshold = new JSpinner();
    }

    /**
     * Initializes the content of the turnover configuration tab. Includes settings
     * such as turnover frequencies,
     * service contract details, and retirement/payout modifiers.
     */
    private void initializeTurnoverTab() {
        chkUseRandomRetirement = new JCheckBox();

        pnlSettings = new JPanel();
        lblTurnoverFixedTargetNumber = new JLabel();
        spnTurnoverFixedTargetNumber = new JSpinner();
        lblTurnoverFrequency = new JLabel();
        comboTurnoverFrequency = new MMComboBox<>("comboTurnoverFrequency", TurnoverFrequency.values());
        chkUseContractCompletionRandomRetirement = new JCheckBox();
        chkUseRandomFounderTurnover = new JCheckBox();
        chkTrackOriginalUnit = new JCheckBox();
        chkAeroRecruitsHaveUnits = new JCheckBox();
        chkUseSubContractSoldiers = new JCheckBox();
        lblServiceContractDuration = new JLabel();
        spnServiceContractDuration = new JSpinner();
        lblServiceContractModifier = new JLabel();
        spnServiceContractModifier = new JSpinner();
        chkPayBonusDefault = new JCheckBox();
        lblPayBonusDefaultThreshold = new JLabel();
        spnPayBonusDefaultThreshold = new JSpinner();
        chkIncludeCivilians = new JCheckBox();

        pnlModifiers = new JPanel();
        chkUseCustomRetirementModifiers = new JCheckBox();
        chkUseFatigueModifiers = new JCheckBox();
        chkUseSkillModifiers = new JCheckBox();
        chkUseAgeModifiers = new JCheckBox();
        chkUseUnitRatingModifiers = new JCheckBox();
        chkUseFactionModifiers = new JCheckBox();
        chkUseMissionStatusModifiers = new JCheckBox();
        chkUseHostileTerritoryModifiers = new JCheckBox();
        chkUseFamilyModifiers = new JCheckBox();
        chkUseLoyaltyModifiers = new JCheckBox();
        chkUseHideLoyalty = new JCheckBox();

        pnlPayout = new JPanel();
        lblPayoutRateOfficer = new JLabel();
        spnPayoutRateOfficer = new JSpinner();
        lblPayoutRateEnlisted = new JLabel();
        spnPayoutRateEnlisted = new JSpinner();
        lblPayoutRetirementMultiplier = new JLabel();
        spnPayoutRetirementMultiplier = new JSpinner();
        chkUsePayoutServiceBonus = new JCheckBox();
        lblPayoutServiceBonusRate = new JLabel();
        spnPayoutServiceBonusRate = new JSpinner();

        pnlUnitCohesion = new JPanel();

        pnlHRStrainWrapper = new JPanel();
        chkUseHRStrain = new JCheckBox();

        pnlHRStrain = new JPanel();
        lblHRCapacity = new JLabel();
        spnHRCapacity = new JSpinner();

        pnlManagementSkillWrapper = new JPanel();
        chkUseManagementSkill = new JCheckBox();

        pnlManagementSkill = new JPanel();
        chkUseCommanderLeadershipOnly = new JCheckBox();
        lblManagementSkillPenalty = new JLabel();
        spnManagementSkillPenalty = new JSpinner();
    }

    /**
     * Creates and configures the "Fatigue" tab with its relevant components. These
     * include options related to enabling
     * fatigue, fatigue rates, injury fatigue, kitchen capacities, and leave
     * thresholds.
     *
     * @return the {@link JPanel} representing the constructed Fatigue tab.
     */
    public JPanel createFatigueTab() {
        // Header
        // start Fatigue Tab
        CampaignOptionsHeaderPanel fatigueHeader = new CampaignOptionsHeaderPanel("FatigueTab",
                getImageDirectory() + "logo_clan_mongoose.png",
                true, true, 5);

        // Contents
        chkUseFatigue = new CampaignOptionsCheckBox("UseFatigue");
        chkUseFatigue.addMouseListener(createTipPanelUpdater(fatigueHeader, "UseFatigue"));

        lblFatigueRate = new CampaignOptionsLabel("FatigueRate",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblFatigueRate.addMouseListener(createTipPanelUpdater(fatigueHeader, "FatigueRate"));
        spnFatigueRate = new CampaignOptionsSpinner("FatigueRate",
                1, 1, 10, 1);
        spnFatigueRate.addMouseListener(createTipPanelUpdater(fatigueHeader, "FatigueRate"));

        chkUseInjuryFatigue = new CampaignOptionsCheckBox("UseInjuryFatigue",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseInjuryFatigue.addMouseListener(createTipPanelUpdater(fatigueHeader, "UseInjuryFatigue"));

        lblFieldKitchenCapacity = new CampaignOptionsLabel("FieldKitchenCapacity");
        lblFieldKitchenCapacity.addMouseListener(createTipPanelUpdater(fatigueHeader, "FieldKitchenCapacity"));
        spnFieldKitchenCapacity = new CampaignOptionsSpinner("FieldKitchenCapacity",
                150, 0, 450, 1);
        spnFieldKitchenCapacity.addMouseListener(createTipPanelUpdater(fatigueHeader, "FieldKitchenCapacity"));

        chkFieldKitchenIgnoreNonCombatants = new CampaignOptionsCheckBox("FieldKitchenIgnoreNonCombatants");
        chkFieldKitchenIgnoreNonCombatants.addMouseListener(createTipPanelUpdater(fatigueHeader,
                "FieldKitchenIgnoreNonCombatants"));

        lblFatigueUndeploymentThreshold = new CampaignOptionsLabel("FatigueUndeploymentThreshold",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.IMPORTANT));
        lblFatigueUndeploymentThreshold.addMouseListener(createTipPanelUpdater(fatigueHeader,
                "FatigueUndeploymentThreshold"));
        spnFatigueUndeploymentThreshold = new CampaignOptionsSpinner("FatigueUndeploymentThreshold",
                9, 0, 17, 1);
        spnFatigueUndeploymentThreshold.addMouseListener(createTipPanelUpdater(fatigueHeader,
                "FatigueUndeploymentThreshold"));

        lblFatigueLeaveThreshold = new CampaignOptionsLabel("FatigueLeaveThreshold",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.IMPORTANT));
        lblFatigueLeaveThreshold.addMouseListener(createTipPanelUpdater(fatigueHeader, "FatigueLeaveThreshold"));
        spnFatigueLeaveThreshold = new CampaignOptionsSpinner("FatigueLeaveThreshold",
                13, 0, 17, 1);
        spnFatigueLeaveThreshold.addMouseListener(createTipPanelUpdater(fatigueHeader, "FatigueLeaveThreshold"));

        // Layout the Panels
        final JPanel panelLeft = new CampaignOptionsStandardPanel("FatigueTabLeft");
        final GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy = 0;
        layoutLeft.gridwidth = 1;
        panelLeft.add(chkUseFatigue, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(chkUseInjuryFatigue, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(chkFieldKitchenIgnoreNonCombatants, layoutLeft);

        final JPanel panelRight = new CampaignOptionsStandardPanel("FatigueTabRight");
        final GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutRight.gridx = 0;
        layoutRight.gridy = 0;
        layoutRight.gridwidth = 1;
        panelRight.add(lblFatigueRate, layoutRight);
        layoutRight.gridx++;
        panelRight.add(spnFatigueRate, layoutRight);

        layoutRight.gridx = 0;
        layoutRight.gridy++;
        panelRight.add(lblFieldKitchenCapacity, layoutRight);
        layoutRight.gridx++;
        panelRight.add(spnFieldKitchenCapacity, layoutRight);

        layoutRight.gridx = 0;
        layoutRight.gridy++;
        panelRight.add(lblFatigueUndeploymentThreshold, layoutRight);
        layoutRight.gridx++;
        panelRight.add(spnFatigueUndeploymentThreshold, layoutRight);

        layoutRight.gridx = 0;
        layoutRight.gridy++;
        panelRight.add(lblFatigueLeaveThreshold, layoutRight);
        layoutRight.gridx++;
        panelRight.add(spnFatigueLeaveThreshold, layoutRight);

        final JPanel panelParent = new CampaignOptionsStandardPanel("FatigueTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(fatigueHeader, layoutParent);

        layoutParent.gridwidth = 1;
        layoutParent.gridy++;
        panelParent.add(panelLeft, layoutParent);
        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);
        fatiguePageCreated = true;
        updateFatigueControlsFromModel();

        // Create Parent Panel and return
        return createParentPanel(panelParent, "FatigueTab");
    }

    /**
     * Creates and configures the "Turnover" tab with its relevant components. These
     * include options for turnover
     * control, random retirement, payout settings, and modifiers for HR Strain and
     * cohesion.
     *
     * @return the {@link JPanel} representing the constructed Turnover tab.
     */
    public JPanel createTurnoverTab() {
        // Header
        turnoverHeader = new CampaignOptionsHeaderPanel("TurnoverTab",
                getImageDirectory() + "logo_duchy_of_andurien.png",
                true, true, 5);

        // Contents
        chkUseRandomRetirement = new CampaignOptionsCheckBox("UseRandomRetirement");
        chkUseRandomRetirement.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseRandomRetirement"));
        pnlSettings = createSettingsPanel();
        pnlModifiers = createModifiersPanel();
        pnlPayout = createPayoutsPanel();
        pnlUnitCohesion = createUnitCohesionPanel();
        turnoverPageCreated = true;
        updateTurnoverControlsFromModel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("TurnoverTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(turnoverHeader, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(chkUseRandomRetirement, layout);

        layout.gridy++;
        panel.add(pnlSettings, layout);
        layout.gridx++;
        panel.add(pnlModifiers, layout);
        layout.gridx++;
        panel.add(pnlPayout, layout);
        layout.gridx++;
        panel.add(pnlUnitCohesion, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "TurnoverTab");
    }

    /**
     * Creates the settings panel for the "Turnover" tab, which organizes various
     * settings like random retirement,
     * contract durations, and bonuses into a layout.
     *
     * @return the {@link JPanel} representing the turnover settings.
     */
    private JPanel createSettingsPanel() {
        // Contents
        lblTurnoverFixedTargetNumber = new CampaignOptionsLabel("TurnoverFixedTargetNumber");
        lblTurnoverFixedTargetNumber.addMouseListener(createTipPanelUpdater(turnoverHeader,
                "TurnoverFixedTargetNumber"));
        spnTurnoverFixedTargetNumber = new CampaignOptionsSpinner("TurnoverFixedTargetNumber",
                3, 0, 10, 1);
        spnTurnoverFixedTargetNumber.addMouseListener(createTipPanelUpdater(turnoverHeader,
                "TurnoverFixedTargetNumber"));

        lblTurnoverFrequency = new CampaignOptionsLabel("TurnoverFrequency",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.RECOMMENDED));
        lblTurnoverFrequency.addMouseListener(createTipPanelUpdater(turnoverHeader, "TurnoverFrequency"));
        comboTurnoverFrequency.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TurnoverFrequency) {
                    list.setToolTipText(((TurnoverFrequency) value).getToolTipText());
                }
                return this;
            }
        });
        comboTurnoverFrequency.addMouseListener(createTipPanelUpdater(turnoverHeader, "TurnoverFrequency"));

        chkUseContractCompletionRandomRetirement = new CampaignOptionsCheckBox(
                "UseContractCompletionRandomRetirement");
        chkUseContractCompletionRandomRetirement.addMouseListener(createTipPanelUpdater(turnoverHeader,
                "UseContractCompletionRandomRetirement"));

        chkUseRandomFounderTurnover = new CampaignOptionsCheckBox("UseRandomFounderTurnover");
        chkUseRandomFounderTurnover
                .addMouseListener(createTipPanelUpdater(turnoverHeader, "UseRandomFounderTurnover"));

        chkTrackOriginalUnit = new CampaignOptionsCheckBox("TrackOriginalUnit");
        chkTrackOriginalUnit.addMouseListener(createTipPanelUpdater(turnoverHeader, "TrackOriginalUnit"));

        chkAeroRecruitsHaveUnits = new CampaignOptionsCheckBox("AeroRecruitsHaveUnits");
        chkAeroRecruitsHaveUnits.addMouseListener(createTipPanelUpdater(turnoverHeader, "AeroRecruitsHaveUnits"));

        chkUseSubContractSoldiers = new CampaignOptionsCheckBox("UseSubContractSoldiers");
        chkUseSubContractSoldiers.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseSubContractSoldiers"));

        lblServiceContractDuration = new CampaignOptionsLabel("ServiceContractDuration");
        lblServiceContractDuration
                .addMouseListener(createTipPanelUpdater(turnoverHeader, "ServiceContractDuration"));
        spnServiceContractDuration = new CampaignOptionsSpinner("ServiceContractDuration",
                36, 0, 120, 1);
        spnServiceContractDuration
                .addMouseListener(createTipPanelUpdater(turnoverHeader, "ServiceContractDuration"));

        lblServiceContractModifier = new CampaignOptionsLabel("ServiceContractModifier");
        lblServiceContractModifier
                .addMouseListener(createTipPanelUpdater(turnoverHeader, "ServiceContractModifier"));
        spnServiceContractModifier = new CampaignOptionsSpinner("ServiceContractModifier",
                3, 0, 10, 1);
        spnServiceContractModifier
                .addMouseListener(createTipPanelUpdater(turnoverHeader, "ServiceContractModifier"));

        chkPayBonusDefault = new CampaignOptionsCheckBox("PayBonusDefault");
        chkPayBonusDefault.addMouseListener(createTipPanelUpdater(turnoverHeader, "PayBonusDefault"));

        lblPayBonusDefaultThreshold = new CampaignOptionsLabel("PayBonusDefaultThreshold");
        lblPayBonusDefaultThreshold
                .addMouseListener(createTipPanelUpdater(turnoverHeader, "PayBonusDefaultThreshold"));
        spnPayBonusDefaultThreshold = new CampaignOptionsSpinner("PayBonusDefaultThreshold",
                3, 0, 12, 1);
        spnPayBonusDefaultThreshold
                .addMouseListener(createTipPanelUpdater(turnoverHeader, "PayBonusDefaultThreshold"));

        chkIncludeCivilians = new CampaignOptionsCheckBox("IncludeCivilians",
                getMetadata(new Version(0, 51, 0), CampaignOptionFlag.IMPORTANT));
        chkIncludeCivilians.addMouseListener(createTipPanelUpdater(turnoverHeader, "IncludeCivilians"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SettingsPanel", true,
                "SettingsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblTurnoverFixedTargetNumber, layout);
        layout.gridx++;
        panel.add(spnTurnoverFixedTargetNumber, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblTurnoverFrequency, layout);
        layout.gridx++;
        panel.add(comboTurnoverFrequency, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkUseContractCompletionRandomRetirement, layout);

        layout.gridy++;
        panel.add(chkUseRandomFounderTurnover, layout);

        layout.gridy++;
        panel.add(chkTrackOriginalUnit, layout);

        layout.gridy++;
        panel.add(chkAeroRecruitsHaveUnits, layout);

        layout.gridy++;
        panel.add(chkUseSubContractSoldiers, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblServiceContractDuration, layout);
        layout.gridx++;
        panel.add(spnServiceContractDuration, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblServiceContractModifier, layout);
        layout.gridx++;
        panel.add(spnServiceContractModifier, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkPayBonusDefault, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblPayBonusDefaultThreshold, layout);
        layout.gridx++;
        panel.add(spnPayBonusDefaultThreshold, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkIncludeCivilians, layout);

        return panel;
    }

    /**
     * Creates the modifiers panel for the "Turnover" tab, which contains gameplay
     * modifiers such as age, skill,
     * faction, and loyalty.
     *
     * @return the {@link JPanel} representing the turnover modifiers.
     */
    private JPanel createModifiersPanel() {
        // Contents
        chkUseCustomRetirementModifiers = new CampaignOptionsCheckBox("UseCustomRetirementModifiers");
        chkUseCustomRetirementModifiers.addMouseListener(createTipPanelUpdater(turnoverHeader,
                "UseCustomRetirementModifiers"));
        chkUseFatigueModifiers = new CampaignOptionsCheckBox("UseFatigueModifiers");
        chkUseFatigueModifiers.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseFatigueModifiers"));
        chkUseSkillModifiers = new CampaignOptionsCheckBox("UseSkillModifiers");
        chkUseSkillModifiers.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseSkillModifiers"));
        chkUseAgeModifiers = new CampaignOptionsCheckBox("UseAgeModifiers");
        chkUseAgeModifiers.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseAgeModifiers"));
        chkUseUnitRatingModifiers = new CampaignOptionsCheckBox("UseUnitRatingModifiers");
        chkUseUnitRatingModifiers.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseUnitRatingModifiers"));
        chkUseFactionModifiers = new CampaignOptionsCheckBox("UseFactionModifiers");
        chkUseFactionModifiers.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseFactionModifiers"));
        chkUseMissionStatusModifiers = new CampaignOptionsCheckBox("UseMissionStatusModifiers");
        chkUseMissionStatusModifiers.addMouseListener(createTipPanelUpdater(turnoverHeader,
                "UseMissionStatusModifiers"));
        chkUseHostileTerritoryModifiers = new CampaignOptionsCheckBox("UseHostileTerritoryModifiers");
        chkUseHostileTerritoryModifiers.addMouseListener(createTipPanelUpdater(turnoverHeader,
                "UseHostileTerritoryModifiers"));
        chkUseFamilyModifiers = new CampaignOptionsCheckBox("UseFamilyModifiers");
        chkUseFamilyModifiers.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseFamilyModifiers"));
        chkUseLoyaltyModifiers = new CampaignOptionsCheckBox("UseLoyaltyModifiers");
        chkUseLoyaltyModifiers.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseLoyaltyModifiers"));
        chkUseHideLoyalty = new CampaignOptionsCheckBox("UseHideLoyalty");
        chkUseHideLoyalty.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseHideLoyalty"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("TurnoverModifiersPanel", true,
                "ModifiersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(chkUseCustomRetirementModifiers, layout);

        layout.gridy++;
        panel.add(chkUseFatigueModifiers, layout);

        layout.gridy++;
        panel.add(chkUseSkillModifiers, layout);

        layout.gridy++;
        panel.add(chkUseAgeModifiers, layout);

        layout.gridy++;
        panel.add(chkUseUnitRatingModifiers, layout);

        layout.gridy++;
        panel.add(chkUseFactionModifiers, layout);

        layout.gridy++;
        panel.add(chkUseMissionStatusModifiers, layout);

        layout.gridy++;
        panel.add(chkUseHostileTerritoryModifiers, layout);

        layout.gridy++;
        panel.add(chkUseFamilyModifiers, layout);

        layout.gridy++;
        panel.add(chkUseLoyaltyModifiers, layout);

        layout.gridy++;
        panel.add(chkUseHideLoyalty, layout);

        return panel;
    }

    /**
     * Creates the payouts panel for the "Turnover" tab. This panel holds settings
     * related to payout rates for officers
     * and enlisted personnel, service bonuses, and retirement multipliers.
     *
     * @return the {@link JPanel} representing the payout settings.
     */
    private JPanel createPayoutsPanel() {
        // Contents
        lblPayoutRateOfficer = new CampaignOptionsLabel("PayoutRateOfficer");
        lblPayoutRateOfficer.addMouseListener(createTipPanelUpdater(turnoverHeader, "PayoutRateOfficer"));
        spnPayoutRateOfficer = new CampaignOptionsSpinner("PayoutRateOfficer",
                3, 0, 12, 1);
        spnPayoutRateOfficer.addMouseListener(createTipPanelUpdater(turnoverHeader, "PayoutRateOfficer"));

        lblPayoutRateEnlisted = new CampaignOptionsLabel("PayoutRateEnlisted");
        lblPayoutRateEnlisted.addMouseListener(createTipPanelUpdater(turnoverHeader, "PayoutRateEnlisted"));
        spnPayoutRateEnlisted = new CampaignOptionsSpinner("PayoutRateEnlisted",
                3, 0, 12, 1);
        spnPayoutRateEnlisted.addMouseListener(createTipPanelUpdater(turnoverHeader, "PayoutRateEnlisted"));

        lblPayoutRetirementMultiplier = new CampaignOptionsLabel("PayoutRetirementMultiplier");
        lblPayoutRetirementMultiplier.addMouseListener(createTipPanelUpdater(turnoverHeader,
                "PayoutRetirementMultiplier"));
        spnPayoutRetirementMultiplier = new CampaignOptionsSpinner("PayoutRetirementMultiplier",
                24, 1, 120, 1);
        spnPayoutRetirementMultiplier.addMouseListener(createTipPanelUpdater(turnoverHeader,
                "PayoutRetirementMultiplier"));

        chkUsePayoutServiceBonus = new CampaignOptionsCheckBox("UsePayoutServiceBonus");
        chkUsePayoutServiceBonus.addMouseListener(createTipPanelUpdater(turnoverHeader, "UsePayoutServiceBonus"));

        lblPayoutServiceBonusRate = new CampaignOptionsLabel("PayoutServiceBonusRate");
        lblPayoutServiceBonusRate.addMouseListener(createTipPanelUpdater(turnoverHeader, "PayoutServiceBonusRate"));
        spnPayoutServiceBonusRate = new CampaignOptionsSpinner("PayoutServiceBonusRate",
                10, 1, 100, 1);
        spnPayoutServiceBonusRate.addMouseListener(createTipPanelUpdater(turnoverHeader, "PayoutServiceBonusRate"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PayoutsPanel", true,
                "PayoutsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblPayoutRateOfficer, layout);
        layout.gridx++;
        panel.add(spnPayoutRateOfficer, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblPayoutRateEnlisted, layout);
        layout.gridx++;
        panel.add(spnPayoutRateEnlisted, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblPayoutRetirementMultiplier, layout);
        layout.gridx++;
        panel.add(spnPayoutRetirementMultiplier, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkUsePayoutServiceBonus, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblPayoutServiceBonusRate, layout);
        layout.gridx++;
        panel.add(spnPayoutServiceBonusRate, layout);

        return panel;
    }

    /**
     * Creates the unit cohesion panel for the "Turnover" tab, which includes
     * settings like HR strain and management
     * skills.
     *
     * @return the {@link JPanel} containing unit cohesion settings.
     */
    private JPanel createUnitCohesionPanel() {
        // Contents
        pnlHRStrainWrapper = createHRStrainWrapperPanel();
        pnlManagementSkillWrapper = createManagementSkillWrapperPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("UnitCohesionPanel", true,
                "UnitCohesionPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(pnlHRStrainWrapper, layout);

        layout.gridy++;
        panel.add(pnlManagementSkillWrapper, layout);

        return panel;
    }

    /**
     * Creates the HR strain wrapper panel. Includes a checkbox to enable HR strain
     * and settings for related capacities
     * and behaviors.
     *
     * @return the {@link JPanel} for managing HR strain settings.
     */
    private JPanel createHRStrainWrapperPanel() {
        // Contents
        chkUseHRStrain = new CampaignOptionsCheckBox("UseHRStrain");
        chkUseHRStrain.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseHRStrain"));
        pnlHRStrain = createHRStrainPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("HRStrainPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(chkUseHRStrain, layout);

        layout.gridy++;
        panel.add(pnlHRStrain, layout);

        return panel;
    }

    /**
     * Creates the panel for HR strain settings, which contains spinners to adjust
     * HR capacity and multi-crew strain
     * dividers.
     *
     * @return the {@link JPanel} for HR strain adjustment.
     */
    private JPanel createHRStrainPanel() {
        // Contents
        lblHRCapacity = new CampaignOptionsLabel("HRCapacity");
        lblHRCapacity.addMouseListener(createTipPanelUpdater(turnoverHeader, "HRCapacity"));
        spnHRCapacity = new CampaignOptionsSpinner("HRCapacity",
                10, 1, 30, 1);
        spnHRCapacity.addMouseListener(createTipPanelUpdater(turnoverHeader, "HRCapacity"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("HRStrain", true,
                "HRStrain");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblHRCapacity, layout);
        layout.gridx++;
        panel.add(spnHRCapacity, layout);

        return panel;
    }

    /**
     * Creates the management skill wrapper panel, which contains settings such as
     * enabling management skill checks and
     * adjusting penalties.
     *
     * @return the {@link JPanel} for managing skill configurations.
     */
    private JPanel createManagementSkillWrapperPanel() {
        // Contents
        chkUseManagementSkill = new CampaignOptionsCheckBox("UseManagementSkill");
        chkUseManagementSkill.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseManagementSkill"));
        pnlManagementSkill = createManagementSkillPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("UnitCohesionPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 2;
        panel.add(chkUseManagementSkill, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(pnlManagementSkill, layout);

        return panel;
    }

    /**
     * Creates the panel for management skill settings, including options for
     * leadership adjustments and penalties.
     *
     * @return the {@link JPanel} for setting management and leadership skill
     *         penalties.
     */
    private JPanel createManagementSkillPanel() {
        // Contents
        chkUseCommanderLeadershipOnly = new CampaignOptionsCheckBox("UseCommanderLeadershipOnly");
        chkUseCommanderLeadershipOnly.addMouseListener(createTipPanelUpdater(turnoverHeader,
                "UseCommanderLeadershipOnly"));

        lblManagementSkillPenalty = new CampaignOptionsLabel("ManagementSkillPenalty");
        lblManagementSkillPenalty.addMouseListener(createTipPanelUpdater(turnoverHeader, "ManagementSkillPenalty"));
        spnManagementSkillPenalty = new CampaignOptionsSpinner("ManagementSkillPenalty",
                0, -10, 10, 1);
        spnManagementSkillPenalty.addMouseListener(createTipPanelUpdater(turnoverHeader, "ManagementSkillPenalty"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ManagementSkill", true,
                "ManagementSkill");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 2;
        panel.add(chkUseCommanderLeadershipOnly, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblManagementSkillPenalty, layout);
        layout.gridx++;
        panel.add(spnManagementSkillPenalty, layout);

        return panel;
    }

    /**
     * Overload of {@code loadValuesFromCampaignOptions} method. Loads values from
     * the current {@link CampaignOptions}
     * instance.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads the current configuration values from the provided
     * {@link CampaignOptions} object and updates the
     * associated UI components in both the Turnover and Fatigue tabs. If no options
     * are provided, the existing campaign
     * options are used.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} instance to load
     *                              settings from, or {@code null} to use
     *                              the current campaign options.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new TurnoverAndRetentionOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the current campaign options based on the configurations in the UI to
     * the given {@link CampaignOptions}.
     * If no options are provided, the current campaign options are updated.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} instance to save
     *                              settings to, or {@code null} to update
     *                              the current campaign options.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        updateModelFromCreatedControls();
        model.applyTo(options);
    }

    private void updateCreatedControlsFromModel() {
        updateTurnoverControlsFromModel();
        updateFatigueControlsFromModel();
    }

    private void updateTurnoverControlsFromModel() {
        if (!turnoverPageCreated || model == null) {
            return;
        }

        chkUseRandomRetirement.setSelected(model.useRandomRetirement);
        spnTurnoverFixedTargetNumber.setValue(model.turnoverFixedTargetNumber);
        comboTurnoverFrequency.setSelectedItem(model.turnoverFrequency);
        chkUseContractCompletionRandomRetirement.setSelected(model.useContractCompletionRandomRetirement);
        chkUseRandomFounderTurnover.setSelected(model.useRandomFounderTurnover);
        chkTrackOriginalUnit.setSelected(model.trackOriginalUnit);
        chkAeroRecruitsHaveUnits.setSelected(model.aeroRecruitsHaveUnits);
        chkUseSubContractSoldiers.setSelected(model.useSubContractSoldiers);
        spnServiceContractDuration.setValue(model.serviceContractDuration);
        spnServiceContractModifier.setValue(model.serviceContractModifier);
        chkPayBonusDefault.setSelected(model.payBonusDefault);
        spnPayBonusDefaultThreshold.setValue(model.payBonusDefaultThreshold);
        chkIncludeCivilians.setSelected(model.includeCivilians);
        chkUseCustomRetirementModifiers.setSelected(model.useCustomRetirementModifiers);
        chkUseFatigueModifiers.setSelected(model.useFatigueModifiers);
        chkUseSkillModifiers.setSelected(model.useSkillModifiers);
        chkUseAgeModifiers.setSelected(model.useAgeModifiers);
        chkUseUnitRatingModifiers.setSelected(model.useUnitRatingModifiers);
        chkUseFactionModifiers.setSelected(model.useFactionModifiers);
        chkUseMissionStatusModifiers.setSelected(model.useMissionStatusModifiers);
        chkUseHostileTerritoryModifiers.setSelected(model.useHostileTerritoryModifiers);
        chkUseFamilyModifiers.setSelected(model.useFamilyModifiers);
        chkUseLoyaltyModifiers.setSelected(model.useLoyaltyModifiers);
        chkUseHideLoyalty.setSelected(model.useHideLoyalty);
        spnPayoutRateOfficer.setValue(model.payoutRateOfficer);
        spnPayoutRateEnlisted.setValue(model.payoutRateEnlisted);
        spnPayoutRetirementMultiplier.setValue(model.payoutRetirementMultiplier);
        chkUsePayoutServiceBonus.setSelected(model.usePayoutServiceBonus);
        spnPayoutServiceBonusRate.setValue(model.payoutServiceBonusRate);
        chkUseHRStrain.setSelected(model.useHRStrain);
        spnHRCapacity.setValue(model.hrCapacity);
        chkUseManagementSkill.setSelected(model.useManagementSkill);
        chkUseCommanderLeadershipOnly.setSelected(model.useCommanderLeadershipOnly);
        spnManagementSkillPenalty.setValue(model.managementSkillPenalty);
    }

    private void updateFatigueControlsFromModel() {
        if (!fatiguePageCreated || model == null) {
            return;
        }

        chkUseFatigue.setSelected(model.useFatigue);
        spnFatigueRate.setValue(model.fatigueRate);
        chkUseInjuryFatigue.setSelected(model.useInjuryFatigue);
        spnFieldKitchenCapacity.setValue(model.fieldKitchenCapacity);
        chkFieldKitchenIgnoreNonCombatants.setSelected(model.fieldKitchenIgnoreNonCombatants);
        spnFatigueUndeploymentThreshold.setValue(model.fatigueUndeploymentThreshold);
        spnFatigueLeaveThreshold.setValue(model.fatigueLeaveThreshold);
    }

    private void updateModelFromCreatedControls() {
        updateModelFromTurnoverControls();
        updateModelFromFatigueControls();
    }

    private void updateModelFromTurnoverControls() {
        if (!turnoverPageCreated || model == null) {
            return;
        }

        model.useRandomRetirement = chkUseRandomRetirement.isSelected();
        model.turnoverFixedTargetNumber = (int) spnTurnoverFixedTargetNumber.getValue();
        model.turnoverFrequency = comboTurnoverFrequency.getSelectedItem();
        model.useContractCompletionRandomRetirement = chkUseContractCompletionRandomRetirement.isSelected();
        model.useRandomFounderTurnover = chkUseRandomFounderTurnover.isSelected();
        model.trackOriginalUnit = chkTrackOriginalUnit.isSelected();
        model.aeroRecruitsHaveUnits = chkAeroRecruitsHaveUnits.isSelected();
        model.useSubContractSoldiers = chkUseSubContractSoldiers.isSelected();
        model.serviceContractDuration = (int) spnServiceContractDuration.getValue();
        model.serviceContractModifier = (int) spnServiceContractModifier.getValue();
        model.payBonusDefault = chkPayBonusDefault.isSelected();
        model.payBonusDefaultThreshold = (int) spnPayBonusDefaultThreshold.getValue();
        model.includeCivilians = chkIncludeCivilians.isSelected();
        model.useCustomRetirementModifiers = chkUseCustomRetirementModifiers.isSelected();
        model.useFatigueModifiers = chkUseFatigueModifiers.isSelected();
        model.useSkillModifiers = chkUseSkillModifiers.isSelected();
        model.useAgeModifiers = chkUseAgeModifiers.isSelected();
        model.useUnitRatingModifiers = chkUseUnitRatingModifiers.isSelected();
        model.useFactionModifiers = chkUseFactionModifiers.isSelected();
        model.useMissionStatusModifiers = chkUseMissionStatusModifiers.isSelected();
        model.useHostileTerritoryModifiers = chkUseHostileTerritoryModifiers.isSelected();
        model.useFamilyModifiers = chkUseFamilyModifiers.isSelected();
        model.useLoyaltyModifiers = chkUseLoyaltyModifiers.isSelected();
        model.useHideLoyalty = chkUseHideLoyalty.isSelected();
        model.payoutRateOfficer = (int) spnPayoutRateOfficer.getValue();
        model.payoutRateEnlisted = (int) spnPayoutRateEnlisted.getValue();
        model.payoutRetirementMultiplier = (int) spnPayoutRetirementMultiplier.getValue();
        model.usePayoutServiceBonus = chkUsePayoutServiceBonus.isSelected();
        model.payoutServiceBonusRate = (int) spnPayoutServiceBonusRate.getValue();
        model.useHRStrain = chkUseHRStrain.isSelected();
        model.hrCapacity = (int) spnHRCapacity.getValue();
        model.useManagementSkill = chkUseManagementSkill.isSelected();
        model.useCommanderLeadershipOnly = chkUseCommanderLeadershipOnly.isSelected();
        model.managementSkillPenalty = (int) spnManagementSkillPenalty.getValue();
    }

    private void updateModelFromFatigueControls() {
        if (!fatiguePageCreated || model == null) {
            return;
        }

        model.useFatigue = chkUseFatigue.isSelected();
        model.fatigueRate = (int) spnFatigueRate.getValue();
        model.useInjuryFatigue = chkUseInjuryFatigue.isSelected();
        model.fieldKitchenCapacity = (int) spnFieldKitchenCapacity.getValue();
        model.fieldKitchenIgnoreNonCombatants = chkFieldKitchenIgnoreNonCombatants.isSelected();
        model.fatigueUndeploymentThreshold = (int) spnFatigueUndeploymentThreshold.getValue();
        model.fatigueLeaveThreshold = (int) spnFatigueLeaveThreshold.getValue();
    }

}
