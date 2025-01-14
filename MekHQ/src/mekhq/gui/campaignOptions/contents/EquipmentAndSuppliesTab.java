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
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.personnel.SkillType;
import mekhq.gui.campaignOptions.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

/**
 * The {@code EquipmentAndSuppliesTab} class represents a graphical user interface (GUI)
 * tab containing various options and settings related to equipment and supplies in a campaign simulation.
 * This class is responsible for building and managing multiple sub-tabs and panels for customization purposes,
 * including acquisition settings, delivery settings, planetary acquisition settings, and more.
 * It also provides methods to initialize, create, and manage these different components.
 * <p>
 * Fields in this class include labels, spinners, combo boxes, checkboxes, and panels used for displaying
 * and managing options in the tab. They allow the user to configure various parameters like transit times,
 * penalties, acquisition limits, faction-specific settings, and modifiers to influence game mechanics.
 * Multiple constants are defined for units of time, representing days, weeks, and months, among others.
 * <p>
 * The class includes initialization methods for different sections of the tab, as well as methods
 * to create panels for specific functionality. Utility methods are also provided for configuring spinners
 * and combo boxes or formatting options and labels.
 */
public class EquipmentAndSuppliesTab {
    // region Variable Declarations
    private static String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE,
        MekHQ.getMHQOptions().getLocale());

    private CampaignOptions campaignOptions;

    //start Acquisition Tab
    private JPanel pnlAcquisitions;
    private JLabel lblChoiceAcquireSkill;
    private MMComboBox<String> choiceAcquireSkill;
    private JCheckBox chkSupportStaffOnly;
    private JLabel lblAcquireClanPenalty;
    private JSpinner spnAcquireClanPenalty;
    private JLabel lblAcquireIsPenalty;
    private JSpinner spnAcquireIsPenalty;
    private JLabel lblAcquireWaitingPeriod;
    private JSpinner spnAcquireWaitingPeriod;
    private JLabel lblMaxAcquisitions;
    private JSpinner spnMaxAcquisitions;
    private JLabel lblDefaultStockPercent;
    private JSpinner spnDefaultStockPercent;
    //end Acquisition Tab

    //start Delivery Tab
    private JPanel pnlDeliveries;
    private JLabel lblNDiceTransitTime;
    private JSpinner spnNDiceTransitTime;
    private JLabel lblConstantTransitTime;
    private JSpinner spnConstantTransitTime;
    private JLabel lblAcquireMosBonus;
    private JSpinner spnAcquireMosBonus;
    private JLabel lblAcquireMinimum;
    private JSpinner spnAcquireMinimum;
    private MMComboBox<String> choiceTransitTimeUnits;
    private MMComboBox<String> choiceAcquireMosUnits;
    private MMComboBox<String> choiceAcquireMinimumUnit;
    private static final int TRANSIT_UNIT_DAY = 0;
    private static final int TRANSIT_UNIT_WEEK = 1;
    private static final int TRANSIT_UNIT_MONTH = 2;
    private static final int TRANSIT_UNIT_NUM = 3;
    //end Delivery Tab

    //start Planetary Acquisition Tab
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
    private JLabel[] lblPlanetAcquireIndustryBonus;
    private JSpinner[] spnPlanetAcquireIndustryBonus;

    private JPanel pnlOutputModifiers;
    private JLabel[] lblPlanetAcquireOutputBonus;
    private JSpinner[] spnPlanetAcquireOutputBonus;
    //end Planetary Acquisition Tab

    //start Tech Limits Tab
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
     * @param campaignOptions the {@link CampaignOptions} object containing
     *                        configuration settings for the campaign
     */
    public EquipmentAndSuppliesTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
    }

    /**
     * Initializes the EquipmentAndSuppliesTab by configuring its various components and panels.
     * This includes setting up the acquisitions tab, delivery tab, planetary acquisitions tab,
     * and tech limits tab.
     */
    void initialize() {
        initializeAcquisitionTab();
        initializeDelivery();
        initializePlanetaryAcquisitionsTab();
        initializeTechLimitsTab();
    }

    /**
     * Initializes the components and configurations for the Planetary Acquisitions tab in the
     * EquipmentAndSuppliesTab. This includes setting up options, labels, spinners, combo boxes,
     * checkboxes, and panels related to planetary acquisitions.
     * <p>
     * The method sets up the following:
     * <li> Configuration options for planetary acquisitions such as enabling/disabling planetary acquisitions,
     *   setting faction limits, and managing clan/Inner Sphere specific rules.</li>
     * <li> Panels and components for modifiers, including technology bonuses, industry bonuses,
     *   and output bonuses.</li>
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
        lblPlanetAcquireTechBonus = new JLabel[6];
        spnPlanetAcquireTechBonus = new JSpinner[6];

        pnlIndustryModifiers = new JPanel();
        lblPlanetAcquireIndustryBonus = new JLabel[6];
        spnPlanetAcquireIndustryBonus = new JSpinner[6];

        pnlOutputModifiers = new JPanel();
        lblPlanetAcquireOutputBonus = new JLabel[6];
        spnPlanetAcquireOutputBonus = new JSpinner[6];
    }

    /**
     * Initializes the components and configurations for the delivery tab within the
     * EquipmentAndSuppliesTab. This method sets up panels, labels, spinners, and combo boxes required
     * for managing delivery-related settings and options.
     * <p>
     * The setup includes:
     * <li> The main delivery panel.</li>
     * <li> Spinners for numeric inputs such as transit time settings and acquisition modifiers.</li>
     * <li> Labels for providing descriptions for components.</li>
     * <li> Combo boxes for selecting unit options related to transit times and acquisitions.</li>
     */
    private void initializeDelivery() {
        pnlDeliveries = new JPanel();
        lblNDiceTransitTime = new JLabel();
        spnNDiceTransitTime = new JSpinner();
        lblConstantTransitTime = new JLabel();
        spnConstantTransitTime = new JSpinner();
        choiceTransitTimeUnits = new MMComboBox<>("choiceTransitTimeUnits", getTransitUnitOptions());

        lblAcquireMosBonus = new JLabel();
        spnAcquireMosBonus = new JSpinner();
        choiceAcquireMosUnits = new MMComboBox<>("choiceAcquireMosUnits", getTransitUnitOptions());

        lblAcquireMinimum = new JLabel();
        spnAcquireMinimum = new JSpinner();
        choiceAcquireMinimumUnit = new MMComboBox<>("choiceAcquireMinimumUnit", getTransitUnitOptions());
    }

    /**
     * Initializes the components and settings for the acquisitions tab in the EquipmentAndSuppliesTab.
     * This method sets up the GUI components required for configuring acquisition-related options,
     * including panels, labels, spinners, combo boxes, and checkboxes.
     */
    private void initializeAcquisitionTab() {
        pnlAcquisitions = new JPanel();
        lblChoiceAcquireSkill = new JLabel();
        choiceAcquireSkill = new MMComboBox<>("choiceAcquireSkill", buildAcquireSkillComboOptions());

        chkSupportStaffOnly = new JCheckBox();

        lblAcquireClanPenalty = new JLabel();
        spnAcquireClanPenalty = new JSpinner();

        lblAcquireIsPenalty = new JLabel();
        spnAcquireIsPenalty = new JSpinner();

        lblAcquireWaitingPeriod = new JLabel();
        spnAcquireWaitingPeriod = new JSpinner();

        lblMaxAcquisitions = new JLabel();
        spnMaxAcquisitions = new JSpinner();
        lblDefaultStockPercent = new JLabel();
        spnDefaultStockPercent = new JSpinner();
    }

    /**
     * Initializes the components and settings for the Tech Limits tab in the EquipmentAndSuppliesTab.
     * This method sets up a series of checkboxes, labels, and combo boxes to configure technology-related
     * limits and options.
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
     * Creates and configures the acquisition tab panel for the user interface.
     * This method initializes and organizes the components such as the header,
     * acquisition panel, and delivery panel, and then returns the fully constructed
     * acquisition tab panel.
     *
     * @return A {@code JPanel} instance representing the complete acquisition tab.
     */
    public JPanel createAcquisitionTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("AcquisitionTab",
            getImageDirectory() + "logo_clan_cloud_cobra.png");

        pnlAcquisitions = createAcquisitionPanel();
        pnlDeliveries = createDeliveryPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("acquisitionTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(pnlAcquisitions, layoutParent);

        layoutParent.gridx++;
        panel.add(pnlDeliveries, layoutParent);


        // Create Parent Panel and return
        return createParentPanel(panel, "acquisitionsTab");
    }

    /**
     * Creates and returns a {@code JPanel} for configuring acquisition-related options.
     * This panel includes various components such as labels, checkboxes, and
     * spinners to allow users to set values for acquisition settings, including
     * penalties, waiting periods, maximum acquisitions, and stock percentages.
     *
     * @return A {@code JPanel} populated with acquisition configuration components and their layout.
     */
    private JPanel createAcquisitionPanel() {
        // Content
        lblChoiceAcquireSkill = new CampaignOptionsLabel("ChoiceAcquireSkill");

        chkSupportStaffOnly = new CampaignOptionsCheckBox("SupportStaffOnly");

        lblAcquireClanPenalty = new CampaignOptionsLabel("AcquireClanPenalty");
        spnAcquireClanPenalty = new CampaignOptionsSpinner("AcquireClanPenalty",
            0, 0, 13, 1);

        lblAcquireIsPenalty = new CampaignOptionsLabel("AcquireISPenalty");
        spnAcquireIsPenalty = new CampaignOptionsSpinner("AcquireISPenalty",
            0, 0, 13, 1);

        lblAcquireWaitingPeriod = new CampaignOptionsLabel("AcquireWaitingPeriod");
        spnAcquireWaitingPeriod = new CampaignOptionsSpinner("AcquireWaitingPeriod",
            1, 1, 365, 1);

        lblMaxAcquisitions = new CampaignOptionsLabel("MaxAcquisitions");
        spnMaxAcquisitions = new CampaignOptionsSpinner("MaxAcquisitions",
            0,0, 100, 1);

        lblDefaultStockPercent = new CampaignOptionsLabel("DefaultStockPercent");
        spnDefaultStockPercent = new CampaignOptionsSpinner("DefaultStockPercent",
                10.0, 0.0, 10000, 0.5);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AcquisitionPanel", true,
            "AcquisitionPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblChoiceAcquireSkill, layout);
        layout.gridx++;
        panel.add(choiceAcquireSkill, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkSupportStaffOnly, layout);

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
        panel.add(lblDefaultStockPercent, layout);
        layout.gridx++;
        panel.add(spnDefaultStockPercent, layout);

        return panel;
    }

    /**
     * Creates and configures a delivery panel composed of multiple sub-panels for managing transit time and delivery-related settings.
     * The panel includes components such as labels, spinners, and choice units, and ensures proper layout and structure.
     *
     * @return a {@code JPanel} instance representing the delivery panel with all configured sub-panels and components.
     */
    private JPanel createDeliveryPanel() {
        lblNDiceTransitTime = new CampaignOptionsLabel("NDiceTransitTime");
        spnNDiceTransitTime = new CampaignOptionsSpinner("NDiceTransitTime", 0,
            0, 365, 1);

        lblConstantTransitTime = new CampaignOptionsLabel("ConstantTransitTime");
        spnConstantTransitTime = new CampaignOptionsSpinner("ConstantTransitTime",
            0, 0, 365, 1);

        lblAcquireMosBonus = new CampaignOptionsLabel("AcquireMosBonus");
        spnAcquireMosBonus = new CampaignOptionsSpinner("AcquireMosBonus",
            0, 0, 365, 1);

        lblAcquireMinimum = new CampaignOptionsLabel("AcquireMinimum");
        spnAcquireMinimum = new CampaignOptionsSpinner("AcquireMinimum",
            0, 0, 365, 1);

        // Layout the Panel
        final JPanel panelTransit = new CampaignOptionsStandardPanel("DeliveryPanelTransit");
        final GridBagConstraints layoutTransit = new CampaignOptionsGridBagConstraints(panelTransit);

        layoutTransit.gridy = 0;
        layoutTransit.gridx = 0;
        layoutTransit.gridwidth = 1;
        panelTransit.add(lblNDiceTransitTime, layoutTransit);
        layoutTransit.gridx++;
        panelTransit.add(spnNDiceTransitTime, layoutTransit);
        layoutTransit.gridx++;
        panelTransit.add(lblConstantTransitTime, layoutTransit);
        layoutTransit.gridx++;
        panelTransit.add(spnConstantTransitTime, layoutTransit);
        layoutTransit.gridx++;
        panelTransit.add(choiceTransitTimeUnits, layoutTransit);

        // Layout the Panel
        final JPanel panelDeliveries = new CampaignOptionsStandardPanel("DeliveryPanelDeliveries");
        final GridBagConstraints layoutDeliveries = new CampaignOptionsGridBagConstraints(panelDeliveries);

        layoutDeliveries.gridy = 0;
        layoutDeliveries.gridx = 0;
        layoutDeliveries.gridwidth = 1;
        panelDeliveries.add(lblAcquireMosBonus, layoutDeliveries);
        layoutDeliveries.gridx++;
        panelDeliveries.add(spnAcquireMosBonus, layoutDeliveries);
        layoutDeliveries.gridx++;
        panelDeliveries.add(choiceAcquireMosUnits, layoutDeliveries);

        layoutDeliveries.gridx = 0;
        layoutDeliveries.gridy++;
        panelDeliveries.add(lblAcquireMinimum, layoutDeliveries);
        layoutDeliveries.gridx++;
        panelDeliveries.add(spnAcquireMinimum, layoutDeliveries);
        layoutDeliveries.gridx++;
        panelDeliveries.add(choiceAcquireMinimumUnit, layoutDeliveries);

        final JPanel panelParent = new CampaignOptionsStandardPanel("DeliveryPanel", true,
            "DeliveryPanel");
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridy = 0;
        layoutParent.gridx = 0;
        layoutParent.gridwidth = 2;
        panelParent.add(panelTransit, layoutParent);
        layoutParent.gridy++;
        panelParent.add(panelDeliveries, layoutParent);

        return panelParent;
    }

    /**
     * Creates and configures the planetary acquisition tab panel in a campaign options interface.
     * The panel includes a header, options, and modifiers section, arranged using
     * layout constraints. Once configured, it is wrapped within a parent panel and returned.
     *
     * @return a {@code JPanel} object representing the planetary acquisition tab with its configured components and layout.
     */
    public JPanel createPlanetaryAcquisitionTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("PlanetaryAcquisitionTab",
            getImageDirectory() + "logo_rim_worlds_republic.png");

        // Sub-Panels
        JPanel options = createOptionsPanel();
        JPanel modifiers = createModifiersPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PlanetaryAcquisitionTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridy = 0;
        panel.add(headerPanel, layoutParent);

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
     * Creates and returns a {@code JPanel} containing the components necessary
     * for configuring campaign options related to planetary acquisitions.
     * This panel includes various labels, checkboxes, and spinners
     * for setting and adjusting relevant options.
     *
     * @return a {@code JPanel} containing the campaign options panel for planetary acquisitions.
     */
    private JPanel createOptionsPanel() {
        usePlanetaryAcquisitions = new CampaignOptionsCheckBox("UsePlanetaryAcquisitions");

        lblMaxJumpPlanetaryAcquisitions = new CampaignOptionsLabel("MaxJumpPlanetaryAcquisitions");
        spnMaxJumpPlanetaryAcquisitions = new CampaignOptionsSpinner("MaxJumpPlanetaryAcquisitions",
            2, 0, 5, 1);

        lblPlanetaryAcquisitionsFactionLimits = new CampaignOptionsLabel("PlanetaryAcquisitionsFactionLimits");

        disallowPlanetaryAcquisitionClanCrossover = new CampaignOptionsCheckBox("DisallowPlanetaryAcquisitionClanCrossover");

        disallowClanPartsFromIS = new CampaignOptionsCheckBox("DisallowClanPartsFromIS");

        lblPenaltyClanPartsFromIS = new CampaignOptionsLabel("PenaltyClanPartsFromIS");
        spnPenaltyClanPartsFromIS = new CampaignOptionsSpinner("PenaltyClanPartsFromIS",
            0, 0, 12, 1);

        usePlanetaryAcquisitionsVerbose = new CampaignOptionsCheckBox("UsePlanetaryAcquisitionsVerbose");

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
     * Creates and returns a panel that organizes and displays the planetary acquisition
     * modifiers for technology, industry, and output. This method sets up spinners and
     * labels for each equipment type rating (A through F) to adjust acquisition bonuses.
     * <p>
     * The method initializes modifier spinners for technology, industry, and output
     * acquisition bonuses, creates separate panels for each category, and combines them
     * into a single panel using a grid layout.
     *
     * @return a {@code JPanel} representing the planetary acquisition modifiers panel, including
     *         elements for adjusting technology, industry, and output modifiers.
     */
    private JPanel createModifiersPanel() {
        // Modifier Spinners
        for (int i = EquipmentType.RATING_A; i <= EquipmentType.RATING_F; i++) {
            String modifierLabel = getModifierLabel(i);

            lblPlanetAcquireTechBonus[i] = new JLabel(String.format("<html>%s</html>",
                modifierLabel));
            spnPlanetAcquireTechBonus[i] = new JSpinner(new SpinnerNumberModel(
                0, -12, 12, 1));
            setSpinnerWidth(spnPlanetAcquireTechBonus[i]);

            lblPlanetAcquireIndustryBonus[i] = new JLabel(String.format("<html>%s</html>",
                modifierLabel));
            spnPlanetAcquireIndustryBonus[i] = new JSpinner(new SpinnerNumberModel(
                0, -12, 12, 1));
            setSpinnerWidth(spnPlanetAcquireIndustryBonus[i]);

            lblPlanetAcquireOutputBonus[i] = new JLabel(String.format("<html>%s</html>",
                modifierLabel));
            spnPlanetAcquireOutputBonus[i] = new JSpinner(new SpinnerNumberModel(
                0, -12, 12, 1));
            setSpinnerWidth(spnPlanetAcquireOutputBonus[i]);
        }

        // Panels
        pnlIndustryModifiers = createIndustryModifiersPanel();
        pnlTechModifiers = createTechModifiersPanel();
        pnlOutputModifiers = createOutputModifiersPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PlanetaryAcquisitionTabModifiers",
            true, "ModifiersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(pnlIndustryModifiers, layout);
        layout.gridx++;
        panel.add(pnlTechModifiers, layout);
        layout.gridx++;
        panel.add(pnlOutputModifiers, layout);

        return panel;
    }

    /**
     * Creates and returns a {@code JPanel} layout containing components for configuring
     * technology-related modifiers in a campaign setting. The panel includes
     * labels and corresponding input components (spinners) arranged in a
     * grid layout.
     *
     * @return a {@code JPanel} containing the layout for technology modifiers configuration
     */
    private JPanel createTechModifiersPanel() {
        JLabel techLabel = new CampaignOptionsLabel("TechLabel");
        techLabel.setName(String.format("<html><center>%s</center></html", techLabel.getText()));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("createTechModifiersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        for (int i = 0; i < 6; i++) {
            layout.gridx = 0;
            layout.gridy = i;
            panel.add(lblPlanetAcquireTechBonus[i], layout);
            layout.gridx++;
            panel.add(spnPlanetAcquireTechBonus[i], layout);
        }

        return panel;
    }

    /**
     * Creates and configures a {@code JPanel} that serves as the Industry Modifiers Panel.
     * The panel contains labels and spinners arranged in a grid layout to display
     * and allow modification of industry bonuses.
     *
     * @return a {@code JPanel} component configured as the Industry Modifiers Panel with
     *         labels and spinners for industry adjustment.
     */
    private JPanel createIndustryModifiersPanel() {
        JLabel industryLabel = new CampaignOptionsLabel("IndustryLabel");
        industryLabel.setName(String.format("<html><center>%s</center></html", industryLabel.getText()));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("IndustryModifiersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        for (int i = 0; i < 6; i++) {
            layout.gridx = 0;
            layout.gridy = i;
            panel.add(lblPlanetAcquireIndustryBonus[i], layout);
            layout.gridx++;
            panel.add(spnPlanetAcquireIndustryBonus[i], layout);
        }

        return panel;
    }

    /**
     * Creates and configures a {@code JPanel} for displaying and adjusting output modifiers.
     * The panel includes labels and corresponding spinner components to modify
     * planet acquisition output bonuses.
     *
     * @return a {@code JPanel} configured with labels and spinners for planet output modifiers
     */
    private JPanel createOutputModifiersPanel() {
        JLabel outputLabel = new CampaignOptionsLabel("OutputLabel");
        outputLabel.setName(String.format("<html><center>%s</center></html", outputLabel.getText()));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("OutputModifiersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        for (int i = 0; i < 6; i++) {
            layout.gridx = 0;
            layout.gridy = i;
            panel.add(lblPlanetAcquireOutputBonus[i], layout);
            layout.gridx++;
            panel.add(spnPlanetAcquireOutputBonus[i], layout);
        }

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
     * Determines the modifier label based on the provided quality rating.
     *
     * @param quality the integer representing the quality rating, corresponding to predefined
     *               constants in EquipmentType.
     * @return the string label associated with the provided quality rating. Returns "A" for RATING_A,
     * "B" for RATING_B, etc., or "ERROR" if the quality does not match any predefined ratings.
     */
    private String getModifierLabel(int quality) {
        return switch (quality) {
            case EquipmentType.RATING_A -> "A";
            case EquipmentType.RATING_B -> "B";
            case EquipmentType.RATING_C -> "C";
            case EquipmentType.RATING_D -> "D";
            case EquipmentType.RATING_E -> "E";
            case EquipmentType.RATING_F -> "F";
            default -> "ERROR";
        };
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
     * @return the name of the transit unit as a string, or "ERROR" if the unit is not recognized
     */
    private static String getTransitUnitName(final int unit) {
        return switch (unit) {
            case TRANSIT_UNIT_DAY -> resources.getString("transitUnitNamesDays.text");
            case TRANSIT_UNIT_WEEK -> resources.getString("transitUnitNamesWeeks.text");
            case TRANSIT_UNIT_MONTH -> resources.getString("transitUnitNamesMonths.text");
            default -> "ERROR";
        };
    }

    /**
     * Builds a DefaultComboBoxModel containing a predefined set of skill options
     * that can be acquired. The options include technical, administrative,
     * scrounge, negotiation, and auto skills.
     *
     * @return a DefaultComboBoxModel containing the skill options as string elements.
     */
    private static DefaultComboBoxModel<String> buildAcquireSkillComboOptions() {
        DefaultComboBoxModel<String> acquireSkillModel = new DefaultComboBoxModel<>();

        acquireSkillModel.addElement(CampaignOptions.S_TECH);
        acquireSkillModel.addElement(SkillType.S_ADMIN);
        acquireSkillModel.addElement(SkillType.S_SCROUNGE);
        acquireSkillModel.addElement(SkillType.S_NEG);
        acquireSkillModel.addElement(CampaignOptions.S_AUTO);

        return acquireSkillModel;
    }

    /**
     * Creates and initializes the "Tech Limits" tab panel within a user interface.
     * The tab includes various settings and options related to technical limitations,
     * such as limiting by year, disallowing extinct technologies, allowing faction-specific purchases,
     * enabling canon-only restrictions, setting maximum tech levels, and more.
     * The method arranges the components in a structured layout and constructs the required parent panel.
     *
     * @return the {@code JPanel} representing the "Tech Limits" tab, fully configured with its components and layout.
     */
    public JPanel createTechLimitsTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("TechLimitsTab",
            getImageDirectory() + "logo_clan_ghost_bear.png");

        limitByYearBox = new CampaignOptionsCheckBox("LimitByYearBox");

        disallowExtinctStuffBox = new CampaignOptionsCheckBox("DisallowExtinctStuffBox");

        allowClanPurchasesBox = new CampaignOptionsCheckBox("AllowClanPurchasesBox");
        allowISPurchasesBox = new CampaignOptionsCheckBox("AllowISPurchasesBox");

        // Canon Purchases/Refits
        allowCanonOnlyBox = new CampaignOptionsCheckBox("AllowCanonOnlyBox");
        allowCanonRefitOnlyBox = new CampaignOptionsCheckBox("AllowCanonRefitOnlyBox");

        // Maximum Tech Level
        lblChoiceTechLevel = new CampaignOptionsLabel("ChoiceTechLevel");
        choiceTechLevel = new MMComboBox<>("choiceTechLevel", getMaximumTechLevelOptions());
        choiceTechLevel.setToolTipText(String.format("<html>%s</html>",
                resources.getString("lblChoiceTechLevel.tooltip")));

        // Variable Tech Level
        variableTechLevelBox = new CampaignOptionsCheckBox("VariableTechLevelBox");

        // Ammo by Type
        useAmmoByTypeBox = new CampaignOptionsCheckBox("UseAmmoByTypeBox");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("TechLimitsTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(headerPanel, layoutParent);

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
     * Creates and returns a DefaultComboBoxModel containing the available
     * options for maximum technology levels.
     *
     * @return A DefaultComboBoxModel<String> populated with the list of
     *         technology level names corresponding to the defined constants
     *         in CampaignOptions (e.g., TECH_INTRO, TECH_STANDARD, etc.).
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
     * Applies the given campaign options to the campaign or uses default options if none are provided.
     * This method updates the campaign settings for acquisitions, deliveries, planetary acquisitions,
     * and technological limits to customize campaign behavior.
     *
     * @param presetCampaignOptions the campaign options to apply; if null, default campaign options
     *                              are used instead
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Acquisitions
        options.setAcquisitionSkill(choiceAcquireSkill.getSelectedItem());
        options.setAcquisitionSupportStaffOnly(chkSupportStaffOnly.isSelected());
        options.setClanAcquisitionPenalty((int) spnAcquireClanPenalty.getValue());
        options.setIsAcquisitionPenalty((int) spnAcquireIsPenalty.getValue());
        options.setWaitingPeriod((int) spnAcquireWaitingPeriod.getValue());
        options.setMaxAcquisitions((int) spnMaxAcquisitions.getValue());
        options.setDefaultStockPercent((double) spnDefaultStockPercent.getValue());

        // Delivery
        options.setNDiceTransitTime((int) spnNDiceTransitTime.getValue());
        options.setConstantTransitTime((int) spnConstantTransitTime.getValue());
        options.setUnitTransitTime(choiceTransitTimeUnits.getSelectedIndex());
        options.setAcquireMosBonus((int) spnAcquireMosBonus.getValue());
        options.setAcquireMosUnit(choiceAcquireMosUnits.getSelectedIndex());
        options.setAcquireMinimumTime((int) spnAcquireMinimum.getValue());
        options.setAcquireMinimumTimeUnit(choiceAcquireMinimumUnit.getSelectedIndex());

        // Planetary Acquisitions
        options.setPlanetaryAcquisition(usePlanetaryAcquisitions.isSelected());
        options.setMaxJumpsPlanetaryAcquisition((int) spnMaxJumpPlanetaryAcquisitions.getValue());
        options.setPlanetAcquisitionFactionLimit(comboPlanetaryAcquisitionsFactionLimits.getSelectedItem());
        options.setDisallowPlanetAcquisitionClanCrossover(disallowPlanetaryAcquisitionClanCrossover.isSelected());
        options.setDisallowPlanetAcquisitionClanCrossover(disallowPlanetaryAcquisitionClanCrossover.isSelected());
        options.setPenaltyClanPartsFromIS((int) spnPenaltyClanPartsFromIS.getValue());
        options.setPlanetAcquisitionVerboseReporting(usePlanetaryAcquisitionsVerbose.isSelected());
        for (int i = ITechnology.RATING_A; i <= ITechnology.RATING_F; i++) {
            options.setPlanetTechAcquisitionBonus((int) spnPlanetAcquireTechBonus[i].getValue(), i);
            options.setPlanetIndustryAcquisitionBonus(
                (int) spnPlanetAcquireIndustryBonus[i].getValue(), i);
            options.setPlanetOutputAcquisitionBonus((int) spnPlanetAcquireOutputBonus[i].getValue(),
                i);
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
     * Loads values from the campaign options. This method serves as a convenience
     * method that calls the overloaded version of {@code loadValuesFromCampaignOptions}
     * with a {@code null} parameter.
     * <p>
     * This method is typically used to initialize or update certain settings or
     * configurations based on the campaign options when no specific options are provided.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads values from the provided CampaignOptions instance into the UI components.
     * If the provided CampaignOptions instance is null, it defaults to using the internal campaignOptions instance.
     *
     * @param presetCampaignOptions the CampaignOptions instance containing the preset values to load,
     *                               or null to use the default internal campaignOptions.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Acquisitions
        choiceAcquireSkill.setSelectedItem(options.getAcquisitionSkill());
        chkSupportStaffOnly.setSelected(options.isAcquisitionSupportStaffOnly());
        spnAcquireClanPenalty.setValue(options.getClanAcquisitionPenalty());
        spnAcquireIsPenalty.setValue(options.getIsAcquisitionPenalty());
        spnAcquireWaitingPeriod.setValue(options.getWaitingPeriod());
        spnMaxAcquisitions.setValue(options.getMaxAcquisitions());
        spnDefaultStockPercent.setValue(options.getDefaultStockPercent());

        // Delivery
        spnNDiceTransitTime.setValue(options.getNDiceTransitTime());
        spnConstantTransitTime.setValue(options.getConstantTransitTime());
        choiceTransitTimeUnits.setSelectedIndex(options.getUnitTransitTime());

        spnAcquireMosBonus.setValue(options.getAcquireMosBonus());
        choiceAcquireMosUnits.setSelectedIndex(options.getAcquireMosUnit());

        spnAcquireMinimum.setValue(options.getAcquireMinimumTime());
        choiceAcquireMinimumUnit.setSelectedIndex(options.getAcquireMinimumTimeUnit());

        // Planetary Acquisitions
        usePlanetaryAcquisitions.setSelected(options.isUsePlanetaryAcquisition());
        spnMaxJumpPlanetaryAcquisitions.setValue(options.getMaxJumpsPlanetaryAcquisition());
        comboPlanetaryAcquisitionsFactionLimits.setSelectedItem(options.getPlanetAcquisitionFactionLimit());
        disallowPlanetaryAcquisitionClanCrossover.setSelected(options.isPlanetAcquisitionNoClanCrossover());
        disallowClanPartsFromIS.setSelected(options.isNoClanPartsFromIS());
        spnPenaltyClanPartsFromIS.setValue(options.getPenaltyClanPartsFromIS());
        usePlanetaryAcquisitionsVerbose.setSelected(options.isPlanetAcquisitionVerbose());
        for (int i = EquipmentType.RATING_A; i <= EquipmentType.RATING_F; i++) {
            spnPlanetAcquireTechBonus[i].setValue(options.getPlanetTechAcquisitionBonus(i));
            spnPlanetAcquireIndustryBonus[i].setValue(options.getPlanetIndustryAcquisitionBonus(i));
            spnPlanetAcquireOutputBonus[i].setValue(options.getPlanetOutputAcquisitionBonus(i));
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
