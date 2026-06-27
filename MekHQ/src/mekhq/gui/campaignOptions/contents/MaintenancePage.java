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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code MaintenancePage} class builds and manages the Maintenance leaf page of the Campaign Options dialog. It owns
 * the widgets for maintenance configuration - cycle scheduling, maintenance bonuses, quality standards, planetary
 * modifiers, and unofficial maintenance rules - and synchronises them with a shared
 * {@link RepairAndMaintenanceOptionsModel}.
 *
 * <p>This view is a sub-component of {@link RepairAndMaintenancePages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code RepairAndMaintenancePages}, while this class is responsible only for constructing the
 * Maintenance panel and copying maintenance values to and from the model. The page is built lazily; until
 * {@link #createPanel(RepairAndMaintenanceOptionsModel)} is called,
 * {@link #readFromModel(RepairAndMaintenanceOptionsModel)} and {@link #writeToModel(RepairAndMaintenanceOptionsModel)}
 * are no-ops.</p>
 */
class MaintenancePage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

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

    private boolean created;

    /**
     * Creates the panel for the Maintenance Page.
     * <p>
     * This page provides configurable options for managing maintenance cycles,
     * quality standards, planetary effects, and
     * custom rules for units' upkeep.
     * </p>
     *
     * @param model the shared repair and maintenance options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Maintenance Page.
     */
    @Nonnull JPanel createPanel(@Nullable RepairAndMaintenanceOptionsModel model) {
        // Header
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

        created = true;
        readFromModel(model);

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
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(checkMaintenance);
        panel.addRow(lblMaintenanceDays, spnMaintenanceDays);
        panel.addRow(lblMaintenanceBonus, spnMaintenanceBonus);
        panel.addRow(lblDefaultMaintenanceTime, spnDefaultMaintenanceTime);
        panel.addCheckBox(logMaintenance);

        return panel;
    }

    private @Nonnull JPanel createMaintenanceQualityPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MaintenanceQualityPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                useQualityMaintenance,
                reverseQualityNames,
                chkUseRandomUnitQualities,
                chkUsePlanetaryModifiers);
        panel.addCheckBox(useUnofficialMaintenance);

        return panel;
    }

    /**
     * Copies maintenance values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared repair and maintenance options model to read values from
     */
    void readFromModel(@Nullable RepairAndMaintenanceOptionsModel model) {
        if (!created || model == null) {
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

    /**
     * Copies maintenance values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared repair and maintenance options model to write values into
     */
    void writeToModel(@Nullable RepairAndMaintenanceOptionsModel model) {
        if (!created || model == null) {
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
