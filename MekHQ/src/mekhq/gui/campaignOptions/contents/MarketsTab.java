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
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.personnel.Skills;
import mekhq.gui.campaignOptions.components.*;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;

import javax.swing.*;
import javax.swing.JSpinner.DefaultEditor;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

/**
 * The {@code MarketsTab} class represents the campaign options tab related to market settings.
 * This tab provides configurations for three key market areas:
 * <p>
 *     <li><b>Personnel Market</b>: Settings for managing personnel hiring, removal targets, and market types.</li>
 *     <li><b>Unit Market</b>: Configurations for purchasing units, special unit chances, rarity modifiers, etc.</li>
 *     <li><b>Contract Market</b>: Options for contract acquisition, such as market methods, search radius,
 *         and payment settings.</li>
 * </p>
 * <p>
 * The class initializes the UI components for these three market types and provides methods to
 * load data into the UI or apply changes from the UI to the campaign settings.
 * </p>
 * <p>
 * This class interacts with {@link CampaignOptions} to retrieve or update the persistent campaign settings.
 * It also utilizes Swing components for building the UI.
 * </p>
 */
public class MarketsTab {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    private final Campaign campaign;
    private final CampaignOptions campaignOptions;

    //start Personnel Market
    private JPanel pnlPersonnelMarketGeneralOptions;
    private JLabel lblPersonnelMarketType;
    private MMComboBox<String> comboPersonnelMarketType;
    private JCheckBox chkPersonnelMarketReportRefresh;
    private JCheckBox chkUsePersonnelHireHiringHallOnly;

    private JPanel pnlRemovalTargets;
    private JLabel lblPersonnelMarketDylansWeight;
    private JSpinner spnPersonnelMarketDylansWeight;
    private Map<SkillLevel, JLabel> lblPersonnelMarketRandomRemovalTargets;
    private Map<SkillLevel, JSpinner> spnPersonnelMarketRandomRemovalTargets;
    //end Personnel Market

    //start Unit Market
    private JLabel lblUnitMarketMethod;
    private MMComboBox<UnitMarketMethod> comboUnitMarketMethod;
    private JCheckBox chkUnitMarketRegionalMekVariations;
    private JLabel lblUnitMarketSpecialUnitChance;
    private JSpinner spnUnitMarketSpecialUnitChance;
    private JLabel lblUnitMarketRarityModifier;
    private JSpinner spnUnitMarketRarityModifier;
    private JCheckBox chkInstantUnitMarketDelivery;
    private JCheckBox chkUnitMarketReportRefresh;
    //end Unit Market

    //start Contract Market
    private JPanel pnlContractMarketGeneralOptions;
    private JLabel lblContractMarketMethod;
    private MMComboBox<ContractMarketMethod> comboContractMarketMethod;
    private JLabel lblContractSearchRadius;
    private JSpinner spnContractSearchRadius;
    private JCheckBox chkVariableContractLength;
    private JCheckBox chkContractMarketReportRefresh;
    private JLabel lblCoontractMaxSalvagePercentage;
    private JSpinner spnContractMaxSalvagePercentage;
    private JLabel lblDropShipBonusPercentage;
    private JSpinner spnDropShipBonusPercentage;

    private JPanel pnlContractPay;
    private JRadioButton btnContractEquipment;
    private JLabel lblEquipPercent;
    private JSpinner spnEquipPercent;
    private JCheckBox chkEquipContractSaleValue;
    private JLabel lblDropShipPercent;
    private JSpinner spnDropShipPercent;
    private JLabel lblJumpShipPercent;
    private JSpinner spnJumpShipPercent;
    private JLabel lblWarShipPercent;
    private JSpinner spnWarShipPercent;
    private JRadioButton btnContractPersonnel;
    private JCheckBox useInfantryDoseNotCountBox;
    private JCheckBox chkMercSizeLimited;
    private JCheckBox chkBLCSaleValue;
    private JCheckBox chkOverageRepaymentInFinalPayment;
    //end Contract Market

