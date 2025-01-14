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

import megamek.common.annotations.Nullable;
import mekhq.campaign.CampaignOptions;
import mekhq.gui.campaignOptions.components.*;

import javax.swing.*;
import java.awt.*;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

/**
 * Represents a tab in the campaign options UI used to configure settings related to
 * repair and maintenance in a campaign.
 * <p>
 * This tab is divided into two sections:
 * </p>
 * <p>
 *     <li><b>Repair Tab:</b> Manages options for era modifications, tech assignments,
 *         equipment quirks handling, destruction margins, and more.</li>
 *     <li><b>Maintenance Tab:</b> Handles maintenance settings such as cycle frequency, quality standards,
 *         planetary modifiers, and unofficial maintenance rules.</li>
 * </p>
 * <p>
 * The class interacts with {@link CampaignOptions}, enabling the retrieval, storage,
 * and application of repair and maintenance configuration settings.
 * </p>
 */
public class RepairAndMaintenanceTab {
    private final CampaignOptions campaignOptions;

    //start Repair Tab
    private JCheckBox useEraModsCheckBox;
    private JCheckBox assignedTechFirstCheckBox;
    private JCheckBox resetToFirstTechCheckBox;
    private JCheckBox useQuirksBox;
    private JCheckBox useAeroSystemHitsBox;
    private JCheckBox useDamageMargin;
    private JLabel lblDamageMargin;
    private JSpinner spnDamageMargin;
    private JLabel lblDestroyPartTarget;
    private JSpinner spnDestroyPartTarget;
    //end Repair Tab

    //start Maintenance Tab
    private JCheckBox checkMaintenance;
    private JLabel lblMaintenanceDays;
    private JSpinner spnMaintenanceDays;
    private JLabel lblMaintenanceBonus;
    private JSpinner spnMaintenanceBonus;
    private JLabel lblDefaultMaintenanceTime;
    private JSpinner spnDefaultMaintenanceTime;
    private JCheckBox useQualityMaintenance;
    private JCheckBox reverseQualityNames;
    private JCheckBox chkUseRandomUnitQualities;
    private JCheckBox chkUsePlanetaryModifiers;
    private JCheckBox useUnofficialMaintenance;
    private JCheckBox logMaintenance;
    //end Maintenance Tab

