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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
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
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
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
 * <p>This view is a sub-component of {@link MarketsPages}: the model snapshot and the overall load/apply lifecycle still
 * live on {@code MarketsPages}, while this class is responsible only for constructing the Contract Market panel and
 * copying contract market values to and from the model. The page is built lazily; until
 * {@link #createPanel(MarketsOptionsModel)} is called, {@link #readFromModel(MarketsOptionsModel)} and
 * {@link #writeToModel(MarketsOptionsModel)} are no-ops.</p>
 */
class ContractMarketPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
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
    private JCheckBox chkContractMarketReportRefresh;
    private JLabel lblContractMaxSalvagePercentage;
    private JSpinner spnContractMaxSalvagePercentage;
    private JLabel lblDropShipBonusPercentage;
    private JSpinner spnDropShipBonusPercentage;
    private JLabel lblPityContracts;
    private JSpinner spnPityContracts;

    private JPanel pnlContractPay;
    private JRadioButton btnContractEquipment;
    private JPanel pnlContractPayEquipmentOptions;
    private JPanel pnlContractPayPersonnelOptions;
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
    @Nonnull JPanel createPanel(@Nullable MarketsOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_federated_suns.png";
        contractMarketHeader = new CampaignOptionsHeaderPanel("ContractMarketPage", imageAddress);
        // Contents
        pnlContractMarketGeneralOptions = createContractMarketGeneralOptionsPanel();
        pnlContractPay = createContractPayPanel();

        final JPanel panel = CampaignOptionsPagePanel.builder("ContractMarketPage", "ContractMarketPage",
                imageAddress)
                .header(contractMarketHeader)
                .quote("contractMarketPage")
                .section("lblContractMarketGeneralOptionsPanel.text",
                        "lblContractMarketGeneralOptionsPanel.summary",
                        pnlContractMarketGeneralOptions)
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
        model.removeElement(ContractMarketMethod.CAM_OPS);
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
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseTwoWayPay.addMouseListener(createTipPanelUpdater("UseTwoWayPay"));

        chkUseCamOpsSalvage = new CampaignOptionsCheckBox("UseCamOpsSalvage",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseCamOpsSalvage.addMouseListener(createTipPanelUpdater("UseCamOpsSalvage"));

        chkUseRiskySalvage = new CampaignOptionsCheckBox("UseRiskySalvage",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseRiskySalvage.addMouseListener(createTipPanelUpdater("UseRiskySalvage"));

        chkEnableSalvageFlagByDefault = new CampaignOptionsCheckBox("EnableSalvageFlagByDefault",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkEnableSalvageFlagByDefault.addMouseListener(createTipPanelUpdater("EnableSalvageFlagByDefault"));

        chkUseDynamicDifficulty = new CampaignOptionsCheckBox("UseDynamicDifficulty");
        chkUseDynamicDifficulty
                .addMouseListener(createTipPanelUpdater("UseDynamicDifficulty"));

        chkUseBolsterContractSkill = new CampaignOptionsCheckBox("UseBolsterContractSkill",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseBolsterContractSkill.addMouseListener(createTipPanelUpdater("UseBolsterContractSkill"));

        chkContractMarketReportRefresh = new CampaignOptionsCheckBox("ContractMarketReportRefresh");
        chkContractMarketReportRefresh.addMouseListener(createTipPanelUpdater("ContractMarketReportRefresh"));

        lblContractMaxSalvagePercentage = new CampaignOptionsLabel("ContractMaxSalvagePercentage");
        lblContractMaxSalvagePercentage.addMouseListener(createTipPanelUpdater("ContractMaxSalvagePercentage"));
        spnContractMaxSalvagePercentage = new CampaignOptionsSpinner("ContractMaxSalvagePercentage", 100, 0, 100,
                10);
        spnContractMaxSalvagePercentage.addMouseListener(createTipPanelUpdater("ContractMaxSalvagePercentage"));

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
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ContractMarketGeneralOptionsPanel",
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
                chkContractMarketReportRefresh);
        panel.addRow(lblContractMaxSalvagePercentage, spnContractMaxSalvagePercentage);
        panel.addRow(lblDropShipBonusPercentage, spnDropShipBonusPercentage);
        panel.addRow(lblPityContracts, spnPityContracts);

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
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseAlternatePaymentMode.addMouseListener(createTipPanelUpdater("UseAlternatePaymentMode"));

        chkUseDiminishingContractPay = new CampaignOptionsCheckBox("UseDiminishingContractPay",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
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

        chkBLCSaleValue = new CampaignOptionsCheckBox("BLCSaleValue");
        chkBLCSaleValue.addMouseListener(createTipPanelUpdater("BLCSaleValue"));

        chkUseInfantryDoesNotCount = new CampaignOptionsCheckBox("UseInfantryDoesNotCount");
        chkUseInfantryDoesNotCount.addMouseListener(createTipPanelUpdater("UseInfantryDoesNotCount"));

        chkOverageRepaymentInFinalPayment = new CampaignOptionsCheckBox("OverageRepaymentInFinalPayment");
        chkOverageRepaymentInFinalPayment.addMouseListener(createTipPanelUpdater("OverageRepaymentInFinalPayment"));

        // Layout the Panel
        final CampaignOptionsFormPanel equipmentValuePanel = new CampaignOptionsFormPanel(
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

        final CampaignOptionsFormPanel personnelPayPanel = new CampaignOptionsFormPanel(
                "ContractPayPersonnelPanel",
                CONTRACT_PAY_LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        personnelPayPanel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkBLCSaleValue,
                chkUseInfantryDoesNotCount,
                chkOverageRepaymentInFinalPayment);
        pnlContractPayPersonnelOptions = personnelPayPanel;

        btnContractEquipment.addActionListener(event -> updateContractPayEnabledState());
        btnContractPersonnel.addActionListener(event -> updateContractPayEnabledState());

        final JPanel panel = new CampaignOptionsStandardPanel("ContractPayPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.weightx = 1.0;
        layout.fill = GridBagConstraints.HORIZONTAL;

        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(createContractPaySubsection("ContractPayEquipmentSubsection",
                btnContractEquipment,
                equipmentValuePanel), layout);

        layout.gridy++;
        panel.add(createContractPaySubsection("ContractPayPersonnelSubsection",
                btnContractPersonnel,
                personnelPayPanel), layout);

        return panel;
    }

    /**
     * Wraps a contract-pay radio button together with the options it controls inside a single bordered card. The
     * radio button acts as the card's header and the supplied options panel is indented beneath it so it is visually
     * clear which settings belong to which payment basis.
     *
     * @param name         the internal panel name
     * @param radioButton  the radio button that selects this payment basis
     * @param options      the options that apply when this payment basis is selected
     *
     * @return the assembled subsection card
     */
    private @Nonnull JPanel createContractPaySubsection(String name, JRadioButton radioButton, JPanel options) {
        final JPanel card = new CampaignOptionsStandardPanel(name, true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(card);
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

    /**
     * Enables the contract-pay options belonging to the currently selected payment basis and disables the options of
     * the other basis, so only the relevant settings are interactive.
     */
    private void updateContractPayEnabledState() {
        if (pnlContractPayEquipmentOptions == null || pnlContractPayPersonnelOptions == null) {
            return;
        }

        boolean equipmentSelected = btnContractEquipment.isSelected();
        setContainerEnabled(pnlContractPayEquipmentOptions, equipmentSelected);
        setContainerEnabled(pnlContractPayPersonnelOptions, !equipmentSelected);
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
        chkContractMarketReportRefresh.setSelected(model.contractMarketReportRefresh);
        spnContractMaxSalvagePercentage.setValue(model.contractMaxSalvagePercentage);
        spnDropShipBonusPercentage.setValue(model.dropShipBonusPercentage);
        spnPityContracts.setValue(model.pityContracts);
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
        updateContractPayEnabledState();
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
        model.contractMarketReportRefresh = chkContractMarketReportRefresh.isSelected();
        model.contractMaxSalvagePercentage = (int) spnContractMaxSalvagePercentage.getValue();
        model.dropShipBonusPercentage = (int) spnDropShipBonusPercentage.getValue();
        model.pityContracts = (int) spnPityContracts.getValue();
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
