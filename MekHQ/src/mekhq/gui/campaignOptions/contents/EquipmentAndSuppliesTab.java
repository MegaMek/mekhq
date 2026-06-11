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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsModifierTablePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;

/**
 * The {@code EquipmentAndSuppliesTab} class represents a graphical user interface (GUI) tab containing various options
 * and settings related to equipment and supplies in a campaign simulation. This class is responsible for building and
 * managing multiple sub-tabs and panels for customization purposes, including acquisition settings, delivery settings,
 * planetary acquisition settings, and more. It also provides methods to initialize, create, and manage these different
 * components.
 * <p>
 * Fields in this class include labels, spinners, combo boxes, checkboxes, and panels used for displaying and managing
 * options in the tab. They allow the user to configure various parameters like transit times, penalties, acquisition
 * limits, faction-specific settings, and modifiers to influence game mechanics. Multiple constants are defined for
 * units of time, representing days, weeks, and months, among others.
 * <p>
 * The class includes initialization methods for different sections of the tab, as well as methods to create panels for
 * specific functionality. Utility methods are also provided for configuring spinners and combo boxes or formatting
 * options and labels.
 */
public class EquipmentAndSuppliesTab {
    private static final int EQUIPMENT_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int EQUIPMENT_CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int AUTO_LOGISTICS_LABEL_COLUMN_WIDTH = 190;
    private static final int AUTO_LOGISTICS_CONTROL_COLUMN_WIDTH = 90;
    private static final int AUTO_LOGISTICS_PAIRS_PER_ROW = 2;
    private static final int MODIFIER_ROW_LABEL_COLUMN_WIDTH = 120;
    private static final int MODIFIER_CONTROL_COLUMN_WIDTH = 104;

    /**
     * Label-column width for the Acquisition tab's single-control sections
     * (Acquisitions, Deliveries). It is computed
     * at runtime from the AutoLogistics grid (see
     * {@link #createAutoLogisticsPanel()}) so those sections' control
     * column lines up with the grid's second label column. Until then it falls back
     * to the standard label width.
     */
    private int acquisitionSectionLabelWidth = EQUIPMENT_LABEL_COLUMN_WIDTH;

    private final CampaignOptions campaignOptions;
    private EquipmentAndSuppliesOptionsModel model;
    private boolean acquisitionPageCreated;
    private boolean planetaryAcquisitionPageCreated;
    private boolean techLimitsPageCreated;

    //start Acquisition Tab
    private CampaignOptionsHeaderPanel acquisitionHeader;
    private JPanel pnlAcquisitions;
    private JLabel lblChoiceAcquireSkill;
    private MMComboBox<AcquisitionsType> choiceAcquireSkill;
    private JCheckBox chkUseFunctionalAppraisal;
    private JLabel lblAcquireClanPenalty;
    private JLabel lblProcurementPersonnelPick;
    private MMComboBox<String> cboProcurementPersonnelPick;
    private JSpinner spnAcquireClanPenalty;
    private JLabel lblAcquireIsPenalty;
    private JSpinner spnAcquireIsPenalty;
    private JLabel lblAcquireWaitingPeriod;
    private JSpinner spnAcquireWaitingPeriod;
    private JLabel lblMaxAcquisitions;
    private JSpinner spnMaxAcquisitions;
    //end Acquisition Tab

    //start autoLogistics Tab
    private JPanel pnlAutoLogistics;
    private JLabel lblAutoLogisticsHeatSink;
    private JSpinner spnAutoLogisticsHeatSink;
    private JLabel lblAutoLogisticsWeapons;
    private JSpinner spnAutoLogisticsWeapons;
    private JLabel lblAutoLogisticsMekHead;
    private JSpinner spnAutoLogisticsMekHead;
    private JLabel lblAutoLogisticsMekLocation;
    private JSpinner spnAutoLogisticsMekLocation;
    private JLabel lblAutoLogisticsNonRepairableLocation;
    private JSpinner spnAutoLogisticsNonRepairableLocation;
    private JLabel lblAutoLogisticsArmor;
    private JSpinner spnAutoLogisticsArmor;
    private JLabel lblAutoLogisticsAmmunition;
    private JSpinner spnAutoLogisticsAmmunition;
    private JLabel lblAutoLogisticsActuators;
    private JSpinner spnAutoLogisticsActuators;
    private JLabel lblAutoLogisticsJumpJets;
    private JSpinner spnAutoLogisticsJumpJets;
    private JLabel lblAutoLogisticsEngines;
    private JSpinner spnAutoLogisticsEngines;
    private JLabel lblAutoLogisticsOther;
    private JSpinner spnAutoLogisticsOther;
    //end autoLogistics Tab

    //start Delivery Tab
    private JLabel lblTransitTimeUnits;
    private MMComboBox<String> choiceTransitTimeUnits;
    private static final int TRANSIT_UNIT_DAY = 0;
    private static final int TRANSIT_UNIT_WEEK = 1;
    private static final int TRANSIT_UNIT_MONTH = 2;
    private static final int TRANSIT_UNIT_NUM = 3;
    private JCheckBox chkNoDeliveriesInTransit;
    //end Delivery Tab

    //start Planetary Acquisition Tab
    private CampaignOptionsHeaderPanel planetaryAcquisitionHeader;
    private JCheckBox usePlanetaryAcquisitions;
    private JLabel lblMaxJumpPlanetaryAcquisitions;
    private JSpinner spnMaxJumpPlanetaryAcquisitions;
    private JLabel lblPlanetaryAcquisitionsFactionLimits;
    private MMComboBox<PlanetaryAcquisitionFactionLimit> comboPlanetaryAcquisitionsFactionLimits;
    private JCheckBox disallowClanPartsFromIS;
    private JCheckBox disallowPlanetaryAcquisitionClanCrossover;
    private JLabel lblPenaltyClanPartsFromIS;
    private JSpinner spnPenaltyClanPartsFromIS;
    private JCheckBox usePlanetaryAcquisitionsVerbose;

    private JSpinner[] spnPlanetAcquireTechBonus;
    private JSpinner[] spnPlanetAcquireIndustryBonus;
    private JSpinner[] spnPlanetAcquireOutputBonus;
    //end Planetary Acquisition Tab

    private JCheckBox limitByYearBox;
    private JCheckBox disallowExtinctStuffBox;
    private JCheckBox allowClanPurchasesBox;
    private JCheckBox allowISPurchasesBox;
    private JCheckBox allowCanonOnlyBox;
    private JCheckBox allowCanonRefitOnlyBox;
    private JLabel lblChoiceTechLevel;
    private MMComboBox<String> choiceTechLevel;
    private JCheckBox variableTechLevelBox;
    private JCheckBox useAmmoByTypeBox;
    //end Tech Limits Tab

