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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.TurnoverFrequency;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

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

    private static final int TURNOVER_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int TURNOVER_CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private final CampaignOptions campaignOptions;
    private TurnoverAndRetentionOptionsModel model;
    private boolean turnoverPageCreated;
    private boolean fatiguePageCreated;

    // start Turnover Tab
    private CampaignOptionsHeaderPanel turnoverHeader;
    private JCheckBox chkUseRandomRetirement;

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

    private JLabel lblPayoutRateOfficer;
    private JSpinner spnPayoutRateOfficer;
    private JLabel lblPayoutRateEnlisted;
    private JSpinner spnPayoutRateEnlisted;
    private JLabel lblPayoutRetirementMultiplier;
    private JSpinner spnPayoutRetirementMultiplier;
    private JCheckBox chkUsePayoutServiceBonus;
    private JLabel lblPayoutServiceBonusRate;
    private JSpinner spnPayoutServiceBonusRate;

    private JCheckBox chkUseHRStrain;

    private JLabel lblHRCapacity;
    private JSpinner spnHRCapacity;

    private JCheckBox chkUseManagementSkill;

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
    public TurnoverAndRetentionTab(@Nonnull CampaignOptions campaignOptions) {
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

        lblPayoutRateOfficer = new JLabel();
        spnPayoutRateOfficer = new JSpinner();
        lblPayoutRateEnlisted = new JLabel();
        spnPayoutRateEnlisted = new JSpinner();
        lblPayoutRetirementMultiplier = new JLabel();
        spnPayoutRetirementMultiplier = new JSpinner();
        chkUsePayoutServiceBonus = new JCheckBox();
        lblPayoutServiceBonusRate = new JLabel();
        spnPayoutServiceBonusRate = new JSpinner();

        chkUseHRStrain = new JCheckBox();

        lblHRCapacity = new JLabel();
        spnHRCapacity = new JSpinner();

        chkUseManagementSkill = new JCheckBox();

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
    public @Nonnull JPanel createFatigueTab() {
        // Header
        // start Fatigue Tab
                String imageAddress = getImageDirectory() + "logo_clan_mongoose.png";
                CampaignOptionsHeaderPanel fatigueHeader = new CampaignOptionsHeaderPanel("FatigueTab", imageAddress, 0);

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

        JPanel panel = CampaignOptionsPagePanel.builder("FatigueTab", "FatigueTab", imageAddress)
                .header(fatigueHeader)
                .quote("fatigueTab")
                .section("lblFatigueRulesPanel.text", "lblFatigueRulesPanel.summary", createFatigueRulesPanel())
                .section("lblFatigueFieldKitchenPanel.text",
                        "lblFatigueFieldKitchenPanel.summary",
                        createFatigueFieldKitchenPanel())
                .section("lblFatigueAutomationPanel.text",
                        "lblFatigueAutomationPanel.summary",
                        createFatigueAutomationPanel())
                .build();
        fatiguePageCreated = true;
        updateFatigueControlsFromModel();

        return panel;
    }

    private @Nonnull JPanel createFatigueRulesPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("FatigueRulesPanel",
                TURNOVER_LABEL_COLUMN_WIDTH,
                TURNOVER_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseFatigue);
        panel.addRow(lblFatigueRate, spnFatigueRate);
        panel.addCheckBox(chkUseInjuryFatigue);

        return panel;
    }

    private @Nonnull JPanel createFatigueFieldKitchenPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("FatigueFieldKitchenPanel",
                TURNOVER_LABEL_COLUMN_WIDTH,
                TURNOVER_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblFieldKitchenCapacity, spnFieldKitchenCapacity);
        panel.addCheckBox(chkFieldKitchenIgnoreNonCombatants);

        return panel;
    }

    private @Nonnull JPanel createFatigueAutomationPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("FatigueAutomationPanel",
                TURNOVER_LABEL_COLUMN_WIDTH,
                TURNOVER_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblFatigueUndeploymentThreshold, spnFatigueUndeploymentThreshold);
        panel.addRow(lblFatigueLeaveThreshold, spnFatigueLeaveThreshold);

        return panel;
    }

    /**
     * Creates and configures the "Turnover" tab with its relevant components. These
     * include options for turnover
     * control, random retirement, payout settings, and modifiers for HR Strain and
     * cohesion.
     *
     * @return the {@link JPanel} representing the constructed Turnover tab.
     */
    public @Nonnull JPanel createTurnoverTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_duchy_of_andurien.png";
        turnoverHeader = new CampaignOptionsHeaderPanel("TurnoverTab", imageAddress, 0);

        // Contents
        JPanel panel = CampaignOptionsPagePanel.builder("TurnoverTab", "TurnoverTab", imageAddress)
                .header(turnoverHeader)
                .quote("turnoverTab")
                .section("lblTurnoverGeneralPanel.text",
                        "lblTurnoverGeneralPanel.summary",
                        createTurnoverGeneralPanel())
                .section("lblTurnoverContractRulesPanel.text",
                        "lblTurnoverContractRulesPanel.summary",
                        createContractRulesPanel())
                .section("lblTurnoverRetentionBonusPanel.text",
                        "lblTurnoverRetentionBonusPanel.summary",
                        createRetentionBonusPanel())
                .section("lblTurnoverModifiersPanel.text",
                        "lblTurnoverModifiersPanel.summary",
                        createModifiersPanel())
                .section("lblPayoutsPanel.text", "lblPayoutsPanel.summary", createPayoutsPanel())
                .section("lblUnitCohesionPanel.text", "lblUnitCohesionPanel.summary", createUnitCohesionPanel())
                .build();
        turnoverPageCreated = true;
        updateTurnoverControlsFromModel();

        return panel;
    }

    private @Nonnull JPanel createTurnoverGeneralPanel() {
        // Contents
        chkUseRandomRetirement = new CampaignOptionsCheckBox("UseRandomRetirement");
        chkUseRandomRetirement.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseRandomRetirement"));

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

        chkIncludeCivilians = new CampaignOptionsCheckBox("IncludeCivilians",
                getMetadata(new Version(0, 51, 0), CampaignOptionFlag.IMPORTANT));
        chkIncludeCivilians.addMouseListener(createTipPanelUpdater(turnoverHeader, "IncludeCivilians"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TurnoverGeneralPanel",
                TURNOVER_LABEL_COLUMN_WIDTH,
                TURNOVER_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseRandomRetirement);
        panel.addRow(lblTurnoverFixedTargetNumber, spnTurnoverFixedTargetNumber);
        panel.addRow(lblTurnoverFrequency, comboTurnoverFrequency);
        panel.addCheckBox(chkIncludeCivilians);

        return panel;
    }

    private @Nonnull JPanel createContractRulesPanel() {
        // Contents

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

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TurnoverContractRulesPanel",
                TURNOVER_LABEL_COLUMN_WIDTH,
                TURNOVER_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseContractCompletionRandomRetirement,
                chkUseRandomFounderTurnover,
                chkTrackOriginalUnit,
                chkAeroRecruitsHaveUnits,
                chkUseSubContractSoldiers);
        panel.addRow(lblServiceContractDuration, spnServiceContractDuration);
        panel.addRow(lblServiceContractModifier, spnServiceContractModifier);

        return panel;
    }

    private @Nonnull JPanel createRetentionBonusPanel() {
        // Contents
        chkPayBonusDefault = new CampaignOptionsCheckBox("PayBonusDefault");
        chkPayBonusDefault.addMouseListener(createTipPanelUpdater(turnoverHeader, "PayBonusDefault"));

        lblPayBonusDefaultThreshold = new CampaignOptionsLabel("PayBonusDefaultThreshold");
        lblPayBonusDefaultThreshold
                .addMouseListener(createTipPanelUpdater(turnoverHeader, "PayBonusDefaultThreshold"));
        spnPayBonusDefaultThreshold = new CampaignOptionsSpinner("PayBonusDefaultThreshold",
                3, 0, 12, 1);
        spnPayBonusDefaultThreshold
                .addMouseListener(createTipPanelUpdater(turnoverHeader, "PayBonusDefaultThreshold"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TurnoverRetentionBonusPanel",
                TURNOVER_LABEL_COLUMN_WIDTH,
                TURNOVER_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkPayBonusDefault);
        panel.addRow(lblPayBonusDefaultThreshold, spnPayBonusDefaultThreshold);

        return panel;
    }

    /**
     * Creates the modifiers panel for the "Turnover" tab, which contains gameplay
     * modifiers such as age, skill,
     * faction, and loyalty.
     *
     * @return the {@link JPanel} representing the turnover modifiers.
     */
    private @Nonnull JPanel createModifiersPanel() {
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
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TurnoverModifiersPanel",
                TURNOVER_LABEL_COLUMN_WIDTH,
                TURNOVER_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseCustomRetirementModifiers,
                chkUseFatigueModifiers,
                chkUseSkillModifiers,
                chkUseAgeModifiers,
                chkUseUnitRatingModifiers,
                chkUseFactionModifiers,
                chkUseMissionStatusModifiers,
                chkUseHostileTerritoryModifiers,
                chkUseFamilyModifiers,
                chkUseLoyaltyModifiers,
                chkUseHideLoyalty);

        return panel;
    }

    /**
     * Creates the payouts panel for the "Turnover" tab. This panel holds settings
     * related to payout rates for officers
     * and enlisted personnel, service bonuses, and retirement multipliers.
     *
     * @return the {@link JPanel} representing the payout settings.
     */
    private @Nonnull JPanel createPayoutsPanel() {
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
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PayoutsPanel",
                TURNOVER_LABEL_COLUMN_WIDTH,
                TURNOVER_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblPayoutRateOfficer, spnPayoutRateOfficer);
        panel.addRow(lblPayoutRateEnlisted, spnPayoutRateEnlisted);
        panel.addRow(lblPayoutRetirementMultiplier, spnPayoutRetirementMultiplier);
        panel.addCheckBox(chkUsePayoutServiceBonus);
        panel.addRow(lblPayoutServiceBonusRate, spnPayoutServiceBonusRate);

        return panel;
    }

    /**
     * Creates the unit cohesion panel for the "Turnover" tab, which includes
     * settings like HR strain and management
     * skills.
     *
     * @return the {@link JPanel} containing unit cohesion settings.
     */
    private @Nonnull JPanel createUnitCohesionPanel() {
        // Contents
        chkUseHRStrain = new CampaignOptionsCheckBox("UseHRStrain");
        chkUseHRStrain.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseHRStrain"));

        lblHRCapacity = new CampaignOptionsLabel("HRCapacity");
        lblHRCapacity.addMouseListener(createTipPanelUpdater(turnoverHeader, "HRCapacity"));
        spnHRCapacity = new CampaignOptionsSpinner("HRCapacity",
                10, 1, 30, 1);
        spnHRCapacity.addMouseListener(createTipPanelUpdater(turnoverHeader, "HRCapacity"));

        chkUseManagementSkill = new CampaignOptionsCheckBox("UseManagementSkill");
        chkUseManagementSkill.addMouseListener(createTipPanelUpdater(turnoverHeader, "UseManagementSkill"));

        chkUseCommanderLeadershipOnly = new CampaignOptionsCheckBox("UseCommanderLeadershipOnly");
        chkUseCommanderLeadershipOnly.addMouseListener(createTipPanelUpdater(turnoverHeader,
                "UseCommanderLeadershipOnly"));

        lblManagementSkillPenalty = new CampaignOptionsLabel("ManagementSkillPenalty");
        lblManagementSkillPenalty.addMouseListener(createTipPanelUpdater(turnoverHeader, "ManagementSkillPenalty"));
        spnManagementSkillPenalty = new CampaignOptionsSpinner("ManagementSkillPenalty",
                0, -10, 10, 1);
        spnManagementSkillPenalty.addMouseListener(createTipPanelUpdater(turnoverHeader, "ManagementSkillPenalty"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("UnitCohesionPanel",
                TURNOVER_LABEL_COLUMN_WIDTH,
                TURNOVER_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseHRStrain);
        panel.addRow(lblHRCapacity, spnHRCapacity);
        panel.addCheckBoxGrid(2, chkUseManagementSkill, chkUseCommanderLeadershipOnly);
        panel.addRow(lblManagementSkillPenalty, spnManagementSkillPenalty);

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
