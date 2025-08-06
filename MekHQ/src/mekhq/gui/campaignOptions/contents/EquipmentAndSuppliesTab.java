/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;
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
    private CampaignOptions campaignOptions;

    //start Acquisition Tab
    private CampaignOptionsHeaderPanel acquisitionHeader;
    private JPanel pnlAcquisitions;
    private JLabel lblChoiceAcquireSkill;
    private MMComboBox<String> choiceAcquireSkill;
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

    private JPanel pnlTechModifiers;
    private JLabel[] lblPlanetAcquireTechBonus;
    private JSpinner[] spnPlanetAcquireTechBonus;

    private JPanel pnlIndustryModifiers;
    private JSpinner[] spnPlanetAcquireIndustryBonus;

    private JPanel pnlOutputModifiers;
    private JSpinner[] spnPlanetAcquireOutputBonus;
    //end Planetary Acquisition Tab

    //start Tech Limits Tab
    private CampaignOptionsHeaderPanel techLimitsHeader;
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
    public EquipmentAndSuppliesTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
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
        pnlTechModifiers = new JPanel();
        lblPlanetAcquireTechBonus = new JLabel[PlanetarySophistication.values().length];
        spnPlanetAcquireTechBonus = new JSpinner[PlanetarySophistication.values().length];

        pnlIndustryModifiers = new JPanel();
        spnPlanetAcquireIndustryBonus = new JSpinner[PlanetaryRating.values().length];

        pnlOutputModifiers = new JPanel();
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
    }

    /**
     * Initializes the components and settings for the acquisitions tab in the EquipmentAndSuppliesTab. This method sets
     * up the GUI components required for configuring acquisition-related options, including panels, labels, spinners,
     * combo boxes, and checkboxes.
     */
    private void initializeAcquisitionTab() {
        pnlAcquisitions = new JPanel();
        lblChoiceAcquireSkill = new JLabel();
        choiceAcquireSkill = new MMComboBox<>("choiceAcquireSkill", buildAcquireSkillComboOptions());

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
    public JPanel createAcquisitionTab() {
        // Header
        acquisitionHeader = new CampaignOptionsHeaderPanel("AcquisitionTab",
              getImageDirectory() + "logo_clan_cloud_cobra.png",
              4);

        pnlAcquisitions = createAcquisitionPanel();
        pnlAutoLogistics = createAutoLogisticsPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("acquisitionTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(acquisitionHeader, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(pnlAcquisitions, layoutParent);

        layoutParent.gridx++;
        panel.add(pnlAutoLogistics, layoutParent);


        // Create Parent Panel and return
        return createParentPanel(panel, "acquisitionsTab");
    }

    /**
     * Creates and returns a {@code JPanel} for configuring acquisition-related options. This panel includes various
     * components such as labels, checkboxes, and spinners to allow users to set values for acquisition settings,
     * including penalties, waiting periods, maximum acquisitions, and stock percentages.
     *
     * @return A {@code JPanel} populated with acquisition configuration components and their layout.
     */
    private JPanel createAcquisitionPanel() {
        // Content
        lblChoiceAcquireSkill = new CampaignOptionsLabel("ChoiceAcquireSkill");
        lblChoiceAcquireSkill.addMouseListener(createTipPanelUpdater(acquisitionHeader, "ChoiceAcquireSkill"));

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

        lblTransitTimeUnits = new CampaignOptionsLabel("TransitTimeUnits");
        lblTransitTimeUnits.addMouseListener(createTipPanelUpdater(acquisitionHeader, "TransitTimeUnits"));
        choiceTransitTimeUnits.addMouseListener(createTipPanelUpdater(acquisitionHeader, "TransitTimeUnits"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AcquisitionPanel", true, "AcquisitionPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblChoiceAcquireSkill, layout);
        layout.gridx++;
        panel.add(choiceAcquireSkill, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblProcurementPersonnelPick, layout);
        layout.gridx++;
        panel.add(cboProcurementPersonnelPick, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblAcquireClanPenalty, layout);
        layout.gridx++;
        panel.add(spnAcquireClanPenalty, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblAcquireIsPenalty, layout);
        layout.gridx++;
        panel.add(spnAcquireIsPenalty, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblAcquireWaitingPeriod, layout);
        layout.gridx++;
        panel.add(spnAcquireWaitingPeriod, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblMaxAcquisitions, layout);
        layout.gridx++;
        panel.add(spnMaxAcquisitions, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblTransitTimeUnits, layout);
        layout.gridx++;
        panel.add(choiceTransitTimeUnits, layout);

        return panel;
    }

    /**
     * Creates and returns a {@code JPanel} for configuring autoLogistics-related options. This panel includes various
     * components such as labels, checkboxes, and spinners to allow users to set values for acquisition settings,
     * including penalties, waiting periods, maximum acquisitions, and stock percentages.
     *
     * @return A {@code JPanel} populated with autoLogistics configuration components and their layout.
     */
    private JPanel createAutoLogisticsPanel() {
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
        final JPanel panel = new CampaignOptionsStandardPanel("AutoLogisticsPanel", true, "AutoLogisticsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        addSpinnerToPanel(panel, layout, lblAutoLogisticsMekHead, spnAutoLogisticsMekHead);
        addSpinnerToPanel(panel, layout, lblAutoLogisticsMekLocation, spnAutoLogisticsMekLocation);
        addSpinnerToPanel(panel, layout, lblAutoLogisticsNonRepairableLocation, spnAutoLogisticsNonRepairableLocation);
        addSpinnerToPanel(panel, layout, lblAutoLogisticsHeatSink, spnAutoLogisticsHeatSink);
        addSpinnerToPanel(panel, layout, lblAutoLogisticsArmor, spnAutoLogisticsArmor);
        addSpinnerToPanel(panel, layout, lblAutoLogisticsAmmunition, spnAutoLogisticsAmmunition);
        addSpinnerToPanel(panel, layout, lblAutoLogisticsActuators, spnAutoLogisticsActuators);
        addSpinnerToPanel(panel, layout, lblAutoLogisticsJumpJets, spnAutoLogisticsJumpJets);
        addSpinnerToPanel(panel, layout, lblAutoLogisticsEngines, spnAutoLogisticsEngines);
        addSpinnerToPanel(panel, layout, lblAutoLogisticsWeapons, spnAutoLogisticsWeapons);
        addSpinnerToPanel(panel, layout, lblAutoLogisticsOther, spnAutoLogisticsOther);

        return panel;
    }

    private void addSpinnerToPanel(JPanel panel, GridBagConstraints layout, JLabel label, JSpinner spinner) {
        layout.gridy++;
        panel.add(label, layout);
        layout.gridx++;
        panel.add(spinner, layout);
        layout.gridx = 0;
    }

    /**
     * Creates and configures the planetary acquisition tab panel in a campaign options interface. The panel includes a
     * header, options, and modifiers section, arranged using layout constraints. Once configured, it is wrapped within
     * a parent panel and returned.
     *
     * @return a {@code JPanel} object representing the planetary acquisition tab with its configured components and
     *       layout.
     */
    public JPanel createPlanetaryAcquisitionTab() {
        // Header
        planetaryAcquisitionHeader = new CampaignOptionsHeaderPanel("PlanetaryAcquisitionTab",
              getImageDirectory() + "logo_rim_worlds_republic.png",
              14);

        // Sub-Panels
        JPanel options = createOptionsPanel();
        JPanel modifiers = createModifiersPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PlanetaryAcquisitionTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridy = 0;
        panel.add(planetaryAcquisitionHeader, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(options, layoutParent);

        layoutParent.gridx++;
        panel.add(modifiers, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panel, "PlanetaryAcquisitionTab");
    }


    /**
     * Creates and returns a {@code JPanel} containing the components necessary for configuring campaign options related
     * to planetary acquisitions. This panel includes various labels, checkboxes, and spinners for setting and adjusting
     * relevant options.
     *
     * @return a {@code JPanel} containing the campaign options panel for planetary acquisitions.
     */
    private JPanel createOptionsPanel() {
        usePlanetaryAcquisitions = new CampaignOptionsCheckBox("UsePlanetaryAcquisitions");
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

        usePlanetaryAcquisitionsVerbose = new CampaignOptionsCheckBox("UsePlanetaryAcquisitionsVerbose");
        usePlanetaryAcquisitionsVerbose.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
              "UsePlanetaryAcquisitionsVerbose"));

        // Layout the Panel
        final JPanel panel = new JPanel();
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(usePlanetaryAcquisitions, layout);
        layout.gridx++;
        panel.add(usePlanetaryAcquisitionsVerbose, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblMaxJumpPlanetaryAcquisitions, layout);
        layout.gridx++;
        panel.add(spnMaxJumpPlanetaryAcquisitions, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblPlanetaryAcquisitionsFactionLimits, layout);
        layout.gridx++;
        panel.add(comboPlanetaryAcquisitionsFactionLimits, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(disallowPlanetaryAcquisitionClanCrossover, layout);

        layout.gridy++;
        panel.add(disallowClanPartsFromIS, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(lblPenaltyClanPartsFromIS, layout);
        layout.gridx++;
        panel.add(spnPenaltyClanPartsFromIS, layout);

        return panel;
    }

    /**
     * Creates and returns a panel that organizes and displays the planetary acquisition modifiers for technology,
     * industry, and output. This method sets up spinners and labels for each equipment type rating (A through F) to
     * adjust acquisition bonuses.
     * <p>
     * The method initializes modifier spinners for technology, industry, and output acquisition bonuses, creates
     * separate panels for each category, and combines them into a single panel using a grid layout.
     *
     * @return a {@code JPanel} representing the planetary acquisition modifiers panel, including elements for adjusting
     *       technology, industry, and output modifiers.
     */
    private JPanel createModifiersPanel() {
        // Modifier Spinners
        int i = 0;
        for (PlanetarySophistication sophistication : PlanetarySophistication.values()) {
            String techModifierLabel = sophistication.getName();
            lblPlanetAcquireTechBonus[i] = new JLabel(String.format("<html>%s</html>",
                  techModifierLabel));
            lblPlanetAcquireTechBonus[i].setHorizontalAlignment(SwingConstants.RIGHT);
            lblPlanetAcquireTechBonus[i].addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
                  "TechLabel"));
            spnPlanetAcquireTechBonus[i] = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            spnPlanetAcquireTechBonus[i].addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
                  "TechLabel"));
            setSpinnerWidth(spnPlanetAcquireTechBonus[i]);
            i++;
        }
        i = 0;
        for (PlanetaryRating rating : PlanetaryRating.values()) {
            String modifierLabel = rating.getName();
            spnPlanetAcquireIndustryBonus[i] = new JSpinner(new SpinnerNumberModel(
                  0, -12, 12, 1));
            spnPlanetAcquireIndustryBonus[i].addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
                  "IndustryLabel"));
            setSpinnerWidth(spnPlanetAcquireIndustryBonus[i]);
            spnPlanetAcquireOutputBonus[i] = new JSpinner(new SpinnerNumberModel(
                  0, -12, 12, 1));
            spnPlanetAcquireOutputBonus[i].addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader,
                  "OutputLabel"));
            setSpinnerWidth(spnPlanetAcquireOutputBonus[i]);
            i++;
        }
        // Panels
        pnlTechModifiers = createTechModifiersPanel();
        pnlIndustryModifiers = createIndustryModifiersPanel();
        pnlOutputModifiers = createOutputModifiersPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PlanetaryAcquisitionTabModifiers",
              true,
              "ModifiersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.anchor = GridBagConstraints.NORTH;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(pnlTechModifiers, layout);
        layout.gridx++;
        panel.add(pnlIndustryModifiers, layout);
        layout.gridx++;
        panel.add(pnlOutputModifiers, layout);

        return panel;
    }

    /**
     * Creates and returns a {@code JPanel} layout containing components for configuring technology-related modifiers in
     * a campaign setting. The panel includes labels and corresponding input components (spinners) arranged in a grid
     * layout.
     *
     * @return a {@code JPanel} containing the layout for technology modifiers configuration
     */
    private JPanel createTechModifiersPanel() {
        JLabel techLabel = new CampaignOptionsLabel("TechLabel");
        techLabel.setHorizontalAlignment(SwingConstants.CENTER);
        techLabel.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader, "TechLabel"));


        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("createTechModifiersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        // Add the label
        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 2;
        layout.weightx = 1.0;
        panel.add(techLabel, layout);

        // Add the other elements
        for (int i = 0; i < PlanetarySophistication.values().length; i++) {
            layout.gridx = 0;
            layout.gridy = i + 1;
            layout.gridwidth = 1;
            layout.weightx = 0;
            layout.anchor = GridBagConstraints.WEST;
            panel.add(lblPlanetAcquireTechBonus[i], layout);

            layout.gridx++;
            panel.add(spnPlanetAcquireTechBonus[i], layout);
        }

        return panel;
    }

    private int getSpinnerHeight() {
        int spinnerHeight = 0;
        if (spnPlanetAcquireIndustryBonus != null &&
                  spnPlanetAcquireIndustryBonus.length > 0 &&
                  spnPlanetAcquireIndustryBonus[0] != null) {
            spinnerHeight = spnPlanetAcquireIndustryBonus[0].getPreferredSize().height;
        } else {
            //fallback
            spinnerHeight = new JSpinner().getPreferredSize().height;
        }
        return spinnerHeight;
    }

    /**
     * Creates and configures a {@code JPanel} that serves as the Industry Modifiers Panel. The panel contains labels
     * and spinners arranged in a grid layout to display and allow modification of industry bonuses.
     *
     * @return a {@code JPanel} component configured as the Industry Modifiers Panel with labels and spinners for
     *       industry adjustment.
     */
    private JPanel createIndustryModifiersPanel() {
        JLabel industryLabel = new CampaignOptionsLabel("IndustryLabel");
        industryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        industryLabel.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader, "IndustryLabel"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("IndustryModifiersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        // Add the label
        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        layout.weightx = 1.0;
        panel.add(industryLabel, layout);
        layout.gridx = 0;
        layout.gridy = 1;
        layout.gridwidth = 1;
        layout.weightx = 0;
        layout.anchor = GridBagConstraints.WEST;
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(1, getSpinnerHeight()));
        panel.add(spacer, layout);
        // Add the other elements
        for (int i = 0; i < PlanetaryRating.values().length; i++) {
            layout.gridx = 0;
            layout.gridy = i + 2;
            layout.gridwidth = 1;
            layout.weightx = 0;
            layout.anchor = GridBagConstraints.WEST;
            panel.add(spnPlanetAcquireIndustryBonus[i], layout);
        }

        // Filler
        layout.gridx = 0;
        layout.gridy = PlanetaryRating.values().length + 2;
        layout.gridwidth = 1;
        JPanel bottomSpacer = new JPanel();
        bottomSpacer.setPreferredSize(new Dimension(1, getSpinnerHeight()));
        panel.add(bottomSpacer, layout);

        return panel;
    }

    /**
     * Creates and configures a {@code JPanel} for displaying and adjusting output modifiers. The panel includes labels
     * and corresponding spinner components to modify planet acquisition output bonuses.
     *
     * @return a {@code JPanel} configured with labels and spinners for planet output modifiers
     */
    private JPanel createOutputModifiersPanel() {
        JLabel outputLabel = new CampaignOptionsLabel("OutputLabel");
        outputLabel.setHorizontalAlignment(SwingConstants.CENTER);
        outputLabel.addMouseListener(createTipPanelUpdater(planetaryAcquisitionHeader, "OutputLabel"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("OutputModifiersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        // Add the label
        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        layout.weightx = 1.0;
        panel.add(outputLabel, layout);

        layout.gridx = 0;
        layout.gridy = 1;
        layout.gridwidth = 1;
        layout.weightx = 0;
        layout.anchor = GridBagConstraints.WEST;
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(1, getSpinnerHeight()));
        panel.add(spacer, layout);

        // Add the other elements
        for (int i = 0; i < PlanetaryRating.values().length; i++) {
            layout.gridx = 0;
            layout.gridy = i + 2;
            layout.gridwidth = 1;
            layout.weightx = 0;
            layout.anchor = GridBagConstraints.WEST;
            panel.add(spnPlanetAcquireOutputBonus[i], layout);
        }

        // Filler
        layout.gridx = 0;
        layout.gridy = PlanetaryRating.values().length + 2;
        layout.gridwidth = 1;
        JPanel bottomSpacer = new JPanel();
        bottomSpacer.setPreferredSize(new Dimension(1, getSpinnerHeight()));
        panel.add(bottomSpacer, layout);

        return panel;
    }

    /**
     * Sets the minimum width of the specified JSpinner component by scaling its preferred size.
     *
     * @param spinner the JSpinner component whose minimum width is to be set
     */
    private void setSpinnerWidth(JSpinner spinner) {
        Dimension size = spinner.getPreferredSize();
        spinner.setMinimumSize(UIUtil.scaleForGUI(size.width, size.height));
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
     * Builds a DefaultComboBoxModel containing a predefined set of skill options that can be acquired. The options
     * include technical, administrative, negotiation, and auto skills.
     *
     * @return a DefaultComboBoxModel containing the skill options as string elements.
     */
    private static DefaultComboBoxModel<String> buildAcquireSkillComboOptions() {
        DefaultComboBoxModel<String> acquireSkillModel = new DefaultComboBoxModel<>();

        acquireSkillModel.addElement(CampaignOptions.S_TECH);
        acquireSkillModel.addElement(SkillType.S_ADMIN);
        acquireSkillModel.addElement(SkillType.S_NEGOTIATION);
        acquireSkillModel.addElement(CampaignOptions.S_AUTO);

        return acquireSkillModel;
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
    public JPanel createTechLimitsTab() {
        // Header
        techLimitsHeader = new CampaignOptionsHeaderPanel("TechLimitsTab",
              getImageDirectory() + "logo_clan_ghost_bear.png",
              3);

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
        variableTechLevelBox = new CampaignOptionsCheckBox("VariableTechLevelBox");
        variableTechLevelBox.addMouseListener(createTipPanelUpdater(techLimitsHeader, "VariableTechLevelBox"));

        // Ammo by Type
        useAmmoByTypeBox = new CampaignOptionsCheckBox("UseAmmoByTypeBox");
        useAmmoByTypeBox.addMouseListener(createTipPanelUpdater(techLimitsHeader, "UseAmmoByTypeBox"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("TechLimitsTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(techLimitsHeader, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(lblChoiceTechLevel, layoutParent);
        layoutParent.gridx++;
        panel.add(choiceTechLevel, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        panel.add(limitByYearBox, layoutParent);
        layoutParent.gridx++;
        panel.add(disallowExtinctStuffBox, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        panel.add(allowClanPurchasesBox, layoutParent);
        layoutParent.gridx++;
        panel.add(allowISPurchasesBox, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        panel.add(allowCanonOnlyBox, layoutParent);
        layoutParent.gridx++;
        panel.add(allowCanonRefitOnlyBox, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        panel.add(variableTechLevelBox, layoutParent);
        layoutParent.gridx++;
        panel.add(useAmmoByTypeBox, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panel, "TechLimitsTab");
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

        // Acquisitions
        options.setAcquisitionSkill(choiceAcquireSkill.getSelectedItem());
        options.setAcquisitionPersonnelCategory(ProcurementPersonnelPick.values()[cboProcurementPersonnelPick.getSelectedIndex()]);
        options.setClanAcquisitionPenalty((int) spnAcquireClanPenalty.getValue());
        options.setIsAcquisitionPenalty((int) spnAcquireIsPenalty.getValue());
        options.setWaitingPeriod((int) spnAcquireWaitingPeriod.getValue());
        options.setMaxAcquisitions((int) spnMaxAcquisitions.getValue());

        // autoLogistics
        options.setAutoLogisticsMekHead((int) spnAutoLogisticsMekHead.getValue());
        options.setAutoLogisticsMekLocation((int) spnAutoLogisticsMekLocation.getValue());
        options.setAutoLogisticsNonRepairableLocation((int) spnAutoLogisticsNonRepairableLocation.getValue());
        options.setAutoLogisticsArmor((int) spnAutoLogisticsArmor.getValue());
        options.setAutoLogisticsAmmunition((int) spnAutoLogisticsAmmunition.getValue());
        options.setAutoLogisticsActuators((int) spnAutoLogisticsActuators.getValue());
        options.setAutoLogisticsJumpJets((int) spnAutoLogisticsJumpJets.getValue());
        options.setAutoLogisticsEngines((int) spnAutoLogisticsEngines.getValue());
        options.setAutoLogisticsHeatSink((int) spnAutoLogisticsHeatSink.getValue());
        options.setAutoLogisticsWeapons((int) spnAutoLogisticsWeapons.getValue());
        options.setAutoLogisticsOther((int) spnAutoLogisticsOther.getValue());

        // Delivery
        options.setUnitTransitTime(choiceTransitTimeUnits.getSelectedIndex());

        // Planetary Acquisitions
        options.setPlanetaryAcquisition(usePlanetaryAcquisitions.isSelected());
        options.setMaxJumpsPlanetaryAcquisition((int) spnMaxJumpPlanetaryAcquisitions.getValue());
        options.setPlanetAcquisitionFactionLimit(comboPlanetaryAcquisitionsFactionLimits.getSelectedItem());
        options.setDisallowPlanetAcquisitionClanCrossover(disallowPlanetaryAcquisitionClanCrossover.isSelected());
        options.setDisallowClanPartsFromIS(disallowClanPartsFromIS.isSelected());
        options.setPenaltyClanPartsFromIS((int) spnPenaltyClanPartsFromIS.getValue());
        options.setPlanetAcquisitionVerboseReporting(usePlanetaryAcquisitionsVerbose.isSelected());

        int i = 0;
        for (PlanetarySophistication sophistication : PlanetarySophistication.values()) {
            options.setPlanetTechAcquisitionBonus((int) spnPlanetAcquireTechBonus[i].getValue(), sophistication);
            i++;
        }
        i = 0;
        for (PlanetaryRating rating : PlanetaryRating.values()) {
            options.setPlanetIndustryAcquisitionBonus((int) spnPlanetAcquireIndustryBonus[i].getValue(), rating);
            options.setPlanetOutputAcquisitionBonus((int) spnPlanetAcquireOutputBonus[i].getValue(), rating);
            i++;
        }

        // Tech Limits
        options.setLimitByYear(limitByYearBox.isSelected());
        options.setDisallowExtinctStuff(disallowExtinctStuffBox.isSelected());
        options.setAllowClanPurchases(allowClanPurchasesBox.isSelected());
        options.setAllowISPurchases(allowISPurchasesBox.isSelected());
        options.setAllowCanonOnly(allowCanonOnlyBox.isSelected());
        options.setAllowCanonRefitOnly(allowCanonRefitOnlyBox.isSelected());
        options.setTechLevel(choiceTechLevel.getSelectedIndex());
        options.setVariableTechLevel(variableTechLevelBox.isSelected());
        options.setUseAmmoByType(useAmmoByTypeBox.isSelected());
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

        // Acquisitions
        choiceAcquireSkill.setSelectedItem(options.getAcquisitionSkill());
        cboProcurementPersonnelPick.setSelectedItem(options.getAcquisitionPersonnelCategory().toString());
        spnAcquireClanPenalty.setValue(options.getClanAcquisitionPenalty());
        spnAcquireIsPenalty.setValue(options.getIsAcquisitionPenalty());
        spnAcquireWaitingPeriod.setValue(options.getWaitingPeriod());
        spnMaxAcquisitions.setValue(options.getMaxAcquisitions());

        // autoLogistics
        spnAutoLogisticsMekHead.setValue(options.getAutoLogisticsMekHead());
        spnAutoLogisticsMekLocation.setValue(options.getAutoLogisticsMekLocation());
        spnAutoLogisticsNonRepairableLocation.setValue(options.getAutoLogisticsNonRepairableLocation());
        spnAutoLogisticsArmor.setValue(options.getAutoLogisticsArmor());
        spnAutoLogisticsAmmunition.setValue(options.getAutoLogisticsAmmunition());
        spnAutoLogisticsActuators.setValue(options.getAutoLogisticsActuators());
        spnAutoLogisticsJumpJets.setValue(options.getAutoLogisticsJumpJets());
        spnAutoLogisticsEngines.setValue(options.getAutoLogisticsEngines());
        spnAutoLogisticsHeatSink.setValue(options.getAutoLogisticsHeatSink());
        spnAutoLogisticsWeapons.setValue(options.getAutoLogisticsWeapons());
        spnAutoLogisticsOther.setValue(options.getAutoLogisticsOther());

        // Delivery
        choiceTransitTimeUnits.setSelectedIndex(options.getUnitTransitTime());

        // Planetary Acquisitions
        usePlanetaryAcquisitions.setSelected(options.isUsePlanetaryAcquisition());
        spnMaxJumpPlanetaryAcquisitions.setValue(options.getMaxJumpsPlanetaryAcquisition());
        comboPlanetaryAcquisitionsFactionLimits.setSelectedItem(options.getPlanetAcquisitionFactionLimit());
        disallowPlanetaryAcquisitionClanCrossover.setSelected(options.isPlanetAcquisitionNoClanCrossover());
        disallowClanPartsFromIS.setSelected(options.isNoClanPartsFromIS());
        spnPenaltyClanPartsFromIS.setValue(options.getPenaltyClanPartsFromIS());
        usePlanetaryAcquisitionsVerbose.setSelected(options.isPlanetAcquisitionVerbose());

        int i = 0;
        for (PlanetarySophistication sophistication : PlanetarySophistication.values()) {
            spnPlanetAcquireTechBonus[i].setValue(options.getPlanetTechAcquisitionBonus(sophistication));
            i++;
        }
        i = 0;
        for (PlanetaryRating rating : PlanetaryRating.values()) {
            spnPlanetAcquireIndustryBonus[i].setValue(options.getPlanetIndustryAcquisitionBonus(rating));
            spnPlanetAcquireOutputBonus[i].setValue(options.getPlanetOutputAcquisitionBonus(rating));
            i++;
        }

        // Tech Limits
        limitByYearBox.setSelected(options.isLimitByYear());
        disallowExtinctStuffBox.setSelected(options.isDisallowExtinctStuff());
        allowClanPurchasesBox.setSelected(options.isAllowClanPurchases());
        allowISPurchasesBox.setSelected(options.isAllowISPurchases());
        allowCanonOnlyBox.setSelected(options.isAllowCanonOnly());
        allowCanonRefitOnlyBox.setSelected(options.isAllowCanonRefitOnly());
        choiceTechLevel.setSelectedIndex(options.getTechLevel());
        variableTechLevelBox.setSelected(options.isVariableTechLevel());
        useAmmoByTypeBox.setSelected(options.isUseAmmoByType());
    }
}