    /**
     * Constructs the EquipmentAndSuppliesTab with the given campaign options.
     *
     * @param campaignOptions the {@link CampaignOptions} object containing configuration settings for the campaign
     */
    public EquipmentAndSuppliesTab(@Nonnull CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
        loadValuesFromCampaignOptions();
    }

    /**
     * Initializes the EquipmentAndSuppliesTab by configuring its various components and panels. This includes setting
     * up the acquisitions tab, delivery tab, planetary acquisitions tab, and tech limits tab.
     */
    void initialize() {
        initializeAcquisitionTab();
        initializeAutoLogisticsTab();
        initializeDelivery();
        initializePlanetaryAcquisitionsTab();
        initializeTechLimitsTab();
    }

    /**
     * Initializes the components and configurations for the Planetary Acquisitions tab in the EquipmentAndSuppliesTab.
     * This includes setting up options, labels, spinners, combo boxes, checkboxes, and panels related to planetary
     * acquisitions.
     * <p>
     * The method sets up the following:
     * <li> Configuration options for planetary acquisitions such as enabling/disabling planetary acquisitions,
     * setting faction limits, and managing clan/Inner Sphere specific rules.</li>
     * <li> Panels and components for modifiers, including technology bonuses, industry bonuses,
     * and output bonuses.</li>
     */
    private void initializePlanetaryAcquisitionsTab() {
        // Options
        usePlanetaryAcquisitions = new JCheckBox();

        lblMaxJumpPlanetaryAcquisitions = new JLabel();
        spnMaxJumpPlanetaryAcquisitions = new JSpinner();

        lblPlanetaryAcquisitionsFactionLimits = new JLabel();
        comboPlanetaryAcquisitionsFactionLimits = new MMComboBox<>("comboPlanetaryAcquisitionsFactionLimits",
              PlanetaryAcquisitionFactionLimit.values());

        disallowPlanetaryAcquisitionClanCrossover = new JCheckBox();

        disallowClanPartsFromIS = new JCheckBox();

        lblPenaltyClanPartsFromIS = new JLabel();
        spnPenaltyClanPartsFromIS = new JSpinner();

        usePlanetaryAcquisitionsVerbose = new JCheckBox();

        // Modifiers
        spnPlanetAcquireTechBonus = new JSpinner[PlanetarySophistication.values().length];
        spnPlanetAcquireIndustryBonus = new JSpinner[PlanetaryRating.values().length];
        spnPlanetAcquireOutputBonus = new JSpinner[PlanetaryRating.values().length];
    }

    /**
     * Initializes the components and configurations for the delivery tab within the EquipmentAndSuppliesTab. This
     * method sets up panels, labels, spinners, and combo boxes required for managing delivery-related settings and
     * options.
     * <p>
     * The setup includes:
     * <li> The main delivery panel.</li>
     * <li> Spinners for numeric inputs such as transit time settings and acquisition modifiers.</li>
     * <li> Labels for providing descriptions for components.</li>
     * <li> Combo boxes for selecting unit options related to transit times and acquisitions.</li>
     */
    private void initializeDelivery() {
        lblTransitTimeUnits = new JLabel();
        choiceTransitTimeUnits = new MMComboBox<>("choiceTransitTimeUnits", getTransitUnitOptions());
        chkNoDeliveriesInTransit = new JCheckBox();
    }

    /**
     * Initializes the components and settings for the acquisitions tab in the EquipmentAndSuppliesTab. This method sets
     * up the GUI components required for configuring acquisition-related options, including panels, labels, spinners,
     * combo boxes, and checkboxes.
     */
    private void initializeAcquisitionTab() {
        pnlAcquisitions = new JPanel();
        lblChoiceAcquireSkill = new JLabel();
        choiceAcquireSkill = new MMComboBox<>("choiceAcquireSkill", AcquisitionsType.values());

        chkUseFunctionalAppraisal = new JCheckBox();

        lblProcurementPersonnelPick = new JLabel();
        cboProcurementPersonnelPick = new MMComboBox<>("procurementPersonnelPick",
              buildProcurementPersonnelPickComboOptions());

        lblAcquireClanPenalty = new JLabel();
        spnAcquireClanPenalty = new JSpinner();

        lblAcquireIsPenalty = new JLabel();
        spnAcquireIsPenalty = new JSpinner();

        lblAcquireWaitingPeriod = new JLabel();
        spnAcquireWaitingPeriod = new JSpinner();

        lblMaxAcquisitions = new JLabel();
        spnMaxAcquisitions = new JSpinner();
    }

    /**
     * Initializes the components and settings for the autoLogistics tab in the EquipmentAndSuppliesTab. This method
     * sets up the GUI components required for configuring acquisition-related options, including panels, labels,
     * spinners, combo boxes, and checkboxes.
     */
    private void initializeAutoLogisticsTab() {
        pnlAutoLogistics = new JPanel();
        lblAutoLogisticsHeatSink = new JLabel();
        spnAutoLogisticsHeatSink = new JSpinner();
        lblAutoLogisticsWeapons = new JLabel();
        spnAutoLogisticsWeapons = new JSpinner();
        lblAutoLogisticsMekHead = new JLabel();
        spnAutoLogisticsMekHead = new JSpinner();
        lblAutoLogisticsMekLocation = new JLabel();
        spnAutoLogisticsMekLocation = new JSpinner();
        lblAutoLogisticsNonRepairableLocation = new JLabel();
        spnAutoLogisticsNonRepairableLocation = new JSpinner();
        lblAutoLogisticsArmor = new JLabel();
        spnAutoLogisticsArmor = new JSpinner();
        lblAutoLogisticsAmmunition = new JLabel();
        spnAutoLogisticsAmmunition = new JSpinner();
        lblAutoLogisticsActuators = new JLabel();
        spnAutoLogisticsActuators = new JSpinner();
        lblAutoLogisticsJumpJets = new JLabel();
        spnAutoLogisticsJumpJets = new JSpinner();
        lblAutoLogisticsEngines = new JLabel();
        spnAutoLogisticsEngines = new JSpinner();
        lblAutoLogisticsOther = new JLabel();
        spnAutoLogisticsOther = new JSpinner();
    }

    /**
     * Initializes the components and settings for the Tech Limits tab in the EquipmentAndSuppliesTab. This method sets
     * up a series of checkboxes, labels, and combo boxes to configure technology-related limits and options.
     */
    private void initializeTechLimitsTab() {
        limitByYearBox = new JCheckBox();
        disallowExtinctStuffBox = new JCheckBox();
        allowClanPurchasesBox = new JCheckBox();
        allowISPurchasesBox = new JCheckBox();
        allowCanonOnlyBox = new JCheckBox();
        allowCanonRefitOnlyBox = new JCheckBox();
        lblChoiceTechLevel = new JLabel();
        choiceTechLevel = new MMComboBox<>("choiceTechLevel", getMaximumTechLevelOptions());
        variableTechLevelBox = new JCheckBox();
        useAmmoByTypeBox = new JCheckBox();
    }

