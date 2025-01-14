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
import mekhq.campaign.personnel.enums.TurnoverFrequency;
import mekhq.gui.campaignOptions.components.*;

import javax.swing.*;
import java.awt.*;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

/**
 * The {@code TurnoverAndRetentionTab} class represents a graphical user interface (GUI)
 * configuration tab in the campaign options for managing unit turnover, retention, and fatigue settings.
 * <p>
 * This class provides functionality to define and customize gameplay-related options such as:
 * <ul>
 *   <li>Unit turnover settings, including retirement, contract durations, payouts, and modifiers.</li>
 *   <li>Administrative strain and management skills impacting unit cohesion.</li>
 *   <li>Fatigue mechanics such as fatigue rates, leave thresholds, and injury fatigue.</li>
 * </ul>
 * </p>
 * The class interacts with a {@link CampaignOptions} object, allowing the user to load and save
 * configurations. It consists of two main panels:
 * <ul>
 *   <li><strong>Turnover Tab:</strong> Controls unit turnover, payouts, and related modifiers.</li>
 *   <li><strong>Fatigue Tab:</strong> Manages fatigue-related options like kitchen capacity
 *   and fatigue rates.</li>
 * </ul>
 */
public class TurnoverAndRetentionTab {
    private final CampaignOptions campaignOptions;

    //start Turnover Tab
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

    private JPanel pnlAdministrativeStrainWrapper;
    private JCheckBox chkUseAdministrativeStrain;

    private JPanel pnlAdministrativeStrain;
    private JLabel lblAdministrativeCapacity;
    private JSpinner spnAdministrativeCapacity;
    private JLabel lblMultiCrewStrainDivider;
    private JSpinner spnMultiCrewStrainDivider;

    private JPanel pnlManagementSkillWrapper;
    private JCheckBox chkUseManagementSkill;

    private JPanel pnlManagementSkill;
    private JCheckBox chkUseCommanderLeadershipOnly;
    private JLabel lblManagementSkillPenalty;
    private JSpinner spnManagementSkillPenalty;
    //end Turnover Tab

    //start Fatigue Tab
    private JCheckBox chkUseFatigue;
    private JLabel lblFatigueRate;
    private JSpinner spnFatigueRate;
    private JCheckBox chkUseInjuryFatigue;
    private JLabel lblFieldKitchenCapacity;
    private JSpinner spnFieldKitchenCapacity;
    private JCheckBox chkFieldKitchenIgnoreNonCombatants;
    private JLabel lblFatigueLeaveThreshold;
    private JSpinner spnFatigueLeaveThreshold;
    //end Fatigue Tab