    /**
     * Constructs a {@code MarketsTab} with the provided campaign. Initializes the market
     * configuration options based on the settings of the given {@link Campaign}.
     *
     * @param campaign The {@link Campaign} associated with this market tab. This campaign
     *                 is used to retrieve and modify {@link CampaignOptions}.
     */
    public MarketsTab(Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();

        initialize();
    }

    /**
     * Initializes the market-related options tabs by setting up configurations
     * for the Personnel Market, Unit Market, and Contract Market.
     * <p>
     * This method is invoked internally within the constructor to prepare the
     * various market configurations for use in the UI.
     */
    private void initialize() {
        initializePersonnelMarket();
        initializeUnitMarket();
        initializeContractMarket();
    }

    /**
     * Initializes the settings and UI components related to the Personnel Market.
     * <p>
     * This includes setting up labels, combo boxes for selecting the personnel market type,
     * checkboxes for additional options, and spinners for configuring removal targets.
     */
    private void initializePersonnelMarket() {
        pnlPersonnelMarketGeneralOptions = new JPanel();
        lblPersonnelMarketType = new JLabel();
        comboPersonnelMarketType = new MMComboBox<>("comboPersonnelMarketType",
            getPersonnelMarketTypeOptions());
        chkPersonnelMarketReportRefresh = new JCheckBox();
        chkUsePersonnelHireHiringHallOnly = new JCheckBox();

        pnlRemovalTargets = new JPanel();
        lblPersonnelMarketDylansWeight = new JLabel();
        spnPersonnelMarketDylansWeight = new JSpinner();
        lblPersonnelMarketRandomRemovalTargets = new HashMap<>();
        spnPersonnelMarketRandomRemovalTargets = new HashMap<>();
    }

    /**
     * Retrieves the available personnel market type options for display in a combo box.
     * <p>
     * These types are fetched from the {@link PersonnelMarketServiceManager} and represent
     * the available personnel market methods configured for the campaign.
     *
     * @return A {@link DefaultComboBoxModel} containing the personnel market type options.
     */
    private static DefaultComboBoxModel<String> getPersonnelMarketTypeOptions() {
        final DefaultComboBoxModel<String> personnelMarketTypeModel = new DefaultComboBoxModel<>();
        for (final PersonnelMarketMethod method : PersonnelMarketServiceManager.getInstance()
            .getAllServices(true)) {
            personnelMarketTypeModel.addElement(method.getModuleName());
        }
        return personnelMarketTypeModel;
    }

    /**
     * Creates and returns the JPanel representing the Personnel Market configuration tab.
     * <p>
     * This tab includes general personnel market settings, as well as removal target
     * configuration options for various skill levels.
     *
     * @return A {@link JPanel} for the Personnel Market configuration tab.
     */
    public JPanel createPersonnelMarketTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("PersonnelMarketTab",
            getImageDirectory() + "logo_st_ives_compact.png");

