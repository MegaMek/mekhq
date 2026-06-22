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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * Represents a page in the campaign options UI used to configure settings
 * related to repair and maintenance in a
 * campaign.
 * <p>
 * This page is divided into two sections:
 * </p>
 * <ul>
 * <li><b>Repair Page:</b> Manages options for era modifications, tech
 * assignments,
 * equipment quirks handling, destruction margins, and more.</li>
 * <li><b>Maintenance Page:</b> Handles maintenance settings such as cycle
 * frequency, quality standards,
 * planetary modifiers, and unofficial maintenance rules.</li>
 * </ul>
 * <p>
 * The class interacts with {@link CampaignOptions}, enabling the retrieval,
 * storage,
 * and application of repair and maintenance configuration settings.
 * </p>
 */
public class RepairAndMaintenancePages {
    private static final int FORM_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int FORM_CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int CHECKBOX_GRID_COLUMNS = 2;

    private final CampaignOptions campaignOptions;
    private RepairAndMaintenanceOptionsModel model;
    private boolean repairPageCreated;
    private boolean maintenancePageCreated;

    private JCheckBox chkTechsUseAdministration;
    private JCheckBox chkUsefulAsTechs;
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
    // end Repair Page

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
    // end Maintenance Page

    /**
     * Constructs a {@code RepairAndMaintenancePages} instance for configuring repair
     * and maintenance-related settings.
     *
     * @param campaignOptions the {@link CampaignOptions} object to be used for
     *                        managing repair and maintenance
     *                        options.
     */
    public RepairAndMaintenancePages(@Nonnull CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
        loadValuesFromCampaignOptions();
    }

    /**
     * Initializes the components of the page, including both the Repair and
     * Maintenance sections.
     */
    void initialize() {
        initializeRepairPage();
        initializeMaintenancePage();
    }