    /**
     * Creates and configures the acquisition tab panel for the user interface. This method initializes and organizes
     * the components such as the header, acquisition panel, and delivery panel, and then returns the fully constructed
     * acquisition tab panel.
     *
     * @return A {@code JPanel} instance representing the complete acquisition tab.
     */
    public @Nonnull JPanel createAcquisitionTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_cloud_cobra.png";
        acquisitionHeader = new CampaignOptionsHeaderPanel("AcquisitionTab", imageAddress);

        // Build AutoLogistics first: it measures its grid and sets
        // acquisitionSectionLabelWidth, which the
        // Acquisitions and Deliveries sections then use so their control column aligns
        // with the grid's second column.
        pnlAutoLogistics = createAutoLogisticsPanel();
        pnlAcquisitions = createAcquisitionPanel();
        JPanel pnlDelivery = createDeliveryPanel();
        acquisitionPageCreated = true;
        updateAcquisitionControlsFromModel();

        return CampaignOptionsPagePanel.builder("AcquisitionTab", "AcquisitionTab", imageAddress)
                .header(acquisitionHeader)
                .quote("acquisitionTab")
                .section("lblAcquisitionPanel.text",
                        "lblAcquisitionPanel.summary",
                        pnlAcquisitions)
                .section("lblDeliveryPanel.text",
                        "lblDeliveryPanel.summary",
                        pnlDelivery)
                .section("lblAutoLogisticsPanel.text",
                        "lblAutoLogisticsPanel.summary",
                        pnlAutoLogistics)
                .build();
    }

    /**
     * Creates and returns a {@code JPanel} for configuring acquisition-related options. This panel includes various
     * components such as labels, checkboxes, and spinners to allow users to set values for acquisition settings,
     * including penalties, waiting periods, maximum acquisitions, and stock percentages.
     *
     * @return A {@code JPanel} populated with acquisition configuration components and their layout.
     */
    private @Nonnull JPanel createAcquisitionPanel() {
        // Content
        lblChoiceAcquireSkill = new CampaignOptionsLabel("ChoiceAcquireSkill");
        lblChoiceAcquireSkill.addMouseListener(createTipPanelUpdater(acquisitionHeader, "ChoiceAcquireSkill"));

        chkUseFunctionalAppraisal = new CampaignOptionsCheckBox("UseFunctionalAppraisal",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseFunctionalAppraisal.addMouseListener(createTipPanelUpdater(acquisitionHeader, "UseFunctionalAppraisal"));

        lblProcurementPersonnelPick = new CampaignOptionsLabel("ProcurementPersonnelPick");
        lblProcurementPersonnelPick.addMouseListener(createTipPanelUpdater(acquisitionHeader,
              "ProcurementPersonnelPick"));

        lblAcquireClanPenalty = new CampaignOptionsLabel("AcquireClanPenalty");
        lblAcquireClanPenalty.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AcquireClanPenalty"));
        spnAcquireClanPenalty = new CampaignOptionsSpinner("AcquireClanPenalty", 0, 0, 13, 1);
        spnAcquireClanPenalty.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AcquireClanPenalty"));

        lblAcquireIsPenalty = new CampaignOptionsLabel("AcquireISPenalty");
        lblAcquireIsPenalty.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AcquireISPenalty"));
        spnAcquireIsPenalty = new CampaignOptionsSpinner("AcquireISPenalty", 0, 0, 13, 1);
        spnAcquireIsPenalty.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AcquireISPenalty"));

        lblAcquireWaitingPeriod = new CampaignOptionsLabel("AcquireWaitingPeriod");
        lblAcquireWaitingPeriod.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AcquireWaitingPeriod"));
        spnAcquireWaitingPeriod = new CampaignOptionsSpinner("AcquireWaitingPeriod", 1, 1, 365, 1);
        spnAcquireWaitingPeriod.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AcquireWaitingPeriod"));

        lblMaxAcquisitions = new CampaignOptionsLabel("MaxAcquisitions");
        lblMaxAcquisitions.addMouseListener(createTipPanelUpdater(acquisitionHeader, "MaxAcquisitions"));
        spnMaxAcquisitions = new CampaignOptionsSpinner("MaxAcquisitions", 0, 0, 100, 1);
        spnMaxAcquisitions.addMouseListener(createTipPanelUpdater(acquisitionHeader, "MaxAcquisitions"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AcquisitionPanel",
                acquisitionSectionLabelWidth,
                      EQUIPMENT_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblChoiceAcquireSkill, choiceAcquireSkill);
        panel.addCheckBox(chkUseFunctionalAppraisal);
        panel.addRow(lblProcurementPersonnelPick, cboProcurementPersonnelPick);
        panel.addRow(lblAcquireClanPenalty, spnAcquireClanPenalty);
        panel.addRow(lblAcquireIsPenalty, spnAcquireIsPenalty);
        panel.addRow(lblAcquireWaitingPeriod, spnAcquireWaitingPeriod);
        panel.addRow(lblMaxAcquisitions, spnMaxAcquisitions);

        return panel;
    }

    private @Nonnull JPanel createDeliveryPanel() {
        lblTransitTimeUnits = new CampaignOptionsLabel("TransitTimeUnits");
        lblTransitTimeUnits.addMouseListener(createTipPanelUpdater(acquisitionHeader, "TransitTimeUnits"));
        choiceTransitTimeUnits.addMouseListener(createTipPanelUpdater(acquisitionHeader, "TransitTimeUnits"));

        chkNoDeliveriesInTransit = new CampaignOptionsCheckBox("NoDeliveriesInTransit",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkNoDeliveriesInTransit.addMouseListener(createTipPanelUpdater(acquisitionHeader, "NoDeliveriesInTransit"));

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("DeliveryPanel",
                acquisitionSectionLabelWidth,
                      EQUIPMENT_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblTransitTimeUnits, choiceTransitTimeUnits);
        panel.addCheckBox(chkNoDeliveriesInTransit);

        return panel;
    }

    /**
     * Creates and returns a {@code JPanel} for configuring autoLogistics-related options. This panel includes various
     * components such as labels, checkboxes, and spinners to allow users to set values for acquisition settings,
     * including penalties, waiting periods, maximum acquisitions, and stock percentages.
     *
     * @return A {@code JPanel} populated with autoLogistics configuration components and their layout.
     */
    private @Nonnull JPanel createAutoLogisticsPanel() {
        // Content
        lblAutoLogisticsMekHead = new CampaignOptionsLabel("AutoLogisticsMekHead");
        lblAutoLogisticsMekHead.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsMekHead"));
        spnAutoLogisticsMekHead = new CampaignOptionsSpinner("AutoLogisticsMekHead", 200, 0, 10000, 1);
        spnAutoLogisticsMekHead.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsMekHead"));

        lblAutoLogisticsMekLocation = new CampaignOptionsLabel("AutoLogisticsMekLocation");
        lblAutoLogisticsMekLocation.addMouseListener(createTipPanelUpdater(acquisitionHeader,
              "AutoLogisticsMekLocation"));
        spnAutoLogisticsMekLocation = new CampaignOptionsSpinner("AutoLogisticsMekLocation", 100, 0, 10000, 1);
        spnAutoLogisticsMekLocation.addMouseListener(createTipPanelUpdater(acquisitionHeader,
              "AutoLogisticsMekLocation"));

        lblAutoLogisticsNonRepairableLocation = new CampaignOptionsLabel("AutoLogisticsNonRepairableLocation");
        lblAutoLogisticsNonRepairableLocation.addMouseListener(createTipPanelUpdater(acquisitionHeader,
              "AutoLogisticsNonRepairableLocation"));
        spnAutoLogisticsNonRepairableLocation = new CampaignOptionsSpinner("AutoLogisticsNonRepairableLocation",
              0,
              0,
              10000,
              1);
        spnAutoLogisticsNonRepairableLocation.addMouseListener(createTipPanelUpdater(acquisitionHeader,
              "AutoLogisticsNonRepairableLocation"));

        lblAutoLogisticsArmor = new CampaignOptionsLabel("AutoLogisticsArmor");
        lblAutoLogisticsArmor.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsArmor"));
        spnAutoLogisticsArmor = new CampaignOptionsSpinner("AutoLogisticsArmor", 500, 0, 10000, 1);
        spnAutoLogisticsArmor.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsArmor"));

        lblAutoLogisticsAmmunition = new CampaignOptionsLabel("AutoLogisticsAmmunition");
        lblAutoLogisticsAmmunition.addMouseListener(createTipPanelUpdater(acquisitionHeader,
              "AutoLogisticsAmmunition"));
        spnAutoLogisticsAmmunition = new CampaignOptionsSpinner("AutoLogisticsAmmunition", 500, 0, 10000, 1);
        spnAutoLogisticsAmmunition.addMouseListener(createTipPanelUpdater(acquisitionHeader,
              "AutoLogisticsAmmunition"));

        lblAutoLogisticsHeatSink = new CampaignOptionsLabel("AutoLogisticsHeatSink");
        lblAutoLogisticsHeatSink.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsHeatSink"));
        spnAutoLogisticsHeatSink = new CampaignOptionsSpinner("AutoLogisticsHeatSink", 250, 0, 10000, 1);
        spnAutoLogisticsHeatSink.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsHeatSink"));

        lblAutoLogisticsWeapons = new CampaignOptionsLabel("AutoLogisticsWeapons");
        lblAutoLogisticsWeapons.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsWeapons"));
        spnAutoLogisticsWeapons = new CampaignOptionsSpinner("AutoLogisticsWeapons", 50, 0, 10000, 1);
        spnAutoLogisticsWeapons.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsWeapons"));

        lblAutoLogisticsActuators = new CampaignOptionsLabel("AutoLogisticsActuators");
        lblAutoLogisticsActuators.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsActuators"));
        spnAutoLogisticsActuators = new CampaignOptionsSpinner("AutoLogisticsActuators", 250, 0, 10000, 1);
        spnAutoLogisticsActuators.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsActuators"));

        lblAutoLogisticsJumpJets = new CampaignOptionsLabel("AutoLogisticsJumpJets");
        lblAutoLogisticsJumpJets.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsJumpJets"));
        spnAutoLogisticsJumpJets = new CampaignOptionsSpinner("AutoLogisticsJumpJets", 250, 0, 10000, 1);
        spnAutoLogisticsJumpJets.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsJumpJets"));

        lblAutoLogisticsEngines = new CampaignOptionsLabel("AutoLogisticsEngines");
        lblAutoLogisticsEngines.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsEngines"));
        spnAutoLogisticsEngines = new CampaignOptionsSpinner("AutoLogisticsEngines", 250, 0, 10000, 1);
        spnAutoLogisticsEngines.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsEngines"));

        lblAutoLogisticsOther = new CampaignOptionsLabel("AutoLogisticsOther");
        lblAutoLogisticsOther.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsOther"));
        spnAutoLogisticsOther = new CampaignOptionsSpinner("AutoLogisticsOther", 50, 0, 10000, 1);
        spnAutoLogisticsOther.addMouseListener(createTipPanelUpdater(acquisitionHeader, "AutoLogisticsOther"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AutoLogisticsPanel",
              AUTO_LOGISTICS_LABEL_COLUMN_WIDTH,
              AUTO_LOGISTICS_CONTROL_COLUMN_WIDTH);
        panel.addRowGrid(AUTO_LOGISTICS_PAIRS_PER_ROW,
              lblAutoLogisticsMekHead, spnAutoLogisticsMekHead,
              lblAutoLogisticsMekLocation, spnAutoLogisticsMekLocation,
              lblAutoLogisticsNonRepairableLocation, spnAutoLogisticsNonRepairableLocation,
              lblAutoLogisticsHeatSink, spnAutoLogisticsHeatSink,
              lblAutoLogisticsArmor, spnAutoLogisticsArmor,
              lblAutoLogisticsAmmunition, spnAutoLogisticsAmmunition,
              lblAutoLogisticsActuators, spnAutoLogisticsActuators,
              lblAutoLogisticsJumpJets, spnAutoLogisticsJumpJets,
              lblAutoLogisticsEngines, spnAutoLogisticsEngines,
              lblAutoLogisticsWeapons, spnAutoLogisticsWeapons,
              lblAutoLogisticsOther, spnAutoLogisticsOther);

        // Compute where this grid's second label column (the "third column") begins, so
        // the single-control sections
        // can size their label column to match and line their control column up with
        // it. With two pairs per row the
        // left pair occupies columns 0 (label) and 1 (spinner); the second pair's label
        // begins after both, plus the
        // inter-pair gap. addRowGrid floors each component at the configured widths, so
        // we measure the realized
        // (font-scaled) preferred widths of the left pair here, which keeps the
        // alignment correct at any GUI scale.
        int firstColumnLabelWidth = widestPreferredWidth(AUTO_LOGISTICS_LABEL_COLUMN_WIDTH,
                lblAutoLogisticsMekHead,
                lblAutoLogisticsNonRepairableLocation,
                lblAutoLogisticsArmor,
                lblAutoLogisticsActuators,
                lblAutoLogisticsEngines,
                lblAutoLogisticsOther);
        int firstColumnControlWidth = widestPreferredWidth(AUTO_LOGISTICS_CONTROL_COLUMN_WIDTH,
                spnAutoLogisticsMekHead,
                spnAutoLogisticsNonRepairableLocation,
                spnAutoLogisticsArmor,
                spnAutoLogisticsActuators,
                spnAutoLogisticsEngines,
                spnAutoLogisticsOther);
        // The label's own right padding and the grid label's right padding are equal,
        // so they cancel; what remains is
        // the first label column, the first control column, and the inter-pair gap
        // between them.
        acquisitionSectionLabelWidth = firstColumnLabelWidth + firstColumnControlWidth
                + CampaignOptionsFormPanel.GRID_COLUMN_GAP;

        return panel;
    }

    /**
     * Returns the widest preferred width among the given components, but never less
     * than {@code floor}.
     *
     * @param floor      the minimum width to return
     * @param components the components to measure
     *
     * @return the largest of {@code floor} and the components' preferred widths
     */
    private static int widestPreferredWidth(int floor, JComponent... components) {
        int width = floor;
        for (JComponent component : components) {
            width = Math.max(width, component.getPreferredSize().width);
        }
        return width;
    }

    /**
     * Creates and configures the planetary acquisition tab panel in a campaign options interface. The panel includes a
     * header, options, and modifiers section, arranged using layout constraints. Once configured, it is wrapped within
     * a parent panel and returned.
     *
     * @return a {@code JPanel} object representing the planetary acquisition tab with its configured components and
     *       layout.
     */
    public @Nonnull JPanel createPlanetaryAcquisitionTab() {
        // Header
        String imageAddress = getImageDirectory() + "logo_rim_worlds_republic.png";
        planetaryAcquisitionHeader = new CampaignOptionsHeaderPanel("PlanetaryAcquisitionTab", imageAddress);

        // Sub-Panels
        JPanel options = createOptionsPanel();
        JPanel modifiers = createModifiersPanel();
        planetaryAcquisitionPageCreated = true;
        updatePlanetaryAcquisitionControlsFromModel();

        return CampaignOptionsPagePanel.builder("PlanetaryAcquisitionTab", "PlanetaryAcquisitionTab", imageAddress)
                .header(planetaryAcquisitionHeader)
                .quote("planetaryAcquisitionTab")
                .section("lblPlanetaryAcquisitionTab.text",
                        "lblPlanetaryAcquisitionTab.summary",
                        options)
                .section("lblModifiersPanel.text",
                        "lblModifiersPanel.summary",
                        modifiers)
                .build();
    }


    /**
     * Creates and returns a {@code JPanel} containing the components necessary for configuring campaign options related
     * to planetary acquisitions. This panel includes various labels, checkboxes, and spinners for setting and adjusting
     * relevant options.
     *
     * @return a {@code JPanel} containing the campaign options panel for planetary acquisitions.
     */
    private @Nonnull JPanel createOptionsPanel() {
        usePlanetaryAcquisitions = new CampaignOptionsCheckBox("UsePlanetaryAcquisitions",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT, CampaignOptionFlag.RECOMMENDED));
        usePlanetaryAcquisitions.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
              "UsePlanetaryAcquisitions"));

        lblMaxJumpPlanetaryAcquisitions = new CampaignOptionsLabel("MaxJumpPlanetaryAcquisitions");
        lblMaxJumpPlanetaryAcquisitions.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
              "MaxJumpPlanetaryAcquisitions"));
        spnMaxJumpPlanetaryAcquisitions = new CampaignOptionsSpinner("MaxJumpPlanetaryAcquisitions", 2, 0, 5, 1);
        spnMaxJumpPlanetaryAcquisitions.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
              "MaxJumpPlanetaryAcquisitions"));

        lblPlanetaryAcquisitionsFactionLimits = new CampaignOptionsLabel("PlanetaryAcquisitionsFactionLimits");
        lblPlanetaryAcquisitionsFactionLimits.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
              "PlanetaryAcquisitionsFactionLimits"));

        disallowPlanetaryAcquisitionClanCrossover = new CampaignOptionsCheckBox(
              "DisallowPlanetaryAcquisitionClanCrossover");
        disallowPlanetaryAcquisitionClanCrossover.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
              "DisallowPlanetaryAcquisitionClanCrossover"));

        disallowClanPartsFromIS = new CampaignOptionsCheckBox("DisallowClanPartsFromIS");
        disallowClanPartsFromIS.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
              "DisallowClanPartsFromIS"));

        lblPenaltyClanPartsFromIS = new CampaignOptionsLabel("PenaltyClanPartsFromIS");
        lblPenaltyClanPartsFromIS.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
              "PenaltyClanPartsFromIS"));
        spnPenaltyClanPartsFromIS = new CampaignOptionsSpinner("PenaltyClanPartsFromIS", 0, 0, 12, 1);
        spnPenaltyClanPartsFromIS.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
              "PenaltyClanPartsFromIS"));

        usePlanetaryAcquisitionsVerbose = new CampaignOptionsCheckBox("UsePlanetaryAcquisitionsVerbose",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        usePlanetaryAcquisitionsVerbose.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
              "UsePlanetaryAcquisitionsVerbose"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PlanetaryAcquisitionOptionsPanel",
              EQUIPMENT_LABEL_COLUMN_WIDTH,
              EQUIPMENT_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(usePlanetaryAcquisitions);
        panel.addCheckBox(usePlanetaryAcquisitionsVerbose);
        panel.addRow(lblMaxJumpPlanetaryAcquisitions, spnMaxJumpPlanetaryAcquisitions);
        panel.addRow(lblPlanetaryAcquisitionsFactionLimits, comboPlanetaryAcquisitionsFactionLimits);
        panel.addCheckBox(disallowPlanetaryAcquisitionClanCrossover);
        panel.addCheckBox(disallowClanPartsFromIS);
        panel.addRow(lblPenaltyClanPartsFromIS, spnPenaltyClanPartsFromIS);

        return panel;
    }

    private @Nonnull JPanel createModifiersPanel() {
        int i = 0;
        for (PlanetarySophistication ignored : PlanetarySophistication.values()) {
            spnPlanetAcquireTechBonus[i] = createModifierSpinner("TechLabel");
            i++;
        }

        i = 0;
        for (PlanetaryRating ignored : PlanetaryRating.values()) {
            spnPlanetAcquireIndustryBonus[i] = createModifierSpinner("IndustryLabel");
            spnPlanetAcquireOutputBonus[i] = createModifierSpinner("OutputLabel");
            i++;
        }

        final CampaignOptionsModifierTablePanel tablePanel = new CampaignOptionsModifierTablePanel(
              "PlanetaryAcquisitionTabModifiers",
              MODIFIER_ROW_LABEL_COLUMN_WIDTH,
              MODIFIER_CONTROL_COLUMN_WIDTH,
              createModifierColumnHeader("TechLabel"),
              createModifierColumnHeader("IndustryLabel"),
              createModifierColumnHeader("OutputLabel"));

        i = 0;
        for (PlanetarySophistication sophistication : PlanetarySophistication.values()) {
            int ratingIndex = getPlanetaryRatingIndex(sophistication.getName());
            tablePanel.addRow(createModifierRowLabel(sophistication.getName()),
                  spnPlanetAcquireTechBonus[i],
                  ratingIndex >= 0 ? spnPlanetAcquireIndustryBonus[ratingIndex] : null,
                  ratingIndex >= 0 ? spnPlanetAcquireOutputBonus[ratingIndex] : null);
            i++;
        }

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PlanetaryAcquisitionTabModifiersPanel",
              MODIFIER_ROW_LABEL_COLUMN_WIDTH,
              MODIFIER_CONTROL_COLUMN_WIDTH);
        panel.addFullWidthComponent(tablePanel);

        return panel;
    }

    private @Nonnull JLabel createModifierColumnHeader(String name) {
        JLabel label = new CampaignOptionsLabel(name);
        label.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader, name));
        return label;
    }

    private @Nonnull JLabel createModifierRowLabel(String text) {
        JLabel label = new JLabel(String.format("<html>%s</html>", text));
        label.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader, "TechLabel"));
        return label;
    }

    private @Nonnull JSpinner createModifierSpinner(String tipKey) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
        spinner.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader, tipKey));
        CampaignOptionsSpinner.installSelectAllOnFocus(spinner);
        return spinner;
    }

    private int getPlanetaryRatingIndex(String name) {
        for (PlanetaryRating rating : PlanetaryRating.values()) {
            if (rating.getName().equals(name)) {
                return rating.getIndex();
            }
        }

        return -1;
    }

    /**
     * Creates and returns a DefaultComboBoxModel containing the transit unit options.
     *
     * @return a DefaultComboBoxModel<String> populated with transit unit names based on TRANSIT_UNIT_NUM.
     */
    private static DefaultComboBoxModel<String> getTransitUnitOptions() {
        DefaultComboBoxModel<String> transitUnitModel = new DefaultComboBoxModel<>();

        for (int i = 0; i < TRANSIT_UNIT_NUM; i++) {
            transitUnitModel.addElement(getTransitUnitName(i));
        }
        return transitUnitModel;
    }

    /**
     * Retrieves the name of the transit unit based on the provided unit value.
     *
     * @param unit the integer value representing the transit unit (e.g., day, week, month)
     *
     * @return the name of the transit unit as a string, or "ERROR" if the unit is not recognized
     */
    private static String getTransitUnitName(final int unit) {
        return switch (unit) {
            case TRANSIT_UNIT_DAY -> getTextAt(getCampaignOptionsResourceBundle(), "transitUnitNamesDays.text");
            case TRANSIT_UNIT_WEEK -> getTextAt(getCampaignOptionsResourceBundle(), "transitUnitNamesWeeks.text");
            case TRANSIT_UNIT_MONTH -> getTextAt(getCampaignOptionsResourceBundle(), "transitUnitNamesMonths.text");
            default -> "ERROR";
        };
    }

    /**
     * Builds a {@link DefaultComboBoxModel} containing options for all available {@link ProcurementPersonnelPick}
     * values.
     *
     * <p>This method iterates through all the values of the {@link ProcurementPersonnelPick}
     * enumeration and adds their names as string elements to the combo box model. The resulting model can be used to
     * populate a combo box in the user interface, allowing users to select a personnel category for procurement
     * purposes.</p>
     *
     * @return A {@link DefaultComboBoxModel} populated with the names of all {@link ProcurementPersonnelPick} values.
     *
     * @see ProcurementPersonnelPick#values() Retrieves all defined personnel pick options.
     */
    private static DefaultComboBoxModel<String> buildProcurementPersonnelPickComboOptions() {
        DefaultComboBoxModel<String> procurementPersonnelPick = new DefaultComboBoxModel<>();

        for (ProcurementPersonnelPick pick : ProcurementPersonnelPick.values()) {
            procurementPersonnelPick.addElement(pick.toString());
        }

        return procurementPersonnelPick;
    }

    /**
     * Creates and initializes the "Tech Limits" tab panel within a user interface. The tab includes various settings
     * and options related to technical limitations, such as limiting by year, disallowing extinct technologies,
     * allowing faction-specific purchases, enabling canon-only restrictions, setting maximum tech levels, and more. The
     * method arranges the components in a structured layout and constructs the required parent panel.
     *
     * @return the {@code JPanel} representing the "Tech Limits" tab, fully configured with its components and layout.
     */
    public @Nonnull JPanel createTechLimitsTab() {
        // Header
        //start Tech Limits Tab
        String imageAddress = getImageDirectory() + "logo_clan_ghost_bear.png";
        CampaignOptionsHeaderPanel techLimitsHeader = new CampaignOptionsHeaderPanel("TechLimitsTab", imageAddress);

        limitByYearBox = new CampaignOptionsCheckBox("LimitByYearBox");
        limitByYearBox.addMouseListener(createTipPanelUpdater(techLimitsHeader, "LimitByYearBox"));

        disallowExtinctStuffBox = new CampaignOptionsCheckBox("DisallowExtinctStuffBox");
        disallowExtinctStuffBox.addMouseListener(createTipPanelUpdater(techLimitsHeader, "DisallowExtinctStuffBox"));

        allowClanPurchasesBox = new CampaignOptionsCheckBox("AllowClanPurchasesBox");
        allowClanPurchasesBox.addMouseListener(createTipPanelUpdater(techLimitsHeader, "AllowClanPurchasesBox"));
        allowISPurchasesBox = new CampaignOptionsCheckBox("AllowISPurchasesBox");
        allowISPurchasesBox.addMouseListener(createTipPanelUpdater(techLimitsHeader, "AllowISPurchasesBox"));

        // Canon Purchases/Refits
        allowCanonOnlyBox = new CampaignOptionsCheckBox("AllowCanonOnlyBox");
        allowCanonOnlyBox.addMouseListener(createTipPanelUpdater(techLimitsHeader, "AllowCanonOnlyBox"));
        allowCanonRefitOnlyBox = new CampaignOptionsCheckBox("AllowCanonRefitOnlyBox");
        allowCanonRefitOnlyBox.addMouseListener(createTipPanelUpdater(techLimitsHeader, "AllowCanonRefitOnlyBox"));

        // Maximum Tech Level
        lblChoiceTechLevel = new CampaignOptionsLabel("ChoiceTechLevel");
        lblChoiceTechLevel.addMouseListener(createTipPanelUpdater(techLimitsHeader, "ChoiceTechLevel"));
        choiceTechLevel = new MMComboBox<>("choiceTechLevel", getMaximumTechLevelOptions());
        choiceTechLevel.addMouseListener(createTipPanelUpdater(techLimitsHeader, "ChoiceTechLevel"));
        choiceTechLevel.setToolTipText(String.format("<html>%s</html>",
              getTextAt(getCampaignOptionsResourceBundle(), "lblChoiceTechLevel.tooltip")));

        // Variable Tech Level
        variableTechLevelBox = new CampaignOptionsCheckBox("VariableTechLevelBox",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        variableTechLevelBox.addMouseListener(createTipPanelUpdater(techLimitsHeader, "VariableTechLevelBox"));

        // Ammo by Type
        useAmmoByTypeBox = new CampaignOptionsCheckBox("UseAmmoByTypeBox",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        useAmmoByTypeBox.addMouseListener(createTipPanelUpdater(techLimitsHeader, "UseAmmoByTypeBox"));

        JPanel techLevelPanel = createTechLevelPanel();
        JPanel purchaseRulesPanel = createPurchaseRulesPanel();
        techLimitsPageCreated = true;
        updateTechLimitsControlsFromModel();

        return CampaignOptionsPagePanel.builder("TechLimitsTab", "TechLimitsTab", imageAddress)
                .header(techLimitsHeader)
                .quote("techLimitsTab")
                .section("lblTechLimitsTab.text",
                        "lblTechLimitsTab.summary",
                        techLevelPanel)
                .section("lblTechPurchaseRulesPanel.text",
                        "lblTechPurchaseRulesPanel.summary",
                        purchaseRulesPanel)
                .build();
    }

    private @Nonnull JPanel createTechLevelPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TechLevelPanel",
              EQUIPMENT_LABEL_COLUMN_WIDTH,
              EQUIPMENT_CONTROL_COLUMN_WIDTH);
        panel.addRow(lblChoiceTechLevel, choiceTechLevel);
        panel.addCheckBox(variableTechLevelBox);
        panel.addCheckBox(useAmmoByTypeBox);

        return panel;
    }

    private @Nonnull JPanel createPurchaseRulesPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("TechPurchaseRulesPanel",
              EQUIPMENT_LABEL_COLUMN_WIDTH,
              EQUIPMENT_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
              limitByYearBox,
              disallowExtinctStuffBox,
              allowClanPurchasesBox,
              allowISPurchasesBox,
              allowCanonOnlyBox,
              allowCanonRefitOnlyBox);

        return panel;
    }

    /**
     * Creates and returns a DefaultComboBoxModel containing the available options for maximum technology levels.
     *
     * @return A DefaultComboBoxModel<String> populated with the list of technology level names corresponding to the
     *       defined constants in CampaignOptions (e.g., TECH_INTRO, TECH_STANDARD, etc.).
     */
    private static DefaultComboBoxModel<String> getMaximumTechLevelOptions() {
        DefaultComboBoxModel<String> maximumTechLevelModel = new DefaultComboBoxModel<>();

        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_INTRO));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_STANDARD));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_ADVANCED));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_EXPERIMENTAL));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_UNOFFICIAL));

        return maximumTechLevelModel;
    }

    /**
     * Loads values from the campaign options. This method serves as a convenience method that calls the overloaded
     * version of {@code loadValuesFromCampaignOptions} with a {@code null} parameter.
     * <p>
     * This method is typically used to initialize or update certain settings or configurations based on the campaign
     * options when no specific options are provided.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads values from the provided CampaignOptions instance into the UI components. If the provided CampaignOptions
     * instance is null, it defaults to using the internal campaignOptions instance.
     *
     * @param presetCampaignOptions the CampaignOptions instance containing the preset values to load, or null to use
     *                              the default internal campaignOptions.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new EquipmentAndSuppliesOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the given campaign options to the campaign or uses default options if none are provided. This method
     * updates the campaign settings for acquisitions, deliveries, planetary acquisitions, and technological limits to
     * customize campaign behavior.
     *
     * @param presetCampaignOptions the campaign options to apply; if null, default campaign options are used instead
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
        updateAcquisitionControlsFromModel();
        updatePlanetaryAcquisitionControlsFromModel();
        updateTechLimitsControlsFromModel();
    }

    private void updateAcquisitionControlsFromModel() {
        if (!acquisitionPageCreated || model == null) {
            return;
        }

        choiceAcquireSkill.setSelectedItem(model.acquisitionType);
        chkUseFunctionalAppraisal.setSelected(model.useFunctionalAppraisal);
        cboProcurementPersonnelPick.setSelectedItem(model.acquisitionPersonnelCategory.toString());
        spnAcquireClanPenalty.setValue(model.clanAcquisitionPenalty);
        spnAcquireIsPenalty.setValue(model.isAcquisitionPenalty);
        spnAcquireWaitingPeriod.setValue(model.waitingPeriod);
        spnMaxAcquisitions.setValue(model.maxAcquisitions);
        spnAutoLogisticsMekHead.setValue(model.autoLogisticsMekHead);
        spnAutoLogisticsMekLocation.setValue(model.autoLogisticsMekLocation);
        spnAutoLogisticsNonRepairableLocation.setValue(model.autoLogisticsNonRepairableLocation);
        spnAutoLogisticsArmor.setValue(model.autoLogisticsArmor);
        spnAutoLogisticsAmmunition.setValue(model.autoLogisticsAmmunition);
        spnAutoLogisticsActuators.setValue(model.autoLogisticsActuators);
        spnAutoLogisticsJumpJets.setValue(model.autoLogisticsJumpJets);
        spnAutoLogisticsEngines.setValue(model.autoLogisticsEngines);
        spnAutoLogisticsHeatSink.setValue(model.autoLogisticsHeatSink);
        spnAutoLogisticsWeapons.setValue(model.autoLogisticsWeapons);
        spnAutoLogisticsOther.setValue(model.autoLogisticsOther);
        choiceTransitTimeUnits.setSelectedIndex(model.unitTransitTime);
        chkNoDeliveriesInTransit.setSelected(model.noDeliveriesInTransit);
    }

    private void updatePlanetaryAcquisitionControlsFromModel() {
        if (!planetaryAcquisitionPageCreated || model == null) {
            return;
        }

        usePlanetaryAcquisitions.setSelected(model.usePlanetaryAcquisition);
        spnMaxJumpPlanetaryAcquisitions.setValue(model.maxJumpsPlanetaryAcquisition);
        comboPlanetaryAcquisitionsFactionLimits.setSelectedItem(model.planetAcquisitionFactionLimit);
        disallowPlanetaryAcquisitionClanCrossover.setSelected(model.disallowPlanetAcquisitionClanCrossover);
        disallowClanPartsFromIS.setSelected(model.noClanPartsFromIS);
        spnPenaltyClanPartsFromIS.setValue(model.penaltyClanPartsFromIS);
        usePlanetaryAcquisitionsVerbose.setSelected(model.planetAcquisitionVerbose);

        for (int i = 0; i < Math.min(spnPlanetAcquireTechBonus.length, model.planetTechAcquisitionBonus.length); i++) {
            spnPlanetAcquireTechBonus[i].setValue(model.planetTechAcquisitionBonus[i]);
        }
        for (int i = 0; i < Math.min(spnPlanetAcquireIndustryBonus.length,
              model.planetIndustryAcquisitionBonus.length); i++) {
            spnPlanetAcquireIndustryBonus[i].setValue(model.planetIndustryAcquisitionBonus[i]);
        }
        for (int i = 0; i < Math.min(spnPlanetAcquireOutputBonus.length,
              model.planetOutputAcquisitionBonus.length); i++) {
            spnPlanetAcquireOutputBonus[i].setValue(model.planetOutputAcquisitionBonus[i]);
        }
    }

    private void updateTechLimitsControlsFromModel() {
        if (!techLimitsPageCreated || model == null) {
            return;
        }

        limitByYearBox.setSelected(model.limitByYear);
        disallowExtinctStuffBox.setSelected(model.disallowExtinctStuff);
        allowClanPurchasesBox.setSelected(model.allowClanPurchases);
        allowISPurchasesBox.setSelected(model.allowISPurchases);
        allowCanonOnlyBox.setSelected(model.allowCanonOnly);
        allowCanonRefitOnlyBox.setSelected(model.allowCanonRefitOnly);
        choiceTechLevel.setSelectedIndex(model.techLevel);
        variableTechLevelBox.setSelected(model.variableTechLevel);
        useAmmoByTypeBox.setSelected(model.useAmmoByType);
    }

    private void updateModelFromCreatedControls() {
        updateModelFromAcquisitionControls();
        updateModelFromPlanetaryAcquisitionControls();
        updateModelFromTechLimitsControls();
    }

    private void updateModelFromAcquisitionControls() {
        if (!acquisitionPageCreated || model == null) {
            return;
        }

        model.acquisitionType = choiceAcquireSkill.getSelectedItem();
        model.useFunctionalAppraisal = chkUseFunctionalAppraisal.isSelected();
        model.acquisitionPersonnelCategory = ProcurementPersonnelPick.values()[cboProcurementPersonnelPick.getSelectedIndex()];
        model.clanAcquisitionPenalty = (int) spnAcquireClanPenalty.getValue();
        model.isAcquisitionPenalty = (int) spnAcquireIsPenalty.getValue();
        model.waitingPeriod = (int) spnAcquireWaitingPeriod.getValue();
        model.maxAcquisitions = (int) spnMaxAcquisitions.getValue();
        model.autoLogisticsMekHead = (int) spnAutoLogisticsMekHead.getValue();
        model.autoLogisticsMekLocation = (int) spnAutoLogisticsMekLocation.getValue();
        model.autoLogisticsNonRepairableLocation = (int) spnAutoLogisticsNonRepairableLocation.getValue();
        model.autoLogisticsArmor = (int) spnAutoLogisticsArmor.getValue();
        model.autoLogisticsAmmunition = (int) spnAutoLogisticsAmmunition.getValue();
        model.autoLogisticsActuators = (int) spnAutoLogisticsActuators.getValue();
        model.autoLogisticsJumpJets = (int) spnAutoLogisticsJumpJets.getValue();
        model.autoLogisticsEngines = (int) spnAutoLogisticsEngines.getValue();
        model.autoLogisticsHeatSink = (int) spnAutoLogisticsHeatSink.getValue();
        model.autoLogisticsWeapons = (int) spnAutoLogisticsWeapons.getValue();
        model.autoLogisticsOther = (int) spnAutoLogisticsOther.getValue();
        model.unitTransitTime = choiceTransitTimeUnits.getSelectedIndex();
        model.noDeliveriesInTransit = chkNoDeliveriesInTransit.isSelected();
    }

    private void updateModelFromPlanetaryAcquisitionControls() {
        if (!planetaryAcquisitionPageCreated || model == null) {
            return;
        }

        model.usePlanetaryAcquisition = usePlanetaryAcquisitions.isSelected();
        model.maxJumpsPlanetaryAcquisition = (int) spnMaxJumpPlanetaryAcquisitions.getValue();
        model.planetAcquisitionFactionLimit = comboPlanetaryAcquisitionsFactionLimits.getSelectedItem();
        model.disallowPlanetAcquisitionClanCrossover = disallowPlanetaryAcquisitionClanCrossover.isSelected();
        model.noClanPartsFromIS = disallowClanPartsFromIS.isSelected();
        model.penaltyClanPartsFromIS = (int) spnPenaltyClanPartsFromIS.getValue();
        model.planetAcquisitionVerbose = usePlanetaryAcquisitionsVerbose.isSelected();

        for (int i = 0; i < Math.min(spnPlanetAcquireTechBonus.length, model.planetTechAcquisitionBonus.length); i++) {
            model.planetTechAcquisitionBonus[i] = (int) spnPlanetAcquireTechBonus[i].getValue();
        }
        for (int i = 0; i < Math.min(spnPlanetAcquireIndustryBonus.length,
              model.planetIndustryAcquisitionBonus.length); i++) {
            model.planetIndustryAcquisitionBonus[i] = (int) spnPlanetAcquireIndustryBonus[i].getValue();
        }
        for (int i = 0; i < Math.min(spnPlanetAcquireOutputBonus.length,
              model.planetOutputAcquisitionBonus.length); i++) {
            model.planetOutputAcquisitionBonus[i] = (int) spnPlanetAcquireOutputBonus[i].getValue();
        }
    }

    private void updateModelFromTechLimitsControls() {
        if (!techLimitsPageCreated || model == null) {
            return;
        }

        model.limitByYear = limitByYearBox.isSelected();
        model.disallowExtinctStuff = disallowExtinctStuffBox.isSelected();
        model.allowClanPurchases = allowClanPurchasesBox.isSelected();
        model.allowISPurchases = allowISPurchasesBox.isSelected();
        model.allowCanonOnly = allowCanonOnlyBox.isSelected();
        model.allowCanonRefitOnly = allowCanonRefitOnlyBox.isSelected();
        model.techLevel = choiceTechLevel.getSelectedIndex();
        model.variableTechLevel = variableTechLevelBox.isSelected();
        model.useAmmoByType = useAmmoByTypeBox.isSelected();
    }

}
