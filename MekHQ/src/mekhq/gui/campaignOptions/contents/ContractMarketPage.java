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

import static mekhq.gui.campaignOptions.CampaignOptionFlag.CUSTOM_SYSTEM;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.settings.SettingsFormPanel;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * The {@code ContractMarketPage} class builds and manages the Contract Market leaf page of the Campaign Options dialog.
 * It owns the widgets for contract market configuration - the market method, search radius, salvage and difficulty
 * toggles, and the equipment/personnel contract-pay subsections - and synchronises them with a shared
 * {@link MarketsOptionsModel}.
 *
 * <p>This view is a sub-component of {@link MarketsPages}: the model snapshot and the overall load/apply lifecycle
 * still
 * live on {@code MarketsPages}, while this class is responsible only for constructing the Contract Market panel and
 * copying contract market values to and from the model. The page is built lazily; until
 * {@link #createPanel(MarketsOptionsModel)} is called, {@link #readFromModel(MarketsOptionsModel)} and
 * {@link #writeToModel(MarketsOptionsModel)} are no-ops.</p>
 */
class ContractMarketPage {
    private static final int LABEL_COLUMN_WIDTH = SettingsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = SettingsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int CHECKBOX_GRID_COLUMNS = 2;
    private static final int CONTRACT_PAY_OPTION_INDENT = 24;
    // The contract-pay option panels are indented under their radio-button headers,
    // which would otherwise push their
    // control column right of the Market Rules section's control column. Shrinking
    // their label column by the indent
    // keeps the indent (the visual nesting) while landing the control column at the
    // same x as Market Rules.
    private static final int CONTRACT_PAY_LABEL_COLUMN_WIDTH = LABEL_COLUMN_WIDTH - CONTRACT_PAY_OPTION_INDENT;

    private CampaignOptionsHeaderPanel contractMarketHeader;
    private JPanel pnlContractMarketGeneralOptions;
    private JLabel lblContractMarketMethod;
    private MMComboBox<ContractMarketMethod> comboContractMarketMethod;
    private JLabel lblContractSearchRadius;
    private JSpinner spnContractSearchRadius;
    private JCheckBox chkVariableContractLength;
    private JCheckBox chkUseTwoWayPay;
    private JCheckBox chkUseCamOpsSalvage;
    private JCheckBox chkUseRiskySalvage;
    private JCheckBox chkEnableSalvageFlagByDefault;
    private JCheckBox chkUseDynamicDifficulty;
    private JCheckBox chkUseBolsterContractSkill;
    private JCheckBox chkUseChaosScaleSupportPointConversion;
    private JCheckBox chkUseContractFactionModifiers;
    private JCheckBox chkUseIntelObfuscation;
    private JLabel lblDropShipBonusPercentage;
    private JSpinner spnDropShipBonusPercentage;
    private JLabel lblPityContracts;
    private JSpinner spnPityContracts;
    private JLabel lblContractBasePayMultiplier;
    private JSpinner spnContractBasePayMultiplier;
    private JLabel lblContractStraightSupportMultiplier;
    private JSpinner spnContractStraightSupportMultiplier;
    private JLabel lblContractBattlefieldLossMultiplier;
    private JSpinner spnContractBattlefieldLossMultiplier;
    private JLabel lblContractTransportMultiplier;
    private JSpinner spnContractTransportMultiplier;
    private JLabel lblContractSalvageMultiplier;
    private JSpinner spnContractSalvageMultiplier;

    private JPanel pnlContractTermMultipliers;

    private JPanel pnlContractPay;
    private JCheckBox chkUseChaosSupportPointConversion;
    private JCheckBox chkUseLegacyOptions;
    private JPanel pnlLegacyContractPayOptions;
    private JRadioButton btnContractEquipment;
    private JPanel pnlContractPayEquipmentOptions;
    private JLabel lblEquipPercent;
    private JSpinner spnEquipPercent;
    private JCheckBox chkUseAlternatePaymentMode;
    private JCheckBox chkUseDiminishingContractPay;
    private JCheckBox chkEquipContractSaleValue;
    private JLabel lblDropShipPercent;
    private JSpinner spnDropShipPercent;
    private JLabel lblJumpShipPercent;
    private JSpinner spnJumpShipPercent;
    private JLabel lblWarShipPercent;
    private JSpinner spnWarShipPercent;
    private JRadioButton btnContractPersonnel;
    private JCheckBox chkUseInfantryDoesNotCount;
    private JCheckBox chkBLCSaleValue;
    private JCheckBox chkOverageRepaymentInFinalPayment;

    private boolean created;

    /**
     * Creates and returns the JPanel representing the Contract Market configuration page.
     * <p>
     * This page includes settings for configuring various aspects of contract acquisition, such as methods, search
     * radius, payment options, and variable contract length.
     *
     * @param model the shared markets options model to populate the freshly built controls from
     *
     * @return A {@link JPanel} for the Contract Market configuration page.
     */
    @Nonnull
    JPanel createPanel(@Nullable MarketsOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_federated_suns.png";
        contractMarketHeader = new CampaignOptionsHeaderPanel("ContractMarketPage", imageAddress);
        // Contents
        pnlContractMarketGeneralOptions = createContractMarketGeneralOptionsPanel();
        pnlContractTermMultipliers = createContractTermMultipliersPanel();
        pnlContractPay = createContractPayPanel();

        final JPanel panel = CampaignOptionsPagePanel.builder("ContractMarketPage", "ContractMarketPage",
                    imageAddress)
                                   .header(contractMarketHeader)
                                   .quote("contractMarketPage")
                                   .section("lblContractMarketGeneralOptionsPanel.text",
                                         "lblContractMarketGeneralOptionsPanel.summary",
                                         pnlContractMarketGeneralOptions)
                                   .section("lblContractTermMultipliersPanel.text",
                                         "lblContractTermMultipliersPanel.summary",
                                         pnlContractTermMultipliers)
                                   .section("lblContractPayPanel.text",
                                         "lblContractPayPanel.summary",
                                         pnlContractPay)
                                   .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Builds the general settings panel for the Contract Market page, which includes options for the contract market
     * method, search radius, salvage percentages, and other general configurations.
     *
     * @return A {@link JPanel} representing general options within the Contract Market page.
     */
    private @Nonnull JPanel createContractMarketGeneralOptionsPanel() {
        // Contents
        lblContractMarketMethod = new CampaignOptionsLabel("ContractMarketMethod");
        lblContractMarketMethod
              .addMouseListener(createTipPanelUpdater("ContractMarketMethod"));
        comboContractMarketMethod = new MMComboBox<>("comboContractMarketMethod");
        DefaultComboBoxModel<ContractMarketMethod> model = new DefaultComboBoxModel<>(
              ContractMarketMethod.values());
        comboContractMarketMethod.setModel(model);
        comboContractMarketMethod
              .addMouseListener(createTipPanelUpdater("ContractMarketMethod"));

        lblContractSearchRadius = new CampaignOptionsLabel("ContractSearchRadius");
        lblContractSearchRadius
              .addMouseListener(createTipPanelUpdater("ContractSearchRadius"));
        spnContractSearchRadius = new CampaignOptionsSpinner("ContractSearchRadius", 300, 1, 2500, 100);
        spnContractSearchRadius
              .addMouseListener(createTipPanelUpdater("ContractSearchRadius"));

        chkVariableContractLength = new CampaignOptionsCheckBox("VariableContractLength");
        chkVariableContractLength.addMouseListener(createTipPanelUpdater("VariableContractLength"));

        chkUseTwoWayPay = new CampaignOptionsCheckBox("UseTwoWayPay",
              getMetadata(new Version(0, 51, 1)));
        chkUseTwoWayPay.addMouseListener(createTipPanelUpdater("UseTwoWayPay"));

        chkUseCamOpsSalvage = new CampaignOptionsCheckBox("UseCamOpsSalvage",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseCamOpsSalvage.addMouseListener(createTipPanelUpdater("UseCamOpsSalvage"));

        chkUseRiskySalvage = new CampaignOptionsCheckBox("UseRiskySalvage",
              getMetadata(MILESTONE_BEFORE_METADATA, CUSTOM_SYSTEM));
        chkUseRiskySalvage.addMouseListener(createTipPanelUpdater("UseRiskySalvage"));

        chkEnableSalvageFlagByDefault = new CampaignOptionsCheckBox("EnableSalvageFlagByDefault",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkEnableSalvageFlagByDefault.addMouseListener(createTipPanelUpdater("EnableSalvageFlagByDefault"));

        // General options that apply regardless of the contract-pay scheme (Chaos or legacy).
        chkBLCSaleValue = new CampaignOptionsCheckBox("BLCSaleValue");
        chkBLCSaleValue.addMouseListener(createTipPanelUpdater("BLCSaleValue"));

        chkOverageRepaymentInFinalPayment = new CampaignOptionsCheckBox("OverageRepaymentInFinalPayment");
        chkOverageRepaymentInFinalPayment.addMouseListener(createTipPanelUpdater("OverageRepaymentInFinalPayment"));

        chkUseDynamicDifficulty = new CampaignOptionsCheckBox("UseDynamicDifficulty");
        chkUseDynamicDifficulty
              .addMouseListener(createTipPanelUpdater("UseDynamicDifficulty"));

        chkUseBolsterContractSkill = new CampaignOptionsCheckBox("UseBolsterContractSkill",
              getMetadata(MILESTONE_BEFORE_METADATA, CUSTOM_SYSTEM));
        chkUseBolsterContractSkill.addMouseListener(createTipPanelUpdater("UseBolsterContractSkill"));

        chkUseChaosScaleSupportPointConversion = new CampaignOptionsCheckBox("UseChaosScaleSupportPointConversion",
              getMetadata(new Version(0, 51, 1)));
        chkUseChaosScaleSupportPointConversion.addMouseListener(
                createTipPanelUpdater("UseChaosScaleSupportPointConversion"));

        chkUseContractFactionModifiers = new CampaignOptionsCheckBox("UseContractFactionModifiers",
              getMetadata(new Version(0, 51, 1), CUSTOM_SYSTEM));
        chkUseContractFactionModifiers.addMouseListener(createTipPanelUpdater("UseContractFactionModifiers"));

        chkUseIntelObfuscation = new CampaignOptionsCheckBox("UseIntelObfuscation",
              getMetadata(new Version(0, 51, 1), CUSTOM_SYSTEM));
        chkUseIntelObfuscation.addMouseListener(createTipPanelUpdater("UseIntelObfuscation"));

        lblDropShipBonusPercentage = new CampaignOptionsLabel("DropShipBonusPercentage");
        lblDropShipBonusPercentage.addMouseListener(createTipPanelUpdater("DropShipBonusPercentage"));
        spnDropShipBonusPercentage = new CampaignOptionsSpinner("DropShipBonusPercentage", 0, 0, 20, 5);
        spnDropShipBonusPercentage.addMouseListener(createTipPanelUpdater("DropShipBonusPercentage"));

        lblPityContracts = new CampaignOptionsLabel("PityContracts", getMetadata(new Version(0, 51, 0)));
        lblPityContracts.addMouseListener(createTipPanelUpdater("PityContracts"));
        spnPityContracts = new CampaignOptionsSpinner("PityContracts", 4, 0, 20, 1);
        spnPityContracts.addMouseListener(createTipPanelUpdater("PityContracts"));

        // Layout the Panel
        //
        // A normal two-column form: one label/control pair per row, with the checkboxes laid out in the standard
        // two-column checkbox grid. addRow and addCheckBoxGrid both use the same two underlying grid columns, so they
        // line up cleanly within a single form panel.
        final SettingsFormPanel panel = new SettingsFormPanel("ContractMarketGeneralOptionsPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addRow(lblContractMarketMethod, comboContractMarketMethod);
        panel.addRow(lblContractSearchRadius, spnContractSearchRadius);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
              chkVariableContractLength,
              chkUseTwoWayPay,
              chkUseCamOpsSalvage,
              chkUseRiskySalvage,
              chkEnableSalvageFlagByDefault,
              chkUseDynamicDifficulty,
                chkUseBolsterContractSkill,
              chkUseChaosScaleSupportPointConversion,
              chkUseContractFactionModifiers,
              chkUseIntelObfuscation,
              chkBLCSaleValue,
              chkOverageRepaymentInFinalPayment);
        panel.addRow(lblDropShipBonusPercentage, spnDropShipBonusPercentage);
        panel.addRow(lblPityContracts, spnPityContracts);

        return panel;
    }

    /**
     * Builds the Contract Term Multipliers panel: per-term multipliers (base pay, straight support, battlefield loss,
     * transport, salvage) applied on top of a contract's negotiated step values. Each defaults to 1.0 (no change).
     *
     * @return a {@link JPanel} of the contract term multiplier spinners
     */
    private @Nonnull JPanel createContractTermMultipliersPanel() {
        lblContractBasePayMultiplier = new CampaignOptionsLabel("ContractBasePayMultiplier",
              getMetadata(new Version(0, 51, 1), CUSTOM_SYSTEM));
        lblContractBasePayMultiplier.addMouseListener(createTipPanelUpdater("ContractBasePayMultiplier"));
        spnContractBasePayMultiplier = new CampaignOptionsSpinner("ContractBasePayMultiplier", 1.0, 0.1, 2.0, 0.1);
        spnContractBasePayMultiplier.addMouseListener(createTipPanelUpdater("ContractBasePayMultiplier"));

        lblContractStraightSupportMultiplier = new CampaignOptionsLabel("ContractStraightSupportMultiplier",
              getMetadata(new Version(0, 51, 1), CUSTOM_SYSTEM));
        lblContractStraightSupportMultiplier.addMouseListener(
              createTipPanelUpdater("ContractStraightSupportMultiplier"));
        spnContractStraightSupportMultiplier = new CampaignOptionsSpinner("ContractStraightSupportMultiplier",
              1.0, 0.1, 2.0, 0.1);
        spnContractStraightSupportMultiplier.addMouseListener(
              createTipPanelUpdater("ContractStraightSupportMultiplier"));

        lblContractBattlefieldLossMultiplier = new CampaignOptionsLabel("ContractBattlefieldLossMultiplier",
              getMetadata(new Version(0, 51, 1), CUSTOM_SYSTEM));
        lblContractBattlefieldLossMultiplier.addMouseListener(
              createTipPanelUpdater("ContractBattlefieldLossMultiplier"));
        spnContractBattlefieldLossMultiplier = new CampaignOptionsSpinner("ContractBattlefieldLossMultiplier",
              1.0, 0.1, 2.0, 0.1);
        spnContractBattlefieldLossMultiplier.addMouseListener(
              createTipPanelUpdater("ContractBattlefieldLossMultiplier"));

        lblContractTransportMultiplier = new CampaignOptionsLabel("ContractTransportMultiplier",
              getMetadata(new Version(0, 51, 1), CUSTOM_SYSTEM));
        lblContractTransportMultiplier.addMouseListener(createTipPanelUpdater("ContractTransportMultiplier"));
        spnContractTransportMultiplier = new CampaignOptionsSpinner("ContractTransportMultiplier", 1.0, 0.1, 2.0, 0.1);
        spnContractTransportMultiplier.addMouseListener(createTipPanelUpdater("ContractTransportMultiplier"));

        lblContractSalvageMultiplier = new CampaignOptionsLabel("ContractSalvageMultiplier",
              getMetadata(new Version(0, 51, 1), CUSTOM_SYSTEM));
        lblContractSalvageMultiplier.addMouseListener(createTipPanelUpdater("ContractSalvageMultiplier"));
        spnContractSalvageMultiplier = new CampaignOptionsSpinner("ContractSalvageMultiplier", 1.0, 0.1, 2.0, 0.1);
        spnContractSalvageMultiplier.addMouseListener(createTipPanelUpdater("ContractSalvageMultiplier"));

        final SettingsFormPanel panel = new SettingsFormPanel("ContractTermMultipliersPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        panel.addRow(lblContractBasePayMultiplier, spnContractBasePayMultiplier);
        panel.addRow(lblContractStraightSupportMultiplier, spnContractStraightSupportMultiplier);
        panel.addRow(lblContractBattlefieldLossMultiplier, spnContractBattlefieldLossMultiplier);
        panel.addRow(lblContractTransportMultiplier, spnContractTransportMultiplier);
        panel.addRow(lblContractSalvageMultiplier, spnContractSalvageMultiplier);

        return panel;
    }

    /**
     * Creates the panel for configuring payment settings in the Contract Market page.
     * <p>
     * This panel contains options for configuring equipment-based payment percentages, override repayment rules, and
     * toggles for contract payment methods.
     *
     * @return A {@link JPanel} containing payment configuration settings for the Contract Market.
     */
    private @Nonnull JPanel createContractPayPanel() {
        // Contents

        // A general Chaos-pay option (applies to the default scheme, not the legacy bases): whether Chaos support-point
        // pay is converted to C-bills. Enabled by default.
        chkUseChaosSupportPointConversion = new CampaignOptionsCheckBox("UseChaosSupportPointConversion",
              getMetadata(new Version(0, 51, 1)));
        chkUseChaosSupportPointConversion.addMouseListener(createTipPanelUpdater("UseChaosSupportPointConversion"));

        // Top-level pay scheme: the default Chaos Campaign scheme, or the legacy force-value / payroll schemes whose
        // basis and options are configured in the nested card below. Leaving the box unticked keeps Chaos pay.
        chkUseLegacyOptions = new CampaignOptionsCheckBox("UseLegacyOptions", getMetadata(new Version(0, 51, 1)));
        chkUseLegacyOptions.addMouseListener(createTipPanelUpdater("UseLegacyOptions"));

        btnContractEquipment = new JRadioButton(getTextAt(getCampaignOptionsResourceBundle(),
              "lblContractEquipment.text"));
        btnContractEquipment.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(),
              "lblContractEquipment.tooltip"));
        btnContractEquipment.addMouseListener(createTipPanelUpdater("ContractEquipment"));

        btnContractPersonnel = new JRadioButton(getTextAt(getCampaignOptionsResourceBundle(),
              "lblContractPersonnel.text"));
        btnContractPersonnel.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(),
              "lblContractPersonnel.tooltip"));
        btnContractPersonnel.addMouseListener(createTipPanelUpdater("ContractPersonnel"));

        ButtonGroup contractGroup = new ButtonGroup();
        contractGroup.add(btnContractEquipment);
        contractGroup.add(btnContractPersonnel);

        chkUseAlternatePaymentMode = new CampaignOptionsCheckBox("UseAlternatePaymentMode",
              getMetadata(MILESTONE_BEFORE_METADATA, CUSTOM_SYSTEM));
        chkUseAlternatePaymentMode.addMouseListener(createTipPanelUpdater("UseAlternatePaymentMode"));

        chkUseDiminishingContractPay = new CampaignOptionsCheckBox("UseDiminishingContractPay",
              getMetadata(MILESTONE_BEFORE_METADATA, CUSTOM_SYSTEM));
        chkUseDiminishingContractPay.addMouseListener(createTipPanelUpdater("UseDiminishingContractPay"));

        chkEquipContractSaleValue = new CampaignOptionsCheckBox("EquipContractSaleValue");
        chkEquipContractSaleValue.addMouseListener(createTipPanelUpdater("EquipContractSaleValue"));

        lblEquipPercent = new CampaignOptionsLabel("EquipPercent");
        lblEquipPercent.addMouseListener(createTipPanelUpdater("EquipPercent"));
        spnEquipPercent = new CampaignOptionsSpinner("EquipPercent",
              0.1,
              0,
              CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT,
              0.1);
        spnEquipPercent.addMouseListener(createTipPanelUpdater("EquipPercent"));

        lblDropShipPercent = new CampaignOptionsLabel("DropShipPercent");
        lblDropShipPercent.addMouseListener(createTipPanelUpdater("DropShipPercent"));
        spnDropShipPercent = new CampaignOptionsSpinner("DropShipPercent",
              0.1,
              0,
              CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT,
              0.1);
        spnDropShipPercent.addMouseListener(createTipPanelUpdater("DropShipPercent"));

        lblJumpShipPercent = new CampaignOptionsLabel("JumpShipPercent");
        lblJumpShipPercent.addMouseListener(createTipPanelUpdater("JumpShipPercent"));
        spnJumpShipPercent = new CampaignOptionsSpinner("JumpShipPercent",
              0.1,
              0,
              CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT,
              0.1);
        spnJumpShipPercent.addMouseListener(createTipPanelUpdater("JumpShipPercent"));

        lblWarShipPercent = new CampaignOptionsLabel("WarShipPercent");
        lblWarShipPercent.addMouseListener(createTipPanelUpdater("WarShipPercent"));
        spnWarShipPercent = new CampaignOptionsSpinner("WarShipPercent",
              0.1,
              0,
              CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT,
              0.1);
        spnWarShipPercent.addMouseListener(createTipPanelUpdater("WarShipPercent"));

        chkUseInfantryDoesNotCount = new CampaignOptionsCheckBox("UseInfantryDoesNotCount");
        chkUseInfantryDoesNotCount.addMouseListener(createTipPanelUpdater("UseInfantryDoesNotCount"));

        // Layout the Panel
        final SettingsFormPanel equipmentValuePanel = new SettingsFormPanel(
              "ContractPayPanelValuePercent",
              CONTRACT_PAY_LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        equipmentValuePanel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
              chkEquipContractSaleValue,
              chkUseAlternatePaymentMode,
              chkUseDiminishingContractPay);
        equipmentValuePanel.addRow(lblEquipPercent, spnEquipPercent);
        equipmentValuePanel.addRow(lblDropShipPercent, spnDropShipPercent);
        equipmentValuePanel.addRow(lblJumpShipPercent, spnJumpShipPercent);
        equipmentValuePanel.addRow(lblWarShipPercent, spnWarShipPercent);
        pnlContractPayEquipmentOptions = equipmentValuePanel;

        // Applies to both legacy bases (TO&E value and payroll), so it sits outside the basis radios rather than under
        // either one. (Battle-loss-compensation and salvage-overage repayment are general options - they also apply
        // under Chaos pay - and live in the Market Rules section instead.)
        final SettingsFormPanel sharedOptionsPanel = new SettingsFormPanel(
                "ContractPaySharedPanel",
              CONTRACT_PAY_LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        sharedOptionsPanel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
              chkUseInfantryDoesNotCount);

        btnContractEquipment.addActionListener(event -> updateContractPayEnabledState());
        btnContractPersonnel.addActionListener(event -> updateContractPayEnabledState());
        chkUseLegacyOptions.addActionListener(event -> updateContractPayModelEnabledState());

        // The legacy container holds the TO&E-value / payroll basis radios and the shared options; it is only
        // interactive when "Use Legacy Options" is ticked above, otherwise Chaos pay is used.
        final JPanel legacyOptions = new CampaignOptionsStandardPanel("ContractPayLegacyOptions");
        legacyOptions.setLayout(new GridBagLayout());
        final GridBagConstraints legacyLayout = defaultGridBagConstraints();
        legacyLayout.weightx = 1.0;
        legacyLayout.fill = GridBagConstraints.HORIZONTAL;

        legacyLayout.gridx = 0;
        legacyLayout.gridy = 0;
        legacyOptions.add(createContractPaySubsection("ContractPayEquipmentSubsection",
                btnContractEquipment,
                equipmentValuePanel), legacyLayout);

        legacyLayout.gridy++;
        legacyOptions.add(btnContractPersonnel, legacyLayout);

        legacyLayout.gridy++;
        legacyOptions.add(sharedOptionsPanel, legacyLayout);
        pnlLegacyContractPayOptions = legacyOptions;

        final JPanel panel = new CampaignOptionsStandardPanel("ContractPayPanel");
        panel.setLayout(new GridBagLayout());
        final GridBagConstraints layout = defaultGridBagConstraints();
        layout.weightx = 1.0;
        layout.fill = GridBagConstraints.HORIZONTAL;

        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(chkUseChaosSupportPointConversion, layout);

        layout.gridy++;
        panel.add(chkUseLegacyOptions, layout);

        layout.gridy++;
        legacyOptions.setBorder(BorderFactory.createEmptyBorder(0, CONTRACT_PAY_OPTION_INDENT, 0, 0));
        panel.add(legacyOptions, layout);

        return panel;
    }

    /**
     * Wraps a contract-pay radio button together with the options it controls inside a single bordered card. The radio
     * button acts as the card's header and the supplied options panel is indented beneath it so it is visually clear
     * which settings belong to which payment basis.
     *
     * @param name        the internal panel name
     * @param radioButton the radio button that selects this payment basis
     * @param options     the options that apply when this payment basis is selected
     *
     * @return the assembled subsection card
     */
    private @Nonnull JPanel createContractPaySubsection(String name, JRadioButton radioButton, JPanel options) {
        final JPanel card = new CampaignOptionsStandardPanel(name, true);
        card.setLayout(new GridBagLayout());
        final GridBagConstraints layout = defaultGridBagConstraints();
        layout.weightx = 1.0;
        layout.anchor = GridBagConstraints.NORTHWEST;
        layout.fill = GridBagConstraints.HORIZONTAL;

        layout.gridx = 0;
        layout.gridy = 0;
        card.add(radioButton, layout);

        options.setBorder(BorderFactory.createEmptyBorder(0, CONTRACT_PAY_OPTION_INDENT, 0, 0));
        layout.gridy++;
        card.add(options, layout);

        return card;
    }

    private static GridBagConstraints defaultGridBagConstraints() {
        GridBagConstraints layout = new GridBagConstraints();
        layout.anchor = GridBagConstraints.NORTHWEST;
        layout.fill = GridBagConstraints.HORIZONTAL;
        layout.insets = new Insets(5, 5, 5, 5);
        return layout;
    }

    /**
     * Enables the contract-pay options belonging to the currently selected payment basis and disables the options of
     * the other basis, so only the relevant settings are interactive.
     */
    private void updateContractPayEnabledState() {
        if (pnlContractPayEquipmentOptions == null) {
            return;
        }

        // The TO&E-value percentages only apply to the equipment basis; the payroll basis has no dedicated options
        // (the shared options below apply to both bases and stay enabled).
        setContainerEnabled(pnlContractPayEquipmentOptions, btnContractEquipment.isSelected());
    }

    /**
     * Enables the whole legacy contract-pay card only when "Use Legacy Options" is ticked; otherwise Chaos Campaign pay
     * is used and the card (basis radios and shared options alike) is greyed out. When legacy pay is active, the
     * equipment-basis gating is applied within it.
     */
    private void updateContractPayModelEnabledState() {
        if (pnlLegacyContractPayOptions == null) {
            return;
        }

        boolean legacySelected = chkUseLegacyOptions.isSelected();
        setContainerEnabled(pnlLegacyContractPayOptions, legacySelected);
        if (legacySelected) {
            updateContractPayEnabledState();
        }
    }

    private void setContainerEnabled(Container container, boolean enabled) {
        for (Component child : container.getComponents()) {
            child.setEnabled(enabled);
            if (child instanceof Container nested) {
                setContainerEnabled(nested, enabled);
            }
        }
    }

    /**
     * Copies contract market values from the shared model into this page's controls. This is a no-op until the page has
     * been built.
     *
     * @param model the shared markets options model to read values from
     */
    void readFromModel(@Nullable MarketsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        comboContractMarketMethod.setSelectedItem(model.contractMarketMethod);
        spnContractSearchRadius.setValue(model.contractSearchRadius);
        chkVariableContractLength.setSelected(model.variableContractLength);
        chkUseTwoWayPay.setSelected(model.useTwoWayPay);
        chkUseCamOpsSalvage.setSelected(model.useCamOpsSalvage);
        chkUseRiskySalvage.setSelected(model.useRiskySalvage);
        chkEnableSalvageFlagByDefault.setSelected(model.enableSalvageFlagByDefault);
        chkUseDynamicDifficulty.setSelected(model.useDynamicDifficulty);
        chkUseBolsterContractSkill.setSelected(model.useBolsterContractSkill);
        chkUseChaosScaleSupportPointConversion.setSelected(model.useChaosScaleSupportPointConversion);
        chkUseContractFactionModifiers.setSelected(model.useContractFactionModifiers);
        chkUseIntelObfuscation.setSelected(model.useIntelObfuscation);
        spnDropShipBonusPercentage.setValue(model.dropShipBonusPercentage);
        spnPityContracts.setValue(model.pityContracts);
        spnContractBasePayMultiplier.setValue(model.contractBasePayMultiplier);
        spnContractStraightSupportMultiplier.setValue(model.contractStraightSupportMultiplier);
        spnContractBattlefieldLossMultiplier.setValue(model.contractBattlefieldLossMultiplier);
        spnContractTransportMultiplier.setValue(model.contractTransportMultiplier);
        spnContractSalvageMultiplier.setValue(model.contractSalvageMultiplier);
        chkUseChaosSupportPointConversion.setSelected(model.useChaosSupportPointConversion);
        chkUseLegacyOptions.setSelected(model.useLegacyContractPay);
        if (model.equipmentContractBase) {
            btnContractEquipment.setSelected(true);
        } else {
            btnContractPersonnel.setSelected(true);
        }
        spnEquipPercent.setValue(model.equipmentContractPercent);
        chkUseAlternatePaymentMode.setSelected(model.useAlternatePaymentMode);
        chkUseDiminishingContractPay.setSelected(model.useDiminishingContractPay);
        chkEquipContractSaleValue.setSelected(model.equipmentContractSaleValue);
        spnDropShipPercent.setValue(model.dropShipContractPercent);
        spnJumpShipPercent.setValue(model.jumpShipContractPercent);
        spnWarShipPercent.setValue(model.warShipContractPercent);
        chkUseInfantryDoesNotCount.setSelected(model.infantryDontCount);
        chkBLCSaleValue.setSelected(model.blcSaleValue);
        chkOverageRepaymentInFinalPayment.setSelected(model.overageRepaymentInFinalPayment);
        updateContractPayModelEnabledState();
    }

    /**
     * Copies contract market values from this page's controls into the shared model. This is a no-op until the page has
     * been built.
     *
     * @param model the shared markets options model to write values into
     */
    void writeToModel(@Nullable MarketsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.contractMarketMethod = comboContractMarketMethod.getSelectedItem();
        model.contractSearchRadius = (int) spnContractSearchRadius.getValue();
        model.variableContractLength = chkVariableContractLength.isSelected();
        model.useTwoWayPay = chkUseTwoWayPay.isSelected();
        model.useCamOpsSalvage = chkUseCamOpsSalvage.isSelected();
        model.useRiskySalvage = chkUseRiskySalvage.isSelected();
        model.enableSalvageFlagByDefault = chkEnableSalvageFlagByDefault.isSelected();
        model.useDynamicDifficulty = chkUseDynamicDifficulty.isSelected();
        model.useBolsterContractSkill = chkUseBolsterContractSkill.isSelected();
        model.useChaosScaleSupportPointConversion = chkUseChaosScaleSupportPointConversion.isSelected();
        model.useContractFactionModifiers = chkUseContractFactionModifiers.isSelected();
        model.useIntelObfuscation = chkUseIntelObfuscation.isSelected();
        model.dropShipBonusPercentage = (int) spnDropShipBonusPercentage.getValue();
        model.pityContracts = (int) spnPityContracts.getValue();
        model.contractBasePayMultiplier = (double) spnContractBasePayMultiplier.getValue();
        model.contractStraightSupportMultiplier = (double) spnContractStraightSupportMultiplier.getValue();
        model.contractBattlefieldLossMultiplier = (double) spnContractBattlefieldLossMultiplier.getValue();
        model.contractTransportMultiplier = (double) spnContractTransportMultiplier.getValue();
        model.contractSalvageMultiplier = (double) spnContractSalvageMultiplier.getValue();
        model.useChaosSupportPointConversion = chkUseChaosSupportPointConversion.isSelected();
        model.useLegacyContractPay = chkUseLegacyOptions.isSelected();
        model.equipmentContractBase = btnContractEquipment.isSelected();
        model.equipmentContractPercent = (double) spnEquipPercent.getValue();
        model.dropShipContractPercent = (double) spnDropShipPercent.getValue();
        model.jumpShipContractPercent = (double) spnJumpShipPercent.getValue();
        model.warShipContractPercent = (double) spnWarShipPercent.getValue();
        model.useAlternatePaymentMode = chkUseAlternatePaymentMode.isSelected();
        model.useDiminishingContractPay = chkUseDiminishingContractPay.isSelected();
        model.equipmentContractSaleValue = chkEquipContractSaleValue.isSelected();
        model.blcSaleValue = chkBLCSaleValue.isSelected();
        model.infantryDontCount = chkUseInfantryDoesNotCount.isSelected();
        model.overageRepaymentInFinalPayment = chkOverageRepaymentInFinalPayment.isSelected();
    }
}