    /**
     * Initializes the components for the Repair Page.
     * <p>
     * The Repair Page includes settings for era-based modifications, technician
     * assignment, equipment quirks, damage
     * margins, and destruction thresholds.
     * </p>
     */
    private void initializeRepairPage() {
        chkTechsUseAdministration = new JCheckBox();
        chkUsefulAsTechs = new JCheckBox();
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
     * Initializes the components for the Maintenance Page.
     * <p>
     * The Maintenance Page includes settings for maintenance scheduling, quality
     * rules, randomization factors, and
     * logging options.
     * </p>
     */
    private void initializeMaintenancePage() {
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
     * Creates the panel for the Repair Page.
     * <p>
     * This page provides configurable options for managing repair rules, handling
     * quirks, setting margins for equipment
     * survival, and incorporating era modifications.
     * </p>
     *
     * @return a {@link JPanel} representing the Repair Page.
     */
    public @Nonnull JPanel createRepairPage() {
        // Header
        // start Repair Page
        String imageAddress = getImageDirectory() + "logo_clan_burrock.png";
        CampaignOptionsHeaderPanel repairHeader = new CampaignOptionsHeaderPanel("RepairPage", imageAddress);

        chkTechsUseAdministration = new CampaignOptionsCheckBox("TechsUseAdministration",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkTechsUseAdministration.addMouseListener(createTipPanelUpdater("TechsUseAdministration"));

        chkUsefulAsTechs = new CampaignOptionsCheckBox("UsefulAsTechs",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUsefulAsTechs.addMouseListener(createTipPanelUpdater("UsefulAsTechs"));

        useEraModsCheckBox = new CampaignOptionsCheckBox("UseEraModsCheckBox");
        useEraModsCheckBox.addMouseListener(createTipPanelUpdater("UseEraModsCheckBox"));

        assignedTechFirstCheckBox = new CampaignOptionsCheckBox("AssignedTechFirstCheckBox");
        assignedTechFirstCheckBox.addMouseListener(createTipPanelUpdater("AssignedTechFirstCheckBox"));
        resetToFirstTechCheckBox = new CampaignOptionsCheckBox("ResetToFirstTechCheckBox");
        resetToFirstTechCheckBox.addMouseListener(createTipPanelUpdater("ResetToFirstTechCheckBox"));

        useQuirksBox = new CampaignOptionsCheckBox("UseQuirksBox");
        useQuirksBox.addMouseListener(createTipPanelUpdater("UseQuirksBox"));

        useAeroSystemHitsBox = new CampaignOptionsCheckBox("UseAeroSystemHitsBox");
        useAeroSystemHitsBox.addMouseListener(createTipPanelUpdater("UseAeroSystemHitsBox"));

        useDamageMargin = new CampaignOptionsCheckBox("UseDamageMargin");
        useDamageMargin.addMouseListener(createTipPanelUpdater("UseDamageMargin"));

        lblDamageMargin = new CampaignOptionsLabel("DamageMargin");
        lblDamageMargin.addMouseListener(createTipPanelUpdater("DamageMargin"));
        spnDamageMargin = new CampaignOptionsSpinner("DamageMargin",
                1, 1, 20, 1);
        spnDamageMargin.addMouseListener(createTipPanelUpdater("DamageMargin"));

        lblDestroyPartTarget = new CampaignOptionsLabel("DestroyPartTarget");
        lblDestroyPartTarget.addMouseListener(createTipPanelUpdater("DestroyPartTarget"));
        spnDestroyPartTarget = new CampaignOptionsSpinner("DestroyPartTarget",
                2, 2, 13, 1);
        spnDestroyPartTarget.addMouseListener(createTipPanelUpdater("DestroyPartTarget"));

        JPanel repairOptionsPanel = createRepairOptionsPanel();
        JPanel componentDamagePanel = createComponentDamagePanel();

        repairPageCreated = true;
        updateRepairControlsFromModel();

        return CampaignOptionsPagePanel.builder("RepairPage", "RepairPage", imageAddress)
                .header(repairHeader)
                .quote("repairPage")
                .section("lblRepairPage.text",
                        "lblRepairPage.summary",
                        repairOptionsPanel)
                .section("lblRepairPageRight.text",
                        "lblRepairPageRight.summary",
                        componentDamagePanel,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .build();
    }

    private @Nonnull JPanel createRepairOptionsPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RepairOptionsPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkTechsUseAdministration,
                chkUsefulAsTechs,
                useEraModsCheckBox,
                assignedTechFirstCheckBox,
                resetToFirstTechCheckBox,
                useQuirksBox);

        return panel;
    }

    private @Nonnull JPanel createComponentDamagePanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ComponentDamagePanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                useAeroSystemHitsBox,
                useDamageMargin);
        panel.addRow(lblDamageMargin, spnDamageMargin);
        panel.addRow(lblDestroyPartTarget, spnDestroyPartTarget);

        return panel;
    }

    /**
     * Creates the panel for the Maintenance Page.
     * <p>
     * This page provides configurable options for managing maintenance cycles,
     * quality standards, planetary effects, and
     * custom rules for units' upkeep.
     * </p>
     *
     * @return a {@link JPanel} representing the Maintenance Page.
     */
    public @Nonnull JPanel createMaintenancePage() {
        // Header
        // start Maintenance Page
        String imageAddress = getImageDirectory() + "logo_magistracy_of_canopus.png";
        CampaignOptionsHeaderPanel maintenanceHeader = new CampaignOptionsHeaderPanel("MaintenancePage", imageAddress);

        // Contents
        checkMaintenance = new CampaignOptionsCheckBox("CheckMaintenance");
        checkMaintenance.addMouseListener(createTipPanelUpdater("CheckMaintenance"));

        lblMaintenanceDays = new CampaignOptionsLabel("MaintenanceDays");
        lblMaintenanceDays.addMouseListener(createTipPanelUpdater("MaintenanceDays"));
        spnMaintenanceDays = new CampaignOptionsSpinner("MaintenanceDays",
                7, 1, 365, 1);
        spnMaintenanceDays.addMouseListener(createTipPanelUpdater("MaintenanceDays"));

        lblMaintenanceBonus = new CampaignOptionsLabel("MaintenanceBonus");
        lblMaintenanceBonus.addMouseListener(createTipPanelUpdater("MaintenanceBonus"));
        spnMaintenanceBonus = new CampaignOptionsSpinner("MaintenanceBonus",
                0, -13, 13, 1);
        spnMaintenanceBonus.addMouseListener(createTipPanelUpdater("MaintenanceBonus"));

        lblDefaultMaintenanceTime = new CampaignOptionsLabel("DefaultMaintenanceTime",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.RECOMMENDED));
        lblDefaultMaintenanceTime.addMouseListener(createTipPanelUpdater("DefaultMaintenanceTime"));
        spnDefaultMaintenanceTime = new CampaignOptionsSpinner("DefaultMaintenanceTime",
                1, 1, 4, 1);
        spnDefaultMaintenanceTime.addMouseListener(createTipPanelUpdater("DefaultMaintenanceTime"));

        useQualityMaintenance = new CampaignOptionsCheckBox("UseQualityMaintenance",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.RECOMMENDED));
        useQualityMaintenance.addMouseListener(createTipPanelUpdater("UseQualityMaintenance"));

