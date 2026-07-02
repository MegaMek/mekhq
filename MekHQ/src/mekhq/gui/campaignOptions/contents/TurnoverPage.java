/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.personnel.enums.TurnoverFrequency;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code TurnoverPage} class builds and manages the Turnover leaf page of the Campaign Options dialog. It owns the
 * widgets for unit turnover configuration - random retirement, contract rules, retention bonuses, the turnover
 * modifiers, payouts, and unit cohesion - and synchronises them with a shared {@link TurnoverAndRetentionOptionsModel}.
 *
 * <p>This view is a sub-component of {@link TurnoverAndRetentionPages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code TurnoverAndRetentionPages}, while this class is responsible only for constructing the
 * Turnover panel and copying turnover values to and from the model. The page is built lazily; until
 * {@link #createPanel(TurnoverAndRetentionOptionsModel)} is called,
 * {@link #readFromModel(TurnoverAndRetentionOptionsModel)} and {@link #writeToModel(TurnoverAndRetentionOptionsModel)}
 * are no-ops.</p>
 */
class TurnoverPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

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

    private boolean created;

    /**
     * Creates and configures the "Turnover" page with its relevant components. These
     * include options for turnover
     * control, random retirement, payout settings, and modifiers for HR Strain and
     * cohesion.
     *
     * @param model the shared turnover and retention options model to populate the freshly built controls from
     *
     * @return the {@link JPanel} representing the constructed Turnover page.
     */
    @Nonnull JPanel createPanel(@Nullable TurnoverAndRetentionOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_duchy_of_andurien.png";
        turnoverHeader = new CampaignOptionsHeaderPanel("TurnoverPage", imageAddress);

        // Contents
        comboTurnoverFrequency = new MMComboBox<>("comboTurnoverFrequency", TurnoverFrequency.values());

        JPanel panel = CampaignOptionsPagePanel.builder("TurnoverPage", "TurnoverPage", imageAddress)
                .header(turnoverHeader)
                .quote("turnoverPage")
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

        created = true;
        readFromModel(model);

        return panel;
    }

    private @Nonnull JPanel createTurnoverGeneralPanel() {
        // Contents
        chkUseRandomRetirement = new CampaignOptionsCheckBox("UseRandomRetirement");
        chkUseRandomRetirement.addMouseListener(createTipPanelUpdater("UseRandomRetirement"));

        lblTurnoverFixedTargetNumber = new CampaignOptionsLabel("TurnoverFixedTargetNumber");
        lblTurnoverFixedTargetNumber.addMouseListener(createTipPanelUpdater("TurnoverFixedTargetNumber"));
        spnTurnoverFixedTargetNumber = new CampaignOptionsSpinner("TurnoverFixedTargetNumber",
                3, 0, 10, 1);
        spnTurnoverFixedTargetNumber.addMouseListener(createTipPanelUpdater("TurnoverFixedTargetNumber"));

        lblTurnoverFrequency = new CampaignOptionsLabel("TurnoverFrequency",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.RECOMMENDED));
        lblTurnoverFrequency.addMouseListener(createTipPanelUpdater("TurnoverFrequency"));
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
        comboTurnoverFrequency.addMouseListener(createTipPanelUpdater("TurnoverFrequency"));

        chkIncludeCivilians = new CampaignOptionsCheckBox("IncludeCivilians",
                getMetadata(new Version(0, 51, 0), CampaignOptionFlag.IMPORTANT));
        chkIncludeCivilians.addMouseListener(createTipPanelUpdater("IncludeCivilians"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TurnoverGeneralPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
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
        chkUseContractCompletionRandomRetirement.addMouseListener(createTipPanelUpdater("UseContractCompletionRandomRetirement"));

        chkUseRandomFounderTurnover = new CampaignOptionsCheckBox("UseRandomFounderTurnover");
        chkUseRandomFounderTurnover
                .addMouseListener(createTipPanelUpdater("UseRandomFounderTurnover"));

        chkTrackOriginalUnit = new CampaignOptionsCheckBox("TrackOriginalUnit");
        chkTrackOriginalUnit.addMouseListener(createTipPanelUpdater("TrackOriginalUnit"));

        chkAeroRecruitsHaveUnits = new CampaignOptionsCheckBox("AeroRecruitsHaveUnits");
        chkAeroRecruitsHaveUnits.addMouseListener(createTipPanelUpdater("AeroRecruitsHaveUnits"));

        chkUseSubContractSoldiers = new CampaignOptionsCheckBox("UseSubContractSoldiers");
        chkUseSubContractSoldiers.addMouseListener(createTipPanelUpdater("UseSubContractSoldiers"));

        lblServiceContractDuration = new CampaignOptionsLabel("ServiceContractDuration");
        lblServiceContractDuration
                .addMouseListener(createTipPanelUpdater("ServiceContractDuration"));
        spnServiceContractDuration = new CampaignOptionsSpinner("ServiceContractDuration",
                36, 0, 120, 1);
        spnServiceContractDuration
                .addMouseListener(createTipPanelUpdater("ServiceContractDuration"));

        lblServiceContractModifier = new CampaignOptionsLabel("ServiceContractModifier");
        lblServiceContractModifier
                .addMouseListener(createTipPanelUpdater("ServiceContractModifier"));
        spnServiceContractModifier = new CampaignOptionsSpinner("ServiceContractModifier",
                3, 0, 10, 1);
        spnServiceContractModifier
                .addMouseListener(createTipPanelUpdater("ServiceContractModifier"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TurnoverContractRulesPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
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
        chkPayBonusDefault.addMouseListener(createTipPanelUpdater("PayBonusDefault"));

        lblPayBonusDefaultThreshold = new CampaignOptionsLabel("PayBonusDefaultThreshold");
        lblPayBonusDefaultThreshold
                .addMouseListener(createTipPanelUpdater("PayBonusDefaultThreshold"));
        spnPayBonusDefaultThreshold = new CampaignOptionsSpinner("PayBonusDefaultThreshold",
                3, 0, 12, 1);
        spnPayBonusDefaultThreshold
                .addMouseListener(createTipPanelUpdater("PayBonusDefaultThreshold"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TurnoverRetentionBonusPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkPayBonusDefault);
        panel.addRow(lblPayBonusDefaultThreshold, spnPayBonusDefaultThreshold);

        return panel;
    }

    /**
     * Creates the modifiers panel for the "Turnover" page, which contains gameplay
     * modifiers such as age, skill,
     * faction, and loyalty.
     *
     * @return the {@link JPanel} representing the turnover modifiers.
     */
    private @Nonnull JPanel createModifiersPanel() {
        // Contents
        chkUseCustomRetirementModifiers = new CampaignOptionsCheckBox("UseCustomRetirementModifiers");
        chkUseCustomRetirementModifiers.addMouseListener(createTipPanelUpdater("UseCustomRetirementModifiers"));
        chkUseFatigueModifiers = new CampaignOptionsCheckBox("UseFatigueModifiers");
        chkUseFatigueModifiers.addMouseListener(createTipPanelUpdater("UseFatigueModifiers"));
        chkUseSkillModifiers = new CampaignOptionsCheckBox("UseSkillModifiers");
        chkUseSkillModifiers.addMouseListener(createTipPanelUpdater("UseSkillModifiers"));
        chkUseAgeModifiers = new CampaignOptionsCheckBox("UseAgeModifiers");
        chkUseAgeModifiers.addMouseListener(createTipPanelUpdater("UseAgeModifiers"));
        chkUseUnitRatingModifiers = new CampaignOptionsCheckBox("UseUnitRatingModifiers");
        chkUseUnitRatingModifiers.addMouseListener(createTipPanelUpdater("UseUnitRatingModifiers"));
        chkUseFactionModifiers = new CampaignOptionsCheckBox("UseFactionModifiers");
        chkUseFactionModifiers.addMouseListener(createTipPanelUpdater("UseFactionModifiers"));
        chkUseMissionStatusModifiers = new CampaignOptionsCheckBox("UseMissionStatusModifiers");
        chkUseMissionStatusModifiers.addMouseListener(createTipPanelUpdater("UseMissionStatusModifiers"));
        chkUseHostileTerritoryModifiers = new CampaignOptionsCheckBox("UseHostileTerritoryModifiers");
        chkUseHostileTerritoryModifiers.addMouseListener(createTipPanelUpdater("UseHostileTerritoryModifiers"));
        chkUseFamilyModifiers = new CampaignOptionsCheckBox("UseFamilyModifiers");
        chkUseFamilyModifiers.addMouseListener(createTipPanelUpdater("UseFamilyModifiers"));
        chkUseLoyaltyModifiers = new CampaignOptionsCheckBox("UseLoyaltyModifiers");
        chkUseLoyaltyModifiers.addMouseListener(createTipPanelUpdater("UseLoyaltyModifiers"));
        chkUseHideLoyalty = new CampaignOptionsCheckBox("UseHideLoyalty");
        chkUseHideLoyalty.addMouseListener(createTipPanelUpdater("UseHideLoyalty"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TurnoverModifiersPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
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
     * Creates the payouts panel for the "Turnover" page. This panel holds settings
     * related to payout rates for officers
     * and enlisted personnel, service bonuses, and retirement multipliers.
     *
     * @return the {@link JPanel} representing the payout settings.
     */
    private @Nonnull JPanel createPayoutsPanel() {
        // Contents
        lblPayoutRateOfficer = new CampaignOptionsLabel("PayoutRateOfficer");
        lblPayoutRateOfficer.addMouseListener(createTipPanelUpdater("PayoutRateOfficer"));
        spnPayoutRateOfficer = new CampaignOptionsSpinner("PayoutRateOfficer",
                3, 0, 12, 1);
        spnPayoutRateOfficer.addMouseListener(createTipPanelUpdater("PayoutRateOfficer"));

        lblPayoutRateEnlisted = new CampaignOptionsLabel("PayoutRateEnlisted");
        lblPayoutRateEnlisted.addMouseListener(createTipPanelUpdater("PayoutRateEnlisted"));
        spnPayoutRateEnlisted = new CampaignOptionsSpinner("PayoutRateEnlisted",
                3, 0, 12, 1);
        spnPayoutRateEnlisted.addMouseListener(createTipPanelUpdater("PayoutRateEnlisted"));

        lblPayoutRetirementMultiplier = new CampaignOptionsLabel("PayoutRetirementMultiplier");
        lblPayoutRetirementMultiplier.addMouseListener(createTipPanelUpdater("PayoutRetirementMultiplier"));
        spnPayoutRetirementMultiplier = new CampaignOptionsSpinner("PayoutRetirementMultiplier",
                24, 1, 120, 1);
        spnPayoutRetirementMultiplier.addMouseListener(createTipPanelUpdater("PayoutRetirementMultiplier"));

        chkUsePayoutServiceBonus = new CampaignOptionsCheckBox("UsePayoutServiceBonus");
        chkUsePayoutServiceBonus.addMouseListener(createTipPanelUpdater("UsePayoutServiceBonus"));

        lblPayoutServiceBonusRate = new CampaignOptionsLabel("PayoutServiceBonusRate");
        lblPayoutServiceBonusRate.addMouseListener(createTipPanelUpdater("PayoutServiceBonusRate"));
        spnPayoutServiceBonusRate = new CampaignOptionsSpinner("PayoutServiceBonusRate",
                10, 1, 100, 1);
        spnPayoutServiceBonusRate.addMouseListener(createTipPanelUpdater("PayoutServiceBonusRate"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PayoutsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblPayoutRateOfficer, spnPayoutRateOfficer);
        panel.addRow(lblPayoutRateEnlisted, spnPayoutRateEnlisted);
        panel.addRow(lblPayoutRetirementMultiplier, spnPayoutRetirementMultiplier);
        panel.addCheckBox(chkUsePayoutServiceBonus);
        panel.addRow(lblPayoutServiceBonusRate, spnPayoutServiceBonusRate);

        return panel;
    }

    /**
     * Creates the unit cohesion panel for the "Turnover" page, which includes
     * settings like HR strain and management
     * skills.
     *
     * @return the {@link JPanel} containing unit cohesion settings.
     */
    private @Nonnull JPanel createUnitCohesionPanel() {
        // Contents
        chkUseHRStrain = new CampaignOptionsCheckBox("UseHRStrain");
        chkUseHRStrain.addMouseListener(createTipPanelUpdater("UseHRStrain"));

        lblHRCapacity = new CampaignOptionsLabel("HRCapacity");
        lblHRCapacity.addMouseListener(createTipPanelUpdater("HRCapacity"));
        spnHRCapacity = new CampaignOptionsSpinner("HRCapacity",
                10, 1, 30, 1);
        spnHRCapacity.addMouseListener(createTipPanelUpdater("HRCapacity"));

        chkUseManagementSkill = new CampaignOptionsCheckBox("UseManagementSkill");
        chkUseManagementSkill.addMouseListener(createTipPanelUpdater("UseManagementSkill"));

        chkUseCommanderLeadershipOnly = new CampaignOptionsCheckBox("UseCommanderLeadershipOnly");
        chkUseCommanderLeadershipOnly.addMouseListener(createTipPanelUpdater("UseCommanderLeadershipOnly"));

        lblManagementSkillPenalty = new CampaignOptionsLabel("ManagementSkillPenalty");
        lblManagementSkillPenalty.addMouseListener(createTipPanelUpdater("ManagementSkillPenalty"));
        spnManagementSkillPenalty = new CampaignOptionsSpinner("ManagementSkillPenalty",
                0, -10, 10, 1);
        spnManagementSkillPenalty.addMouseListener(createTipPanelUpdater("ManagementSkillPenalty"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("UnitCohesionPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseHRStrain);
        panel.addRow(lblHRCapacity, spnHRCapacity);
        panel.addCheckBoxGrid(2, chkUseManagementSkill, chkUseCommanderLeadershipOnly);
        panel.addRow(lblManagementSkillPenalty, spnManagementSkillPenalty);

        return panel;
    }

    /**
     * Copies turnover values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared turnover and retention options model to read values from
     */
    void readFromModel(@Nullable TurnoverAndRetentionOptionsModel model) {
        if (!created || model == null) {
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

    /**
     * Copies turnover values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared turnover and retention options model to write values into
     */
    void writeToModel(@Nullable TurnoverAndRetentionOptionsModel model) {
        if (!created || model == null) {
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
}