    /**
     * Constructs a {@code TurnoverAndRetentionTab} and initializes the tab with the given
     * {@link CampaignOptions}. This sets up necessary UI components and their default
     * configurations.
     *
     * @param campaignOptions the {@code CampaignOptions} instance that holds the settings
     *                        to be modified or displayed in this tab.
     */
    public TurnoverAndRetentionTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
    }

    /**
     * Initializes the content and configuration of the turnover and fatigue tabs.
     * This method sets up their respective panels and components.
     */
    private void initialize() {
        initializeTurnoverTab();
        initializeFatigueTab();
    }

    /**
     * Initializes the content of the fatigue configuration tab.
     * Includes settings such as fatigue rate, injury fatigue, field kitchen capacity,
     * and fatigue leave thresholds.
     */
    private void initializeFatigueTab() {
        chkUseFatigue = new JCheckBox();
        lblFatigueRate = new JLabel();
        spnFatigueRate = new JSpinner();
        chkUseInjuryFatigue = new JCheckBox();
        lblFieldKitchenCapacity = new JLabel();
        spnFieldKitchenCapacity = new JSpinner();
        chkFieldKitchenIgnoreNonCombatants = new JCheckBox();
        lblFatigueLeaveThreshold = new JLabel();
        spnFatigueLeaveThreshold = new JSpinner();
    }

    /**
     * Initializes the content of the turnover configuration tab.
     * Includes settings such as turnover frequencies, service contract details,
     * and retirement/payout modifiers.
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

        pnlAdministrativeStrainWrapper = new JPanel();
        chkUseAdministrativeStrain = new JCheckBox();

        pnlAdministrativeStrain = new JPanel();
        lblAdministrativeCapacity = new JLabel();
        spnAdministrativeCapacity = new JSpinner();
        lblMultiCrewStrainDivider = new JLabel();
        spnMultiCrewStrainDivider = new JSpinner();

        pnlManagementSkillWrapper = new JPanel();
        chkUseManagementSkill = new JCheckBox();

        pnlManagementSkill = new JPanel();
        chkUseCommanderLeadershipOnly = new JCheckBox();
        lblManagementSkillPenalty = new JLabel();
        spnManagementSkillPenalty = new JSpinner();
    }

    /**
     * Creates and configures the "Fatigue" tab with its relevant components.
     * These include options related to enabling fatigue, fatigue rates, injury fatigue,
     * kitchen capacities, and leave thresholds.
     *
     * @return the {@link JPanel} representing the constructed Fatigue tab.
     */
    public JPanel createFatigueTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("FatigueTab",
            getImageDirectory() + "logo_clan_mongoose.png", true);

        // Contents
        chkUseFatigue = new CampaignOptionsCheckBox("UseFatigue");

        lblFatigueRate = new CampaignOptionsLabel("FatigueRate");
        spnFatigueRate = new CampaignOptionsSpinner("FatigueRate",
            1, 1, 10, 1);

        chkUseInjuryFatigue = new CampaignOptionsCheckBox("UseInjuryFatigue");

        lblFieldKitchenCapacity = new CampaignOptionsLabel("FieldKitchenCapacity");
        spnFieldKitchenCapacity = new CampaignOptionsSpinner("FieldKitchenCapacity",
            150, 0, 450, 1);

        chkFieldKitchenIgnoreNonCombatants = new CampaignOptionsCheckBox("FieldKitchenIgnoreNonCombatants");

        lblFatigueLeaveThreshold = new CampaignOptionsLabel("FatigueLeaveThreshold");
        spnFatigueLeaveThreshold = new CampaignOptionsSpinner("FatigueLeaveThreshold",
            13, 0, 17, 1);

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
        panelRight.add(lblFatigueLeaveThreshold, layoutRight);
        layoutRight.gridx++;
        panelRight.add(spnFatigueLeaveThreshold, layoutRight);

        final JPanel panelParent = new CampaignOptionsStandardPanel("FatigueTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridwidth = 1;
        layoutParent.gridy++;
        panelParent.add(panelLeft, layoutParent);
        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "FatigueTab");
    }

    /**
     * Creates and configures the "Turnover" tab with its relevant components.
     * These include options for turnover control, random retirement, payout settings,
     * and modifiers for administrative strain and cohesion.
     *
     * @return the {@link JPanel} representing the constructed Turnover tab.
     */
    public JPanel createTurnoverTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("TurnoverTab",
            getImageDirectory() + "logo_duchy_of_andurien.png", true);

        // Contents
        chkUseRandomRetirement = new CampaignOptionsCheckBox("UseRandomRetirement");
        pnlSettings = createSettingsPanel();
        pnlModifiers = createModifiersPanel();
        pnlPayout = createPayoutsPanel();
        pnlUnitCohesion = createUnitCohesionPanel();

        // Layout the Panel
        final JPanel panelTop = new CampaignOptionsStandardPanel("TurnoverTabTop");
        final GridBagConstraints layoutTop = new CampaignOptionsGridBagConstraints(panelTop);

        layoutTop.gridwidth = 1;
        layoutTop.gridx = 0;
        layoutTop.gridy = 0;
        panelTop.add(chkUseRandomRetirement, layoutTop);

        layoutTop.gridy++;
        panelTop.add(pnlSettings, layoutTop);
        layoutTop.gridx++;
        panelTop.add(pnlModifiers, layoutTop);
        layoutTop.gridx++;
        panelTop.add(pnlPayout, layoutTop);

        // Layout the Panel
        final JPanel panelParent = new CampaignOptionsStandardPanel("TurnoverTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelTop, layoutParent);

        layoutParent.gridy++;
        panelParent.add(pnlUnitCohesion, layoutParent);


        // Create Parent Panel and return
        return createParentPanel(panelParent, "TurnoverTab");
    }

    /**
     * Creates the settings panel for the "Turnover" tab, which organizes various
     * settings like random retirement, contract durations, and bonuses into a layout.
     *
     * @return the {@link JPanel} representing the turnover settings.
     */
    private JPanel createSettingsPanel() {
        // Contents
        lblTurnoverFixedTargetNumber = new CampaignOptionsLabel("TurnoverFixedTargetNumber");
        spnTurnoverFixedTargetNumber = new CampaignOptionsSpinner("TurnoverFixedTargetNumber",
            3, 0, 10, 1);

        lblTurnoverFrequency = new CampaignOptionsLabel("TurnoverFrequency");
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

        chkUseContractCompletionRandomRetirement = new CampaignOptionsCheckBox(
            "UseContractCompletionRandomRetirement");

        chkUseRandomFounderTurnover = new CampaignOptionsCheckBox("UseRandomFounderTurnover");

        chkTrackOriginalUnit = new CampaignOptionsCheckBox("TrackOriginalUnit");

        chkAeroRecruitsHaveUnits = new CampaignOptionsCheckBox("AeroRecruitsHaveUnits");

        chkUseSubContractSoldiers = new CampaignOptionsCheckBox("UseSubContractSoldiers");

        lblServiceContractDuration = new CampaignOptionsLabel("ServiceContractDuration");
        spnServiceContractDuration = new CampaignOptionsSpinner("ServiceContractDuration",
            36, 0, 120, 1);

        lblServiceContractModifier = new CampaignOptionsLabel("ServiceContractModifier");
        spnServiceContractModifier = new CampaignOptionsSpinner("ServiceContractModifier",
            3, 0, 10, 1);

        chkPayBonusDefault = new CampaignOptionsCheckBox("PayBonusDefault");

        lblPayBonusDefaultThreshold = new CampaignOptionsLabel("PayBonusDefaultThreshold");
        spnPayBonusDefaultThreshold = new CampaignOptionsSpinner("PayBonusDefaultThreshold",
            3, 0, 12, 1);

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

        return panel;
    }

    /**
     * Creates the modifiers panel for the "Turnover" tab, which contains gameplay
     * modifiers such as age, skill, faction, and loyalty.
     *
     * @return the {@link JPanel} representing the turnover modifiers.
     */
    private JPanel createModifiersPanel() {
        // Contents
        chkUseCustomRetirementModifiers = new CampaignOptionsCheckBox("UseCustomRetirementModifiers");
        chkUseFatigueModifiers = new CampaignOptionsCheckBox("UseFatigueModifiers");
        chkUseSkillModifiers = new CampaignOptionsCheckBox("UseSkillModifiers");
        chkUseAgeModifiers = new CampaignOptionsCheckBox("UseAgeModifiers");
        chkUseUnitRatingModifiers = new CampaignOptionsCheckBox("UseUnitRatingModifiers");
        chkUseFactionModifiers = new CampaignOptionsCheckBox("UseFactionModifiers");
        chkUseMissionStatusModifiers = new CampaignOptionsCheckBox("UseMissionStatusModifiers");
        chkUseHostileTerritoryModifiers = new CampaignOptionsCheckBox("UseHostileTerritoryModifiers");
        chkUseFamilyModifiers = new CampaignOptionsCheckBox("UseFamilyModifiers");
        chkUseLoyaltyModifiers = new CampaignOptionsCheckBox("UseLoyaltyModifiers");
        chkUseHideLoyalty = new CampaignOptionsCheckBox("UseHideLoyalty");

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
     * Creates the payouts panel for the "Turnover" tab. This panel holds settings related to
     * payout rates for officers and enlisted personnel, service bonuses, and retirement multipliers.
     *
     * @return the {@link JPanel} representing the payout settings.
     */
    private JPanel createPayoutsPanel() {
        // Contents
        lblPayoutRateOfficer = new CampaignOptionsLabel("PayoutRateOfficer");
        spnPayoutRateOfficer = new CampaignOptionsSpinner("PayoutRateOfficer",
            3, 0, 12, 1);

        lblPayoutRateEnlisted = new CampaignOptionsLabel("PayoutRateEnlisted");
        spnPayoutRateEnlisted = new CampaignOptionsSpinner("PayoutRateEnlisted",
            3, 0, 12, 1);

        lblPayoutRetirementMultiplier = new CampaignOptionsLabel("PayoutRetirementMultiplier");
        spnPayoutRetirementMultiplier = new CampaignOptionsSpinner("PayoutRetirementMultiplier",
            24, 1, 120, 1);

        chkUsePayoutServiceBonus = new CampaignOptionsCheckBox("UsePayoutServiceBonus");

        lblPayoutServiceBonusRate = new CampaignOptionsLabel("PayoutServiceBonusRate");
        spnPayoutServiceBonusRate = new CampaignOptionsSpinner("PayoutServiceBonusRate",
            10, 1, 100, 1);

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
     * Creates the unit cohesion panel for the "Turnover" tab, which includes settings like
     * administrative strain and management skills.
     *
     * @return the {@link JPanel} containing unit cohesion settings.
     */
    private JPanel createUnitCohesionPanel() {
        // Contents
        pnlAdministrativeStrainWrapper = createAdministrativeStrainWrapperPanel();
        pnlManagementSkillWrapper = createManagementSkillWrapperPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("UnitCohesionPanel", true,
            "UnitCohesionPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(pnlAdministrativeStrainWrapper, layout);
        layout.gridx++;
        panel.add(pnlManagementSkillWrapper, layout);

        return panel;
    }

    /**
     * Creates the administrative strain wrapper panel. Includes a checkbox to enable
     * administrative strain and settings for related capacities and behaviors.
     *
     * @return the {@link JPanel} for managing administrative strain settings.
     */
    private JPanel createAdministrativeStrainWrapperPanel() {
        // Contents
        chkUseAdministrativeStrain = new CampaignOptionsCheckBox("UseAdministrativeStrain");
        pnlAdministrativeStrain = createAdministrativeStrainPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AdministrativeStrainPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(chkUseAdministrativeStrain, layout);

        layout.gridy++;
        panel.add(pnlAdministrativeStrain, layout);

        return panel;
    }

    /**
     * Creates the panel for administrative strain settings, which contains
     * spinners to adjust administrative capacity and multi-crew strain dividers.
     *
     * @return the {@link JPanel} for administrative strain adjustment.
     */
    private JPanel createAdministrativeStrainPanel() {
        // Contents
        lblAdministrativeCapacity = new CampaignOptionsLabel("AdministrativeCapacity");
        spnAdministrativeCapacity = new CampaignOptionsSpinner("AdministrativeCapacity",
            10, 1, 30, 1);

        lblMultiCrewStrainDivider = new CampaignOptionsLabel("MultiCrewStrainDivider");
        spnMultiCrewStrainDivider = new CampaignOptionsSpinner("MultiCrewStrainDivider",
            5, 1, 25, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AdministrativeStrain", true,
            "AdministrativeStrain");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblAdministrativeCapacity, layout);
        layout.gridx++;
        panel.add(spnAdministrativeCapacity, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblMultiCrewStrainDivider, layout);
        layout.gridx++;
        panel.add(spnMultiCrewStrainDivider, layout);

        return panel;
    }

    /**
     * Creates the management skill wrapper panel, which contains settings such as enabling
     * management skill checks and adjusting penalties.
     *
     * @return the {@link JPanel} for managing skill configurations.
     */
    private JPanel createManagementSkillWrapperPanel() {
        // Contents
        chkUseManagementSkill = new CampaignOptionsCheckBox("UseManagementSkill");
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
     * Creates the panel for management skill settings, including options for leadership adjustments
     * and penalties.
     *
     * @return the {@link JPanel} for setting management and leadership skill penalties.
     */
    private JPanel createManagementSkillPanel() {
        // Contents
        chkUseCommanderLeadershipOnly = new CampaignOptionsCheckBox("UseCommanderLeadershipOnly");

        lblManagementSkillPenalty = new CampaignOptionsLabel("ManagementSkillPenalty");
        spnManagementSkillPenalty = new CampaignOptionsSpinner("ManagementSkillPenalty",
            0, -10, 10, 1);

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
     * Overload of {@code loadValuesFromCampaignOptions} method.
     * Loads values from the current {@link CampaignOptions} instance.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads the current configuration values from the provided {@link CampaignOptions}
     * object and updates the associated UI components in both the Turnover and Fatigue tabs.
     * If no options are provided, the existing campaign options are used.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} instance to load settings from,
     *                              or {@code null} to use the current campaign options.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Turnover
        chkUseRandomRetirement.setSelected(options.isUseRandomRetirement());
        spnTurnoverFixedTargetNumber.setValue(options.getTurnoverFixedTargetNumber());
        comboTurnoverFrequency.setSelectedItem(options.getTurnoverFrequency());
        chkUseContractCompletionRandomRetirement.setSelected(options.isUseContractCompletionRandomRetirement());
        chkUseRandomFounderTurnover.setSelected(options.isUseRandomFounderTurnover());
        chkTrackOriginalUnit.setSelected(options.isTrackOriginalUnit());
        chkAeroRecruitsHaveUnits.setSelected(options.isAeroRecruitsHaveUnits());
        chkUseSubContractSoldiers.setSelected(options.isUseSubContractSoldiers());
        spnServiceContractDuration.setValue(options.getServiceContractDuration());
        spnServiceContractModifier.setValue(options.getServiceContractModifier());
        chkPayBonusDefault.setSelected(options.isPayBonusDefault());
        spnPayBonusDefaultThreshold.setValue(options.getPayBonusDefaultThreshold());
        chkUseCustomRetirementModifiers.setSelected(options.isUseCustomRetirementModifiers());
        chkUseFatigueModifiers.setSelected(options.isUseFatigueModifiers());
        chkUseSkillModifiers.setSelected(options.isUseSkillModifiers());
        chkUseAgeModifiers.setSelected(options.isUseAgeModifiers());
        chkUseUnitRatingModifiers.setSelected(options.isUseUnitRatingModifiers());
        chkUseFactionModifiers.setSelected(options.isUseFactionModifiers());
        chkUseMissionStatusModifiers.setSelected(options.isUseMissionStatusModifiers());
        chkUseHostileTerritoryModifiers.setSelected(options.isUseHostileTerritoryModifiers());
        chkUseFamilyModifiers.setSelected(options.isUseFamilyModifiers());
        chkUseLoyaltyModifiers.setSelected(options.isUseLoyaltyModifiers());
        chkUseHideLoyalty.setSelected(options.isUseHideLoyalty());
        spnPayoutRateOfficer.setValue(options.getPayoutRateOfficer());
        spnPayoutRateEnlisted.setValue(options.getPayoutRateEnlisted());
        spnPayoutRetirementMultiplier.setValue(options.getPayoutRetirementMultiplier());
        chkUsePayoutServiceBonus.setSelected(options.isUsePayoutServiceBonus());
        spnPayoutServiceBonusRate.setValue(options.getPayoutServiceBonusRate());
        chkUseAdministrativeStrain.setSelected(options.isUseAdministrativeStrain());
        spnAdministrativeCapacity.setValue(options.getAdministrativeCapacity());
        spnMultiCrewStrainDivider.setValue(options.getMultiCrewStrainDivider());
        chkUseManagementSkill.setSelected(options.isUseManagementSkill());
        chkUseCommanderLeadershipOnly.setSelected(options.isUseCommanderLeadershipOnly());
        spnManagementSkillPenalty.setValue(options.getManagementSkillPenalty());

        // Fatigue
        chkUseFatigue.setSelected(options.isUseFatigue());
        spnFatigueRate.setValue(options.getFatigueRate());
        chkUseInjuryFatigue.setSelected(options.isUseInjuryFatigue());
        spnFieldKitchenCapacity.setValue(options.getFieldKitchenCapacity());
        chkFieldKitchenIgnoreNonCombatants.setSelected(options.isUseFieldKitchenIgnoreNonCombatants());
        spnFatigueLeaveThreshold.setValue(options.getFatigueLeaveThreshold());
    }

    /**
     * Applies the current campaign options based on the configurations in the UI
     * to the given {@link CampaignOptions}. If no options are provided, the current
     * campaign options are updated.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} instance to save settings to,
     *                              or {@code null} to update the current campaign options.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Turnover
        options.setUseRandomRetirement(chkUseRandomRetirement.isSelected());
        options.setTurnoverFixedTargetNumber((int) spnTurnoverFixedTargetNumber.getValue());
        options.setTurnoverFrequency(comboTurnoverFrequency.getSelectedItem());
        options.setUseContractCompletionRandomRetirement(chkUseContractCompletionRandomRetirement.isSelected());
        options.setUseRandomFounderTurnover(chkUseRandomFounderTurnover.isSelected());
        options.setTrackOriginalUnit(chkTrackOriginalUnit.isSelected());
        options.setAeroRecruitsHaveUnits(chkAeroRecruitsHaveUnits.isSelected());
        options.setUseSubContractSoldiers(chkUseSubContractSoldiers.isSelected());
        options.setServiceContractDuration((int) spnServiceContractDuration.getValue());
        options.setServiceContractModifier((int) spnServiceContractModifier.getValue());
        options.setPayBonusDefault(chkPayBonusDefault.isSelected());
        options.setPayBonusDefaultThreshold((int) spnPayBonusDefaultThreshold.getValue());
        options.setUseCustomRetirementModifiers(chkUseCustomRetirementModifiers.isSelected());
        options.setUseFatigueModifiers(chkUseFatigueModifiers.isSelected());
        options.setUseSkillModifiers(chkUseSkillModifiers.isSelected());
        options.setUseAgeModifiers(chkUseAgeModifiers.isSelected());
        options.setUseUnitRatingModifiers(chkUseUnitRatingModifiers.isSelected());
        options.setUseFactionModifiers(chkUseFactionModifiers.isSelected());
        options.setUseMissionStatusModifiers(chkUseMissionStatusModifiers.isSelected());
        options.setUseHostileTerritoryModifiers(chkUseHostileTerritoryModifiers.isSelected());
        options.setUseFamilyModifiers(chkUseFamilyModifiers.isSelected());
        options.setUseLoyaltyModifiers(chkUseLoyaltyModifiers.isSelected());
        options.setUseHideLoyalty(chkUseHideLoyalty.isSelected());
        options.setPayoutRateOfficer((int) spnPayoutRateOfficer.getValue());
        options.setPayoutRateEnlisted((int) spnPayoutRateEnlisted.getValue());
        options.setPayoutRetirementMultiplier((int) spnPayoutRetirementMultiplier.getValue());
        options.setUsePayoutServiceBonus(chkUsePayoutServiceBonus.isSelected());
        options.setPayoutServiceBonusRate((int) spnPayoutServiceBonusRate.getValue());
        options.setUseAdministrativeStrain(chkUseAdministrativeStrain.isSelected());
        options.setAdministrativeCapacity((int) spnAdministrativeCapacity.getValue());
        options.setMultiCrewStrainDivider((int) spnMultiCrewStrainDivider.getValue());
        options.setUseManagementSkill(chkUseManagementSkill.isSelected());
        options.setUseCommanderLeadershipOnly(chkUseCommanderLeadershipOnly.isSelected());
        options.setManagementSkillPenalty((int) spnManagementSkillPenalty.getValue());

        // Fatigue
        options.setUseFatigue(chkUseFatigue.isSelected());
        options.setFatigueRate((int) spnFatigueRate.getValue());
        options.setUseInjuryFatigue(chkUseInjuryFatigue.isSelected());
        options.setFieldKitchenCapacity((int) spnFieldKitchenCapacity.getValue());
        options.setFatigueLeaveThreshold((int) spnFatigueLeaveThreshold.getValue());
    }
}