    /**
     * Constructs a {@code RepairAndMaintenanceTab} instance for configuring
     * repair and maintenance-related settings.
     *
     * @param campaignOptions the {@link CampaignOptions} object to be used
     *        for managing repair and maintenance options.
     */
    public RepairAndMaintenanceTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
    }

    /**
     * Initializes the components of the tab, including both the Repair and Maintenance sections.
     */
    void initialize() {
        initializeRepairTab();
        initializeMaintenanceTab();
    }

    /**
     * Initializes the components for the Repair Tab.
     * <p>
     * The Repair Tab includes settings for era-based modifications, technician assignment,
     * equipment quirks, damage margins, and destruction thresholds.
     * </p>
     */
    private void initializeRepairTab() {
        useEraModsCheckBox = new JCheckBox();

        assignedTechFirstCheckBox = new JCheckBox();

        resetToFirstTechCheckBox = new JCheckBox();

        useQuirksBox = new JCheckBox();

        useAeroSystemHitsBox = new JCheckBox();

        useDamageMargin = new JCheckBox();
        lblDamageMargin = new JLabel();
        spnDamageMargin = new JSpinner();

        lblDestroyPartTarget = new JLabel();
        spnDestroyPartTarget = new JSpinner();
    }

    /**
     * Initializes the components for the Maintenance Tab.
     * <p>
     * The Maintenance Tab includes settings for maintenance scheduling, quality rules,
     * randomization factors, and logging options.
     * </p>
     */
    private void initializeMaintenanceTab() {
        checkMaintenance = new JCheckBox();

        lblMaintenanceDays = new JLabel();
        spnMaintenanceDays = new JSpinner();

        lblMaintenanceBonus = new JLabel();
        spnMaintenanceBonus = new JSpinner();

        lblDefaultMaintenanceTime = new JLabel();
        spnDefaultMaintenanceTime = new JSpinner();

        useQualityMaintenance = new JCheckBox();

        reverseQualityNames = new JCheckBox();

        chkUseRandomUnitQualities = new JCheckBox();

        chkUsePlanetaryModifiers = new JCheckBox();

        useUnofficialMaintenance = new JCheckBox();

        logMaintenance = new JCheckBox();
    }

    /**
     * Creates the panel for the Repair Tab.
     * <p>
     * This tab provides configurable options for managing repair rules, handling quirks,
     * setting margins for equipment survival, and incorporating era modifications.
     * </p>
     *
     * @return a {@link JPanel} representing the Repair Tab.
     */
    public JPanel createRepairTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("RepairTab",
            getImageDirectory() + "logo_clan_burrock.png");

        // Era Mods
            useEraModsCheckBox = new CampaignOptionsCheckBox("UseEraModsCheckBox");

        // Tech Placement
        assignedTechFirstCheckBox = new CampaignOptionsCheckBox("AssignedTechFirstCheckBox");
        resetToFirstTechCheckBox = new CampaignOptionsCheckBox("ResetToFirstTechCheckBox");

        // Use Quirks
        useQuirksBox = new CampaignOptionsCheckBox("UseQuirksBox");

        // Aero System Damage
        useAeroSystemHitsBox = new CampaignOptionsCheckBox("UseAeroSystemHitsBox");

        // Damage by Margin
        useDamageMargin = new CampaignOptionsCheckBox("UseDamageMargin");
        useDamageMargin.addActionListener(evt -> spnDamageMargin.setEnabled(useDamageMargin.isSelected()));

        lblDamageMargin = new CampaignOptionsLabel("DamageMargin");
        spnDamageMargin = new CampaignOptionsSpinner("DamageMargin",
            1, 1, 20, 1);

        // Equipment Survival
        lblDestroyPartTarget = new CampaignOptionsLabel("DestroyPartTarget");
        spnDestroyPartTarget = new CampaignOptionsSpinner("DestroyPartTarget",
            2, 2, 13, 1);

        // Layout the Panel
        final JPanel panelLeft = new CampaignOptionsStandardPanel("repairTabLeft");
        final GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy = 0;
        layoutLeft.gridwidth = 1;
        panelLeft.add(useEraModsCheckBox, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(assignedTechFirstCheckBox, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(resetToFirstTechCheckBox, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(useQuirksBox, layoutLeft);

        final JPanel panelRight = new CampaignOptionsStandardPanel("RepairTabRight", true,
            "RepairTabRight");
        final GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutRight.gridx = 0;
        layoutRight.gridy = 0;
        layoutRight.gridwidth = 2;
        panelRight.add(useAeroSystemHitsBox, layoutRight);

        layoutRight.gridy++;
        panelRight.add(useDamageMargin, layoutRight);

        layoutRight.gridy++;
        layoutRight.gridwidth = 1;
        panelRight.add(lblDamageMargin, layoutRight);
        layoutRight.gridx++;
        panelRight.add(spnDamageMargin, layoutRight);

        layoutRight.gridx = 0;
        layoutRight.gridy++;
        panelRight.add(lblDestroyPartTarget, layoutRight);
        layoutRight.gridx++;
        panelRight.add(spnDestroyPartTarget, layoutRight);

        final JPanel panelParent = new CampaignOptionsStandardPanel("RepairTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);

        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "repairTab");
    }

    /**
     * Creates the panel for the Maintenance Tab.
     * <p>
     * This tab provides configurable options for managing maintenance cycles,
     * quality standards, planetary effects, and custom rules for units' upkeep.
     * </p>
     *
     * @return a {@link JPanel} representing the Maintenance Tab.
     */
    public JPanel createMaintenanceTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("MaintenanceTab",
            getImageDirectory() + "logo_magistracy_of_canopus.png");

        // Contents
        checkMaintenance = new CampaignOptionsCheckBox("CheckMaintenance");

        lblMaintenanceDays = new CampaignOptionsLabel("MaintenanceDays");
        spnMaintenanceDays = new CampaignOptionsSpinner("MaintenanceDays",
            7, 1, 365, 1);

        lblMaintenanceBonus = new CampaignOptionsLabel("MaintenanceBonus");
        spnMaintenanceBonus = new CampaignOptionsSpinner("MaintenanceBonus",
            0, -13, 13, 1);

        lblDefaultMaintenanceTime = new CampaignOptionsLabel("DefaultMaintenanceTime");
        spnDefaultMaintenanceTime = new CampaignOptionsSpinner("DefaultMaintenanceTime",
            1, 1, 4, 1);

        useQualityMaintenance = new CampaignOptionsCheckBox("UseQualityMaintenance");

        reverseQualityNames = new CampaignOptionsCheckBox("ReverseQualityNames");

        chkUseRandomUnitQualities = new CampaignOptionsCheckBox("UseRandomUnitQualities");

        chkUsePlanetaryModifiers = new CampaignOptionsCheckBox("UsePlanetaryModifiers");

        useUnofficialMaintenance = new CampaignOptionsCheckBox("UseUnofficialMaintenance");

        logMaintenance = new CampaignOptionsCheckBox("LogMaintenance");

        // Layout the Panel
        final JPanel panelLeft = new CampaignOptionsStandardPanel("repairTabLeft");
        GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy = 0;
        layoutLeft.gridwidth = 1;
        panelLeft.add(checkMaintenance, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(lblMaintenanceDays, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnMaintenanceDays, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblMaintenanceBonus, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnMaintenanceBonus, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblDefaultMaintenanceTime, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnDefaultMaintenanceTime, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        layoutLeft.gridwidth = 2;
        panelLeft.add(logMaintenance, layoutLeft);

        final JPanel panelRight = new CampaignOptionsStandardPanel("repairTabRight", true);
        GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutLeft.gridx = 0;
        layoutLeft.gridy = 0;
        layoutLeft.gridwidth = 1;
        panelRight.add(useQualityMaintenance, layoutRight);

        layoutRight.gridy++;
        panelRight.add(reverseQualityNames, layoutRight);

        layoutRight.gridy++;
        panelRight.add(chkUseRandomUnitQualities, layoutRight);

        layoutRight.gridy++;
        panelRight.add(chkUseRandomUnitQualities, layoutRight);

        layoutRight.gridy++;
        panelRight.add(chkUsePlanetaryModifiers, layoutRight);

        layoutRight.gridy++;
        panelRight.add(useUnofficialMaintenance, layoutRight);

        final JPanel panelParent = new CampaignOptionsStandardPanel("repairTab", true);
        GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);
        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "maintenanceTab");
    }

    /**
     * Applies the current tab's repair and maintenance settings from the UI components
     * to the provided {@link CampaignOptions}.
     * <p>
     * If no custom {@link CampaignOptions} are provided, uses the default
     * {@link CampaignOptions} associated with this tab.
     * </p>
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} object to
     *                              apply the current settings to. If {@code null}, the default options are modified.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Repair
        options.setEraMods(useEraModsCheckBox.isSelected());
        options.setAssignedTechFirst(assignedTechFirstCheckBox.isSelected());
        options.setResetToFirstTech(resetToFirstTechCheckBox.isSelected());
        options.setQuirks(useQuirksBox.isSelected());
        options.setUseAeroSystemHits(useAeroSystemHitsBox.isSelected());
        options.setDestroyByMargin(useDamageMargin.isSelected());
        options.setDestroyMargin((int) spnDamageMargin.getValue());
        options.setDestroyPartTarget((int) spnDestroyPartTarget.getValue());

        // Maintenance
        options.setCheckMaintenance(checkMaintenance.isSelected());
        options.setMaintenanceCycleDays((int) spnMaintenanceDays.getValue());
        options.setMaintenanceBonus((int) spnMaintenanceBonus.getValue());
        options.setDefaultMaintenanceTime((int) spnDefaultMaintenanceTime.getValue());
        options.setUseQualityMaintenance(useQualityMaintenance.isSelected());
        options.setReverseQualityNames(reverseQualityNames.isSelected());
        options.setUseRandomUnitQualities(chkUseRandomUnitQualities.isSelected());
        options.setUsePlanetaryModifiers(chkUsePlanetaryModifiers.isSelected());
        options.setUseUnofficialMaintenance(useUnofficialMaintenance.isSelected());
        options.setLogMaintenance(logMaintenance.isSelected());
    }

    /**
     * Loads the repair and maintenance settings from the default {@link CampaignOptions}
     * into the tab's UI components.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads the repair and maintenance settings from the {@link CampaignOptions} object
     * into the tab's UI components.
     * <p>
     * If no custom {@link CampaignOptions} are provided, the default
     * {@link CampaignOptions} associated with this tab is used.
     * </p>
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} object to load settings from.
     *                              If {@code null}, the default options are used.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Repair
        useEraModsCheckBox.setSelected(options.isUseEraMods());
        assignedTechFirstCheckBox.setSelected(options.isAssignedTechFirst());
        resetToFirstTechCheckBox.setSelected(options.isResetToFirstTech());
        useQuirksBox.setSelected(options.isUseQuirks());
        useAeroSystemHitsBox.setSelected(options.isUseAeroSystemHits());
        useDamageMargin.setSelected(options.isDestroyByMargin());
        spnDamageMargin.setValue(options.getDestroyMargin());
        spnDestroyPartTarget.setValue(options.getDestroyPartTarget());

        // Maintenance
        checkMaintenance.setSelected(options.isCheckMaintenance());
        spnMaintenanceDays.setValue(options.getMaintenanceCycleDays());
        spnMaintenanceBonus.setValue(options.getMaintenanceBonus());
        spnDefaultMaintenanceTime.setValue(options.getDefaultMaintenanceTime());
        useQualityMaintenance.setSelected(options.isUseQualityMaintenance());
        reverseQualityNames.setSelected(options.isReverseQualityNames());
        chkUseRandomUnitQualities.setSelected(options.isUseRandomUnitQualities());
        chkUsePlanetaryModifiers.setSelected(options.isUsePlanetaryModifiers());
        useUnofficialMaintenance.setSelected(options.isUseUnofficialMaintenance());
        logMaintenance.setSelected(options.isLogMaintenance());
    }
}