        // Contents
        pnlPersonnelMarketGeneralOptions = createPersonnelMarketGeneralOptionsPanel();
        pnlRemovalTargets = createPersonnelMarketRemovalOptionsPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelMarketTab", true,
            "");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(pnlPersonnelMarketGeneralOptions, layout);
        layout.gridx++;
        panel.add(pnlRemovalTargets, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "PersonnelMarketTab");

    }

    /**
     * Builds the general options panel for the Personnel Market tab, which includes settings
     * such as the personnel market type, Dylan's weight, and options like report refresh toggles.
     * <p>
     * These components are laid out into a panel and returned for use in the UI.
     *
     * @return A {@link JPanel} representing the general options within the Personnel Market tab.
     */
    private JPanel createPersonnelMarketGeneralOptionsPanel() {
        // Contents
        lblPersonnelMarketType = new CampaignOptionsLabel("PersonnelMarketType");
        comboPersonnelMarketType = new MMComboBox<>("comboPersonnelMarketType",
            getPersonnelMarketTypeOptions());

        lblPersonnelMarketDylansWeight = new CampaignOptionsLabel("PersonnelMarketDylansWeight");
        spnPersonnelMarketDylansWeight = new CampaignOptionsSpinner("PersonnelMarketDylansWeight",
            0.3, 0, 1, 0.1);

        chkPersonnelMarketReportRefresh = new CampaignOptionsCheckBox("PersonnelMarketReportRefresh");

        chkUsePersonnelHireHiringHallOnly = new CampaignOptionsCheckBox("UsePersonnelHireHiringHallOnly");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelMarketGeneralOptionsPanel", false,
            "");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(lblPersonnelMarketType, layout);
        layout.gridx++;
        panel.add(comboPersonnelMarketType, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblPersonnelMarketDylansWeight, layout);
        layout.gridx++;
        panel.add(spnPersonnelMarketDylansWeight, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkPersonnelMarketReportRefresh, layout);

        layout.gridy++;
        panel.add(chkUsePersonnelHireHiringHallOnly, layout);

        return panel;
    }

    /**
     * Creates and configures the removal options panel for the Personnel Market tab.
     * <p>
     * This panel includes settings for removal targets, which are based on various
     * {@link SkillLevel} entries. Each skill level configuration includes both a label
     * and an associated spinner for setting values.
     *
     * @return A {@link JPanel} containing removal options for the Personnel Market.
     */
    private JPanel createPersonnelMarketRemovalOptionsPanel() {
        // Contents
        for (final SkillLevel skillLevel : Skills.SKILL_LEVELS) {
            final JLabel jLabel = new JLabel(skillLevel.toString());
            lblPersonnelMarketRandomRemovalTargets.put(skillLevel, jLabel);

            final JSpinner jSpinner = new JSpinner(
                new SpinnerNumberModel(0, 0, 12, 1));

            DefaultEditor editor = (DefaultEditor) jSpinner.getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);

            spnPersonnelMarketRandomRemovalTargets.put(skillLevel, jSpinner);
        }

        // Layout the Panels
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelMarketRemovalOptionsPanel",
            true, "PersonnelMarketRemovalOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.NONE), layout);
        layout.gridx++;
        panel.add(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.NONE), layout);
        layout.gridx++;
        panel.add(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.VETERAN), layout);
        layout.gridx++;
        panel.add(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.VETERAN), layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.ULTRA_GREEN), layout);
        layout.gridx++;
        panel.add(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.ULTRA_GREEN), layout);
        layout.gridx++;
        panel.add(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.ELITE), layout);
        layout.gridx++;
        panel.add(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.ELITE), layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.GREEN), layout);
        layout.gridx++;
        panel.add(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.GREEN), layout);
        layout.gridx++;
        panel.add(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.HEROIC), layout);
        layout.gridx++;
        panel.add(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.HEROIC), layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.REGULAR), layout);
        layout.gridx++;
        panel.add(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.REGULAR), layout);
        layout.gridx++;
        panel.add(lblPersonnelMarketRandomRemovalTargets.get(SkillLevel.LEGENDARY), layout);
        layout.gridx++;
        panel.add(spnPersonnelMarketRandomRemovalTargets.get(SkillLevel.LEGENDARY), layout);

        return panel;
    }

    /**
     * Initializes the settings and UI components related to the Unit Market tab.
     * <p>
     * This includes various elements such as labels, combo boxes, checkboxes, and
     * spinners for settings like unit market methods, rarity modifiers, and delivery options.
     */
    private void initializeUnitMarket() {
        lblUnitMarketMethod = new JLabel();
        comboUnitMarketMethod = new MMComboBox<>("comboUnitMarketMethod", UnitMarketMethod.values());
        chkUnitMarketRegionalMekVariations = new JCheckBox();
        lblUnitMarketSpecialUnitChance = new JLabel();
        spnUnitMarketSpecialUnitChance = new JSpinner();
        lblUnitMarketRarityModifier = new JLabel();
        spnUnitMarketRarityModifier = new JSpinner();
        chkInstantUnitMarketDelivery = new JCheckBox();
        chkUnitMarketReportRefresh = new JCheckBox();
    }

    /**
     * Creates and returns the JPanel representing the Unit Market configuration tab.
     * <p>
     * This tab includes options such as unit market methods, rarity modifiers,
     * special unit chance settings, and more.
     *
     * @return A {@link JPanel} for the Unit Market configuration tab.
     */
    public JPanel createUnitMarketTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("UnitMarketTab",
            getImageDirectory() + "logo_clan_ice_hellion.png");

        // Contents
        lblUnitMarketMethod = new CampaignOptionsLabel("UnitMarketMethod");
        comboUnitMarketMethod = new MMComboBox<>("comboUnitMarketMethod", UnitMarketMethod.values());

        chkUnitMarketRegionalMekVariations = new CampaignOptionsCheckBox("UnitMarketRegionalMekVariations");

        lblUnitMarketSpecialUnitChance = new CampaignOptionsLabel("UnitMarketSpecialUnitChance");
        spnUnitMarketSpecialUnitChance = new CampaignOptionsSpinner("UnitMarketSpecialUnitChance",
            30, 0, 100, 1);

        lblUnitMarketRarityModifier = new CampaignOptionsLabel("UnitMarketRarityModifier");
        spnUnitMarketRarityModifier = new CampaignOptionsSpinner("UnitMarketRarityModifier",
            0, -10, 10, 1);

        chkInstantUnitMarketDelivery = new CampaignOptionsCheckBox("InstantUnitMarketDelivery");

        chkUnitMarketReportRefresh = new CampaignOptionsCheckBox("UnitMarketReportRefresh");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("UnitMarketTab", true,
            "");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblUnitMarketMethod, layout);
        layout.gridx++;
        panel.add(comboUnitMarketMethod, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkUnitMarketRegionalMekVariations, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblUnitMarketSpecialUnitChance, layout);
        layout.gridx++;
        panel.add(spnUnitMarketSpecialUnitChance, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblUnitMarketRarityModifier, layout);
        layout.gridx++;
        panel.add(spnUnitMarketRarityModifier, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkInstantUnitMarketDelivery, layout);

        layout.gridy++;
        panel.add(chkUnitMarketReportRefresh, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "UnitMarketTab");
    }

    /**
     * Initializes the settings and UI components related to the Contract Market.
     * <p>
     * This includes options for contract market methods, payment settings, salvage percentages,
     * and other contract-specific configurations.
     */
    private void initializeContractMarket() {
        pnlContractMarketGeneralOptions = new JPanel();
        lblContractMarketMethod = new JLabel();
        comboContractMarketMethod = new MMComboBox<>("comboContractMarketMethod",
            ContractMarketMethod.values());
        lblContractSearchRadius = new JLabel();
        spnContractSearchRadius = new JSpinner();
        chkVariableContractLength = new JCheckBox();
        chkContractMarketReportRefresh = new JCheckBox();
        lblCoontractMaxSalvagePercentage = new JLabel();
        spnContractMaxSalvagePercentage = new JSpinner();
        lblDropShipBonusPercentage = new JLabel();
        spnDropShipBonusPercentage = new JSpinner();

        pnlContractPay = new JPanel();
        btnContractEquipment = new JRadioButton();
        lblEquipPercent = new JLabel();
        spnEquipPercent = new JSpinner();
        chkEquipContractSaleValue = new JCheckBox();
        lblDropShipPercent = new JLabel();
        spnDropShipPercent = new JSpinner();
        lblJumpShipPercent = new JLabel();
        spnJumpShipPercent = new JSpinner();
        lblWarShipPercent = new JLabel();
        spnWarShipPercent = new JSpinner();
        btnContractPersonnel = new JRadioButton();
        useInfantryDoseNotCountBox = new JCheckBox();
        chkMercSizeLimited = new JCheckBox();
        chkBLCSaleValue = new JCheckBox();
        chkOverageRepaymentInFinalPayment = new JCheckBox();
    }

    /**
     * Creates and returns the JPanel representing the Contract Market configuration tab.
     * <p>
     * This tab includes settings for configuring various aspects of contract acquisition,
     * such as methods, search radius, payment options, and variable contract length.
     *
     * @return A {@link JPanel} for the Contract Market configuration tab.
     */
    public JPanel createContractMarketTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("ContractMarketTab",
            getImageDirectory() + "logo_federated_suns.png");

        // Contents
        pnlContractMarketGeneralOptions = createContractMarketGeneralOptionsPanel();
        pnlContractPay = createContractPayPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ContractMarketTab", true,
            "");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(pnlContractMarketGeneralOptions, layout);
        layout.gridx++;
        panel.add(pnlContractPay, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "ContractMarketTab");
    }

    /**
     * Builds the general settings panel for the Contract Market tab, which includes
     * options for the contract market method, search radius, salvage percentages, and
     * other general configurations.
     *
     * @return A {@link JPanel} representing general options within the Contract Market tab.
     */
    private JPanel createContractMarketGeneralOptionsPanel() {
        // Contents
        lblContractMarketMethod = new CampaignOptionsLabel("ContractMarketMethod");
        comboContractMarketMethod = new MMComboBox<>("comboContractMarketMethod",
            ContractMarketMethod.values());

        lblContractSearchRadius = new CampaignOptionsLabel("ContractSearchRadius");
        spnContractSearchRadius = new CampaignOptionsSpinner("ContractSearchRadius",
            300, 100, 2500, 100);

        chkVariableContractLength = new CampaignOptionsCheckBox("VariableContractLength");

        chkContractMarketReportRefresh = new CampaignOptionsCheckBox("ContractMarketReportRefresh");

        lblCoontractMaxSalvagePercentage = new CampaignOptionsLabel("CoontractMaxSalvagePercentage");
        spnContractMaxSalvagePercentage = new CampaignOptionsSpinner("CoontractMaxSalvagePercentage",
            100, 0, 100, 10);

        lblDropShipBonusPercentage = new CampaignOptionsLabel("DropShipBonusPercentage");
        spnDropShipBonusPercentage = new CampaignOptionsSpinner("DropShipBonusPercentage",
            0, 0, 20, 5);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ContractMarketGeneralOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(lblContractMarketMethod, layout);
        layout.gridx++;
        panel.add(comboContractMarketMethod, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblContractSearchRadius, layout);
        layout.gridx++;
        panel.add(spnContractSearchRadius, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkVariableContractLength, layout);

        layout.gridy++;
        panel.add(chkContractMarketReportRefresh, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblCoontractMaxSalvagePercentage, layout);
        layout.gridx++;
        panel.add(spnContractMaxSalvagePercentage, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblDropShipBonusPercentage, layout);
        layout.gridx++;
        panel.add(spnDropShipBonusPercentage, layout);

        return panel;
    }

    /**
     * Creates the panel for configuring payment settings in the Contract Market tab.
     * <p>
     * This panel contains options for configuring equipment-based payment percentages,
     * override repayment rules, and toggles for contract payment methods.
     *
     * @return A {@link JPanel} containing payment configuration settings for the Contract Market.
     */
    private JPanel createContractPayPanel() {
        // Contents
        btnContractEquipment = new JRadioButton(resources.getString("lblContractEquipment.text"));
        btnContractEquipment.setToolTipText(resources.getString("lblContractEquipment.tooltip"));

        btnContractPersonnel = new JRadioButton(resources.getString("lblContractPersonnel.text"));
        btnContractPersonnel.setToolTipText(resources.getString("lblContractPersonnel.tooltip"));

        // Create a ButtonGroup to link the buttons
        ButtonGroup contractGroup = new ButtonGroup();
        contractGroup.add(btnContractEquipment);
        contractGroup.add(btnContractPersonnel);

        // Add other components here...
        chkEquipContractSaleValue = new CampaignOptionsCheckBox("EquipContractSaleValue");

        lblEquipPercent = new CampaignOptionsLabel("EquipPercent");
        spnEquipPercent = new CampaignOptionsSpinner("EquipPercent",
                0.1, 0, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT, 0.1);

        lblDropShipPercent = new CampaignOptionsLabel("DropShipPercent");
        spnDropShipPercent = new CampaignOptionsSpinner("DropShipPercent",
                0.1, 0, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT, 0.1);

        lblJumpShipPercent = new CampaignOptionsLabel("JumpShipPercent");
        spnJumpShipPercent = new CampaignOptionsSpinner("JumpShipPercent",
                0.1, 0, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT, 0.1);

        lblWarShipPercent = new CampaignOptionsLabel("WarShipPercent");
        spnWarShipPercent = new CampaignOptionsSpinner("WarShipPercent",
                0.1, 0, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT, 0.1);

        chkBLCSaleValue = new CampaignOptionsCheckBox("BLCSaleValue");

        useInfantryDoseNotCountBox = new CampaignOptionsCheckBox("UseInfantryDoseNotCountBox");

        chkMercSizeLimited = new CampaignOptionsCheckBox("MercSizeLimited");

        chkOverageRepaymentInFinalPayment = new CampaignOptionsCheckBox("OverageRepaymentInFinalPayment");

        // Layout the Panel
        final JPanel panelValuePercent = new CampaignOptionsStandardPanel("ContractPayPanelValuePercent",
            false);
        final GridBagConstraints layoutValuePercent = new CampaignOptionsGridBagConstraints(panelValuePercent);

        layoutValuePercent.gridx = 0;
        layoutValuePercent.gridy = 0;
        layoutValuePercent.gridwidth = 1;
        panelValuePercent.add(chkEquipContractSaleValue, layoutValuePercent);

        layoutValuePercent.gridy++;
        panelValuePercent.add(lblEquipPercent, layoutValuePercent);
        layoutValuePercent.gridx++;
        panelValuePercent.add(spnEquipPercent, layoutValuePercent);

        layoutValuePercent.gridx = 0;
        layoutValuePercent.gridy++;
        panelValuePercent.add(lblDropShipPercent, layoutValuePercent);
        layoutValuePercent.gridx++;
        panelValuePercent.add(spnDropShipPercent, layoutValuePercent);

        layoutValuePercent.gridx = 0;
        layoutValuePercent.gridy++;
        panelValuePercent.add(lblJumpShipPercent, layoutValuePercent);
        layoutValuePercent.gridx++;
        panelValuePercent.add(spnJumpShipPercent, layoutValuePercent);

        layoutValuePercent.gridx = 0;
        layoutValuePercent.gridy++;
        panelValuePercent.add(lblWarShipPercent, layoutValuePercent);
        layoutValuePercent.gridx++;
        panelValuePercent.add(spnWarShipPercent, layoutValuePercent);

        final JPanel panel = new CampaignOptionsStandardPanel("ContractPayPanel",
            true, "ContractPayPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(btnContractEquipment, layout);

        layout.gridy++;
        panel.add(panelValuePercent, layout);

        layout.gridy++;
        panel.add(btnContractPersonnel, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkBLCSaleValue, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(useInfantryDoseNotCountBox, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkMercSizeLimited, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkOverageRepaymentInFinalPayment, layout);

        return panel;
    }


    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads the campaign options from the associated {@link Campaign} into
     * the UI components of the market tabs. This includes personnel, unit,
     * and contract market settings.
     * <p>
     * If no preset options are provided, the current campaign options are loaded.
     *
     * @param presetCampaignOptions A {@link CampaignOptions} object with previously
     *                              configured settings, or {@code null} to use the
     *                              current campaign's options.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Personnel Market
        comboPersonnelMarketType.setSelectedItem(options.getPersonnelMarketName());
        chkPersonnelMarketReportRefresh.setSelected(options.isContractMarketReportRefresh());
        chkUsePersonnelHireHiringHallOnly.setSelected(options.isUsePersonnelHireHiringHallOnly());
        spnPersonnelMarketDylansWeight.setValue(options.getPersonnelMarketDylansWeight());
        for (final Entry<SkillLevel, JSpinner> entry : spnPersonnelMarketRandomRemovalTargets.entrySet()) {
            entry.getValue().setValue(options.getPersonnelMarketRandomRemovalTargets().get(entry.getKey()));
        }

        // Unit Market
        comboUnitMarketMethod.setSelectedItem(options.getUnitMarketMethod());
        chkUnitMarketRegionalMekVariations.setSelected(options.isRegionalMekVariations());
        spnUnitMarketSpecialUnitChance.setValue(options.getUnitMarketSpecialUnitChance());
        spnUnitMarketRarityModifier.setValue(options.getUnitMarketRarityModifier());
        chkInstantUnitMarketDelivery.setSelected(options.isInstantUnitMarketDelivery());
        chkUnitMarketReportRefresh.setSelected(options.isContractMarketReportRefresh());

        // Contract Market
        comboContractMarketMethod.setSelectedItem(options.getContractMarketMethod());
        spnContractSearchRadius.setValue(options.getContractSearchRadius());
        chkVariableContractLength.setSelected(options.isVariableContractLength());
        chkContractMarketReportRefresh.setSelected(options.isContractMarketReportRefresh());
        spnContractMaxSalvagePercentage.setValue(options.getContractMaxSalvagePercentage());
        spnDropShipBonusPercentage.setValue(options.getDropShipBonusPercentage());
        if (options.isEquipmentContractBase()) {
            btnContractEquipment.setSelected(true);
        } else {
            btnContractPersonnel.setSelected(true);
        }
        spnEquipPercent.setValue(options.getEquipmentContractPercent());
        chkEquipContractSaleValue.setSelected(options.isEquipmentContractSaleValue());
        spnDropShipPercent.setValue(options.getDropShipContractPercent());
        spnJumpShipPercent.setValue(options.getJumpShipContractPercent());
        spnWarShipPercent.setValue(options.getWarShipContractPercent());
        useInfantryDoseNotCountBox.setSelected(options.isInfantryDontCount());
        chkMercSizeLimited.setSelected(options.isMercSizeLimited());
        chkBLCSaleValue.setSelected(options.isBLCSaleValue());
        chkOverageRepaymentInFinalPayment.setSelected(options.isOverageRepaymentInFinalPayment());
    }

    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Personnel Market
        options.setPersonnelMarketName(comboPersonnelMarketType.getSelectedItem());
        if (Objects.equals(comboPersonnelMarketType.getSelectedItem(), "Campaign Ops")) {
            campaign.getPersonnelMarket().setPaidRecruitment(false);
        }
        options.setPersonnelMarketDylansWeight((double) spnPersonnelMarketDylansWeight.getValue());
        options.setUsePersonnelHireHiringHallOnly(chkUsePersonnelHireHiringHallOnly.isSelected());
        options.setPersonnelMarketReportRefresh(chkPersonnelMarketReportRefresh.isSelected());
        for (final Entry<SkillLevel, JSpinner> entry : spnPersonnelMarketRandomRemovalTargets
            .entrySet()) {
            options.getPersonnelMarketRandomRemovalTargets().put(entry.getKey(),
                (int) entry.getValue().getValue());
        }

        // Unit Market
        options.setUnitMarketMethod(comboUnitMarketMethod.getSelectedItem());
        options.setUnitMarketRegionalMekVariations(chkUnitMarketRegionalMekVariations.isSelected());
        options.setUnitMarketSpecialUnitChance((int) spnUnitMarketSpecialUnitChance.getValue());
        options.setUnitMarketRarityModifier((int) spnUnitMarketRarityModifier.getValue());
        options.setInstantUnitMarketDelivery(chkInstantUnitMarketDelivery.isSelected());
        options.setUnitMarketReportRefresh(chkUnitMarketReportRefresh.isSelected());

        // Contract Market
        options.setContractMarketMethod(comboContractMarketMethod.getSelectedItem());
        options.setContractSearchRadius((int) spnContractSearchRadius.getValue());
        options.setVariableContractLength(chkVariableContractLength.isSelected());
        options.setContractMarketReportRefresh(chkContractMarketReportRefresh.isSelected());
        options.setContractMaxSalvagePercentage((int) spnContractMaxSalvagePercentage.getValue());
        options.setDropShipBonusPercentage((int) spnDropShipBonusPercentage.getValue());
        options.setEquipmentContractBase(btnContractEquipment.isSelected());
        options.setEquipmentContractPercent((double) spnEquipPercent.getValue());
        options.setDropShipContractPercent((double) spnDropShipPercent.getValue());
        options.setJumpShipContractPercent((double) spnJumpShipPercent.getValue());
        options.setWarShipContractPercent((double) spnWarShipPercent.getValue());
        options.setEquipmentContractSaleValue(chkEquipContractSaleValue.isSelected());
        options.setBLCSaleValue(chkBLCSaleValue.isSelected());
        options.setOverageRepaymentInFinalPayment(chkOverageRepaymentInFinalPayment.isSelected());
    }
}
