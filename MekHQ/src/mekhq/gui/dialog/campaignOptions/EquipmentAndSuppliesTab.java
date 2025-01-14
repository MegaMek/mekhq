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
package mekhq.gui.dialog.campaignOptions;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.personnel.SkillType;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.*;

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

    public EquipmentAndSuppliesTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
    }

    /**
     * Calls the initialization methods for all the tab panels.
     */
    void initialize() {
        initializeAcquisitionTab();
        initializeDelivery();
        initializePlanetaryAcquisitionsTab();
        initializeTechLimitsTab();
    }

    /**
     * Initializes the components of the ProcreationTab.
     * The panel contains settings related to character procreation in the simulation.
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
     * Initializes the components of the DivorceTab.
     * The panel contains settings related to divorce mechanics within the simulation.
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
     * Initializes the components of the MarriageTab.
     * The panel contains various settings related to marriage mechanics within the simulation.
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
    }

    /**
     * Initializes the components of the TechLimitsTab.
     * This panel contains various controls for setting technological limits.
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
     * Creates the acquisition tab panel.
     *
     * @return the created tab panel as a {@link JPanel}
     */
    JPanel createAcquisitionTab() {
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
     * Creates the acquisition tab panel.
     *
     * @return the created tab panel as a {@link JPanel}
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

        return panel;
    }

    /**
     * Creates the delivery tab panel.
     *
     * @return the created tab panel as a {@link JPanel}
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

    JPanel createPlanetaryAcquisitionTab() {
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
     * Creates the planetary acquisition options panel.
     *
     * @return the created tab panel as a {@link JPanel}
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
     * Creates the planetary acquisition modifiers panel.
     *
     * @return the created tab panel as a {@link JPanel}
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
     * Creates the planetary acquisition modifiers panel.
     *
     * @return the created tab panel as a {@link JPanel}
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
     * Creates the planetary acquisition modifiers panel.
     *
     * @return the created tab panel as a {@link JPanel}
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
     * Sets the width of a {@link JSpinner} component based on the width of the text that would be
     * displayed in it.
     *
     * @param spinner the {@link JSpinner} to set the width for
     */
    private void setSpinnerWidth(JSpinner spinner) {
        Dimension size = spinner.getPreferredSize();
        spinner.setMinimumSize(UIUtil.scaleForGUI(size.width, size.height));
    }

    /**
     * Retrieves the label for a given quality modifier.
     *
     * @param quality The quality modifier represented by an integer value.
     * @return The label corresponding to the quality modifier.
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
     * Retrieves the transit unit options as a {@link DefaultComboBoxModel}.
     *
     * @return The {@link DefaultComboBoxModel} containing the transit unit options with labels fetched
     * from {@code getTransitUnitName()}.
     */
    private static DefaultComboBoxModel<String> getTransitUnitOptions() {
        DefaultComboBoxModel<String> transitUnitModel = new DefaultComboBoxModel<>();

        for (int i = 0; i < TRANSIT_UNIT_NUM; i++) {
            transitUnitModel.addElement(getTransitUnitName(i));
        }
        return transitUnitModel;
    }

    /**
     * Returns the name of the transit unit based on the given unit value.
     *
     * @param unit the unit value representing the transit unit
     * @return the name of the transit unit as a {@link String}
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
     * Builds the options for the acquisition skill combo box.
     *
     * @return the default combo box model containing the acquisition skill options
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
     * Creates a {@link JPanel} representing the tech limits tab.
     * This method constructs various components including checkboxes, labels, and combo boxes
     * to customize the tech limit settings.
     *
     * @return a {@link JPanel} containing the technical limits tab with all its configured components
     */
    JPanel createTechLimitsTab() {
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
     * @return a {@link DefaultComboBoxModel} containing options for maximum technology levels.
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

    void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
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

    void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
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