        reverseQualityNames = new CampaignOptionsCheckBox("ReverseQualityNames",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.IMPORTANT));
        reverseQualityNames.addMouseListener(createTipPanelUpdater("ReverseQualityNames"));

        chkUseRandomUnitQualities = new CampaignOptionsCheckBox("UseRandomUnitQualities",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseRandomUnitQualities.addMouseListener(createTipPanelUpdater("UseRandomUnitQualities"));

        chkUsePlanetaryModifiers = new CampaignOptionsCheckBox("UsePlanetaryModifiers");
        chkUsePlanetaryModifiers.addMouseListener(createTipPanelUpdater("UsePlanetaryModifiers"));

        useUnofficialMaintenance = new CampaignOptionsCheckBox("UseUnofficialMaintenance",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                        CampaignOptionFlag.IMPORTANT));
        useUnofficialMaintenance.addMouseListener(createTipPanelUpdater("UseUnofficialMaintenance"));

        logMaintenance = new CampaignOptionsCheckBox("LogMaintenance");
        logMaintenance.addMouseListener(createTipPanelUpdater("LogMaintenance"));

        JPanel schedulePanel = createMaintenanceSchedulePanel();
        JPanel qualityPanel = createMaintenanceQualityPanel();

        maintenancePageCreated = true;
        updateMaintenanceControlsFromModel();

        return CampaignOptionsPagePanel.builder("MaintenancePage", "MaintenancePage", imageAddress)
                .header(maintenanceHeader)
                .quote("maintenancePage")
                .section("lblMaintenancePage.text",
                        "lblMaintenancePage.summary",
                        schedulePanel)
                .section("lblMaintenanceQualityPanel.text",
                        "lblMaintenanceQualityPanel.summary",
                        qualityPanel,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .build();
    }

    private @Nonnull JPanel createMaintenanceSchedulePanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MaintenanceSchedulePanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(checkMaintenance);
        panel.addRow(lblMaintenanceDays, spnMaintenanceDays);
        panel.addRow(lblMaintenanceBonus, spnMaintenanceBonus);
        panel.addRow(lblDefaultMaintenanceTime, spnDefaultMaintenanceTime);
        panel.addCheckBox(logMaintenance);

        return panel;
    }

    private @Nonnull JPanel createMaintenanceQualityPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MaintenanceQualityPanel",
                FORM_LABEL_COLUMN_WIDTH,
                FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                useQualityMaintenance,
                reverseQualityNames,
                chkUseRandomUnitQualities,
                chkUsePlanetaryModifiers);
        panel.addCheckBox(useUnofficialMaintenance);

        return panel;
    }

    /**
     * Loads the repair and maintenance settings from the default
     * {@link CampaignOptions} into the page's UI components.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads the repair and maintenance settings from the {@link CampaignOptions}
     * object into the page's UI components.
     * <p>
     * If no custom {@link CampaignOptions} are provided, the default
     * {@link CampaignOptions} associated with this page
     * is used.
     * </p>
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} object
     *                              to load settings from. If
     *                              {@code null}, the default options are used.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new RepairAndMaintenanceOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the current page's repair and maintenance settings from the UI
     * components to the provided
     * {@link CampaignOptions}.
     * <p>
     * If no custom {@link CampaignOptions} are provided, uses the default
     * {@link CampaignOptions} associated with this
     * page.
     * </p>
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} object
     *                              to apply the current settings to. If
     *                              {@code null}, the default options are modified.
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
        updateRepairControlsFromModel();
        updateMaintenanceControlsFromModel();
    }

    private void updateRepairControlsFromModel() {
        if (!repairPageCreated || model == null) {
            return;
        }

        chkTechsUseAdministration.setSelected(model.techsUseAdministration);
        chkUsefulAsTechs.setSelected(model.useUsefulAsTechs);
        useEraModsCheckBox.setSelected(model.useEraMods);
        assignedTechFirstCheckBox.setSelected(model.assignedTechFirst);
        resetToFirstTechCheckBox.setSelected(model.resetToFirstTech);
        useQuirksBox.setSelected(model.useQuirks);
        useAeroSystemHitsBox.setSelected(model.useAeroSystemHits);
        useDamageMargin.setSelected(model.destroyByMargin);
        spnDamageMargin.setValue(model.destroyMargin);
        spnDestroyPartTarget.setValue(model.destroyPartTarget);
    }

    private void updateMaintenanceControlsFromModel() {
        if (!maintenancePageCreated || model == null) {
            return;
        }

        checkMaintenance.setSelected(model.checkMaintenance);
        spnMaintenanceDays.setValue(model.maintenanceCycleDays);
        spnMaintenanceBonus.setValue(model.maintenanceBonus);
        spnDefaultMaintenanceTime.setValue(model.defaultMaintenanceTime);
        useQualityMaintenance.setSelected(model.useQualityMaintenance);
        reverseQualityNames.setSelected(model.reverseQualityNames);
        chkUseRandomUnitQualities.setSelected(model.useRandomUnitQualities);
        chkUsePlanetaryModifiers.setSelected(model.usePlanetaryModifiers);
        useUnofficialMaintenance.setSelected(model.useUnofficialMaintenance);
        logMaintenance.setSelected(model.logMaintenance);
    }

    private void updateModelFromCreatedControls() {
        updateModelFromRepairControls();
        updateModelFromMaintenanceControls();
    }

    private void updateModelFromRepairControls() {
        if (!repairPageCreated || model == null) {
            return;
        }

        model.techsUseAdministration = chkTechsUseAdministration.isSelected();
        model.useUsefulAsTechs = chkUsefulAsTechs.isSelected();
        model.useEraMods = useEraModsCheckBox.isSelected();
        model.assignedTechFirst = assignedTechFirstCheckBox.isSelected();
        model.resetToFirstTech = resetToFirstTechCheckBox.isSelected();
        model.useQuirks = useQuirksBox.isSelected();
        model.useAeroSystemHits = useAeroSystemHitsBox.isSelected();
        model.destroyByMargin = useDamageMargin.isSelected();
        model.destroyMargin = (int) spnDamageMargin.getValue();
        model.destroyPartTarget = (int) spnDestroyPartTarget.getValue();
    }

    private void updateModelFromMaintenanceControls() {
        if (!maintenancePageCreated || model == null) {
            return;
        }

        model.checkMaintenance = checkMaintenance.isSelected();
        model.maintenanceCycleDays = (int) spnMaintenanceDays.getValue();
        model.maintenanceBonus = (int) spnMaintenanceBonus.getValue();
        model.defaultMaintenanceTime = (int) spnDefaultMaintenanceTime.getValue();
        model.useQualityMaintenance = useQualityMaintenance.isSelected();
        model.reverseQualityNames = reverseQualityNames.isSelected();
        model.useRandomUnitQualities = chkUseRandomUnitQualities.isSelected();
        model.usePlanetaryModifiers = chkUsePlanetaryModifiers.isSelected();
        model.useUnofficialMaintenance = useUnofficialMaintenance.isSelected();
        model.logMaintenance = logMaintenance.isSelected();
    }

}
