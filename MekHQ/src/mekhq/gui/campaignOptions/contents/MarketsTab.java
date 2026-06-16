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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPairedFieldGridPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;

/**
 * The {@code MarketsTab} class represents the campaign options tab related to
 * market settings. This tab provides
 * configurations for three key market areas:
 * <ul>
 * <li><b>Personnel Market</b>: Settings for managing personnel hiring, removal
 * targets, and market types.</li>
 * <li><b>Unit Market</b>: Configurations for purchasing units, special unit
 * chances, rarity modifiers, etc.</li>
 * <li><b>Contract Market</b>: Options for contract acquisition, such as market
 * methods, search radius,
 * and payment settings.</li>
 * </ul>
 * <p>
 * The class initializes the UI components for these three market types and
 * provides methods to
 * load data into the UI or apply changes from the UI to the campaign settings.
 * </p>
 * <p>
 * This class interacts with {@link CampaignOptions} to retrieve or update the
 * persistent campaign settings.
 * It also utilizes Swing components for building the UI.
 * </p>
 */
public class MarketsTab {
      private static final int FORM_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
      private static final int FORM_CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
      private static final int FORM_LABEL_CONTROL_GAP = 12;
      private static final int GRID_CONTROL_COLUMN_WIDTH = 100;
      private static final int CHECKBOX_GRID_COLUMNS = 2;
      private static final int REMOVAL_TARGET_GRID_COLUMNS = 2;
      private static final int CONTRACT_PAY_OPTION_INDENT = 24;
      // The contract-pay option panels are indented under their radio-button headers,
      // which would otherwise push their
      // control column right of the Market Rules section's control column. Shrinking
      // their label column by the indent
      // keeps the indent (the visual nesting) while landing the control column at the
      // same x as Market Rules.
      private static final int CONTRACT_PAY_LABEL_COLUMN_WIDTH = FORM_LABEL_COLUMN_WIDTH - CONTRACT_PAY_OPTION_INDENT;

      private final Campaign campaign;
      private final CampaignOptions campaignOptions;
      private MarketsOptionsModel model;
      private boolean personnelMarketPageCreated;
      private boolean unitMarketPageCreated;
      private boolean contractMarketPageCreated;

      // start Personnel Market
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
      // end Personnel Market

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
      // end Unit Market

      // start Contract Market
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
      private JCheckBox useInfantryDoseNotCountBox;
      private JCheckBox chkBLCSaleValue;
      private JCheckBox chkOverageRepaymentInFinalPayment;
      // end Contract Market

      /**
       * Constructs a {@code MarketsTab} with the provided campaign. Initializes the
       * market configuration options based on
       * the settings of the given {@link Campaign}.
       *
       * @param campaign The {@link Campaign} associated with this market tab. This
       *                 campaign is used to retrieve and
       *                 modify {@link CampaignOptions}.
       */
      public MarketsTab(@Nonnull Campaign campaign) {
            this.campaign = campaign;
            this.campaignOptions = campaign.getCampaignOptions();

            initialize();
            loadValuesFromCampaignOptions();
      }

      /**
       * Initializes the market-related options tabs by setting up configurations for
       * the Personnel Market, Unit Market,
       * and Contract Market.
       * <p>
       * This method is invoked internally within the constructor to prepare the
       * various market configurations for use in
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
       * This includes setting up labels, combo boxes for selecting the personnel
       * market type, checkboxes for additional
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
       * Retrieves the available personnel market type options for display in a combo
       * box.
       * <p>
       * These types are fetched from the {@link PersonnelMarketServiceManager} and
       * represent the available personnel
       * market methods configured for the campaign.
       *
       * @return A {@link DefaultComboBoxModel} containing the personnel market type
       *         options.
       */
      @Deprecated(since = "0.50.06")
      private static DefaultComboBoxModel<String> getPersonnelMarketTypeOptions() {
            final DefaultComboBoxModel<String> personnelMarketTypeModel = new DefaultComboBoxModel<>();
            for (final PersonnelMarketMethod method : PersonnelMarketServiceManager.getInstance()
                        .getAllServices(true)) {
                  personnelMarketTypeModel.addElement(method.getModuleName());
            }
            return personnelMarketTypeModel;
      }

