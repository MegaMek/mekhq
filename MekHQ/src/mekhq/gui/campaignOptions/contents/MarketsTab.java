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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.market.personnelMarket.markets.PersonnelMarketCamOpsRevised;
import mekhq.campaign.market.personnelMarket.markets.PersonnelMarketCamOpsStrict;
import mekhq.campaign.market.personnelMarket.markets.PersonnelMarketMekHQ;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

/**
 * The {@code MarketsTab} class represents the campaign options tab related to market settings. This tab provides
 * configurations for three key market areas:
 * <ul>
 *     <li><b>Personnel Market</b>: Settings for managing personnel hiring, removal targets, and market types.</li>
 *     <li><b>Unit Market</b>: Configurations for purchasing units, special unit chances, rarity modifiers, etc.</li>
 *     <li><b>Contract Market</b>: Options for contract acquisition, such as market methods, search radius,
 *         and payment settings.</li>
 * </ul>
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
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
      private MarketsDraft draft;
      private boolean personnelMarketPageCreated;
      private boolean unitMarketPageCreated;
      private boolean contractMarketPageCreated;

    //start Personnel Market
    private CampaignOptionsHeaderPanel personnelMarketHeader;
    private JPanel pnlPersonnelMarketGeneralOptions;
    private JLabel lblPersonnelMarketStyle;
    private MMComboBox<PersonnelMarketStyle> comboPersonnelMarketStyle;
    private JCheckBox chkPersonnelMarketReportRefresh;
    private JCheckBox chkUsePersonnelHireHiringHallOnly;
    @Deprecated(since = "0.50.06")
    private JLabel lblPersonnelMarketType;
    @Deprecated(since = "0.50.06")
    private MMComboBox<String> comboPersonnelMarketType;

    @Deprecated(since = "0.50.06")
    private JPanel pnlRemovalTargets;
    @Deprecated(since = "0.50.06")
    private JLabel lblPersonnelMarketDylansWeight;
    @Deprecated(since = "0.50.06")
    private JSpinner spnPersonnelMarketDylansWeight;
    @Deprecated(since = "0.50.06")
    private Map<SkillLevel, JLabel> lblPersonnelMarketRandomRemovalTargets;
    @Deprecated(since = "0.50.06")
    private Map<SkillLevel, JSpinner> spnPersonnelMarketRandomRemovalTargets;
    //end Personnel Market

    private JLabel lblUnitMarketMethod;
    private MMComboBox<UnitMarketMethod> comboUnitMarketMethod;
    private JCheckBox chkUnitMarketRegionalMekVariations;
    private JLabel lblUnitMarketArtilleryUnitChance;
    private JSpinner spnUnitMarketArtilleryUnitChance;
    private JLabel lblUnitMarketRarityModifier;
    private JSpinner spnUnitMarketRarityModifier;
    private JCheckBox chkInstantUnitMarketDelivery;
    private JCheckBox chkMothballUnitMarketDeliveries;
    private JCheckBox chkUnitMarketReportRefresh;
    //end Unit Market

    //start Contract Market
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
    private JCheckBox useInfantryDoseNotCountBox;
    private JCheckBox chkMercSizeLimited;
    private JCheckBox chkBLCSaleValue;
    private JCheckBox chkOverageRepaymentInFinalPayment;
    //end Contract Market

    /**
     * Constructs a {@code MarketsTab} with the provided campaign. Initializes the market configuration options based on
     * the settings of the given {@link Campaign}.
     *
     * @param campaign The {@link Campaign} associated with this market tab. This campaign is used to retrieve and
     *                 modify {@link CampaignOptions}.
     */
    public MarketsTab(Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();

        initialize();
            loadValuesFromCampaignOptions();
    }

    /**
     * Initializes the market-related options tabs by setting up configurations for the Personnel Market, Unit Market,
     * and Contract Market.
     * <p>
     * This method is invoked internally within the constructor to prepare the various market configurations for use in
     * the UI.
     */
    private void initialize() {
        initializePersonnelMarket();
        initializeUnitMarket();
        initializeContractMarket();
    }

    /**
     * Initializes the settings and UI components related to the Personnel Market.
     * <p>
     * This includes setting up labels, combo boxes for selecting the personnel market type, checkboxes for additional
     * options, and spinners for configuring removal targets.
     */
    private void initializePersonnelMarket() {
        pnlPersonnelMarketGeneralOptions = new JPanel();
        lblPersonnelMarketType = new JLabel();
        comboPersonnelMarketType = new MMComboBox<>("comboPersonnelMarketType", getPersonnelMarketTypeOptions());
        lblPersonnelMarketStyle = new JLabel();
        comboPersonnelMarketStyle = new MMComboBox<>("comboPersonnelMarketStyle", PersonnelMarketStyle.values());
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
     * These types are fetched from the {@link PersonnelMarketServiceManager} and represent the available personnel
     * market methods configured for the campaign.
     *
     * @return A {@link DefaultComboBoxModel} containing the personnel market type options.
     */
    @Deprecated(since = "0.50.06")
    private static DefaultComboBoxModel<String> getPersonnelMarketTypeOptions() {
        final DefaultComboBoxModel<String> personnelMarketTypeModel = new DefaultComboBoxModel<>();
        for (final PersonnelMarketMethod method : PersonnelMarketServiceManager.getInstance().getAllServices(true)) {
            personnelMarketTypeModel.addElement(method.getModuleName());
        }
        return personnelMarketTypeModel;
    }

    /**
     * Creates and returns the JPanel representing the Personnel Market configuration tab.
     * <p>
     * This tab includes general personnel market settings, as well as removal target configuration options for various
     * skill levels.
     *
     * @return A {@link JPanel} for the Personnel Market configuration tab.
     */
    public JPanel createPersonnelMarketTab() {
        // Header
        personnelMarketHeader = new CampaignOptionsHeaderPanel("PersonnelMarketTab",
              getImageDirectory() + "logo_st_ives_compact.png", 9);

        // Contents
        pnlPersonnelMarketGeneralOptions = createPersonnelMarketGeneralOptionsPanel();
        pnlRemovalTargets = createPersonnelMarketRemovalOptionsPanel();
      personnelMarketPageCreated = true;
      updatePersonnelMarketControlsFromDraft();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelMarketTab", true, "");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(personnelMarketHeader, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(pnlPersonnelMarketGeneralOptions, layout);
        layout.gridx++;
        panel.add(pnlRemovalTargets, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "PersonnelMarketTab");

    }

    /**
     * Builds the general options panel for the Personnel Market tab, which includes settings such as the personnel
     * market type, Dylan's weight, and options like report refresh toggles.
     * <p>
     * These components are laid out into a panel and returned for use in the UI.
     *
     * @return A {@link JPanel} representing the general options within the Personnel Market tab.
     */
    private JPanel createPersonnelMarketGeneralOptionsPanel() {
        // Contents
        lblPersonnelMarketStyle = new CampaignOptionsLabel("PersonnelMarketStyle",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED));
        lblPersonnelMarketStyle.addMouseListener(createTipPanelUpdater(personnelMarketHeader, "PersonnelMarketStyle"));
        comboPersonnelMarketStyle.addMouseListener(createTipPanelUpdater(personnelMarketHeader,
              "PersonnelMarketStyle"));

        lblPersonnelMarketType = new CampaignOptionsLabel("PersonnelMarketType");
        lblPersonnelMarketType.addMouseListener(createTipPanelUpdater(personnelMarketHeader, "PersonnelMarketType"));
        comboPersonnelMarketType = new MMComboBox<>("comboPersonnelMarketType", getPersonnelMarketTypeOptions());
        comboPersonnelMarketType.addMouseListener(createTipPanelUpdater(personnelMarketHeader, "PersonnelMarketType"));

        lblPersonnelMarketDylansWeight = new CampaignOptionsLabel("PersonnelMarketDylansWeight");
        lblPersonnelMarketDylansWeight.addMouseListener(createTipPanelUpdater(personnelMarketHeader,
              "PersonnelMarketDylansWeight"));
        spnPersonnelMarketDylansWeight = new CampaignOptionsSpinner("PersonnelMarketDylansWeight", 0.3, 0, 1, 0.1);
        spnPersonnelMarketDylansWeight.addMouseListener(createTipPanelUpdater(personnelMarketHeader,
              "PersonnelMarketDylansWeight"));

        chkPersonnelMarketReportRefresh = new CampaignOptionsCheckBox("PersonnelMarketReportRefresh");
        chkPersonnelMarketReportRefresh.addMouseListener(createTipPanelUpdater(personnelMarketHeader,
              "PersonnelMarketReportRefresh"));

        chkUsePersonnelHireHiringHallOnly = new CampaignOptionsCheckBox("UsePersonnelHireHiringHallOnly",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUsePersonnelHireHiringHallOnly.addMouseListener(createTipPanelUpdater(personnelMarketHeader,
              "UsePersonnelHireHiringHallOnly"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelMarketGeneralOptionsPanel", false, "");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(lblPersonnelMarketStyle, layout);
        layout.gridx++;
        panel.add(comboPersonnelMarketStyle, layout);

        layout.gridx = 0;
        layout.gridy++;
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
     * This panel includes settings for removal targets, which are based on various {@link SkillLevel} entries. Each
     * skill level configuration includes both a label and an associated spinner for setting values.
     *
     * @return A {@link JPanel} containing removal options for the Personnel Market.
     */
    private JPanel createPersonnelMarketRemovalOptionsPanel() {
        // Contents
        for (final SkillLevel skillLevel : Skills.SKILL_LEVELS) {
            final JLabel jLabel = new JLabel(skillLevel.toString());
            lblPersonnelMarketRandomRemovalTargets.put(skillLevel, jLabel);

            final JSpinner jSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));

            DefaultEditor editor = (DefaultEditor) jSpinner.getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);

            spnPersonnelMarketRandomRemovalTargets.put(skillLevel, jSpinner);
        }

        // Layout the Panels
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelMarketRemovalOptionsPanel",
              true,
              "PersonnelMarketRemovalOptionsPanel");
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
     * This includes various elements such as labels, combo boxes, checkboxes, and spinners for settings like unit
     * market methods, rarity modifiers, and delivery options.
     */
    private void initializeUnitMarket() {
        lblUnitMarketMethod = new JLabel();
        comboUnitMarketMethod = new MMComboBox<>("comboUnitMarketMethod", UnitMarketMethod.values());
        chkUnitMarketRegionalMekVariations = new JCheckBox();
        lblUnitMarketArtilleryUnitChance = new JLabel();
        spnUnitMarketArtilleryUnitChance = new JSpinner();
        lblUnitMarketRarityModifier = new JLabel();
        spnUnitMarketRarityModifier = new JSpinner();
        chkInstantUnitMarketDelivery = new JCheckBox();
        chkMothballUnitMarketDeliveries = new JCheckBox();
        chkUnitMarketReportRefresh = new JCheckBox();
    }

    /**
     * Creates and returns the JPanel representing the Unit Market configuration tab.
     * <p>
     * This tab includes options such as unit market methods, rarity modifiers, special unit change settings, and more.
     *
     * @return A {@link JPanel} for the Unit Market configuration tab.
     */
    public JPanel createUnitMarketTab() {
        // Header
        //start Unit Market
        CampaignOptionsHeaderPanel unitMarketHeader = new CampaignOptionsHeaderPanel("UnitMarketTab",
              getImageDirectory() + "logo_clan_ice_hellion.png",
              4);

        // Contents
        lblUnitMarketMethod = new CampaignOptionsLabel("UnitMarketMethod");
        lblUnitMarketMethod.addMouseListener(createTipPanelUpdater(unitMarketHeader, "UnitMarketMethod"));
        comboUnitMarketMethod = new MMComboBox<>("comboUnitMarketMethod", UnitMarketMethod.values());
        comboUnitMarketMethod.addMouseListener(createTipPanelUpdater(unitMarketHeader, "UnitMarketMethod"));

        chkUnitMarketRegionalMekVariations = new CampaignOptionsCheckBox("UnitMarketRegionalMekVariations");
        chkUnitMarketRegionalMekVariations.addMouseListener(createTipPanelUpdater(unitMarketHeader,
              "UnitMarketRegionalMekVariations"));

        lblUnitMarketArtilleryUnitChance = new CampaignOptionsLabel("UnitMarketArtilleryUnitChance");
        lblUnitMarketArtilleryUnitChance.addMouseListener(createTipPanelUpdater(unitMarketHeader,
              "UnitMarketArtilleryUnitChance"));
        spnUnitMarketArtilleryUnitChance = new CampaignOptionsSpinner("UnitMarketArtilleryUnitChance", 30, 0, 100, 1);
        spnUnitMarketArtilleryUnitChance.addMouseListener(createTipPanelUpdater(unitMarketHeader,
              "UnitMarketArtilleryUnitChance"));

        lblUnitMarketRarityModifier = new CampaignOptionsLabel("UnitMarketRarityModifier");
        lblUnitMarketRarityModifier.addMouseListener(createTipPanelUpdater(unitMarketHeader,
              "UnitMarketRarityModifier"));
        spnUnitMarketRarityModifier = new CampaignOptionsSpinner("UnitMarketRarityModifier", 0, -10, 10, 1);
        spnUnitMarketRarityModifier.addMouseListener(createTipPanelUpdater(unitMarketHeader,
              "UnitMarketRarityModifier"));

        chkInstantUnitMarketDelivery = new CampaignOptionsCheckBox("InstantUnitMarketDelivery");
        chkInstantUnitMarketDelivery.addMouseListener(createTipPanelUpdater(unitMarketHeader,
              "InstantUnitMarketDelivery"));

        chkMothballUnitMarketDeliveries = new CampaignOptionsCheckBox("MothballUnitMarketDeliveries");
        chkMothballUnitMarketDeliveries.addMouseListener(createTipPanelUpdater(unitMarketHeader,
              "MothballUnitMarketDeliveries"));

        chkUnitMarketReportRefresh = new CampaignOptionsCheckBox("UnitMarketReportRefresh");
        chkUnitMarketReportRefresh.addMouseListener(createTipPanelUpdater(unitMarketHeader, "UnitMarketReportRefresh"));
      unitMarketPageCreated = true;
      updateUnitMarketControlsFromDraft();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("UnitMarketTab", true, "");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridy = 0;
        panel.add(unitMarketHeader, layout);

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
        panel.add(lblUnitMarketArtilleryUnitChance, layout);
        layout.gridx++;
        panel.add(spnUnitMarketArtilleryUnitChance, layout);

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
        panel.add(chkMothballUnitMarketDeliveries, layout);

        layout.gridy++;
        panel.add(chkUnitMarketReportRefresh, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "UnitMarketTab");
    }

    /**
     * Initializes the settings and UI components related to the Contract Market.
     * <p>
     * This includes options for contract market methods, payment settings, salvage percentages, and other
     * contract-specific configurations.
     */
    private void initializeContractMarket() {
        pnlContractMarketGeneralOptions = new JPanel();
        lblContractMarketMethod = new JLabel();
        comboContractMarketMethod = new MMComboBox<>("comboContractMarketMethod");
        lblContractSearchRadius = new JLabel();
        spnContractSearchRadius = new JSpinner();
        chkVariableContractLength = new JCheckBox();
        chkUseTwoWayPay = new JCheckBox();
        chkUseCamOpsSalvage = new JCheckBox();
        chkUseRiskySalvage = new JCheckBox();
        chkEnableSalvageFlagByDefault = new JCheckBox();
        chkUseDynamicDifficulty = new JCheckBox();
        chkUseBolsterContractSkill = new JCheckBox();
        chkContractMarketReportRefresh = new JCheckBox();
        lblContractMaxSalvagePercentage = new JLabel();
        spnContractMaxSalvagePercentage = new JSpinner();
        lblDropShipBonusPercentage = new JLabel();
        spnDropShipBonusPercentage = new JSpinner();
        lblPityContracts = new JLabel();
        spnPityContracts = new JSpinner();

        pnlContractPay = new JPanel();
        btnContractEquipment = new JRadioButton();
        lblEquipPercent = new JLabel();
        spnEquipPercent = new JSpinner();
        chkUseAlternatePaymentMode = new JCheckBox();
        chkUseDiminishingContractPay = new JCheckBox();
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
     * This tab includes settings for configuring various aspects of contract acquisition, such as methods, search
     * radius, payment options, and variable contract length.
     *
     * @return A {@link JPanel} for the Contract Market configuration tab.
     */
    public JPanel createContractMarketTab() {
        // Header
        contractMarketHeader = new CampaignOptionsHeaderPanel("ContractMarketTab",
              getImageDirectory() + "logo_federated_suns.png",
              3);
        // Contents
        pnlContractMarketGeneralOptions = createContractMarketGeneralOptionsPanel();
        pnlContractPay = createContractPayPanel();
      contractMarketPageCreated = true;
      updateContractMarketControlsFromDraft();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ContractMarketTab", true, "");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(contractMarketHeader, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(pnlContractMarketGeneralOptions, layout);
        layout.gridx++;
        panel.add(pnlContractPay, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "ContractMarketTab");
    }

    /**
     * Builds the general settings panel for the Contract Market tab, which includes options for the contract market
     * method, search radius, salvage percentages, and other general configurations.
     *
     * @return A {@link JPanel} representing general options within the Contract Market tab.
     */
    private JPanel createContractMarketGeneralOptionsPanel() {
        // Contents
        lblContractMarketMethod = new CampaignOptionsLabel("ContractMarketMethod");
        lblContractMarketMethod.addMouseListener(createTipPanelUpdater(contractMarketHeader, "ContractMarketMethod"));
        comboContractMarketMethod = new MMComboBox<>("comboContractMarketMethod");
        DefaultComboBoxModel<ContractMarketMethod> model = new DefaultComboBoxModel<>(ContractMarketMethod.values());
        model.removeElement(ContractMarketMethod.CAM_OPS);
        comboContractMarketMethod.setModel(model);
        comboContractMarketMethod.addMouseListener(createTipPanelUpdater(contractMarketHeader, "ContractMarketMethod"));

        lblContractSearchRadius = new CampaignOptionsLabel("ContractSearchRadius");
        lblContractSearchRadius.addMouseListener(createTipPanelUpdater(contractMarketHeader, "ContractSearchRadius"));
        spnContractSearchRadius = new CampaignOptionsSpinner("ContractSearchRadius", 300, 1, 2500, 100);
        spnContractSearchRadius.addMouseListener(createTipPanelUpdater(contractMarketHeader, "ContractSearchRadius"));

        chkVariableContractLength = new CampaignOptionsCheckBox("VariableContractLength");
        chkVariableContractLength.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "VariableContractLength"));

        chkUseTwoWayPay = new CampaignOptionsCheckBox("UseTwoWayPay",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseTwoWayPay.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "UseTwoWayPay"));

        chkUseCamOpsSalvage = new CampaignOptionsCheckBox("UseCamOpsSalvage",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseCamOpsSalvage.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "UseCamOpsSalvage"));

        chkUseRiskySalvage = new CampaignOptionsCheckBox("UseRiskySalvage",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseRiskySalvage.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "UseRiskySalvage"));

        chkEnableSalvageFlagByDefault = new CampaignOptionsCheckBox("EnableSalvageFlagByDefault",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkEnableSalvageFlagByDefault.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "EnableSalvageFlagByDefault"));

        chkUseDynamicDifficulty = new CampaignOptionsCheckBox("UseDynamicDifficulty");
        chkUseDynamicDifficulty.addMouseListener(createTipPanelUpdater(contractMarketHeader, "UseDynamicDifficulty"));

        chkUseBolsterContractSkill = new CampaignOptionsCheckBox("UseBolsterContractSkill",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseBolsterContractSkill.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "UseBolsterContractSkill"));

        chkContractMarketReportRefresh = new CampaignOptionsCheckBox("ContractMarketReportRefresh");
        chkContractMarketReportRefresh.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "ContractMarketReportRefresh"));

        lblContractMaxSalvagePercentage = new CampaignOptionsLabel("ContractMaxSalvagePercentage");
        lblContractMaxSalvagePercentage.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "ContractMaxSalvagePercentage"));
        spnContractMaxSalvagePercentage = new CampaignOptionsSpinner("ContractMaxSalvagePercentage", 100, 0, 100, 10);
        spnContractMaxSalvagePercentage.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "ContractMaxSalvagePercentage"));

        lblDropShipBonusPercentage = new CampaignOptionsLabel("DropShipBonusPercentage");
        lblDropShipBonusPercentage.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "DropShipBonusPercentage"));
        spnDropShipBonusPercentage = new CampaignOptionsSpinner("DropShipBonusPercentage", 0, 0, 20, 5);
        spnDropShipBonusPercentage.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "DropShipBonusPercentage"));

        lblPityContracts = new CampaignOptionsLabel("PityContracts", getMetadata(new Version(0, 51, 0)));
        lblPityContracts.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "PityContracts"));
        spnPityContracts = new CampaignOptionsSpinner("PityContracts", 4, 0, 20, 1);
        spnPityContracts.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "PityContracts"));

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
        panel.add(chkUseTwoWayPay, layout);

        layout.gridy++;
        panel.add(chkUseCamOpsSalvage, layout);

        layout.gridy++;
        panel.add(chkUseRiskySalvage, layout);

        layout.gridy++;
        panel.add(chkEnableSalvageFlagByDefault, layout);

        layout.gridy++;
        panel.add(chkUseDynamicDifficulty, layout);

        layout.gridy++;
        panel.add(chkUseBolsterContractSkill, layout);

        layout.gridy++;
        panel.add(chkContractMarketReportRefresh, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblContractMaxSalvagePercentage, layout);
        layout.gridx++;
        panel.add(spnContractMaxSalvagePercentage, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblDropShipBonusPercentage, layout);
        layout.gridx++;
        panel.add(spnDropShipBonusPercentage, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblPityContracts, layout);
        layout.gridx++;
        panel.add(spnPityContracts, layout);

        return panel;
    }

    /**
     * Creates the panel for configuring payment settings in the Contract Market tab.
     * <p>
     * This panel contains options for configuring equipment-based payment percentages, override repayment rules, and
     * toggles for contract payment methods.
     *
     * @return A {@link JPanel} containing payment configuration settings for the Contract Market.
     */
    private JPanel createContractPayPanel() {
        // Contents
        btnContractEquipment = new JRadioButton(getTextAt(getCampaignOptionsResourceBundle(),
              "lblContractEquipment.text"));
        btnContractEquipment.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(),
              "lblContractEquipment.tooltip"));
        btnContractEquipment.addMouseListener(createTipPanelUpdater(contractMarketHeader, "ContractEquipment"));

        btnContractPersonnel = new JRadioButton(getTextAt(getCampaignOptionsResourceBundle(),
              "lblContractPersonnel.text"));
        btnContractPersonnel.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(),
              "lblContractPersonnel.tooltip"));
        btnContractPersonnel.addMouseListener(createTipPanelUpdater(contractMarketHeader, "ContractPersonnel"));

        ButtonGroup contractGroup = new ButtonGroup();
        contractGroup.add(btnContractEquipment);
        contractGroup.add(btnContractPersonnel);

        chkUseAlternatePaymentMode = new CampaignOptionsCheckBox("UseAlternatePaymentMode",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseAlternatePaymentMode.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "UseAlternatePaymentMode"));

        chkUseDiminishingContractPay = new CampaignOptionsCheckBox("UseDiminishingContractPay",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseDiminishingContractPay.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "UseDiminishingContractPay"));

        chkEquipContractSaleValue = new CampaignOptionsCheckBox("EquipContractSaleValue");
        chkEquipContractSaleValue.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "EquipContractSaleValue"));

        lblEquipPercent = new CampaignOptionsLabel("EquipPercent");
        lblEquipPercent.addMouseListener(createTipPanelUpdater(contractMarketHeader, "EquipPercent"));
        spnEquipPercent = new CampaignOptionsSpinner("EquipPercent",
              0.1,
              0,
              CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT,
              0.1);
        spnEquipPercent.addMouseListener(createTipPanelUpdater(contractMarketHeader, "EquipPercent"));

        lblDropShipPercent = new CampaignOptionsLabel("DropShipPercent");
        lblDropShipPercent.addMouseListener(createTipPanelUpdater(contractMarketHeader, "DropShipPercent"));
        spnDropShipPercent = new CampaignOptionsSpinner("DropShipPercent",
              0.1,
              0,
              CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT,
              0.1);
        spnDropShipPercent.addMouseListener(createTipPanelUpdater(contractMarketHeader, "DropShipPercent"));

        lblJumpShipPercent = new CampaignOptionsLabel("JumpShipPercent");
        lblJumpShipPercent.addMouseListener(createTipPanelUpdater(contractMarketHeader, "JumpShipPercent"));
        spnJumpShipPercent = new CampaignOptionsSpinner("JumpShipPercent",
              0.1,
              0,
              CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT,
              0.1);
        spnJumpShipPercent.addMouseListener(createTipPanelUpdater(contractMarketHeader, "JumpShipPercent"));

        lblWarShipPercent = new CampaignOptionsLabel("WarShipPercent");
        lblWarShipPercent.addMouseListener(createTipPanelUpdater(contractMarketHeader, "WarShipPercent"));
        spnWarShipPercent = new CampaignOptionsSpinner("WarShipPercent",
              0.1,
              0,
              CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT,
              0.1);
        spnWarShipPercent.addMouseListener(createTipPanelUpdater(contractMarketHeader, "WarShipPercent"));

        chkBLCSaleValue = new CampaignOptionsCheckBox("BLCSaleValue");
        chkBLCSaleValue.addMouseListener(createTipPanelUpdater(contractMarketHeader, "BLCSaleValue"));

        useInfantryDoseNotCountBox = new CampaignOptionsCheckBox("UseInfantryDoseNotCountBox");
        useInfantryDoseNotCountBox.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "UseInfantryDoseNotCountBox"));

        chkMercSizeLimited = new CampaignOptionsCheckBox("MercSizeLimited");
        chkMercSizeLimited.addMouseListener(createTipPanelUpdater(contractMarketHeader, "MercSizeLimited"));

        chkOverageRepaymentInFinalPayment = new CampaignOptionsCheckBox("OverageRepaymentInFinalPayment");
        chkOverageRepaymentInFinalPayment.addMouseListener(createTipPanelUpdater(contractMarketHeader,
              "OverageRepaymentInFinalPayment"));

        // Layout the Panel
        final JPanel panelValuePercent = new CampaignOptionsStandardPanel("ContractPayPanelValuePercent", false);
        final GridBagConstraints layoutValuePercent = new CampaignOptionsGridBagConstraints(panelValuePercent);

        layoutValuePercent.gridx = 0;
        layoutValuePercent.gridy = 0;
        layoutValuePercent.gridwidth = 1;
        panelValuePercent.add(chkEquipContractSaleValue, layoutValuePercent);

        layoutValuePercent.gridy++;
        panelValuePercent.add(chkUseAlternatePaymentMode, layoutValuePercent);

        layoutValuePercent.gridy++;
        panelValuePercent.add(chkUseDiminishingContractPay, layoutValuePercent);

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

        final JPanel panel = new CampaignOptionsStandardPanel("ContractPayPanel", true, "ContractPayPanel");
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
     * Loads the campaign options from the associated {@link Campaign} into the UI components of the market tabs. This
     * includes personnel, unit, and contract market settings.
     * <p>
     * If no preset options are provided, the current campaign options are loaded.
     *
     * @param presetCampaignOptions A {@link CampaignOptions} object with previously configured settings, or
     *                              {@code null} to use the current campaign's options.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

            draft = new MarketsDraft(options);
            updateCreatedControlsFromDraft();
    }

      private void updateCreatedControlsFromDraft() {
            updatePersonnelMarketControlsFromDraft();
            updateUnitMarketControlsFromDraft();
            updateContractMarketControlsFromDraft();
      }

      private void updatePersonnelMarketControlsFromDraft() {
            if (!personnelMarketPageCreated || draft == null) {
                  return;
            }

            comboPersonnelMarketStyle.setSelectedItem(draft.personnelMarketStyle);
            comboPersonnelMarketType.setSelectedItem(draft.personnelMarketName);
            chkPersonnelMarketReportRefresh.setSelected(draft.personnelMarketReportRefresh);
            chkUsePersonnelHireHiringHallOnly.setSelected(draft.usePersonnelHireHiringHallOnly);
            spnPersonnelMarketDylansWeight.setValue(draft.personnelMarketDylansWeight);
            for (final Entry<SkillLevel, JSpinner> entry : spnPersonnelMarketRandomRemovalTargets.entrySet()) {
                  entry.getValue().setValue(draft.personnelMarketRandomRemovalTargets.get(entry.getKey()));
            }
      }

      private void updateUnitMarketControlsFromDraft() {
            if (!unitMarketPageCreated || draft == null) {
                  return;
            }

            comboUnitMarketMethod.setSelectedItem(draft.unitMarketMethod);
            chkUnitMarketRegionalMekVariations.setSelected(draft.unitMarketRegionalMekVariations);
            spnUnitMarketArtilleryUnitChance.setValue(draft.unitMarketArtilleryUnitChance);
            spnUnitMarketRarityModifier.setValue(draft.unitMarketRarityModifier);
            chkInstantUnitMarketDelivery.setSelected(draft.instantUnitMarketDelivery);
            chkMothballUnitMarketDeliveries.setSelected(draft.mothballUnitMarketDeliveries);
            chkUnitMarketReportRefresh.setSelected(draft.unitMarketReportRefresh);
      }

      private void updateContractMarketControlsFromDraft() {
            if (!contractMarketPageCreated || draft == null) {
                  return;
            }

            comboContractMarketMethod.setSelectedItem(draft.contractMarketMethod);
            spnContractSearchRadius.setValue(draft.contractSearchRadius);
            chkVariableContractLength.setSelected(draft.variableContractLength);
            chkUseTwoWayPay.setSelected(draft.useTwoWayPay);
            chkUseCamOpsSalvage.setSelected(draft.useCamOpsSalvage);
            chkUseRiskySalvage.setSelected(draft.useRiskySalvage);
            chkEnableSalvageFlagByDefault.setSelected(draft.enableSalvageFlagByDefault);
            chkUseDynamicDifficulty.setSelected(draft.useDynamicDifficulty);
            chkUseBolsterContractSkill.setSelected(draft.useBolsterContractSkill);
            chkContractMarketReportRefresh.setSelected(draft.contractMarketReportRefresh);
            spnContractMaxSalvagePercentage.setValue(draft.contractMaxSalvagePercentage);
            spnDropShipBonusPercentage.setValue(draft.dropShipBonusPercentage);
            spnPityContracts.setValue(draft.pityContracts);
            if (draft.equipmentContractBase) {
                  btnContractEquipment.setSelected(true);
            } else {
                  btnContractPersonnel.setSelected(true);
            }
            spnEquipPercent.setValue(draft.equipmentContractPercent);
            chkUseAlternatePaymentMode.setSelected(draft.useAlternatePaymentMode);
            chkUseDiminishingContractPay.setSelected(draft.useDiminishingContractPay);
            chkEquipContractSaleValue.setSelected(draft.equipmentContractSaleValue);
            spnDropShipPercent.setValue(draft.dropShipContractPercent);
            spnJumpShipPercent.setValue(draft.jumpShipContractPercent);
            spnWarShipPercent.setValue(draft.warShipContractPercent);
            useInfantryDoseNotCountBox.setSelected(draft.infantryDontCount);
            chkMercSizeLimited.setSelected(draft.mercSizeLimited);
            chkBLCSaleValue.setSelected(draft.blcSaleValue);
            chkOverageRepaymentInFinalPayment.setSelected(draft.overageRepaymentInFinalPayment);
      }

    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

            updateDraftFromCreatedControls();

        // Personnel Market
            PersonnelMarketStyle selectedPersonnelMarketStyle = draft.personnelMarketStyle;
        if (selectedPersonnelMarketStyle != null) {
            PersonnelMarketStyle originalPersonnelMarketStyle = options.getPersonnelMarketStyle();
            if (selectedPersonnelMarketStyle != originalPersonnelMarketStyle) {
                NewPersonnelMarket replacementMarket = switch (selectedPersonnelMarketStyle) {
                    case PERSONNEL_MARKET_DISABLED -> new NewPersonnelMarket();
                    case MEKHQ -> new PersonnelMarketMekHQ();
                    case CAMPAIGN_OPERATIONS_REVISED -> new PersonnelMarketCamOpsRevised();
                    case CAMPAIGN_OPERATIONS_STRICT -> new PersonnelMarketCamOpsStrict();
                };
                replacementMarket.setCampaign(campaign);
                campaign.setNewPersonnelMarket(replacementMarket);
            }
                  options.setPersonnelMarketStyle(selectedPersonnelMarketStyle);
        }

            options.setPersonnelMarketName(draft.personnelMarketName);
            if (Objects.equals(draft.personnelMarketName, "Campaign Ops")) {
            campaign.getPersonnelMarket().setPaidRecruitment(false);
        }
            options.setPersonnelMarketDylansWeight(draft.personnelMarketDylansWeight);
            options.setUsePersonnelHireHiringHallOnly(draft.usePersonnelHireHiringHallOnly);
            options.setPersonnelMarketReportRefresh(draft.personnelMarketReportRefresh);
            options.getPersonnelMarketRandomRemovalTargets().putAll(draft.personnelMarketRandomRemovalTargets);

        // Unit Market
            options.setUnitMarketMethod(draft.unitMarketMethod);
            options.setUnitMarketRegionalMekVariations(draft.unitMarketRegionalMekVariations);
            options.setUnitMarketArtilleryUnitChance(draft.unitMarketArtilleryUnitChance);
            options.setUnitMarketRarityModifier(draft.unitMarketRarityModifier);
            options.setInstantUnitMarketDelivery(draft.instantUnitMarketDelivery);
            options.setMothballUnitMarketDeliveries(draft.mothballUnitMarketDeliveries);
            options.setUnitMarketReportRefresh(draft.unitMarketReportRefresh);

        // Contract Market
            options.setContractMarketMethod(draft.contractMarketMethod);
            options.setContractSearchRadius(draft.contractSearchRadius);
            options.setVariableContractLength(draft.variableContractLength);
            options.setUseTwoWayPay(draft.useTwoWayPay);
            options.setUseCamOpsSalvage(draft.useCamOpsSalvage);
            options.setUseRiskySalvage(draft.useRiskySalvage);
            options.setEnableSalvageFlagByDefault(draft.enableSalvageFlagByDefault);
            options.setUseDynamicDifficulty(draft.useDynamicDifficulty);
            options.setUseBolsterContractSkill(draft.useBolsterContractSkill);
            options.setContractMarketReportRefresh(draft.contractMarketReportRefresh);
            options.setContractMaxSalvagePercentage(draft.contractMaxSalvagePercentage);
            options.setDropShipBonusPercentage(draft.dropShipBonusPercentage);
            options.setPityContracts(draft.pityContracts);
            options.setEquipmentContractBase(draft.equipmentContractBase);
            options.setEquipmentContractPercent(draft.equipmentContractPercent);
            options.setDropShipContractPercent(draft.dropShipContractPercent);
            options.setJumpShipContractPercent(draft.jumpShipContractPercent);
            options.setWarShipContractPercent(draft.warShipContractPercent);
            options.setUseAlternatePaymentMode(draft.useAlternatePaymentMode);
            options.setUseDiminishingContractPay(draft.useDiminishingContractPay);
            options.setEquipmentContractSaleValue(draft.equipmentContractSaleValue);
            options.setMercSizeLimited(draft.mercSizeLimited);
            options.setBLCSaleValue(draft.blcSaleValue);
            options.setUseInfantryDontCount(draft.infantryDontCount);
            options.setOverageRepaymentInFinalPayment(draft.overageRepaymentInFinalPayment);
    }

      private void updateDraftFromCreatedControls() {
            updateDraftFromPersonnelMarketControls();
            updateDraftFromUnitMarketControls();
            updateDraftFromContractMarketControls();
      }

      private void updateDraftFromPersonnelMarketControls() {
            if (!personnelMarketPageCreated || draft == null) {
                  return;
            }

            draft.personnelMarketStyle = comboPersonnelMarketStyle.getSelectedItem();
            draft.personnelMarketName = comboPersonnelMarketType.getSelectedItem();
            draft.personnelMarketReportRefresh = chkPersonnelMarketReportRefresh.isSelected();
            draft.usePersonnelHireHiringHallOnly = chkUsePersonnelHireHiringHallOnly.isSelected();
            draft.personnelMarketDylansWeight = (double) spnPersonnelMarketDylansWeight.getValue();
            for (final Entry<SkillLevel, JSpinner> entry : spnPersonnelMarketRandomRemovalTargets.entrySet()) {
                  draft.personnelMarketRandomRemovalTargets.put(entry.getKey(), (int) entry.getValue().getValue());
            }
      }

      private void updateDraftFromUnitMarketControls() {
            if (!unitMarketPageCreated || draft == null) {
                  return;
            }

            draft.unitMarketMethod = comboUnitMarketMethod.getSelectedItem();
            draft.unitMarketRegionalMekVariations = chkUnitMarketRegionalMekVariations.isSelected();
            draft.unitMarketArtilleryUnitChance = (int) spnUnitMarketArtilleryUnitChance.getValue();
            draft.unitMarketRarityModifier = (int) spnUnitMarketRarityModifier.getValue();
            draft.instantUnitMarketDelivery = chkInstantUnitMarketDelivery.isSelected();
            draft.mothballUnitMarketDeliveries = chkMothballUnitMarketDeliveries.isSelected();
            draft.unitMarketReportRefresh = chkUnitMarketReportRefresh.isSelected();
      }

      private void updateDraftFromContractMarketControls() {
            if (!contractMarketPageCreated || draft == null) {
                  return;
            }

            draft.contractMarketMethod = comboContractMarketMethod.getSelectedItem();
            draft.contractSearchRadius = (int) spnContractSearchRadius.getValue();
            draft.variableContractLength = chkVariableContractLength.isSelected();
            draft.useTwoWayPay = chkUseTwoWayPay.isSelected();
            draft.useCamOpsSalvage = chkUseCamOpsSalvage.isSelected();
            draft.useRiskySalvage = chkUseRiskySalvage.isSelected();
            draft.enableSalvageFlagByDefault = chkEnableSalvageFlagByDefault.isSelected();
            draft.useDynamicDifficulty = chkUseDynamicDifficulty.isSelected();
            draft.useBolsterContractSkill = chkUseBolsterContractSkill.isSelected();
            draft.contractMarketReportRefresh = chkContractMarketReportRefresh.isSelected();
            draft.contractMaxSalvagePercentage = (int) spnContractMaxSalvagePercentage.getValue();
            draft.dropShipBonusPercentage = (int) spnDropShipBonusPercentage.getValue();
            draft.pityContracts = (int) spnPityContracts.getValue();
            draft.equipmentContractBase = btnContractEquipment.isSelected();
            draft.equipmentContractPercent = (double) spnEquipPercent.getValue();
            draft.dropShipContractPercent = (double) spnDropShipPercent.getValue();
            draft.jumpShipContractPercent = (double) spnJumpShipPercent.getValue();
            draft.warShipContractPercent = (double) spnWarShipPercent.getValue();
            draft.useAlternatePaymentMode = chkUseAlternatePaymentMode.isSelected();
            draft.useDiminishingContractPay = chkUseDiminishingContractPay.isSelected();
            draft.equipmentContractSaleValue = chkEquipContractSaleValue.isSelected();
            draft.mercSizeLimited = chkMercSizeLimited.isSelected();
            draft.blcSaleValue = chkBLCSaleValue.isSelected();
            draft.infantryDontCount = useInfantryDoseNotCountBox.isSelected();
            draft.overageRepaymentInFinalPayment = chkOverageRepaymentInFinalPayment.isSelected();
      }

      private static class MarketsDraft {
            private PersonnelMarketStyle personnelMarketStyle;
            private String personnelMarketName;
            private boolean personnelMarketReportRefresh;
            private boolean usePersonnelHireHiringHallOnly;
            private double personnelMarketDylansWeight;
            private Map<SkillLevel, Integer> personnelMarketRandomRemovalTargets;
            private UnitMarketMethod unitMarketMethod;
            private boolean unitMarketRegionalMekVariations;
            private int unitMarketArtilleryUnitChance;
            private int unitMarketRarityModifier;
            private boolean instantUnitMarketDelivery;
            private boolean mothballUnitMarketDeliveries;
            private boolean unitMarketReportRefresh;
            private ContractMarketMethod contractMarketMethod;
            private int contractSearchRadius;
            private boolean variableContractLength;
            private boolean useTwoWayPay;
            private boolean useCamOpsSalvage;
            private boolean useRiskySalvage;
            private boolean enableSalvageFlagByDefault;
            private boolean useDynamicDifficulty;
            private boolean useBolsterContractSkill;
            private boolean contractMarketReportRefresh;
            private int contractMaxSalvagePercentage;
            private int dropShipBonusPercentage;
            private int pityContracts;
            private boolean equipmentContractBase;
            private double equipmentContractPercent;
            private boolean useAlternatePaymentMode;
            private boolean useDiminishingContractPay;
            private boolean equipmentContractSaleValue;
            private double dropShipContractPercent;
            private double jumpShipContractPercent;
            private double warShipContractPercent;
            private boolean infantryDontCount;
            private boolean mercSizeLimited;
            private boolean blcSaleValue;
            private boolean overageRepaymentInFinalPayment;

            private MarketsDraft(CampaignOptions options) {
                  personnelMarketStyle = options.getPersonnelMarketStyle();
                  personnelMarketName = options.getPersonnelMarketName();
                  personnelMarketReportRefresh = options.isPersonnelMarketReportRefresh();
                  usePersonnelHireHiringHallOnly = options.isUsePersonnelHireHiringHallOnly();
                  personnelMarketDylansWeight = options.getPersonnelMarketDylansWeight();
                  personnelMarketRandomRemovalTargets = new HashMap<>(options.getPersonnelMarketRandomRemovalTargets());
                  unitMarketMethod = options.getUnitMarketMethod();
                  unitMarketRegionalMekVariations = options.isRegionalMekVariations();
                  unitMarketArtilleryUnitChance = options.getUnitMarketArtilleryUnitChance();
                  unitMarketRarityModifier = options.getUnitMarketRarityModifier();
                  instantUnitMarketDelivery = options.isInstantUnitMarketDelivery();
                  mothballUnitMarketDeliveries = options.isMothballUnitMarketDeliveries();
                  unitMarketReportRefresh = options.isUnitMarketReportRefresh();
                  contractMarketMethod = options.getContractMarketMethod();
                  contractSearchRadius = options.getContractSearchRadius();
                  variableContractLength = options.isVariableContractLength();
                  useTwoWayPay = options.isUseTwoWayPay();
                  useCamOpsSalvage = options.isUseCamOpsSalvage();
                  useRiskySalvage = options.isUseRiskySalvage();
                  enableSalvageFlagByDefault = options.isEnableSalvageFlagByDefault();
                  useDynamicDifficulty = options.isUseDynamicDifficulty();
                  useBolsterContractSkill = options.isUseBolsterContractSkill();
                  contractMarketReportRefresh = options.isContractMarketReportRefresh();
                  contractMaxSalvagePercentage = options.getContractMaxSalvagePercentage();
                  dropShipBonusPercentage = options.getDropShipBonusPercentage();
                  pityContracts = options.getPityContracts();
                  equipmentContractBase = options.isEquipmentContractBase();
                  equipmentContractPercent = options.getEquipmentContractPercent();
                  useAlternatePaymentMode = options.isUseAlternatePaymentMode();
                  useDiminishingContractPay = options.isUseDiminishingContractPay();
                  equipmentContractSaleValue = options.isEquipmentContractSaleValue();
                  dropShipContractPercent = options.getDropShipContractPercent();
                  jumpShipContractPercent = options.getJumpShipContractPercent();
                  warShipContractPercent = options.getWarShipContractPercent();
                  infantryDontCount = options.isInfantryDontCount();
                  mercSizeLimited = options.isMercSizeLimited();
                  blcSaleValue = options.isBLCSaleValue();
                  overageRepaymentInFinalPayment = options.isOverageRepaymentInFinalPayment();
            }
      }
}