      /**
       * Creates and returns the JPanel representing the Personnel Market
       * configuration tab.
       * <p>
       * This tab includes general personnel market settings, as well as removal
       * target configuration options for various
       * skill levels.
       *
       * @return A {@link JPanel} for the Personnel Market configuration tab.
       */
      public @Nonnull JPanel createPersonnelMarketTab() {
            // Header
            String imageAddress = getImageDirectory() + "logo_st_ives_compact.png";
            personnelMarketHeader = new CampaignOptionsHeaderPanel("PersonnelMarketTab", imageAddress);

            // Contents
            pnlPersonnelMarketGeneralOptions = createPersonnelMarketGeneralOptionsPanel();
            pnlRemovalTargets = createPersonnelMarketRemovalOptionsPanel();

            final JPanel panel = CampaignOptionsPagePanel.builder("PersonnelMarketTab", "PersonnelMarketTab",
                        imageAddress)
                        .header(personnelMarketHeader)
                        .quote("personnelMarketTab")
                        .section("lblPersonnelMarketGeneralOptionsPanel.text",
                                    "lblPersonnelMarketGeneralOptionsPanel.summary",
                                    pnlPersonnelMarketGeneralOptions)
                        .section("lblPersonnelMarketRemovalOptionsPanel.text",
                                    "lblPersonnelMarketRemovalOptionsPanel.summary",
                                    pnlRemovalTargets,
                                    getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                        .build();

            personnelMarketPageCreated = true;
            updatePersonnelMarketControlsFromModel();

            return panel;
      }

      /**
       * Builds the general options panel for the Personnel Market tab, which includes
       * settings such as the personnel
       * market type, Dylan's weight, and options like report refresh toggles.
       * <p>
       * These components are laid out into a panel and returned for use in the UI.
       *
       * @return A {@link JPanel} representing the general options within the
       *         Personnel Market tab.
       */
      private @Nonnull JPanel createPersonnelMarketGeneralOptionsPanel() {
            // Contents
            lblPersonnelMarketStyle = new CampaignOptionsLabel("PersonnelMarketStyle",
                        getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                                    CampaignOptionFlag.RECOMMENDED));
            lblPersonnelMarketStyle
                        .addMouseListener(createTipPanelUpdater(personnelMarketHeader, "PersonnelMarketStyle"));
            comboPersonnelMarketStyle.addMouseListener(createTipPanelUpdater(personnelMarketHeader,
                        "PersonnelMarketStyle"));

            lblPersonnelMarketType = new CampaignOptionsLabel("PersonnelMarketType");
            lblPersonnelMarketType
                        .addMouseListener(createTipPanelUpdater(personnelMarketHeader, "PersonnelMarketType"));
            comboPersonnelMarketType = new MMComboBox<>("comboPersonnelMarketType", getPersonnelMarketTypeOptions());
            comboPersonnelMarketType
                        .addMouseListener(createTipPanelUpdater(personnelMarketHeader, "PersonnelMarketType"));

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
            final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PersonnelMarketGeneralOptionsPanel",
                        FORM_LABEL_COLUMN_WIDTH,
                        FORM_CONTROL_COLUMN_WIDTH);
            panel.addRow(lblPersonnelMarketStyle, comboPersonnelMarketStyle);
            panel.addRow(lblPersonnelMarketType, comboPersonnelMarketType);
            panel.addRow(lblPersonnelMarketDylansWeight, spnPersonnelMarketDylansWeight);
            panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                        chkPersonnelMarketReportRefresh,
                        chkUsePersonnelHireHiringHallOnly);

            return panel;
      }

      /**
       * Creates and configures the removal options panel for the Personnel Market
       * tab.
       * <p>
       * This panel includes settings for removal targets, which are based on various
       * {@link SkillLevel} entries. Each
       * skill level configuration includes both a label and an associated spinner for
       * setting values.
       *
       * @return A {@link JPanel} containing removal options for the Personnel Market.
       */
      private @Nonnull JPanel createPersonnelMarketRemovalOptionsPanel() {
            // Contents
            for (final SkillLevel skillLevel : Skills.SKILL_LEVELS) {
                  final JLabel jLabel = new JLabel(skillLevel.toString());
                  lblPersonnelMarketRandomRemovalTargets.put(skillLevel, jLabel);

                  final JSpinner jSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));

                  DefaultEditor editor = (DefaultEditor) jSpinner.getEditor();
                  editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
                  CampaignOptionsSpinner.installSelectAllOnFocus(jSpinner);

                  spnPersonnelMarketRandomRemovalTargets.put(skillLevel, jSpinner);
            }

            // Layout the Panels
            //
            // Use the same column geometry as the two-column sections (label width + gap, then control width) so the
            // grid columns line up with the General section above it. A compact grid control width keeps the spinners
            // tight while the wider pair widths keep the section broad enough that the section title stays on one line.
            final List<JComponent> labels = new ArrayList<>();
            final List<JComponent> controls = new ArrayList<>();
            for (SkillLevel skillLevel : Skills.SKILL_LEVELS) {
                  labels.add(lblPersonnelMarketRandomRemovalTargets.get(skillLevel));
                  controls.add(spnPersonnelMarketRandomRemovalTargets.get(skillLevel));
            }

            return createPairedFieldGridPanel("PersonnelMarketRemovalOptionsPanel",
                        labels.toArray(new JComponent[0]),
                        controls.toArray(new JComponent[0]),
                        REMOVAL_TARGET_GRID_COLUMNS,
                        GRID_CONTROL_COLUMN_WIDTH);
      }

      /**
       * Creates a dense paired-field grid whose columns line up with the two-column form sections. The first column pair
       * reserves the label-column width plus the label/control gap, and following pairs reserve the control-column width,
       * matching the geometry used by {@link CampaignOptionsFormPanel#addRow}.
       *
       * @param name         the internal panel name
       * @param labels       the labels, one per control
       * @param controls     the controls, one per label
       * @param columnCount  the number of label/control pairs per row
       * @param controlWidth the minimum width of each control within its pair
       *
       * @return the assembled grid panel
       */
      private @Nonnull JPanel createPairedFieldGridPanel(String name, JComponent[] labels, JComponent[] controls,
                  int columnCount, int controlWidth) {
            final CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel(name,
                        FORM_LABEL_COLUMN_WIDTH + FORM_LABEL_CONTROL_GAP,
                        FORM_CONTROL_COLUMN_WIDTH,
                        controlWidth,
                        columnCount);
            panel.addPairs(labels, controls);
            return panel;
      }

      /**
       * Initializes the settings and UI components related to the Unit Market tab.
       * <p>
       * This includes various elements such as labels, combo boxes, checkboxes, and
       * spinners for settings like unit
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
       * Creates and returns the JPanel representing the Unit Market configuration
       * tab.
       * <p>
       * This tab includes options such as unit market methods, rarity modifiers,
       * special unit change settings, and more.
       *
       * @return A {@link JPanel} for the Unit Market configuration tab.
       */
      public @Nonnull JPanel createUnitMarketTab() {
            // Header
            // start Unit Market
            String imageAddress = getImageDirectory() + "logo_clan_ice_hellion.png";
            CampaignOptionsHeaderPanel unitMarketHeader = new CampaignOptionsHeaderPanel("UnitMarketTab", imageAddress);

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
            spnUnitMarketArtilleryUnitChance = new CampaignOptionsSpinner("UnitMarketArtilleryUnitChance", 30, 0, 100,
                        1);
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
            chkUnitMarketReportRefresh
                        .addMouseListener(createTipPanelUpdater(unitMarketHeader, "UnitMarketReportRefresh"));

            JPanel generationPanel = createUnitMarketGenerationPanel();
            JPanel deliveryPanel = createUnitMarketDeliveryPanel();

            final JPanel panel = CampaignOptionsPagePanel.builder("UnitMarketTab", "UnitMarketTab", imageAddress)
                        .header(unitMarketHeader)
                        .quote("unitMarketTab")
                        .section("lblUnitMarketGenerationPanel.text",
                                    "lblUnitMarketGenerationPanel.summary",
                                    generationPanel)
                        .section("lblUnitMarketDeliveryPanel.text",
                                    "lblUnitMarketDeliveryPanel.summary",
                                    deliveryPanel)
                        .build();

            unitMarketPageCreated = true;
            updateUnitMarketControlsFromModel();

            return panel;
      }

      private @Nonnull JPanel createUnitMarketGenerationPanel() {
            final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("UnitMarketGenerationPanel",
                        FORM_LABEL_COLUMN_WIDTH,
                        FORM_CONTROL_COLUMN_WIDTH);
            panel.addRow(lblUnitMarketMethod, comboUnitMarketMethod);
            panel.addCheckBox(chkUnitMarketRegionalMekVariations);
            panel.addRow(lblUnitMarketArtilleryUnitChance, spnUnitMarketArtilleryUnitChance);
            panel.addRow(lblUnitMarketRarityModifier, spnUnitMarketRarityModifier);

            return panel;
      }

      private @Nonnull JPanel createUnitMarketDeliveryPanel() {
            final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("UnitMarketDeliveryPanel",
                        FORM_LABEL_COLUMN_WIDTH,
                        FORM_CONTROL_COLUMN_WIDTH);
            panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                        chkInstantUnitMarketDelivery,
                        chkMothballUnitMarketDeliveries,
                        chkUnitMarketReportRefresh);

            return panel;
      }

      /**
       * Initializes the settings and UI components related to the Contract Market.
       * <p>
       * This includes options for contract market methods, payment settings, salvage
       * percentages, and other
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
            chkBLCSaleValue = new JCheckBox();
            chkOverageRepaymentInFinalPayment = new JCheckBox();
      }

      /**
       * Creates and returns the JPanel representing the Contract Market configuration
       * tab.
       * <p>
       * This tab includes settings for configuring various aspects of contract
       * acquisition, such as methods, search
       * radius, payment options, and variable contract length.
       *
       * @return A {@link JPanel} for the Contract Market configuration tab.
       */
      public @Nonnull JPanel createContractMarketTab() {
            // Header
            String imageAddress = getImageDirectory() + "logo_federated_suns.png";
            contractMarketHeader = new CampaignOptionsHeaderPanel("ContractMarketTab", imageAddress);
            // Contents
            pnlContractMarketGeneralOptions = createContractMarketGeneralOptionsPanel();
            pnlContractPay = createContractPayPanel();

            final JPanel panel = CampaignOptionsPagePanel.builder("ContractMarketTab", "ContractMarketTab",
                        imageAddress)
                        .header(contractMarketHeader)
                        .quote("contractMarketTab")
                        .section("lblContractMarketGeneralOptionsPanel.text",
                                    "lblContractMarketGeneralOptionsPanel.summary",
                                    pnlContractMarketGeneralOptions)
                        .section("lblContractPayPanel.text",
                                    "lblContractPayPanel.summary",
                                    pnlContractPay)
                        .build();

            contractMarketPageCreated = true;
            updateContractMarketControlsFromModel();

            return panel;
      }

      /**
       * Builds the general settings panel for the Contract Market tab, which includes
       * options for the contract market
       * method, search radius, salvage percentages, and other general configurations.
       *
       * @return A {@link JPanel} representing general options within the Contract
       *         Market tab.
       */
      private @Nonnull JPanel createContractMarketGeneralOptionsPanel() {
            // Contents
            lblContractMarketMethod = new CampaignOptionsLabel("ContractMarketMethod");
            lblContractMarketMethod
                        .addMouseListener(createTipPanelUpdater(contractMarketHeader, "ContractMarketMethod"));
            comboContractMarketMethod = new MMComboBox<>("comboContractMarketMethod");
            DefaultComboBoxModel<ContractMarketMethod> model = new DefaultComboBoxModel<>(
                        ContractMarketMethod.values());
            model.removeElement(ContractMarketMethod.CAM_OPS);
            comboContractMarketMethod.setModel(model);
            comboContractMarketMethod
                        .addMouseListener(createTipPanelUpdater(contractMarketHeader, "ContractMarketMethod"));

            lblContractSearchRadius = new CampaignOptionsLabel("ContractSearchRadius");
            lblContractSearchRadius
                        .addMouseListener(createTipPanelUpdater(contractMarketHeader, "ContractSearchRadius"));
            spnContractSearchRadius = new CampaignOptionsSpinner("ContractSearchRadius", 300, 1, 2500, 100);
            spnContractSearchRadius
                        .addMouseListener(createTipPanelUpdater(contractMarketHeader, "ContractSearchRadius"));

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
            chkUseDynamicDifficulty
                        .addMouseListener(createTipPanelUpdater(contractMarketHeader, "UseDynamicDifficulty"));

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
            spnContractMaxSalvagePercentage = new CampaignOptionsSpinner("ContractMaxSalvagePercentage", 100, 0, 100,
                        10);
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
            //
            // A normal two-column form: one label/control pair per row, with the checkboxes laid out in the standard
            // two-column checkbox grid. addRow and addCheckBoxGrid both use the same two underlying grid columns, so they
            // line up cleanly within a single form panel.
            final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ContractMarketGeneralOptionsPanel",
                        FORM_LABEL_COLUMN_WIDTH,
                        FORM_CONTROL_COLUMN_WIDTH);
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
       * Creates the panel for configuring payment settings in the Contract Market
       * tab.
       * <p>
       * This panel contains options for configuring equipment-based payment
       * percentages, override repayment rules, and
       * toggles for contract payment methods.
       *
       * @return A {@link JPanel} containing payment configuration settings for the
       *         Contract Market.
       */
      private @Nonnull JPanel createContractPayPanel() {
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

            chkOverageRepaymentInFinalPayment = new CampaignOptionsCheckBox("OverageRepaymentInFinalPayment");
            chkOverageRepaymentInFinalPayment.addMouseListener(createTipPanelUpdater(contractMarketHeader,
                        "OverageRepaymentInFinalPayment"));

            // Layout the Panel
            final CampaignOptionsFormPanel equipmentValuePanel = new CampaignOptionsFormPanel(
                        "ContractPayPanelValuePercent",
                    CONTRACT_PAY_LABEL_COLUMN_WIDTH,
                                FORM_CONTROL_COLUMN_WIDTH);
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
                                FORM_CONTROL_COLUMN_WIDTH);
            personnelPayPanel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                        chkBLCSaleValue,
                        useInfantryDoseNotCountBox,
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

      public void loadValuesFromCampaignOptions() {
            loadValuesFromCampaignOptions(null);
      }

      /**
       * Loads the campaign options from the associated {@link Campaign} into the UI
       * components of the market tabs. This
       * includes personnel, unit, and contract market settings.
       * <p>
       * If no preset options are provided, the current campaign options are loaded.
       *
       * @param presetCampaignOptions A {@link CampaignOptions} object with previously
       *                              configured settings, or
       *                              {@code null} to use the current campaign's
       *                              options.
       */
      public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
            CampaignOptions options = presetCampaignOptions;
            if (presetCampaignOptions == null) {
                  options = this.campaignOptions;
            }

            model = new MarketsOptionsModel(options);
            updateCreatedControlsFromModel();
      }

      public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
            CampaignOptions options = presetCampaignOptions;
            if (presetCampaignOptions == null) {
                  options = this.campaignOptions;
            }

            updateModelFromCreatedControls();
            model.applyTo(campaign, options);
      }

      private void updateCreatedControlsFromModel() {
            updatePersonnelMarketControlsFromModel();
            updateUnitMarketControlsFromModel();
            updateContractMarketControlsFromModel();
      }

      private void updatePersonnelMarketControlsFromModel() {
            if (!personnelMarketPageCreated || model == null) {
                  return;
            }

            comboPersonnelMarketStyle.setSelectedItem(model.personnelMarketStyle);
            comboPersonnelMarketType.setSelectedItem(model.personnelMarketName);
            chkPersonnelMarketReportRefresh.setSelected(model.personnelMarketReportRefresh);
            chkUsePersonnelHireHiringHallOnly.setSelected(model.usePersonnelHireHiringHallOnly);
            spnPersonnelMarketDylansWeight.setValue(model.personnelMarketDylansWeight);
            for (final Entry<SkillLevel, JSpinner> entry : spnPersonnelMarketRandomRemovalTargets.entrySet()) {
                  entry.getValue().setValue(model.personnelMarketRandomRemovalTargets.get(entry.getKey()));
            }
      }

      private void updateUnitMarketControlsFromModel() {
            if (!unitMarketPageCreated || model == null) {
                  return;
            }

            comboUnitMarketMethod.setSelectedItem(model.unitMarketMethod);
            chkUnitMarketRegionalMekVariations.setSelected(model.unitMarketRegionalMekVariations);
            spnUnitMarketArtilleryUnitChance.setValue(model.unitMarketArtilleryUnitChance);
            spnUnitMarketRarityModifier.setValue(model.unitMarketRarityModifier);
            chkInstantUnitMarketDelivery.setSelected(model.instantUnitMarketDelivery);
            chkMothballUnitMarketDeliveries.setSelected(model.mothballUnitMarketDeliveries);
            chkUnitMarketReportRefresh.setSelected(model.unitMarketReportRefresh);
      }

      private void updateContractMarketControlsFromModel() {
            if (!contractMarketPageCreated || model == null) {
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
            useInfantryDoseNotCountBox.setSelected(model.infantryDontCount);
            chkBLCSaleValue.setSelected(model.blcSaleValue);
            chkOverageRepaymentInFinalPayment.setSelected(model.overageRepaymentInFinalPayment);
            updateContractPayEnabledState();
      }

      private void updateModelFromCreatedControls() {
            updateModelFromPersonnelMarketControls();
            updateModelFromUnitMarketControls();
            updateModelFromContractMarketControls();
      }

      private void updateModelFromPersonnelMarketControls() {
            if (!personnelMarketPageCreated || model == null) {
                  return;
            }

            model.personnelMarketStyle = comboPersonnelMarketStyle.getSelectedItem();
            model.personnelMarketName = comboPersonnelMarketType.getSelectedItem();
            model.personnelMarketReportRefresh = chkPersonnelMarketReportRefresh.isSelected();
            model.usePersonnelHireHiringHallOnly = chkUsePersonnelHireHiringHallOnly.isSelected();
            model.personnelMarketDylansWeight = (double) spnPersonnelMarketDylansWeight.getValue();
            for (final Entry<SkillLevel, JSpinner> entry : spnPersonnelMarketRandomRemovalTargets.entrySet()) {
                  model.personnelMarketRandomRemovalTargets.put(entry.getKey(), (int) entry.getValue().getValue());
            }
      }

      private void updateModelFromUnitMarketControls() {
            if (!unitMarketPageCreated || model == null) {
                  return;
            }

            model.unitMarketMethod = comboUnitMarketMethod.getSelectedItem();
            model.unitMarketRegionalMekVariations = chkUnitMarketRegionalMekVariations.isSelected();
            model.unitMarketArtilleryUnitChance = (int) spnUnitMarketArtilleryUnitChance.getValue();
            model.unitMarketRarityModifier = (int) spnUnitMarketRarityModifier.getValue();
            model.instantUnitMarketDelivery = chkInstantUnitMarketDelivery.isSelected();
            model.mothballUnitMarketDeliveries = chkMothballUnitMarketDeliveries.isSelected();
            model.unitMarketReportRefresh = chkUnitMarketReportRefresh.isSelected();
      }

      private void updateModelFromContractMarketControls() {
            if (!contractMarketPageCreated || model == null) {
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
            model.infantryDontCount = useInfantryDoseNotCountBox.isSelected();
            model.overageRepaymentInFinalPayment = chkOverageRepaymentInFinalPayment.isSelected();
      }

}
